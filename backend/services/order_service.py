"""
Order service for traffic management
"""

from typing import Optional
from datetime import date
from decimal import Decimal
from sqlalchemy.ext.asyncio import AsyncSession
from backend.models.order import Order, RateType, OrderStatus, ApprovalStatus
from backend.models.order_template import OrderTemplate
import structlog

logger = structlog.get_logger()


class OrderService:
    """Service for managing orders and order-related business logic"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    def validate_date_range(self, start_date: date, end_date: date) -> bool:
        """Check if contract dates are valid"""
        if start_date >= end_date:
            return False
        return True
    
    def calculate_total_value(self, order: Order) -> Decimal:
        """Compute order value from rates"""
        if not order.rates:
            return Decimal("0.00")
        
        total_value = Decimal("0.00")
        
        if order.rate_type == RateType.ROS:
            # Run of Schedule - simple rate per spot
            rate_per_spot = Decimal(str(order.rates.get("rate_per_spot", 0)))
            total_value = rate_per_spot * order.total_spots
        
        elif order.rate_type == RateType.DAYPART:
            # Daypart-specific rates
            daypart_rates = order.rates.get("daypart_rates", {})
            daypart_counts = order.rates.get("daypart_counts", {})
            
            for daypart, rate in daypart_rates.items():
                count = daypart_counts.get(daypart, 0)
                total_value += Decimal(str(rate)) * count
        
        elif order.rate_type == RateType.PROGRAM:
            # Program-specific rates
            program_rates = order.rates.get("program_rates", {})
            program_counts = order.rates.get("program_counts", {})
            
            for program, rate in program_rates.items():
                count = program_counts.get(program, 0)
                total_value += Decimal(str(rate)) * count
        
        elif order.rate_type == RateType.FIXED_TIME:
            # Fixed time slot rates
            fixed_rates = order.rates.get("fixed_rates", [])
            for fixed_rate in fixed_rates:
                total_value += Decimal(str(fixed_rate.get("rate", 0)))
        
        return total_value
    
    def check_approval_required(self, order: Order) -> bool:
        """Determine if approval is needed based on order value or other criteria"""
        # Simple logic: orders over $1000 require approval
        if order.total_value and order.total_value > Decimal("1000.00"):
            return True
        return False
    
    async def create_from_template(
        self,
        template_id: int,
        advertiser_id: int,
        start_date: date,
        end_date: date,
        order_number: str,
        **overrides
    ) -> Order:
        """Create order from template with quick entry"""
        from sqlalchemy import select
        
        # Get template
        result = await self.db.execute(
            select(OrderTemplate).where(OrderTemplate.id == template_id)
        )
        template = result.scalar_one_or_none()
        
        if not template:
            raise ValueError("Template not found")
        
        # Validate date range
        if not self.validate_date_range(start_date, end_date):
            raise ValueError("Start date must be before end date")
        
        # Create order from template
        order_data = {
            "order_number": order_number,
            "advertiser_id": advertiser_id,
            "start_date": start_date,
            "end_date": end_date,
            "spot_lengths": template.default_spot_lengths or overrides.get("spot_lengths"),
            "rate_type": RateType[template.default_rate_type] if template.default_rate_type else RateType.ROS,
            "rates": template.default_rates or overrides.get("rates", {}),
            "total_spots": overrides.get("total_spots", 0),
            "status": OrderStatus.DRAFT,
            "approval_status": ApprovalStatus.NOT_REQUIRED,
        }
        
        # Apply overrides
        order_data.update(overrides)
        
        new_order = Order(**order_data)
        
        # Calculate total value
        new_order.total_value = self.calculate_total_value(new_order)
        
        # Check if approval required
        if self.check_approval_required(new_order):
            new_order.approval_status = ApprovalStatus.PENDING
        
        self.db.add(new_order)
        await self.db.commit()
        await self.db.refresh(new_order)
        
        logger.info("Order created from template", order_id=new_order.id, template_id=template_id)
        
        return new_order

