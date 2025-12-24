#!/usr/bin/env python3
"""
QA Test Script: Sales Workflow - Founding 50 Package
Tests the complete sales workflow as specified in the QA plan.
"""

import requests
import json
import os
from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional

# Use environment variable or container name, fallback to localhost for local development
BASE_URL = os.getenv("LIBRELOG_API_URL", "http://api:8000")
API_BASE = BASE_URL  # Routes are at root level, not /api

# Test data
TEST_AGENCY_NAME = "QA Test Agency"
TEST_CLIENT_NAME = "QA Test Client - Founding 50"
TEST_ORDER_NAME = "Founding 50 Sales Package"
TEST_CONTRACT_REF = "Founding 50 Sales Package - Subscription Document"

# Issues found during testing
issues: List[Dict[str, Any]] = []


def log_issue(phase: str, step: str, description: str, expected: str, actual: str, 
              error: Optional[str] = None, details: Optional[Dict] = None):
    """Log an issue found during testing"""
    issue = {
        "phase": phase,
        "step": step,
        "description": description,
        "expected": expected,
        "actual": actual,
        "error": error,
        "details": details or {},
        "timestamp": datetime.now().isoformat()
    }
    issues.append(issue)
    print(f"\n❌ ISSUE FOUND [{phase} - {step}]:")
    print(f"   Description: {description}")
    print(f"   Expected: {expected}")
    print(f"   Actual: {actual}")
    if error:
        print(f"   Error: {error}")
    print()


def log_success(phase: str, step: str, message: str):
    """Log a successful test step"""
    print(f"✅ [{phase} - {step}]: {message}")


