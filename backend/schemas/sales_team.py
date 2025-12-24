"""
Pydantic schemas for SalesTeam model
"""

from pydantic import BaseModel, ConfigDict
from typing import Optional, List
from datetime import datetime
from uuid import UUID


class SalesTeamBase(BaseModel):
    """Base sales team schema"""
    name: str
    description: Optional[str] = None
    active: bool = True


class SalesTeamCreate(SalesTeamBase):
    """Schema for creating a sales team"""
    pass


class SalesTeamUpdate(BaseModel):
    """Schema for updating a sales team"""
    name: Optional[str] = None
    description: Optional[str] = None
    active: Optional[bool] = None


class SalesTeamResponse(SalesTeamBase):
    """Schema for sales team response"""
    id: UUID
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class SalesTeamWithReps(SalesTeamResponse):
    """Sales team with sales reps"""
    sales_reps: List[dict] = []

