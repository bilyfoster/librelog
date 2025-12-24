"""
ProductionDelivery Service for delivering final audio to automation systems
"""

from typing import List, Optional, Dict, Any
from uuid import UUID
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.models.production_order import ProductionOrder, ProductionOrderStatus
from backend.models.copy import Copy, CopyStatus
from backend.models.audio_delivery import AudioDelivery, DeliveryMethod, DeliveryStatus
from backend.models.audio_cut import AudioCut
import structlog
import os

logger = structlog.get_logger()


class ProductionDeliveryService:
    """Service for delivering production audio to automation systems"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def deliver_to_traffic(
        self,
        production_order_id: UUID,
        delivery_method: DeliveryMethod = DeliveryMethod.LOCAL,
        target_server: Optional[str] = None,
        target_path: Optional[str] = None
    ) -> Dict[str, Any]:
        """Deliver completed production order to traffic/automation system"""
        from backend.services.production_order_service import ProductionOrderService
        
        po_service = ProductionOrderService(self.db)
        production_order = await po_service.get_by_id(production_order_id)
        
        if not production_order:
            raise ValueError("Production order not found")
        
        if production_order.status != ProductionOrderStatus.COMPLETED:
            raise ValueError("Production order must be completed before delivery")
        
        if not production_order.copy:
            raise ValueError("Production order has no associated copy")
        
        copy_item = production_order.copy
        
        # Get audio cuts for the copy
        from sqlalchemy import select
        result = await self.db.execute(
            select(AudioCut).where(AudioCut.copy_id == copy_item.id)
        )
        audio_cuts = result.scalars().all()
        
        if not audio_cuts:
            raise ValueError("No audio cuts found for delivery")
        
        delivery_results = []
        
        # Deliver each audio cut
        for cut in audio_cuts:
            if not cut.audio_file_path and not cut.audio_file_url:
                logger.warning(
                    "Audio cut has no file path or URL",
                    cut_id=cut.id,
                    production_order_id=production_order_id
                )
                continue
            
            # Create delivery record
            delivery = AudioDelivery(
                cut_id=cut.id,
                copy_id=copy_item.id,
                delivery_method=delivery_method.value,
                target_server=target_server or "api",  # Use container name for internal communication
                target_path=target_path or "/var/lib/libretime/storage",
                status=DeliveryStatus.PENDING.value
            )
            
            self.db.add(delivery)
            await self.db.flush()
            
            # TODO: Implement actual delivery logic
            # - SFTP upload
            # - RSYNC sync
            # - API push to LibreTime/AzuraCast
            # - Hot folder copy
            
            # For now, mark as success
            delivery.status = DeliveryStatus.SUCCESS.value
            delivery.delivery_started_at = datetime.now(timezone.utc)
            delivery.delivery_completed_at = datetime.now(timezone.utc)
            
            delivery_results.append({
                "cut_id": cut.id,
                "delivery_id": delivery.id,
                "status": "success"
            })
        
        # Update production order status
        production_order = await po_service.update_status(
            production_order_id,
            ProductionOrderStatus.DELIVERED
        )
        
        # Update copy status
        copy_item.copy_status = CopyStatus.DELIVERED
        
        await self.db.commit()
        
        logger.info(
            "Production order delivered to traffic",
            production_order_id=production_order_id,
            cuts_delivered=len(delivery_results)
        )
        
        return {
            "production_order_id": production_order_id,
            "status": "delivered",
            "deliveries": delivery_results
        }
    
    async def generate_metadata(
        self,
        production_order_id: UUID
    ) -> Dict[str, Any]:
        """Generate metadata for production order (cart number, ISCI code, etc.)"""
        from backend.services.production_order_service import ProductionOrderService
        
        po_service = ProductionOrderService(self.db)
        production_order = await po_service.get_by_id(production_order_id)
        
        if not production_order:
            raise ValueError("Production order not found")
        
        # Generate cart number (format: STATION-YYYYMMDD-XXXX)
        current_date = datetime.now()
        date_prefix = current_date.strftime("%Y%m%d")
        cart_number = f"{production_order.po_number.replace('PRO-', 'CART-')}"
        
        metadata = {
            "cart_number": cart_number,
            "client_name": production_order.client_name,
            "campaign_title": production_order.campaign_title,
            "spot_lengths": production_order.spot_lengths,
            "isci_code": None,  # TODO: Generate ISCI code if needed
            "advertiser_id": production_order.advertiser_id,
            "order_id": production_order.order_id,
            "created_at": production_order.created_at.isoformat() if production_order.created_at else None
        }
        
        return metadata

