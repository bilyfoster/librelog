"""
Station model for traffic management
"""

from sqlalchemy import Column, String, DateTime, Boolean, Text, Numeric, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Station(Base):
    """Station model for managing radio stations"""
    __tablename__ = "stations"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    call_letters = Column(String(10), nullable=False, unique=True, index=True)  # e.g., "KABC"
    frequency = Column(String(20), nullable=True, index=True)  # e.g., "101.5 FM" or "640 AM"
    market = Column(String(255), nullable=True, index=True)  # Market name
    format = Column(String(100), nullable=True)  # Format type (e.g., "Top 40", "Country", "News/Talk")
    ownership = Column(String(255), nullable=True)  # Ownership company
    contacts = Column(JSONB, nullable=True)  # JSON array of contact info
    rates = Column(JSONB, nullable=True)  # JSON object with rate information
    inventory_class = Column(String(50), nullable=True, index=True)  # Inventory classification
    libretime_config = Column(JSONB, nullable=True)  # LibreTime integration config: {api_url, api_key, public_url}
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    clusters = relationship("Cluster", secondary="station_clusters", back_populates="stations")
    spots = relationship("Spot", back_populates="station")
    daily_logs = relationship("DailyLog", back_populates="station")
    order_lines = relationship("OrderLine", back_populates="station")
    invoice_lines = relationship("InvoiceLine", back_populates="station")
    users = relationship("User", secondary="user_stations", back_populates="stations")
    clock_templates = relationship("ClockTemplate", back_populates="station")
    dayparts = relationship("Daypart", back_populates="station")
    break_structures = relationship("BreakStructure", back_populates="station")
    tracks = relationship("Track", back_populates="station")

    def __repr__(self):
        return f"<Station(call_letters='{self.call_letters}', frequency={self.frequency}, active={self.active})>"

