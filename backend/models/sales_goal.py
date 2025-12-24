"""
Sales Goal model for revenue management
"""

from sqlalchemy import Column, String, DateTime, Date, Numeric, ForeignKey, Enum as SQLEnum, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class PeriodType(str, enum.Enum):
    """Period type enumeration"""
    DAILY = "DAILY"
    WEEKLY = "WEEKLY"
    MONTHLY = "MONTHLY"
    YEARLY = "YEARLY"


class SalesGoal(Base):
    """Sales Goal model for tracking sales targets"""
    __tablename__ = "sales_goals"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    sales_rep_id = Column(UUID(as_uuid=True), ForeignKey("sales_reps.id"), nullable=False, index=True)
    period = Column(SQLEnum(PeriodType), nullable=False, index=True)
    target_date = Column(Date, nullable=False, index=True)
    goal_amount = Column(Numeric(10, 2), nullable=False)
    actual_amount = Column(Numeric(10, 2), default=0)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    sales_rep = relationship("SalesRep", back_populates="sales_goals")

    def __repr__(self):
        return f"<SalesGoal(sales_rep_id={self.sales_rep_id}, period='{self.period}', goal_amount={self.goal_amount})>"

