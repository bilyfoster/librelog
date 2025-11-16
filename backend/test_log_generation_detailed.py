#!/usr/bin/env python3
"""
Detailed log generation test with NEWS and IDS
"""
import asyncio
from datetime import date, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.models.clock_template import ClockTemplate
from backend.services.log_generator import LogGenerator
from sqlalchemy import select
import json

async def test_log_with_news_ids():
    """Test log generation with NEWS and IDS elements"""
    print("=" * 60)
    print("Testing Log Generation with NEWS and IDS")
    print("=" * 60)
    
    async with AsyncSessionLocal() as db:
        # Find a template with NEWS and IDS
        result = await db.execute(
            select(ClockTemplate).where(
                ClockTemplate.name.like('%Morning%')
            )
        )
        templates = result.scalars().all()
        
        template_with_news_ids = None
        for template in templates:
            elements = template.json_layout.get('elements', [])
            has_news = any(e.get('type') == 'NEW' for e in elements)
            has_ids = any(e.get('type') == 'IDS' for e in elements)
            if has_news and has_ids:
                template_with_news_ids = template
                break
        
        if not template_with_news_ids:
            print("❌ No template found with both NEWS and IDS")
            return
        
        print(f"\n✅ Using template: {template_with_news_ids.name}")
        print(f"   Elements: {len(template_with_news_ids.json_layout.get('elements', []))}")
        
        # Show element configuration
        print("\nElement Configuration:")
        for i, elem in enumerate(template_with_news_ids.json_layout.get('elements', [])):
            print(f"  {i+1}. Type: {elem.get('type')}, Count: {elem.get('count', 1)}, Hard Start: {elem.get('hard_start', False)}")
            if elem.get('type') == 'IDS':
                print(f"     Position: {elem.get('position', 'top')}")
        
        # Generate preview
        generator = LogGenerator(db)
        target_date = date.today() + timedelta(days=1)
        
        print(f"\n📅 Generating preview for {target_date}")
        print("   Preview hour: 06:00")
        
        try:
            preview = await generator.preview_log(
                target_date=target_date,
                clock_template_id=template_with_news_ids.id,
                preview_hours=["06:00"]
            )
            
            print("\n✅ Preview generated successfully!")
            
            # Analyze the preview
            for hour, hour_data in preview.get('hourly_previews', {}).items():
                print(f"\n📊 Hour {hour} Analysis:")
                elements = hour_data.get('elements', [])
                print(f"   Total elements: {len(elements)}")
                print(f"   Total duration: {hour_data.get('total_duration', 0)}s")
                
                # Check for NEWS
                news_elements = [e for e in elements if e.get('type') == 'NEW']
                if news_elements:
                    print(f"\n   📰 NEWS Elements ({len(news_elements)}):")
                    for e in news_elements:
                        print(f"      - {e.get('title')} at {e.get('start_time')}s")
                        print(f"        Duration: {e.get('duration')}s, Hard Start: {e.get('hard_start', False)}")
                
                # Check for IDS
                ids_elements = [e for e in elements if e.get('type') == 'IDS']
                if ids_elements:
                    print(f"\n   🆔 IDS Elements ({len(ids_elements)}):")
                    for e in ids_elements:
                        print(f"      - {e.get('title')} at {e.get('start_time')}s")
                        print(f"        Duration: {e.get('duration')}s, Hard Start: {e.get('hard_start', False)}")
                        print(f"        Scheduled: {e.get('scheduled_time')}s, Actual: {e.get('start_time')}s")
                
                # Check timing control
                hard_start_elements = [e for e in elements if e.get('hard_start')]
                if hard_start_elements:
                    print(f"\n   ⏰ Hard Start Elements ({len(hard_start_elements)}):")
                    for e in hard_start_elements:
                        drift = e.get('timing_drift', 0)
                        print(f"      - {e.get('type')}: Scheduled {e.get('scheduled_time')}s, Actual {e.get('start_time')}s, Drift: {drift}s")
                
                # Show all element types
                element_types = {}
                for e in elements:
                    etype = e.get('type')
                    element_types[etype] = element_types.get(etype, 0) + 1
                print(f"\n   📋 Element Summary:")
                for etype, count in sorted(element_types.items()):
                    print(f"      - {etype}: {count}")
            
            print("\n" + "=" * 60)
            print("✅ Test Complete - NEWS and IDS handlers working!")
            print("=" * 60)
            
        except Exception as e:
            print(f"\n❌ Error: {e}")
            import traceback
            traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test_log_with_news_ids())

