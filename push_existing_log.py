#!/usr/bin/env python3
"""
Find an existing log and push it to LibreTime
"""
import requests
import json
from datetime import date

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

# Get existing logs
print("\n📋 Getting existing logs...")
logs_response = requests.get(f"{BASE_URL}/logs?limit=10", headers=auth_headers, timeout=30)
if logs_response.status_code != 200:
    print(f"❌ Failed to get logs: {logs_response.status_code}")
    print(logs_response.text)
    exit(1)

logs_data = logs_response.json()
logs = logs_data.get("logs", [])

if not logs:
    print("❌ No existing logs found. Cannot proceed without a log to publish.")
    exit(1)

# Find a log with content
log_id = None
for log in logs:
    log_id_candidate = log["id"]
    # Check if this log has content
    log_check = requests.get(f"{BASE_URL}/logs/{log_id_candidate}", headers=auth_headers, timeout=30)
    if log_check.status_code == 200:
        log_details = log_check.json()
        json_data = log_details.get("json_data", {})
        hours = json_data.get("hours", {})
        if hours and len(hours) > 0:
            log_id = log_id_candidate
            log_date = log.get("date", "Unknown")
            published = log.get("published", False)
            print(f"✅ Found log with content - ID: {log_id}")
            print(f"   Date: {log_date}")
            print(f"   Published: {published}")
            break

if not log_id:
    # Use the first log anyway
    log = logs[0]
    log_id = log["id"]
    log_date = log.get("date", "Unknown")
    published = log.get("published", False)
    print(f"⚠️  Using log ID: {log_id} (may not have content)")
    print(f"   Date: {log_date}")
    print(f"   Published: {published}")

# Get log details
print("\n📄 Getting log details...")
log_response = requests.get(f"{BASE_URL}/logs/{log_id}", headers=auth_headers, timeout=30)
if log_response.status_code == 200:
    log_details = log_response.json()
    json_data = log_details.get("json_data", {})
    hours = json_data.get("hours", {})
    print(f"✅ Log has {len(hours)} hours of content")
    
    # Show summary
    if hours:
        total_elements = sum(len(h.get("elements", [])) for h in hours.values())
        print(f"   Total elements: {total_elements}")

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
    error_detail = publish_response.json().get("detail", "Unknown error") if publish_response.headers.get("content-type", "").startswith("application/json") else publish_response.text
    print(f"   Error: {error_detail}")
    exit(1)

# Check sync status before publishing
print("\n🔍 Checking LibreTime connection status...")
sync_response = requests.get(f"{BASE_URL}/sync/status", headers=auth_headers, timeout=30)
if sync_response.status_code == 200:
    sync_data = sync_response.json()
    connection_status = sync_data.get('connection_status', False)
    print(f"   LibreTime connection status: {connection_status}")
    print(f"   Total tracks: {sync_data.get('total_tracks', 0)}")
    if sync_data.get('last_track_sync'):
        print(f"   Last track sync: {sync_data['last_track_sync']}")
    
    if not connection_status:
        print("⚠️  WARNING: LibreTime connection status is False")
        print("   Publishing may fail. Check LIBRETIME_INTERNAL_URL and LIBRETIME_API_KEY")
else:
    print(f"⚠️  Could not check sync status: {sync_response.status_code}")

# Check LibreTime config
print("\n🔧 Checking LibreTime configuration...")
config_response = requests.get(f"{BASE_URL}/sync/libretime/config", headers=auth_headers, timeout=30)
if config_response.status_code == 200:
    config_data = config_response.json()
    print(f"   Config available: {bool(config_data)}")
    if config_data:
        print(f"   API URL configured: {bool(config_data.get('api_url'))}")
        print(f"   API Key configured: {bool(config_data.get('api_key'))}")
else:
    print(f"⚠️  Could not get config: {config_response.status_code}")

print("\n" + "="*60)
print("✅ SUCCESS! Log pushed to LibreTime")
print("="*60)
print(f"Log ID: {log_id}")
print(f"Date: {log_date}")
print(f"Check LibreTime schedule for date: {log_date}")
print("="*60)

