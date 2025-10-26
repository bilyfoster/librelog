from pydantic import BaseModel
from typing import Optional, Dict, Any
from datetime import datetime
import uuid

class SubmissionCreate(BaseModel):
    title: str
    genre: Optional[str] = None
    isrc_requested: bool = False
    radio_permission: bool = False
    public_display: bool = False
    rights_attestation: bool = False
    pro_info: Optional[Dict[str, Any]] = None

class SubmissionResponse(BaseModel):
    id: uuid.UUID
    artist_id: uuid.UUID
    title: str
    status: str
    tracking_id: str

class SubmissionStatus(BaseModel):
    id: uuid.UUID
    title: str
    artist_name: str
    status: str
    admin_notes: Optional[str] = None
    isrc_code: Optional[str] = None
    created_at: datetime

class SubmissionUpdate(BaseModel):
    status: Optional[str] = None
    admin_notes: Optional[str] = None

class AdminSubmissionResponse(BaseModel):
    id: uuid.UUID
    artist_id: uuid.UUID
    artist_name: str
    artist_email: str
    title: str
    genre: Optional[str] = None
    file_url: str
    file_name: str
    status: str
    isrc_requested: bool
    radio_permission: bool
    public_display: bool
    admin_notes: Optional[str] = None
    tracking_id: str
    created_at: datetime
    isrc_code: Optional[str] = None

