"""
Cluster model for traffic management
"""

from sqlalchemy import Column, String, DateTime, Boolean, Text, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Cluster(Base):
    """Cluster model for managing station clusters"""
    __tablename__ = "clusters"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(255), nullable=False, unique=True, index=True)
    description = Column(Text, nullable=True)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    stations = relationship("Station", secondary="station_clusters", back_populates="clusters")

    def __repr__(self):
        return f"<Cluster(name='{self.name}', active={self.active})>"

