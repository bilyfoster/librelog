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
from backend.services.voice_track_slot_service import VoiceTrackSlotService
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
        
        # Create voice track slots for this log
        try:
            slot_service = VoiceTrackSlotService(self.db)
            # Extract break structure from clock template or use defaults
            break_structure = self._extract_break_structure(clock_template, hourly_logs)
            slots = await slot_service.create_slots_for_log(daily_log.id, break_structure)
            logger.info("Voice track slots created", log_id=daily_log.id, slot_count=len(slots))
        except Exception as e:
            logger.warning("Failed to create voice track slots", error=str(e), exc_info=True)
            # Don't fail log generation if slot creation fails
        
        logger.info("Daily log generated", log_id=daily_log.id, date=target_date)
        
        return {
            "log_id": daily_log.id,
            "date": target_date.isoformat(),
            "total_duration": total_duration,
            "hourly_logs": hourly_logs,
            "message": "Daily log generated successfully"
        }
    
    def _extract_break_structure(
        self,
        clock_template: ClockTemplate,
        hourly_logs: Dict[str, Any]
    ) -> Dict[int, List[int]]:
        """
        Extract break structure from clock template or hourly logs
        
        Returns:
            Dictionary mapping hour to list of break positions in seconds
        """
        break_structure = {}
        
        # Try to get break structure from clock template
        layout = clock_template.json_layout
        elements = layout.get("elements", [])
        
        # Find break positions (typically spots or voice track slots)
        for hour in range(24):
            hour_str = f"{hour:02d}:00"
            hour_data = hourly_logs.get(hour_str, {})
            hour_elements = hour_data.get("elements", [])
            
            break_positions = []
            cumulative_time = 0
            
            for elem in hour_elements:
                elem_type = elem.get("type")
                # Look for spots or breaks (typically at 15, 30, 45 minutes)
                if elem_type in ["spot", "break", "voice_track"]:
                    break_positions.append(cumulative_time)
                cumulative_time += elem.get("duration", 0)
            
            # If no breaks found, use default positions (15, 30, 45 minutes)
            if not break_positions:
                break_positions = [900, 1800, 2700]  # 15, 30, 45 minutes
            
            break_structure[hour] = break_positions
        
        return break_structure
    
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
        scheduled_times = {}  # Track scheduled times for elements
        timing_drift = 0  # Track cumulative timing drift
        
        # First pass: calculate scheduled times for all elements
        for idx, element_config in enumerate(elements):
            element_type = element_config.get("type")
            count = element_config.get("count", 1)
            scheduled_offset = element_config.get("scheduled_time", None)  # Optional scheduled time in seconds
            hard_start = element_config.get("hard_start", False)
            
            for i in range(count):
                element_key = f"{idx}_{i}"
                if scheduled_offset is not None:
                    scheduled_times[element_key] = scheduled_offset
                else:
                    scheduled_times[element_key] = current_time
                    current_time += element_config.get("duration", 180)  # Estimate based on typical duration
        
        # Second pass: generate elements with timing control
        current_time = 0
        for idx, element_config in enumerate(elements):
            element_type = element_config.get("type")
            count = element_config.get("count", 1)
            fallback = element_config.get("fallback")
            hard_start = element_config.get("hard_start", False)
            scheduled_offset = element_config.get("scheduled_time", None)
            
            # Generate elements for this configuration
            for i in range(count):
                element_key = f"{idx}_{i}"
                scheduled_time = scheduled_times.get(element_key, current_time)
                
                # If hard_start is true, adjust current_time to match scheduled time
                if hard_start and scheduled_offset is not None:
                    current_time = scheduled_offset
                    timing_drift = 0  # Reset drift when hard starting
                elif hard_start:
                    # Hard start at scheduled time (top of hour for IDS, etc.)
                    if element_type == "IDS":
                        # IDS can be at top (00:00) or bottom (59:XX) of hour
                        position = element_config.get("position", "top")  # "top" or "bottom"
                        if position == "top":
                            current_time = 0
                        else:
                            # Bottom of hour - will be adjusted later
                            current_time = 3540  # 59:00
                    else:
                        current_time = scheduled_time
                    timing_drift = 0
                else:
                    # Flexible timing: get as close as possible to scheduled time
                    # Adjust for any accumulated drift
                    target_time = scheduled_time - timing_drift
                    if target_time < current_time:
                        # Can't go backwards, start immediately
                        current_time = current_time
                    else:
                        # Adjust to get closer to scheduled time
                        current_time = target_time
                
                element = await self._select_element(
                    element_type=element_type,
                    target_date=target_date,
                    target_hour=hour,
                    fallback=fallback
                )
                
                if element:
                    element_duration = element.get("duration", 180)
                    actual_start = current_time
                    
                    element_log = {
                        "type": element_type,
                        "title": element.get("title", ""),
                        "artist": element.get("artist", ""),
                        "duration": element_duration,
                        "start_time": actual_start,
                        "end_time": actual_start + element_duration,
                        "scheduled_time": scheduled_time,
                        "hard_start": hard_start,
                        "timing_drift": timing_drift,
                        "file_path": element.get("file_path", ""),
                        "fallback_used": element.get("fallback_used", False)
                    }
                    
                    hour_log["elements"].append(element_log)
                    hour_log["total_duration"] += element_duration
                    
                    # Calculate timing drift for next element
                    expected_end = scheduled_time + element_duration
                    actual_end = actual_start + element_duration
                    timing_drift = actual_end - expected_end
                    
                    # Add to timeline
                    hour_log["timeline"].append({
                        "time": f"{actual_start // 60:02d}:{actual_start % 60:02d}",
                        "element": element_log
                    })
                    
                    current_time = actual_end
        
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
        elif element_type == "IDS":
            return await self._select_station_id()
        elif element_type == "NEW":
            return await self._select_news()
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
            # Try to convert libretime_id to int, but handle string IDs
            media_id = None
            if track.libretime_id:
                try:
                    media_id = int(track.libretime_id)
                except (ValueError, TypeError):
                    # If it's not a numeric ID, use None
                    pass
            
            return {
                "title": track.title,
                "artist": track.artist,
                "duration": track.duration or 180,
                "file_path": track.filepath,
                "media_id": media_id,
                "libretime_id": track.libretime_id,
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
    
    async def _select_station_id(self) -> Optional[Dict[str, Any]]:
        """Select a station ID (for top or bottom of hour)"""
        result = await self.db.execute(
            select(Track).where(Track.type == "IDS").order_by(Track.last_played.asc().nullsfirst())
        )
        tracks = result.scalars().all()
        
        if tracks:
            track = random.choice(tracks)
            # Try to convert libretime_id to int, but handle string IDs
            media_id = None
            if track.libretime_id:
                try:
                    media_id = int(track.libretime_id)
                except (ValueError, TypeError):
                    # If it's not a numeric ID, try to extract number or use None
                    # Some libretime_ids might be strings like "LT-IDS-3169"
                    pass
            
            return {
                "title": track.title,
                "artist": track.artist or "Station ID",
                "duration": track.duration or 30,
                "file_path": track.filepath,
                "media_id": media_id,
                "libretime_id": track.libretime_id,
                "fallback_used": False
            }
        
        return None
    
    async def _select_news(self) -> Optional[Dict[str, Any]]:
        """Select a news segment"""
        result = await self.db.execute(
            select(Track).where(Track.type == "NEW").order_by(Track.last_played.asc().nullsfirst())
        )
        tracks = result.scalars().all()
        
        if tracks:
            track = random.choice(tracks)
            # Try to convert libretime_id to int, but handle string IDs
            media_id = None
            if track.libretime_id:
                try:
                    media_id = int(track.libretime_id)
                except (ValueError, TypeError):
                    # If it's not a numeric ID, try to extract number or use None
                    pass
            
            return {
                "title": track.title,
                "artist": track.artist or "News",
                "duration": track.duration or 120,
                "file_path": track.filepath,
                "media_id": media_id,
                "libretime_id": track.libretime_id,
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
                    "IDS": "track",
                    "NEW": "track",
                    "INT": "track",
                    "BED": "track"
                }
                libretime_type = type_mapping.get(element_type, "track")
                
                # Determine hard_start from element configuration
                hard_start = element.get("hard_start", False)
                
                entries.append({
                    "start": element_start_time.isoformat(),
                    "media_id": int(media_id),
                    "type": libretime_type,
                    "hard_start": hard_start
                })
                
                current_second += element_duration
        
        return entries
