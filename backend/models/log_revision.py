"""
Log Revision model for tracking log changes
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class LogRevision(Base):
    """Log Revision model for tracking log version history"""
    __tablename__ = "log_revisions"

    id = Column(Integer, primary_key=True, index=True)
    log_id = Column(Integer, ForeignKey("daily_logs.id"), nullable=False, index=True)
    revision_number = Column(Integer, nullable=False, index=True)
    json_data = Column(JSONB, nullable=False)  # Snapshot of log data at this revision
    changed_by = Column(Integer, ForeignKey("users.id"), nullable=False)
    change_summary = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    # Relationships
    log = relationship("DailyLog", back_populates="revisions")
    changed_by_user = relationship("User", foreign_keys=[changed_by])

    def __repr__(self):
        return f"<LogRevision(log_id={self.log_id}, revision_number={self.revision_number})>"

