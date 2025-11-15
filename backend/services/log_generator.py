"""
Log generation engine - core algorithm for building radio logs
"""

from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime, date, time, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.models.clock_template import ClockTemplate
from backend.models.campaign import Campaign
from backend.models.track import Track
from backend.models.voice_track import VoiceTrack
from backend.models.daily_log import DailyLog
from backend.services.campaign_service import CampaignService
from backend.services.clock_service import ClockTemplateService
from backend.integrations.libretime_client import libretime_client
import structlog
import json
import random

logger = structlog.get_logger()


class LogGenerator:
    """Core log generation engine"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        self.campaign_service = CampaignService(db)
        self.clock_service = ClockTemplateService(db)
    
    async def generate_daily_log(
        self,
        target_date: date,
        clock_template_id: int,
        user_id: int
    ) -> Dict[str, Any]:
        """Generate a complete daily log for a specific date"""
        
        # Get clock template
        clock_template = await self.clock_service.get_template(clock_template_id)
        if not clock_template:
            raise ValueError("Clock template not found")
        
        # Generate hourly logs
        hourly_logs = {}
        total_duration = 0
        
        for hour in range(24):
            hour_str = f"{hour:02d}:00"
            hour_log = await self._generate_hour_log(target_date, hour_str, clock_template)
            hourly_logs[hour_str] = hour_log
            total_duration += hour_log.get("total_duration", 0)
        
        # Create daily log entry
        daily_log_data = {
            "date": target_date.isoformat(),
            "clock_template_id": clock_template_id,
            "clock_template_name": clock_template.name,
            "hourly_logs": hourly_logs,
            "total_duration": total_duration,
            "generated_at": datetime.now().isoformat(),
            "generated_by": user_id
        }
        
        # Save to database
        daily_log = DailyLog(
            date=target_date,
            generated_by=user_id,
            json_data=daily_log_data,
            published=False
        )
        
        self.db.add(daily_log)
        await self.db.commit()
        await self.db.refresh(daily_log)
        
        logger.info("Daily log generated", log_id=daily_log.id, date=target_date)
        
        return {
            "log_id": daily_log.id,
            "date": target_date.isoformat(),
            "total_duration": total_duration,
            "hourly_logs": hourly_logs,
            "message": "Daily log generated successfully"
        }
    
    async def _generate_hour_log(
        self,
        target_date: date,
        hour: str,
        clock_template: ClockTemplate
    ) -> Dict[str, Any]:
        """Generate log for a specific hour"""
        
        layout = clock_template.json_layout
        elements = layout.get("elements", [])
        
        hour_log = {
            "hour": hour,
            "elements": [],
            "total_duration": 0,
            "timeline": []
        }
        
        current_time = 0
        
        for element_config in elements:
            element_type = element_config.get("type")
            count = element_config.get("count", 1)
            fallback = element_config.get("fallback")
            
            # Generate elements for this configuration
            for i in range(count):
                element = await self._select_element(
                    element_type=element_type,
                    target_date=target_date,
                    target_hour=hour,
                    fallback=fallback
                )
                
                if element:
                    element_log = {
                        "type": element_type,
                        "title": element.get("title", ""),
                        "artist": element.get("artist", ""),
                        "duration": element.get("duration", 180),
                        "start_time": current_time,
                        "end_time": current_time + element.get("duration", 180),
                        "file_path": element.get("file_path", ""),
                        "fallback_used": element.get("fallback_used", False)
                    }
                    
                    hour_log["elements"].append(element_log)
                    hour_log["total_duration"] += element_log["duration"]
                    
                    # Add to timeline
                    hour_log["timeline"].append({
                        "time": f"{current_time // 60:02d}:{current_time % 60:02d}",
                        "element": element_log
                    })
                    
                    current_time += element_log["duration"]
        
        return hour_log
    
    async def _select_element(
        self,
        element_type: str,
        target_date: date,
        target_hour: str,
        fallback: Optional[str] = None
    ) -> Optional[Dict[str, Any]]:
        """Select appropriate element based on type and availability"""
        
        if element_type == "MUS":
            return await self._select_music_track()
        elif element_type == "ADV":
            return await self._select_advertisement(target_date, target_hour)
        elif element_type == "PSA":
            return await self._select_psa()
        elif element_type == "LIN":
            return await self._select_liner()
        elif element_type == "INT":
            return await self._select_interview()
        elif element_type == "PRO":
            return await self._select_promo()
        elif element_type == "BED":
            return await self._select_bed()
        else:
            return None
    
    async def _select_music_track(self) -> Optional[Dict[str, Any]]:
        """Select a music track"""
        result = await self.db.execute(
            select(Track).where(
                and_(
                    Track.type == "MUS",
                    Track.filepath.isnot(None)
                )
            ).order_by(Track.last_played.asc().nullsfirst())
        )
        tracks = result.scalars().all()
        
        if tracks:
            track = random.choice(tracks)
            return {
                "title": track.title,
                "artist": track.artist,
                "duration": track.duration or 180,
                "file_path": track.filepath,
                "media_id": int(track.libretime_id) if track.libretime_id else None,
                "libretime_id": int(track.libretime_id) if track.libretime_id else None,
                "fallback_used": False
            }
        
        return None
    
    async def _select_advertisement(
        self,
        target_date: date,
        target_hour: str
    ) -> Optional[Dict[str, Any]]:
        """Select advertisement using campaign service"""
        try:
            schedule = await self.campaign_service.generate_ad_schedule(target_date, target_hour)
            ads = schedule.get("ads", [])
            
            # Find first ADV ad
            for ad in ads:
                if ad["type"] == "ADV":
                    return {
                        "title": ad["campaign_name"],
                        "artist": ad["advertiser"],
                        "duration": ad["duration"],
                        "file_path": ad["file_path"],
                        "fallback_used": ad["fallback"]
                    }
            
            # If no ADV ads, try PRO
            for ad in ads:
                if ad["type"] == "PRO":
                    return {
                        "title": ad["campaign_name"],
                        "artist": ad["advertiser"],
                        "duration": ad["duration"],
                        "file_path": ad["file_path"],
                        "fallback_used": ad["fallback"]
                    }
            
            # If no PRO ads, try PSA
            for ad in ads:
                if ad["type"] == "PSA":
                    return {
                        "title": ad["campaign_name"],
                        "artist": ad["advertiser"],
                        "duration": ad["duration"],
                        "file_path": ad["file_path"],
                        "fallback_used": ad["fallback"]
                    }
            
        except Exception as e:
            logger.error("Failed to select advertisement", error=str(e))
        
        return None
    
    async def _select_psa(self) -> Optional[Dict[str, Any]]:
        """Select a PSA track"""
        result = await self.db.execute(
            select(Track).where(Track.type == "PSA").order_by(Track.last_played.asc().nullsfirst())
        )
        tracks = result.scalars().all()
        
        if tracks:
            track = random.choice(tracks)
            return {
                "title": track.title,
                "artist": track.artist or "PSA",
                "duration": track.duration or 45,
                "file_path": track.filepath,
                "fallback_used": False
            }
        
        return None
    
    async def _select_liner(self) -> Optional[Dict[str, Any]]:
        """Select a liner"""
        result = await self.db.execute(
            select(Track).where(Track.type == "LIN").order_by(Track.last_played.asc().nullsfirst())
        )
        tracks = result.scalars().all()
        
        if tracks:
            track = random.choice(tracks)
            return {
                "title": track.title,
                "artist": track.artist or "Station ID",
                "duration": track.duration or 30,
                "file_path": track.filepath,
                "fallback_used": False
            }
        
        return None
    
    async def _select_interview(self) -> Optional[Dict[str, Any]]:
        """Select an interview segment"""
        result = await self.db.execute(
            select(Track).where(Track.type == "INT").order_by(Track.last_played.asc().nullsfirst())
        )
        tracks = result.scalars().all()
        
        if tracks:
            track = random.choice(tracks)
            return {
                "title": track.title,
                "artist": track.artist or "Interview",
                "duration": track.duration or 300,
                "file_path": track.filepath,
                "fallback_used": False
            }
        
        return None
    
    async def _select_promo(self) -> Optional[Dict[str, Any]]:
        """Select a promo"""
        result = await self.db.execute(
            select(Track).where(Track.type == "PRO").order_by(Track.last_played.asc().nullsfirst())
        )
        tracks = result.scalars().all()
        
        if tracks:
            track = random.choice(tracks)
            return {
                "title": track.title,
                "artist": track.artist or "Promo",
                "duration": track.duration or 30,
                "file_path": track.filepath,
                "fallback_used": False
            }
        
        return None
    
    async def _select_bed(self) -> Optional[Dict[str, Any]]:
        """Select a bed/music bed"""
        result = await self.db.execute(
            select(Track).where(Track.type == "BED").order_by(Track.last_played.asc().nullsfirst())
        )
        tracks = result.scalars().all()
        
        if tracks:
            track = random.choice(tracks)
            return {
                "title": track.title,
                "artist": track.artist or "Music Bed",
                "duration": track.duration or 15,
                "file_path": track.filepath,
                "fallback_used": False
            }
        
        return None
    
    async def preview_log(
        self,
        target_date: date,
        clock_template_id: int,
        preview_hours: List[str] = None
    ) -> Dict[str, Any]:
        """Generate a preview of the log without saving"""
        
        if preview_hours is None:
            preview_hours = ["06:00", "12:00", "18:00"]  # Default preview hours
        
        clock_template = await self.clock_service.get_template(clock_template_id)
        if not clock_template:
            raise ValueError("Clock template not found")
        
        preview = {
            "date": target_date.isoformat(),
            "clock_template_name": clock_template.name,
            "preview_hours": preview_hours,
            "hourly_previews": {},
            "total_duration": 0
        }
        
        for hour in preview_hours:
            hour_log = await self._generate_hour_log(target_date, hour, clock_template)
            preview["hourly_previews"][hour] = hour_log
            preview["total_duration"] += hour_log.get("total_duration", 0)
        
        return preview
    
    async def publish_log(self, log_id: int) -> bool:
        """Publish a generated log to LibreTime"""
        
        # Get the log
        result = await self.db.execute(
            select(DailyLog).where(DailyLog.id == log_id)
        )
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        try:
            # Convert to LibreTime format
            libretime_entries = await self._convert_to_libretime_format(log.json_data, log.date)
            
            if not libretime_entries:
                logger.warning("No entries to publish", log_id=log_id)
                return False
            
            # Push to LibreTime API
            success = await libretime_client.publish_schedule(log.date, libretime_entries)
            
            if success:
                log.published = True
                await self.db.commit()
                logger.info("Log published to LibreTime", log_id=log_id, date=log.date, entries=len(libretime_entries))
                return True
            else:
                logger.error("LibreTime API returned failure", log_id=log_id)
                return False
            
        except Exception as e:
            logger.error("Failed to publish log", log_id=log_id, error=str(e))
            await self.db.rollback()
            return False
    
    async def _convert_to_libretime_format(self, log_data: Dict[str, Any], target_date: date) -> List[Dict[str, Any]]:
        """Convert LibreLog format to LibreTime replace-day format"""
        
        entries = []
        log_date_str = target_date.isoformat()
        
        for hour_str, hour_data in log_data.get("hourly_logs", {}).items():
            # Parse hour (e.g., "00:00" -> hour=0)
            hour = int(hour_str.split(":")[0])
            
            # Get elements for this hour
            elements = hour_data.get("elements", [])
            
            current_second = 0  # Seconds from start of hour
            
            for element in elements:
                # Calculate absolute start time
                element_start_seconds = element.get("start_time", current_second)
                element_duration = element.get("duration", 180)
                
                # Create datetime for this element
                element_start_time = datetime.combine(
                    target_date,
                    time(hour=hour, minute=0, second=0)
                ) + timedelta(seconds=element_start_seconds)
                
                # Get media_id from element or look up by file_path
                media_id = element.get("media_id") or element.get("libretime_id")
                
                if not media_id and element.get("file_path"):
                    # Try to find track by file_path
                    track_result = await self.db.execute(
                        select(Track).where(Track.filepath == element["file_path"])
                    )
                    track = track_result.scalar_one_or_none()
                    if track and track.libretime_id:
                        media_id = int(track.libretime_id)
                
                # Skip if we can't find media_id
                if not media_id:
                    logger.warning(
                        "Skipping element without media_id",
                        element=element.get("title"),
                        file_path=element.get("file_path")
                    )
                    current_second += element_duration
                    continue
                
                # Map element type to LibreTime type
                element_type = element.get("type", "track")
                type_mapping = {
                    "MUS": "track",
                    "ADV": "track",
                    "PSA": "track",
                    "PRO": "track",
                    "LIN": "track",
                    "INT": "track",
                    "BED": "track"
                }
                libretime_type = type_mapping.get(element_type, "track")
                
                # Determine hard_start (typically false, but could be configurable)
                hard_start = element.get("hard_start", False)
                
                entries.append({
                    "start": element_start_time.isoformat(),
                    "media_id": int(media_id),
                    "type": libretime_type,
                    "hard_start": hard_start
                })
                
                current_second += element_duration
        
        return entries
