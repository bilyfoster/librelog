"""
Campaign management service with ad fallback logic
"""

from typing import List, Dict, Any, Optional
from datetime import datetime, date
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_
from sqlalchemy.exc import IntegrityError
from backend.models.campaign import Campaign
from backend.models.track import Track
import structlog
import json

logger = structlog.get_logger()


class CampaignService:
    """Service for managing campaigns and ad fallback logic"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def create_campaign(
        self,
        name: str,
        advertiser: str,
        start_date: date,
        end_date: date,
        priority: int,
        target_hours: List[str],
        ad_type: str,
        file_path: Optional[str] = None,
        user_id: Optional[UUID] = None
    ) -> Campaign:
        """Create a new campaign with validation"""
        # Validate date range
        if start_date >= end_date:
            raise ValueError("Start date must be before end date")
        
        # Validate priority
        if priority < 1 or priority > 10:
            raise ValueError("Priority must be between 1 and 10")
        
        # Validate ad type
        valid_types = ["ADV", "PSA", "PRO"]
        if ad_type not in valid_types:
            raise ValueError(f"Ad type must be one of: {valid_types}")
        
        # Note: Campaign model doesn't have name, target_hours, or ad_type fields
        # These are stored in other ways or not yet implemented
        campaign = Campaign(
            advertiser=advertiser,  # Use advertiser as the identifier (name is not in model)
            start_date=start_date,
            end_date=end_date,
            priority=priority,
            file_url=file_path,  # Use file_url instead of file_path
            active=True
        )
        
        try:
            self.db.add(campaign)
            await self.db.commit()
            await self.db.refresh(campaign)
            
            logger.info("Campaign created", campaign_id=campaign.id, advertiser=advertiser)
            return campaign
            
        except IntegrityError:
            await self.db.rollback()
            raise ValueError("Campaign creation failed - possible duplicate")
    
    async def get_campaign(self, campaign_id: UUID) -> Optional[Campaign]:
        """Get a campaign by ID"""
        result = await self.db.execute(
            select(Campaign).where(Campaign.id == campaign_id)
        )
        return result.scalar_one_or_none()
    
    async def list_campaigns(
        self,
        skip: int = 0,
        limit: int = 100,
        active_only: bool = True,
        date_filter: Optional[date] = None
    ) -> List[Campaign]:
        """List campaigns with optional filtering"""
        query = select(Campaign)
        
        if active_only:
            query = query.where(Campaign.active == True)
        
        if date_filter:
            query = query.where(
                and_(
                    Campaign.start_date <= date_filter,
                    Campaign.end_date >= date_filter
                )
            )
        
        query = query.offset(skip).limit(limit).order_by(Campaign.priority.desc(), Campaign.created_at.desc())
        
        result = await self.db.execute(query)
        return result.scalars().all()
    
    async def update_campaign(
        self,
        campaign_id: UUID,
        name: Optional[str] = None,
        advertiser: Optional[str] = None,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
        priority: Optional[int] = None,
        target_hours: Optional[List[str]] = None,
        ad_type: Optional[str] = None,
        file_path: Optional[str] = None,
        active: Optional[bool] = None
    ) -> Optional[Campaign]:
        """Update a campaign"""
        campaign = await self.get_campaign(campaign_id)
        if not campaign:
            return None
        
        if name is not None:
            campaign.name = name
        if advertiser is not None:
            campaign.advertiser = advertiser
        if start_date is not None:
            campaign.start_date = start_date
        if end_date is not None:
            campaign.end_date = end_date
        if priority is not None:
            campaign.priority = priority
        if target_hours is not None:
            campaign.target_hours = target_hours
        if ad_type is not None:
            campaign.ad_type = ad_type
        if file_path is not None:
            campaign.file_path = file_path
        if active is not None:
            campaign.active = active
        
        try:
            await self.db.commit()
            await self.db.refresh(campaign)
            
            logger.info("Campaign updated", campaign_id=campaign_id)
            return campaign
            
        except IntegrityError:
            await self.db.rollback()
            raise ValueError("Campaign name already exists")
    
    async def delete_campaign(self, campaign_id: UUID) -> bool:
        """Delete a campaign"""
        campaign = await self.get_campaign(campaign_id)
        if not campaign:
            return False
        
        await self.db.delete(campaign)
        await self.db.commit()
        
        logger.info("Campaign deleted", campaign_id=campaign_id)
        return True
    
    async def get_active_campaigns_for_hour(self, target_hour: str, target_date: date) -> List[Campaign]:
        """Get active campaigns for a specific hour and date"""
        result = await self.db.execute(
            select(Campaign).where(
                and_(
                    Campaign.active == True,
                    Campaign.start_date <= target_date,
                    Campaign.end_date >= target_date,
                    Campaign.target_hours.contains([target_hour])
                )
            ).order_by(Campaign.priority.desc())
        )
        return result.scalars().all()
    
    async def generate_ad_schedule(
        self,
        target_date: date,
        target_hour: str
    ) -> Dict[str, Any]:
        """Generate ad schedule for a specific hour using fallback logic"""
        campaigns = await self.get_active_campaigns_for_hour(target_hour, target_date)
        
        # Group campaigns by type
        adv_campaigns = [c for c in campaigns if c.ad_type == "ADV"]
        pro_campaigns = [c for c in campaigns if c.ad_type == "PRO"]
        psa_campaigns = [c for c in campaigns if c.ad_type == "PSA"]
        
        schedule = {
            "date": target_date.isoformat(),
            "hour": target_hour,
            "ads": [],
            "fallback_used": False,
            "total_duration": 0
        }
        
        # Try to fill with advertisements first (priority waterfall)
        for campaign in adv_campaigns:
            if self._can_add_ad(schedule["ads"], campaign):
                ad_slot = {
                    "campaign_id": campaign.id,
                    "campaign_name": campaign.name,
                    "advertiser": campaign.advertiser,
                    "type": campaign.ad_type,
                    "priority": campaign.priority,
                    "file_path": campaign.file_path,
                    "duration": 60,  # Default 60 seconds for ads
                    "fallback": False
                }
                schedule["ads"].append(ad_slot)
                schedule["total_duration"] += ad_slot["duration"]
        
        # If we need more ads, try promos
        if len(schedule["ads"]) < 3:  # Target 3 ads per hour
            for campaign in pro_campaigns:
                if self._can_add_ad(schedule["ads"], campaign):
                    ad_slot = {
                        "campaign_id": campaign.id,
                        "campaign_name": campaign.name,
                        "advertiser": campaign.advertiser,
                        "type": campaign.ad_type,
                        "priority": campaign.priority,
                        "file_path": campaign.file_path,
                        "duration": 30,  # Default 30 seconds for promos
                        "fallback": False
                    }
                    schedule["ads"].append(ad_slot)
                    schedule["total_duration"] += ad_slot["duration"]
        
        # If still need more, use PSAs as fallback
        if len(schedule["ads"]) < 3:
            schedule["fallback_used"] = True
            for campaign in psa_campaigns:
                if self._can_add_ad(schedule["ads"], campaign):
                    ad_slot = {
                        "campaign_id": campaign.id,
                        "campaign_name": campaign.name,
                        "advertiser": campaign.advertiser,
                        "type": campaign.ad_type,
                        "priority": campaign.priority,
                        "file_path": campaign.file_path,
                        "duration": 45,  # Default 45 seconds for PSAs
                        "fallback": True
                    }
                    schedule["ads"].append(ad_slot)
                    schedule["total_duration"] += ad_slot["duration"]
        
        return schedule
    
    def _can_add_ad(self, existing_ads: List[Dict], campaign: Campaign) -> bool:
        """Check if we can add another ad from this campaign"""
        # Don't add more than 2 ads from the same campaign per hour
        campaign_count = sum(1 for ad in existing_ads if ad["campaign_id"] == campaign.id)
        return campaign_count < 2
    
    async def get_campaign_stats(self, campaign_id: UUID) -> Dict[str, Any]:
        """Get statistics for a campaign"""
        campaign = await self.get_campaign(campaign_id)
        if not campaign:
            return {}
        
        # Calculate days active
        days_active = (campaign.end_date - campaign.start_date).days + 1
        
        # Calculate total potential plays
        total_hours = len(campaign.target_hours) * days_active
        max_plays_per_hour = 2  # Maximum ads per hour from same campaign
        total_potential_plays = total_hours * max_plays_per_hour
        
        return {
            "campaign_id": campaign_id,
            "campaign_name": campaign.name,
            "advertiser": campaign.advertiser,
            "start_date": campaign.start_date.isoformat(),
            "end_date": campaign.end_date.isoformat(),
            "days_active": days_active,
            "target_hours": campaign.target_hours,
            "priority": campaign.priority,
            "ad_type": campaign.ad_type,
            "total_potential_plays": total_potential_plays,
            "active": campaign.active
        }
