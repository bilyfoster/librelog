"""
Pydantic schemas for SalesRegion model
"""

from pydantic import BaseModel, ConfigDict
from typing import Optional, List
from datetime import datetime
from uuid import UUID


class SalesRegionBase(BaseModel):
    """Base sales region schema"""
    name: str
    description: Optional[str] = None
    active: bool = True


class SalesRegionCreate(SalesRegionBase):
    """Schema for creating a sales region"""
    pass


class SalesRegionUpdate(BaseModel):
    """Schema for updating a sales region"""
    name: Optional[str] = None
    description: Optional[str] = None
    active: Optional[bool] = None


class SalesRegionResponse(SalesRegionBase):
    """Schema for sales region response"""
    id: UUID
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class SalesRegionWithOffices(SalesRegionResponse):
    """Sales region with offices"""
    sales_offices: List[dict] = []

