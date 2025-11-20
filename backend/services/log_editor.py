"""
Log Editor service for manual log editing
"""

from typing import Dict, Any, Optional
from datetime import datetime, timezone, date
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.models.daily_log import DailyLog
from backend.models.spot import Spot
from backend.models.voice_track_slot import VoiceTrackSlot
from backend.services.voice_track_slot_service import VoiceTrackSlotService
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
    
    async def update_element(
        self,
        log_id: int,
        hour: str,
        element_index: int,
        updates: Dict[str, Any]
    ) -> bool:
        """Update an element in a log"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        if log.locked:
            logger.warning("Attempted to edit locked log", log_id=log_id)
            return False
        
        json_data = log.json_data.copy()
        hourly_logs = json_data.get("hourly_logs", {})
        
        if hour not in hourly_logs:
            return False
        
        elements = hourly_logs[hour].get("elements", [])
        if element_index >= len(elements):
            return False
        
        # Update element
        elements[element_index].update(updates)
        
        # Recalculate timing if duration or start_time changed
        if "duration" in updates or "start_time" in updates:
            self._recalculate_timing(hourly_logs[hour], element_index)
        
        hourly_logs[hour]["elements"] = elements
        json_data["hourly_logs"] = hourly_logs
        
        log.json_data = json_data
        log.updated_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(log)
        
        logger.info("Element updated", log_id=log_id, hour=hour, element_index=element_index)
        return True
    
    async def add_element(
        self,
        log_id: int,
        hour: str,
        element: Dict[str, Any],
        position: Optional[int] = None
    ) -> bool:
        """Add an element to a log"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        if log.locked:
            logger.warning("Attempted to edit locked log", log_id=log_id)
            return False
        
        json_data = log.json_data.copy()
        hourly_logs = json_data.get("hourly_logs", {})
        
        if hour not in hourly_logs:
            hourly_logs[hour] = {"elements": [], "total_duration": 0}
        
        elements = hourly_logs[hour].get("elements", [])
        
        # Set default start_time if not provided
        if "start_time" not in element:
            if elements:
                last_element = elements[-1]
                element["start_time"] = last_element.get("end_time", 0)
            else:
                element["start_time"] = 0
        
        # Set end_time based on start_time and duration
        duration = element.get("duration", 180)
        element["end_time"] = element["start_time"] + duration
        
        # Insert at position or append
        if position is not None and 0 <= position <= len(elements):
            elements.insert(position, element)
        else:
            elements.append(element)
        
        # Recalculate timing for all elements after insertion
        self._recalculate_timing(hourly_logs[hour], position if position is not None else len(elements) - 1)
        
        hourly_logs[hour]["elements"] = elements
        hourly_logs[hour]["total_duration"] = sum(e.get("duration", 0) for e in elements)
        json_data["hourly_logs"] = hourly_logs
        
        # Update total duration
        json_data["total_duration"] = sum(
            h.get("total_duration", 0) for h in hourly_logs.values()
        )
        
        log.json_data = json_data
        log.updated_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(log)
        
        logger.info("Element added", log_id=log_id, hour=hour)
        return True
    
    async def remove_element(
        self,
        log_id: int,
        hour: str,
        element_index: int
    ) -> bool:
        """Remove an element from a log"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        if log.locked:
            logger.warning("Attempted to edit locked log", log_id=log_id)
            return False
        
        json_data = log.json_data.copy()
        hourly_logs = json_data.get("hourly_logs", {})
        
        if hour not in hourly_logs:
            return False
        
        elements = hourly_logs[hour].get("elements", [])
        if element_index >= len(elements):
            return False
        
        # Remove element
        elements.pop(element_index)
        
        # Recalculate timing for remaining elements
        if elements:
            self._recalculate_timing(hourly_logs[hour], 0)
        
        hourly_logs[hour]["elements"] = elements
        hourly_logs[hour]["total_duration"] = sum(e.get("duration", 0) for e in elements)
        json_data["hourly_logs"] = hourly_logs
        
        # Update total duration
        json_data["total_duration"] = sum(
            h.get("total_duration", 0) for h in hourly_logs.values()
        )
        
        log.json_data = json_data
        log.updated_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(log)
        
        logger.info("Element removed", log_id=log_id, hour=hour, element_index=element_index)
        return True
    
    async def reorder_elements(
        self,
        log_id: int,
        hour: str,
        from_index: int,
        to_index: int
    ) -> bool:
        """Reorder elements in a log (for drag-drop)"""
        result = await self.db.execute(select(DailyLog).where(DailyLog.id == log_id))
        log = result.scalar_one_or_none()
        
        if not log:
            return False
        
        if log.locked:
            logger.warning("Attempted to edit locked log", log_id=log_id)
            return False
        
        json_data = log.json_data.copy()
        hourly_logs = json_data.get("hourly_logs", {})
        
        if hour not in hourly_logs:
            return False
        
        elements = hourly_logs[hour].get("elements", [])
        if from_index >= len(elements) or to_index >= len(elements):
            return False
        
        # Move element
        element = elements.pop(from_index)
        elements.insert(to_index, element)
        
        # Recalculate timing for all elements
        self._recalculate_timing(hourly_logs[hour], 0)
        
        hourly_logs[hour]["elements"] = elements
        json_data["hourly_logs"] = hourly_logs
        
        log.json_data = json_data
        log.updated_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(log)
        
        logger.info("Elements reordered", log_id=log_id, hour=hour, from_index=from_index, to_index=to_index)
        return True
    
    async def sync_voice_track_to_log_element(
        self,
        log_id: int,
        voice_track_id: int,
        slot_id: int
    ) -> bool:
        """
        Sync a recorded voice track back to the corresponding log element
        
        Args:
            log_id: Daily log ID
            voice_track_id: Voice track ID that was recorded
            slot_id: Voice track slot ID
        
        Returns:
            True if successful
        """
        try:
            from backend.models.voice_track import VoiceTrack
            
            # Get the slot to find hour and break position
            slot_result = await self.db.execute(
                select(VoiceTrackSlot).where(VoiceTrackSlot.id == slot_id)
            )
            slot = slot_result.scalar_one_or_none()
            
            if not slot or slot.log_id != log_id:
                return False
            
            # Get voice track
            track_result = await self.db.execute(
                select(VoiceTrack).where(VoiceTrack.id == voice_track_id)
            )
            voice_track = track_result.scalar_one_or_none()
            
            if not voice_track:
                return False
            
            # Get log
            log_result = await self.db.execute(
                select(DailyLog).where(DailyLog.id == log_id)
            )
            log = log_result.scalar_one_or_none()
            
            if not log:
                return False
            
            # Find VOT element in the hour that matches this slot
            hour_str = f"{slot.hour:02d}:00"
            json_data = log.json_data.copy()
            hourly_logs = json_data.get("hourly_logs", {})
            hour_data = hourly_logs.get(hour_str, {})
            elements = hour_data.get("elements", [])
            
            # Find VOT element - look for one that's pending or matches standardized_name
            for i, element in enumerate(elements):
                if element.get("type") == "VOT":
                    # Check if this element should be linked to this slot
                    # We can match by position in hour or by standardized_name
                    element_standardized = element.get("standardized_name")
                    if element_standardized == slot.standardized_name or not element_standardized:
                        # Update element with voice track info
                        elements[i].update({
                            "title": voice_track.show_name or "Voice Track",
                            "artist": "Voice Over",
                            "file_path": voice_track.file_url,
                            "libretime_id": voice_track.libretime_id,
                            "media_id": int(voice_track.libretime_id) if voice_track.libretime_id and str(voice_track.libretime_id).isdigit() else None,
                            "voice_track_id": voice_track.id,
                            "pending": False,
                            "standardized_name": slot.standardized_name,
                            "fallback_used": False
                        })
                        break
            
            hourly_logs[hour_str]["elements"] = elements
            json_data["hourly_logs"] = hourly_logs
            log.json_data = json_data
            log.updated_at = datetime.now(timezone.utc)
            
            await self.db.commit()
            await self.db.refresh(log)
            
            logger.info("Voice track synced to log element", log_id=log_id, voice_track_id=voice_track_id, slot_id=slot_id)
            return True
            
        except Exception as e:
            await self.db.rollback()
            logger.error("Failed to sync voice track to log element", error=str(e), exc_info=True)
            return False
    
    async def update_vot_elements_with_fallback(
        self,
        log_id: int
    ) -> bool:
        """
        Update all VOT elements in a log with fallback lookup
        
        This should be called before publishing to ensure all VOT elements
        have the correct voice track (current or fallback) assigned.
        """
        try:
            slot_service = VoiceTrackSlotService(self.db)
            
            # Get log
            log_result = await self.db.execute(
                select(DailyLog).where(DailyLog.id == log_id)
            )
            log = log_result.scalar_one_or_none()
            
            if not log:
                return False
            
            json_data = log.json_data.copy()
            hourly_logs = json_data.get("hourly_logs", {})
            updated = False
            
            # Get all slots for this log
            slots = await slot_service.get_slots_for_log(log_id)
            slot_map = {(slot.hour, slot.break_position): slot for slot in slots}
            
            # Update each hour's VOT elements
            for hour_str, hour_data in hourly_logs.items():
                elements = hour_data.get("elements", [])
                hour_int = int(hour_str.split(":")[0])
                
                for i, element in enumerate(elements):
                    if element.get("type") == "VOT" and element.get("pending"):
                        # Try to find matching slot
                        # VOT elements might be at break positions, so we need to match them
                        # For now, try to find any slot in this hour and use its standardized_name
                        matching_slot = None
                        for (slot_hour, slot_break), slot in slot_map.items():
                            if slot_hour == hour_int:
                                matching_slot = slot
                                break
                        
                        if matching_slot and matching_slot.standardized_name:
                            # Look up voice track with fallback
                            fallback_result = await slot_service.find_voice_track_with_fallback(
                                matching_slot.standardized_name,
                                log.date
                            )
                            
                            if fallback_result:
                                voice_track = fallback_result["voice_track"]
                                elements[i].update({
                                    "title": voice_track.show_name or "Voice Track",
                                    "artist": "Voice Over",
                                    "file_path": voice_track.file_url,
                                    "libretime_id": voice_track.libretime_id,
                                    "media_id": int(voice_track.libretime_id) if voice_track.libretime_id and str(voice_track.libretime_id).isdigit() else None,
                                    "voice_track_id": voice_track.id,
                                    "pending": False,
                                    "standardized_name": matching_slot.standardized_name,
                                    "fallback_used": fallback_result.get("is_fallback", False),
                                    "fallback_info": {
                                        "is_fallback": fallback_result.get("is_fallback", False),
                                        "fallback_days": fallback_result.get("fallback_days", 0),
                                        "fallback_date": fallback_result.get("fallback_date").isoformat() if fallback_result.get("fallback_date") else None,
                                        "recorded_date": fallback_result.get("recorded_date").isoformat() if fallback_result.get("recorded_date") else None
                                    } if fallback_result.get("is_fallback") else None
                                })
                                updated = True
            
            if updated:
                json_data["hourly_logs"] = hourly_logs
                log.json_data = json_data
                log.updated_at = datetime.now(timezone.utc)
                await self.db.commit()
                await self.db.refresh(log)
                logger.info("VOT elements updated with fallback", log_id=log_id)
            
            return True
            
        except Exception as e:
            await self.db.rollback()
            logger.error("Failed to update VOT elements with fallback", error=str(e), exc_info=True)
            return False
    
    async def add_missing_vot_placeholders(
        self,
        log_id: int
    ) -> bool:
        """
        Add VOT placeholder elements to a log if they're missing
        
        This is useful for logs generated before VOT placeholder functionality was added.
        It will add VOT placeholders at break positions (15, 30, 45 minutes) if not present.
        """
        try:
            from backend.models.clock_template import ClockTemplate
            from backend.services.voice_track_slot_service import VoiceTrackSlotService
            
            # Get log
            log_result = await self.db.execute(
                select(DailyLog).where(DailyLog.id == log_id)
            )
            log = log_result.scalar_one_or_none()
            
            if not log:
                return False
            
            # Get clock template to check if it has VOT elements
            clock_template_id = log.json_data.get("clock_template_id")
            if not clock_template_id:
                logger.warning("Log has no clock_template_id, cannot determine VOT positions", log_id=log_id)
                return False
            
            from backend.services.clock_service import ClockTemplateService
            clock_service = ClockTemplateService(self.db)
            clock_template = await clock_service.get_template(clock_template_id)
            
            if not clock_template:
                logger.warning("Clock template not found", clock_template_id=clock_template_id)
                return False
            
            # Get voice track slots to determine break positions
            slot_service = VoiceTrackSlotService(self.db)
            slots = await slot_service.get_slots_for_log(log_id)
            
            if not slots:
                logger.info("No voice track slots found, creating them", log_id=log_id)
                # Create slots if they don't exist - extract break structure from clock template
                layout = clock_template.json_layout
                elements = layout.get("elements", [])
                hourly_logs = log.json_data.get("hourly_logs", {})
                
                # Extract break structure (similar to LogGenerator._extract_break_structure)
                break_structure = {}
                for hour in range(24):
                    hour_str = f"{hour:02d}:00"
                    hour_data = hourly_logs.get(hour_str, {})
                    hour_elements = hour_data.get("elements", [])
                    
                    break_positions = []
                    cumulative_time = 0
                    
                    for elem in hour_elements:
                        elem_type = elem.get("type")
                        if elem_type in ["spot", "break", "voice_track", "VOT"]:
                            break_positions.append(cumulative_time)
                        cumulative_time += elem.get("duration", 0)
                    
                    # Default break positions if none found (15, 30, 45 minutes)
                    if not break_positions:
                        break_positions = [900, 1800, 2700]  # 15, 30, 45 minutes
                    
                    if break_positions:
                        break_structure[hour] = break_positions
                
                slots = await slot_service.create_slots_for_log(log_id, break_structure)
            
            json_data = log.json_data.copy()
            hourly_logs = json_data.get("hourly_logs", {})
            updated = False
            
            # Check each hour for missing VOT elements
            for slot in slots:
                hour_str = f"{slot.hour:02d}:00"
                hour_data = hourly_logs.get(hour_str, {})
                elements = hour_data.get("elements", [])
                
                # Check if VOT element exists for this break position
                has_vot = False
                for elem in elements:
                    if elem.get("type") == "VOT":
                        # Check if it matches this break position
                        # We can match by position in the hour or by standardized_name
                        if slot.standardized_name and elem.get("standardized_name") == slot.standardized_name:
                            has_vot = True
                            break
                        # Or check if it's at the break position
                        break_sec = None
                        if slot.break_position:
                            if slot.break_position.isalpha():
                                break_index = ord(slot.break_position.upper()) - 65
                                default_breaks = [900, 1800, 2700]  # 15, 30, 45 minutes
                                if break_index < len(default_breaks):
                                    break_sec = default_breaks[break_index]
                        
                        if break_sec:
                            cumulative = 0
                            for e in elements:
                                if cumulative <= break_sec < cumulative + e.get("duration", 0):
                                    if e.get("type") == "VOT":
                                        has_vot = True
                                        break
                                cumulative += e.get("duration", 0)
                
                # If no VOT found, add placeholder
                if not has_vot:
                    # Find insertion point (at break position)
                    break_sec = None
                    if slot.break_position:
                        if slot.break_position.isalpha():
                            break_index = ord(slot.break_position.upper()) - 65
                            default_breaks = [900, 1800, 2700]  # 15, 30, 45 minutes
                            if break_index < len(default_breaks):
                                break_sec = default_breaks[break_index]
                    
                    # Insert VOT placeholder at appropriate position
                    cumulative = 0
                    insert_index = len(elements)
                    for i, elem in enumerate(elements):
                        elem_end = cumulative + elem.get("duration", 0)
                        if break_sec and cumulative <= break_sec < elem_end:
                            insert_index = i + 1
                            break
                        cumulative = elem_end
                    
                    vot_element = {
                        "type": "VOT",
                        "title": "Voice Track - Pending Recording",
                        "artist": "Pending",
                        "duration": 30,
                        "start_time": cumulative,
                        "end_time": cumulative + 30,
                        "scheduled_time": break_sec or cumulative,
                        "file_path": None,
                        "libretime_id": None,
                        "media_id": None,
                        "fallback_used": False,
                        "pending": True,
                        "standardized_name": slot.standardized_name
                    }
                    
                    elements.insert(insert_index, vot_element)
                    # Recalculate timing for remaining elements
                    for i in range(insert_index + 1, len(elements)):
                        elements[i]["start_time"] = elements[i-1]["end_time"]
                        elements[i]["end_time"] = elements[i]["start_time"] + elements[i]["duration"]
                    
                    hour_data["elements"] = elements
                    hour_data["total_duration"] = sum(e.get("duration", 0) for e in elements)
                    hourly_logs[hour_str] = hour_data
                    updated = True
            
            if updated:
                json_data["hourly_logs"] = hourly_logs
                log.json_data = json_data
                log.updated_at = datetime.now(timezone.utc)
                await self.db.commit()
                await self.db.refresh(log)
                logger.info("Added missing VOT placeholders to log", log_id=log_id)
            
            return True
            
        except Exception as e:
            await self.db.rollback()
            logger.error("Failed to add missing VOT placeholders", error=str(e), exc_info=True)
            return False
    
    def _recalculate_timing(self, hour_data: Dict[str, Any], start_index: int = 0):
        """Recalculate timing for elements starting from start_index"""
        elements = hour_data.get("elements", [])
        if not elements:
            return
        
        # If start_index is 0, start from beginning
        if start_index == 0:
            current_time = 0
        else:
            # Start from previous element's end_time
            prev_element = elements[start_index - 1]
            current_time = prev_element.get("end_time", 0)
        
        # Recalculate timing for elements from start_index onwards
        for i in range(start_index, len(elements)):
            element = elements[i]
            duration = element.get("duration", 180)
            
            # Preserve hard_start timing if set
            if element.get("hard_start") and element.get("scheduled_time") is not None:
                current_time = element.get("scheduled_time", current_time)
            
            element["start_time"] = current_time
            element["end_time"] = current_time + duration
            current_time = element["end_time"]

