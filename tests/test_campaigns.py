"""
Campaign management tests
"""

import pytest
from fastapi import status
from backend.models.campaign import Campaign


class TestCampaignEndpoints:
    """Test campaign management endpoints"""
    
    def test_list_campaigns_empty(self, client, auth_headers):
        """Test listing campaigns when none exist"""
        response = client.get("/api/campaigns", headers=auth_headers)
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["campaigns"] == []
        assert data["total"] == 0
    
    def test_list_campaigns_active_only(self, client, auth_headers):
        """Test listing campaigns with active filter"""
        response = client.get(
            "/api/campaigns?active_only=true",
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert "campaigns" in data
        assert "total" in data
    
    def test_create_campaign(self, client, auth_headers):
        """Test campaign creation"""
        campaign_data = {
            "advertiser": "Test Advertiser",
            "start_date": "2024-01-01",
            "end_date": "2024-01-31",
            "priority": 1
        }
        
        response = client.post(
            "/api/campaigns",
            json=campaign_data,
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["message"] == "Campaign created successfully"
    
    def test_get_campaign_not_found(self, client, auth_headers):
        """Test getting nonexistent campaign"""
        response = client.get("/api/campaigns/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND
    
    def test_update_campaign_not_found(self, client, auth_headers):
        """Test updating nonexistent campaign"""
        campaign_data = {
            "advertiser": "Updated Advertiser"
        }
        
        response = client.put(
            "/api/campaigns/999",
            json=campaign_data,
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_404_NOT_FOUND
    
    def test_delete_campaign_not_found(self, client, auth_headers):
        """Test deleting nonexistent campaign"""
        response = client.delete("/api/campaigns/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND


class TestCampaignModel:
    """Test Campaign model"""
    
    def test_campaign_creation(self):
        """Test campaign model creation"""
        campaign = Campaign(
            advertiser="Test Advertiser",
            start_date="2024-01-01",
            end_date="2024-01-31",
            priority=1
        )
        
        assert campaign.advertiser == "Test Advertiser"
        assert campaign.start_date == "2024-01-01"
        assert campaign.end_date == "2024-01-31"
        assert campaign.priority == 1
        assert campaign.active is True  # Default value
    
    def test_campaign_repr(self):
        """Test campaign string representation"""
        campaign = Campaign(
            advertiser="Test Advertiser",
            start_date="2024-01-01",
            end_date="2024-01-31",
            priority=1
        )
        
        repr_str = repr(campaign)
        assert "Test Advertiser" in repr_str
        assert "1" in repr_str
