from sqlalchemy import Column, String, Integer, DateTime, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.core.database import Base
import uuid


class ISRC(Base):
    __tablename__ = "isrcs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    submission_id = Column(UUID(as_uuid=True), ForeignKey("submissions.id", ondelete="CASCADE"), nullable=False, index=True)
    isrc_code = Column(String(12), unique=True, nullable=False, index=True)
    country_code = Column(String(2), nullable=False)
    registrant_code = Column(String(3), nullable=False)
    year = Column(Integer, nullable=False)
    sequence_number = Column(Integer, nullable=False)
    assigned_at = Column(DateTime(timezone=True), server_default=func.now())
    assigned_by = Column(UUID(as_uuid=True), ForeignKey("admin_users.id"))
    certificate_path = Column(String(500))
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    submission = relationship("Submission", back_populates="isrc")

