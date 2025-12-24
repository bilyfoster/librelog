"""
Pydantic schemas for Track model
"""
from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List
from uuid import UUID


class TrackBase(BaseModel):
    """Base track schema"""
    title: str
    artist: Optional[str] = None
    album: Optional[str] = None
    type: str
    genre: Optional[str] = None
    duration: Optional[int] = None
    filepath: str
    libretime_id: Optional[str] = None
    bpm: Optional[int] = None
    daypart_eligible: Optional[List[int]] = None
    is_new_release: Optional[bool] = False
    allow_back_to_back: Optional[bool] = False


class TrackCreate(TrackBase):
    """Schema for creating a track"""
    pass


class TrackUpdate(BaseModel):
    """Schema for updating a track"""
    title: Optional[str] = None
    artist: Optional[str] = None
    album: Optional[str] = None
    type: Optional[str] = None
    genre: Optional[str] = None
    duration: Optional[int] = None
    filepath: Optional[str] = None
    libretime_id: Optional[str] = None
    bpm: Optional[int] = None
    daypart_eligible: Optional[List[int]] = None
    is_new_release: Optional[bool] = None
    allow_back_to_back: Optional[bool] = None


class TrackResponse(TrackBase):
    """Schema for track response"""
    id: UUID
    last_played: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

