"""
AudioSearchService for advanced audio asset search
"""

from typing import List, Optional, Dict, Any
from uuid import UUID
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_, func
from backend.models.audio_cut import AudioCut
from backend.models.copy import Copy
from backend.models.advertiser import Advertiser
from backend.models.order import Order
import structlog

logger = structlog.get_logger()


class AudioSearchService:
    """Service for advanced audio search"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def global_search(
        self,
        search_term: str,
        limit: int = 50,
        skip: int = 0
    ) -> Dict[str, Any]:
        """Global search across all audio assets"""
        results = {
            "cuts": [],
            "copy": [],
            "total": 0
        }
        
        # Search cuts
        cuts_query = select(AudioCut).join(Copy).where(
            or_(
                AudioCut.cut_id.ilike(f"%{search_term}%"),
                AudioCut.cut_name.ilike(f"%{search_term}%"),
                AudioCut.notes.ilike(f"%{search_term}%"),
                Copy.title.ilike(f"%{search_term}%")
            )
        ).limit(limit).offset(skip)
        
        cuts_result = await self.db.execute(cuts_query)
        results["cuts"] = list(cuts_result.scalars().all())
        
        # Search copy
        copy_query = select(Copy).where(
            or_(
                Copy.title.ilike(f"%{search_term}%"),
                Copy.script_text.ilike(f"%{search_term}%")
            )
        ).limit(limit).offset(skip)
        
        copy_result = await self.db.execute(copy_query)
        results["copy"] = list(copy_result.scalars().all())
        
        results["total"] = len(results["cuts"]) + len(results["copy"])
        
        return results
    
    async def advanced_search(
        self,
        advertiser_id: Optional[UUID] = None,
        order_id: Optional[UUID] = None,
        cut_id: Optional[str] = None,
        tags: Optional[List[str]] = None,
        status: Optional[str] = None,  # active, inactive, expired
        expires_before: Optional[datetime] = None,
        expires_after: Optional[datetime] = None,
        limit: int = 50,
        skip: int = 0
    ) -> List[AudioCut]:
        """Advanced multi-criteria search"""
        query = select(AudioCut).join(Copy)
        
        if advertiser_id:
            query = query.where(Copy.advertiser_id == advertiser_id)
        if order_id:
            query = query.where(Copy.order_id == order_id)
        if cut_id:
            query = query.where(AudioCut.cut_id == cut_id)
        if tags:
            # Search for cuts with any of the specified tags
            tag_conditions = [func.jsonb_exists(AudioCut.tags, tag) for tag in tags]
            query = query.where(or_(*tag_conditions))
        if status == "active":
            query = query.where(AudioCut.active == True)
        elif status == "inactive":
            query = query.where(AudioCut.active == False)
        elif status == "expired":
            query = query.where(
                and_(
                    AudioCut.expires_at.isnot(None),
                    AudioCut.expires_at < datetime.now(timezone.utc)
                )
            )
        if expires_before:
            query = query.where(AudioCut.expires_at <= expires_before)
        if expires_after:
            query = query.where(AudioCut.expires_at >= expires_after)
        
        query = query.limit(limit).offset(skip).order_by(AudioCut.created_at.desc())
        
        result = await self.db.execute(query)
        return list(result.scalars().all())
    
    async def search_by_folder(
        self,
        advertiser_id: Optional[UUID] = None,
        order_id: Optional[UUID] = None
    ) -> Dict[str, Any]:
        """Folder-like navigation: Advertiser → Order → Cuts"""
        folder_structure = {}
        
        if advertiser_id:
            # Get advertiser
            adv_result = await self.db.execute(select(Advertiser).where(Advertiser.id == advertiser_id))
            advertiser = adv_result.scalar_one_or_none()
            
            if advertiser:
                folder_structure["advertiser"] = {
                    "id": advertiser.id,
                    "name": advertiser.name
                }
                
                # Get orders for this advertiser
                orders_result = await self.db.execute(
                    select(Order).where(Order.advertiser_id == advertiser_id)
                )
                orders = orders_result.scalars().all()
                
                folder_structure["orders"] = []
                for order in orders:
                    order_data = {
                        "id": order.id,
                        "name": order.name or f"Order #{order.id}",
                        "cuts": []
                    }
                    
                    # Get copy for this order
                    copy_result = await self.db.execute(
                        select(Copy).where(Copy.order_id == order.id)
                    )
                    copy_items = copy_result.scalars().all()
                    
                    for copy_item in copy_items:
                        # Get cuts for this copy
                        cuts_result = await self.db.execute(
                            select(AudioCut).where(AudioCut.copy_id == copy_item.id)
                        )
                        cuts = cuts_result.scalars().all()
                        
                        for cut in cuts:
                            order_data["cuts"].append({
                                "id": cut.id,
                                "cut_id": cut.cut_id,
                                "cut_name": cut.cut_name,
                                "version": cut.version,
                                "active": cut.active
                            })
                    
                    folder_structure["orders"].append(order_data)
        
        return folder_structure

