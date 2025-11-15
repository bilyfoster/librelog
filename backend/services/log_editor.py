"""
Log Editor service for manual log editing
"""

from typing import Dict, Any, Optional
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.models.daily_log import DailyLog
from backend.models.spot import Spot
import structlog

logger = structlog.get_logger()


class LogEditor:
    """Service for manual log editing operations"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def add_spot_to_log(self, log_id: int, spot_data: Dict[str, Any]) -> bool:
        """Add a spot to a log manually"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        if log.locked:
            logger.warning("Attempted to edit locked log", log_id=log_id)
            return False
        
        # Update JSON data with new spot
        json_data = log.json_data.copy()
        hourly_logs = json_data.get("hourly_logs", {})
        
        # Extract hour from scheduled time
        scheduled_time = spot_data.get("scheduled_time", "00:00:00")
        hour = int(scheduled_time.split(':')[0])
        hour_key = f"{hour:02d}:00"
        
        if hour_key not in hourly_logs:
            hourly_logs[hour_key] = {"elements": [], "total_duration": 0}
        
        # Add spot to elements
        elements = hourly_logs[hour_key].get("elements", [])
        elements.append({
            "title": spot_data.get("title", "Spot"),
            "type": spot_data.get("type", "ADV"),
            "duration": spot_data.get("spot_length", 30),
            "start_time": int(scheduled_time.split(':')[1]) * 60 + int(scheduled_time.split(':')[2]),
            "media_id": spot_data.get("media_id"),
            "file_path": spot_data.get("file_path"),
        })
        
        hourly_logs[hour_key]["elements"] = elements
        json_data["hourly_logs"] = hourly_logs
        
        log.json_data = json_data
        log.updated_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(log)
        
        logger.info("Spot added to log", log_id=log_id, spot_data=spot_data)
        
        return True
    
    async def move_spot(self, log_id: int, spot_id: int, new_data: Dict[str, Any]) -> bool:
        """Move a spot within a log (drag-drop functionality)"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        if log.locked:
            logger.warning("Attempted to edit locked log", log_id=log_id)
            return False
        
        # Update spot in JSON data
        json_data = log.json_data.copy()
        hourly_logs = json_data.get("hourly_logs", {})
        
        # Find and update the spot
        for hour_key, hour_data in hourly_logs.items():
            elements = hour_data.get("elements", [])
            for i, element in enumerate(elements):
                if element.get("spot_id") == spot_id or element.get("id") == spot_id:
                    # Update element with new data
                    elements[i].update({
                        "start_time": new_data.get("start_time", element.get("start_time")),
                        "duration": new_data.get("duration", element.get("duration")),
                    })
                    break
        
        json_data["hourly_logs"] = hourly_logs
        log.json_data = json_data
        log.updated_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(log)
        
        logger.info("Spot moved in log", log_id=log_id, spot_id=spot_id)
        
        return True
    
    async def remove_spot(self, log_id: int, spot_id: int) -> bool:
        """Remove a spot from a log"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        if log.locked:
            logger.warning("Attempted to edit locked log", log_id=log_id)
            return False
        
        # Remove spot from JSON data
        json_data = log.json_data.copy()
        hourly_logs = json_data.get("hourly_logs", {})
        
        for hour_key, hour_data in hourly_logs.items():
            elements = hour_data.get("elements", [])
            elements = [e for e in elements if e.get("spot_id") != spot_id and e.get("id") != spot_id]
            hourly_logs[hour_key]["elements"] = elements
        
        json_data["hourly_logs"] = hourly_logs
        log.json_data = json_data
        log.updated_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(log)
        
        logger.info("Spot removed from log", log_id=log_id, spot_id=spot_id)
        
        return True
    
    async def lock_log(self, log_id: int, user_id: int) -> bool:
        """Lock a log to prevent further edits"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        log.locked = True
        log.locked_at = datetime.now(timezone.utc)
        log.locked_by = user_id
        
        await self.db.commit()
        await self.db.refresh(log)
        
        logger.info("Log locked", log_id=log_id, user_id=user_id)
        
        return True
    
    async def validate_log(self, log_id: int) -> Dict[str, Any]:
        """Validate a log before export"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return {"valid": False, "errors": ["Log not found"]}
        
        errors = []
        warnings = []
        
        # Check for conflicts
        if log.conflicts:
            errors.extend([c.get("message", "Conflict detected") for c in log.conflicts])
        
        # Check for oversell
        if log.oversell_warnings:
            warnings.extend([w.get("message", "Oversell warning") for w in log.oversell_warnings])
        
        # Check JSON structure
        if not log.json_data.get("hourly_logs"):
            errors.append("Log has no hourly logs")
        
        return {
            "valid": len(errors) == 0,
            "errors": errors,
            "warnings": warnings
        }

