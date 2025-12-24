#!/usr/bin/env python3
"""
Create a clock template for testing
"""
import requests
import json

BASE_URL = "http://api:8000"
HEADERS = {"Host": "log.gayphx.com", "Content-Type": "application/json", "Accept": "application/json"}

# Authenticate
print("🔐 Authenticating...")
login_response = requests.post(
    f"{BASE_URL}/auth/login",
    headers=HEADERS,
    json={"username": "admin", "password": "admin123"},
    timeout=30
)

if login_response.status_code != 200:
    print(f"❌ Authentication failed: {login_response.status_code}")
    print(login_response.text)
    exit(1)

token = login_response.json()["access_token"]
auth_headers = {**HEADERS, "Authorization": f"Bearer {token}"}
print("✅ Authentication successful")

# Check existing clock templates
print("\n📋 Checking existing clock templates...")
clocks_response = requests.get(f"{BASE_URL}/clocks", headers=auth_headers, timeout=30)
if clocks_response.status_code == 200:
    try:
        clocks_data = clocks_response.json()
        templates = clocks_data.get("templates", [])
        if templates:
            print(f"✅ Found {len(templates)} existing clock template(s):")
            for t in templates:
                print(f"   - ID: {t.get('id')}, Name: {t.get('name')}")
            print("\n✅ Clock templates already exist. No need to create one.")
            exit(0)
    except:
        pass

# Get station ID first
print("\n📻 Getting stations...")
stations_response = requests.get(f"{BASE_URL}/stations", headers=auth_headers, timeout=30)
station_id = 1  # Default
if stations_response.status_code == 200:
    try:
        stations_data = stations_response.json()
        if isinstance(stations_data, list) and stations_data:
            station_id = stations_data[0]["id"]
        elif isinstance(stations_data, dict) and stations_data.get("stations"):
            station_id = stations_data["stations"][0]["id"]
        print(f"✅ Using station ID: {station_id}")
    except:
        print(f"⚠️  Could not parse stations, using default station_id: {station_id}")

# Create a clock template
print("\n📝 Creating clock template...")
# The layout needs "hour" and "elements" at top level
# This represents a single hour template that gets repeated
clock_data = {
    "name": "Default Clock Template",
    "description": "Default clock template for log generation",
    "json_layout": {
        "hour": "00",
        "elements": [
            {"type": "MUS", "position": 0, "duration": 180},
            {"type": "ADV", "position": 180, "duration": 30},
            {"type": "MUS", "position": 210, "duration": 180},
            {"type": "IDS", "position": 390, "duration": 30},
            {"type": "MUS", "position": 420, "duration": 180}
        ]
    },
    "station_id": station_id
}

create_response = requests.post(
    f"{BASE_URL}/clocks/",
    headers=auth_headers,
    json=clock_data,
    timeout=30
)

if create_response.status_code in [200, 201]:
    try:
        result = create_response.json()
        clock_id = result.get("id") or result.get("clock_id")
        print(f"✅ Clock template created successfully!")
        print(f"   ID: {clock_id}")
        print(f"   Name: {result.get('name', 'Default Clock Template')}")
        print(f"   Message: {result.get('message', 'Created')}")
    except:
        print(f"✅ Clock template created (status: {create_response.status_code})")
        print(f"   Response: {create_response.text[:200]}")
else:
    print(f"❌ Failed to create clock template: {create_response.status_code}")
    print(f"   Response: {create_response.text}")
    
    # Try to get more details
    try:
        error_detail = create_response.json().get("detail", "Unknown error")
        print(f"   Error detail: {error_detail}")
    except:
        pass
    exit(1)

# Verify it was created
print("\n🔍 Verifying clock template...")
verify_response = requests.get(f"{BASE_URL}/clocks", headers=auth_headers, timeout=30)
if verify_response.status_code == 200:
    try:
        verify_data = verify_response.json()
        templates = verify_data.get("templates", [])
        print(f"✅ Verified: {len(templates)} clock template(s) now available")
        for t in templates:
            print(f"   - ID: {t.get('id')}, Name: {t.get('name')}")
    except:
        print("✅ Clock template created (verification response format unexpected)")

print("\n" + "="*60)
print("✅ SUCCESS! Clock template created")
print("="*60)
print("You can now generate logs using this clock template.")
print("="*60)

