"""
Data migration script to convert existing Copy records to multi-cut structure
"""

import asyncio
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.models.copy import Copy
from backend.models.audio_cut import AudioCut
from sqlalchemy import select
import structlog
import os

logger = structlog.get_logger()


async def migrate_copy_to_multicut():
    """Migrate existing Copy records to new multi-cut structure"""
    async with AsyncSessionLocal() as db:
        try:
            # Get all copy records that have audio files but no cuts
            result = await db.execute(
                select(Copy).where(
                    Copy.audio_file_path.isnot(None)
                )
            )
            copy_records = result.scalars().all()
            
            migrated_count = 0
            
            for copy_record in copy_records:
                # Check if cuts already exist for this copy
                cuts_result = await db.execute(
                    select(AudioCut).where(AudioCut.copy_id == copy_record.id)
                )
                existing_cuts = cuts_result.scalars().all()
                
                if existing_cuts:
                    logger.info("Copy already has cuts, skipping", copy_id=copy_record.id)
                    continue
                
                # Create a default cut (Cut A) from existing audio file
                if copy_record.audio_file_path and os.path.exists(copy_record.audio_file_path):
                    default_cut = AudioCut(
                        copy_id=copy_record.id,
                        cut_id="A",  # Default to "A"
                        cut_name=f"{copy_record.title} - Cut A",
                        audio_file_path=copy_record.audio_file_path,
                        audio_file_url=copy_record.audio_file_url,
                        version=copy_record.version or 1,
                        rotation_weight=1.0,
                        active=copy_record.active,
                        expires_at=copy_record.expires_at,
                        notes="Migrated from legacy copy structure"
                    )
                    
                    db.add(default_cut)
                    copy_record.cut_count = 1
                    migrated_count += 1
                    
                    logger.info("Created default cut for copy", copy_id=copy_record.id, cut_id="A")
            
            await db.commit()
            logger.info("Copy migration completed", migrated_count=migrated_count, total_records=len(copy_records))
            
        except Exception as e:
            logger.error("Copy migration failed", error=str(e), exc_info=True)
            await db.rollback()
            raise


if __name__ == "__main__":
    asyncio.run(migrate_copy_to_multicut())

