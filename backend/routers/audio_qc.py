"""
Audio QC router for quality control operations
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.audio_qc_service import AudioQCService
from pydantic import BaseModel
from typing import Optional, List
import structlog

logger = structlog.get_logger()

router = APIRouter(prefix="/audio-qc", tags=["audio-qc"])


class QCOverrideRequest(BaseModel):
    override_reason: str


class QCResultResponse(BaseModel):
    id: int
    cut_id: Optional[int]
    qc_passed: bool
    qc_warnings: Optional[List[str]]
    qc_errors: Optional[List[str]]
    silence_detected: bool
    clipping_detected: bool
    format_valid: bool
    file_corrupted: bool
    overridden: bool
    created_at: str

    class Config:
        from_attributes = True


@router.post("/run/{cut_id}", response_model=QCResultResponse)
async def run_qc(
    cut_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Run QC on an audio cut"""
    from backend.models.audio_cut import AudioCut
    from sqlalchemy import select
    
    # Get cut
    result = await db.execute(select(AudioCut).where(AudioCut.id == cut_id))
    cut = result.scalar_one_or_none()
    if not cut:
        raise HTTPException(status_code=404, detail="Cut not found")
    
    if not cut.audio_file_path:
        raise HTTPException(status_code=400, detail="Cut has no audio file")
    
    service = AudioQCService(db)
    try:
        qc_result = await service.run_qc(cut.audio_file_path, cut_id=cut_id)
        return QCResultResponse.model_validate(qc_result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/{cut_id}/results", response_model=List[QCResultResponse])
async def get_qc_results(
    cut_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get QC results for a cut"""
    service = AudioQCService(db)
    results = await service.get_qc_results_for_cut(cut_id)
    return [QCResultResponse.model_validate(r) for r in results]


@router.post("/{qc_result_id}/override", response_model=QCResultResponse)
async def override_qc(
    qc_result_id: int,
    override_request: QCOverrideRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Override a QC failure"""
    service = AudioQCService(db)
    try:
        qc_result = await service.override_qc(
            qc_result_id=qc_result_id,
            overridden_by=current_user.id,
            override_reason=override_request.override_reason
        )
        return QCResultResponse.model_validate(qc_result)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.get("/", response_model=List[QCResultResponse])
async def list_qc_results(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    cut_id: Optional[int] = Query(None),
    qc_passed: Optional[bool] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all QC results"""
    from backend.models.audio_qc_result import AudioQCResult
    from sqlalchemy import select
    
    query = select(AudioQCResult)
    
    if cut_id:
        query = query.where(AudioQCResult.cut_id == cut_id)
    
    if qc_passed is not None:
        query = query.where(AudioQCResult.qc_passed == qc_passed)
    
    query = query.order_by(AudioQCResult.created_at.desc()).offset(skip).limit(limit)
    
    result = await db.execute(query)
    qc_results = result.scalars().all()
    
    return [QCResultResponse.model_validate(r) for r in qc_results]


@router.post("/", response_model=QCResultResponse, status_code=status.HTTP_201_CREATED)
async def create_qc_result(
    cut_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new QC result by running QC on a cut"""
    return await run_qc(cut_id, db, current_user)


@router.get("/{qc_result_id}", response_model=QCResultResponse)
async def get_qc_result(
    qc_result_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific QC result"""
    from backend.models.audio_qc_result import AudioQCResult
    from sqlalchemy import select
    
    result = await db.execute(
        select(AudioQCResult).where(AudioQCResult.id == qc_result_id)
    )
    qc_result = result.scalar_one_or_none()
    
    if not qc_result:
        raise HTTPException(status_code=404, detail="QC result not found")
    
    return QCResultResponse.model_validate(qc_result)

