"""
Copy Service for audio asset and script management
"""

from typing import List, Optional
from datetime import datetime, timedelta, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.models.copy import Copy
import structlog

logger = structlog.get_logger()


class CopyService:
    """Service for managing copy and audio assets"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def check_expiring(self, days_ahead: int = 30) -> List[Copy]:
        """Find copy expiring soon"""
        cutoff_date = datetime.now(timezone.utc) + timedelta(days=days_ahead)
        
        result = await self.db.execute(
            select(Copy).where(
                and_(
                    Copy.expires_at.isnot(None),
                    Copy.expires_at <= cutoff_date,
                    Copy.active == True
                )
            ).order_by(Copy.expires_at)
        )
        
        return result.scalars().all()
    
    async def create_version(self, copy_id: int) -> Copy:
        """Create a new version of copy"""
        result = await self.db.execute(select(Copy).where(Copy.id == copy_id))
        original = result.scalar_one_or_none()
        
        if not original:
            raise ValueError("Copy not found")
        
        # Create new version
        new_version = Copy(
            order_id=original.order_id,
            advertiser_id=original.advertiser_id,
            title=f"{original.title} (v{original.version + 1})",
            script_text=original.script_text,
            audio_file_path=original.audio_file_path,
            audio_file_url=original.audio_file_url,
            version=original.version + 1,
            expires_at=original.expires_at,
            active=True
        )
        
        self.db.add(new_version)
        await self.db.commit()
        await self.db.refresh(new_version)
        
        logger.info("Copy version created", original_id=copy_id, new_version_id=new_version.id)
        
        return new_version
    
    async def link_to_order(self, copy_id: int, order_id: int) -> bool:
        """Automatically link copy to order"""
        result = await self.db.execute(select(Copy).where(Copy.id == copy_id))
        copy_item = result.scalar_one_or_none()
        
        if not copy_item:
            return False
        
        copy_item.order_id = order_id
        await self.db.commit()
        await self.db.refresh(copy_item)
        
        logger.info("Copy linked to order", copy_id=copy_id, order_id=order_id)
        
        return True
    
    async def search_archive(self, search_term: str, limit: int = 50) -> List[Copy]:
        """Search copy archive"""
        result = await self.db.execute(
            select(Copy).where(
                Copy.title.ilike(f"%{search_term}%")
            ).limit(limit).order_by(Copy.created_at.desc())
        )
        
        return result.scalars().all()

