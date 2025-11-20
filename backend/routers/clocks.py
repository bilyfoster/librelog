"""
Clock templates router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from backend.database import get_db
from backend.models.clock_template import ClockTemplate
from backend.services.clock_service import ClockTemplateService
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, Dict, Any

router = APIRouter()


@router.get("/count")
async def get_clocks_count(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get total count of clock templates"""
    query = select(func.count(ClockTemplate.id))
    result = await db.execute(query)
    count = result.scalar() or 0
    
    return {"count": count}


class ClockTemplateCreate(BaseModel):
    name: str
    description: Optional[str] = None
    json_layout: Dict[str, Any]


class ClockTemplateUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    json_layout: Optional[Dict[str, Any]] = None


@router.get("/")
@router.get("")  # Handle both with and without trailing slash
async def list_clock_templates(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all clock templates"""
    service = ClockTemplateService(db)
    templates = await service.list_templates(skip=skip, limit=limit, search=search)
    
    return {
        "templates": [
            {
                "id": t.id,
                "name": t.name,
                "description": t.description,
                "json_layout": t.json_layout,
                "created_at": t.created_at,
                "updated_at": t.updated_at
            }
            for t in templates
        ],
        "total": len(templates)
    }


@router.post("/")
async def create_clock_template(
    template_data: ClockTemplateCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new clock template"""
    service = ClockTemplateService(db)
    
    try:
        template = await service.create_template(
            name=template_data.name,
            description=template_data.description,
            json_layout=template_data.json_layout,
            user_id=current_user.id
        )
        
        return {
            "id": template.id,
            "name": template.name,
            "description": template.description,
            "json_layout": template.json_layout,
            "created_at": template.created_at,
            "message": "Clock template created successfully"
        }
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.get("/{template_id}")
async def get_clock_template(
    template_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific clock template"""
    service = ClockTemplateService(db)
    template = await service.get_template(template_id)
    
    if not template:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Clock template not found")
    
    return {
        "id": template.id,
        "name": template.name,
        "description": template.description,
        "json_layout": template.json_layout,
        "created_at": template.created_at,
        "updated_at": template.updated_at
    }


@router.put("/{template_id}")
async def update_clock_template(
    template_id: int,
    template_data: ClockTemplateUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a clock template"""
    service = ClockTemplateService(db)
    
    try:
        template = await service.update_template(
            template_id=template_id,
            name=template_data.name,
            description=template_data.description,
            json_layout=template_data.json_layout
        )
        
        if not template:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Clock template not found")
        
        return {
            "id": template.id,
            "name": template.name,
            "description": template.description,
            "json_layout": template.json_layout,
            "updated_at": template.updated_at,
            "message": "Clock template updated successfully"
        }
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.delete("/{template_id}")
async def delete_clock_template(
    template_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a clock template"""
    service = ClockTemplateService(db)
    success = await service.delete_template(template_id)
    
    if not success:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Clock template not found")
    
    return {"message": "Clock template deleted successfully"}


@router.get("/{template_id}/preview")
async def preview_clock_template(
    template_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate a preview of what an hour would look like"""
    service = ClockTemplateService(db)
    
    try:
        preview = await service.generate_preview(template_id)
        return preview
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.post("/{template_id}/export")
async def export_clock_template(
    template_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Export clock template to LibreTime as Smart Block"""
    service = ClockTemplateService(db)
    success = await service.export_to_libretime(template_id)
    
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to export template to LibreTime"
        )
    
    return {"message": "Clock template exported to LibreTime successfully"}
