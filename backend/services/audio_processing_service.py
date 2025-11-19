"""
Audio processing service for voice tracks
Handles ducking, mixdown, normalization, and LibreTime compatibility
"""

import os
import subprocess
from pathlib import Path
from typing import Optional, Dict, Any
from pydub import AudioSegment
from mutagen.id3 import ID3, TIT2, TPE1, TCON
from mutagen.mp3 import MP3
import structlog

logger = structlog.get_logger()


class AudioProcessingService:
    """Service for audio processing operations"""
    
    def __init__(self):
        self.ffmpeg_path = os.getenv("FFMPEG_PATH", "ffmpeg")
        self.ffprobe_path = os.getenv("FFPROBE_PATH", "ffprobe")
    
    def duck_music_bed(
        self,
        voice_track_path: str,
        music_track_path: str,
        output_path: str,
        threshold: float = -18.0
    ) -> bool:
        """
        Apply ducking to music bed under voice track
        
        Args:
            voice_track_path: Path to voice track audio file
            music_track_path: Path to music bed audio file
            output_path: Path to save ducked output
            threshold: Ducking threshold in dB (default -18dB)
        
        Returns:
            True if successful, False otherwise
        """
        try:
            # Load audio files
            voice = AudioSegment.from_file(voice_track_path)
            music = AudioSegment.from_file(music_track_path)
            
            # Ensure same sample rate and channels
            if voice.frame_rate != music.frame_rate:
                music = music.set_frame_rate(voice.frame_rate)
            if voice.channels != music.channels:
                music = music.set_channels(voice.channels)
            
            # Make music same length as voice (loop if needed)
            if len(music) < len(voice):
                # Loop music to match voice length
                loops_needed = (len(voice) // len(music)) + 1
                music = music * loops_needed
                music = music[:len(voice)]
            else:
                music = music[:len(voice)]
            
            # Apply ducking: reduce music volume when voice is present
            # Simple approach: reduce music by threshold amount
            ducked_music = music + threshold  # threshold is negative, so this reduces volume
            
            # Mix voice and ducked music
            mixed = voice.overlay(ducked_music)
            
            # Export
            mixed.export(output_path, format="mp3", bitrate="320k")
            
            logger.info("Ducking applied", voice_track=voice_track_path, threshold=threshold)
            return True
            
        except Exception as e:
            logger.error("Ducking failed", error=str(e), exc_info=True)
            return False
    
    def mix_voice_track(
        self,
        voice_track_path: str,
        previous_track_path: Optional[str],
        next_track_path: Optional[str],
        output_path: str,
        ramp_time: float = 5.0,
        back_time: float = 5.0
    ) -> bool:
        """
        Create full mixdown with previous and next tracks
        
        Args:
            voice_track_path: Path to voice track
            previous_track_path: Path to previous track (for outro)
            next_track_path: Path to next track (for intro)
            output_path: Path to save mixed output
            ramp_time: Seconds for intro ramp
            back_time: Seconds for outro ramp
        
        Returns:
            True if successful, False otherwise
        """
        try:
            voice = AudioSegment.from_file(voice_track_path)
            segments = []
            
            # Add previous track outro if provided
            if previous_track_path and os.path.exists(previous_track_path):
                prev = AudioSegment.from_file(previous_track_path)
                outro_ms = int(back_time * 1000)
                if len(prev) > outro_ms:
                    prev_outro = prev[-outro_ms:]
                    # Fade out
                    prev_outro = prev_outro.fade_out(int(back_time * 500))
                    segments.append(prev_outro)
            
            # Add voice track
            segments.append(voice)
            
            # Add next track intro if provided
            if next_track_path and os.path.exists(next_track_path):
                next_track = AudioSegment.from_file(next_track_path)
                intro_ms = int(ramp_time * 1000)
                if len(next_track) > intro_ms:
                    next_intro = next_track[:intro_ms]
                    # Fade in
                    next_intro = next_intro.fade_in(int(ramp_time * 500))
                    segments.append(next_intro)
            
            # Concatenate all segments
            mixed = sum(segments)
            
            # Export
            mixed.export(output_path, format="mp3", bitrate="320k")
            
            logger.info("Mixdown completed", output=output_path)
            return True
            
        except Exception as e:
            logger.error("Mixdown failed", error=str(e), exc_info=True)
            return False
    
    def normalize_audio(
        self,
        audio_file: str,
        output_path: Optional[str] = None,
        target_lufs: float = -23.0
    ) -> bool:
        """
        Normalize audio to target LUFS level
        
        Args:
            audio_file: Path to input audio file
            output_path: Path to save normalized output (if None, overwrites input)
            target_lufs: Target LUFS level (default -23.0)
        
        Returns:
            True if successful, False otherwise
        """
        try:
            if output_path is None:
                output_path = audio_file
            
            # Use ffmpeg for LUFS normalization
            cmd = [
                self.ffmpeg_path,
                "-i", audio_file,
                "-af", f"loudnorm=I={target_lufs}:TP=-1.5:LRA=11",
                "-y",  # Overwrite output file
                output_path
            ]
            
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                check=True
            )
            
            logger.info("Audio normalized", input=audio_file, target_lufs=target_lufs)
            return True
            
        except subprocess.CalledProcessError as e:
            logger.error("Normalization failed", error=e.stderr, exc_info=True)
            return False
        except Exception as e:
            logger.error("Normalization error", error=str(e), exc_info=True)
            return False
    
    def trim_audio(
        self,
        audio_file: str,
        start: float,
        end: float,
        output_path: str
    ) -> bool:
        """
        Trim audio file
        
        Args:
            audio_file: Path to input audio file
            start: Start time in seconds
            end: End time in seconds
            output_path: Path to save trimmed output
        
        Returns:
            True if successful, False otherwise
        """
        try:
            audio = AudioSegment.from_file(audio_file)
            start_ms = int(start * 1000)
            end_ms = int(end * 1000)
            
            trimmed = audio[start_ms:end_ms]
            trimmed.export(output_path, format="mp3", bitrate="320k")
            
            logger.info("Audio trimmed", input=audio_file, start=start, end=end)
            return True
            
        except Exception as e:
            logger.error("Trim failed", error=str(e), exc_info=True)
            return False
    
    def encode_for_libretime(
        self,
        audio_file: str,
        output_path: str,
        metadata: Dict[str, Any]
    ) -> bool:
        """
        Encode audio file to MP3 320kbps CBR with ID3 tags for LibreTime compatibility
        
        Args:
            audio_file: Path to input audio file
            output_path: Path to save encoded output
            metadata: Dictionary with title, artist, genre, etc.
        
        Returns:
            True if successful, False otherwise
        """
        try:
            # Load and convert to MP3 320kbps CBR
            audio = AudioSegment.from_file(audio_file)
            audio.export(
                output_path,
                format="mp3",
                bitrate="320k",
                parameters=["-b:a", "320k", "-ar", "44100"]  # CBR, 44.1kHz
            )
            
            # Add ID3 tags using mutagen
            self.add_id3_metadata(output_path, metadata)
            
            logger.info("Encoded for LibreTime", output=output_path, metadata=metadata)
            return True
            
        except Exception as e:
            logger.error("LibreTime encoding failed", error=str(e), exc_info=True)
            return False
    
    def add_id3_metadata(
        self,
        audio_file: str,
        metadata: Dict[str, Any]
    ) -> bool:
        """
        Add ID3 tags to MP3 file using mutagen
        
        Args:
            audio_file: Path to MP3 file
            metadata: Dictionary with title, artist, genre, etc.
        
        Returns:
            True if successful, False otherwise
        """
        try:
            # Try to load existing ID3 tags, create if doesn't exist
            try:
                audio = MP3(audio_file, ID3=ID3)
            except:
                audio = MP3(audio_file)
                audio.add(ID3())
            
            # Add title (required)
            if "title" in metadata and metadata["title"]:
                audio.tags.add(TIT2(encoding=3, text=metadata["title"]))
            
            # Add artist (recommended)
            if "artist" in metadata and metadata["artist"]:
                audio.tags.add(TPE1(encoding=3, text=metadata["artist"]))
            
            # Add genre (optional)
            if "genre" in metadata and metadata["genre"]:
                audio.tags.add(TCON(encoding=3, text=metadata["genre"]))
            else:
                # Default genre for voice tracks
                audio.tags.add(TCON(encoding=3, text="Voice Over Track"))
            
            # Save tags
            audio.save()
            
            logger.info("ID3 tags added", file=audio_file, metadata=metadata)
            return True
            
        except Exception as e:
            logger.error("ID3 tag addition failed", error=str(e), exc_info=True)
            return False
    
    def prepare_for_libretime_upload(
        self,
        audio_file: str,
        metadata: Dict[str, Any],
        output_path: Optional[str] = None
    ) -> Optional[str]:
        """
        Prepare audio file for LibreTime upload
        Ensures MP3 320kbps CBR format with proper ID3 tags
        
        Args:
            audio_file: Path to input audio file
            metadata: Dictionary with title (required), artist (recommended), genre (optional)
            output_path: Optional output path (if None, creates temp file)
        
        Returns:
            Path to prepared file, or None if failed
        """
        try:
            if output_path is None:
                # Create temp file with .mp3 extension
                base = Path(audio_file).stem
                output_path = str(Path(audio_file).parent / f"{base}_libretime.mp3")
            
            # Encode to MP3 320kbps CBR with ID3 tags
            success = self.encode_for_libretime(audio_file, output_path, metadata)
            
            if success:
                # Verify file exists and is valid
                if os.path.exists(output_path) and os.path.getsize(output_path) > 0:
                    logger.info("File prepared for LibreTime", file=output_path)
                    return output_path
                else:
                    logger.error("Prepared file is invalid", file=output_path)
                    return None
            else:
                return None
                
        except Exception as e:
            logger.error("LibreTime preparation failed", error=str(e), exc_info=True)
            return None
    
    def verify_libretime_compatibility(self, file_path: str) -> Dict[str, Any]:
        """
        Verify file meets LibreTime compatibility requirements
        
        Args:
            file_path: Path to audio file
        
        Returns:
            Dictionary with verification results
        """
        result = {
            "compatible": False,
            "format": None,
            "bitrate": None,
            "sample_rate": None,
            "channels": None,
            "has_id3_tags": False,
            "errors": []
        }
        
        try:
            if not os.path.exists(file_path):
                result["errors"].append("File does not exist")
                return result
            
            # Check file extension
            ext = Path(file_path).suffix.lower()
            if ext != ".mp3":
                result["errors"].append(f"File must be MP3, got {ext}")
                return result
            
            # Load file and check properties
            audio = AudioSegment.from_file(file_path)
            result["format"] = "mp3"
            result["sample_rate"] = audio.frame_rate
            result["channels"] = audio.channels
            
            # Check sample rate (should be 44.1kHz or 48kHz)
            if audio.frame_rate not in [44100, 48000]:
                result["errors"].append(f"Sample rate should be 44.1kHz or 48kHz, got {audio.frame_rate}Hz")
            
            # Check bitrate (should be 320kbps)
            # Note: pydub doesn't directly give bitrate, we'll check via ffprobe
            try:
                cmd = [
                    self.ffprobe_path,
                    "-v", "error",
                    "-select_streams", "a:0",
                    "-show_entries", "stream=bit_rate",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    file_path
                ]
                output = subprocess.run(cmd, capture_output=True, text=True, check=True)
                bitrate = int(output.stdout.strip())
                result["bitrate"] = bitrate
                if bitrate < 300000:  # Less than ~300kbps
                    result["errors"].append(f"Bitrate should be 320kbps CBR, got {bitrate}bps")
            except:
                result["errors"].append("Could not verify bitrate")
            
            # Check ID3 tags
            try:
                audio_mp3 = MP3(file_path, ID3=ID3)
                if audio_mp3.tags:
                    result["has_id3_tags"] = True
                    # Check for required title tag
                    if "TIT2" not in audio_mp3.tags:
                        result["errors"].append("Missing required ID3 title tag")
                else:
                    result["errors"].append("No ID3 tags found")
            except:
                result["errors"].append("Could not read ID3 tags")
            
            # If no errors, file is compatible
            if not result["errors"]:
                result["compatible"] = True
            
            return result
            
        except Exception as e:
            result["errors"].append(f"Verification error: {str(e)}")
            logger.error("Compatibility verification failed", error=str(e), exc_info=True)
            return result

