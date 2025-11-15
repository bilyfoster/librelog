#!/usr/bin/env python3
"""Test LibreTime connectivity"""
import asyncio
import os
import httpx

async def test():
    libretime_url = os.getenv("LIBRETIME_URL", "").rstrip("/api/v2").rstrip("/")
    libretime_api_key = os.getenv("LIBRETIME_API_KEY", "")
    
    print(f"LibreTime URL: {libretime_url}")
    print(f"API Key: {libretime_api_key[:10]}...")
    
    download_url = f"{libretime_url}/api/v2/files/7/download"
    print(f"\nTesting: {download_url}")
    
    async with httpx.AsyncClient() as client:
        headers = {}
        if libretime_api_key:
            headers["Authorization"] = f"Api-Key {libretime_api_key}"
        
        try:
            response = await client.get(download_url, headers=headers, timeout=10.0, follow_redirects=True)
            print(f"Status: {response.status_code}")
            print(f"Headers: {dict(response.headers)}")
            
            if response.status_code == 200:
                print(f"Content length: {len(response.content)} bytes")
            elif "X-Accel-Redirect" in response.headers:
                accel = response.headers["X-Accel-Redirect"]
                print(f"X-Accel-Redirect: {accel}")
                media_url = f"{libretime_url}{accel}"
                print(f"Trying media URL: {media_url}")
                media_response = await client.get(media_url, headers=headers, timeout=10.0, follow_redirects=True)
                print(f"Media Status: {media_response.status_code}")
                if media_response.status_code == 200:
                    print(f"Media Content length: {len(media_response.content)} bytes")
        except Exception as e:
            print(f"Error: {e}")
            import traceback
            traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test())



