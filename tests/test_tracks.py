"""
Track management tests
"""

import pytest
from fastapi import status
from backend.models.track import Track


class TestTrackEndpoints:
    """Test track management endpoints"""
    
    def test_list_tracks_empty(self, client, auth_headers):
        """Test listing tracks when none exist"""
        response = client.get("/api/tracks", headers=auth_headers)
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["tracks"] == []
        assert data["total"] == 0
    
    def test_list_tracks_with_filter(self, client, auth_headers):
        """Test listing tracks with type filter"""
        response = client.get(
            "/api/tracks?track_type=MUS",
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert "tracks" in data
        assert "total" in data
    
    def test_create_track(self, client, auth_headers):
        """Test track creation"""
        track_data = {
            "title": "Test Track",
            "artist": "Test Artist",
            "type": "MUS",
            "duration": 180
        }
        
        response = client.post(
            "/api/tracks",
            json=track_data,
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["message"] == "Track created successfully"
    
    def test_get_track_not_found(self, client, auth_headers):
        """Test getting nonexistent track"""
        response = client.get("/api/tracks/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND
    
    def test_update_track_not_found(self, client, auth_headers):
        """Test updating nonexistent track"""
        track_data = {
            "title": "Updated Track",
            "artist": "Updated Artist"
        }
        
        response = client.put(
            "/api/tracks/999",
            json=track_data,
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_404_NOT_FOUND
    
    def test_delete_track_not_found(self, client, auth_headers):
        """Test deleting nonexistent track"""
        response = client.delete("/api/tracks/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND


class TestTrackModel:
    """Test Track model"""
    
    def test_track_creation(self):
        """Test track model creation"""
        track = Track(
            title="Test Track",
            artist="Test Artist",
            type="MUS",
            duration=180,
            filepath="/path/to/file.mp3"
        )
        
        assert track.title == "Test Track"
        assert track.artist == "Test Artist"
        assert track.type == "MUS"
        assert track.duration == 180
        assert track.filepath == "/path/to/file.mp3"
    
    def test_track_repr(self):
        """Test track string representation"""
        track = Track(
            title="Test Track",
            artist="Test Artist",
            type="MUS",
            filepath="/path/to/file.mp3"
        )
        
        repr_str = repr(track)
        assert "Test Track" in repr_str
        assert "Test Artist" in repr_str
        assert "MUS" in repr_str
