"""
Campaign management router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.models.campaign import Campaign
from backend.services.campaign_service import CampaignService
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List
from datetime import date

router = APIRouter()


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
                "name": c.name,
                "advertiser": c.advertiser,
                "start_date": c.start_date,
                "end_date": c.end_date,
                "priority": c.priority,
                "target_hours": c.target_hours,
                "ad_type": c.ad_type,
                "file_path": c.file_path,
                "active": c.active,
                "created_at": c.created_at,
                "updated_at": c.updated_at
            }
            for c in campaigns
        ],
        "total": len(campaigns)
    }


@router.post("/")
async def create_campaign(
    campaign_data: CampaignCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new campaign"""
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
        
        return {
            "id": campaign.id,
            "name": campaign.name,
            "advertiser": campaign.advertiser,
            "start_date": campaign.start_date,
            "end_date": campaign.end_date,
            "priority": campaign.priority,
            "target_hours": campaign.target_hours,
            "ad_type": campaign.ad_type,
            "file_path": campaign.file_path,
            "active": campaign.active,
            "created_at": campaign.created_at,
            "message": "Campaign created successfully"
        }
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.get("/{campaign_id}")
async def get_campaign(
    campaign_id: int,
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
        "name": campaign.name,
        "advertiser": campaign.advertiser,
        "start_date": campaign.start_date,
        "end_date": campaign.end_date,
        "priority": campaign.priority,
        "target_hours": campaign.target_hours,
        "ad_type": campaign.ad_type,
        "file_path": campaign.file_path,
        "active": campaign.active,
        "created_at": campaign.created_at,
        "updated_at": campaign.updated_at
    }


@router.put("/{campaign_id}")
async def update_campaign(
    campaign_id: int,
    campaign_data: CampaignUpdate,
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
        
        return {
            "id": campaign.id,
            "name": campaign.name,
            "advertiser": campaign.advertiser,
            "start_date": campaign.start_date,
            "end_date": campaign.end_date,
            "priority": campaign.priority,
            "target_hours": campaign.target_hours,
            "ad_type": campaign.ad_type,
            "file_path": campaign.file_path,
            "active": campaign.active,
            "updated_at": campaign.updated_at,
            "message": "Campaign updated successfully"
        }
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.delete("/{campaign_id}")
async def delete_campaign(
    campaign_id: int,
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
    campaign_id: int,
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
