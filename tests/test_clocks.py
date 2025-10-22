"""
Clock template tests
"""

import pytest
from fastapi import status
from backend.models.clock_template import ClockTemplate


class TestClockTemplateEndpoints:
    """Test clock template management endpoints"""
    
    def test_list_clock_templates_empty(self, client, auth_headers):
        """Test listing clock templates when none exist"""
        response = client.get("/api/clocks", headers=auth_headers)
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["templates"] == []
        assert data["total"] == 0
    
    def test_create_clock_template(self, client, auth_headers):
        """Test clock template creation"""
        template_data = {
            "name": "Morning Template",
            "description": "Morning programming template",
            "json_layout": {
                "hour": "06:00",
                "elements": [
                    {"type": "LIN", "title": "Top-of-Hour ID"},
                    {"type": "MUS", "count": 3}
                ]
            }
        }
        
        response = client.post(
            "/api/clocks",
            json=template_data,
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["message"] == "Clock template created successfully"
    
    def test_get_clock_template_not_found(self, client, auth_headers):
        """Test getting nonexistent clock template"""
        response = client.get("/api/clocks/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND
    
    def test_update_clock_template_not_found(self, client, auth_headers):
        """Test updating nonexistent clock template"""
        template_data = {
            "name": "Updated Template"
        }
        
        response = client.put(
            "/api/clocks/999",
            json=template_data,
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_404_NOT_FOUND
    
    def test_delete_clock_template_not_found(self, client, auth_headers):
        """Test deleting nonexistent clock template"""
        response = client.delete("/api/clocks/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND


class TestClockTemplateModel:
    """Test ClockTemplate model"""
    
    def test_clock_template_creation(self):
        """Test clock template model creation"""
        layout = {
            "hour": "06:00",
            "elements": [
                {"type": "LIN", "title": "Top-of-Hour ID"},
                {"type": "MUS", "count": 3}
            ]
        }
        
        template = ClockTemplate(
            name="Morning Template",
            description="Morning programming template",
            json_layout=layout
        )
        
        assert template.name == "Morning Template"
        assert template.description == "Morning programming template"
        assert template.json_layout == layout
    
    def test_clock_template_repr(self):
        """Test clock template string representation"""
        layout = {
            "hour": "06:00",
            "elements": []
        }
        
        template = ClockTemplate(
            name="Morning Template",
            json_layout=layout
        )
        
        repr_str = repr(template)
        assert "Morning Template" in repr_str
