"""
LiveReadService for managing live read scripts and performance tracking
"""

from typing import List, Optional, Dict, Any
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_
from backend.models.live_read import LiveRead
from backend.models.copy import Copy
from backend.models.order import Order
from backend.models.makegood import Makegood
import structlog
import json

logger = structlog.get_logger()


class LiveReadService:
    """Service for managing live reads"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def create_live_read(
        self,
        script_text: str,
        copy_id: Optional[int] = None,
        order_id: Optional[int] = None,
        advertiser_id: Optional[int] = None,
        script_title: Optional[str] = None,
        scheduled_time: Optional[datetime] = None,
        scheduled_date: Optional[datetime] = None,
        notes: Optional[str] = None
    ) -> LiveRead:
        """Create a new live read"""
        live_read = LiveRead(
            copy_id=copy_id,
            order_id=order_id,
            advertiser_id=advertiser_id,
            script_text=script_text,
            script_title=script_title,
            scheduled_time=scheduled_time,
            scheduled_date=scheduled_date,
            status="scheduled",
            notes=notes
        )
        
        self.db.add(live_read)
        await self.db.commit()
        await self.db.refresh(live_read)
        
        logger.info("Live read created", live_read_id=live_read.id)
        return live_read
    
    async def get_live_read(self, live_read_id: int) -> Optional[LiveRead]:
        """Get a live read by ID"""
        result = await self.db.execute(select(LiveRead).where(LiveRead.id == live_read_id))
        return result.scalar_one_or_none()
    
    async def get_live_reads(
        self,
        copy_id: Optional[int] = None,
        order_id: Optional[int] = None,
        advertiser_id: Optional[int] = None,
        status: Optional[str] = None,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None
    ) -> List[LiveRead]:
        """Get live reads with optional filtering"""
        query = select(LiveRead)
        
        if copy_id:
            query = query.where(LiveRead.copy_id == copy_id)
        if order_id:
            query = query.where(LiveRead.order_id == order_id)
        if advertiser_id:
            query = query.where(LiveRead.advertiser_id == advertiser_id)
        if status:
            query = query.where(LiveRead.status == status)
        if start_date:
            query = query.where(LiveRead.scheduled_time >= start_date)
        if end_date:
            query = query.where(LiveRead.scheduled_time <= end_date)
        
        query = query.order_by(LiveRead.scheduled_time.desc())
        result = await self.db.execute(query)
        return list(result.scalars().all())
    
    async def confirm_performance(
        self,
        live_read_id: int,
        performed_by: int,
        performed_time: Optional[datetime] = None,
        proof_data: Optional[Dict[str, Any]] = None
    ) -> LiveRead:
        """Confirm that a live read was performed"""
        live_read = await self.get_live_read(live_read_id)
        if not live_read:
            raise ValueError(f"Live read with id {live_read_id} not found")
        
        live_read.performed_by = performed_by
        live_read.performed_time = performed_time or datetime.now(timezone.utc)
        live_read.confirmed = True
        live_read.confirmation_timestamp = datetime.now(timezone.utc)
        live_read.status = "performed"
        
        # Create proof of performance record
        proof_record = {
            "performed_at": live_read.performed_time.isoformat(),
            "performed_by": performed_by,
            "confirmed_at": live_read.confirmation_timestamp.isoformat(),
            "script_title": live_read.script_title,
            "additional_data": proof_data or {}
        }
        live_read.proof_of_performance = json.dumps(proof_record)
        
        await self.db.commit()
        await self.db.refresh(live_read)
        
        logger.info("Live read confirmed", live_read_id=live_read_id, performed_by=performed_by)
        return live_read
    
    async def mark_missed(
        self,
        live_read_id: int,
        create_makegood: bool = True
    ) -> LiveRead:
        """Mark a live read as missed"""
        live_read = await self.get_live_read(live_read_id)
        if not live_read:
            raise ValueError(f"Live read with id {live_read_id} not found")
        
        live_read.status = "missed"
        live_read.makegood_required = create_makegood
        
        # Create makegood if requested
        if create_makegood and live_read.order_id:
            makegood = Makegood(
                original_spot_id=None,  # Live reads don't have spots
                makegood_spot_id=None,
                campaign_id=None,
                reason=f"Missed live read: {live_read.script_title or 'Untitled'}",
                status="pending"
            )
            self.db.add(makegood)
            await self.db.flush()
            live_read.makegood_id = makegood.id
        
        await self.db.commit()
        await self.db.refresh(live_read)
        
        logger.info("Live read marked as missed", live_read_id=live_read_id, makegood_created=create_makegood)
        return live_read
    
    async def get_scheduled_live_reads(
        self,
        start_date: datetime,
        end_date: datetime
    ) -> List[LiveRead]:
        """Get scheduled live reads in a date range"""
        result = await self.db.execute(
            select(LiveRead).where(
                and_(
                    LiveRead.scheduled_time >= start_date,
                    LiveRead.scheduled_time <= end_date,
                    LiveRead.status == "scheduled"
                )
            ).order_by(LiveRead.scheduled_time)
        )
        return list(result.scalars().all())
    
    async def get_performed_live_reads(
        self,
        start_date: datetime,
        end_date: datetime
    ) -> List[LiveRead]:
        """Get performed live reads in a date range"""
        result = await self.db.execute(
            select(LiveRead).where(
                and_(
                    LiveRead.performed_time >= start_date,
                    LiveRead.performed_time <= end_date,
                    LiveRead.status == "performed"
                )
            ).order_by(LiveRead.performed_time.desc())
        )
        return list(result.scalars().all())
    
    async def get_missed_live_reads(
        self,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None
    ) -> List[LiveRead]:
        """Get missed live reads"""
        query = select(LiveRead).where(LiveRead.status == "missed")
        
        if start_date:
            query = query.where(LiveRead.scheduled_time >= start_date)
        if end_date:
            query = query.where(LiveRead.scheduled_time <= end_date)
        
        query = query.order_by(LiveRead.scheduled_time.desc())
        result = await self.db.execute(query)
        return list(result.scalars().all())
    
    async def update_live_read(
        self,
        live_read_id: int,
        script_text: Optional[str] = None,
        script_title: Optional[str] = None,
        scheduled_time: Optional[datetime] = None,
        scheduled_date: Optional[datetime] = None,
        notes: Optional[str] = None
    ) -> LiveRead:
        """Update a live read"""
        live_read = await self.get_live_read(live_read_id)
        if not live_read:
            raise ValueError(f"Live read with id {live_read_id} not found")
        
        if script_text is not None:
            live_read.script_text = script_text
        if script_title is not None:
            live_read.script_title = script_title
        if scheduled_time is not None:
            live_read.scheduled_time = scheduled_time
        if scheduled_date is not None:
            live_read.scheduled_date = scheduled_date
        if notes is not None:
            live_read.notes = notes
        
        await self.db.commit()
        await self.db.refresh(live_read)
        
        logger.info("Live read updated", live_read_id=live_read_id)
        return live_read
    
    async def delete_live_read(self, live_read_id: int) -> bool:
        """Delete a live read"""
        live_read = await self.get_live_read(live_read_id)
        if not live_read:
            raise ValueError(f"Live read with id {live_read_id} not found")
        
        await self.db.delete(live_read)
        await self.db.commit()
        
        logger.info("Live read deleted", live_read_id=live_read_id)
        return True

