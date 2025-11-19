"""
ProductionComment model for comments and notes on production orders
"""

from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, Boolean
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class ProductionComment(Base):
    """ProductionComment model for threaded comments on production orders"""
    __tablename__ = "production_comments"

    id = Column(Integer, primary_key=True, index=True)
    production_order_id = Column(Integer, ForeignKey("production_orders.id"), nullable=False, index=True)
    
    # Comment content
    comment_text = Column(Text, nullable=False)
    author_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    
    # Threading support
    parent_comment_id = Column(Integer, ForeignKey("production_comments.id"), nullable=True)
    
    # Attachments (stored as JSONB array)
    attachments = Column(JSONB, nullable=True)  # [{type: "file", path: "...", name: "..."}, ...]
    
    # Metadata
    is_internal = Column(Boolean, default=False)  # Internal notes vs. visible comments
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    production_order = relationship("ProductionOrder", back_populates="comments")
    author = relationship("User", foreign_keys=[author_id])
    parent_comment = relationship("ProductionComment", remote_side=[id], backref="replies")

    def __repr__(self):
        return f"<ProductionComment(production_order_id={self.production_order_id}, author_id={self.author_id})>"

