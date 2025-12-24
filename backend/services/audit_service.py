"""
Audit Service for action logging and revision management
"""

from typing import Dict, Any, Optional, List
from uuid import UUID
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from backend.models.audit_log import AuditLog
from backend.models.log_revision import LogRevision
from backend.models.daily_log import DailyLog
import structlog
import json

logger = structlog.get_logger()


class AuditService:
    """Service for audit logging and revision management"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def log_action(
        self,
        user_id: UUID,
        action: str,
        resource_type: str,
        resource_id: Optional[UUID] = None,
        changes: Optional[Dict[str, Any]] = None,
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None
    ) -> AuditLog:
        """Record an action in the audit log"""
        # Serialize changes dict to JSON string for String field
        details_str = json.dumps(changes) if changes else None
        
        audit_log = AuditLog(
            user_id=user_id,
            action=action,
            resource_type=resource_type,
            resource_id=resource_id,
            details=details_str,  # String field - JSON serialized
            ip_address=ip_address,
            user_agent=user_agent
        )
        
        self.db.add(audit_log)
        await self.db.commit()
        await self.db.refresh(audit_log)
        
        logger.info("Action logged", user_id=user_id, action=action, resource_type=resource_type)
        
        return audit_log
    
    async def get_revision_history(self, log_id: UUID) -> List[LogRevision]:
        """Get revision history for a log"""
        result = await self.db.execute(
            select(LogRevision).where(LogRevision.log_id == log_id)
            .order_by(LogRevision.revision_number.desc())
        )
        
        return result.scalars().all()
    
    async def create_revision(
        self,
        log_id: UUID,
        changed_by: int,
        change_summary: Optional[str] = None
    ) -> LogRevision:
        """Create a new revision snapshot"""
        # Get current log
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            raise ValueError("Log not found")
        
        # Get next revision number
        result = await self.db.execute(
            select(func.max(LogRevision.revision_number))
            .where(LogRevision.log_id == log_id)
        )
        max_revision = result.scalar() or 0
        next_revision = max_revision + 1
        
        # Create revision
        revision = LogRevision(
            log_id=log_id,
            revision_number=next_revision,
            json_data=log.json_data,
            changed_by=changed_by,
            change_summary=change_summary
        )
        
        self.db.add(revision)
        await self.db.commit()
        await self.db.refresh(revision)
        
        logger.info("Revision created", log_id=log_id, revision_number=next_revision)
        
        return revision
    
    async def revert_to_revision(
        self,
        log_id: UUID,
        revision_id: UUID,
        user_id: UUID
    ) -> bool:
        """Revert log to a specific revision"""
        # Get revision
        result = await self.db.execute(
            select(LogRevision).where(
                LogRevision.id == revision_id,
                LogRevision.log_id == log_id
            )
        )
        revision = result.scalar_one_or_none()
        
        if not revision:
            return False
        
        # Get log
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        if log.locked:
            logger.warning("Attempted to revert locked log", log_id=log_id)
            return False
        
        # Create new revision before reverting
        await self.create_revision(log_id, user_id, f"Reverted to revision {revision.revision_number}")
        
        # Restore log data
        log.json_data = revision.json_data
        log.updated_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(log)
        
        # Log the revert action
        await self.log_action(
            user_id=user_id,
            action="REVERT_LOG",
            resource_type="DailyLog",
            resource_id=log_id,
            changes={"reverted_to_revision": revision.revision_number}
        )
        
        logger.info("Log reverted", log_id=log_id, revision_id=revision_id)
        
        return True

