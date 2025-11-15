"""
Sales Rep model for traffic management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Numeric, ForeignKey
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class SalesRep(Base):
    """Sales Rep model for managing sales representatives"""
    __tablename__ = "sales_reps"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, unique=True, index=True)
    employee_id = Column(String(50), unique=True, index=True)
    commission_rate = Column(Numeric(5, 2))  # Percentage, e.g., 5.00 for 5%
    sales_goal = Column(Numeric(10, 2))  # Monthly or annual goal
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    user = relationship("User", back_populates="sales_rep")
    orders = relationship("Order", back_populates="sales_rep")
    sales_goals = relationship("SalesGoal", back_populates="sales_rep")

    def __repr__(self):
        return f"<SalesRep(user_id={self.user_id}, active={self.active})>"

