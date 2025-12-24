"""
Pydantic schemas for SalesOffice model
"""

from pydantic import BaseModel, ConfigDict
from typing import Optional
from datetime import datetime
from uuid import UUID


class SalesOfficeBase(BaseModel):
    """Base sales office schema"""
    name: str
    address: Optional[str] = None
    region_id: Optional[UUID] = None
    active: bool = True


class SalesOfficeCreate(SalesOfficeBase):
    """Schema for creating a sales office"""
    pass


class SalesOfficeUpdate(BaseModel):
    """Schema for updating a sales office"""
    name: Optional[str] = None
    address: Optional[str] = None
    region_id: Optional[UUID] = None
    active: Optional[bool] = None


class SalesOfficeResponse(SalesOfficeBase):
    """Schema for sales office response"""
    id: UUID
    created_at: datetime
    updated_at: datetime
    region_name: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

