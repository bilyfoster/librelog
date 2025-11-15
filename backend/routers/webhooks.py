"""
Webhooks router for external integrations
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update, delete
from typing import List, Optional
from pydantic import BaseModel, HttpUrl

from backend.database import get_db
from backend.models.webhook import Webhook, WebhookType, WebhookEvent
from backend.models.user import User
from backend.routers.auth import get_current_user
from backend.services.webhook_service import WebhookService

router = APIRouter(prefix="/webhooks", tags=["webhooks"])


class WebhookCreate(BaseModel):
    name: str
    webhook_type: WebhookType
    url: str
    events: List[WebhookEvent]
    secret: Optional[str] = None
    headers: Optional[dict] = None


class WebhookUpdate(BaseModel):
    name: Optional[str] = None
    url: Optional[str] = None
    events: Optional[List[WebhookEvent]] = None
    secret: Optional[str] = None
    active: Optional[bool] = None
    headers: Optional[dict] = None


class WebhookResponse(BaseModel):
    id: int
    name: str
    webhook_type: WebhookType
    url: str
    events: List[str]
    active: bool
    last_triggered_at: Optional[str] = None
    last_error: Optional[str] = None
    error_count: int
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=List[WebhookResponse])
async def list_webhooks(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(False),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all webhooks"""
    query = select(Webhook)

    if active_only:
        query = query.where(Webhook.active == True)

    query = query.offset(skip).limit(limit).order_by(Webhook.created_at.desc())

    result = await db.execute(query)
    webhooks = result.scalars().all()
    return webhooks


@router.post("/", response_model=WebhookResponse, status_code=status.HTTP_201_CREATED)
async def create_webhook(
    webhook: WebhookCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new webhook"""
    new_webhook = Webhook(
        name=webhook.name,
        webhook_type=webhook.webhook_type,
        url=webhook.url,
        events=[e.value for e in webhook.events],
        secret=webhook.secret,
        headers=webhook.headers,
        created_by=current_user.id,
        active=True
    )
    db.add(new_webhook)
    await db.commit()
    await db.refresh(new_webhook)
    return new_webhook


@router.get("/{webhook_id}", response_model=WebhookResponse)
async def get_webhook(
    webhook_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific webhook"""
    result = await db.execute(select(Webhook).where(Webhook.id == webhook_id))
    webhook = result.scalar_one_or_none()

    if not webhook:
        raise HTTPException(status_code=404, detail="Webhook not found")

    return webhook


@router.put("/{webhook_id}", response_model=WebhookResponse)
async def update_webhook(
    webhook_id: int,
    webhook_update: WebhookUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a webhook"""
    result = await db.execute(select(Webhook).where(Webhook.id == webhook_id))
    webhook = result.scalar_one_or_none()

    if not webhook:
        raise HTTPException(status_code=404, detail="Webhook not found")

    update_data = webhook_update.model_dump(exclude_unset=True)
    if "events" in update_data and update_data["events"]:
        update_data["events"] = [e.value if isinstance(e, WebhookEvent) else e for e in update_data["events"]]

    for field, value in update_data.items():
        setattr(webhook, field, value)

    await db.commit()
    await db.refresh(webhook)
    return webhook


@router.delete("/{webhook_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_webhook(
    webhook_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a webhook"""
    result = await db.execute(select(Webhook).where(Webhook.id == webhook_id))
    webhook = result.scalar_one_or_none()

    if not webhook:
        raise HTTPException(status_code=404, detail="Webhook not found")

    await db.delete(webhook)
    await db.commit()


@router.post("/{webhook_id}/test", status_code=status.HTTP_200_OK)
async def test_webhook(
    webhook_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Test a webhook with a sample payload"""
    result = await db.execute(select(Webhook).where(Webhook.id == webhook_id))
    webhook = result.scalar_one_or_none()

    if not webhook:
        raise HTTPException(status_code=404, detail="Webhook not found")

    # Use first event or default to USER_ACTIVITY
    test_event = WebhookEvent(webhook.events[0]) if webhook.events else WebhookEvent.USER_ACTIVITY

    success = await WebhookService.trigger_webhook(
        db,
        webhook,
        test_event,
        {"test": True, "message": "This is a test webhook from LibreLog"}
    )

    if success:
        return {"status": "success", "message": "Webhook triggered successfully"}
    else:
        raise HTTPException(status_code=500, detail="Webhook test failed")

