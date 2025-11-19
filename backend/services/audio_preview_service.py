"""
Audio preview service for generating waveforms and track metadata
"""

import os
import subprocess
from pathlib import Path
from typing import Optional, Dict, Any, List
import librosa
import numpy as np
import structlog

logger = structlog.get_logger()


class AudioPreviewService:
    """Service for audio preview and waveform generation"""
    
    def __init__(self):
        self.ffprobe_path = os.getenv("FFPROBE_PATH", "ffprobe")
    
    def extract_track_metadata(self, track_path: str) -> Dict[str, Any]:
        """
        Extract metadata from audio track
        
        Args:
            track_path: Path to audio file
        
        Returns:
            Dictionary with duration, sample_rate, channels, etc.
        """
        try:
            if not os.path.exists(track_path):
                logger.warning("Track file not found", path=track_path)
                return {}
            
            # Use librosa for basic info
            y, sr = librosa.load(track_path, sr=None, duration=30)  # Load first 30s for analysis
            duration = librosa.get_duration(y=y, sr=sr)
            
            # Get full duration using ffprobe
            try:
                cmd = [
                    self.ffprobe_path,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    track_path
                ]
                result = subprocess.run(cmd, capture_output=True, text=True, check=True)
                full_duration = float(result.stdout.strip())
            except:
                full_duration = duration
            
            metadata = {
                "duration": full_duration,
                "sample_rate": sr,
                "channels": 2,  # librosa loads as mono, but we'll assume stereo
                "file_path": track_path,
                "file_size": os.path.getsize(track_path)
            }
            
            return metadata
            
        except Exception as e:
            logger.error("Metadata extraction failed", error=str(e), exc_info=True)
            return {}
    
    def generate_waveform_data(
        self,
        track_path: str,
        width: int = 800,
        height: int = 200
    ) -> Optional[Dict[str, Any]]:
        """
        Generate waveform data for visualization
        
        Args:
            track_path: Path to audio file
            width: Waveform width in pixels
            height: Waveform height in pixels
        
        Returns:
            Dictionary with waveform points and metadata
        """
        try:
            if not os.path.exists(track_path):
                logger.warning("Track file not found", path=track_path)
                return None
            
            # Load audio
            y, sr = librosa.load(track_path, sr=22050)  # Downsample for efficiency
            
            # Calculate duration
            duration = librosa.get_duration(y=y, sr=sr)
            
            # Generate waveform points
            # Downsample to width points
            samples_per_pixel = len(y) // width
            if samples_per_pixel < 1:
                samples_per_pixel = 1
            
            waveform_points = []
            for i in range(0, len(y), samples_per_pixel):
                chunk = y[i:i + samples_per_pixel]
                if len(chunk) > 0:
                    # Get RMS for this chunk
                    rms = np.sqrt(np.mean(chunk**2))
                    # Normalize to 0-1 range
                    normalized = float(rms)
                    waveform_points.append(normalized)
            
            # Ensure we have exactly width points
            if len(waveform_points) > width:
                waveform_points = waveform_points[:width]
            elif len(waveform_points) < width:
                # Pad with zeros
                waveform_points.extend([0.0] * (width - len(waveform_points)))
            
            return {
                "points": waveform_points,
                "duration": duration,
                "sample_rate": sr,
                "width": width,
                "height": height
            }
            
        except Exception as e:
            logger.error("Waveform generation failed", error=str(e), exc_info=True)
            return None
    
    def calculate_ramp_times(
        self,
        track_path: str,
        intro_duration: float = 5.0,
        outro_duration: float = 5.0
    ) -> Dict[str, float]:
        """
        Calculate intro and outro ramp times from track analysis
        
        Args:
            track_path: Path to audio file
            intro_duration: Expected intro duration in seconds
            outro_duration: Expected outro duration in seconds
        
        Returns:
            Dictionary with ramp_in and ramp_out times
        """
        try:
            if not os.path.exists(track_path):
                return {"ramp_in": intro_duration, "ramp_out": outro_duration}
            
            # Load audio
            y, sr = librosa.load(track_path, sr=22050)
            duration = librosa.get_duration(y=y, sr=sr)
            
            # Analyze intro (first N seconds)
            intro_samples = int(intro_duration * sr)
            if intro_samples > len(y):
                intro_samples = len(y)
            
            intro_section = y[:intro_samples]
            
            # Analyze outro (last N seconds)
            outro_samples = int(outro_duration * sr)
            if outro_samples > len(y):
                outro_samples = len(y)
            
            outro_section = y[-outro_samples:]
            
            # Calculate energy levels
            intro_energy = np.mean(np.abs(intro_section))
            outro_energy = np.mean(np.abs(outro_section))
            main_energy = np.mean(np.abs(y[intro_samples:-outro_samples if outro_samples < len(y) else len(y)]))
            
            # Determine ramp times based on energy levels
            # If intro/outro have lower energy, they're likely ramps
            ramp_in = intro_duration
            ramp_out = outro_duration
            
            if intro_energy < main_energy * 0.7:
                # Intro is quieter, likely a ramp
                ramp_in = intro_duration
            else:
                # Intro is loud, might be shorter ramp
                ramp_in = min(intro_duration, 3.0)
            
            if outro_energy < main_energy * 0.7:
                # Outro is quieter, likely a ramp
                ramp_out = outro_duration
            else:
                # Outro is loud, might be shorter ramp
                ramp_out = min(outro_duration, 3.0)
            
            return {
                "ramp_in": float(ramp_in),
                "ramp_out": float(ramp_out),
                "duration": float(duration)
            }
            
        except Exception as e:
            logger.error("Ramp time calculation failed", error=str(e), exc_info=True)
            return {"ramp_in": intro_duration, "ramp_out": outro_duration}
    
    def detect_intro_outro_points(
        self,
        track_path: str
    ) -> Dict[str, float]:
        """
        Detect intro and outro points in track
        
        Args:
            track_path: Path to audio file
        
        Returns:
            Dictionary with intro_start, intro_end, outro_start, outro_end
        """
        try:
            if not os.path.exists(track_path):
                return {}
            
            # Load audio
            y, sr = librosa.load(track_path, sr=22050)
            duration = librosa.get_duration(y=y, sr=sr)
            
            # Use onset detection for intro
            onset_frames = librosa.onset.onset_detect(y=y, sr=sr, units='time')
            
            intro_end = 0.0
            if len(onset_frames) > 0:
                # First significant onset is likely end of intro
                intro_end = float(onset_frames[0])
            
            # For outro, look at last portion
            outro_start = max(0.0, duration - 10.0)  # Last 10 seconds
            
            return {
                "intro_start": 0.0,
                "intro_end": intro_end,
                "outro_start": outro_start,
                "outro_end": duration
            }
            
        except Exception as e:
            logger.error("Intro/outro detection failed", error=str(e), exc_info=True)
            return {}

