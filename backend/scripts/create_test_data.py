"""
Create comprehensive test data for LibreLog system testing
"""
import asyncio
import os
import sys
from pathlib import Path
from datetime import datetime, timedelta, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import AsyncSessionLocal
from backend.models.user import User
from backend.models.advertiser import Advertiser
from backend.models.agency import Agency
from backend.models.sales_rep import SalesRep
from backend.models.order import Order, OrderStatus
from backend.models.copy import Copy
from backend.models.spot import Spot, SpotStatus
from backend.models.campaign import Campaign
from backend.models.invoice import Invoice, InvoiceStatus
from backend.models.payment import Payment
from backend.auth.oauth2 import get_password_hash
import structlog

logger = structlog.get_logger()

# Test data constants
TEST_ADVERTISERS = [
    {"name": "Phoenix Auto Group", "contact_name": "John Smith", "email": "john@phoenixauto.com", "phone": "602-555-0101"},
    {"name": "Desert Realty", "contact_name": "Sarah Johnson", "email": "sarah@desertrealty.com", "phone": "602-555-0102"},
    {"name": "Valley Medical Center", "contact_name": "Dr. Michael Brown", "email": "mbrown@valleymed.com", "phone": "602-555-0103"},
    {"name": "Arizona State University", "contact_name": "Jennifer Davis", "email": "jdavis@asu.edu", "phone": "480-555-0104"},
    {"name": "Phoenix Suns", "contact_name": "Robert Wilson", "email": "rwilson@suns.com", "phone": "602-555-0105"},
    {"name": "Southwest Airlines", "contact_name": "Lisa Martinez", "email": "lmartinez@southwest.com", "phone": "602-555-0106"},
    {"name": "Fry's Food Stores", "contact_name": "David Anderson", "email": "danderson@frys.com", "phone": "602-555-0107"},
    {"name": "Banner Health", "contact_name": "Patricia Taylor", "email": "ptaylor@banner.org", "phone": "602-555-0108"},
]

TEST_AGENCIES = [
    {"name": "Desert Media Group", "contact_name": "Mark Thompson", "email": "mark@desertmedia.com"},
    {"name": "Valley Advertising", "contact_name": "Emily White", "email": "emily@valleyad.com"},
    {"name": "Phoenix Creative", "contact_name": "James Garcia", "email": "james@phoenixcreative.com"},
]

TEST_SALES_REPS = [
    {"name": "Alex Rodriguez", "email": "arodriguez@librelog.local", "phone": "602-555-0201"},
    {"name": "Maria Gonzalez", "email": "mgonzalez@librelog.local", "phone": "602-555-0202"},
    {"name": "Chris Johnson", "email": "cjohnson@librelog.local", "phone": "602-555-0203"},
]

