"""
Log generation tests
"""

import pytest
from fastapi import status
from backend.models.daily_log import DailyLog


class TestLogEndpoints:
    """Test log management endpoints"""
    
    def test_list_logs_empty(self, client, auth_headers):
        """Test listing logs when none exist"""
        response = client.get("/api/logs", headers=auth_headers)
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["logs"] == []
        assert data["total"] == 0
    
    def test_generate_log(self, client, auth_headers):
        """Test log generation"""
        response = client.post(
            "/api/logs/generate",
            json={"date": "2024-01-15"},
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert "Log generated for 2024-01-15" in data["message"]
    
    def test_get_log_not_found(self, client, auth_headers):
        """Test getting nonexistent log"""
        response = client.get("/api/logs/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND
    
    def test_publish_log_not_found(self, client, auth_headers):
        """Test publishing nonexistent log"""
        response = client.post("/api/logs/999/publish", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND


class TestDailyLogModel:
    """Test DailyLog model"""
    
    def test_daily_log_creation(self):
        """Test daily log model creation"""
        log_data = {
            "date": "2024-01-15",
            "hours": [
                {
                    "hour": "06:00",
                    "tracks": [
                        {
                            "time": "06:00:00",
                            "title": "Morning Show Intro",
                            "type": "LIN",
                            "duration": 30
                        }
                    ]
                }
            ]
        }
        
        log = DailyLog(
            date="2024-01-15",
            generated_by=1,
            json_data=log_data
        )
        
        assert log.date == "2024-01-15"
        assert log.generated_by == 1
        assert log.json_data == log_data
        assert log.published is False  # Default value
    
    def test_daily_log_repr(self):
        """Test daily log string representation"""
        log = DailyLog(
            date="2024-01-15",
            generated_by=1,
            json_data={"date": "2024-01-15", "hours": []}
        )
        
        repr_str = repr(log)
        assert "2024-01-15" in repr_str
