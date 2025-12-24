"""
Voice track slot service for managing break assignments
"""

from typing import List, Optional, Dict, Any
from uuid import UUID
from datetime import date, datetime, timezone, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.models.voice_track_slot import VoiceTrackSlot
from backend.models.daily_log import DailyLog
from backend.models.track import Track
from backend.services.audio_preview_service import AudioPreviewService
import structlog

logger = structlog.get_logger()


class VoiceTrackSlotService:
    """Service for managing voice track slots"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        self.audio_preview = AudioPreviewService()
    
    async def create_slots_for_log(
        self,
        log_id: UUID,
        break_structure: Optional[Dict[int, List[int]]] = None
    ) -> List[VoiceTrackSlot]:
        """
        Create voice track slots for a log based on break structure
        
        Args:
            log_id: Daily log ID
            break_structure: Dictionary mapping hour to list of break positions in seconds
        
        Returns:
            List of created slots
        """
        try:
            # Get log
            result = await self.db.execute(
                select(DailyLog).where(DailyLog.id == log_id)
            )
            log = result.scalar_one_or_none()
            
            if not log:
                raise ValueError("Log not found")
            
            slots = []
            hourly_logs = log.json_data.get("hourly_logs", {})
            
            # Default break structure if not provided
            if break_structure is None:
                # Default: breaks at 15, 30, 45 minutes (900, 1800, 2700 seconds)
                break_structure = {
                    h: [900, 1800, 2700] for h in range(24)
                }
            
            # Create slots for each hour
            for hour in range(24):
                hour_str = f"{hour:02d}:00"
                hour_data = hourly_logs.get(hour_str, {})
                elements = hour_data.get("elements", [])
                
                # Get break positions for this hour
                break_positions = break_structure.get(hour, [])
                
                # Create slots for each break position
                for idx, break_sec in enumerate(break_positions):
                    # Determine break position label (A, B, C, etc.)
                    break_label = chr(65 + idx) if idx < 26 else str(idx)  # A-Z, then numbers
                    
                    # Generate standardized name: "HH-00_BreakX"
                    standardized_name = f"{hour:02d}-00_Break{break_label}"
                    
                    # Find previous and next tracks from log elements
                    previous_track_id = None
                    next_track_id = None
                    
                    # Find tracks around break position
                    cumulative_time = 0
                    for i, elem in enumerate(elements):
                        elem_start = cumulative_time
                        elem_end = cumulative_time + elem.get("duration", 0)
                        
                        # Check if break is before this element
                        if break_sec < elem_start:
                            # Next track is this element if it's a music track
                            if elem.get("type") in ["MUS", "IDS", "NEW"]:
                                # Try to find track by file_path or libretime_id
                                if elem.get("file_path"):
                                    track_result = await self.db.execute(
                                        select(Track).where(Track.filepath == elem.get("file_path"))
                                    )
                                    track = track_result.scalar_one_or_none()
                                    if track:
                                        next_track_id = track.id
                                elif elem.get("libretime_id") or elem.get("media_id"):
                                    # Try to find by libretime_id
                                    libretime_id = elem.get("libretime_id") or str(elem.get("media_id"))
                                    track_result = await self.db.execute(
                                        select(Track).where(Track.libretime_id == libretime_id)
                                    )
                                    track = track_result.scalar_one_or_none()
                                    if track:
                                        next_track_id = track.id
                            break
                        
                        # Check if break is within this element
                        if elem_start <= break_sec < elem_end:
                            # Previous track is the element before this one
                            if i > 0:
                                prev_elem = elements[i - 1]
                                if prev_elem.get("type") in ["MUS", "IDS", "NEW"]:
                                    if prev_elem.get("file_path"):
                                        track_result = await self.db.execute(
                                            select(Track).where(Track.filepath == prev_elem.get("file_path"))
                                        )
                                        track = track_result.scalar_one_or_none()
                                        if track:
                                            previous_track_id = track.id
                                    elif prev_elem.get("libretime_id") or prev_elem.get("media_id"):
                                        libretime_id = prev_elem.get("libretime_id") or str(prev_elem.get("media_id"))
                                        track_result = await self.db.execute(
                                            select(Track).where(Track.libretime_id == libretime_id)
                                        )
                                        track = track_result.scalar_one_or_none()
                                        if track:
                                            previous_track_id = track.id
                            
                            # Next track is this element or the one after
                            if elem.get("type") in ["MUS", "IDS", "NEW"]:
                                if elem.get("file_path"):
                                    track_result = await self.db.execute(
                                        select(Track).where(Track.filepath == elem.get("file_path"))
                                    )
                                    track = track_result.scalar_one_or_none()
                                    if track:
                                        next_track_id = track.id
                                elif elem.get("libretime_id") or elem.get("media_id"):
                                    libretime_id = elem.get("libretime_id") or str(elem.get("media_id"))
                                    track_result = await self.db.execute(
                                        select(Track).where(Track.libretime_id == libretime_id)
                                    )
                                    track = track_result.scalar_one_or_none()
                                    if track:
                                        next_track_id = track.id
                            elif i + 1 < len(elements):
                                next_elem = elements[i + 1]
                                if next_elem.get("type") in ["MUS", "IDS", "NEW"]:
                                    if next_elem.get("file_path"):
                                        track_result = await self.db.execute(
                                            select(Track).where(Track.filepath == next_elem.get("file_path"))
                                        )
                                        track = track_result.scalar_one_or_none()
                                        if track:
                                            next_track_id = track.id
                                    elif next_elem.get("libretime_id") or next_elem.get("media_id"):
                                        libretime_id = next_elem.get("libretime_id") or str(next_elem.get("media_id"))
                                        track_result = await self.db.execute(
                                            select(Track).where(Track.libretime_id == libretime_id)
                                        )
                                        track = track_result.scalar_one_or_none()
                                        if track:
                                            next_track_id = track.id
                            break
                        
                        # Track previous element if it's a music track
                        if elem.get("type") in ["MUS", "IDS", "NEW"] and elem_end <= break_sec:
                            if elem.get("file_path"):
                                track_result = await self.db.execute(
                                    select(Track).where(Track.filepath == elem.get("file_path"))
                                )
                                track = track_result.scalar_one_or_none()
                                if track:
                                    previous_track_id = track.id
                            elif elem.get("libretime_id") or elem.get("media_id"):
                                libretime_id = elem.get("libretime_id") or str(elem.get("media_id"))
                                track_result = await self.db.execute(
                                    select(Track).where(Track.libretime_id == libretime_id)
                                )
                                track = track_result.scalar_one_or_none()
                                if track:
                                    previous_track_id = track.id
                        
                        cumulative_time = elem_end
                    
                    # Calculate ramp time from next track if available
                    ramp_time = None
                    if next_track_id:
                        track_result = await self.db.execute(
                            select(Track).where(Track.id == next_track_id)
                        )
                        next_track = track_result.scalar_one_or_none()
                        if next_track and next_track.filepath:
                            # Run blocking audio analysis in thread pool to avoid blocking async event loop
                            import asyncio
                            try:
                                loop = asyncio.get_event_loop()
                                ramp_data = await loop.run_in_executor(
                                    None,
                                    self.audio_preview.calculate_ramp_times,
                                    next_track.filepath
                                )
                                ramp_time = ramp_data.get("ramp_in", 5.0)
                            except Exception as e:
                                logger.warning("Failed to calculate ramp times", error=str(e), track_path=next_track.filepath)
                                ramp_time = 5.0  # Default fallback
                    
                    # Create slot
                    slot = VoiceTrackSlot(
                        log_id=log_id,
                        hour=hour,
                        break_position=break_label,
                        standardized_name=standardized_name,
                        ramp_time=ramp_time,
                        status="pending"
                    )
                    
                    if previous_track_id:
                        slot.previous_track_id = previous_track_id
                    if next_track_id:
                        slot.next_track_id = next_track_id
                    
                    self.db.add(slot)
                    slots.append(slot)
            
            await self.db.commit()
            
            logger.info("Voice track slots created", log_id=log_id, count=len(slots))
            return slots
            
        except Exception as e:
            await self.db.rollback()
            logger.error("Slot creation failed", error=str(e), exc_info=True)
            raise
    
    async def get_slots_for_log(
        self,
        log_id: UUID,
        hour: Optional[UUID] = None,
        status: Optional[str] = None
    ) -> List[VoiceTrackSlot]:
        """
        Get voice track slots for a log
        
        Args:
            log_id: Daily log ID
            hour: Optional hour filter (0-23)
            status: Optional status filter
        
        Returns:
            List of slots
        """
        try:
            query = select(VoiceTrackSlot).where(VoiceTrackSlot.log_id == log_id)
            
            if hour is not None:
                query = query.where(VoiceTrackSlot.hour == hour)
            
            if status is not None:
                query = query.where(VoiceTrackSlot.status == status)
            
            query = query.order_by(VoiceTrackSlot.hour, VoiceTrackSlot.break_position)
            
            result = await self.db.execute(query)
            return list(result.scalars().all())
            
        except Exception as e:
            logger.error("Get slots failed", error=str(e), exc_info=True)
            return []
    
    async def assign_dj_to_slot(
        self,
        slot_id: UUID,
        dj_id: UUID
    ) -> Optional[VoiceTrackSlot]:
        """
        Assign DJ to a slot
        
        Args:
            slot_id: Slot ID
            dj_id: User ID of DJ
        
        Returns:
            Updated slot or None if not found
        """
        try:
            result = await self.db.execute(
                select(VoiceTrackSlot).where(VoiceTrackSlot.id == slot_id)
            )
            slot = result.scalar_one_or_none()
            
            if not slot:
                return None
            
            slot.assigned_dj_id = dj_id
            slot.status = "assigned"
            await self.db.commit()
            await self.db.refresh(slot)
            
            logger.info("DJ assigned to slot", slot_id=slot_id, dj_id=dj_id)
            return slot
            
        except Exception as e:
            await self.db.rollback()
            logger.error("DJ assignment failed", error=str(e), exc_info=True)
            return None
    
    async def link_voice_track_to_slot(
        self,
        slot_id: UUID,
        voice_track_id: UUID
    ) -> Optional[VoiceTrackSlot]:
        """
        Link a voice track to a slot
        
        Args:
            slot_id: Slot ID
            voice_track_id: Voice track ID
        
        Returns:
            Updated slot or None if not found
        """
        try:
            from backend.models.voice_track import VoiceTrack
            
            result = await self.db.execute(
                select(VoiceTrackSlot).where(VoiceTrackSlot.id == slot_id)
            )
            slot = result.scalar_one_or_none()
            
            if not slot:
                return None
            
            # Get voice track to update standardized_name
            track_result = await self.db.execute(
                select(VoiceTrack).where(VoiceTrack.id == voice_track_id)
            )
            voice_track = track_result.scalar_one_or_none()
            
            if voice_track and slot.standardized_name:
                # Update voice track with standardized name and recorded date
                voice_track.standardized_name = slot.standardized_name
                voice_track.recorded_date = datetime.now(timezone.utc)
                voice_track.break_id = slot_id
                
                # Rename file to match standardized_name if different
                import os
                from pathlib import Path
                from backend.routers.voice_tracks import VOICE_TRACKS_DIR
                
                current_file_path = Path(voice_track.file_url.replace("/api/voice/", "").replace("/file", ""))
                current_full_path = Path(VOICE_TRACKS_DIR) / current_file_path.name
                
                # Generate new filename from standardized_name
                file_ext = current_file_path.suffix or ".mp3"
                new_filename = f"{slot.standardized_name}{file_ext}"
                new_full_path = Path(VOICE_TRACKS_DIR) / new_filename
                
                # Rename file if it exists and name is different
                if current_full_path.exists() and current_full_path != new_full_path:
                    try:
                        os.rename(str(current_full_path), str(new_full_path))
                        voice_track.file_url = f"/api/voice/{new_filename}/file"
                        logger.info("Renamed voice track file", old_name=current_file_path.name, new_name=new_filename)
                    except Exception as e:
                        logger.warning("Failed to rename voice track file", error=str(e), old_path=str(current_full_path), new_path=str(new_full_path))
                
                await self.db.flush()
            
            slot.voice_track_id = voice_track_id
            slot.status = "recorded"
            await self.db.commit()
            await self.db.refresh(slot)
            
            # Sync voice track to log element
            if voice_track and slot.log_id:
                from backend.services.log_editor import LogEditor
                editor = LogEditor(self.db)
                await editor.sync_voice_track_to_log_element(
                    slot.log_id,
                    voice_track_id,
                    slot_id
                )
            
            logger.info("Voice track linked to slot", slot_id=slot_id, voice_track_id=voice_track_id, standardized_name=slot.standardized_name)
            return slot
            
        except Exception as e:
            await self.db.rollback()
            logger.error("Voice track linking failed", error=str(e), exc_info=True)
            return None
    
    async def find_voice_track_with_fallback(
        self,
        standardized_name: str,
        target_date: date,
        max_days_back: int = 28
    ) -> Optional[Dict[str, Any]]:
        """
        Find a voice track by standardized name with fallback to previous days
        
        Args:
            standardized_name: Standardized name (e.g., "14-00_BreakA")
            target_date: Target date to find recording for
            max_days_back: Maximum days to look back (default 28 = 4 weeks)
        
        Returns:
            Dict with voice_track info and fallback_date if using fallback, or None
        """
        try:
            from backend.models.voice_track import VoiceTrack
            
            # Try current date first
            current_date = target_date
            for days_back in range(max_days_back + 1):
                check_date = current_date - timedelta(days=days_back)
                
                # Look for voice track with this standardized_name recorded on or before check_date
                result = await self.db.execute(
                    select(VoiceTrack).where(
                        VoiceTrack.standardized_name == standardized_name,
                        VoiceTrack.recorded_date <= datetime.combine(check_date, datetime.max.time()).replace(tzinfo=timezone.utc)
                    ).order_by(VoiceTrack.recorded_date.desc())
                )
                voice_track = result.scalar_one_or_none()
                
                if voice_track:
                    return {
                        "voice_track": voice_track,
                        "voice_track_id": voice_track.id,
                        "libretime_id": voice_track.libretime_id,
                        "file_url": voice_track.file_url,
                        "recorded_date": voice_track.recorded_date,
                        "is_fallback": days_back > 0,
                        "fallback_days": days_back,
                        "fallback_date": check_date if days_back > 0 else None
                    }
            
            return None
            
        except Exception as e:
            logger.error("Fallback lookup failed", standardized_name=standardized_name, error=str(e), exc_info=True)
            return None

