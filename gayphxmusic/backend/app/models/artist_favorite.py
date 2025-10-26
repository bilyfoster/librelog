from sqlalchemy import Column, String, DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from sqlalchemy.dialects.postgresql import UUID
import uuid
from datetime import datetime

from app.core.database import Base

class ArtistFavorite(Base):
    __tablename__ = "artist_favorites"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_email = Column(String, nullable=False, index=True)  # Email of the user who favorited
    artist_id = Column(UUID(as_uuid=True), ForeignKey("artists.id", ondelete="CASCADE"), nullable=False)
    favorited_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    # Relationship to artist
    artist = relationship("Artist", back_populates="favorites")

    # Ensure a user can only favorite an artist once
    __table_args__ = (
        UniqueConstraint('user_email', 'artist_id', name='unique_user_artist_favorite'),
    )

    def __repr__(self):
        return f"<ArtistFavorite(user_email='{self.user_email}', artist_id='{self.artist_id}')>"

