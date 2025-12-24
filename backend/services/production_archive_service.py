"""
Production Archive Service for searching production history
"""

from typing import List, Optional, Dict, Any
from uuid import UUID
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_, func
from sqlalchemy.orm import selectinload
from backend.models.production_order import ProductionOrder, ProductionOrderStatus
from backend.models.copy import Copy
from backend.models.advertiser import Advertiser
from backend.models.voice_talent_request import VoiceTalentRequest
from backend.models.production_assignment import ProductionAssignment
import structlog

logger = structlog.get_logger()


class ProductionArchiveService:
    """Service for searching production archive"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def search_by_client(
        self,
        client_name: str,
        limit: int = 50
    ) -> List[ProductionOrder]:
        """Search production orders by client name"""
        result = await self.db.execute(
            select(ProductionOrder)
            .where(ProductionOrder.client_name.ilike(f"%{client_name}%"))
            .order_by(ProductionOrder.created_at.desc())
            .limit(limit)
        )
        return result.scalars().all()
    
    async def search_by_script_keyword(
        self,
        keyword: str,
        limit: int = 50
    ) -> List[Dict[str, Any]]:
        """Search production orders by script keyword"""
        # Search in copy script_text
        result = await self.db.execute(
            select(Copy)
            .where(Copy.script_text.ilike(f"%{keyword}%"))
            .where(Copy.production_order_id.isnot(None))
            .limit(limit)
        )
        copies = result.scalars().all()
        
        results = []
        for copy in copies:
            if copy.production_order_id:
                po_result = await self.db.execute(
                    select(ProductionOrder).where(ProductionOrder.id == copy.production_order_id)
                )
                po = po_result.scalar_one_or_none()
                if po:
                    results.append({
                        "production_order": po,
                        "copy": copy,
                        "match_type": "script_keyword"
                    })
        
        return results
    
    async def search_by_voice_talent(
        self,
        talent_user_id: UUID,
        limit: int = 50
    ) -> List[ProductionOrder]:
        """Search production orders by voice talent"""
        result = await self.db.execute(
            select(ProductionOrder)
            .join(VoiceTalentRequest)
            .where(VoiceTalentRequest.talent_user_id == talent_user_id)
            .order_by(ProductionOrder.created_at.desc())
            .limit(limit)
        )
        return result.scalars().all()
    
    async def search_by_spot_length(
        self,
        spot_length: int,
        limit: int = 50
    ) -> List[ProductionOrder]:
        """Search production orders by spot length"""
        # Spot lengths are stored as JSONB array
        result = await self.db.execute(
            select(ProductionOrder)
            .where(ProductionOrder.spot_lengths.contains([spot_length]))
            .order_by(ProductionOrder.created_at.desc())
            .limit(limit)
        )
        return result.scalars().all()
    
    async def search_by_station(
        self,
        station_name: str,
        limit: int = 50
    ) -> List[ProductionOrder]:
        """Search production orders by station"""
        # Stations are stored as JSONB array
        result = await self.db.execute(
            select(ProductionOrder)
            .where(ProductionOrder.stations.contains([station_name]))
            .order_by(ProductionOrder.created_at.desc())
            .limit(limit)
        )
        return result.scalars().all()
    
    async def get_production_history(
        self,
        client_name: Optional[str] = None,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None,
        status: Optional[ProductionOrderStatus] = None,
        limit: int = 100
    ) -> List[ProductionOrder]:
        """Get production history with filters"""
        query = select(ProductionOrder)
        
        if client_name:
            query = query.where(ProductionOrder.client_name.ilike(f"%{client_name}%"))
        
        if start_date:
            query = query.where(ProductionOrder.created_at >= start_date)
        
        if end_date:
            query = query.where(ProductionOrder.created_at <= end_date)
        
        if status:
            query = query.where(ProductionOrder.status == status)
        
        query = query.order_by(ProductionOrder.created_at.desc()).limit(limit)
        
        result = await self.db.execute(query)
        return result.scalars().all()
    
    async def get_renewal_candidates(
        self,
        days_before_expiry: int = 30
    ) -> List[Dict[str, Any]]:
        """Get production orders that might need renewal"""
        from datetime import timedelta
        
        cutoff_date = datetime.now() + timedelta(days=days_before_expiry)
        
        result = await self.db.execute(
            select(ProductionOrder)
            .where(
                and_(
                    ProductionOrder.end_date.isnot(None),
                    ProductionOrder.end_date <= cutoff_date,
                    ProductionOrder.end_date >= datetime.now().date(),
                    ProductionOrder.status.in_([
                        ProductionOrderStatus.COMPLETED,
                        ProductionOrderStatus.DELIVERED
                    ])
                )
            )
            .order_by(ProductionOrder.end_date.asc())
        )
        orders = result.scalars().all()
        
        candidates = []
        for order in orders:
            candidates.append({
                "production_order": order,
                "days_until_expiry": (order.end_date - datetime.now().date()).days if order.end_date else None,
                "renewal_suggested": True
            })
        
        return candidates

