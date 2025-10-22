"""
Reports tests
"""

import pytest
from fastapi import status


class TestReportEndpoints:
    """Test report management endpoints"""
    
    def test_reconciliation_report(self, client, auth_headers):
        """Test reconciliation report generation"""
        response = client.get(
            "/api/reports/reconciliation",
            params={
                "start_date": "2024-01-01",
                "end_date": "2024-01-31"
            },
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert "report" in data
        assert "total_discrepancies" in data
    
    def test_compliance_report(self, client, auth_headers):
        """Test compliance report generation"""
        response = client.get(
            "/api/reports/compliance",
            params={
                "start_date": "2024-01-01",
                "end_date": "2024-01-31",
                "format": "csv"
            },
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert "Compliance report generated in csv format" in data["message"]
    
    def test_playback_history(self, client, auth_headers):
        """Test playback history retrieval"""
        response = client.get(
            "/api/reports/playback",
            params={
                "start_date": "2024-01-01",
                "end_date": "2024-01-31"
            },
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert "playback_history" in data
        assert "total_tracks" in data
