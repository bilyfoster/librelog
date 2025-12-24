#!/usr/bin/env python3
"""
Create a log and push it to LibreTime
"""
import requests
import json
from datetime import date, timedelta

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

# Get clock templates
print("\n📋 Getting clock templates...")
clocks_response = requests.get(f"{BASE_URL}/clocks", headers=auth_headers, timeout=30)
print(f"   Response status: {clocks_response.status_code}")
print(f"   Response headers: {dict(clocks_response.headers)}")

if clocks_response.status_code != 200:
    print(f"⚠️  Failed to get clocks: {clocks_response.status_code}, creating one...")
    # Create a simple clock template
    create_clock_response = requests.post(
        f"{BASE_URL}/clocks/",
        headers=auth_headers,
        json={
            "name": "Test Clock Template",
            "station_id": 1,
            "json_layout": {
                "hour": "00",
                "elements": [
                    {"type": "track", "position": 0, "duration": 180},
                    {"type": "spot", "position": 180, "duration": 30},
                    {"type": "track", "position": 210, "duration": 180}
                ]
            }
        },
        timeout=30
    )
    print(f"   Create clock response: {create_clock_response.status_code}")
    if create_clock_response.status_code in [200, 201]:
        try:
            clock_data = create_clock_response.json()
            clock_template_id = clock_data.get("id") or clock_data.get("clock_id")
            station_id = clock_data.get("station_id", 1)
            print(f"✅ Created clock template ID: {clock_template_id}, Station ID: {station_id}")
        except:
            print(f"⚠️  Could not parse clock response, using defaults")
            clock_template_id = 1
            station_id = 1
    else:
        print(f"⚠️  Failed to create clock template, using defaults")
        print(f"   Response: {create_clock_response.text[:200]}")
        clock_template_id = 1
        station_id = 1
else:
    try:
        clocks_data = clocks_response.json()
        templates = clocks_data.get("templates", [])
        if templates:
            clock_template_id = templates[0]["id"]
            station_id = templates[0].get("station_id", 1)
            print(f"✅ Found clock template ID: {clock_template_id}, Station ID: {station_id}")
        else:
            print("⚠️  No templates in response, using defaults")
            clock_template_id = 1
            station_id = 1
    except:
        print("⚠️  Could not parse clocks response, using defaults")
        clock_template_id = 1
        station_id = 1

# Get stations to verify and get actual station_id
print("\n📻 Getting stations...")
stations_response = requests.get(f"{BASE_URL}/stations", headers=auth_headers, timeout=30)
if stations_response.status_code == 200:
    try:
        stations_data = stations_response.json()
        if isinstance(stations_data, list) and stations_data:
            actual_station_id = stations_data[0]["id"]
            print(f"✅ Found station ID: {actual_station_id}")
            # Use the station from the list if we don't have one from clock template
            if not station_id or station_id == 1:
                station_id = actual_station_id
        elif isinstance(stations_data, dict) and stations_data.get("stations"):
            actual_station_id = stations_data["stations"][0]["id"]
            print(f"✅ Found station ID: {actual_station_id}")
            if not station_id or station_id == 1:
                station_id = actual_station_id
    except:
        pass

# If we still don't have a valid station_id, try to get it from existing logs
if not station_id or station_id == 1:
    print("⚠️  Trying to get station_id from existing logs...")
    logs_response = requests.get(f"{BASE_URL}/logs?limit=1", headers=auth_headers, timeout=30)
    if logs_response.status_code == 200:
        try:
            logs_data = logs_response.json()
            if isinstance(logs_data, dict) and logs_data.get("logs"):
                if logs_data["logs"]:
                    station_id = logs_data["logs"][0].get("station_id", 3)
                    print(f"✅ Using station ID from existing log: {station_id}")
        except:
            pass

# Default to station_id 3 if still not set (from error message)
if not station_id or station_id == 1:
    station_id = 3
    print(f"⚠️  Using default station ID: {station_id}")

print(f"✅ Final station ID: {station_id}")

# Use tomorrow's date for the log
target_date = (date.today() + timedelta(days=1)).isoformat()
print(f"\n📅 Target date: {target_date}")

# Generate log
print("\n📝 Generating log...")
print(f"   Using clock_template_id: {clock_template_id}, station_id: {station_id}")
generate_response = requests.post(
    f"{BASE_URL}/logs/generate",
    headers=auth_headers,
    json={
        "target_date": target_date,
        "clock_template_id": clock_template_id,
        "station_id": station_id
    },
    timeout=60
)

if generate_response.status_code not in [200, 201]:
    print(f"❌ Failed to generate log: {generate_response.status_code}")
    print(generate_response.text)
    exit(1)

log_data = generate_response.json()
log_id = log_data.get("log_id") or log_data.get("id")
if not log_id:
    print("❌ No log ID in response")
    print(json.dumps(log_data, indent=2))
    exit(1)

print(f"✅ Log created successfully! Log ID: {log_id}")
print(f"   Date: {target_date}")
print(f"   Clock Template: {clock_template_id}")
print(f"   Station: {station_id}")

# Get log details
print("\n📄 Getting log details...")
log_response = requests.get(f"{BASE_URL}/logs/{log_id}", headers=auth_headers, timeout=30)
if log_response.status_code == 200:
    log_details = log_response.json()
    json_data = log_details.get("json_data", {})
    hours = json_data.get("hours", {})
    print(f"✅ Log has {len(hours)} hours of content")
    
    # Show first hour summary
    if hours:
        first_hour = list(hours.keys())[0]
        first_hour_data = hours[first_hour]
        elements = first_hour_data.get("elements", [])
        print(f"   First hour ({first_hour}): {len(elements)} elements")

# Publish to LibreTime
print("\n🚀 Publishing log to LibreTime...")
publish_response = requests.post(
    f"{BASE_URL}/logs/{log_id}/publish",
    headers=auth_headers,
    timeout=60
)

if publish_response.status_code == 200:
    publish_data = publish_response.json()
    print("✅ Log published to LibreTime successfully!")
    print(f"   Message: {publish_data.get('message', 'Published')}")
    if publish_data.get("warnings"):
        print(f"   Warnings: {publish_data['warnings']}")
else:
    print(f"❌ Failed to publish log: {publish_response.status_code}")
    print(publish_response.text)
    exit(1)

# Check sync status
print("\n🔍 Checking LibreTime sync status...")
sync_response = requests.get(f"{BASE_URL}/sync/status", headers=auth_headers, timeout=30)
if sync_response.status_code == 200:
    sync_data = sync_response.json()
    print(f"✅ LibreTime connection status: {sync_data.get('connection_status', 'Unknown')}")
    print(f"   Total tracks: {sync_data.get('total_tracks', 0)}")
    if sync_data.get('last_track_sync'):
        print(f"   Last track sync: {sync_data['last_track_sync']}")

print("\n" + "="*60)
print("✅ SUCCESS! Log created and pushed to LibreTime")
print("="*60)
print(f"Log ID: {log_id}")
print(f"Date: {target_date}")
print(f"Check LibreTime schedule for date: {target_date}")
print("="*60)

