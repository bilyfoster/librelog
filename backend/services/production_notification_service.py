"""
Production Notification Service for production workflow notifications
"""

from typing import List, Optional, Dict, Any
from datetime import datetime, timezone, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.models.notification import Notification, NotificationType, NotificationStatus
from backend.models.production_order import ProductionOrder, ProductionOrderStatus
from backend.models.production_assignment import ProductionAssignment
from backend.models.copy import Copy, CopyApprovalStatus
from backend.services.notification_service import NotificationService
import structlog

logger = structlog.get_logger()


class ProductionNotificationService:
    """Service for production-specific notifications"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        self.notification_service = NotificationService
    
    async def notify_production_order_created(
        self,
        production_order_id: int,
        production_director_ids: Optional[List[int]] = None
    ):
        """Notify production director when PO is created"""
        from backend.services.production_order_service import ProductionOrderService
        
        po_service = ProductionOrderService(self.db)
        production_order = await po_service.get_by_id(production_order_id)
        
        if not production_order:
            return
        
        # Get production directors if not provided
        if not production_director_ids:
            from backend.models.user import User
            result = await self.db.execute(
                select(User).where(User.role == 'production_director')
            )
            production_directors = result.scalars().all()
            production_director_ids = [pd.id for pd in production_directors]
        
        # Notify each production director
        for pd_id in production_director_ids:
            await self.notification_service.create_notification(
                self.db,
                user_id=pd_id,
                message=f"New production order created: {production_order.po_number} for {production_order.client_name}",
                notification_type=NotificationType.IN_APP,
                subject="New Production Order",
                metadata={
                    "type": "production_order_created",
                    "production_order_id": production_order_id,
                    "po_number": production_order.po_number,
                    "client_name": production_order.client_name
                }
            )
        
        logger.info(
            "Production order created notifications sent",
            production_order_id=production_order_id,
            recipients=len(production_director_ids)
        )
    
    async def notify_assignment(
        self,
        assignment_id: int
    ):
        """Notify user when assigned to production order"""
        result = await self.db.execute(
            select(ProductionAssignment)
            .where(ProductionAssignment.id == assignment_id)
        )
        assignment = result.scalar_one_or_none()
        
        if not assignment:
            return
        
        po_service = ProductionOrderService(self.db)
        production_order = await po_service.get_by_id(assignment.production_order_id)
        
        if not production_order:
            return
        
        await self.notification_service.create_notification(
            self.db,
            user_id=assignment.user_id,
            message=f"You have been assigned to production order {production_order.po_number} ({assignment.assignment_type})",
            notification_type=NotificationType.IN_APP,
            subject="Production Assignment",
            metadata={
                "type": "production_assignment",
                "assignment_id": assignment_id,
                "production_order_id": assignment.production_order_id,
                "assignment_type": assignment.assignment_type.value if hasattr(assignment.assignment_type, 'value') else str(assignment.assignment_type)
            }
        )
        
        logger.info(
            "Assignment notification sent",
            assignment_id=assignment_id,
            user_id=assignment.user_id
        )
    
    async def notify_deadline_approaching(
        self,
        days_ahead: int = 1
    ):
        """Notify about production orders with approaching deadlines"""
        cutoff_date = datetime.now(timezone.utc) + timedelta(days=days_ahead)
        
        result = await self.db.execute(
            select(ProductionOrder)
            .where(
                and_(
                    ProductionOrder.deadline.isnot(None),
                    ProductionOrder.deadline <= cutoff_date,
                    ProductionOrder.deadline >= datetime.now(timezone.utc),
                    ProductionOrder.status.notin_([
                        ProductionOrderStatus.COMPLETED,
                        ProductionOrderStatus.DELIVERED,
                        ProductionOrderStatus.CANCELLED
                    ])
                )
            )
        )
        orders = result.scalars().all()
        
        for order in orders:
            # Get assignments for this order
            assignments_result = await self.db.execute(
                select(ProductionAssignment)
                .where(ProductionAssignment.production_order_id == order.id)
            )
            assignments = assignments_result.scalars().all()
            
            for assignment in assignments:
                await self.notification_service.create_notification(
                    self.db,
                    user_id=assignment.user_id,
                    message=f"Production order {order.po_number} deadline approaching: {order.deadline.strftime('%Y-%m-%d %H:%M') if order.deadline else 'N/A'}",
                    notification_type=NotificationType.IN_APP,
                    subject="Production Deadline Approaching",
                    metadata={
                        "type": "deadline_approaching",
                        "production_order_id": order.id,
                        "po_number": order.po_number,
                        "deadline": order.deadline.isoformat() if order.deadline else None
                    }
                )
        
        logger.info(
            "Deadline approaching notifications sent",
            orders_count=len(orders)
        )
    
    async def notify_copy_approved(
        self,
        copy_id: int,
        production_order_created: bool = False
    ):
        """Notify when copy is approved and production order is created"""
        from sqlalchemy import select
        result = await self.db.execute(select(Copy).where(Copy.id == copy_id))
        copy_item = result.scalar_one_or_none()
        
        if not copy_item:
            return
        
        # Notify production director if PO was created
        if production_order_created and copy_item.production_order_id:
            from backend.models.user import User
            result = await self.db.execute(
                select(User).where(User.role == 'production_director')
            )
            production_directors = result.scalars().all()
            
            for pd in production_directors:
                await self.notification_service.create_notification(
                    self.db,
                    user_id=pd.id,
                    message=f"Copy '{copy_item.title}' approved. Production order created automatically.",
                    notification_type=NotificationType.IN_APP,
                    subject="Copy Approved - Production Order Created",
                    metadata={
                        "type": "copy_approved_po_created",
                        "copy_id": copy_id,
                        "production_order_id": copy_item.production_order_id
                    }
                )
    
    async def notify_production_completed(
        self,
        production_order_id: int
    ):
        """Notify traffic when production is completed and ready for delivery"""
        from backend.services.production_order_service import ProductionOrderService
        
        po_service = ProductionOrderService(self.db)
        production_order = await po_service.get_by_id(production_order_id)
        
        if not production_order:
            return
        
        # Notify traffic users
        from backend.models.user import User
        result = await self.db.execute(
            select(User).where(User.role.in_(['admin', 'producer']))
        )
        traffic_users = result.scalars().all()
        
        for user in traffic_users:
            await self.notification_service.create_notification(
                self.db,
                user_id=user.id,
                message=f"Production order {production_order.po_number} completed and ready for delivery",
                notification_type=NotificationType.IN_APP,
                subject="Production Completed",
                metadata={
                    "type": "production_completed",
                    "production_order_id": production_order_id,
                    "po_number": production_order.po_number
                }
            )
        
        logger.info(
            "Production completed notifications sent",
            production_order_id=production_order_id
        )

