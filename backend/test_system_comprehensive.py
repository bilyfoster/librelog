#!/usr/bin/env python3
"""
Comprehensive system test script
Tests all major features of LibreLog system
"""
import asyncio
import sys
from datetime import date, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.models.clock_template import ClockTemplate
from backend.models.track import Track
from backend.models.spot import Spot
from backend.models.campaign import Campaign
from backend.models.invoice import Invoice
from backend.models.copy import Copy
from backend.services.log_generator import LogGenerator
from sqlalchemy import select, func
import json

async def test_tracks():
    """Test track retrieval"""
    print("\n=== Testing Tracks ===")
    async with AsyncSessionLocal() as db:
        # Test all track types
        track_types = ["MUS", "PRO", "LIN", "IDS", "NEW", "PSA", "INT"]
        for track_type in track_types:
            result = await db.execute(
                select(func.count(Track.id)).where(Track.type == track_type)
            )
            count = result.scalar() or 0
            print(f"  {track_type}: {count} tracks")
            if count > 0:
                result = await db.execute(
                    select(Track).where(Track.type == track_type).limit(1)
                )
                track = result.scalar_one_or_none()
                if track:
                    print(f"    Example: {track.title} by {track.artist}")

async def test_clock_templates():
    """Test clock templates"""
    print("\n=== Testing Clock Templates ===")
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(ClockTemplate))
        templates = result.scalars().all()
        print(f"  Found {len(templates)} clock templates")
        for template in templates:
            print(f"  - {template.name}: {len(template.json_layout.get('elements', []))} elements")
            # Check for NEWS and IDS in elements
            elements = template.json_layout.get('elements', [])
            has_news = any(e.get('type') == 'NEW' for e in elements)
            has_ids = any(e.get('type') == 'IDS' for e in elements)
            has_hard_start = any(e.get('hard_start') for e in elements)
            print(f"    NEWS: {has_news}, IDS: {has_ids}, Hard Start: {has_hard_start}")

async def test_log_generation():
    """Test log generation with NEWS and IDS"""
    print("\n=== Testing Log Generation ===")
    async with AsyncSessionLocal() as db:
        # Get a clock template
        result = await db.execute(select(ClockTemplate).limit(1))
        template = result.scalar_one_or_none()
        
        if not template:
            print("  ❌ No clock templates found")
            return
        
        print(f"  Using template: {template.name}")
        
        # Test log generator
        generator = LogGenerator(db)
        target_date = date.today() + timedelta(days=1)
        
        try:
            # Get a user ID (use 1 as default)
            preview = await generator.preview_log(
                target_date=target_date,
                clock_template_id=template.id,
                preview_hours=["06:00"]
            )
            
            print(f"  ✅ Preview generated for {target_date}")
            print(f"  Preview hours: {preview.get('preview_hours', [])}")
            
            # Check for NEWS and IDS in preview
            for hour, hour_data in preview.get('hourly_previews', {}).items():
                elements = hour_data.get('elements', [])
                element_types = [e.get('type') for e in elements]
                print(f"  Hour {hour}: {len(elements)} elements")
                print(f"    Types: {', '.join(set(element_types))}")
                
                # Check for hard_start
                hard_start_elements = [e for e in elements if e.get('hard_start')]
                if hard_start_elements:
                    print(f"    Hard start elements: {len(hard_start_elements)}")
                    for e in hard_start_elements:
                        print(f"      - {e.get('type')} at {e.get('start_time')}s (scheduled: {e.get('scheduled_time')}s)")
                
                # Check for timing drift
                drift_elements = [e for e in elements if e.get('timing_drift', 0) != 0]
                if drift_elements:
                    print(f"    Elements with timing drift: {len(drift_elements)}")
            
        except Exception as e:
            print(f"  ❌ Error generating preview: {e}")
            import traceback
            traceback.print_exc()

async def test_spots():
    """Test spots"""
    print("\n=== Testing Spots ===")
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(func.count(Spot.id)))
        count = result.scalar() or 0
        print(f"  Total spots: {count}")
        
        if count > 0:
            result = await db.execute(select(Spot).limit(5))
            spots = result.scalars().all()
            print(f"  Sample spots:")
            for spot in spots:
                print(f"    - Spot {spot.id}: {spot.scheduled_date} {spot.scheduled_time} ({spot.daypart})")

async def test_campaigns():
    """Test campaigns"""
    print("\n=== Testing Campaigns ===")
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(Campaign))
        campaigns = result.scalars().all()
        print(f"  Total campaigns: {len(campaigns)}")
        for campaign in campaigns:
            print(f"  - {campaign.advertiser}: Priority {campaign.priority}, Active: {campaign.active}")

async def test_invoices():
    """Test invoices"""
    print("\n=== Testing Invoices ===")
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(func.count(Invoice.id)))
        count = result.scalar() or 0
        print(f"  Total invoices: {count}")

async def test_copy():
    """Test copy items"""
    print("\n=== Testing Copy ===")
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(func.count(Copy.id)))
        count = result.scalar() or 0
        print(f"  Total copy items: {count}")

async def main():
    """Run all tests"""
    print("=" * 60)
    print("LibreLog Comprehensive System Test")
    print("=" * 60)
    
    await test_tracks()
    await test_clock_templates()
    await test_log_generation()
    await test_spots()
    await test_campaigns()
    await test_invoices()
    await test_copy()
    
    print("\n" + "=" * 60)
    print("Test Complete")
    print("=" * 60)

if __name__ == "__main__":
    asyncio.run(main())

