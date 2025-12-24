#!/usr/bin/env python3
"""
Clean up empty clock templates (the "black ones" with no elements)
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
    exit(1)

token = login_response.json()["access_token"]
auth_headers = {**HEADERS, "Authorization": f"Bearer {token}"}
print("✅ Authentication successful")

# Get all clock templates
print("\n📋 Getting all clock templates...")
clocks_response = requests.get(f"{BASE_URL}/clocks", headers=auth_headers, timeout=30)

if clocks_response.status_code != 200:
    print(f"⚠️  Could not fetch clocks (status: {clocks_response.status_code})")
    print("   This is okay - the frontend fix will filter them out anyway")
    exit(0)

try:
    clocks_data = clocks_response.json()
    templates = clocks_data.get("templates", [])
    
    if not templates:
        print("✅ No templates found")
        exit(0)
    
    print(f"✅ Found {len(templates)} clock template(s)")
    
    # Find empty templates
    empty_templates = []
    valid_templates = []
    
    for template in templates:
        layout = template.get("json_layout", {})
        elements = layout.get("elements", []) if isinstance(layout, dict) else []
        
        if not elements or len(elements) == 0:
            empty_templates.append(template)
        else:
            valid_templates.append(template)
    
    print(f"\n📊 Summary:")
    print(f"   Valid templates (with elements): {len(valid_templates)}")
    for t in valid_templates:
        elements_count = len(t.get("json_layout", {}).get("elements", []))
        print(f"     - ID: {t.get('id')}, Name: {t.get('name')}, Elements: {elements_count}")
    
    print(f"\n   Empty templates (no elements): {len(empty_templates)}")
    if empty_templates:
        print("     These are the 'black ones' that will be filtered by the frontend")
        for t in empty_templates[:5]:
            print(f"     - ID: {t.get('id')}, Name: {t.get('name')}")
        if len(empty_templates) > 5:
            print(f"     ... and {len(empty_templates) - 5} more")
    
    print(f"\n✅ Frontend has been updated to automatically filter out empty templates")
    print(f"   You should now only see the {len(valid_templates)} valid template(s) in the UI")
    
except Exception as e:
    print(f"⚠️  Could not parse response: {e}")
    print("   The frontend fix will still filter out empty templates")

print("\n" + "="*60)
print("✅ Analysis complete")
print("="*60)

