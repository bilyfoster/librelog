"""
Spot Scheduler service for traffic scheduling
"""

from typing import List, Dict, Any, Optional
from uuid import UUID
from datetime import date, datetime, time, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_
from backend.models.spot import Spot, SpotStatus, BreakPosition, Daypart as SpotDaypart
from backend.models.order import Order, RateType
from backend.models.daypart import Daypart
from backend.models.break_structure import BreakStructure
from backend.models.daily_log import DailyLog
import structlog
import random

logger = structlog.get_logger()


class SpotScheduler:
    """Service for scheduling spots with various scheduling rules"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def schedule_ros_spots(self, order: Order, start_date: date, end_date: date, station_id: UUID) -> List[Spot]:
        """Schedule spots using Run-of-Schedule logic"""
        spots = []
        current_date = start_date
        
        # Get total spots to schedule
        total_spots = order.total_spots
        days_in_range = (end_date - start_date).days + 1
        spots_per_day = max(1, total_spots // days_in_range)
        
        spot_count = 0
        while current_date <= end_date and spot_count < total_spots:
            # Schedule spots for this day
            for _ in range(min(spots_per_day, total_spots - spot_count)):
                # Random time within business hours (6am-11pm)
                hour = random.randint(6, 22)
                minute = random.randint(0, 59)
                scheduled_time = f"{hour:02d}:{minute:02d}:00"
                
                # Get spot length (use first from array or default 30)
                spot_length = order.spot_lengths[0] if order.spot_lengths else 30
                
                spot = Spot(
                    order_id=order.id,
                    campaign_id=order.campaign_id,
                    station_id=station_id,
                    scheduled_date=current_date,
                    scheduled_time=scheduled_time,
                    spot_length=spot_length,
                    status=SpotStatus.SCHEDULED
                )
                spots.append(spot)
                spot_count += 1
            
            current_date += timedelta(days=1)
        
        return spots
    
    async def schedule_daypart_spots(
        self,
        order: Order,
        start_date: date,
        end_date: date,
        station_id: UUID,
        daypart_name: Optional[str] = None
    ) -> List[Spot]:
        """Schedule spots based on daypart restrictions"""
        spots = []
        
        # Get daypart if specified
        daypart = None
        if daypart_name:
            result = await self.db.execute(
                select(Daypart).where(Daypart.name == daypart_name, Daypart.active == True)
            )
            daypart = result.scalar_one_or_none()
        
        if not daypart:
            # Use default daypart (Morning Drive)
            result = await self.db.execute(
                select(Daypart).where(Daypart.name.ilike("%morning%"), Daypart.active == True).limit(1)
            )
            daypart = result.scalar_one_or_none()
        
        if not daypart:
            # Fallback to ROS if no daypart found
            return await self.schedule_ros_spots(order, start_date, end_date, station_id)
        
        # Schedule within daypart time range
        current_date = start_date
        total_spots = order.total_spots
        days_in_range = (end_date - start_date).days + 1
        spots_per_day = max(1, total_spots // days_in_range)
        
        spot_count = 0
        while current_date <= end_date and spot_count < total_spots:
            # Get daypart time range
            start_hour = daypart.start_time.hour
            end_hour = daypart.end_time.hour
            
            for _ in range(min(spots_per_day, total_spots - spot_count)):
                hour = random.randint(start_hour, end_hour - 1)
                minute = random.randint(0, 59)
                scheduled_time = f"{hour:02d}:{minute:02d}:00"
                
                spot_length = order.spot_lengths[0] if order.spot_lengths else 30
                
                # Determine daypart enum
                spot_daypart = None
                if start_hour >= 6 and end_hour <= 10:
                    spot_daypart = SpotDaypart.MORNING_DRIVE
                elif start_hour >= 15 and end_hour <= 19:
                    spot_daypart = SpotDaypart.AFTERNOON_DRIVE
                elif start_hour >= 19 or end_hour <= 6:
                    spot_daypart = SpotDaypart.EVENING
                else:
                    spot_daypart = SpotDaypart.MIDDAY
                
                spot = Spot(
                    order_id=order.id,
                    campaign_id=order.campaign_id,
                    station_id=station_id,
                    scheduled_date=current_date,
                    scheduled_time=scheduled_time,
                    spot_length=spot_length,
                    daypart=spot_daypart,
                    status=SpotStatus.SCHEDULED
                )
                spots.append(spot)
                spot_count += 1
            
            current_date += timedelta(days=1)
        
        return spots
    
    async def schedule_fixed_time_spots(
        self,
        order: Order,
        start_date: date,
        end_date: date,
        station_id: UUID,
        fixed_times: List[str]  # List of HH:MM:SS times
    ) -> List[Spot]:
        """Schedule spots at fixed times"""
        spots = []
        current_date = start_date
        
        spot_length = order.spot_lengths[0] if order.spot_lengths else 30
        
        while current_date <= end_date:
            for fixed_time in fixed_times:
                spot = Spot(
                    order_id=order.id,
                    campaign_id=order.campaign_id,
                    station_id=station_id,
                    scheduled_date=current_date,
                    scheduled_time=fixed_time,
                    spot_length=spot_length,
                    status=SpotStatus.SCHEDULED
                )
                spots.append(spot)
            
            current_date += timedelta(days=1)
        
        return spots
    
    async def schedule_alternating_weeks(
        self,
        order: Order,
        start_date: date,
        end_date: date,
        station_id: UUID,
        week_offset: int = 0  # 0 for first week, 1 for second week
    ) -> List[Spot]:
        """Schedule spots on alternating weeks"""
        spots = []
        current_date = start_date
        
        # Calculate which week we're in
        days_since_start = (current_date - start_date).days
        current_week = (days_since_start // 7) % 2
        
        if current_week != week_offset:
            # Skip this week
            return spots
        
        # Schedule using ROS logic but only on matching weeks
        return await self.schedule_ros_spots(order, start_date, end_date, station_id)
    
    async def detect_conflicts(self, log_date: date, station_id: UUID) -> List[Dict[str, Any]]:
        """Detect scheduling conflicts for a date and station"""
        conflicts = []
        
        # Get all spots for this date and station
        result = await self.db.execute(
            select(Spot).where(
                and_(
                    Spot.scheduled_date == log_date,
                    Spot.station_id == station_id,
                    Spot.status == SpotStatus.SCHEDULED
                )
            ).order_by(Spot.scheduled_time)
        )
        spots = result.scalars().all()
        
        # Check for overlapping times
        for i, spot1 in enumerate(spots):
            for spot2 in spots[i+1:]:
                time1 = datetime.combine(spot1.scheduled_date, time.fromisoformat(spot1.scheduled_time))
                end1 = time1 + timedelta(seconds=spot1.spot_length)
                time2 = datetime.combine(spot2.scheduled_date, time.fromisoformat(spot2.scheduled_time))
                
                # Check if times overlap
                if time1 <= time2 < end1 or time2 <= time1 < (time2 + timedelta(seconds=spot2.spot_length)):
                    conflicts.append({
                        "type": "overlap",
                        "spot1_id": spot1.id,
                        "spot2_id": spot2.id,
                        "time": spot1.scheduled_time,
                        "message": f"Spots {spot1.id} and {spot2.id} overlap at {spot1.scheduled_time}"
                    })
        
        return conflicts
    
    async def calculate_avails(self, log_date: date, station_id: UUID) -> Dict[str, Any]:
        """Calculate available inventory for a date and station"""
        # Get all spots scheduled for this date and station
        result = await self.db.execute(
            select(Spot).where(
                and_(
                    Spot.scheduled_date == log_date,
                    Spot.station_id == station_id
                )
            )
        )
        spots = result.scalars().all()
        
        # Calculate by hour
        hourly_avails = {}
        for hour in range(24):
            hour_spots = [s for s in spots if int(s.scheduled_time.split(':')[0]) == hour]
            total_seconds = sum(s.spot_length for s in hour_spots)
            available_seconds = 3600 - total_seconds  # 1 hour = 3600 seconds
            
            hourly_avails[f"{hour:02d}:00"] = {
                "booked_seconds": total_seconds,
                "available_seconds": max(0, available_seconds),
                "booked_percentage": (total_seconds / 3600) * 100,
                "spot_count": len(hour_spots)
            }
        
        return {
            "date": log_date.isoformat(),
            "hourly_avails": hourly_avails,
            "total_spots": len(spots)
        }
    
    async def check_oversell(self, log_date: date, station_id: UUID) -> List[Dict[str, Any]]:
        """Check for oversell warnings"""
        warnings = []
        avails = await self.calculate_avails(log_date, station_id)
        
        for hour, data in avails["hourly_avails"].items():
            if data["booked_percentage"] > 100:
                warnings.append({
                    "type": "oversell",
                    "hour": hour,
                    "booked_percentage": data["booked_percentage"],
                    "message": f"Hour {hour} is oversold by {data['booked_percentage'] - 100:.1f}%"
                })
        
        return warnings