class QATester:
    def __init__(self):
        self.session = requests.Session()
        self.token = None
        self.user_id = None
        self.agency_id = None
        self.client_id = None
        self.order_id = None
        self.sales_rep_id = None
        self.station_id = None
        
    def authenticate(self, username: str = "admin", password: str = "admin123"):
        """Authenticate and get access token"""
        try:
            response = self.session.post(
                f"{API_BASE}/auth/login",
                json={"username": username, "password": password}
            )
            if response.status_code == 200:
                data = response.json()
                self.token = data.get("access_token")
                self.session.headers.update({"Authorization": f"Bearer {self.token}"})
                self.user_id = data.get("user_id")
                log_success("Phase 1", "Authentication", f"Logged in as {username}")
                return True
            else:
                log_issue("Phase 1", "Authentication", 
                         "Failed to authenticate", 
                         "Status 200 with access_token",
                         f"Status {response.status_code}: {response.text}")
                return False
        except Exception as e:
            log_issue("Phase 1", "Authentication",
                     "Exception during authentication",
                     "Successful login",
                     f"Exception: {str(e)}")
            return False
    
    def get_sales_reps(self) -> List[Dict]:
        """Get list of sales reps"""
        try:
            response = self.session.get(f"{API_BASE}/sales-reps")
            if response.status_code == 200:
                data = response.json()
                return data if isinstance(data, list) else []
            return []
        except Exception as e:
            log_issue("Phase 1", "Get Sales Reps",
                     "Failed to get sales reps",
                     "List of sales reps",
                     f"Exception: {str(e)}")
            return []
    
    def get_stations(self) -> List[Dict]:
        """Get list of stations"""
        try:
            response = self.session.get(f"{API_BASE}/proxy/stations", params={"active_only": True})
            if response.status_code == 200:
                data = response.json()
                return data if isinstance(data, list) else (data.get("stations", []) if isinstance(data, dict) else [])
            return []
        except Exception as e:
            log_issue("Phase 1", "Get Stations",
                     "Failed to get stations",
                     "List of stations",
                     f"Exception: {str(e)}")
            return []
    
    def create_agency(self) -> bool:
        """Create a test agency"""
        try:
            agency_data = {
                "name": TEST_AGENCY_NAME,
                "contact_first_name": "Test",
                "contact_last_name": "Contact",
                "email": "test@agency.com",
                "phone": "555-0000",
                "address": "123 Test St, Test City, ST 12345",
                "commission_rate": 15.0,
                "active": True
            }
            response = self.session.post(f"{API_BASE}/agencies", json=agency_data)
            if response.status_code in [200, 201]:
                data = response.json()
                self.agency_id = data.get("id")
                log_success("Phase 2", "Create Agency", f"Agency created with ID {self.agency_id}")
                return True
            else:
                log_issue("Phase 2", "Create Agency",
                         "Failed to create agency",
                         f"Status 200/201 with agency ID",
                         f"Status {response.status_code}: {response.text}",
                         details={"request": agency_data, "response": response.text})
                return False
        except Exception as e:
            log_issue("Phase 2", "Create Agency",
                     "Exception creating agency",
                     "Agency created successfully",
                     f"Exception: {str(e)}")
            return False
    
    def create_client(self) -> bool:
        """Create a test client (advertiser) with agency"""
        if not self.agency_id:
            log_issue("Phase 3", "Create Client",
                     "Cannot create client without agency",
                     "Agency ID available",
                     "Agency ID is None")
            return False
        
        try:
            client_data = {
                "name": TEST_CLIENT_NAME,
                "contact_name": "Test Contact",
                "email": "client@test.com",
                "phone": "555-0001",
                "address": "456 Client Ave, Client City, ST 67890",
                "payment_terms": "Net 30",
                "credit_limit": 5000.0,
                "agency_id": self.agency_id,
                "active": True
            }
            response = self.session.post(f"{API_BASE}/advertisers", json=client_data)
            if response.status_code in [200, 201]:
                data = response.json()
                self.client_id = data.get("id")
                log_success("Phase 3", "Create Client", f"Client created with ID {self.client_id}, linked to agency {self.agency_id}")
                
                # Verify agency relationship
                if data.get("agency_id") != self.agency_id:
                    log_issue("Phase 3", "Create Client",
                             "Agency relationship not saved",
                             f"Agency ID {self.agency_id}",
                             f"Got {data.get('agency_id')}")
                return True
            else:
                log_issue("Phase 3", "Create Client",
                         "Failed to create client",
                         f"Status 200/201 with client ID",
                         f"Status {response.status_code}: {response.text}",
                         details={"request": client_data, "response": response.text})
                return False
        except Exception as e:
            log_issue("Phase 3", "Create Client",
                     "Exception creating client",
                     "Client created successfully",
                     f"Exception: {str(e)}")
            return False
    
    def create_order(self) -> bool:
        """Create order for Founding 50 Sales Package"""
        if not self.client_id:
            log_issue("Phase 4", "Create Order",
                     "Cannot create order without client",
                     "Client ID available",
                     "Client ID is None")
            return False
        
        # Get sales rep
        sales_reps = self.get_sales_reps()
        if sales_reps:
            self.sales_rep_id = sales_reps[0].get("id")
        else:
            log_issue("Phase 4", "Create Order",
                     "No sales rep available",
                     "At least one sales rep exists",
                     "Sales rep list is empty")
            # Continue anyway, sales_rep_id can be None
        
        # Get station
        stations = self.get_stations()
        if stations:
            self.station_id = stations[0].get("id")
        else:
            log_issue("Phase 4", "Create Order",
                     "No station available",
                     "At least one station exists",
                     "Station list is empty")
        
        # Calculate dates
        start_date = datetime.now().date() + timedelta(days=1)
        end_date = start_date + timedelta(days=30)
        
        try:
            order_data = {
                "order_name": TEST_ORDER_NAME,
                "advertiser_id": self.client_id,
                "agency_id": self.agency_id,
                "sales_rep_id": self.sales_rep_id,
                "start_date": start_date.isoformat(),
                "end_date": end_date.isoformat(),
                "total_spots": 10,
                "spot_lengths": [30],
                "rate_type": "ROS",
                "rates": {"ROS": {"30": 10.0}},
                "total_value": 100.0,
                "contract_reference": TEST_CONTRACT_REF,
                "status": "DRAFT",
                "approval_status": "NOT_REQUIRED"
            }
            response = self.session.post(f"{API_BASE}/orders", json=order_data)
            if response.status_code in [200, 201]:
                data = response.json()
                self.order_id = data.get("id")
                log_success("Phase 4", "Create Order", f"Order created with ID {self.order_id}")
                
                # Verify key fields
                if data.get("order_name") != TEST_ORDER_NAME:
                    log_issue("Phase 4", "Create Order",
                             "Order name not saved correctly",
                             TEST_ORDER_NAME,
                             data.get("order_name"))
                
                if data.get("contract_reference") != TEST_CONTRACT_REF:
                    log_issue("Phase 4", "Create Order",
                             "Contract reference not saved",
                             TEST_CONTRACT_REF,
                             data.get("contract_reference"))
                
                if data.get("total_value") != 100.0:
                    log_issue("Phase 4", "Create Order",
                             "Total value incorrect",
                             "100.0",
                             str(data.get("total_value")))
                
                if data.get("agency_id") != self.agency_id:
                    log_issue("Phase 4", "Create Order",
                             "Agency not linked to order",
                             str(self.agency_id),
                             str(data.get("agency_id")))
                
                return True
            else:
                log_issue("Phase 4", "Create Order",
                         "Failed to create order",
                         f"Status 200/201 with order ID",
                         f"Status {response.status_code}: {response.text}",
                         details={"request": order_data, "response": response.text})
                return False
        except Exception as e:
            log_issue("Phase 4", "Create Order",
                     "Exception creating order",
                     "Order created successfully",
                     f"Exception: {str(e)}")
            return False
    
    def approve_order(self) -> bool:
        """Approve the order"""
        if not self.order_id:
            return False
        
        try:
            # Update status to APPROVED
            update_data = {
                "status": "APPROVED",
                "approval_status": "APPROVED"
            }
            response = self.session.put(f"{API_BASE}/orders/{self.order_id}", json=update_data)
            if response.status_code == 200:
                data = response.json()
                if data.get("status") == "APPROVED":
                    log_success("Phase 6", "Approve Order", "Order approved successfully")
                    return True
                else:
                    log_issue("Phase 6", "Approve Order",
                             "Order status not updated to APPROVED",
                             "APPROVED",
                             data.get("status"))
                    return False
            else:
                log_issue("Phase 6", "Approve Order",
                         "Failed to approve order",
                         "Status 200 with APPROVED status",
                         f"Status {response.status_code}: {response.text}")
                return False
        except Exception as e:
            log_issue("Phase 6", "Approve Order",
                     "Exception approving order",
                     "Order approved successfully",
                     f"Exception: {str(e)}")
            return False
    
    def schedule_spots(self) -> bool:
        """Schedule 10 ROS spots"""
        if not self.order_id:
            return False
        
        if not self.station_id:
            log_issue("Phase 7", "Schedule Spots",
                     "Cannot schedule spots without station",
                     "Station ID available",
                     "Station ID is None")
            return False
        
        # Get order to get date range
        try:
            order_response = self.session.get(f"{API_BASE}/orders/{self.order_id}")
            if order_response.status_code != 200:
                log_issue("Phase 7", "Schedule Spots",
                         "Cannot get order details",
                         "Order details retrieved",
                         f"Status {order_response.status_code}")
                return False
            
            order_data = order_response.json()
            start_date = datetime.fromisoformat(order_data["start_date"]).date()
            end_date = datetime.fromisoformat(order_data["end_date"]).date()
            
            # Create spots - distribute 10 spots across date range
            spots = []
            current_date = start_date
            spots_per_day = max(1, 10 // max(1, (end_date - start_date).days + 1))
            spot_count = 0
            
            while current_date <= end_date and spot_count < 10:
                for i in range(spots_per_day):
                    if spot_count >= 10:
                        break
                    # Schedule at different times throughout the day (ROS)
                    hour = 8 + (spot_count % 12)  # 8 AM to 7 PM
                    spots.append({
                        "order_id": self.order_id,
                        "station_id": self.station_id,
                        "scheduled_date": current_date.isoformat(),
                        "scheduled_time": f"{hour:02d}:00:00",
                        "spot_length": 30,
                        "status": "SCHEDULED"
                    })
                    spot_count += 1
                current_date += timedelta(days=1)
            
            # Create spots via bulk endpoint
            response = self.session.post(
                f"{API_BASE}/spots/bulk",
                params={"order_id": self.order_id},
                json=spots
            )
            
            if response.status_code in [200, 201]:
                created_spots = response.json()
                if isinstance(created_spots, list) and len(created_spots) == 10:
                    log_success("Phase 7", "Schedule Spots", f"Successfully scheduled {len(created_spots)} spots")
                    return True
                else:
                    log_issue("Phase 7", "Schedule Spots",
                             "Wrong number of spots created",
                             "10 spots",
                             f"{len(created_spots) if isinstance(created_spots, list) else 'non-list response'}")
                    return False
            else:
                log_issue("Phase 7", "Schedule Spots",
                         "Failed to schedule spots",
                         f"Status 200/201 with 10 spots",
                         f"Status {response.status_code}: {response.text}",
                         details={"spots_requested": len(spots), "response": response.text})
                return False
        except Exception as e:
            log_issue("Phase 7", "Schedule Spots",
                     "Exception scheduling spots",
                     "Spots scheduled successfully",
                     f"Exception: {str(e)}")
            return False
    
    def verify_workflow(self) -> bool:
        """Verify complete workflow data integrity"""
        all_good = True
        
        # Verify agency exists
        try:
            response = self.session.get(f"{API_BASE}/agencies/{self.agency_id}")
            if response.status_code != 200:
                log_issue("Phase 8", "Verify Workflow",
                         "Agency not found",
                         "Agency exists",
                         f"Status {response.status_code}")
                all_good = False
        except Exception as e:
            log_issue("Phase 8", "Verify Workflow",
                     "Exception verifying agency",
                     "Agency verified",
                     f"Exception: {str(e)}")
            all_good = False
        
        # Verify client exists and is linked to agency
        try:
            response = self.session.get(f"{API_BASE}/advertisers/{self.client_id}")
            if response.status_code == 200:
                data = response.json()
                if data.get("agency_id") != self.agency_id:
                    log_issue("Phase 8", "Verify Workflow",
                             "Client not linked to agency",
                             f"Agency ID {self.agency_id}",
                             str(data.get("agency_id")))
                    all_good = False
            else:
                log_issue("Phase 8", "Verify Workflow",
                         "Client not found",
                         "Client exists",
                         f"Status {response.status_code}")
                all_good = False
        except Exception as e:
            log_issue("Phase 8", "Verify Workflow",
                     "Exception verifying client",
                     "Client verified",
                     f"Exception: {str(e)}")
            all_good = False
        
        # Verify order exists with correct data
        try:
            response = self.session.get(f"{API_BASE}/orders/{self.order_id}")
            if response.status_code == 200:
                data = response.json()
                if data.get("order_name") != TEST_ORDER_NAME:
                    log_issue("Phase 8", "Verify Workflow",
                             "Order name incorrect",
                             TEST_ORDER_NAME,
                             data.get("order_name"))
                    all_good = False
                if data.get("contract_reference") != TEST_CONTRACT_REF:
                    log_issue("Phase 8", "Verify Workflow",
                             "Contract reference incorrect",
                             TEST_CONTRACT_REF,
                             data.get("contract_reference"))
                    all_good = False
            else:
                log_issue("Phase 8", "Verify Workflow",
                         "Order not found",
                         "Order exists",
                         f"Status {response.status_code}")
                all_good = False
        except Exception as e:
            log_issue("Phase 8", "Verify Workflow",
                     "Exception verifying order",
                     "Order verified",
                     f"Exception: {str(e)}")
            all_good = False
        
        # Verify spots exist
        try:
            response = self.session.get(f"{API_BASE}/spots", params={"order_id": self.order_id})
            if response.status_code == 200:
                spots = response.json()
                if isinstance(spots, list):
                    if len(spots) != 10:
                        log_issue("Phase 8", "Verify Workflow",
                                 "Wrong number of spots",
                                 "10 spots",
                                 f"{len(spots)} spots")
                        all_good = False
                else:
                    log_issue("Phase 8", "Verify Workflow",
                             "Spots response not a list",
                             "List of spots",
                             f"Type: {type(spots)}")
                    all_good = False
            else:
                log_issue("Phase 8", "Verify Workflow",
                         "Failed to get spots",
                         "List of spots",
                         f"Status {response.status_code}")
                all_good = False
        except Exception as e:
            log_issue("Phase 8", "Verify Workflow",
                     "Exception verifying spots",
                     "Spots verified",
                     f"Exception: {str(e)}")
            all_good = False
        
        if all_good:
            log_success("Phase 8", "Verify Workflow", "All workflow data verified successfully")
        
        return all_good
    
    def run_full_test(self):
        """Run the complete QA test workflow"""
        print("=" * 80)
        print("QA TEST: Sales Workflow - Founding 50 Package")
        print("=" * 80)
        print()
        
        # Phase 1: Setup
        print("Phase 1: Setup and Prerequisites")
        print("-" * 80)
        if not self.authenticate():
            print("❌ Cannot proceed without authentication")
            return
        
        sales_reps = self.get_sales_reps()
        if sales_reps:
            log_success("Phase 1", "Check Sales Reps", f"Found {len(sales_reps)} sales rep(s)")
        else:
            log_issue("Phase 1", "Check Sales Reps", "No sales reps found", "At least one sales rep", "None found")
        
        stations = self.get_stations()
        if stations:
            log_success("Phase 1", "Check Stations", f"Found {len(stations)} station(s)")
            self.station_id = stations[0].get("id")
        else:
            log_issue("Phase 1", "Check Stations", "No stations found", "At least one station", "None found")
        
        print()
        
        # Phase 2: Create Agency
        print("Phase 2: Create Agency")
        print("-" * 80)
        if not self.create_agency():
            print("❌ Cannot proceed without agency")
            return
        print()
        
        # Phase 3: Create Client
        print("Phase 3: Create Client with Agency")
        print("-" * 80)
        if not self.create_client():
            print("❌ Cannot proceed without client")
            return
        print()
        
        # Phase 4: Create Order
        print("Phase 4: Create Order - Founding 50 Sales Package")
        print("-" * 80)
        if not self.create_order():
            print("❌ Cannot proceed without order")
            return
        print()
        
        # Phase 6: Approve Order
        print("Phase 6: Approve Order")
        print("-" * 80)
        self.approve_order()
        print()
        
        # Phase 7: Schedule Spots
        print("Phase 7: Schedule Spots to ROS")
        print("-" * 80)
        if not self.schedule_spots():
            print("⚠️  Spot scheduling had issues, but continuing...")
        print()
        
        # Phase 8: Verify Workflow
        print("Phase 8: Verify Complete Workflow")
        print("-" * 80)
        self.verify_workflow()
        print()
        
        # Phase 9: Report Issues
        print("=" * 80)
        print("TEST SUMMARY")
        print("=" * 80)
        print(f"Total Issues Found: {len(issues)}")
        print()
        
        if issues:
            print("ISSUES FOUND:")
            print("-" * 80)
            for i, issue in enumerate(issues, 1):
                print(f"\n{i}. [{issue['phase']} - {issue['step']}]")
                print(f"   Description: {issue['description']}")
                print(f"   Expected: {issue['expected']}")
                print(f"   Actual: {issue['actual']}")
                if issue.get('error'):
                    print(f"   Error: {issue['error']}")
        else:
            print("✅ No issues found! All tests passed.")
        
        # Save issues to file
        with open("/home/jenkins/docker/librelog/qa_test_issues.json", "w") as f:
            json.dump(issues, f, indent=2)
        print(f"\n📄 Issues saved to: qa_test_issues.json")


if __name__ == "__main__":
    tester = QATester()
    tester.run_full_test()

