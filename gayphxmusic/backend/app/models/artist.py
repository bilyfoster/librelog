from sqlalchemy import Column, String, Text, Boolean, DateTime, JSON, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.core.database import Base
import uuid


class Artist(Base):
    __tablename__ = "artists"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email = Column(String(255), nullable=False, index=True)  # Removed unique constraint
    name = Column(String(255), nullable=False, index=True)
    pronouns = Column(String(50))
    bio = Column(Text)
    social_links = Column(JSON, default=dict)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    is_active = Column(Boolean, default=True)

    # Relationships
    submissions = relationship("Submission", back_populates="artist")
    favorites = relationship("ArtistFavorite", back_populates="artist")
    
    # Table constraints
    __table_args__ = (
        UniqueConstraint('email', 'name', name='unique_email_artist_name'),
    )
