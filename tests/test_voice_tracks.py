"""
Voice tracking tests
"""

import pytest
from fastapi import status
from backend.models.voice_track import VoiceTrack


class TestVoiceTrackEndpoints:
    """Test voice track management endpoints"""
    
    def test_list_voice_tracks_empty(self, client, auth_headers):
        """Test listing voice tracks when none exist"""
        response = client.get("/api/voice", headers=auth_headers)
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["voice_tracks"] == []
        assert data["total"] == 0
    
    def test_upload_voice_track(self, client, auth_headers):
        """Test voice track upload"""
        response = client.post(
            "/api/voice/upload",
            files={"file": ("test.wav", b"fake audio data", "audio/wav")},
            data={"show_name": "Morning Show"},
            headers=auth_headers
        )
        
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data["message"] == "Voice track uploaded successfully"
    
    def test_get_voice_track_not_found(self, client, auth_headers):
        """Test getting nonexistent voice track"""
        response = client.get("/api/voice/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND
    
    def test_delete_voice_track_not_found(self, client, auth_headers):
        """Test deleting nonexistent voice track"""
        response = client.delete("/api/voice/999", headers=auth_headers)
        
        assert response.status_code == status.HTTP_404_NOT_FOUND


class TestVoiceTrackModel:
    """Test VoiceTrack model"""
    
    def test_voice_track_creation(self):
        """Test voice track model creation"""
        voice_track = VoiceTrack(
            show_name="Morning Show",
            file_url="/uploads/voice/morning_intro.wav",
            uploaded_by=1
        )
        
        assert voice_track.show_name == "Morning Show"
        assert voice_track.file_url == "/uploads/voice/morning_intro.wav"
        assert voice_track.uploaded_by == 1
    
    def test_voice_track_repr(self):
        """Test voice track string representation"""
        voice_track = VoiceTrack(
            show_name="Morning Show",
            file_url="/uploads/voice/morning_intro.wav",
            uploaded_by=1
        )
        
        repr_str = repr(voice_track)
        assert "Morning Show" in repr_str
