"""
Production Archive router for searching production history
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.production_archive_service import ProductionArchiveService
from backend.models.production_order import ProductionOrderStatus
from typing import Optional
from datetime import datetime

router = APIRouter()


@router.get("/search/client")
async def search_by_client(
    client_name: str = Query(...),
    limit: int = Query(50, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Search production orders by client name"""
    archive_service = ProductionArchiveService(db)
    orders = await archive_service.search_by_client(client_name, limit)
    
    return {
        "query": client_name,
        "results": [
            {
                "id": po.id,
                "po_number": po.po_number,
                "client_name": po.client_name,
                "status": po.status.value if hasattr(po.status, 'value') else str(po.status),
                "created_at": po.created_at.isoformat() if po.created_at else None
            }
            for po in orders
        ]
    }


@router.get("/search/script")
async def search_by_script(
    keyword: str = Query(...),
    limit: int = Query(50, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Search production orders by script keyword"""
    archive_service = ProductionArchiveService(db)
    results = await archive_service.search_by_script_keyword(keyword, limit)
    
    return {
        "query": keyword,
        "results": [
            {
                "production_order": {
                    "id": item["production_order"].id,
                    "po_number": item["production_order"].po_number,
                    "client_name": item["production_order"].client_name,
                },
                "copy": {
                    "id": item["copy"].id,
                    "title": item["copy"].title,
                }
            }
            for item in results
        ]
    }


@router.get("/search/voice-talent")
async def search_by_voice_talent(
    talent_user_id: UUID = Query(...),
    limit: int = Query(50, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Search production orders by voice talent"""
    archive_service = ProductionArchiveService(db)
    orders = await archive_service.search_by_voice_talent(talent_user_id, limit)
    
    return {
        "talent_user_id": talent_user_id,
        "results": [
            {
                "id": po.id,
                "po_number": po.po_number,
                "client_name": po.client_name,
                "status": po.status.value if hasattr(po.status, 'value') else str(po.status),
            }
            for po in orders
        ]
    }


@router.get("/search/spot-length")
async def search_by_spot_length(
    spot_length: int = Query(...),
    limit: int = Query(50, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Search production orders by spot length"""
    archive_service = ProductionArchiveService(db)
    orders = await archive_service.search_by_spot_length(spot_length, limit)
    
    return {
        "spot_length": spot_length,
        "results": [
            {
                "id": po.id,
                "po_number": po.po_number,
                "client_name": po.client_name,
                "spot_lengths": po.spot_lengths,
            }
            for po in orders
        ]
    }


@router.get("/history")
async def get_production_history(
    client_name: Optional[str] = Query(None),
    start_date: Optional[str] = Query(None),
    end_date: Optional[str] = Query(None),
    status: Optional[str] = Query(None),
    limit: int = Query(100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get production history with filters"""
    archive_service = ProductionArchiveService(db)
    
    start_dt = None
    end_dt = None
    status_enum = None
    
    if start_date:
        try:
            start_dt = datetime.fromisoformat(start_date.replace('Z', '+00:00'))
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid start_date format")
    
    if end_date:
        try:
            end_dt = datetime.fromisoformat(end_date.replace('Z', '+00:00'))
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid end_date format")
    
    if status:
        try:
            status_enum = ProductionOrderStatus(status)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid status")
    
    orders = await archive_service.get_production_history(
        client_name=client_name,
        start_date=start_dt,
        end_date=end_dt,
        status=status_enum,
        limit=limit
    )
    
    return {
        "results": [
            {
                "id": po.id,
                "po_number": po.po_number,
                "client_name": po.client_name,
                "campaign_title": po.campaign_title,
                "status": po.status.value if hasattr(po.status, 'value') else str(po.status),
                "created_at": po.created_at.isoformat() if po.created_at else None,
                "completed_at": po.completed_at.isoformat() if po.completed_at else None,
            }
            for po in orders
        ]
    }


@router.get("/renewal-candidates")
async def get_renewal_candidates(
    days_before_expiry: int = Query(30, ge=1, le=365),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get production orders that might need renewal"""
    archive_service = ProductionArchiveService(db)
    candidates = await archive_service.get_renewal_candidates(days_before_expiry)
    
    return {
        "candidates": [
            {
                "production_order": {
                    "id": item["production_order"].id,
                    "po_number": item["production_order"].po_number,
                    "client_name": item["production_order"].client_name,
                    "end_date": item["production_order"].end_date.isoformat() if item["production_order"].end_date else None,
                },
                "days_until_expiry": item["days_until_expiry"],
                "renewal_suggested": item["renewal_suggested"]
            }
            for item in candidates
        ]
    }


@router.get("/")
async def get_production_archive(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    status: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get production archive (all completed/archived orders)"""
    archive_service = ProductionArchiveService(db)
    status_enum = None
    
    if status:
        try:
            status_enum = ProductionOrderStatus(status)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid status")
    
    orders = await archive_service.get_archive(status=status_enum, skip=skip, limit=limit)
    
    return {
        "results": [
            {
                "id": po.id,
                "po_number": po.po_number,
                "client_name": po.client_name,
                "campaign_title": po.campaign_title,
                "status": po.status.value if hasattr(po.status, 'value') else str(po.status),
                "created_at": po.created_at.isoformat() if po.created_at else None,
                "completed_at": po.completed_at.isoformat() if po.completed_at else None,
            }
            for po in orders
        ]
    }


@router.get("/{order_id}")
async def get_production_archive_item(
    order_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific archived production order"""
    from backend.models.production_order import ProductionOrder
    from sqlalchemy import select
    
    result = await db.execute(
        select(ProductionOrder).where(ProductionOrder.id == order_id)
    )
    order = result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Production order not found")
    
    return {
        "id": order.id,
        "po_number": order.po_number,
        "client_name": order.client_name,
        "campaign_title": order.campaign_title,
        "status": order.status.value if hasattr(order.status, 'value') else str(order.status),
        "created_at": order.created_at.isoformat() if order.created_at else None,
        "completed_at": order.completed_at.isoformat() if order.completed_at else None,
        "notes": order.notes,
    }

