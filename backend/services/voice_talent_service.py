"""
VoiceTalent Service for managing voice talent requests and takes
"""

from typing import List, Optional, Dict, Any
from uuid import UUID
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from sqlalchemy.orm import selectinload
from backend.models.voice_talent_request import VoiceTalentRequest, TalentRequestStatus, TalentType
from backend.models.production_order import ProductionOrder
import structlog

logger = structlog.get_logger()


class VoiceTalentService:
    """Service for managing voice talent requests"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def create_request(
        self,
        production_order_id: UUID,
        script: str,
        talent_type: TalentType,
        talent_user_id: Optional[UUID] = None,
        pronunciation_guides: Optional[str] = None,
        talent_instructions: Optional[str] = None,
        deadline: Optional[datetime] = None
    ) -> VoiceTalentRequest:
        """Create a voice talent request"""
        # Get production order
        result = await self.db.execute(
            select(ProductionOrder).where(ProductionOrder.id == production_order_id)
        )
        production_order = result.scalar_one_or_none()
        
        if not production_order:
            raise ValueError("Production order not found")
        
        request = VoiceTalentRequest(
            production_order_id=production_order_id,
            talent_user_id=talent_user_id,
            talent_type=talent_type,
            script=script,
            pronunciation_guides=pronunciation_guides,
            talent_instructions=talent_instructions,
            deadline=deadline,
            status=TalentRequestStatus.PENDING if not talent_user_id else TalentRequestStatus.ASSIGNED
        )
        
        if talent_user_id:
            request.assigned_at = datetime.now(timezone.utc)
        
        self.db.add(request)
        await self.db.commit()
        await self.db.refresh(request)
        
        logger.info(
            "Voice talent request created",
            request_id=request.id,
            production_order_id=production_order_id,
            talent_type=talent_type.value
        )
        
        return request
    
    async def assign_talent(
        self,
        request_id: UUID,
        talent_user_id: UUID
    ) -> VoiceTalentRequest:
        """Assign talent to a request"""
        result = await self.db.execute(
            select(VoiceTalentRequest).where(VoiceTalentRequest.id == request_id)
        )
        request = result.scalar_one_or_none()
        
        if not request:
            raise ValueError("Voice talent request not found")
        
        request.talent_user_id = talent_user_id
        request.status = TalentRequestStatus.ASSIGNED
        request.assigned_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(request)
        
        logger.info(
            "Talent assigned to request",
            request_id=request_id,
            talent_user_id=talent_user_id
        )
        
        return request
    
    async def upload_take(
        self,
        request_id: UUID,
        file_path: str,
        file_url: Optional[str] = None,
        take_number: Optional[UUID] = None
    ) -> VoiceTalentRequest:
        """Upload a take for a voice talent request"""
        result = await self.db.execute(
            select(VoiceTalentRequest).where(VoiceTalentRequest.id == request_id)
        )
        request = result.scalar_one_or_none()
        
        if not request:
            raise ValueError("Voice talent request not found")
        
        # Get existing takes or initialize
        takes = request.takes or []
        
        # Determine take number
        if take_number is None:
            take_number = len(takes) + 1
        
        # Add new take
        new_take = {
            "take_number": take_number,
            "file_path": file_path,
            "file_url": file_url,
            "uploaded_at": datetime.now(timezone.utc).isoformat(),
            "approved": False
        }
        
        takes.append(new_take)
        request.takes = takes
        request.status = TalentRequestStatus.UPLOADED
        
        await self.db.commit()
        await self.db.refresh(request)
        
        logger.info(
            "Take uploaded",
            request_id=request_id,
            take_number=take_number
        )
        
        return request
    
    async def approve_take(
        self,
        request_id: UUID,
        take_number: int
    ) -> VoiceTalentRequest:
        """Approve a specific take"""
        result = await self.db.execute(
            select(VoiceTalentRequest).where(VoiceTalentRequest.id == request_id)
        )
        request = result.scalar_one_or_none()
        
        if not request:
            raise ValueError("Voice talent request not found")
        
        takes = request.takes or []
        
        # Find and approve the take
        for take in takes:
            if take.get("take_number") == take_number:
                take["approved"] = True
                take["approved_at"] = datetime.now(timezone.utc).isoformat()
                break
        
        request.takes = takes
        request.status = TalentRequestStatus.APPROVED
        request.completed_at = datetime.now(timezone.utc)
        
        await self.db.commit()
        await self.db.refresh(request)
        
        logger.info(
            "Take approved",
            request_id=request_id,
            take_number=take_number
        )
        
        return request
    
    async def get_requests_for_talent(
        self,
        talent_user_id: UUID,
        status: Optional[TalentRequestStatus] = None
    ) -> List[VoiceTalentRequest]:
        """Get all requests assigned to a talent"""
        query = select(VoiceTalentRequest).where(
            VoiceTalentRequest.talent_user_id == talent_user_id
        )
        
        if status:
            query = query.where(VoiceTalentRequest.status == status)
        
        query = query.order_by(VoiceTalentRequest.deadline.asc())
        
        result = await self.db.execute(query)
        return result.scalars().all()
    
    async def get_by_production_order(
        self,
        production_order_id: UUID
    ) -> Optional[VoiceTalentRequest]:
        """Get voice talent request for a production order"""
        result = await self.db.execute(
            select(VoiceTalentRequest)
            .options(selectinload(VoiceTalentRequest.production_order))
            .where(VoiceTalentRequest.production_order_id == production_order_id)
        )
        return result.scalar_one_or_none()

