"""
Daily log management router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from backend.database import get_db
from backend.models.daily_log import DailyLog
from backend.models.spot import Spot
from backend.models.voice_track_slot import VoiceTrackSlot
from backend.services.log_generator import LogGenerator
from backend.services.log_editor import LogEditor
from backend.services.voice_track_slot_service import VoiceTrackSlotService
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import date, datetime, timezone

router = APIRouter()


@router.get("/count")
async def get_logs_count(
    published_only: bool = Query(False),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get total count of daily logs"""
    query = select(func.count(DailyLog.id))
    
    if published_only:
        query = query.where(DailyLog.published == True)
    
    result = await db.execute(query)
    count = result.scalar() or 0
    
    return {"count": count}


class LogGenerateRequest(BaseModel):
    target_date: date
    clock_template_id: int


class LogPreviewRequest(BaseModel):
    target_date: date
    clock_template_id: int
    preview_hours: Optional[List[str]] = None


@router.get("/")
@router.get("")  # Handle both with and without trailing slash
async def list_logs(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    published_only: bool = Query(False),
    date_filter: Optional[date] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all daily logs"""
    query = select(DailyLog)
    
    if published_only:
        query = query.where(DailyLog.published == True)
    
    if date_filter:
        query = query.where(DailyLog.date == date_filter)
    
    query = query.offset(skip).limit(limit).order_by(DailyLog.date.desc())
    
    result = await db.execute(query)
    logs = result.scalars().all()
    
    return {
        "logs": [
            {
                "id": log.id,
                "date": log.date,
                "generated_by": log.generated_by,
                "published": log.published,
                "created_at": log.created_at,
                "updated_at": log.updated_at,
                "total_duration": log.json_data.get("total_duration", 0)
            }
            for log in logs
        ],
        "total": len(logs)
    }


@router.post("/generate")
async def generate_log(
    request: LogGenerateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate a new daily log"""
    generator = LogGenerator(db)
    
    try:
        result = await generator.generate_daily_log(
            target_date=request.target_date,
            clock_template_id=request.clock_template_id,
            user_id=current_user.id
        )
        
        return result
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))


class BatchLogGenerateRequest(BaseModel):
    start_date: date
    end_date: date
    clock_template_id: int


@router.post("/generate-batch")
async def generate_batch_logs(
    request: BatchLogGenerateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate logs for multiple days (week view)"""
    from datetime import timedelta
    
    generator = LogGenerator(db)
    results = []
    errors = []
    
    current_date = request.start_date
    while current_date <= request.end_date:
        try:
            result = await generator.generate_daily_log(
                target_date=current_date,
                clock_template_id=request.clock_template_id,
                user_id=current_user.id
            )
            results.append({
                "date": current_date.isoformat(),
                "log_id": result.get("log_id"),
                "status": "success"
            })
        except Exception as e:
            errors.append({
                "date": current_date.isoformat(),
                "error": str(e),
                "status": "error"
            })
            logger.error("Failed to generate log for date", date=current_date, error=str(e))
        
        current_date += timedelta(days=1)
    
    return {
        "generated": len(results),
        "failed": len(errors),
        "results": results,
        "errors": errors
    }


@router.post("/preview")
async def preview_log(
    request: LogPreviewRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Preview a daily log without saving"""
    generator = LogGenerator(db)
    
    try:
        preview = await generator.preview_log(
            target_date=request.target_date,
            clock_template_id=request.clock_template_id,
            preview_hours=request.preview_hours
        )
        
        return preview
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))


@router.get("/{log_id}")
async def get_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific daily log"""
    result = await db.execute(
        select(DailyLog).where(DailyLog.id == log_id)
    )
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Daily log not found")
    
    return {
        "id": log.id,
        "date": log.date,
        "generated_by": log.generated_by,
        "published": log.published,
        "json_data": log.json_data,
        "created_at": log.created_at,
        "updated_at": log.updated_at
    }


@router.post("/{log_id}/publish")
async def publish_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Publish a daily log to LibreTime"""
    # Validate log before publishing
    editor = LogEditor(db)
    validation = await editor.validate_log(log_id)
    
    if not validation["valid"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={
                "message": "Log validation failed",
                "errors": validation["errors"],
                "warnings": validation["warnings"]
            }
        )
    
    # Check if log is locked
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Cannot publish locked log")
    
    generator = LogGenerator(db)
    
    success = await generator.publish_log(log_id)
    
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to publish log to LibreTime"
        )
    
    return {
        "message": "Log published to LibreTime successfully",
        "warnings": validation["warnings"]
    }


