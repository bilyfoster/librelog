import httpx
import asyncio
from datetime import datetime, timedelta
from typing import List, Dict, Optional
from sqlalchemy.orm import Session
from app.models.play_log import PlayLog, PlayStatistics, LibreTimeIntegration
from app.models.submission import Submission
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)


class LibreTimeService:
    def __init__(self, db: Session):
        self.db = db
        self.base_url = None
        self.api_key = None
        self._load_config()

    def _load_config(self):
        """Load LibreTime configuration from database"""
        config = self.db.query(LibreTimeIntegration).first()
        if config:
            self.base_url = config.libretime_url
            self.api_key = config.api_key
        else:
            # Default configuration - you'll need to set this up
            self.base_url = getattr(settings, 'LIBRETIME_URL', 'http://studio.gayphx.com')
            self.api_key = getattr(settings, 'LIBRETIME_API_KEY', '')

    async def validate_api_key(self, libretime_url: str, api_key: str) -> Dict[str, any]:
        """Validate LibreTime API key by making a test request"""
        try:
            # Clean up the URL - remove trailing slash and ensure it has protocol
            if not libretime_url.startswith(('http://', 'https://')):
                libretime_url = f"https://{libretime_url}"
            libretime_url = libretime_url.rstrip('/')
            
            async with httpx.AsyncClient() as client:
                # Test with the version endpoint which is simpler and more reliable
                url = f"{libretime_url}/api/version/format/json/api_key/{api_key}"
                
                logger.info(f"Attempting to validate LibreTime API key at: {url}")
                
                response = await client.get(url, timeout=10.0)
                
                logger.info(f"LibreTime API response: {response.status_code}")
                
                if response.status_code == 200:
                    try:
                        data = response.json()
                        logger.info(f"LibreTime API response data: {data}")
                        # Check if we got valid LibreTime version data
                        if 'airtime_version' in data and 'api_version' in data:
                            return {
                                "valid": True,
                                "message": f"API key is valid - LibreTime {data.get('airtime_version')} (API {data.get('api_version')})"
                            }
                        else:
                            return {
                                "valid": False,
                                "message": "Invalid response format from LibreTime server"
                            }
                    except Exception as e:
                        logger.error(f"JSON parsing error: {e}")
                        return {
                            "valid": False,
                            "message": "Invalid JSON response from LibreTime server"
                        }
                elif response.status_code == 401:
                    return {
                        "valid": False,
                        "message": "Invalid API key - authentication failed"
                    }
                elif response.status_code == 403:
                    return {
                        "valid": False,
                        "message": "API key lacks required permissions"
                    }
                else:
                    return {
                        "valid": False,
                        "message": f"API request failed with status {response.status_code}"
                    }
                    
        except httpx.ConnectError as e:
            logger.error(f"Connection error: {e}")
            return {
                "valid": False,
                "message": f"Cannot connect to LibreTime server at {libretime_url} - check URL and network connectivity"
            }
        except httpx.TimeoutException:
            logger.error("Timeout error")
            return {
                "valid": False,
                "message": "Connection timeout - LibreTime server may be slow or unreachable"
            }
        except Exception as e:
            logger.error(f"Validation error: {e}")
            return {
                "valid": False,
                "message": f"Validation error: {str(e)}"
            }

    async def sync_plays(self, hours_back: int = 24) -> Dict[str, int]:
        """Sync play data from LibreTime for the last N hours"""
        if not self.base_url or not self.api_key:
            logger.warning("LibreTime not configured")
            return {"error": "LibreTime not configured"}

        try:
            # Calculate time range
            end_time = datetime.utcnow()
            start_time = end_time - timedelta(hours=hours_back)
            
            # Fetch plays from LibreTime API
            plays_data = await self._fetch_plays_from_libretime(start_time, end_time)
            
            # Process and log plays
            synced_count = 0
            new_plays = 0
            
            for play_data in plays_data:
                # Check if this play already exists
                existing_play = self.db.query(PlayLog).filter(
                    PlayLog.libretime_play_id == play_data.get('id')
                ).first()
                
                if not existing_play:
                    # Find matching submission by ISRC or filename
                    submission = await self._find_submission_for_play(play_data)
                    
                    if submission:
                        # Create new play log
                        play_log = PlayLog(
                            submission_id=submission.id,
                            played_at=datetime.fromisoformat(play_data['played_at'].replace('Z', '+00:00')),
                            duration_played=play_data.get('duration', 0),
                            play_type='radio',
                            source='libretime',
                            libretime_play_id=play_data.get('id'),
                            libretime_show_id=play_data.get('show_id'),
                            libretime_show_name=play_data.get('show_name'),
                            dj_name=play_data.get('dj_name'),
                            show_name=play_data.get('show_name'),
                            time_slot=self._get_time_slot(play_data['played_at']),
                            extra_data=play_data
                        )
                        
                        self.db.add(play_log)
                        new_plays += 1
                
                synced_count += 1
            
            # Update play statistics
            await self._update_play_statistics()
            
            self.db.commit()
            
            return {
                "synced_plays": synced_count,
                "new_plays": new_plays,
                "status": "success"
            }
            
        except Exception as e:
            logger.error(f"Error syncing LibreTime plays: {e}")
            self.db.rollback()
            return {"error": str(e)}

    async def _fetch_plays_from_libretime(self, start_time: datetime, end_time: datetime) -> List[Dict]:
        """Fetch play data from LibreTime API"""
        async with httpx.AsyncClient() as client:
            try:
                # Try multiple LibreTime API endpoint formats
                endpoints_to_try = [
                    f"{self.base_url}/api/play-history/format/json/api_key/{self.api_key}",
                    f"{self.base_url}/api/play-history/api_key/{self.api_key}",
                    f"{self.base_url}/api/play-history?api_key={self.api_key}",
                    f"{self.base_url}/api/v2/play-history?api_key={self.api_key}",
                    f"{self.base_url}/api/play-history/format/json?api_key={self.api_key}"
                ]
                
                for url in endpoints_to_try:
                    try:
                        logger.info(f"Trying LibreTime API endpoint: {url}")
                        
                        params = {
                            'start': start_time.isoformat(),
                            'end': end_time.isoformat()
                        }
                        
                        response = await client.get(url, params=params, timeout=30.0)
                        logger.info(f"Response status: {response.status_code}")
                        logger.info(f"Response headers: {dict(response.headers)}")
                        
                        if response.status_code == 200:
                            data = response.json()
                            logger.info(f"Successfully fetched data from {url}")
                            logger.info(f"Data type: {type(data)}, Length: {len(data) if isinstance(data, list) else 'N/A'}")
                            return data if isinstance(data, list) else []
                        else:
                            logger.warning(f"Endpoint {url} returned status {response.status_code}")
                            
                    except httpx.HTTPError as e:
                        logger.warning(f"HTTP error with endpoint {url}: {e}")
                        continue
                    except Exception as e:
                        logger.warning(f"Error with endpoint {url}: {e}")
                        continue
                
                logger.error("All LibreTime API endpoints failed")
                return []
                
            except Exception as e:
                logger.error(f"Error fetching LibreTime data: {e}")
                return []

    async def _find_submission_for_play(self, play_data: Dict) -> Optional[Submission]:
        """Find submission that matches the played track"""
        # Try to match by ISRC first (most reliable)
        isrc_code = play_data.get('isrc')
        if isrc_code:
            submission = self.db.query(Submission).join(Submission.isrc).filter(
                Submission.isrc.has(isrc_code=isrc_code)
            ).first()
            if submission:
                return submission
        
        # Try to match by filename
        filename = play_data.get('filename', '')
        if filename:
            # Extract base filename without path
            base_filename = filename.split('/')[-1]
            submission = self.db.query(Submission).filter(
                Submission.file_name.ilike(f'%{base_filename}%')
            ).first()
            if submission:
                return submission
        
        # Try to match by artist name + song title (fuzzy matching)
        artist_name = play_data.get('artist', '')
        song_title = play_data.get('title', '')
        if artist_name and song_title:
            # Look for submissions with similar artist and song names
            submission = self.db.query(Submission).join(Submission.artist).filter(
                Submission.artist.has(name.ilike(f'%{artist_name}%')),
                Submission.song_title.ilike(f'%{song_title}%')
            ).first()
            if submission:
                return submission
        
        # Try to match by song title only (last resort)
        if song_title:
            submission = self.db.query(Submission).filter(
                Submission.song_title.ilike(f'%{song_title}%')
            ).first()
            if submission:
                return submission
        
        return None

    def _get_time_slot(self, played_at_str: str) -> str:
        """Determine time slot based on play time"""
        try:
            played_at = datetime.fromisoformat(played_at_str.replace('Z', '+00:00'))
            hour = played_at.hour
            
            if 6 <= hour < 12:
                return "Morning"
            elif 12 <= hour < 17:
                return "Afternoon"
            elif 17 <= hour < 22:
                return "Evening"
            else:
                return "Late Night"
        except:
            return "Unknown"

    async def _update_play_statistics(self):
        """Update play statistics for all submissions"""
        submissions = self.db.query(Submission).all()
        
        for submission in submissions:
            # Get play statistics
            stats = self.db.query(PlayStatistics).filter(
                PlayStatistics.submission_id == submission.id
            ).first()
            
            if not stats:
                stats = PlayStatistics(submission_id=submission.id)
                self.db.add(stats)
            
            # Count total plays (LibreTime doesn't distinguish between play types)
            plays = self.db.query(PlayLog).filter(
                PlayLog.submission_id == submission.id
            ).all()
            
            stats.total_plays = len(plays)
            # LibreTime only provides total plays, not broken down by type
            stats.radio_plays = len(plays)  # All plays are radio plays from LibreTime
            stats.podcast_plays = 0  # Not tracked separately
            stats.commercial_plays = 0  # Not tracked separately
            
            # Time-based statistics
            now = datetime.utcnow()
            week_ago = now - timedelta(days=7)
            month_ago = now - timedelta(days=30)
            year_ago = now - timedelta(days=365)
            
            stats.plays_this_week = len([p for p in plays if p.played_at >= week_ago])
            stats.plays_this_month = len([p for p in plays if p.played_at >= month_ago])
            stats.plays_this_year = len([p for p in plays if p.played_at >= year_ago])
            
            # Last play info
            if plays:
                last_play = max(plays, key=lambda p: p.played_at)
                stats.last_played_at = last_play.played_at
                stats.last_played_by = last_play.dj_name or last_play.show_name
            
            # Peak times analysis
            if plays:
                hour_counts = {}
                day_counts = {}
                
                for play in plays:
                    hour = play.played_at.hour
                    day = play.played_at.weekday()
                    
                    hour_counts[hour] = hour_counts.get(hour, 0) + 1
                    day_counts[day] = day_counts.get(day, 0) + 1
                
                stats.most_played_hour = max(hour_counts, key=hour_counts.get) if hour_counts else None
                stats.most_played_day = max(day_counts, key=day_counts.get) if day_counts else None

    async def manual_log_play(
        self, 
        submission_id: str, 
        played_at: datetime,
        dj_name: str = None,
        show_name: str = None,
        duration_played: int = None
    ) -> PlayLog:
        """Manually log a play (for admin use)"""
        submission = self.db.query(Submission).filter(Submission.id == submission_id).first()
        if not submission:
            raise ValueError("Submission not found")
        
        play_log = PlayLog(
            submission_id=submission_id,
            played_at=played_at,
            duration_played=duration_played,
            play_type='radio',
            source='manual',
            dj_name=dj_name,
            show_name=show_name,
            time_slot=self._get_time_slot(played_at.isoformat())
        )
        
        self.db.add(play_log)
        await self._update_play_statistics()
        self.db.commit()
        
        return play_log

    def get_play_statistics(self, submission_id: str = None) -> Dict:
        """Get play statistics for a submission or all submissions"""
        if submission_id:
            # Get play counts by type for this submission
            from sqlalchemy import func
            
            play_counts = self.db.query(
                PlayLog.play_type,
                func.count(PlayLog.id).label('count')
            ).filter(
                PlayLog.submission_id == submission_id
            ).group_by(PlayLog.play_type).all()
            
            # Get total plays
            total_plays = sum(pc.count for pc in play_counts)
            
            # Get last played date
            last_play = self.db.query(PlayLog).filter(
                PlayLog.submission_id == submission_id
            ).order_by(PlayLog.played_at.desc()).first()
            
            # Separate by play type
            radio_plays = next((pc.count for pc in play_counts if pc.play_type == "radio"), 0)
            gallery_plays = next((pc.count for pc in play_counts if pc.play_type == "gallery"), 0)
            other_plays = total_plays - radio_plays - gallery_plays
            
            return {
                "submission_id": submission_id,
                "total_plays": total_plays,
                "radio_plays": radio_plays,
                "gallery_plays": gallery_plays,
                "other_plays": other_plays,
                "last_played_at": last_play.played_at.isoformat() if last_play else None,
                "last_played_by": last_play.dj_name if last_play else None
            }
        else:
            # Get overall statistics
            total_submissions = self.db.query(Submission).count()
            
            # Get play counts by type
            from sqlalchemy import func
            play_counts = self.db.query(
                PlayLog.play_type,
                func.count(PlayLog.id).label('count')
            ).group_by(PlayLog.play_type).all()
            
            total_plays = sum(pc.count for pc in play_counts)
            radio_plays = next((pc.count for pc in play_counts if pc.play_type == "radio"), 0)
            gallery_plays = next((pc.count for pc in play_counts if pc.play_type == "gallery"), 0)
            other_plays = total_plays - radio_plays - gallery_plays
            
            recent_plays = self.db.query(PlayLog).filter(
                PlayLog.played_at >= datetime.utcnow() - timedelta(days=7)
            ).count()
            
            return {
                "total_submissions": total_submissions,
                "total_plays": total_plays,
                "radio_plays": radio_plays,
                "gallery_plays": gallery_plays,
                "other_plays": other_plays,
                "plays_this_week": recent_plays,
                "average_plays_per_submission": total_plays / total_submissions if total_submissions > 0 else 0
            }

