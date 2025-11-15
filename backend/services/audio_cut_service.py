"""
AudioCutService for managing audio cuts with versioning, rotation, and expiration
"""

from typing import List, Optional, Dict, Any
from datetime import datetime, timezone, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_, func
from backend.models.audio_cut import AudioCut
from backend.models.audio_version import AudioVersion
from backend.models.copy import Copy
import structlog
import hashlib
import os

logger = structlog.get_logger()


class AudioCutService:
    """Service for managing audio cuts"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def create_cut(
        self,
        copy_id: int,
        cut_id: str,
        cut_name: Optional[str] = None,
        audio_file_path: Optional[str] = None,
        audio_file_url: Optional[str] = None,
        rotation_weight: float = 1.0,
        daypart_restrictions: Optional[List[int]] = None,
        program_associations: Optional[List[int]] = None,
        expires_at: Optional[datetime] = None,
        notes: Optional[str] = None,
        tags: Optional[List[str]] = None,
        created_by: Optional[int] = None
    ) -> AudioCut:
        """Create a new audio cut"""
        # Verify copy exists
        copy_result = await self.db.execute(select(Copy).where(Copy.id == copy_id))
        copy = copy_result.scalar_one_or_none()
        if not copy:
            raise ValueError(f"Copy with id {copy_id} not found")
        
        # Check if cut_id already exists for this copy
        existing = await self.db.execute(
            select(AudioCut).where(
                and_(
                    AudioCut.copy_id == copy_id,
                    AudioCut.cut_id == cut_id
                )
            )
        )
        if existing.scalar_one_or_none():
            raise ValueError(f"Cut ID '{cut_id}' already exists for copy {copy_id}")
        
        # Calculate file checksum if file exists
        file_checksum = None
        if audio_file_path and os.path.exists(audio_file_path):
            file_checksum = self._calculate_checksum(audio_file_path)
        
        # Create new cut
        cut = AudioCut(
            copy_id=copy_id,
            cut_id=cut_id,
            cut_name=cut_name,
            audio_file_path=audio_file_path,
            audio_file_url=audio_file_url,
            file_checksum=file_checksum,
            version=1,
            rotation_weight=rotation_weight,
            daypart_restrictions=daypart_restrictions,
            program_associations=program_associations,
            expires_at=expires_at,
            active=True,
            notes=notes,
            tags=tags,
            created_by=created_by
        )
        
        self.db.add(cut)
        
        # Update copy cut_count
        copy.cut_count = (copy.cut_count or 0) + 1
        await self.db.commit()
        await self.db.refresh(cut)
        
        logger.info("Audio cut created", cut_id=cut.id, copy_id=copy_id, cut_identifier=cut_id)
        return cut
    
    async def get_cut(self, cut_id: int) -> Optional[AudioCut]:
        """Get a cut by ID"""
        result = await self.db.execute(select(AudioCut).where(AudioCut.id == cut_id))
        return result.scalar_one_or_none()
    
    async def get_cuts_by_copy(self, copy_id: int, active_only: bool = False) -> List[AudioCut]:
        """Get all cuts for a copy"""
        query = select(AudioCut).where(AudioCut.copy_id == copy_id)
        if active_only:
            query = query.where(AudioCut.active == True)
        query = query.order_by(AudioCut.cut_id)
        
        result = await self.db.execute(query)
        return list(result.scalars().all())
    
    async def update_cut(
        self,
        cut_id: int,
        cut_name: Optional[str] = None,
        rotation_weight: Optional[float] = None,
        daypart_restrictions: Optional[List[int]] = None,
        program_associations: Optional[List[int]] = None,
        expires_at: Optional[datetime] = None,
        active: Optional[bool] = None,
        notes: Optional[str] = None,
        tags: Optional[List[str]] = None
    ) -> AudioCut:
        """Update a cut"""
        cut = await self.get_cut(cut_id)
        if not cut:
            raise ValueError(f"Cut with id {cut_id} not found")
        
        if cut_name is not None:
            cut.cut_name = cut_name
        if rotation_weight is not None:
            if rotation_weight < 0:
                raise ValueError("Rotation weight must be >= 0")
            cut.rotation_weight = rotation_weight
        if daypart_restrictions is not None:
            cut.daypart_restrictions = daypart_restrictions
        if program_associations is not None:
            cut.program_associations = program_associations
        if expires_at is not None:
            cut.expires_at = expires_at
        if active is not None:
            cut.active = active
        if notes is not None:
            cut.notes = notes
        if tags is not None:
            cut.tags = tags
        
        await self.db.commit()
        await self.db.refresh(cut)
        
        logger.info("Audio cut updated", cut_id=cut_id)
        return cut
    
    async def delete_cut(self, cut_id: int) -> bool:
        """Delete a cut"""
        cut = await self.get_cut(cut_id)
        if not cut:
            raise ValueError(f"Cut with id {cut_id} not found")
        
        copy_id = cut.copy_id
        
        # Delete associated versions
        versions_result = await self.db.execute(
            select(AudioVersion).where(AudioVersion.cut_id == cut_id)
        )
        versions = versions_result.scalars().all()
        for version in versions:
            await self.db.delete(version)
        
        await self.db.delete(cut)
        
        # Update copy cut_count
        copy_result = await self.db.execute(select(Copy).where(Copy.id == copy_id))
        copy = copy_result.scalar_one_or_none()
        if copy:
            copy.cut_count = max(0, (copy.cut_count or 0) - 1)
        
        await self.db.commit()
        
        logger.info("Audio cut deleted", cut_id=cut_id, copy_id=copy_id)
        return True
    
    async def create_version(
        self,
        cut_id: int,
        audio_file_path: Optional[str] = None,
        audio_file_url: Optional[str] = None,
        version_notes: Optional[str] = None,
        changed_by: Optional[int] = None
    ) -> AudioVersion:
        """Create a new version of a cut"""
        cut = await self.get_cut(cut_id)
        if not cut:
            raise ValueError(f"Cut with id {cut_id} not found")
        
        # Archive current version
        current_version = AudioVersion(
            cut_id=cut_id,
            version_number=cut.version,
            audio_file_path=cut.audio_file_path,
            audio_file_url=cut.audio_file_url,
            file_checksum=cut.file_checksum,
            version_notes=version_notes or "Previous version",
            changed_by=changed_by
        )
        self.db.add(current_version)
        
        # Update cut to new version
        cut.version += 1
        if audio_file_path:
            cut.audio_file_path = audio_file_path
            if os.path.exists(audio_file_path):
                cut.file_checksum = self._calculate_checksum(audio_file_path)
        if audio_file_url:
            cut.audio_file_url = audio_file_url
        
        await self.db.commit()
        await self.db.refresh(current_version)
        
        logger.info("Audio cut version created", cut_id=cut_id, new_version=cut.version)
        return current_version
    
    async def rollback_to_version(self, cut_id: int, version_number: int) -> AudioCut:
        """Rollback a cut to a previous version"""
        cut = await self.get_cut(cut_id)
        if not cut:
            raise ValueError(f"Cut with id {cut_id} not found")
        
        # Find the version to rollback to
        version_result = await self.db.execute(
            select(AudioVersion).where(
                and_(
                    AudioVersion.cut_id == cut_id,
                    AudioVersion.version_number == version_number
                )
            )
        )
        version = version_result.scalar_one_or_none()
        if not version:
            raise ValueError(f"Version {version_number} not found for cut {cut_id}")
        
        # Create new version with current state before rollback
        await self.create_version(
            cut_id=cut_id,
            version_notes=f"Rollback to version {version_number}"
        )
        
        # Restore from version
        cut.audio_file_path = version.audio_file_path
        cut.audio_file_url = version.audio_file_url
        cut.file_checksum = version.file_checksum
        
        await self.db.commit()
        await self.db.refresh(cut)
        
        logger.info("Audio cut rolled back", cut_id=cut_id, to_version=version_number)
        return cut
    
    async def get_versions(self, cut_id: int) -> List[AudioVersion]:
        """Get all versions for a cut"""
        result = await self.db.execute(
            select(AudioVersion)
            .where(AudioVersion.cut_id == cut_id)
            .order_by(AudioVersion.version_number.desc())
        )
        return list(result.scalars().all())
    
    async def get_active_cuts_for_rotation(
        self,
        copy_id: int,
        daypart_id: Optional[int] = None,
        program_id: Optional[int] = None
    ) -> List[AudioCut]:
        """Get active cuts for rotation, filtered by daypart/program if specified"""
        query = select(AudioCut).where(
            and_(
                AudioCut.copy_id == copy_id,
                AudioCut.active == True,
                or_(
                    AudioCut.expires_at.is_(None),
                    AudioCut.expires_at > datetime.now(timezone.utc)
                )
            )
        )
        
        if daypart_id:
            # Filter by daypart restrictions
            query = query.where(
                or_(
                    AudioCut.daypart_restrictions.is_(None),
                    func.jsonb_array_length(AudioCut.daypart_restrictions) == 0,
                    func.jsonb_exists(AudioCut.daypart_restrictions, str(daypart_id))
                )
            )
        
        if program_id:
            # Filter by program associations
            query = query.where(
                or_(
                    AudioCut.program_associations.is_(None),
                    func.jsonb_array_length(AudioCut.program_associations) == 0,
                    func.jsonb_exists(AudioCut.program_associations, str(program_id))
                )
            )
        
        query = query.order_by(AudioCut.cut_id)
        result = await self.db.execute(query)
        return list(result.scalars().all())
    
    async def select_cut_for_rotation(
        self,
        copy_id: int,
        rotation_mode: str,
        daypart_id: Optional[int] = None,
        program_id: Optional[int] = None,
        previous_cut_id: Optional[str] = None
    ) -> Optional[AudioCut]:
        """Select a cut for rotation based on rotation mode"""
        cuts = await self.get_active_cuts_for_rotation(copy_id, daypart_id, program_id)
        
        if not cuts:
            return None
        
        if rotation_mode == "SEQUENTIAL":
            # Play in order, skip previous if specified
            if previous_cut_id:
                # Find index of previous cut
                prev_index = next((i for i, c in enumerate(cuts) if c.cut_id == previous_cut_id), -1)
                if prev_index >= 0 and prev_index < len(cuts) - 1:
                    return cuts[prev_index + 1]
            return cuts[0]
        
        elif rotation_mode == "RANDOM":
            import random
            return random.choice(cuts)
        
        elif rotation_mode == "WEIGHTED":
            import random
            # Weighted random selection
            weights = [c.rotation_weight for c in cuts]
            total_weight = sum(weights)
            if total_weight == 0:
                return random.choice(cuts)
            
            r = random.uniform(0, total_weight)
            cumulative = 0
            for cut, weight in zip(cuts, weights):
                cumulative += weight
                if r <= cumulative:
                    return cut
            return cuts[-1]  # Fallback
        
        elif rotation_mode == "EVEN":
            # Even distribution - would need play history, for now use sequential
            if previous_cut_id:
                prev_index = next((i for i, c in enumerate(cuts) if c.cut_id == previous_cut_id), -1)
                if prev_index >= 0 and prev_index < len(cuts) - 1:
                    return cuts[prev_index + 1]
            return cuts[0]
        
        # Default to first cut
        return cuts[0]
    
    async def check_expiring_cuts(self, days_ahead: int = 7) -> List[AudioCut]:
        """Find cuts expiring soon"""
        cutoff_date = datetime.now(timezone.utc) + timedelta(days=days_ahead)
        
        result = await self.db.execute(
            select(AudioCut).where(
                and_(
                    AudioCut.expires_at.isnot(None),
                    AudioCut.expires_at <= cutoff_date,
                    AudioCut.expires_at > datetime.now(timezone.utc),
                    AudioCut.active == True
                )
            ).order_by(AudioCut.expires_at)
        )
        
        return list(result.scalars().all())
    
    def _calculate_checksum(self, file_path: str) -> str:
        """Calculate MD5 checksum of a file"""
        hash_md5 = hashlib.md5()
        try:
            with open(file_path, "rb") as f:
                for chunk in iter(lambda: f.read(4096), b""):
                    hash_md5.update(chunk)
            return hash_md5.hexdigest()
        except Exception as e:
            logger.error("Failed to calculate checksum", file_path=file_path, error=str(e))
            return ""

