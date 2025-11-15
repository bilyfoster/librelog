"""
PoliticalComplianceService for tracking political ads and FCC compliance
"""

from typing import List, Optional, Dict, Any
from datetime import datetime, timezone, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_
from backend.models.political_record import PoliticalRecord
from backend.models.copy import Copy
import structlog

logger = structlog.get_logger()


class PoliticalComplianceService:
    """Service for managing political compliance"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def create_political_record(
        self,
        copy_id: Optional[int] = None,
        order_id: Optional[int] = None,
        advertiser_id: Optional[int] = None,
        advertiser_category: str = "political",
        sponsor_name: str = "",
        sponsor_id: Optional[str] = None,
        office_sought: Optional[str] = None,
        disclaimers_required: Optional[str] = None,
        political_window_start: Optional[datetime] = None,
        political_window_end: Optional[datetime] = None,
        notes: Optional[str] = None
    ) -> PoliticalRecord:
        """Create a new political record"""
        # Mark copy as political
        if copy_id:
            copy_result = await self.db.execute(select(Copy).where(Copy.id == copy_id))
            copy = copy_result.scalar_one_or_none()
            if copy:
                copy.political_flag = True
        
        record = PoliticalRecord(
            copy_id=copy_id,
            order_id=order_id,
            advertiser_id=advertiser_id,
            advertiser_category=advertiser_category,
            sponsor_name=sponsor_name,
            sponsor_id=sponsor_id,
            office_sought=office_sought,
            disclaimers_required=disclaimers_required,
            disclaimers_included=False,
            compliance_status="pending",
            no_substitution_allowed=True,
            political_window_start=political_window_start,
            political_window_end=political_window_end,
            no_preemptions=True,
            priority_scheduling=True,
            notes=notes
        )
        
        self.db.add(record)
        await self.db.commit()
        await self.db.refresh(record)
        
        logger.info("Political record created", record_id=record.id, sponsor_name=sponsor_name)
        return record
    
    async def get_political_record(self, record_id: int) -> Optional[PoliticalRecord]:
        """Get a political record by ID"""
        result = await self.db.execute(select(PoliticalRecord).where(PoliticalRecord.id == record_id))
        return result.scalar_one_or_none()
    
    async def get_political_records(
        self,
        copy_id: Optional[int] = None,
        order_id: Optional[int] = None,
        advertiser_id: Optional[int] = None,
        compliance_status: Optional[str] = None,
        archived: Optional[bool] = None
    ) -> List[PoliticalRecord]:
        """Get political records with optional filtering"""
        query = select(PoliticalRecord)
        
        if copy_id:
            query = query.where(PoliticalRecord.copy_id == copy_id)
        if order_id:
            query = query.where(PoliticalRecord.order_id == order_id)
        if advertiser_id:
            query = query.where(PoliticalRecord.advertiser_id == advertiser_id)
        if compliance_status:
            query = query.where(PoliticalRecord.compliance_status == compliance_status)
        if archived is not None:
            query = query.where(PoliticalRecord.archived == archived)
        
        query = query.order_by(PoliticalRecord.created_at.desc())
        result = await self.db.execute(query)
        return list(result.scalars().all())
    
    async def update_compliance_status(
        self,
        record_id: int,
        compliance_status: str,
        disclaimers_included: Optional[bool] = None
    ) -> PoliticalRecord:
        """Update compliance status"""
        record = await self.get_political_record(record_id)
        if not record:
            raise ValueError(f"Political record with id {record_id} not found")
        
        record.compliance_status = compliance_status
        if disclaimers_included is not None:
            record.disclaimers_included = disclaimers_included
        
        await self.db.commit()
        await self.db.refresh(record)
        
        logger.info("Compliance status updated", record_id=record_id, status=compliance_status)
        return record
    
    async def archive_political_record(
        self,
        record_id: int,
        archive_location: Optional[str] = None
    ) -> PoliticalRecord:
        """Archive a political record (FCC requires 2-year retention)"""
        record = await self.get_political_record(record_id)
        if not record:
            raise ValueError(f"Political record with id {record_id} not found")
        
        record.archived = True
        record.archive_date = datetime.now(timezone.utc)
        if archive_location:
            record.archive_location = archive_location
        
        await self.db.commit()
        await self.db.refresh(record)
        
        logger.info("Political record archived", record_id=record_id)
        return record
    
    async def get_records_for_archival(self, years_old: int = 2) -> List[PoliticalRecord]:
        """Get political records that are old enough to archive"""
        cutoff_date = datetime.now(timezone.utc) - timedelta(days=years_old * 365)
        
        result = await self.db.execute(
            select(PoliticalRecord).where(
                and_(
                    PoliticalRecord.created_at <= cutoff_date,
                    PoliticalRecord.archived == False
                )
            )
        )
        return list(result.scalars().all())
    
    async def enforce_no_substitution(self, copy_id: int) -> bool:
        """Check if substitution is allowed for a copy (political ads cannot be substituted)"""
        result = await self.db.execute(
            select(PoliticalRecord).where(
                and_(
                    PoliticalRecord.copy_id == copy_id,
                    PoliticalRecord.no_substitution_allowed == True
                )
            )
        )
        record = result.scalar_one_or_none()
        return record is not None
    
    async def generate_fcc_compliance_log(
        self,
        start_date: datetime,
        end_date: datetime
    ) -> List[Dict[str, Any]]:
        """Generate FCC compliance log for political ads"""
        result = await self.db.execute(
            select(PoliticalRecord).where(
                and_(
                    PoliticalRecord.created_at >= start_date,
                    PoliticalRecord.created_at <= end_date
                )
            ).order_by(PoliticalRecord.created_at)
        )
        records = result.scalars().all()
        
        compliance_log = []
        for record in records:
            compliance_log.append({
                "id": record.id,
                "sponsor_name": record.sponsor_name,
                "sponsor_id": record.sponsor_id,
                "office_sought": record.office_sought,
                "advertiser_category": record.advertiser_category,
                "disclaimers_required": record.disclaimers_required,
                "disclaimers_included": record.disclaimers_included,
                "compliance_status": record.compliance_status,
                "created_at": record.created_at.isoformat() if record.created_at else None,
                "copy_id": record.copy_id,
                "order_id": record.order_id
            })
        
        return compliance_log

