"""
Script to save LibreTime configuration to LibreLog settings
Run this after setting LIBRETIME_API_URL and LIBRETIME_API_KEY environment variables
"""

import asyncio
import os
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from backend.database import AsyncSessionLocal
from backend.services.libretime_config_service import LibreTimeConfigService


async def main():
    """Save LibreTime configuration"""
    async with AsyncSessionLocal() as db:
        try:
            result = await LibreTimeConfigService.save_libretime_config(db)
            print("✅ LibreTime configuration saved successfully!")
            print(f"   Saved settings: {', '.join(result['saved_settings'])}")
            print(f"   API URL: {result.get('libretime_api_url', 'Not set')}")
            print(f"   Public URL: {result.get('libretime_public_url', 'Not set')}")
            print(f"   API Key: {'Set' if result.get('libretime_api_key_set') else 'Not set'}")
        except Exception as e:
            print(f"❌ Error saving configuration: {e}")
            import traceback
            traceback.print_exc()
            sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())

