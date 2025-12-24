"""
AudioQCService for audio quality control and validation
"""

from typing import Dict, Any, Optional, List
from uuid import UUID
from pathlib import Path
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.models.audio_qc_result import AudioQCResult
from backend.models.audio_cut import AudioCut
import structlog
import os

logger = structlog.get_logger()

# Try to import audio processing libraries
try:
    from pydub import AudioSegment
    from pydub.utils import mediainfo
    PYDUB_AVAILABLE = True
except ImportError:
    PYDUB_AVAILABLE = False
    logger.warning("pydub not available, audio QC features will be limited")

try:
    import librosa
    LIBROSA_AVAILABLE = True
except ImportError:
    LIBROSA_AVAILABLE = False
    logger.warning("librosa not available, advanced audio analysis will be limited")

try:
    import numpy as np
    NUMPY_AVAILABLE = True
except ImportError:
    NUMPY_AVAILABLE = False
    logger.warning("numpy not available, some audio analysis features will be limited")

try:
    from mutagen import File as MutagenFile
    MUTAGEN_AVAILABLE = True
except ImportError:
    MUTAGEN_AVAILABLE = False
    logger.warning("mutagen not available, metadata extraction will be limited")


class AudioQCService:
    """Service for audio quality control"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        # Default thresholds (can be configured)
        self.silence_threshold_db = -40  # dB threshold for silence detection
        self.min_volume_threshold = -30  # dB minimum volume
        self.clipping_threshold = 0.99  # Clipping threshold (0-1)
        self.lufs_target = -16  # Target LUFS level
    
    async def run_qc(
        self,
        audio_file_path: str,
        cut_id: Optional[UUID] = None,
        version_id: Optional[UUID] = None,
        auto_fix: bool = False
    ) -> AudioQCResult:
        """Run comprehensive QC on an audio file"""
        if not os.path.exists(audio_file_path):
            raise ValueError(f"Audio file not found: {audio_file_path}")
        
        qc_data = {}
        warnings = []
        errors = []
        
        # Basic file validation
        format_valid, format_type, format_errors = self._validate_format(audio_file_path)
        if not format_valid:
            errors.extend(format_errors)
        
        # Load audio for analysis
        audio_segment = None
        audio_array = None
        sample_rate = None
        
        if PYDUB_AVAILABLE:
            try:
                audio_segment = AudioSegment.from_file(audio_file_path)
                sample_rate = audio_segment.frame_rate
                duration_seconds = len(audio_segment) / 1000.0
            except Exception as e:
                errors.append(f"Failed to load audio file: {str(e)}")
                logger.error("Failed to load audio", file_path=audio_file_path, error=str(e))
        
        if LIBROSA_AVAILABLE and not audio_array:
            try:
                audio_array, sample_rate = librosa.load(audio_file_path, sr=None)
                duration_seconds = len(audio_array) / sample_rate
            except Exception as e:
                if not audio_segment:
                    errors.append(f"Failed to load audio with librosa: {str(e)}")
                logger.warning("Failed to load audio with librosa", file_path=audio_file_path, error=str(e))
        
        # Extract metadata
        metadata = self._extract_metadata(audio_file_path)
        bitrate = metadata.get('bitrate', 0)
        channels = metadata.get('channels', 0)
        
        # Silence detection
        silence_at_head = 0.0
        silence_at_tail = 0.0
        silence_detected = False
        
        if audio_segment:
            silence_at_head, silence_at_tail = self._detect_silence(audio_segment)
            silence_detected = silence_at_head > 0.1 or silence_at_tail > 0.1
            if silence_detected:
                warnings.append(f"Silence detected: {silence_at_head:.2f}s at head, {silence_at_tail:.2f}s at tail")
        
        # Volume analysis
        peak_level = None
        rms_level = None
        lufs_level = None
        
        if audio_segment:
            peak_level = audio_segment.max_possible_amplitude
            rms_level = audio_segment.rms
            # Convert to dB (pydub already provides dBFS)
            try:
                peak_level = audio_segment.max_dBFS if hasattr(audio_segment, 'max_dBFS') else 0
                rms_level = audio_segment.rms if hasattr(audio_segment, 'rms') else 0
            except:
                peak_level = 0
                rms_level = 0
        
        if LIBROSA_AVAILABLE and NUMPY_AVAILABLE and audio_array is not None:
            # Calculate LUFS (simplified - full LUFS requires more complex calculation)
            try:
                # Simple loudness approximation
                rms = np.sqrt(np.mean(audio_array**2))
                if rms > 0:
                    lufs_level = 20 * np.log10(rms) - 0.691  # Rough approximation
            except Exception as e:
                logger.warning("Failed to calculate LUFS", error=str(e))
        
        # Clipping detection
        clipping_detected = False
        clipping_samples = 0
        
        if audio_array is not None:
            clipping_detected, clipping_samples = self._detect_clipping(audio_array)
            if clipping_detected:
                errors.append(f"Clipping detected: {clipping_samples} samples")
        
        # Volume threshold check
        volume_threshold_passed = True
        minimum_volume = min(peak_level or 0, rms_level or 0)
        if minimum_volume < self.min_volume_threshold:
            volume_threshold_passed = False
            warnings.append(f"Volume below threshold: {minimum_volume:.2f} dB (min: {self.min_volume_threshold} dB)")
        
        # File corruption check
        file_corrupted = False
        corruption_details = None
        
        if audio_segment:
            # Try to access audio data to check for corruption
            try:
                _ = len(audio_segment)
            except Exception as e:
                file_corrupted = True
                corruption_details = str(e)
                errors.append(f"File appears corrupted: {str(e)}")
        
        # Compile QC data
        qc_data = {
            "duration_seconds": duration_seconds if 'duration_seconds' in locals() else None,
            "sample_rate": sample_rate,
            "bitrate": bitrate,
            "channels": channels,
            "format_type": format_type,
            "silence_at_head": silence_at_head,
            "silence_at_tail": silence_at_tail,
            "peak_level": peak_level,
            "rms_level": rms_level,
            "lufs_level": lufs_level,
            "clipping_samples": clipping_samples,
            "minimum_volume": minimum_volume,
            "metadata": metadata
        }
        
        # Determine overall QC status
        qc_passed = len(errors) == 0 and volume_threshold_passed
        
        # Create QC result
        qc_result = AudioQCResult(
            cut_id=cut_id,
            version_id=version_id,
            qc_data=qc_data,
            duration_seconds=duration_seconds if 'duration_seconds' in locals() else None,
            sample_rate=sample_rate,
            bitrate=bitrate,
            channels=channels,
            silence_at_head=silence_at_head,
            silence_at_tail=silence_at_tail,
            silence_detected=silence_detected,
            peak_level=peak_level,
            rms_level=rms_level,
            lufs_level=lufs_level,
            clipping_detected=clipping_detected,
            clipping_samples=clipping_samples,
            volume_threshold_passed=volume_threshold_passed,
            minimum_volume=minimum_volume,
            format_valid=format_valid,
            format_type=format_type,
            format_errors="; ".join(format_errors) if format_errors else None,
            file_corrupted=file_corrupted,
            corruption_details=corruption_details,
            qc_passed=qc_passed,
            qc_warnings=warnings,
            qc_errors=errors
        )
        
        self.db.add(qc_result)
        await self.db.commit()
        await self.db.refresh(qc_result)
        
        logger.info("QC completed", file_path=audio_file_path, passed=qc_passed, warnings=len(warnings), errors=len(errors))
        
        return qc_result
    
    def _validate_format(self, file_path: str):
        """Validate audio file format"""
        errors = []
        file_ext = Path(file_path).suffix.lower()
        
        # Allowed formats
        allowed_formats = {'.mp3', '.wav', '.m4a', '.ogg', '.flac', '.aac'}
        if file_ext not in allowed_formats:
            errors.append(f"Unsupported format: {file_ext}")
            return False, None, errors
        
        # Check if file is readable
        if MUTAGEN_AVAILABLE:
            try:
                audio_file = MutagenFile(file_path)
                if audio_file is None:
                    errors.append("File format not recognized")
                    return False, file_ext, errors
            except Exception as e:
                errors.append(f"Failed to read file metadata: {str(e)}")
                return False, file_ext, errors
        
        return True, file_ext[1:].upper(), errors
    
    def _extract_metadata(self, file_path: str) -> Dict[str, Any]:
        """Extract audio metadata"""
        metadata = {}
        
        if MUTAGEN_AVAILABLE:
            try:
                audio_file = MutagenFile(file_path)
                if audio_file:
                    metadata['bitrate'] = getattr(audio_file.info, 'bitrate', 0) // 1000 if hasattr(audio_file.info, 'bitrate') else 0
                    metadata['channels'] = getattr(audio_file.info, 'channels', 0) if hasattr(audio_file.info, 'channels') else 0
                    metadata['sample_rate'] = getattr(audio_file.info, 'sample_rate', 0) if hasattr(audio_file.info, 'sample_rate') else 0
            except Exception as e:
                logger.warning("Failed to extract metadata", file_path=file_path, error=str(e))
        
        if PYDUB_AVAILABLE:
            try:
                info = mediainfo(file_path)
                if info:
                    metadata.update({
                        'bitrate': int(info.get('bit_rate', 0)) // 1000 if info.get('bit_rate') else 0,
                        'channels': int(info.get('channels', 0)) if info.get('channels') else 0,
                        'sample_rate': int(info.get('sample_rate', 0)) if info.get('sample_rate') else 0,
                    })
            except Exception as e:
                logger.warning("Failed to extract metadata with pydub", file_path=file_path, error=str(e))
        
        return metadata
    
    def _detect_silence(self, audio_segment) -> tuple[float, float]:
        """Detect silence at head and tail"""
        if not PYDUB_AVAILABLE:
            return 0.0, 0.0
        
        silence_threshold = audio_segment.max_possible_amplitude * (10 ** (self.silence_threshold_db / 20))
        
        # Detect silence at head
        head_silence = 0.0
        for i in range(0, min(1000, len(audio_segment)), 10):  # Check first second in 10ms chunks
            chunk = audio_segment[i:i+10]
            if chunk.max_possible_amplitude < silence_threshold:
                head_silence += 0.01
            else:
                break
        
        # Detect silence at tail
        tail_silence = 0.0
        for i in range(len(audio_segment) - 10, max(0, len(audio_segment) - 1000), -10):
            chunk = audio_segment[i:i+10]
            if chunk.max_possible_amplitude < silence_threshold:
                tail_silence += 0.01
            else:
                break
        
        return head_silence, tail_silence
    
    def _detect_clipping(self, audio_array) -> tuple[bool, int]:
        """Detect clipping in audio"""
        if audio_array is None or not NUMPY_AVAILABLE:
            return False, 0
        
        clipped = np.abs(audio_array) >= self.clipping_threshold
        clipping_samples = int(np.sum(clipped))
        return clipping_samples > 0, clipping_samples
    
    async def auto_trim_silence(
        self,
        input_path: str,
        output_path: str,
        head_threshold: float = 0.1,
        tail_threshold: float = 0.1
    ) -> str:
        """Auto-trim silence from audio file"""
        if not PYDUB_AVAILABLE:
            raise ValueError("pydub not available for silence trimming")
        
        try:
            audio = AudioSegment.from_file(input_path)
            
            # Trim silence at head
            if head_threshold > 0:
                audio = audio.strip_silence(silence_len=100, silence_thresh=self.silence_threshold_db)
            
            # Export trimmed audio
            audio.export(output_path, format=Path(output_path).suffix[1:])
            
            logger.info("Silence trimmed", input_path=input_path, output_path=output_path)
            return output_path
        except Exception as e:
            logger.error("Failed to trim silence", input_path=input_path, error=str(e))
            raise
    
    async def normalize_audio(
        self,
        input_path: str,
        output_path: str,
        method: str = "PEAK",
        target_level: float = -3.0
    ) -> str:
        """Normalize audio to target level"""
        if not PYDUB_AVAILABLE:
            raise ValueError("pydub not available for normalization")
        
        try:
            audio = AudioSegment.from_file(input_path)
            
            if method == "PEAK":
                # Normalize to peak level
                change_in_dB = target_level - audio.max_dBFS
                audio = audio.apply_gain(change_in_dB)
            elif method == "RMS":
                # Normalize to RMS level (simplified)
                change_in_dB = target_level - audio.rms
                audio = audio.apply_gain(change_in_dB)
            # LUFS normalization would require more complex calculation
            
            # Export normalized audio
            audio.export(output_path, format=Path(output_path).suffix[1:])
            
            logger.info("Audio normalized", input_path=input_path, output_path=output_path, method=method)
            return output_path
        except Exception as e:
            logger.error("Failed to normalize audio", input_path=input_path, error=str(e))
            raise
    
    async def get_qc_result(self, qc_result_id: UUID) -> Optional[AudioQCResult]:
        """Get a QC result by ID"""
        result = await self.db.execute(select(AudioQCResult).where(AudioQCResult.id == qc_result_id))
        return result.scalar_one_or_none()
    
    async def get_qc_results_for_cut(self, cut_id: UUID) -> List[AudioQCResult]:
        """Get all QC results for a cut"""
        result = await self.db.execute(
            select(AudioQCResult)
            .where(AudioQCResult.cut_id == cut_id)
            .order_by(AudioQCResult.created_at.desc())
        )
        return list(result.scalars().all())
    
    async def override_qc(
        self,
        qc_result_id: UUID,
        overridden_by: int,
        override_reason: str
    ) -> AudioQCResult:
        """Override a QC failure"""
        qc_result = await self.get_qc_result(qc_result_id)
        if not qc_result:
            raise ValueError(f"QC result with id {qc_result_id} not found")
        
        qc_result.overridden = True
        qc_result.overridden_by = overridden_by
        qc_result.override_reason = override_reason
        qc_result.override_timestamp = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(qc_result)
        
        logger.info("QC result overridden", qc_result_id=qc_result_id, overridden_by=overridden_by)
        return qc_result

