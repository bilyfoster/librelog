"""
Pydantic schemas for Station model
"""

from pydantic import BaseModel, ConfigDict
from typing import Optional, Dict, Any, List
from datetime import datetime
from uuid import UUID


class StationBase(BaseModel):
    """Base station schema"""
    call_letters: str
    frequency: Optional[str] = None
    market: Optional[str] = None
    format: Optional[str] = None
    ownership: Optional[str] = None
    contacts: Optional[Dict[str, Any]] = None
    rates: Optional[Dict[str, Any]] = None
    inventory_class: Optional[str] = None
    active: bool = True


class StationCreate(StationBase):
    """Schema for creating a station"""
    pass


class StationUpdate(BaseModel):
    """Schema for updating a station"""
    call_letters: Optional[str] = None
    frequency: Optional[str] = None
    market: Optional[str] = None
    format: Optional[str] = None
    ownership: Optional[str] = None
    contacts: Optional[Dict[str, Any]] = None
    rates: Optional[Dict[str, Any]] = None
    inventory_class: Optional[str] = None
    active: Optional[bool] = None
    libretime_config: Optional[Dict[str, Any]] = None  # LibreTime integration config: {api_url, api_key, public_url}


class StationResponse(StationBase):
    """Schema for station response"""
    id: UUID
    created_at: datetime
    updated_at: datetime
    clusters: List[dict] = []
    libretime_config: Optional[Dict[str, Any]] = None  # LibreTime integration config: {api_url, api_key, public_url}

    model_config = ConfigDict(from_attributes=True)

