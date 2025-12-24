"""
Timing service for back-timing and show timing calculations
"""

from datetime import datetime, timedelta
from uuid import UUID
from typing import Dict, Any, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.models.daily_log import DailyLog
import structlog

logger = structlog.get_logger()


class TimingService:
    """Service for timing calculations"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def calculate_hour_remaining(
        self,
        log_id: UUID,
        hour: int
    ) -> Dict[str, Any]:
        """
        Calculate time remaining in hour
        
        Args:
            log_id: Daily log ID
            hour: Hour (0-23)
        
        Returns:
            Dictionary with remaining time, total time, etc.
        """
        try:
            # Get log
            result = await self.db.execute(
                select(DailyLog).where(DailyLog.id == log_id)
            )
            log = result.scalar_one_or_none()
            
            if not log:
                return {"error": "Log not found"}
            
            # Get hourly log data
            hourly_logs = log.json_data.get("hourly_logs", {})
            hour_str = f"{hour:02d}:00"
            hour_data = hourly_logs.get(hour_str, {})
            
            # Calculate total duration of scheduled items
            elements = hour_data.get("elements", [])
            total_duration = sum(
                elem.get("duration", 0) for elem in elements
            )
            
            # Hour duration is 3600 seconds
            hour_duration = 3600.0
            remaining = hour_duration - total_duration
            
            return {
                "hour": hour,
                "total_duration": total_duration,
                "hour_duration": hour_duration,
                "remaining": remaining,
                "remaining_formatted": str(timedelta(seconds=int(remaining))),
                "over_under": remaining  # Positive = under, negative = over
            }
            
        except Exception as e:
            logger.error("Hour remaining calculation failed", error=str(e), exc_info=True)
            return {"error": str(e)}
    
    async def calculate_hour_end_time(
        self,
        log_id: UUID,
        hour: int,
        log_date: Optional[datetime] = None
    ) -> Dict[str, Any]:
        """
        Calculate exact ending time of hour
        
        Args:
            log_id: Daily log ID
            hour: Hour (0-23)
            log_date: Optional log date (if None, uses log's date)
        
        Returns:
            Dictionary with end time, etc.
        """
        try:
            # Get log
            result = await self.db.execute(
                select(DailyLog).where(DailyLog.id == log_id)
            )
            log = result.scalar_one_or_none()
            
            if not log:
                return {"error": "Log not found"}
            
            # Use log date or provided date
            if log_date is None:
                log_date = datetime.combine(log.date, datetime.min.time())
            
            # Calculate hour start
            hour_start = log_date.replace(hour=hour, minute=0, second=0, microsecond=0)
            
            # Get hour data
            hourly_logs = log.json_data.get("hourly_logs", {})
            hour_str = f"{hour:02d}:00"
            hour_data = hourly_logs.get(hour_str, {})
            
            # Calculate total duration
            elements = hour_data.get("elements", [])
            total_duration = sum(
                elem.get("duration", 0) for elem in elements
            )
            
            # Calculate end time
            hour_end = hour_start + timedelta(seconds=total_duration)
            
            return {
                "hour": hour,
                "hour_start": hour_start.isoformat(),
                "hour_end": hour_end.isoformat(),
                "total_duration": total_duration,
                "hour_duration": 3600.0,
                "over_under": total_duration - 3600.0
            }
            
        except Exception as e:
            logger.error("Hour end time calculation failed", error=str(e), exc_info=True)
            return {"error": str(e)}
    
    async def check_timing_balance(
        self,
        log_id: UUID,
        hour: int
    ) -> Dict[str, Any]:
        """
        Check timing balance and return warnings
        
        Args:
            log_id: Daily log ID
            hour: Hour (0-23)
        
        Returns:
            Dictionary with balance status, warnings, etc.
        """
        try:
            # Get hour remaining
            remaining_data = await self.calculate_hour_remaining(log_id, hour)
            
            if "error" in remaining_data:
                return remaining_data
            
            remaining = remaining_data["remaining"]
            total_duration = remaining_data["total_duration"]
            
            # Determine status
            if remaining < -60:  # More than 1 minute over
                status = "over"
                severity = "error"
                message = f"Hour is {abs(remaining):.0f} seconds over"
            elif remaining < 0:  # Under 1 minute over
                status = "over"
                severity = "warning"
                message = f"Hour is {abs(remaining):.0f} seconds over"
            elif remaining < 60:  # Less than 1 minute remaining
                status = "tight"
                severity = "warning"
                message = f"Only {remaining:.0f} seconds remaining"
            else:
                status = "ok"
                severity = "info"
                message = f"{remaining:.0f} seconds remaining"
            
            return {
                "hour": hour,
                "status": status,
                "severity": severity,
                "message": message,
                "remaining": remaining,
                "total_duration": total_duration,
                "hour_duration": 3600.0,
                "over_under": remaining,
                "percentage": (total_duration / 3600.0) * 100
            }
            
        except Exception as e:
            logger.error("Timing balance check failed", error=str(e), exc_info=True)
            return {"error": str(e)}

