"""
Migration script to convert order numbers from ORD-YYYY-NNNN to YYYYMMDD-XXXX format
"""
import asyncio
import sys
import re
from pathlib import Path
from datetime import datetime, timezone
from typing import Set

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update
from backend.database import AsyncSessionLocal
from backend.models.order import Order
import structlog

logger = structlog.get_logger()


async def migrate_order_numbers(db: AsyncSession) -> dict:
    """
    Migrate all orders from ORD-YYYY-NNNN format to YYYYMMDD-XXXX format.
    Uses the order's start_date for the date prefix, or created_at if start_date is not available.
    """
    # Find all orders with old format
    result = await db.execute(
        select(Order).where(Order.order_number.like("ORD-%"))
    )
    old_orders = result.scalars().all()
    
    if not old_orders:
        logger.info("No orders found with old format")
        return {
            "total": 0,
            "migrated": 0,
            "skipped": 0,
            "errors": 0
        }
    
    logger.info(f"Found {len(old_orders)} orders with old format")
    
    stats = {
        "total": len(old_orders),
        "migrated": 0,
        "skipped": 0,
        "errors": 0
    }
    
    # Group orders by date to handle numbering
    orders_by_date = {}
    
    for order in old_orders:
        # Use start_date if available, otherwise use created_at
        if order.start_date:
            date_key = order.start_date
        elif order.created_at:
            date_key = order.created_at.date()
        else:
            # Fallback to today's date
            date_key = datetime.now(timezone.utc).date()
            logger.warning(f"Order {order.id} has no start_date or created_at, using today's date")
        
        if date_key not in orders_by_date:
            orders_by_date[date_key] = []
        orders_by_date[date_key].append(order)
    
    # Process each date group
    for date_key, orders in orders_by_date.items():
        # %Y%m%d ensures leading zeros: 20240105 (Jan 5), 20241215 (Dec 15)
        date_prefix = date_key.strftime("%Y%m%d")
        
        # Sort orders by created_at to maintain relative order
        orders_sorted = sorted(orders, key=lambda o: o.created_at if o.created_at else datetime.min.replace(tzinfo=timezone.utc))
        
        # Check existing orders for this date to find the next available number
        existing_result = await db.execute(
            select(Order.order_number)
            .where(Order.order_number.like(f"{date_prefix}-%"))
            .where(~Order.order_number.like("ORD-%"))  # Exclude old format
        )
        existing_numbers: Set[str] = set(existing_result.scalars().all())
        
        # Extract existing number parts
        max_number = 0
        for num in existing_numbers:
            match = re.search(rf"{re.escape(date_prefix)}-(\d+)", num)
            if match:
                max_number = max(max_number, int(match.group(1)))
        
        # Assign new numbers starting from max_number + 1
        next_number = max_number + 1
        
        for order in orders_sorted:
            new_order_number = f"{date_prefix}-{next_number:04d}"
            
            # Check if this new number already exists (shouldn't happen, but be safe)
            check_result = await db.execute(
                select(Order).where(Order.order_number == new_order_number)
            )
            if check_result.scalar_one_or_none():
                logger.warning(f"Order number {new_order_number} already exists, skipping order {order.id}")
                stats["skipped"] += 1
                continue
            
            try:
                # Update the order
                await db.execute(
                    update(Order)
                    .where(Order.id == order.id)
                    .values(order_number=new_order_number)
                )
                
                logger.info(
                    f"Migrated order {order.id}: {order.order_number} -> {new_order_number}",
                    old_number=order.order_number,
                    new_number=new_order_number
                )
                
                stats["migrated"] += 1
                next_number += 1
                
            except Exception as e:
                logger.error(f"Error migrating order {order.id}: {e}")
                stats["errors"] += 1
    
    # Commit all changes
    await db.commit()
    
    return stats


async def main():
    """Main migration function"""
    async with AsyncSessionLocal() as db:
        try:
            logger.info("Starting order number migration...")
            stats = await migrate_order_numbers(db)
            
            logger.info("Migration completed", **stats)
            print(f"\nMigration Summary:")
            print(f"  Total orders found: {stats['total']}")
            print(f"  Successfully migrated: {stats['migrated']}")
            print(f"  Skipped: {stats['skipped']}")
            print(f"  Errors: {stats['errors']}")
            
        except Exception as e:
            logger.error(f"Migration failed: {e}")
            await db.rollback()
            raise


if __name__ == "__main__":
    asyncio.run(main())

