"""
Voice Talent router for managing voice talent requests and takes
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query, UploadFile, File
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.models.voice_talent_request import VoiceTalentRequest, TalentRequestStatus, TalentType
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.voice_talent_service import VoiceTalentService
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
import os
import aiofiles

router = APIRouter()


class VoiceTalentRequestCreate(BaseModel):
    production_order_id: int
    script: str
    talent_type: str
    talent_user_id: Optional[int] = None
    pronunciation_guides: Optional[str] = None
    talent_instructions: Optional[str] = None
    deadline: Optional[datetime] = None


class VoiceTalentRequestResponse(BaseModel):
    id: int
    production_order_id: int
    talent_user_id: Optional[int]
    talent_type: str
    script: str
    pronunciation_guides: Optional[str]
    talent_instructions: Optional[str]
    status: str
    takes: Optional[List[dict]]
    assigned_at: Optional[str]
    deadline: Optional[str]
    completed_at: Optional[str]
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


def voice_talent_request_to_response(vtr: VoiceTalentRequest) -> dict:
    """Convert VoiceTalentRequest to response dict"""
    return {
        "id": vtr.id,
        "production_order_id": vtr.production_order_id,
        "talent_user_id": vtr.talent_user_id,
        "talent_type": vtr.talent_type.value if vtr.talent_type else None,
        "script": vtr.script,
        "pronunciation_guides": vtr.pronunciation_guides,
        "talent_instructions": vtr.talent_instructions,
        "status": vtr.status.value if vtr.status else None,
        "takes": vtr.takes,
        "assigned_at": vtr.assigned_at.isoformat() if vtr.assigned_at else None,
        "deadline": vtr.deadline.isoformat() if vtr.deadline else None,
        "completed_at": vtr.completed_at.isoformat() if vtr.completed_at else None,
        "created_at": vtr.created_at.isoformat() if vtr.created_at else None,
        "updated_at": vtr.updated_at.isoformat() if vtr.updated_at else None,
    }


@router.get("/requests", response_model=List[VoiceTalentRequestResponse])
async def list_voice_talent_requests(
    status: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get voice talent requests assigned to current user"""
    talent_service = VoiceTalentService(db)
    
    status_enum = None
    if status:
        try:
            status_enum = TalentRequestStatus(status)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid status")
    
    requests = await talent_service.get_requests_for_talent(
        talent_user_id=current_user.id,
        status=status_enum
    )
    
    return [voice_talent_request_to_response(req) for req in requests]


@router.post("/requests", response_model=VoiceTalentRequestResponse, status_code=status.HTTP_201_CREATED)
async def create_voice_talent_request(
    data: VoiceTalentRequestCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a voice talent request"""
    talent_service = VoiceTalentService(db)
    
    try:
        talent_type_enum = TalentType(data.talent_type)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid talent_type")
    
    try:
        request = await talent_service.create_request(
            production_order_id=data.production_order_id,
            script=data.script,
            talent_type=talent_type_enum,
            talent_user_id=data.talent_user_id,
            pronunciation_guides=data.pronunciation_guides,
            talent_instructions=data.talent_instructions,
            deadline=data.deadline
        )
        
        return voice_talent_request_to_response(request)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/requests/{request_id}/upload-take")
async def upload_take(
    request_id: int,
    file: UploadFile = File(...),
    take_number: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload a take for a voice talent request"""
    talent_service = VoiceTalentService(db)
    
    # Save file
    upload_dir = os.getenv("VOICE_TALENT_UPLOADS_DIR", "/var/lib/librelog/voice_talent_uploads")
    os.makedirs(upload_dir, exist_ok=True)
    
    file_path = os.path.join(upload_dir, f"request_{request_id}_take_{take_number or 'new'}_{file.filename}")
    
    async with aiofiles.open(file_path, 'wb') as f:
        content = await file.read()
        await f.write(content)
    
    file_url = f"/uploads/voice_talent/{os.path.basename(file_path)}"
    
    try:
        request = await talent_service.upload_take(
            request_id=request_id,
            file_path=file_path,
            file_url=file_url,
            take_number=take_number
        )
        
        return voice_talent_request_to_response(request)
    except ValueError as e:
        # Clean up file on error
        if os.path.exists(file_path):
            os.remove(file_path)
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/requests/{request_id}/takes")
async def list_takes(
    request_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all takes for a voice talent request"""
    from sqlalchemy import select
    from backend.models.voice_talent_request import VoiceTalentRequest
    
    result = await db.execute(
        select(VoiceTalentRequest).where(VoiceTalentRequest.id == request_id)
    )
    request = result.scalar_one_or_none()
    
    if not request:
        raise HTTPException(status_code=404, detail="Voice talent request not found")
    
    # Check if user has access
    if request.talent_user_id != current_user.id and current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Access denied")
    
    return {
        "request_id": request_id,
        "takes": request.takes or []
    }


@router.post("/requests/{request_id}/approve-take")
async def approve_take(
    request_id: int,
    take_number: int = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Approve a specific take"""
    talent_service = VoiceTalentService(db)
    
    try:
        request = await talent_service.approve_take(request_id, take_number)
        return voice_talent_request_to_response(request)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/requests/{request_id}/assign")
async def assign_talent(
    request_id: int,
    talent_user_id: int = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Assign talent to a request"""
    talent_service = VoiceTalentService(db)
    
    try:
        request = await talent_service.assign_talent(request_id, talent_user_id)
        return voice_talent_request_to_response(request)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

