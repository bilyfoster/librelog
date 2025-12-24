"""
Audio Delivery router for managing audio file delivery to playback systems
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.audio_delivery_service import AudioDeliveryService
from pydantic import BaseModel
from typing import Optional
import structlog

logger = structlog.get_logger()

router = APIRouter(prefix="/audio-delivery", tags=["audio-delivery"])


class DeliveryRequest(BaseModel):
    cut_id: UUID
    delivery_method: str
    target_server: str
    target_path: Optional[str] = None
    max_retries: int = 3


class DeliveryResponse(BaseModel):
    id: UUID
    cut_id: UUID
    delivery_method: str
    target_server: str
    status: str
    retry_count: int
    checksum_verified: bool
    created_at: str

    class Config:
        from_attributes = True


@router.post("/", response_model=DeliveryResponse, status_code=status.HTTP_201_CREATED)
async def deliver_audio(
    request: DeliveryRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Deliver an audio file to a playback system"""
    service = AudioDeliveryService(db)
    try:
        delivery = await service.deliver_audio(
            cut_id=request.cut_id,
            delivery_method=request.delivery_method,
            target_server=request.target_server,
            target_path=request.target_path,
            max_retries=request.max_retries
        )
        return DeliveryResponse.model_validate(delivery)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/{delivery_id}/retry", response_model=DeliveryResponse)
async def retry_delivery(
    delivery_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Retry a failed delivery"""
    service = AudioDeliveryService(db)
    try:
        delivery = await service.retry_delivery(delivery_id)
        return DeliveryResponse.model_validate(delivery)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

