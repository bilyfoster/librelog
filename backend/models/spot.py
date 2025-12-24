"""
Spot model for traffic scheduling
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, ForeignKey, Enum as SQLEnum, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class BreakPosition(str, enum.Enum):
    """Break position enumeration"""
    A = "A"
    B = "B"
    C = "C"
    D = "D"
    E = "E"


class Daypart(str, enum.Enum):
    """Daypart enumeration"""
    MORNING_DRIVE = "MORNING_DRIVE"  # 6am-10am
    MIDDAY = "MIDDAY"  # 10am-3pm
    AFTERNOON_DRIVE = "AFTERNOON_DRIVE"  # 3pm-7pm
    EVENING = "EVENING"  # 7pm-12am
    OVERNIGHT = "OVERNIGHT"  # 12am-6am


class SpotStatus(str, enum.Enum):
    """Spot status enumeration"""
    SCHEDULED = "SCHEDULED"
    AIRED = "AIRED"
    MISSED = "MISSED"
    MAKEGOOD = "MAKEGOOD"


class Spot(Base):
    """Spot model for individual scheduled advertising spots"""
    __tablename__ = "spots"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=False, index=True)
    campaign_id = Column(UUID(as_uuid=True), ForeignKey("campaigns.id"), nullable=True, index=True)
    station_id = Column(UUID(as_uuid=True), ForeignKey("stations.id"), nullable=False, index=True)
    scheduled_date = Column(Date, nullable=False, index=True)
    scheduled_time = Column(String(8), nullable=False)  # HH:MM:SS format
    spot_length = Column(Integer, nullable=False)  # Length in seconds
    break_position = Column(SQLEnum(BreakPosition), nullable=True)
    daypart = Column(SQLEnum(Daypart), nullable=True, index=True)
    status = Column(SQLEnum(SpotStatus), nullable=False, default=SpotStatus.SCHEDULED, index=True)
    actual_air_time = Column(DateTime(timezone=True), nullable=True)
    makegood_of_id = Column(UUID(as_uuid=True), ForeignKey("spots.id"), nullable=True)
    conflict_resolved = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    order = relationship("Order", back_populates="spots")
    campaign = relationship("Campaign", back_populates="spots")
    station = relationship("Station", back_populates="spots")
    copy_assignment = relationship("CopyAssignment", back_populates="spot", uselist=False)
    makegood_of = relationship("Spot", remote_side=[id], backref="makegoods")

    def __repr__(self):
        return f"<Spot(order_id={self.order_id}, scheduled_date='{self.scheduled_date}', scheduled_time='{self.scheduled_time}')>"

