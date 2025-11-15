#!/usr/bin/env python3
"""
Test LibreTime connection from LibreLog
"""
import asyncio
import os
import sys

# Add backend to path
sys.path.insert(0, '/app')

from backend.integrations.libretime_client import libretime_client

async def test_connection():
    print("Testing LibreTime connection...")
    print(f"LibreTime URL: {os.getenv('LIBRETIME_URL', 'Not set')}")
    print(f"API Key: {'Set' if os.getenv('LIBRETIME_API_KEY') else 'Not set'}")
    print()
    
    # Test library-full endpoint
    print("1. Testing library-full endpoint...")
    try:
        tracks = await libretime_client.get_tracks(limit=3)
        print(f"   ✓ Success! Got {len(tracks)} tracks")
        if tracks:
            print(f"   Sample track: {tracks[0].get('title', 'N/A')} by {tracks[0].get('artist', 'N/A')}")
    except Exception as e:
        print(f"   ✗ Failed: {e}")
    
    print()
    
    # Test schedule status
    print("2. Testing schedule status endpoint...")
    try:
        from datetime import date
        status = await libretime_client.get_schedule_status(date.today())
        print(f"   ✓ Success! Schedule status retrieved")
        print(f"   Scheduled count: {status.get('scheduled_count', 0)}")
        print(f"   Success: {status.get('success', False)}")
    except Exception as e:
        print(f"   ✗ Failed: {e}")
    
    print()
    
    # Test playout history
    print("3. Testing playout-history-full endpoint...")
    try:
        from datetime import date, timedelta
        end_date = date.today()
        start_date = end_date - timedelta(days=7)
        history = await libretime_client.get_playback_history(start_date, end_date)
        print(f"   ✓ Success! Got {len(history)} history entries")
        if history:
            print(f"   Sample entry: Media ID {history[0].get('media_id', 'N/A')} played at {history[0].get('start_time', 'N/A')}")
    except Exception as e:
        print(f"   ✗ Failed: {e}")
    
    print()
    print("LibreTime connection test complete!")

if __name__ == "__main__":
    asyncio.run(test_connection())