async def create_test_advertisers(db: AsyncSession) -> list[Advertiser]:
    """Create test advertisers"""
    advertisers = []
    for adv_data in TEST_ADVERTISERS:
        # Check if advertiser already exists
        result = await db.execute(
            select(Advertiser).where(Advertiser.name == adv_data["name"])
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            logger.info("Advertiser already exists", name=adv_data["name"])
            advertisers.append(existing)
            continue
        
        advertiser = Advertiser(**adv_data)
        db.add(advertiser)
        advertisers.append(advertiser)
    
    await db.commit()
    for adv in advertisers:
        await db.refresh(adv)
    
    logger.info("Created advertisers", count=len(advertisers))
    return advertisers

async def create_test_agencies(db: AsyncSession) -> list[Agency]:
    """Create test agencies"""
    agencies = []
    for agency_data in TEST_AGENCIES:
        result = await db.execute(
            select(Agency).where(Agency.name == agency_data["name"])
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            logger.info("Agency already exists", name=agency_data["name"])
            agencies.append(existing)
            continue
        
        agency = Agency(**agency_data)
        db.add(agency)
        agencies.append(agency)
    
    await db.commit()
    for agency in agencies:
        await db.refresh(agency)
    
    logger.info("Created agencies", count=len(agencies))
    return agencies

async def create_test_sales_reps(db: AsyncSession) -> list[SalesRep]:
    """Create test sales reps (requires users first)"""
    reps = []
    for rep_data in TEST_SALES_REPS:
        # First, create or get user
        username = rep_data["email"].split("@")[0]  # Use email prefix as username
        result = await db.execute(
            select(User).where(User.username == username)
        )
        user = result.scalar_one_or_none()
        
        if not user:
            user = User(
                username=username,
                password_hash=get_password_hash("test123"),  # Default password
                role="sales"
            )
            db.add(user)
            await db.flush()
        
        # Check if sales rep already exists for this user
        result = await db.execute(
            select(SalesRep).where(SalesRep.user_id == user.id)
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            logger.info("Sales rep already exists", user_id=user.id)
            reps.append(existing)
            continue
        
        rep = SalesRep(
            user_id=user.id,
            employee_id=f"EMP-{1000 + len(reps)}",
            commission_rate=5.0,
            active=True
        )
        db.add(rep)
        reps.append(rep)
    
    await db.commit()
    for rep in reps:
        await db.refresh(rep)
    
    logger.info("Created sales reps", count=len(reps))
    return reps

async def create_test_orders(db: AsyncSession, advertisers: list[Advertiser], sales_reps: list[SalesRep]) -> list[Order]:
    """Create test orders/campaigns"""
    orders = []
    start_date = datetime.now(timezone.utc).date()
    
    for i, advertiser in enumerate(advertisers[:10]):  # Create orders for first 10 advertisers
        order_number = f"ORD-{datetime.now().year}-{1000 + i}"
        
        # Check if order already exists
        result = await db.execute(
            select(Order).where(Order.order_number == order_number)
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            logger.info("Order already exists", order_number=order_number)
            orders.append(existing)
            continue
        
        end_date = start_date + timedelta(days=30 + (i * 7))
        
        base_rate = 500.00 + (i * 50)
        order = Order(
            order_number=order_number,
            advertiser_id=advertiser.id,
            sales_rep_id=sales_reps[i % len(sales_reps)].id,
            start_date=start_date,
            end_date=end_date,
            status=OrderStatus.ACTIVE,
            total_spots=100 + (i * 10),
            spot_lengths=[30, 60],  # JSONB array
            rate_type="ROS",
            rates={"ROS": base_rate},  # JSONB object
            total_value=base_rate * (100 + (i * 10)),
        )
        db.add(order)
        orders.append(order)
    
    await db.commit()
    for order in orders:
        await db.refresh(order)
    
    logger.info("Created orders", count=len(orders))
    return orders

async def create_test_copy(db: AsyncSession, orders: list[Order], advertisers: list[Advertiser]) -> list[Copy]:
    """Create test copy items (without actual audio files for now)"""
    copy_items = []
    
    # Get copy files directory
    copy_files_dir = os.getenv("COPY_FILES_DIR", "/var/lib/librelog/copy_files")
    try:
        Path(copy_files_dir).mkdir(parents=True, exist_ok=True)
    except PermissionError:
        copy_files_dir = "/tmp/librelog/copy_files"
        Path(copy_files_dir).mkdir(parents=True, exist_ok=True)
    
    copy_titles = [
        "Summer Sale Announcement",
        "Grand Opening Special",
        "Holiday Promotion",
        "New Product Launch",
        "Community Event",
        "Special Offer",
        "Anniversary Celebration",
        "Back to School",
        "Black Friday Deal",
        "Year End Clearance",
        "Spring Collection",
        "Summer Fun",
        "Fall Festival",
        "Winter Wonderland",
        "Valentine's Special",
        "Easter Promotion",
        "Memorial Day Sale",
        "Independence Day",
        "Labor Day Weekend",
        "Thanksgiving Special",
    ]
    
    for i, title in enumerate(copy_titles):
        order = orders[i % len(orders)] if orders else None
        advertiser = advertisers[i % len(advertisers)]
        
        expires_at = datetime.now(timezone.utc) + timedelta(days=30 + (i * 5))
        
        copy_item = Copy(
            title=title,
            order_id=order.id if order else None,
            advertiser_id=advertiser.id,
            script_text=f"This is the script text for {title}. Please read this carefully and with enthusiasm.",
            audio_file_path=None,  # Will be set when audio is uploaded
            audio_file_url=None,
            expires_at=expires_at,
            active=True,
            version=1,
        )
        db.add(copy_item)
        copy_items.append(copy_item)
    
    await db.commit()
    for copy_item in copy_items:
        await db.refresh(copy_item)
    
    logger.info("Created copy items", count=len(copy_items))
    return copy_items

async def create_test_invoices(db: AsyncSession, orders: list[Order], advertisers: list[Advertiser]) -> list[Invoice]:
    """Create test invoices"""
    invoices = []
    
    for i, order in enumerate(orders[:5]):  # Create invoices for first 5 orders
        invoice_date = datetime.now(timezone.utc).date() - timedelta(days=i * 7)
        due_date = invoice_date + timedelta(days=30)
        
        invoice = Invoice(
            invoice_number=f"INV-{datetime.now().year}-{2000 + i}",
            advertiser_id=order.advertiser_id,
            order_id=order.id,
            invoice_date=invoice_date,
            due_date=due_date,
            subtotal=order.total_value or 0.0,
            tax=0.0,
            total=order.total_value or 0.0,
            status=InvoiceStatus.DRAFT if i < 2 else InvoiceStatus.SENT,
            payment_terms="Net 30",
        )
        db.add(invoice)
        invoices.append(invoice)
    
    await db.commit()
    for invoice in invoices:
        await db.refresh(invoice)
    
    logger.info("Created invoices", count=len(invoices))
    return invoices

async def main():
    """Main function to create all test data"""
    logger.info("Starting test data creation")
    
    async with AsyncSessionLocal() as db:
        try:
            # Create advertisers
            advertisers = await create_test_advertisers(db)
            
            # Create agencies
            agencies = await create_test_agencies(db)
            
            # Create sales reps
            sales_reps = await create_test_sales_reps(db)
            
            # Create orders
            orders = await create_test_orders(db, advertisers, sales_reps)
            
            # Create copy items
            copy_items = await create_test_copy(db, orders, advertisers)
            
            # Create invoices
            invoices = await create_test_invoices(db, orders, advertisers)
            
            logger.info("Test data creation completed successfully")
            print("=" * 60)
            print("TEST DATA CREATION COMPLETE")
            print("=" * 60)
            print(f"Created:")
            print(f"  - {len(advertisers)} advertisers")
            print(f"  - {len(agencies)} agencies")
            print(f"  - {len(sales_reps)} sales reps")
            print(f"  - {len(orders)} orders")
            print(f"  - {len(copy_items)} copy items")
            print(f"  - {len(invoices)} invoices")
            print("=" * 60)
            
        except Exception as e:
            logger.error("Test data creation failed", error=str(e), exc_info=True)
            await db.rollback()
            raise

if __name__ == "__main__":
    asyncio.run(main())
