"""
Campaign management router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query, Request
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from backend.database import get_db
from backend.models.campaign import Campaign
from backend.services.campaign_service import CampaignService
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.logging.audit import audit_logger
from pydantic import BaseModel
from typing import Optional, List
from datetime import date

router = APIRouter()


@router.get("/count")
async def get_campaigns_count(
    active_only: bool = Query(True),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get total count of campaigns"""
    query = select(func.count(Campaign.id))
    
    if active_only:
        query = query.where(Campaign.active == True)
    
    result = await db.execute(query)
    count = result.scalar() or 0
    
    return {"count": count}


class CampaignCreate(BaseModel):
    name: str
    advertiser: str
    start_date: date
    end_date: date
    priority: int
    target_hours: List[str]
    ad_type: str
    file_path: Optional[str] = None


class CampaignUpdate(BaseModel):
    name: Optional[str] = None
    advertiser: Optional[str] = None
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    priority: Optional[int] = None
    target_hours: Optional[List[str]] = None
    ad_type: Optional[str] = None
    file_path: Optional[str] = None
    active: Optional[bool] = None


@router.get("/")
async def list_campaigns(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    date_filter: Optional[date] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all campaigns"""
    service = CampaignService(db)
    campaigns = await service.list_campaigns(
        skip=skip,
        limit=limit,
        active_only=active_only,
        date_filter=date_filter
    )
    
    return {
        "campaigns": [
            {
                "id": c.id,
                "advertiser": c.advertiser,
                "start_date": c.start_date,
                "end_date": c.end_date,
                "priority": c.priority,
                "file_url": c.file_url,
                "active": c.active,
                "order_number": c.order_number,
                "contract_number": c.contract_number,
                "spot_lengths": c.spot_lengths,
                "rate_type": c.rate_type.value if c.rate_type else None,
                "rates": c.rates,
                "approval_status": c.approval_status.value if c.approval_status else None,
                "created_at": c.created_at.isoformat() if c.created_at else None,
                "updated_at": c.updated_at.isoformat() if c.updated_at else None
            }
            for c in campaigns
        ],
        "total": len(campaigns)
    }


@router.post("/")
async def create_campaign(
    campaign_data: CampaignCreate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new campaign"""
    import logging
    logger = logging.getLogger(__name__)
    
    service = CampaignService(db)
    
    try:
        campaign = await service.create_campaign(
            name=campaign_data.name,
            advertiser=campaign_data.advertiser,
            start_date=campaign_data.start_date,
            end_date=campaign_data.end_date,
            priority=campaign_data.priority,
            target_hours=campaign_data.target_hours,
            ad_type=campaign_data.ad_type,
            file_path=campaign_data.file_path,
            user_id=current_user.id
        )
        
        # Log audit action (non-blocking)
        try:
            client_ip = request.client.host if request.client else None
            user_agent = request.headers.get("user-agent")
            await audit_logger.log_action(
                db_session=db,
                user_id=current_user.id,
                action="CREATE_CAMPAIGN",
                resource_type="Campaign",
                resource_id=campaign.id,
                details={
                    "name": campaign.name,
                    "advertiser": campaign.advertiser,
                    "start_date": campaign.start_date.isoformat() if campaign.start_date else None,
                    "end_date": campaign.end_date.isoformat() if campaign.end_date else None
                },
                ip_address=client_ip,
                user_agent=user_agent
            )
        except Exception as e:
            logger.warning(f"Failed to log audit action for campaign creation: {e}")
        
        return {
            "id": campaign.id,
            "advertiser": campaign.advertiser,
            "start_date": campaign.start_date,
            "end_date": campaign.end_date,
            "priority": campaign.priority,
            "file_url": campaign.file_url,
            "active": campaign.active,
            "order_number": campaign.order_number,
            "contract_number": campaign.contract_number,
            "spot_lengths": campaign.spot_lengths,
            "rate_type": campaign.rate_type.value if campaign.rate_type else None,
            "approval_status": campaign.approval_status.value if campaign.approval_status else None,
            "created_at": campaign.created_at.isoformat() if campaign.created_at else None,
            "message": "Campaign created successfully"
        }
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception as e:
        logger.error(f"Error creating campaign: {e}", exc_info=True)
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create campaign: {str(e)}"
        )