@router.post("/{log_id}/publish-hour")
async def publish_log_hour(
    log_id: int,
    hour_data: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Publish a specific hour of a log to LibreTime"""
    hour = hour_data.get("hour")
    
    if not hour:
        raise HTTPException(status_code=400, detail="hour parameter is required")
    
    # Parse hour (accept "HH:00" or "HH" format)
    hour_str = hour
    if ":" in hour:
        hour_str = hour.split(":")[0]
    
    try:
        hour_int = int(hour_str)
        if hour_int < 0 or hour_int > 23:
            raise HTTPException(status_code=400, detail="hour must be between 0 and 23")
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid hour format")
    
    # Get log
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Cannot publish locked log")
    
    generator = LogGenerator(db)
    
    success = await generator.publish_log_hour(log_id, hour_int)
    
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to publish hour {hour} to LibreTime"
        )
    
    return {
        "message": f"Hour {hour} published to LibreTime successfully",
        "hour": hour
    }


@router.delete("/{log_id}")
async def delete_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a daily log"""
    result = await db.execute(
        select(DailyLog).where(DailyLog.id == log_id)
    )
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Daily log not found")
    
    await db.delete(log)
    await db.commit()
    
    return {"message": "Daily log deleted successfully"}


@router.get("/{log_id}/timeline")
async def get_log_timeline(
    log_id: int,
    hour: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get timeline view of a daily log"""
    result = await db.execute(
        select(DailyLog).where(DailyLog.id == log_id)
    )
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Daily log not found")
    
    json_data = log.json_data
    
    if hour:
        # Return specific hour timeline
        hourly_logs = json_data.get("hourly_logs", {})
        if hour not in hourly_logs:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Hour not found in log")
        
        return {
            "log_id": log_id,
            "date": log.date.isoformat(),
            "hour": hour,
            "timeline": hourly_logs[hour].get("timeline", [])
        }
    else:
        # Return all hours timeline
        return {
            "log_id": log_id,
            "date": log.date.isoformat(),
            "hourly_timelines": {
                hour: data.get("timeline", [])
                for hour, data in json_data.get("hourly_logs", {}).items()
            }
        }


@router.post("/{log_id}/lock")
async def lock_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Lock a log to prevent further edits"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Log is already locked")
    
    log.locked = True
    log.locked_at = datetime.now(timezone.utc)
    log.locked_by = current_user.id
    
    await db.commit()
    await db.refresh(log)
    
    return {"message": "Log locked successfully", "locked": True, "locked_at": log.locked_at.isoformat()}


@router.post("/{log_id}/unlock")
async def unlock_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Unlock a log to allow edits"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if not log.locked:
        raise HTTPException(status_code=400, detail="Log is not locked")
    
    log.locked = False
    log.locked_at = None
    log.locked_by = None
    
    await db.commit()
    
    return {"message": "Log unlocked successfully", "locked": False}


@router.get("/{log_id}/conflicts")
async def get_log_conflicts(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get conflicts for a log"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    return {
        "log_id": log_id,
        "conflicts": log.conflicts or [],
        "conflict_count": len(log.conflicts) if log.conflicts else 0
    }


@router.get("/{log_id}/avails")
async def get_log_avails(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get available inventory for a log"""
    from backend.services.spot_scheduler import SpotScheduler
    
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    scheduler = SpotScheduler(db)
    avails = await scheduler.calculate_avails(log.date)
    
    return {
        "log_id": log_id,
        "date": log.date.isoformat(),
        "avails": avails
    }


@router.post("/{log_id}/spots")
async def add_spot_to_log(
    log_id: int,
    spot_data: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Add a spot to a log"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Log is locked and cannot be edited")
    
    editor = LogEditor(db)
    success = await editor.add_spot_to_log(log_id, spot_data)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to add spot to log")
    
    return {"message": "Spot added to log successfully"}


@router.put("/{log_id}/spots/{spot_id}")
async def update_spot_in_log(
    log_id: int,
    spot_id: int,
    spot_update: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a spot in a log (for drag-drop)"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Log is locked and cannot be edited")
    
    editor = LogEditor(db)
    success = await editor.move_spot(log_id, spot_id, spot_update)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to update spot in log")
    
    return {"message": "Spot updated in log successfully"}


@router.delete("/{log_id}/spots/{spot_id}")
async def remove_spot_from_log(
    log_id: int,
    spot_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Remove a spot from a log"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Log is locked and cannot be edited")
    
    editor = LogEditor(db)
    success = await editor.remove_spot(log_id, spot_id)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to remove spot from log")
    
    return {"message": "Spot removed from log successfully"}


@router.get("/{log_id}/voice-slots")
async def get_voice_slots(
    log_id: int,
    hour: Optional[int] = Query(None, ge=0, le=23),
    status: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get all voice track slots for a log. Auto-creates slots if they don't exist."""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    slot_service = VoiceTrackSlotService(db)
    slots = await slot_service.get_slots_for_log(log_id, hour, status)
    
    # If no slots exist, create them automatically
    if not slots:
        from backend.services.log_generator import LogGenerator
        from backend.services.clock_service import ClockTemplateService
        
        try:
            # Get clock template from log
            clock_service = ClockTemplateService(db)
            clock_template = await clock_service.get_template(log.json_data.get("clock_template_id"))
            
            if clock_template:
                # Extract break structure from log
                # Use the same logic as log generation to determine break positions
                hourly_logs = log.json_data.get("hourly_logs", {})
                break_structure = {}
                
                # Extract break positions from hourly logs (look for VOT, spots, or breaks)
                for hour in range(24):
                    hour_str = f"{hour:02d}:00"
                    hour_data = hourly_logs.get(hour_str, {})
                    hour_elements = hour_data.get("elements", [])
                    
                    break_positions = []
                    cumulative_time = 0
                    
                    for elem in hour_elements:
                        elem_type = elem.get("type")
                        # Look for VOT, spots, or breaks (VOT elements indicate voice track positions)
                        if elem_type in ["VOT", "spot", "break", "voice_track"]:
                            break_positions.append(cumulative_time)
                        cumulative_time += elem.get("duration", 0)
                    
                    # If no breaks found, use default positions (15, 30, 45 minutes)
                    if not break_positions:
                        break_positions = [900, 1800, 2700]  # 15, 30, 45 minutes
                    
                    break_structure[hour] = break_positions
                
                # Create slots
                slots = await slot_service.create_slots_for_log(log_id, break_structure)
        except Exception as e:
            # If auto-creation fails, log but don't fail the request
            import structlog
            logger = structlog.get_logger()
            logger.warning("Failed to auto-create voice track slots", log_id=log_id, error=str(e))
    
    return {
        "log_id": log_id,
        "slots": [
            {
                "id": slot.id,
                "hour": slot.hour,
                "break_position": slot.break_position,
                "assigned_dj_id": slot.assigned_dj_id,
                "voice_track_id": slot.voice_track_id,
                "previous_track_id": slot.previous_track_id,
                "next_track_id": slot.next_track_id,
                "ramp_time": float(slot.ramp_time) if slot.ramp_time else None,
                "status": slot.status,
                "created_at": slot.created_at.isoformat() if slot.created_at else None,
                "updated_at": slot.updated_at.isoformat() if slot.updated_at else None
            }
            for slot in slots
        ],
        "count": len(slots)
    }


@router.get("/{log_id}/voice-slots/{slot_id}/context")
async def get_voice_slot_context(
    log_id: int,
    slot_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get context (previous and next tracks) for a voice track slot"""
    from backend.models.voice_track_slot import VoiceTrackSlot
    from backend.models.track import Track
    
    # Get slot
    slot_result = await db.execute(
        select(VoiceTrackSlot).where(
            VoiceTrackSlot.id == slot_id,
            VoiceTrackSlot.log_id == log_id
        )
    )
    slot = slot_result.scalar_one_or_none()
    
    if not slot:
        raise HTTPException(status_code=404, detail="Voice track slot not found")
    
    # Get log to extract element context
    log_result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = log_result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    hour_str = f"{slot.hour:02d}:00"
    hourly_logs = log.json_data.get("hourly_logs", {})
    hour_data = hourly_logs.get(hour_str, {})
    elements = hour_data.get("elements", [])
    
    # Find break position in elements
    break_sec = None
    if slot.break_position:
        # Try to parse break position (could be "A", "B", "C" or time like "15:00")
        if slot.break_position.isalpha():
            # Letter-based: A=first break, B=second, etc.
            break_index = ord(slot.break_position.upper()) - 65
            # Default break positions at 15, 30, 45 minutes
            default_breaks = [900, 1800, 2700]
            if break_index < len(default_breaks):
                break_sec = default_breaks[break_index]
        else:
            # Try to parse as time
            try:
                parts = slot.break_position.split(':')
                if len(parts) == 2:
                    break_sec = int(parts[0]) * 60 + int(parts[1])
            except:
                pass
    
    # Find previous and next elements
    previous_element = None
    next_element = None
    cumulative_time = 0
    
    for i, elem in enumerate(elements):
        elem_end = cumulative_time + elem.get("duration", 0)
        if break_sec is not None:
            if elem_end <= break_sec:
                previous_element = elem
            elif cumulative_time < break_sec < elem_end:
                previous_element = elem if i > 0 else None
                next_element = elem
                break
            elif cumulative_time >= break_sec:
                if not next_element:
                    next_element = elem
                break
        cumulative_time = elem_end
    
    # Get track details if IDs are available
    previous_track = None
    next_track = None
    
    if slot.previous_track_id:
        track_result = await db.execute(select(Track).where(Track.id == slot.previous_track_id))
        prev_track = track_result.scalar_one_or_none()
        if prev_track:
            previous_track = {
                "id": prev_track.id,
                "title": prev_track.title,
                "artist": prev_track.artist,
                "duration": prev_track.duration,
                "type": prev_track.type,
                "libretime_id": prev_track.libretime_id
            }
    
    if slot.next_track_id:
        track_result = await db.execute(select(Track).where(Track.id == slot.next_track_id))
        next_track = track_result.scalar_one_or_none()
        if next_track:
            next_track = {
                "id": next_track.id,
                "title": next_track.title,
                "artist": next_track.artist,
                "duration": next_track.duration,
                "type": next_track.type,
                "libretime_id": next_track.libretime_id
            }
    
    # If no track IDs but we have elements, use element data
    if not previous_track and previous_element:
        previous_track = {
            "title": previous_element.get("title", ""),
            "artist": previous_element.get("artist", ""),
            "duration": previous_element.get("duration", 0),
            "type": previous_element.get("type", ""),
            "libretime_id": previous_element.get("libretime_id")
        }
    
    if not next_track and next_element:
        next_track = {
            "title": next_element.get("title", ""),
            "artist": next_element.get("artist", ""),
            "duration": next_element.get("duration", 0),
            "type": next_element.get("type", ""),
            "libretime_id": next_element.get("libretime_id")
        }
    
    return {
        "slot_id": slot_id,
        "log_id": log_id,
        "hour": slot.hour,
        "break_position": slot.break_position,
        "previous_track": previous_track,
        "next_track": next_track,
        "previous_element": previous_element,
        "next_element": next_element,
        "ramp_time": float(slot.ramp_time) if slot.ramp_time else None
    }


@router.put("/{log_id}/elements/{hour}")
async def update_element_in_log(
    log_id: int,
    hour: str,
    element_data: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update an element in a log"""
    editor = LogEditor(db)
    element_index = element_data.get("element_index")
    updates = element_data.get("updates", {})
    
    if element_index is None:
        raise HTTPException(status_code=400, detail="element_index is required")
    
    success = await editor.update_element(log_id, hour, element_index, updates)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to update element")
    
    return {"message": "Element updated successfully"}


@router.post("/{log_id}/elements/{hour}")
async def add_element_to_log(
    log_id: int,
    hour: str,
    element_data: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Add an element to a log"""
    editor = LogEditor(db)
    element = element_data.get("element", {})
    position = element_data.get("position")
    
    success = await editor.add_element(log_id, hour, element, position)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to add element")
    
    return {"message": "Element added successfully"}


@router.delete("/{log_id}/elements/{hour}/{element_index}")
async def remove_element_from_log(
    log_id: int,
    hour: str,
    element_index: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Remove an element from a log"""
    editor = LogEditor(db)
    
    success = await editor.remove_element(log_id, hour, element_index)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to remove element")
    
    return {"message": "Element removed successfully"}


@router.post("/{log_id}/add-vot-placeholders")
async def add_vot_placeholders_to_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Add missing VOT placeholder elements to an existing log"""
    editor = LogEditor(db)
    success = await editor.add_missing_vot_placeholders(log_id)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to add VOT placeholders")
    
    return {"message": "VOT placeholders added successfully"}


@router.post("/{log_id}/elements/{hour}/reorder")
async def reorder_elements_in_log(
    log_id: int,
    hour: str,
    reorder_data: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Reorder elements in a log"""
    editor = LogEditor(db)
    from_index = reorder_data.get("from_index")
    to_index = reorder_data.get("to_index")
    
    if from_index is None or to_index is None:
        raise HTTPException(status_code=400, detail="from_index and to_index are required")
    
    success = await editor.reorder_elements(log_id, hour, from_index, to_index)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to reorder elements")
    
    return {"message": "Elements reordered successfully"}
