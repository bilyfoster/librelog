"""
Create QA test data for BearUnion campaign testing
This script sets up all prerequisites for the QA test scenario
"""
import asyncio
import os
import sys
from pathlib import Path
from datetime import datetime, timedelta, time, timezone
from decimal import Decimal
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import AsyncSessionLocal
from backend.models.user import User
from backend.models.advertiser import Advertiser
from backend.models.sales_rep import SalesRep
from backend.models.order import Order, OrderStatus, RateType, ApprovalStatus
from backend.models.copy import Copy
from backend.models.spot import Spot, SpotStatus
from backend.models.daypart import Daypart
from backend.models.daypart_category import DaypartCategory
from backend.auth.oauth2 import get_password_hash
import structlog

logger = structlog.get_logger()


async def create_standard_daypart_categories(db: AsyncSession) -> list[DaypartCategory]:
    """Create standard daypart categories"""
    categories_data = [
        {
            "name": "Drive Time",
            "description": "High-audience drive time periods",
            "color": "#1976d2",
            "sort_order": 1
        },
        {
            "name": "Prime Time",
            "description": "Prime listening hours",
            "color": "#d32f2f",
            "sort_order": 2
        },
        {
            "name": "Off-Peak",
            "description": "Lower audience periods",
            "color": "#757575",
            "sort_order": 3
        },
        {
            "name": "Weekend",
            "description": "Weekend programming",
            "color": "#388e3c",
            "sort_order": 4
        }
    ]
    
    categories = []
    for cat_data in categories_data:
        result = await db.execute(
            select(DaypartCategory).where(DaypartCategory.name == cat_data["name"])
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            logger.info("Daypart category already exists", name=cat_data["name"])
            categories.append(existing)
            continue
        
        category = DaypartCategory(**cat_data, active=True)
        db.add(category)
        categories.append(category)
    
    await db.commit()
    for cat in categories:
        await db.refresh(cat)
    
    logger.info("Created daypart categories", count=len(categories))
    return categories


async def create_standard_dayparts(db: AsyncSession, categories: list[DaypartCategory]) -> list[Daypart]:
    """Create standard dayparts"""
    # Map category names to IDs
    category_map = {cat.name: cat.id for cat in categories}
    
    dayparts_data = [
        {
            "name": "Morning Drive",
            "start_time": time(6, 0),
            "end_time": time(10, 0),
            "days_of_week": [0, 1, 2, 3, 4],  # Monday-Friday
            "category_id": category_map.get("Drive Time"),
            "description": "Morning drive time - highest audience"
        },
        {
            "name": "Midday",
            "start_time": time(10, 0),
            "end_time": time(15, 0),
            "days_of_week": [0, 1, 2, 3, 4],  # Monday-Friday
            "category_id": category_map.get("Prime Time"),
            "description": "Midday programming"
        },
        {
            "name": "Afternoon Drive",
            "start_time": time(15, 0),
            "end_time": time(19, 0),
            "days_of_week": [0, 1, 2, 3, 4],  # Monday-Friday
            "category_id": category_map.get("Drive Time"),
            "description": "Afternoon drive time - high audience"
        },
        {
            "name": "Evening",
            "start_time": time(19, 0),
            "end_time": time(0, 0),
            "days_of_week": [0, 1, 2, 3, 4],  # Monday-Friday
            "category_id": category_map.get("Prime Time"),
            "description": "Evening programming"
        },
        {
            "name": "Overnight",
            "start_time": time(0, 0),
            "end_time": time(6, 0),
            "days_of_week": [0, 1, 2, 3, 4],  # Monday-Friday
            "category_id": category_map.get("Off-Peak"),
            "description": "Overnight programming"
        },
        {
            "name": "Weekend Morning",
            "start_time": time(8, 0),
            "end_time": time(12, 0),
            "days_of_week": [5, 6],  # Saturday-Sunday
            "category_id": category_map.get("Weekend"),
            "description": "Weekend morning programming"
        },
        {
            "name": "Weekend Afternoon",
            "start_time": time(12, 0),
            "end_time": time(18, 0),
            "days_of_week": [5, 6],  # Saturday-Sunday
            "category_id": category_map.get("Weekend"),
            "description": "Weekend afternoon programming"
        },
        {
            "name": "Weekend Evening",
            "start_time": time(18, 0),
            "end_time": time(0, 0),
            "days_of_week": [5, 6],  # Saturday-Sunday
            "category_id": category_map.get("Weekend"),
            "description": "Weekend evening programming"
        }
    ]
    
    dayparts = []
    for dp_data in dayparts_data:
        result = await db.execute(
            select(Daypart).where(Daypart.name == dp_data["name"])
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            logger.info("Daypart already exists", name=dp_data["name"])
            dayparts.append(existing)
            continue
        
        daypart = Daypart(**dp_data, active=True)
        db.add(daypart)
        dayparts.append(daypart)
    
    await db.commit()
    for dp in dayparts:
        await db.refresh(dp)
    
    logger.info("Created standard dayparts", count=len(dayparts))
    return dayparts


async def create_donny_demo_sales_rep(db: AsyncSession) -> SalesRep:
    """Create Donny Demo sales rep"""
    username = "donnydemo"
    
    # Check if user exists
    result = await db.execute(
        select(User).where(User.username == username)
    )
    user = result.scalar_one_or_none()
    
    if not user:
        user = User(
            username=username,
            password_hash=get_password_hash("demo123"),
            role="sales"
        )
        db.add(user)
        await db.flush()
        logger.info("Created user for Donny Demo", username=username)
    else:
        logger.info("User already exists", username=username)
    
    # Check if sales rep exists
    result = await db.execute(
        select(SalesRep).where(SalesRep.user_id == user.id)
    )
    sales_rep = result.scalar_one_or_none()
    
    if sales_rep:
        logger.info("Sales rep already exists for Donny Demo", user_id=user.id)
        return sales_rep
    
    sales_rep = SalesRep(
        user_id=user.id,
        employee_id="DEMO-001",
        commission_rate=Decimal("10.00"),
        active=True
    )
    db.add(sales_rep)
    await db.commit()
    await db.refresh(sales_rep)
    
    logger.info("Created Donny Demo sales rep", sales_rep_id=sales_rep.id)
    return sales_rep


async def create_mustachephx_advertiser(db: AsyncSession) -> Advertiser:
    """Create MustachePHX advertiser"""
    result = await db.execute(
        select(Advertiser).where(Advertiser.name == "MustachePHX")
    )
    advertiser = result.scalar_one_or_none()
    
    if advertiser:
        logger.info("MustachePHX advertiser already exists", advertiser_id=advertiser.id)
        return advertiser
    
    advertiser = Advertiser(
        name="MustachePHX",
        contact_name="Demo Contact",
        email="contact@mustachephx.com",
        phone="602-555-0000",
        address="123 Demo St, Phoenix, AZ 85001",
        payment_terms="Net 30",
        credit_limit=Decimal("5000.00"),
        active=True
    )
    db.add(advertiser)
    await db.commit()
    await db.refresh(advertiser)
    
    logger.info("Created MustachePHX advertiser", advertiser_id=advertiser.id)
    return advertiser


async def find_bearunion_copy(db: AsyncSession) -> Copy:
    """Find BearUnion-Nov25.mp3 copy in the system"""
    # Try to find by title containing "BearUnion" or "Nov25"
    # Use raw SQL to avoid schema mismatch issues
    from sqlalchemy import text, or_
    try:
        # Try simple query first
        result = await db.execute(
            select(Copy.id, Copy.title, Copy.audio_file_path, Copy.order_id, Copy.advertiser_id).where(
                or_(
                    Copy.title.ilike("%BearUnion%"),
                    Copy.title.ilike("%Nov25%"),
                    Copy.audio_file_path.ilike("%BearUnion%"),
                    Copy.audio_file_path.ilike("%Nov25%")
                )
            ).limit(1)
        )
        row = result.first()
        
        if row:
            # Get full object
            copy_item = await db.get(Copy, row.id)
            logger.info("Found BearUnion copy", copy_id=copy_item.id, title=copy_item.title)
            return copy_item
    except Exception as e:
        logger.warning("Error searching for copy, will skip", error=str(e))
    
    logger.warning("BearUnion-Nov25.mp3 copy not found in system - will need to link manually")
    return None


async def create_bearunion_order(
    db: AsyncSession,
    advertiser: Advertiser,
    sales_rep: SalesRep
) -> Order:
    """Create BearUnion order for QA testing"""
    start_date = datetime(2024, 11, 19).date()
    end_date = datetime(2024, 11, 22).date()
    
    # Generate order number
    order_number = f"20241119-0001"
    
    result = await db.execute(
        select(Order).where(Order.order_number == order_number)
    )
    existing = result.scalar_one_or_none()
    
    if existing:
        logger.info("BearUnion order already exists", order_number=order_number)
        return existing
    
    order = Order(
        order_number=order_number,
        advertiser_id=advertiser.id,
        sales_rep_id=sales_rep.id,
        start_date=start_date,
        end_date=end_date,
        spot_lengths=[30],  # 30 second spots
        total_spots=10,
        rate_type=RateType.ROS,
        rates={"ROS": float(Decimal("10.00"))},  # Trade at $10/spot (convert to float for JSON)
        total_value=Decimal("100.00"),  # 10 spots × $10
        status=OrderStatus.DRAFT,
        approval_status=ApprovalStatus.NOT_REQUIRED
    )
    db.add(order)
    await db.commit()
    await db.refresh(order)
    
    logger.info("Created BearUnion order", order_id=order.id, order_number=order_number)
    return order


async def link_copy_to_order(db: AsyncSession, copy_item: Copy, order: Order) -> bool:
    """Link existing copy to the order"""
    if not copy_item:
        logger.warning("Cannot link copy - copy not found")
        return False
    
    # Update copy to link to order
    copy_item.order_id = order.id
    if not copy_item.advertiser_id:
        copy_item.advertiser_id = order.advertiser_id
    
    await db.commit()
    await db.refresh(copy_item)
    
    logger.info("Linked copy to order", copy_id=copy_item.id, order_id=order.id)
    return True


async def main():
    """Main function to create all QA test data"""
    async with AsyncSessionLocal() as db:
        try:
            logger.info("Starting QA test data creation")
            
            # 1. Create daypart categories
            logger.info("Step 1: Creating daypart categories")
            categories = await create_standard_daypart_categories(db)
            
            # 2. Create standard dayparts
            logger.info("Step 2: Creating standard dayparts")
            dayparts = await create_standard_dayparts(db, categories)
            
            # 3. Create Donny Demo sales rep
            logger.info("Step 3: Creating Donny Demo sales rep")
            sales_rep = await create_donny_demo_sales_rep(db)
            
            # 4. Create MustachePHX advertiser
            logger.info("Step 4: Creating MustachePHX advertiser")
            advertiser = await create_mustachephx_advertiser(db)
            
            # 5. Find BearUnion copy
            logger.info("Step 5: Finding BearUnion-Nov25.mp3 copy")
            copy_item = await find_bearunion_copy(db)
            
            # 6. Create BearUnion order
            logger.info("Step 6: Creating BearUnion order")
            order = await create_bearunion_order(db, advertiser, sales_rep)
            
            # 7. Link copy to order
            if copy_item:
                logger.info("Step 7: Linking copy to order")
                await link_copy_to_order(db, copy_item, order)
            else:
                logger.warning("Step 7: Copy not found - cannot link to order")
                logger.warning("Please manually link BearUnion-Nov25.mp3 to order after upload")
            
            logger.info("QA test data creation completed successfully")
            logger.info("Next steps:")
            logger.info("1. Verify BearUnion-Nov25.mp3 copy is linked to order")
            logger.info("2. Change order status to PENDING")
            logger.info("3. Approve order (status: APPROVED)")
            logger.info("4. Use Spot Scheduler to schedule 10 spots")
            logger.info("5. Verify spots are created and copy is assigned")
            
        except Exception as e:
            logger.error("Error creating QA test data", error=str(e), exc_info=True)
            await db.rollback()
            raise


if __name__ == "__main__":
    asyncio.run(main())

