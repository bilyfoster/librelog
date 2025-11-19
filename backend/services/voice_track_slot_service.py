"""
Voice track slot service for managing break assignments
"""

from typing import List, Optional, Dict, Any
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
        log_id: int,
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
                    
                    # Find previous and next tracks
                    previous_track_id = None
                    next_track_id = None
                    
                    # Find track before break
                    cumulative_time = 0
                    for elem in elements:
                        if cumulative_time + elem.get("duration", 0) <= break_sec:
                            if elem.get("type") == "track" and elem.get("track_id"):
                                previous_track_id = elem.get("track_id")
                        cumulative_time += elem.get("duration", 0)
                        if cumulative_time > break_sec:
                            if elem.get("type") == "track" and elem.get("track_id"):
                                next_track_id = elem.get("track_id")
                            break
                    
                    # Calculate ramp time from next track if available
                    ramp_time = None
                    if next_track_id:
                        track_result = await self.db.execute(
                            select(Track).where(Track.id == next_track_id)
                        )
                        next_track = track_result.scalar_one_or_none()
                        if next_track and next_track.filepath:
                            ramp_data = self.audio_preview.calculate_ramp_times(next_track.filepath)
                            ramp_time = ramp_data.get("ramp_in", 5.0)
                    
                    # Create slot
                    slot = VoiceTrackSlot(
                        log_id=log_id,
                        hour=hour,
                        break_position=break_label,
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
        log_id: int,
        hour: Optional[int] = None,
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
        slot_id: int,
        dj_id: int
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
        slot_id: int,
        voice_track_id: int
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
            result = await self.db.execute(
                select(VoiceTrackSlot).where(VoiceTrackSlot.id == slot_id)
            )
            slot = result.scalar_one_or_none()
            
            if not slot:
                return None
            
            slot.voice_track_id = voice_track_id
            slot.status = "recorded"
            await self.db.commit()
            await self.db.refresh(slot)
            
            logger.info("Voice track linked to slot", slot_id=slot_id, voice_track_id=voice_track_id)
            return slot
            
        except Exception as e:
            await self.db.rollback()
            logger.error("Voice track linking failed", error=str(e), exc_info=True)
            return None

