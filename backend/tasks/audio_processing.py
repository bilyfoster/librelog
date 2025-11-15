"""
Celery tasks for audio processing and QC automation
"""

from celery import shared_task
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.services.audio_qc_service import AudioQCService
from backend.models.audio_cut import AudioCut
from sqlalchemy import select
import structlog

logger = structlog.get_logger()


@shared_task
async def process_audio_qc(cut_id: int):
    """Run QC on an audio cut"""
    async with AsyncSessionLocal() as db:
        try:
            # Get cut
            result = await db.execute(select(AudioCut).where(AudioCut.id == cut_id))
            cut = result.scalar_one_or_none()
            
            if not cut or not cut.audio_file_path:
                logger.warning("Cut not found or has no audio file", cut_id=cut_id)
                return
            
            # Run QC
            qc_service = AudioQCService(db)
            qc_result = await qc_service.run_qc(cut.audio_file_path, cut_id=cut_id)
            
            logger.info("QC processing completed", cut_id=cut_id, qc_passed=qc_result.qc_passed)
            
        except Exception as e:
            logger.error("QC processing failed", cut_id=cut_id, error=str(e), exc_info=True)
            raise


@shared_task
async def auto_trim_silence_task(cut_id: int, head_threshold: float = 0.1, tail_threshold: float = 0.1):
    """Auto-trim silence from an audio cut"""
    async with AsyncSessionLocal() as db:
        try:
            # Get cut
            result = await db.execute(select(AudioCut).where(AudioCut.id == cut_id))
            cut = result.scalar_one_or_none()
            
            if not cut or not cut.audio_file_path:
                logger.warning("Cut not found or has no audio file", cut_id=cut_id)
                return
            
            # Trim silence
            qc_service = AudioQCService(db)
            from pathlib import Path
            output_path = str(Path(cut.audio_file_path).with_suffix('.trimmed' + Path(cut.audio_file_path).suffix))
            
            trimmed_path = await qc_service.auto_trim_silence(
                cut.audio_file_path,
                output_path,
                head_threshold,
                tail_threshold
            )
            
            # Update cut with trimmed file
            cut.audio_file_path = trimmed_path
            await db.commit()
            
            logger.info("Silence trimming completed", cut_id=cut_id, output_path=trimmed_path)
            
        except Exception as e:
            logger.error("Silence trimming failed", cut_id=cut_id, error=str(e), exc_info=True)
            raise


@shared_task
async def normalize_audio_task(cut_id: int, method: str = "PEAK", target_level: float = -3.0):
    """Normalize an audio cut"""
    async with AsyncSessionLocal() as db:
        try:
            # Get cut
            result = await db.execute(select(AudioCut).where(AudioCut.id == cut_id))
            cut = result.scalar_one_or_none()
            
            if not cut or not cut.audio_file_path:
                logger.warning("Cut not found or has no audio file", cut_id=cut_id)
                return
            
            # Normalize
            qc_service = AudioQCService(db)
            from pathlib import Path
            output_path = str(Path(cut.audio_file_path).with_suffix('.normalized' + Path(cut.audio_file_path).suffix))
            
            normalized_path = await qc_service.normalize_audio(
                cut.audio_file_path,
                output_path,
                method,
                target_level
            )
            
            # Update cut with normalized file
            cut.audio_file_path = normalized_path
            await db.commit()
            
            logger.info("Audio normalization completed", cut_id=cut_id, method=method, output_path=normalized_path)
            
        except Exception as e:
            logger.error("Audio normalization failed", cut_id=cut_id, error=str(e), exc_info=True)
            raise

