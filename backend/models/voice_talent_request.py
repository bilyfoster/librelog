"""
VoiceTalentRequest model for managing voice talent requests and takes
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, ForeignKey, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class TalentType(str, enum.Enum):
    """Talent type enumeration"""
    MALE = "male"
    FEMALE = "female"
    CHARACTER = "character"
    AE_VOICE = "ae_voice"
    ANY = "any"


class TalentRequestStatus(str, enum.Enum):
    """Talent request status enumeration"""
    PENDING = "PENDING"
    ASSIGNED = "ASSIGNED"
    RECORDING = "RECORDING"
    UPLOADED = "UPLOADED"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"


class VoiceTalentRequest(Base):
    """VoiceTalentRequest model for managing voice talent requests"""
    __tablename__ = "voice_talent_requests"

    id = Column(Integer, primary_key=True, index=True)
    production_order_id = Column(Integer, ForeignKey("production_orders.id"), nullable=False, index=True)
    
    # Talent assignment
    talent_user_id = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)
    talent_type = Column(SQLEnum(TalentType), nullable=False)
    
    # Script and instructions
    script = Column(Text, nullable=False)
    pronunciation_guides = Column(Text, nullable=True)  # Pronunciation notes
    talent_instructions = Column(Text, nullable=True)  # Specific instructions for talent
    
    # Status
    status = Column(SQLEnum(TalentRequestStatus), nullable=False, default=TalentRequestStatus.PENDING, index=True)
    
    # Takes (stored as JSONB array of take objects)
    takes = Column(JSONB, nullable=True)  # [{take_number: 1, file_path: "...", uploaded_at: "...", approved: false}, ...]
    
    # Timestamps
    assigned_at = Column(DateTime(timezone=True), nullable=True)
    deadline = Column(DateTime(timezone=True), nullable=True, index=True)
    completed_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    production_order = relationship("ProductionOrder", back_populates="voice_talent_requests")
    talent_user = relationship("User", foreign_keys=[talent_user_id])

    def __repr__(self):
        return f"<VoiceTalentRequest(production_order_id={self.production_order_id}, talent_type='{self.talent_type}', status='{self.status}')>"

