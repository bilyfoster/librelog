"""
Create comprehensive test data for LibreLog system testing
"""
import asyncio
import os
import sys
from pathlib import Path
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import AsyncSessionLocal
from backend.models.user import User
from backend.models.advertiser import Advertiser
from backend.models.agency import Agency
from backend.models.sales_rep import SalesRep
from backend.models.order import Order, OrderStatus
from backend.models.copy import Copy
from backend.models.spot import Spot, SpotStatus, Daypart
from backend.models.campaign import Campaign
from backend.models.invoice import Invoice, InvoiceStatus
from backend.models.payment import Payment
from backend.models.track import Track
from backend.models.voice_track import VoiceTrack
from backend.models.copy_assignment import CopyAssignment
from backend.models.makegood import Makegood
from backend.models.clock_template import ClockTemplate
from backend.auth.oauth2 import get_password_hash
import structlog
import random

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
        # Use new format: YYYYMMDD-XXXX (with leading zeros for month and day)
        current_date = datetime.now(timezone.utc)
        # %Y%m%d ensures leading zeros: 20240105 (Jan 5), 20241215 (Dec 15)
        date_prefix = current_date.strftime("%Y%m%d")
        order_number = f"{date_prefix}-{i + 1:04d}"
        
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
        invoice_number = f"INV-{datetime.now().year}-{2000 + i}"
        
        # Check if invoice already exists
        result = await db.execute(
            select(Invoice).where(Invoice.invoice_number == invoice_number)
        )
        existing = result.scalar_one_or_none()
        
        if existing:
            logger.info("Invoice already exists", invoice_number=invoice_number)
            invoices.append(existing)
            continue
        
        invoice_date = datetime.now(timezone.utc).date() - timedelta(days=i * 7)
        due_date = invoice_date + timedelta(days=30)
        
        invoice = Invoice(
            invoice_number=invoice_number,
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

async def create_test_tracks(db: AsyncSession) -> list[Track]:
    """Create test tracks of all types"""
    tracks = []
    
    # Music tracks (MUS)
    music_tracks = [
        {"title": "Dancing Queen", "artist": "ABBA", "album": "Arrival", "genre": "Pop", "duration": 230},
        {"title": "Bohemian Rhapsody", "artist": "Queen", "album": "A Night at the Opera", "genre": "Rock", "duration": 355},
        {"title": "Billie Jean", "artist": "Michael Jackson", "album": "Thriller", "genre": "Pop", "duration": 294},
        {"title": "Sweet Caroline", "artist": "Neil Diamond", "album": "Brother Love's Travelling Salvation Show", "genre": "Pop", "duration": 203},
        {"title": "I Will Survive", "artist": "Gloria Gaynor", "album": "Love Tracks", "genre": "Disco", "duration": 198},
    ]
    
    # Promos (PRO)
    promo_tracks = [
        {"title": "Weekend Show Promo", "artist": "Station", "album": None, "genre": None, "duration": 30},
        {"title": "Morning Drive Promo", "artist": "Station", "album": None, "genre": None, "duration": 30},
        {"title": "Evening Show Promo", "artist": "Station", "album": None, "genre": None, "duration": 30},
    ]
    
    # Liners (LIN)
    liner_tracks = [
        {"title": "GayPHX Radio - Your Community Station", "artist": "Station", "album": None, "genre": None, "duration": 15},
        {"title": "GayPHX Radio - Music for Everyone", "artist": "Station", "album": None, "genre": None, "duration": 20},
        {"title": "GayPHX Radio - Phoenix's Pride", "artist": "Station", "album": None, "genre": None, "duration": 18},
    ]
    
    # Station IDs (IDS)
    id_tracks = [
        {"title": "GayPHX Radio - Top of Hour ID", "artist": "Station", "album": None, "genre": None, "duration": 10},
        {"title": "GayPHX Radio - Station ID", "artist": "Station", "album": None, "genre": None, "duration": 8},
        {"title": "GayPHX Radio - Call Letters", "artist": "Station", "album": None, "genre": None, "duration": 12},
    ]
    
    # News (NEW)
    news_tracks = [
        {"title": "Morning News Update", "artist": "News Department", "album": None, "genre": None, "duration": 120},
        {"title": "Midday News Update", "artist": "News Department", "album": None, "genre": None, "duration": 120},
        {"title": "Evening News Update", "artist": "News Department", "album": None, "genre": None, "duration": 120},
    ]
    
    # PSAs
    psa_tracks = [
        {"title": "Community Health PSA", "artist": "Public Service", "album": None, "genre": None, "duration": 60},
        {"title": "Voting Information PSA", "artist": "Public Service", "album": None, "genre": None, "duration": 45},
        {"title": "Community Event PSA", "artist": "Public Service", "album": None, "genre": None, "duration": 30},
    ]
    
    # Interviews
    interview_tracks = [
        {"title": "Artist Interview - Part 1", "artist": "Interview", "album": None, "genre": None, "duration": 300},
        {"title": "Community Leader Interview", "artist": "Interview", "album": None, "genre": None, "duration": 240},
    ]
    
    # Note: BED type is not currently in the Track model constraint, so excluded for now
    # BED support exists in log generator but needs to be added to model
    
    all_track_data = [
        (music_tracks, "MUS"),
        (promo_tracks, "PRO"),
        (liner_tracks, "LIN"),
        (id_tracks, "IDS"),
        (news_tracks, "NEW"),
        (psa_tracks, "PSA"),
        (interview_tracks, "INT"),
    ]
    
    for track_list, track_type in all_track_data:
        for track_data in track_list:
            # Create fake filepath
            filepath = f"/var/lib/libretime/music/{track_type.lower()}/{track_data['title'].replace(' ', '_')}.mp3"
            
            track = Track(
                title=track_data["title"],
                artist=track_data.get("artist"),
                album=track_data.get("album"),
                type=track_type,
                genre=track_data.get("genre"),
                duration=track_data.get("duration"),
                filepath=filepath,
                libretime_id=f"LT-{track_type}-{random.randint(1000, 9999)}"
            )
            db.add(track)
            tracks.append(track)
    
    await db.commit()
    for track in tracks:
        await db.refresh(track)
    
    logger.info("Created tracks", count=len(tracks))
    return tracks

async def create_test_spots(db: AsyncSession, orders: list[Order], copy_items: list[Copy]) -> list[Spot]:
    """Create test spots"""
    spots = []
    start_date = datetime.now(timezone.utc).date()
    
    for i, order in enumerate(orders):
        # Create multiple spots per order
        for j in range(5):
            spot_date = start_date + timedelta(days=j * 2)
            spot_time = f"{8 + (j * 3) % 16:02d}:{j * 15 % 60:02d}:00"
            
            # Determine daypart based on time
            hour = int(spot_time.split(":")[0])
            if hour < 6:
                daypart = Daypart.OVERNIGHT
            elif hour < 10:
                daypart = Daypart.MORNING_DRIVE
            elif hour < 15:
                daypart = Daypart.MIDDAY
            elif hour < 19:
                daypart = Daypart.AFTERNOON_DRIVE
            elif hour < 24:
                daypart = Daypart.EVENING
            else:
                daypart = Daypart.OVERNIGHT
            
            spot = Spot(
                order_id=order.id,
                campaign_id=None,
                scheduled_date=spot_date,
                scheduled_time=spot_time,
                spot_length=30 if j % 2 == 0 else 60,
                break_position="A" if j % 3 == 0 else "B",
                daypart=daypart,
                status=SpotStatus.SCHEDULED if j < 3 else SpotStatus.AIRED,
                conflict_resolved=False,
            )
            db.add(spot)
            spots.append(spot)
    
    await db.commit()
    for spot in spots:
        await db.refresh(spot)
    
    logger.info("Created spots", count=len(spots))
    return spots

async def create_test_campaigns(db: AsyncSession, advertisers: list[Advertiser]) -> list[Campaign]:
    """Create test campaigns"""
    campaigns = []
    start_date = datetime.now(timezone.utc).date()
    
    campaign_data = [
        {"advertiser": advertisers[0].name, "priority": 1, "days": 30},
        {"advertiser": advertisers[1].name, "priority": 2, "days": 45},
        {"advertiser": advertisers[2].name, "priority": 1, "days": 60},
    ]
    
    for i, camp_data in enumerate(campaign_data):
        advertiser = next((a for a in advertisers if a.name == camp_data["advertiser"]), advertisers[0])
        end_date = start_date + timedelta(days=camp_data["days"])
        
        campaign = Campaign(
            advertiser=advertiser.name,
            start_date=start_date,
            end_date=end_date,
            priority=camp_data["priority"],
            active=True
        )
        db.add(campaign)
        campaigns.append(campaign)
    
    await db.commit()
    for campaign in campaigns:
        await db.refresh(campaign)
    
    logger.info("Created campaigns", count=len(campaigns))
    return campaigns

async def create_test_clock_templates(db: AsyncSession) -> list[ClockTemplate]:
    """Create test clock templates with all element types"""
    templates = []
    
    # Morning template with all element types
    morning_layout = {
        "elements": [
            {"type": "IDS", "count": 1, "hard_start": True, "scheduled_time": 0, "position": "top", "duration": 10},
            {"type": "NEW", "count": 1, "hard_start": True, "scheduled_time": 0, "duration": 120},
            {"type": "MUS", "count": 3, "duration": 180},
            {"type": "ADV", "count": 2, "duration": 30},
            {"type": "LIN", "count": 1, "duration": 15},
            {"type": "PRO", "count": 1, "duration": 30},
            {"type": "PSA", "count": 1, "duration": 60},
        ]
    }
    
    # Afternoon template
    afternoon_layout = {
        "elements": [
            {"type": "IDS", "count": 1, "hard_start": True, "scheduled_time": 0, "position": "top", "duration": 10},
            {"type": "MUS", "count": 4, "duration": 180},
            {"type": "ADV", "count": 3, "duration": 30},
            {"type": "LIN", "count": 2, "duration": 15},
            {"type": "PRO", "count": 1, "duration": 30},
        ]
    }
    
    # Evening template with bottom of hour ID
    evening_layout = {
        "elements": [
            {"type": "MUS", "count": 5, "duration": 180},
            {"type": "ADV", "count": 2, "duration": 30},
            {"type": "LIN", "count": 1, "duration": 15},
            {"type": "IDS", "count": 1, "hard_start": True, "scheduled_time": 3540, "position": "bottom", "duration": 10},
        ]
    }
    
    template_data = [
        {"name": "Morning Drive", "description": "Morning programming template", "layout": morning_layout},
        {"name": "Afternoon Mix", "description": "Afternoon programming template", "layout": afternoon_layout},
        {"name": "Evening Show", "description": "Evening programming template", "layout": evening_layout},
    ]
    
    for temp_data in template_data:
        template = ClockTemplate(
            name=temp_data["name"],
            description=temp_data["description"],
            json_layout=temp_data["layout"]
        )
        db.add(template)
        templates.append(template)
    
    await db.commit()
    for template in templates:
        await db.refresh(template)
    
    logger.info("Created clock templates", count=len(templates))
    return templates

async def create_test_voice_tracks(db: AsyncSession) -> list[VoiceTrack]:
    """Create test voice tracks"""
    voice_tracks = []
    
    # Get a user for uploader
    result = await db.execute(select(User).limit(1))
    user = result.scalar_one_or_none()
    if not user:
        logger.warning("No user found for voice tracks")
        return []
    
    show_names = ["Morning Show", "Afternoon Drive", "Evening Show", "Weekend Special"]
    
    for i, show_name in enumerate(show_names):
        scheduled_time = datetime.now(timezone.utc) + timedelta(days=i, hours=i * 6)
        file_url = f"/var/lib/librelog/voice/{show_name.replace(' ', '_')}_{i}.mp3"
        
        voice_track = VoiceTrack(
            show_name=show_name,
            file_url=file_url,
            scheduled_time=scheduled_time,
            uploaded_by=user.id
        )
        db.add(voice_track)
        voice_tracks.append(voice_track)
    
    await db.commit()
    for vt in voice_tracks:
        await db.refresh(vt)
    
    logger.info("Created voice tracks", count=len(voice_tracks))
    return voice_tracks

async def create_test_copy_assignments(db: AsyncSession, spots: list[Spot], copy_items: list[Copy]) -> list[CopyAssignment]:
    """Create test copy assignments"""
    assignments = []
    
    # Get a user for assigner
    result = await db.execute(select(User).limit(1))
    user = result.scalar_one_or_none()
    
    # Assign copy to some spots
    for i, spot in enumerate(spots[:min(10, len(spots))]):
        if i < len(copy_items):
            assignment = CopyAssignment(
                spot_id=spot.id,
                copy_id=copy_items[i].id,
                order_id=spot.order_id,
                assigned_by=user.id if user else None
            )
            db.add(assignment)
            assignments.append(assignment)
    
    await db.commit()
    for assignment in assignments:
        await db.refresh(assignment)
    
    logger.info("Created copy assignments", count=len(assignments))
    return assignments

async def create_test_payments(db: AsyncSession, invoices: list[Invoice]) -> list[Payment]:
    """Create test payments"""
    payments = []
    
    for i, invoice in enumerate(invoices[:3]):  # Create payments for first 3 invoices
        payment_date = invoice.invoice_date + timedelta(days=15 + i * 5)
        amount = invoice.total * Decimal("0.5") if i == 0 else invoice.total  # Partial payment for first
        
        payment = Payment(
            invoice_id=invoice.id,
            amount=amount,
            payment_date=payment_date,
            payment_method="Check" if i % 2 == 0 else "Credit Card",
            reference_number=f"PAY-{datetime.now().year}-{3000 + i}",
            notes=f"Payment for invoice {invoice.invoice_number}"
        )
        db.add(payment)
        payments.append(payment)
    
    await db.commit()
    for payment in payments:
        await db.refresh(payment)
    
    logger.info("Created payments", count=len(payments))
    return payments

async def create_test_makegoods(db: AsyncSession, spots: list[Spot], campaigns: list[Campaign]) -> list[Makegood]:
    """Create test makegoods"""
    makegoods = []
    
    # Get a user for approver
    result = await db.execute(select(User).limit(1))
    user = result.scalar_one_or_none()
    
    # Create makegoods for some spots
    for i in range(min(3, len(spots) - 1)):
        original_spot = spots[i]
        makegood_spot = spots[i + 1]
        campaign = campaigns[0] if campaigns else None
        
        makegood = Makegood(
            original_spot_id=original_spot.id,
            makegood_spot_id=makegood_spot.id,
            campaign_id=campaign.id if campaign else None,
            reason=f"Original spot {original_spot.id} was preempted",
            approved_by=user.id if user else None,
            approved_at=datetime.now(timezone.utc) if user else None
        )
        db.add(makegood)
        makegoods.append(makegood)
    
    await db.commit()
    for mg in makegoods:
        await db.refresh(mg)
    
    logger.info("Created makegoods", count=len(makegoods))
    return makegoods

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
            
            # Create tracks (all types)
            tracks = await create_test_tracks(db)
            
            # Create spots
            spots = await create_test_spots(db, orders, copy_items)
            
            # Create campaigns
            campaigns = await create_test_campaigns(db, advertisers)
            
            # Create clock templates
            clock_templates = await create_test_clock_templates(db)
            
            # Create voice tracks
            voice_tracks = await create_test_voice_tracks(db)
            
            # Create copy assignments
            copy_assignments = await create_test_copy_assignments(db, spots, copy_items)
            
            # Create payments
            payments = await create_test_payments(db, invoices)
            
            # Create makegoods
            makegoods = await create_test_makegoods(db, spots, campaigns)
            
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
            print(f"  - {len(tracks)} tracks (all types)")
            print(f"  - {len(spots)} spots")
            print(f"  - {len(campaigns)} campaigns")
            print(f"  - {len(clock_templates)} clock templates")
            print(f"  - {len(voice_tracks)} voice tracks")
            print(f"  - {len(copy_assignments)} copy assignments")
            print(f"  - {len(payments)} payments")
            print(f"  - {len(makegoods)} makegoods")
            print("=" * 60)
            
        except Exception as e:
            logger.error("Test data creation failed", error=str(e), exc_info=True)
            await db.rollback()
            raise

if __name__ == "__main__":
    asyncio.run(main())
