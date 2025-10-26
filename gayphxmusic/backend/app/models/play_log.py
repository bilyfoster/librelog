from sqlalchemy import Column, String, Boolean, DateTime, ForeignKey, Text, JSON, Integer, Numeric, Index
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.core.database import Base
import uuid


class PlayLog(Base):
    __tablename__ = "play_logs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    submission_id = Column(UUID(as_uuid=True), ForeignKey("submissions.id"), nullable=False)
    
    # Play details
    played_at = Column(DateTime(timezone=True), nullable=False, index=True)
    duration_played = Column(Integer)  # Seconds played
    play_type = Column(String(20), nullable=False)  # radio, podcast, commercial, etc.
    source = Column(String(50), nullable=False)  # libretime, manual, api, etc.
    
    # LibreTime integration
    libretime_play_id = Column(String(100))  # LibreTime's play ID
    libretime_show_id = Column(String(100))  # LibreTime's show ID
    libretime_show_name = Column(String(255))  # Show name from LibreTime
    
    # Play context
    dj_name = Column(String(255))  # DJ who played it
    show_name = Column(String(255))  # Show name
    time_slot = Column(String(50))  # Morning, Afternoon, Evening, etc.
    
    # Additional data
    extra_data = Column(JSON, default=dict)  # Additional context from LibreTime
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    submission = relationship("Submission")
    
    # Indexes for performance
    __table_args__ = (
        Index('idx_play_logs_submission_played_at', 'submission_id', 'played_at'),
        Index('idx_play_logs_played_at_type', 'played_at', 'play_type'),
        Index('idx_play_logs_libretime_id', 'libretime_play_id'),
    )


class PlayStatistics(Base):
    __tablename__ = "play_statistics"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    submission_id = Column(UUID(as_uuid=True), ForeignKey("submissions.id"), unique=True, nullable=False)
    
    # Play counts
    total_plays = Column(Integer, default=0)
    radio_plays = Column(Integer, default=0)
    podcast_plays = Column(Integer, default=0)
    commercial_plays = Column(Integer, default=0)
    
    # Time-based statistics
    plays_this_week = Column(Integer, default=0)
    plays_this_month = Column(Integer, default=0)
    plays_this_year = Column(Integer, default=0)
    
    # Peak times
    most_played_hour = Column(Integer)  # 0-23
    most_played_day = Column(Integer)   # 0-6 (Monday-Sunday)
    
    # Last play info
    last_played_at = Column(DateTime(timezone=True))
    last_played_by = Column(String(255))  # DJ or show name
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    submission = relationship("Submission")


class LibreTimeIntegration(Base):
    __tablename__ = "libretime_integration"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    
    # LibreTime connection details
    libretime_url = Column(String(255), nullable=False)
    api_key = Column(String(255), nullable=False)
    last_sync_at = Column(DateTime(timezone=True))
    sync_status = Column(String(20), default="active")  # active, paused, error
    
    # Sync settings
    sync_interval_minutes = Column(Integer, default=15)  # How often to sync
    auto_sync_enabled = Column(Boolean, default=True)
    
    # Error tracking
    last_error = Column(Text)
    error_count = Column(Integer, default=0)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

