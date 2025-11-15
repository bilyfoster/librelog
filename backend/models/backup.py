"""
Backup model for tracking backup history and restore points
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey, Text, Enum as SQLEnum, Float
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class BackupType(str, enum.Enum):
    """Backup type enumeration"""
    FULL = "FULL"  # Full database and files backup
    DATABASE = "DATABASE"  # Database only
    FILES = "FILES"  # Files only


class BackupStatus(str, enum.Enum):
    """Backup status enumeration"""
    PENDING = "PENDING"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


class StorageProvider(str, enum.Enum):
    """Storage provider enumeration"""
    LOCAL = "LOCAL"
    S3 = "S3"
    BACKBLAZE_B2 = "BACKBLAZE_B2"


class Backup(Base):
    """Backup model for tracking backup history"""
    __tablename__ = "backups"

    id = Column(Integer, primary_key=True, index=True)
    backup_type = Column(SQLEnum(BackupType), nullable=False, default=BackupType.FULL)
    status = Column(SQLEnum(BackupStatus), nullable=False, default=BackupStatus.PENDING, index=True)
    storage_provider = Column(SQLEnum(StorageProvider), nullable=False, default=StorageProvider.LOCAL)
    
    # Backup metadata
    filename = Column(String(255), nullable=False)
    file_path = Column(Text, nullable=True)  # Local path
    remote_path = Column(Text, nullable=True)  # Remote path (S3/B2)
    file_size = Column(Float, nullable=True)  # Size in bytes
    
    # Backup details
    database_dump = Column(Boolean, default=True)
    files_included = Column(Boolean, default=True)
    description = Column(Text, nullable=True)
    
    # Execution info
    started_at = Column(DateTime(timezone=True), nullable=True)
    completed_at = Column(DateTime(timezone=True), nullable=True)
    error_message = Column(Text, nullable=True)
    
    # Metadata
    created_by = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    
    # Relationships
    creator = relationship("User", foreign_keys=[created_by])

    def __repr__(self):
        return f"<Backup(id={self.id}, type='{self.backup_type}', status='{self.status}')>"

