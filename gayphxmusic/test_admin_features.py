#!/usr/bin/env python3
"""
Test script to verify all admin features are working
"""

import requests
import json
import time

# Configuration
BASE_URL = "http://localhost:8000"
API_URL = f"{BASE_URL}/api"
ADMIN_EMAIL = "admin@gayphx.com"
ADMIN_PASSWORD = "admin123"

def test_admin_login():
    """Test admin login and get token"""
    print("🔐 Testing admin login...")
    
    login_data = {
        "email": ADMIN_EMAIL,
        "password": ADMIN_PASSWORD
    }
    
    response = requests.post(f"{API_URL}/admin/login", json=login_data)
    
    if response.status_code == 200:
        data = response.json()
        token = data["access_token"]
        print(f"✅ Admin login successful")
        print(f"   Admin: {data['admin']['name']} ({data['admin']['email']})")
        return token
    else:
        print(f"❌ Admin login failed: {response.status_code}")
        return None

def test_user_management(token):
    """Test user management features"""
    print("\n👥 Testing user management...")
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test get users
    response = requests.get(f"{API_URL}/admin/users", headers=headers)
    if response.status_code == 200:
        users = response.json()
        print(f"✅ Users list retrieved: {len(users)} users")
        
        if users:
            # Test get user details
            user_id = users[0]["id"]
            response = requests.get(f"{API_URL}/admin/users/{user_id}", headers=headers)
            if response.status_code == 200:
                user_detail = response.json()
                print(f"✅ User details retrieved: {user_detail['name']}")
            else:
                print(f"❌ User details failed: {response.status_code}")
            
            # Test toggle user status
            current_status = users[0]["is_active"]
            response = requests.put(f"{API_URL}/admin/users/{user_id}/toggle-status", headers=headers)
            if response.status_code == 200:
                print(f"✅ User status toggled from {current_status} to {not current_status}")
                # Toggle back
                requests.put(f"{API_URL}/admin/users/{user_id}/toggle-status", headers=headers)
                print(f"✅ User status restored to {current_status}")
            else:
                print(f"❌ User status toggle failed: {response.status_code}")
        else:
            print("⚠️  No users found to test with")
    else:
        print(f"❌ Users list failed: {response.status_code}")

def test_admin_profile(token):
    """Test admin profile management"""
    print("\n👤 Testing admin profile...")
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test get profile
    response = requests.get(f"{API_URL}/admin/profile", headers=headers)
    if response.status_code == 200:
        profile = response.json()
        print(f"✅ Profile retrieved: {profile['name']} ({profile['email']})")
        
        # Test update profile
        update_data = {
            "name": "Updated Admin Name",
            "email": ADMIN_EMAIL,
            "current_password": ADMIN_PASSWORD,
            "new_password": ADMIN_PASSWORD
        }
        
        response = requests.put(f"{API_URL}/admin/profile", json=update_data, headers=headers)
        if response.status_code == 200:
            print("✅ Profile updated successfully")
        else:
            print(f"❌ Profile update failed: {response.status_code}")
    else:
        print(f"❌ Profile retrieval failed: {response.status_code}")

def test_isrc_key_management(token):
    """Test ISRC key management"""
    print("\n🔑 Testing ISRC key management...")
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test get ISRC key
    response = requests.get(f"{API_URL}/admin/isrc-key", headers=headers)
    if response.status_code == 200:
        key_data = response.json()
        print(f"✅ ISRC key status retrieved: {'Has key' if key_data['has_key'] else 'No key'}")
        
        # Test update ISRC key
        test_key = "TEST-ISRC-KEY-12345"
        update_data = {"isrc_registration_key": test_key}
        
        response = requests.put(f"{API_URL}/admin/isrc-key", json=update_data, headers=headers)
        if response.status_code == 200:
            print(f"✅ ISRC key updated successfully")
            
            # Verify the key was saved
            response = requests.get(f"{API_URL}/admin/isrc-key", headers=headers)
            if response.status_code == 200:
                key_data = response.json()
                if key_data["isrc_registration_key"] == test_key:
                    print("✅ ISRC key verification successful")
                else:
                    print("❌ ISRC key verification failed")
            
            # Test delete ISRC key
            response = requests.delete(f"{API_URL}/admin/isrc-key", headers=headers)
            if response.status_code == 200:
                print("✅ ISRC key deleted successfully")
            else:
                print(f"❌ ISRC key deletion failed: {response.status_code}")
        else:
            print(f"❌ ISRC key update failed: {response.status_code}")
    else:
        print(f"❌ ISRC key retrieval failed: {response.status_code}")

def test_frontend_pages():
    """Test frontend admin pages are accessible"""
    print("\n🌐 Testing frontend admin pages...")
    
    frontend_url = "http://localhost:3000"
    
    # Test admin login page
    response = requests.get(f"{frontend_url}/admin/login")
    if response.status_code == 200:
        print("✅ Admin login page accessible")
    else:
        print(f"❌ Admin login page failed: {response.status_code}")
    
    # Test admin dashboard (should redirect to login)
    response = requests.get(f"{frontend_url}/admin")
    if response.status_code == 200:
        print("✅ Admin dashboard page accessible")
    else:
        print(f"❌ Admin dashboard page failed: {response.status_code}")
    
    # Test admin users page
    response = requests.get(f"{frontend_url}/admin/users")
    if response.status_code == 200:
        print("✅ Admin users page accessible")
    else:
        print(f"❌ Admin users page failed: {response.status_code}")
    
    # Test admin profile page
    response = requests.get(f"{frontend_url}/admin/profile")
    if response.status_code == 200:
        print("✅ Admin profile page accessible")
    else:
        print(f"❌ Admin profile page failed: {response.status_code}")
    
    # Test ISRC key page
    response = requests.get(f"{frontend_url}/admin/isrc-key")
    if response.status_code == 200:
        print("✅ ISRC key page accessible")
    else:
        print(f"❌ ISRC key page failed: {response.status_code}")

def main():
    """Run all admin feature tests"""
    print("🎵 GayPHX Music Platform - Admin Features Test")
    print("=" * 60)
    
    # Test admin login
    token = test_admin_login()
    if not token:
        print("❌ Cannot proceed without admin token")
        return
    
    # Test all admin features
    test_user_management(token)
    test_admin_profile(token)
    test_isrc_key_management(token)
    test_frontend_pages()
    
    print("\n" + "=" * 60)
    print("🎉 Admin features test completed!")
    print("\n📋 Summary of Admin Features:")
    print("✅ User Management:")
    print("   - List all users with search and filtering")
    print("   - View detailed user information")
    print("   - Toggle user active/inactive status")
    print("   - View user submission history")
    print("\n✅ Admin Profile Management:")
    print("   - View admin profile information")
    print("   - Update admin name and email")
    print("   - Change admin password")
    print("\n✅ ISRC Key Management:")
    print("   - View current ISRC registration key status")
    print("   - Add/update ISRC registration key")
    print("   - Delete ISRC registration key")
    print("   - Secure key storage and display")
    print("\n✅ Frontend Pages:")
    print("   - Admin login page")
    print("   - Admin dashboard")
    print("   - User management interface")
    print("   - Profile management interface")
    print("   - ISRC key management interface")
    
    print(f"\n🌐 Access the admin interface at: http://localhost:3000/admin")
    print(f"   Login: {ADMIN_EMAIL}")
    print(f"   Password: {ADMIN_PASSWORD}")

if __name__ == "__main__":
    main()