@router.get("/{campaign_id}")
async def get_campaign(
    campaign_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific campaign"""
    service = CampaignService(db)
    campaign = await service.get_campaign(campaign_id)
    
    if not campaign:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Campaign not found")
    
    return {
        "id": campaign.id,
        "advertiser": campaign.advertiser,
        "start_date": campaign.start_date,
        "end_date": campaign.end_date,
        "priority": campaign.priority,
        "file_url": campaign.file_url,
        "active": campaign.active,
        "order_number": campaign.order_number,
        "contract_number": campaign.contract_number,
        "spot_lengths": campaign.spot_lengths,
        "rate_type": campaign.rate_type.value if campaign.rate_type else None,
        "rates": campaign.rates,
        "approval_status": campaign.approval_status.value if campaign.approval_status else None,
        "created_at": campaign.created_at.isoformat() if campaign.created_at else None,
        "updated_at": campaign.updated_at.isoformat() if campaign.updated_at else None
    }


@router.put("/{campaign_id}")
async def update_campaign(
    campaign_id: UUID,
    campaign_data: CampaignUpdate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a campaign"""
    service = CampaignService(db)
    
    try:
        campaign = await service.update_campaign(
            campaign_id=campaign_id,
            name=campaign_data.name,
            advertiser=campaign_data.advertiser,
            start_date=campaign_data.start_date,
            end_date=campaign_data.end_date,
            priority=campaign_data.priority,
            target_hours=campaign_data.target_hours,
            ad_type=campaign_data.ad_type,
            file_path=campaign_data.file_path,
            active=campaign_data.active
        )
        
        if not campaign:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Campaign not found")
        
        # Log audit action
        client_ip = request.client.host if request.client else None
        user_agent = request.headers.get("user-agent")
        update_fields = {k: v for k, v in campaign_data.model_dump(exclude_unset=True).items() if v is not None}
        await audit_logger.log_action(
            db_session=db,
            user_id=current_user.id,
            action="UPDATE_CAMPAIGN",
            resource_type="Campaign",
            resource_id=campaign_id,
            details={
                "name": campaign.name,
                "updated_fields": list(update_fields.keys())
            },
            ip_address=client_ip,
            user_agent=user_agent
        )
        
        return {
            "id": campaign.id,
            "advertiser": campaign.advertiser,
            "start_date": campaign.start_date,
            "end_date": campaign.end_date,
            "priority": campaign.priority,
            "file_url": campaign.file_url,
            "active": campaign.active,
            "order_number": campaign.order_number,
            "contract_number": campaign.contract_number,
            "spot_lengths": campaign.spot_lengths,
            "rate_type": campaign.rate_type.value if campaign.rate_type else None,
            "approval_status": campaign.approval_status.value if campaign.approval_status else None,
            "updated_at": campaign.updated_at.isoformat() if campaign.updated_at else None,
            "message": "Campaign updated successfully"
        }
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.delete("/{campaign_id}")
async def delete_campaign(
    campaign_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a campaign"""
    service = CampaignService(db)
    success = await service.delete_campaign(campaign_id)
    
    if not success:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Campaign not found")
    
    return {"message": "Campaign deleted successfully"}


@router.get("/{campaign_id}/stats")
async def get_campaign_stats(
    campaign_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get campaign statistics"""
    service = CampaignService(db)
    stats = await service.get_campaign_stats(campaign_id)
    
    if not stats:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Campaign not found")
    
    return stats


@router.get("/schedule/{target_date}/{target_hour}")
async def get_ad_schedule(
    target_date: date,
    target_hour: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate ad schedule for a specific hour"""
    service = CampaignService(db)
    schedule = await service.generate_ad_schedule(target_date, target_hour)
    
    return schedule
