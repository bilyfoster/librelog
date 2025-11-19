"""
ProductionRouting Service for smart routing of production orders
"""

from typing import List, Optional, Dict, Any
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func
from backend.models.production_order import ProductionOrder, ProductionOrderStatus
from backend.models.production_assignment import ProductionAssignment, AssignmentType, AssignmentStatus
from backend.models.user import User
from backend.models.user_roles import Role
from backend.services.production_notification_service import ProductionNotificationService
import structlog

logger = structlog.get_logger()


class ProductionRoutingService:
    """Service for routing production orders to appropriate users"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def find_available_producers(
        self,
        station_id: Optional[int] = None,
        exclude_user_ids: Optional[List[int]] = None
    ) -> List[User]:
        """Find available producers based on station and workload"""
        query = select(User).where(User.role == Role.PRODUCER.value)
        
        if exclude_user_ids:
            query = query.where(~User.id.in_(exclude_user_ids))
        
        result = await self.db.execute(query)
        producers = result.scalars().all()
        
        # TODO: Add workload balancing logic here
        # For now, return all producers
        
        return list(producers)
    
    async def find_available_voice_talent(
        self,
        talent_type: str,
        exclude_user_ids: Optional[List[int]] = None
    ) -> List[User]:
        """Find available voice talent based on type"""
        query = select(User).where(User.role == Role.VOICE_TALENT.value)
        
        if exclude_user_ids:
            query = query.where(~User.id.in_(exclude_user_ids))
        
        result = await self.db.execute(query)
        talent = result.scalars().all()
        
        # TODO: Add talent type matching logic
        # For now, return all voice talent
        
        return list(talent)
    
    async def assign_producer(
        self,
        production_order_id: int,
        producer_id: int,
        notes: Optional[str] = None
    ) -> ProductionAssignment:
        """Assign a producer to a production order"""
        # Check if assignment already exists
        result = await self.db.execute(
            select(ProductionAssignment).where(
                and_(
                    ProductionAssignment.production_order_id == production_order_id,
                    ProductionAssignment.assignment_type == AssignmentType.PRODUCER
                )
            )
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            # Update existing assignment
            existing.user_id = producer_id
            existing.status = AssignmentStatus.PENDING
            if notes:
                existing.notes = notes
            await self.db.commit()
            await self.db.refresh(existing)
            return existing
        
        # Create new assignment
        assignment = ProductionAssignment(
            production_order_id=production_order_id,
            user_id=producer_id,
            assignment_type=AssignmentType.PRODUCER,
            status=AssignmentStatus.PENDING,
            notes=notes
        )
        
        self.db.add(assignment)
        
        # Update production order status
        po_result = await self.db.execute(
            select(ProductionOrder).where(ProductionOrder.id == production_order_id)
        )
        production_order = po_result.scalar_one_or_none()
        
        if production_order:
            if production_order.status == ProductionOrderStatus.PENDING:
                production_order.status = ProductionOrderStatus.ASSIGNED
        
        await self.db.commit()
        await self.db.refresh(assignment)
        
        # Send notification
        try:
            notification_service = ProductionNotificationService(self.db)
            await notification_service.notify_assignment(assignment.id)
        except Exception as e:
            logger.warning("Failed to send assignment notification", error=str(e))
        
        logger.info(
            "Producer assigned to production order",
            production_order_id=production_order_id,
            producer_id=producer_id
        )
        
        return assignment
    
    async def assign_voice_talent(
        self,
        production_order_id: int,
        talent_id: int,
        notes: Optional[str] = None
    ) -> ProductionAssignment:
        """Assign voice talent to a production order"""
        # Check if assignment already exists
        result = await self.db.execute(
            select(ProductionAssignment).where(
                and_(
                    ProductionAssignment.production_order_id == production_order_id,
                    ProductionAssignment.assignment_type == AssignmentType.VOICE_TALENT
                )
            )
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            existing.user_id = talent_id
            existing.status = AssignmentStatus.PENDING
            if notes:
                existing.notes = notes
            await self.db.commit()
            await self.db.refresh(existing)
            return existing
        
        assignment = ProductionAssignment(
            production_order_id=production_order_id,
            user_id=talent_id,
            assignment_type=AssignmentType.VOICE_TALENT,
            status=AssignmentStatus.PENDING,
            notes=notes
        )
        
        self.db.add(assignment)
        await self.db.commit()
        await self.db.refresh(assignment)
        
        logger.info(
            "Voice talent assigned to production order",
            production_order_id=production_order_id,
            talent_id=talent_id
        )
        
        return assignment
    
    async def auto_route(
        self,
        production_order_id: int,
        station_id: Optional[int] = None
    ) -> Dict[str, Any]:
        """Auto-route production order to appropriate users"""
        result = await self.db.execute(
            select(ProductionOrder).where(ProductionOrder.id == production_order_id)
        )
        production_order = result.scalar_one_or_none()
        
        if not production_order:
            raise ValueError("Production order not found")
        
        assignments = {}
        
        # Route to producer
        producers = await self.find_available_producers(station_id=station_id)
        if producers:
            producer_assignment = await self.assign_producer(
                production_order_id,
                producers[0].id
            )
            assignments['producer'] = producer_assignment
        
        # Route to voice talent if needed
        if production_order.talent_needs:
            talent_type = production_order.talent_needs.get('type', 'any')
            talent_list = await self.find_available_voice_talent(talent_type)
            if talent_list:
                talent_assignment = await self.assign_voice_talent(
                    production_order_id,
                    talent_list[0].id
                )
                assignments['voice_talent'] = talent_assignment
        
        logger.info(
            "Production order auto-routed",
            production_order_id=production_order_id,
            assignments=assignments
        )
        
        return assignments

