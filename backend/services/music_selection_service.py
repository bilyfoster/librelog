"""
Music Selection Service
Handles intelligent music selection based on BPM, dayparts, and artist separation rules
"""

from sqlalchemy.ext.asyncio import AsyncSession
from uuid import UUID
from sqlalchemy import select, and_, or_, func
from typing import Optional, List, Dict, Any
from datetime import datetime, timezone
import structlog
from backend.models.track import Track
from backend.models.daypart import Daypart

logger = structlog.get_logger()


class MusicSelectionService:
    """Service for intelligent music selection with separation rules"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def select_music_track(
        self,
        target_hour: int,
        target_date: Optional[datetime] = None,
        bpm_range: Optional[tuple[int, int]] = None,
        exclude_artists: Optional[List[str]] = None,
        exclude_tracks: Optional[List[int]] = None,
        recently_played_artists: Optional[Dict[str, datetime]] = None,
        min_time_between_artist: int = 60  # minutes
    ) -> Optional[Track]:
        """
        Select a music track based on multiple criteria:
        - Daypart eligibility
        - BPM range (if specified)
        - Artist separation rules
        - Recently played tracks
        
        Args:
            target_hour: Hour of day (0-23)
            target_date: Target date for scheduling
            bpm_range: Optional (min_bpm, max_bpm) tuple
            exclude_artists: List of artist names to exclude
            exclude_tracks: List of track IDs to exclude
            recently_played_artists: Dict of {artist: last_played_datetime}
            min_time_between_artist: Minimum minutes between same artist plays
        
        Returns:
            Selected Track or None if no suitable track found
        """
        try:
            # Get current daypart based on hour
            daypart = await self._get_daypart_for_hour(target_hour)
            daypart_id = daypart.id if daypart else None
            
            # Build query
            query = select(Track).where(
                Track.type == 'MUS',
                Track.duration.isnot(None)
            )
            
            # Filter by daypart eligibility
            if daypart_id:
                # Check if track is eligible for this daypart
                # daypart_eligible is a JSON array of daypart IDs
                query = query.where(
                    or_(
                        Track.daypart_eligible.is_(None),  # No restrictions
                        Track.daypart_eligible.contains([daypart_id])  # Contains this daypart
                    )
                )
            
            # Filter by BPM range if specified
            if bpm_range:
                min_bpm, max_bpm = bpm_range
                query = query.where(
                    or_(
                        Track.bpm.is_(None),  # No BPM set
                        and_(
                            Track.bpm >= min_bpm,
                            Track.bpm <= max_bpm
                        )
                    )
                )
            
            # Exclude specific tracks
            if exclude_tracks:
                query = query.where(~Track.id.in_(exclude_tracks))
            
            # Apply artist separation rules
            if exclude_artists:
                query = query.where(~Track.artist.in_(exclude_artists))
            
            # Filter by recently played artists (unless new release or back-to-back allowed)
            if recently_played_artists and target_date:
                excluded_artists = []
                for artist, last_played in recently_played_artists.items():
                    time_diff = (target_date - last_played).total_seconds() / 60  # minutes
                    if time_diff < min_time_between_artist:
                        excluded_artists.append(artist)
                
                if excluded_artists:
                    # Allow tracks that are new releases or allow back-to-back
                    query = query.where(
                        or_(
                            ~Track.artist.in_(excluded_artists),
                            Track.is_new_release == True,
                            Track.allow_back_to_back == True
                        )
                    )
            
            # Order by least recently played, then by random
            query = query.order_by(
                Track.last_played.asc().nullsfirst(),
                func.random()
            ).limit(100)  # Get top 100 candidates
            
            result = await self.db.execute(query)
            candidates = result.scalars().all()
            
            if not candidates:
                logger.warning(
                    "No music tracks found matching criteria",
                    target_hour=target_hour,
                    daypart_id=daypart_id,
                    bpm_range=bpm_range
                )
                return None
            
            # Select from candidates (prefer least recently played)
            selected = candidates[0]
            
            logger.info(
                "Music track selected",
                track_id=selected.id,
                title=selected.title,
                artist=selected.artist,
                bpm=selected.bpm,
                daypart_id=daypart_id
            )
            
            return selected
            
        except Exception as e:
            logger.error(
                "Error selecting music track",
                error=str(e),
                exc_info=True
            )
            return None
    
    async def _get_daypart_for_hour(self, hour: int) -> Optional[Daypart]:
        """Get the daypart for a given hour"""
        try:
            from datetime import time
            target_time = time(hour=hour, minute=0)
            
            # Get all active dayparts and find which one matches this hour
            result = await self.db.execute(
                select(Daypart).where(
                    Daypart.active == True,
                    Daypart.start_time <= target_time,
                    Daypart.end_time > target_time
                ).order_by(Daypart.start_time.desc())
            )
            daypart = result.scalar_one_or_none()
            return daypart
        except Exception as e:
            logger.warning("Error getting daypart for hour", hour=hour, error=str(e))
            return None
    
    async def get_recently_played_artists(
        self,
        log_id: Optional[UUID] = None,
        hours_back: int = 2
    ) -> Dict[str, datetime]:
        """
        Get recently played artists from logs
        
        Args:
            log_id: Optional log ID to check
            hours_back: How many hours back to check
        
        Returns:
            Dict of {artist: last_played_datetime}
        """
        try:
            from backend.models.daily_log import DailyLog
            from datetime import timedelta
            
            cutoff_time = datetime.now(timezone.utc) - timedelta(hours=hours_back)
            
            # Query recent logs
            query = select(DailyLog).where(
                DailyLog.created_at >= cutoff_time
            )
            
            if log_id:
                query = query.where(DailyLog.id == log_id)
            
            result = await self.db.execute(query)
            logs = result.scalars().all()
            
            artist_times = {}
            
            for log in logs:
                hourly_logs = log.json_data.get("hourly_logs", {})
                for hour_key, hour_data in hourly_logs.items():
                    elements = hour_data.get("elements", [])
                    for element in elements:
                        if element.get("type") == "MUS" and element.get("artist"):
                            artist = element.get("artist")
                            # Use log creation time as approximation
                            if artist not in artist_times or log.created_at > artist_times[artist]:
                                artist_times[artist] = log.created_at
            
            return artist_times
            
        except Exception as e:
            logger.error("Error getting recently played artists", error=str(e))
            return {}
    
    async def check_artist_separation_violation(
        self,
        track: Track,
        target_hour: int,
        target_date: datetime,
        min_time_between: int = 60
    ) -> bool:
        """
        Check if playing this track would violate artist separation rules
        
        Returns:
            True if violation would occur, False otherwise
        """
        if track.allow_back_to_back or track.is_new_release:
            return False
        
        recently_played = await self.get_recently_played_artists(hours_back=2)
        
        if track.artist in recently_played:
            last_played = recently_played[track.artist]
            time_diff = (target_date - last_played).total_seconds() / 60
            return time_diff < min_time_between
        
        return False

