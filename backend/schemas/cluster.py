"""
Pydantic schemas for Cluster model
"""

from pydantic import BaseModel, ConfigDict
from typing import Optional, List
from datetime import datetime
from uuid import UUID


class ClusterBase(BaseModel):
    """Base cluster schema"""
    name: str
    description: Optional[str] = None
    active: bool = True


class ClusterCreate(ClusterBase):
    """Schema for creating a cluster"""
    pass


class ClusterUpdate(BaseModel):
    """Schema for updating a cluster"""
    name: Optional[str] = None
    description: Optional[str] = None
    active: Optional[bool] = None


class ClusterResponse(ClusterBase):
    """Schema for cluster response"""
    id: UUID
    created_at: datetime
    updated_at: datetime
    stations: List[dict] = []

    model_config = ConfigDict(from_attributes=True)

