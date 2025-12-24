"""
Activity feed endpoints
"""

from fastapi import APIRouter, Depends, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, desc
from backend.database import get_db
from backend.models.track import Track
from backend.models.daily_log import DailyLog
from backend.models.campaign import Campaign
from backend.models.voice_track import VoiceTrack
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime, timedelta, timezone

router = APIRouter()


class ActivityItem(BaseModel):
    """Single activity item"""
    text: str
    time: str
    type: str  # "track", "log", "campaign", "voice"


class ActivityResponse(BaseModel):
    """Activity feed response"""
    activities: List[ActivityItem]


@router.get("/recent", response_model=ActivityResponse)
async def get_recent_activity(
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get recent activity from various sources"""
    try:
        activities = []
        cutoff_time = datetime.now(timezone.utc) - timedelta(days=7)
        
        # Get recently synced/updated tracks (last 7 days)
        try:
            track_result = await db.execute(
                select(Track)
                .where(Track.updated_at >= cutoff_time)
                .order_by(desc(Track.updated_at))
                .limit(10)
            )
            recent_tracks = track_result.scalars().all()
            
            for track in recent_tracks:
                if track.updated_at:
                    time_ago = _format_time_ago(track.updated_at)
                    activities.append(ActivityItem(
                        text=f'Track synced: "{track.title}"' + (f' by {track.artist}' if track.artist else ''),
                        time=time_ago,
                        type="track"
                    ))
        except Exception as e:
            # Log but don't fail the entire endpoint
            pass
        
        # Get recently published logs
        try:
            log_result = await db.execute(
                select(DailyLog)
                .where(DailyLog.published == True)
                .where(DailyLog.updated_at >= cutoff_time)
                .order_by(desc(DailyLog.updated_at))
                .limit(10)
            )
            recent_logs = log_result.scalars().all()
            
            for log in recent_logs:
                if log.updated_at:
                    time_ago = _format_time_ago(log.updated_at)
                    activities.append(ActivityItem(
                        text=f'Daily log published for {log.date.isoformat()}',
                        time=time_ago,
                        type="log"
                    ))
        except Exception as e:
            # Log but don't fail the entire endpoint
            pass
        
        # Get recently created campaigns
        try:
            campaign_result = await db.execute(
                select(Campaign)
                .where(Campaign.created_at >= cutoff_time)
                .order_by(desc(Campaign.created_at))
                .limit(10)
            )
            recent_campaigns = campaign_result.scalars().all()
            
            for campaign in recent_campaigns:
                if campaign.created_at:
                    time_ago = _format_time_ago(campaign.created_at)
                    activities.append(ActivityItem(
                        text=f'Campaign "{campaign.advertiser}" created',
                        time=time_ago,
                        type="campaign"
                    ))
        except Exception as e:
            # Log but don't fail the entire endpoint
            pass
        
        # Get recently recorded voice tracks
        try:
            voice_result = await db.execute(
                select(VoiceTrack)
                .where(VoiceTrack.created_at >= cutoff_time)
                .order_by(desc(VoiceTrack.created_at))
                .limit(10)
            )
            recent_voice = voice_result.scalars().all()
            
            for voice in recent_voice:
                if voice.created_at:
                    time_ago = _format_time_ago(voice.created_at)
                    activities.append(ActivityItem(
                        text=f'Voice track recorded: "{voice.show_name}"',
                        time=time_ago,
                        type="voice"
                    ))
        except Exception as e:
            # Log but don't fail the entire endpoint
            pass
        
        # Sort by time (most recent first) and limit
        activities.sort(key=lambda x: _parse_time_ago(x.time), reverse=True)
        activities = activities[:limit]
        
        return ActivityResponse(activities=activities)
    except Exception as e:
        # Return empty activities on error rather than failing
        import traceback
        traceback.print_exc()
        return ActivityResponse(activities=[])


def _format_time_ago(dt: datetime) -> str:
    """Format datetime as human-readable time ago"""
    if not dt:
        return "unknown"
    
    # Ensure both datetimes are timezone-aware
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    now = datetime.now(timezone.utc)
    diff = now - dt
    
    if diff.days > 0:
        return f"{diff.days} day{'s' if diff.days > 1 else ''} ago"
    elif diff.seconds >= 3600:
        hours = diff.seconds // 3600
        return f"{hours} hour{'s' if hours > 1 else ''} ago"
    elif diff.seconds >= 60:
        minutes = diff.seconds // 60
        return f"{minutes} minute{'s' if minutes > 1 else ''} ago"
    else:
        return "just now"


def _parse_time_ago(time_str: str) -> datetime:
    """Parse time ago string to datetime for sorting"""
    # This is a simple implementation - just return current time minus estimated duration
    now = datetime.now(timezone.utc)
    if "day" in time_str:
        days = int(time_str.split()[0])
        return now - timedelta(days=days)
    elif "hour" in time_str:
        hours = int(time_str.split()[0])
        return now - timedelta(hours=hours)
    elif "minute" in time_str:
        minutes = int(time_str.split()[0])
        return now - timedelta(minutes=minutes)
    else:
        return now

