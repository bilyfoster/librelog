"""
Seed database with admin user and comprehensive demo data.

This script creates:
1. Admin user (admin/admin123)
2. Comprehensive demo order with all features:
   - Advertiser, Agency, Campaign
   - Sales team structure
   - Order with multiple order lines
   - Copy/scripts
   - Production orders
   - Spots
   - Invoices
   - And more...

Usage:
    python seed_demo_data.py [--database-url DATABASE_URL]
"""
import asyncio
import os
import sys
from datetime import date, datetime, timedelta
from pathlib import Path
from uuid import uuid4
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy import text, select
import structlog
from backend.auth.oauth2 import get_password_hash

logger = structlog.get_logger()


async def seed_demo_data(db_url: str) -> None:
    """
    Seed database with admin user and comprehensive demo data.
    
    Args:
        db_url: Database connection URL
    """
    # Convert to async URL if needed
    if db_url.startswith("postgresql://"):
        db_url = db_url.replace("postgresql://", "postgresql+asyncpg://", 1)
    
    # Create async engine
    engine = create_async_engine(
        db_url,
        echo=False,
        pool_pre_ping=True,
    )
    
    # Create session factory
    AsyncSessionLocal = async_sessionmaker(
        engine,
        class_=AsyncSession,
        expire_on_commit=False
    )
    
    try:
        async with AsyncSessionLocal() as session:
            print("🌱 Seeding demo data...")
            
            # 1. Create Admin User
            print("  Creating admin user...")
            admin_password_hash = get_password_hash("admin123")
            
            # Check if admin exists
            admin_check = await session.execute(
                text("SELECT id FROM users WHERE username = 'admin'")
            )
            if admin_check.fetchone():
                print("  ✓ Admin user already exists")
            else:
                await session.execute(
                    text("""
                        INSERT INTO users (username, password_hash, role, permissions, created_at)
                        VALUES ('admin', :password_hash, 'admin', '["all"]'::jsonb, NOW())
                    """),
                    {"password_hash": admin_password_hash}
                )
                print("  ✓ Created admin user (username: admin, password: admin123)")
            
            # 2. Create Station
            print("  Creating station...")
            station_result = await session.execute(
                text("""
                    INSERT INTO stations (call_letters, frequency, market, format, active, created_at, updated_at)
                    VALUES ('GAYPHX', NULL, 'Phoenix', 'Online/Streaming', true, NOW(), NOW())
                    ON CONFLICT (call_letters) DO NOTHING
                    RETURNING id
                """)
            )
            station_row = station_result.fetchone()
            if station_row:
                station_id = station_row[0]
                print(f"  ✓ Created station GayPHX.com (ID: {station_id})")
            else:
                # Get existing station
                station_result = await session.execute(
                    text("SELECT id FROM stations WHERE call_letters = 'GAYPHX'")
                )
                station_row = station_result.fetchone()
                station_id = station_row[0] if station_row else None
                print(f"  ✓ Using existing station GayPHX.com (ID: {station_id})")
            
            # 3. Create Sales Structure
            print("  Creating sales structure...")
            
            # Sales Region
            region_result = await session.execute(
                text("""
                    INSERT INTO sales_regions (name, description, created_at, updated_at)
                    VALUES ('Southwest Region', 'Arizona, New Mexico, Nevada', NOW(), NOW())
                    ON CONFLICT (name) DO NOTHING
                    RETURNING id
                """)
            )
            region_row = region_result.fetchone()
            if region_row:
                region_id = region_row[0]
            else:
                region_result = await session.execute(
                    text("SELECT id FROM sales_regions WHERE name = 'Southwest Region'")
                )
                region_row = region_result.fetchone()
                region_id = region_row[0] if region_row else None
            
            # Sales Office
            office_result = await session.execute(
                text("""
                    INSERT INTO sales_offices (name, region_id, address, phone, created_at, updated_at)
                    VALUES ('Phoenix Office', :region_id, '123 Main St, Phoenix, AZ 85001', '602-555-0100', NOW(), NOW())
                    ON CONFLICT (name) DO NOTHING
                    RETURNING id
                """),
                {"region_id": region_id}
            )
            office_row = office_result.fetchone()
            if office_row:
                office_id = office_row[0]
            else:
                office_result = await session.execute(
                    text("SELECT id FROM sales_offices WHERE name = 'Phoenix Office'")
                )
                office_row = office_result.fetchone()
                office_id = office_row[0] if office_row else None
            
            # Sales Team
            team_result = await session.execute(
                text("""
                    INSERT INTO sales_teams (name, description, created_at, updated_at)
                    VALUES ('Local Sales Team', 'Handles local advertising accounts', NOW(), NOW())
                    ON CONFLICT (name) DO NOTHING
                    RETURNING id
                """)
            )
            team_row = team_result.fetchone()
            if team_row:
                team_id = team_row[0]
            else:
                team_result = await session.execute(
                    text("SELECT id FROM sales_teams WHERE name = 'Local Sales Team'")
                )
                team_row = team_result.fetchone()
                team_id = team_row[0] if team_row else None
            
            # Sales Rep User
            sales_user_result = await session.execute(
                text("""
                    INSERT INTO users (username, password_hash, role, created_at)
                    VALUES ('salesrep', :password_hash, 'sales', NOW())
                    ON CONFLICT (username) DO NOTHING
                    RETURNING id
                """),
                {"password_hash": get_password_hash("demo123")}
            )
            sales_user_row = sales_user_result.fetchone()
            if sales_user_row:
                sales_user_id = sales_user_row[0]
            else:
                sales_user_result = await session.execute(
                    text("SELECT id FROM users WHERE username = 'salesrep'")
                )
                sales_user_row = sales_user_result.fetchone()
                sales_user_id = sales_user_row[0] if sales_user_row else None
            
            # Sales Rep
            sales_rep_result = await session.execute(
                text("""
                    INSERT INTO sales_reps (user_id, employee_id, commission_rate, active, created_at, updated_at)
                    VALUES (:user_id, 'EMP-001', 0.15, true, NOW(), NOW())
                    ON CONFLICT (user_id) DO NOTHING
                    RETURNING id
                """),
                {"user_id": sales_user_id}
            )
            sales_rep_row = sales_rep_result.fetchone()
            if sales_rep_row:
                sales_rep_id = sales_rep_row[0]
            else:
                sales_rep_result = await session.execute(
                    text("SELECT id FROM sales_reps WHERE user_id = :user_id"),
                    {"user_id": sales_user_id}
                )
                sales_rep_row = sales_rep_result.fetchone()
                sales_rep_id = sales_rep_row[0] if sales_rep_row else None
            
            # Link sales rep to team/office/region
            await session.execute(
                text("""
                    INSERT INTO sales_rep_teams (sales_rep_id, sales_team_id)
                    VALUES (:sales_rep_id, :team_id)
                    ON CONFLICT DO NOTHING
                """),
                {"sales_rep_id": sales_rep_id, "team_id": team_id}
            )
            await session.execute(
                text("""
                    INSERT INTO sales_rep_offices (sales_rep_id, sales_office_id)
                    VALUES (:sales_rep_id, :office_id)
                    ON CONFLICT DO NOTHING
                """),
                {"sales_rep_id": sales_rep_id, "office_id": office_id}
            )
            await session.execute(
                text("""
                    INSERT INTO sales_rep_regions (sales_rep_id, sales_region_id)
                    VALUES (:sales_rep_id, :region_id)
                    ON CONFLICT DO NOTHING
                """),
                {"sales_rep_id": sales_rep_id, "region_id": region_id}
            )
            print("  ✓ Created sales structure")
            
            # 4. Create Advertiser (1LPro LLC - Real Client)
            print("  Creating advertiser (1LPro LLC)...")
            # Check if exists first
            check_result = await session.execute(
                text("SELECT id FROM advertisers WHERE name = '1LPro LLC'")
            )
            existing = check_result.fetchone()
            
            if existing:
                advertiser_id = existing[0]
                print(f"  ✓ Using existing advertiser: 1LPro LLC")
            else:
                advertiser_result = await session.execute(
                    text("""
                        INSERT INTO advertisers (name, contact_first_name, contact_last_name, email, phone, address, active, created_at, updated_at)
                        VALUES ('1LPro LLC', 'John', 'Smith', 'john@1lpro.com', '602-555-0300', '123 Business Blvd, Phoenix, AZ 85001', true, NOW(), NOW())
                        RETURNING id
                    """)
                )
                advertiser_row = advertiser_result.fetchone()
                advertiser_id = advertiser_row[0] if advertiser_row else None
                print(f"  ✓ Created advertiser: 1LPro LLC")
            
            # 5. Create Agency (Demo Agency)
            print("  Creating agency (Demo Agency)...")
            check_result = await session.execute(
                text("SELECT id FROM agencies WHERE name = 'Demo Agency'")
            )
            existing = check_result.fetchone()
            
            if existing:
                agency_id = existing[0]
                print(f"  ✓ Using existing agency: Demo Agency")
            else:
                agency_result = await session.execute(
                    text("""
                        INSERT INTO agencies (name, contact_first_name, contact_last_name, email, phone, address, commission_rate, active, created_at, updated_at)
                        VALUES ('Demo Agency', 'Demo', 'Agent', 'demo@demoagency.com', '602-555-0400', '789 Agency Blvd, Phoenix, AZ 85003', 0.15, true, NOW(), NOW())
                        RETURNING id
                    """)
                )
                agency_row = agency_result.fetchone()
                agency_id = agency_row[0] if agency_row else None
                print(f"  ✓ Created agency: Demo Agency")
            
            # 6. Create Campaign
            print("  Creating campaign...")
            check_result = await session.execute(
                text("SELECT id FROM campaigns WHERE advertiser = '1LPro LLC' AND order_number = 'CAMP-2025-001'")
            )
            existing = check_result.fetchone()
            
            if existing:
                campaign_id = existing[0]
                print(f"  ✓ Using existing campaign: 1LPro LLC Q1 Campaign")
            else:
                campaign_result = await session.execute(
                    text("""
                        INSERT INTO campaigns (advertiser, order_number, start_date, end_date, priority, active, created_at, updated_at)
                        VALUES ('1LPro LLC', 'CAMP-2025-001', :start_date, :end_date, 1, true, NOW(), NOW())
                        RETURNING id
                    """),
                    {
                        "start_date": date.today() + timedelta(days=7),
                        "end_date": date.today() + timedelta(days=37)
                    }
                )
                campaign_row = campaign_result.fetchone()
                campaign_id = campaign_row[0] if campaign_row else None
                print(f"  ✓ Created campaign: 1LPro LLC Q1 Campaign")
            
            # 7. Create Order (Demo Order)
            print("  Creating comprehensive order (Demo Order)...")
            order_number = f"ORD-{datetime.now().strftime('%Y%m%d')}-001"
            
            # Get admin user ID
            admin_result = await session.execute(text("SELECT id FROM users WHERE username = 'admin'"))
            admin_id = admin_result.scalar()
            
            stations_json = f'[{{"id": "{station_id}", "call_letters": "GAYPHX"}}]'
            spot_lengths_json = '[30, 60]'
            rates_json = '{"30": 100.00, "60": 150.00}'
            
            order_result = await session.execute(
                text("""
                    INSERT INTO orders (
                        order_number, order_name, campaign_id, advertiser_id, agency_id, sales_rep_id,
                        sales_team_id, sales_office_id, sales_region_id, stations,
                        order_type, start_date, end_date, spot_lengths, total_spots,
                        rate_type, rates, gross_amount, net_amount, total_value,
                        agency_commission_percent, agency_commission_amount,
                        billing_cycle, invoice_type, status, approval_status,
                        traffic_ready, billing_ready, created_at, updated_at, created_by
                    )
                    VALUES (
                        :order_number, 'Demo Order - Q1 Campaign', :campaign_id, :advertiser_id, :agency_id, :sales_rep_id,
                        :team_id, :office_id, :region_id, CAST(:stations_json AS jsonb),
                        'LOCAL', :start_date, :end_date, CAST(:spot_lengths_json AS jsonb), 120,
                        'ROS', CAST(:rates_json AS jsonb), 12000.00, 10200.00, 12000.00,
                        15.00, 1800.00,
                        'MONTHLY', 'STANDARD', 'APPROVED', 'APPROVED',
                        true, true, NOW(), NOW(), :admin_id
                    )
                    ON CONFLICT (order_number) DO NOTHING
                    RETURNING id
                """),
                {
                    "order_number": order_number,
                    "campaign_id": campaign_id,
                    "advertiser_id": advertiser_id,
                    "agency_id": agency_id,
                    "sales_rep_id": sales_rep_id,
                    "team_id": team_id,
                    "office_id": office_id,
                    "region_id": region_id,
                    "stations_json": stations_json,
                    "start_date": date.today() + timedelta(days=7),
                    "end_date": date.today() + timedelta(days=37),
                    "spot_lengths_json": spot_lengths_json,
                    "rates_json": rates_json,
                    "admin_id": admin_id
                }
            )
            order_row = order_result.fetchone()
            if order_row:
                order_id = order_row[0]
            else:
                order_result = await session.execute(
                    text("SELECT id FROM orders WHERE order_number = :order_number"),
                    {"order_number": order_number}
                )
                order_row = order_result.fetchone()
                order_id = order_row[0] if order_row else None
            print(f"  ✓ Created order: {order_number} (Demo Order)")
            
            # 8. Create Order Lines
            print("  Creating order lines...")
            await session.execute(
                text("""
                    INSERT INTO order_lines (
                        order_id, line_number, station_id, length, daypart, 
                        spot_frequency, rate, start_date, end_date,
                        makegood_eligible, guaranteed_position, preemptible,
                        created_at, updated_at
                    )
                    VALUES
                    (:order_id, 1, :station_id, 30, 'MORNING_DRIVE', 10, 100.00, :start_date, :end_date, true, false, true, NOW(), NOW()),
                    (:order_id, 2, :station_id, 60, 'AFTERNOON_DRIVE', 15, 150.00, :start_date, :end_date, true, false, true, NOW(), NOW())
                    ON CONFLICT DO NOTHING
                """),
                {
                    "order_id": order_id,
                    "station_id": station_id,
                    "start_date": date.today() + timedelta(days=7),
                    "end_date": date.today() + timedelta(days=37)
                }
            )
            print("  ✓ Created order lines")
            
            # 9. Create Copy/Scripts (Demo Copy)
            print("  Creating copy/scripts (Demo Copy)...")
            copy_result = await session.execute(
                text("""
                    INSERT INTO copy (
                        order_id, advertiser_id, title, copy_code, isci_code,
                        copy_type, script_text, copy_status, copy_approval_status,
                        active, created_at, updated_at
                    )
                    VALUES (
                        :order_id, :advertiser_id, 'Demo Copy - Q1 Campaign', 'COPY-001', 'ISCI-DEMO-001',
                        'NEW', 'This is demo copy for teaching purposes. Visit 1LPro LLC for amazing products and services. Contact us today!', 
                        'APPROVED', 'APPROVED',
                        true, NOW(), NOW()
                    )
                    ON CONFLICT DO NOTHING
                    RETURNING id
                """),
                {"order_id": order_id, "advertiser_id": advertiser_id}
            )
            copy_row = copy_result.fetchone()
            if copy_row:
                copy_id = copy_row[0]
            else:
                copy_result = await session.execute(
                    text("SELECT id FROM copy WHERE order_id = :order_id AND title = 'Demo Copy - Q1 Campaign'"),
                    {"order_id": order_id}
                )
                copy_row = copy_result.fetchone()
                copy_id = copy_row[0] if copy_row else None
            print("  ✓ Created copy/script (Demo Copy)")
            
            # 10. Create Production Order
            print("  Creating production order...")
            po_number = f"PO-{datetime.now().strftime('%Y%m%d')}-001"
            prod_order_result = await session.execute(
                text("""
                    INSERT INTO production_orders (
                        po_number, copy_id, order_id, campaign_id, advertiser_id,
                        client_name, campaign_title, start_date, end_date, budget,
                        order_type, status, created_at, updated_at
                    )
                    VALUES (
                        :po_number, :copy_id, :order_id, :campaign_id, :advertiser_id,
                        '1LPro LLC', '1LPro LLC Q1 Campaign', :start_date, :end_date, 5000.00,
                        'STANDARD', 'APPROVED', NOW(), NOW()
                    )
                    ON CONFLICT (po_number) DO NOTHING
                    RETURNING id
                """),
                {
                    "po_number": po_number,
                    "copy_id": copy_id,
                    "order_id": order_id,
                    "campaign_id": campaign_id,
                    "advertiser_id": advertiser_id,
                    "start_date": date.today() + timedelta(days=7),
                    "end_date": date.today() + timedelta(days=37)
                }
            )
            prod_order_row = prod_order_result.fetchone()
            if prod_order_row:
                prod_order_id = prod_order_row[0]
            else:
                prod_order_result = await session.execute(
                    text("SELECT id FROM production_orders WHERE po_number = :po_number"),
                    {"po_number": po_number}
                )
                prod_order_row = prod_order_result.fetchone()
                prod_order_id = prod_order_row[0] if prod_order_row else None
            print("  ✓ Created production order")
            
            # 11. Create Spots
            print("  Creating sample spots...")
            await session.execute(
                text("""
                    INSERT INTO spots (
                        order_id, campaign_id, station_id, scheduled_date, scheduled_time, 
                        spot_length, daypart, status, created_at, updated_at
                    )
                    VALUES
                    (:order_id, :campaign_id, :station_id, :start_date, '07:30:00', 30, 'MORNING_DRIVE', 'SCHEDULED', NOW(), NOW()),
                    (:order_id, :campaign_id, :station_id, :start_date, '17:00:00', 60, 'AFTERNOON_DRIVE', 'SCHEDULED', NOW(), NOW())
                    ON CONFLICT DO NOTHING
                """),
                {
                    "order_id": order_id,
                    "campaign_id": campaign_id,
                    "station_id": station_id,
                    "start_date": date.today() + timedelta(days=7)
                }
            )
            print("  ✓ Created sample spots")
            
            # 12. Create Invoice
            print("  Creating invoice...")
            invoice_result = await session.execute(
                text("""
                    INSERT INTO invoices (
                        invoice_number, order_id, campaign_id, advertiser_id, agency_id,
                        invoice_date, due_date, subtotal, tax, total,
                        status, created_at, updated_at
                    )
                    VALUES (
                        :invoice_number, :order_id, :campaign_id, :advertiser_id, :agency_id,
                        :invoice_date, :due_date, 10200.00, 0.00, 10200.00,
                        'DRAFT', NOW(), NOW()
                    )
                    ON CONFLICT (invoice_number) DO NOTHING
                    RETURNING id
                """),
                {
                    "invoice_number": f"INV-{datetime.now().strftime('%Y%m%d')}-001",
                    "order_id": order_id,
                    "campaign_id": campaign_id,
                    "advertiser_id": advertiser_id,
                    "agency_id": agency_id,
                    "invoice_date": date.today(),
                    "due_date": date.today() + timedelta(days=30)
                }
            )
            invoice_row = invoice_result.fetchone()
            if invoice_row:
                invoice_id = invoice_row[0]
                # Create invoice lines
                await session.execute(
                    text("""
                        INSERT INTO invoice_lines (
                            invoice_id, station_id, description, quantity, unit_price, total
                        )
                        SELECT :invoice_id, station_id,
                               'Spot ' || length || 's - ' || daypart, 
                               spot_frequency * 4, rate, rate * spot_frequency * 4
                        FROM order_lines
                        WHERE order_id = :order_id
                        ON CONFLICT DO NOTHING
                    """),
                    {"invoice_id": invoice_id, "order_id": order_id}
                )
            print("  ✓ Created invoice with lines")
            
            # 13. Create Daypart Categories
            print("  Creating daypart categories...")
            await session.execute(
                text("""
                    INSERT INTO daypart_categories (name, description, created_at, updated_at)
                    VALUES 
                    ('Drive Time', 'Morning and afternoon drive periods', NOW(), NOW()),
                    ('Daytime', 'Midday programming', NOW(), NOW()),
                    ('Evening', 'Evening and overnight', NOW(), NOW())
                    ON CONFLICT (name) DO NOTHING
                """)
            )
            print("  ✓ Created daypart categories")
            
            await session.commit()
            
            print("\n✅ Demo data seeded successfully!")
            print("\n📋 Login Credentials:")
            print("   Admin: admin / admin123")
            print("   Sales Rep: salesrep / demo123")
            print(f"\n📻 Station: GayPHX.com (Online/Streaming)")
            print(f"\n👤 Real Client:")
            print(f"   Advertiser: 1LPro LLC")
            print(f"\n📦 Demo Entities Created:")
            print(f"   Order: {order_number} (Demo Order - Q1 Campaign)")
            print(f"   Campaign: 1LPro LLC Q1 Campaign")
            print(f"   Agency: Demo Agency")
            print(f"   Copy: Demo Copy - Q1 Campaign")
            print(f"\n🎯 Complete Order Lifecycle Data:")
            print("   ✓ Multiple order lines with different spot lengths")
            print("   ✓ Copy/scripts with approval workflow")
            print("   ✓ Production orders")
            print("   ✓ Scheduled spots")
            print("   ✓ Invoice with line items")
            print("   ✓ Sales team structure")
            print("   ✓ Campaign management")
            print("\n💡 Use this data to walk through:")
            print("   1. Order creation and configuration")
            print("   2. Copy/script management and approval")
            print("   3. Production order workflow")
            print("   4. Spot scheduling")
            print("   5. Invoice generation and billing")
            
    except Exception as e:
        logger.error("Failed to seed demo data", error=str(e), exc_info=True)
        print(f"✗ Error seeding demo data: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        await engine.dispose()


def main():
    """Main entry point for the script."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Seed database with admin user and comprehensive demo data"
    )
    parser.add_argument(
        "--database-url",
        type=str,
        default=os.getenv("POSTGRES_URI", "postgresql://librelog:password@db:5432/librelog"),
        help="Database connection URL (default: from POSTGRES_URI env var)"
    )
    
    args = parser.parse_args()
    
    asyncio.run(seed_demo_data(args.database_url))


if __name__ == "__main__":
    main()

