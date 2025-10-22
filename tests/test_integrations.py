"""
Integration tests
"""

import pytest
from unittest.mock import AsyncMock, patch
from backend.integrations.libretime_client import LibreTimeClient
from backend.integrations.azuracast_client import AzuraCastClient
from backend.auth.token_manager import TokenManager


class TestLibreTimeClient:
    """Test LibreTime API client"""
    
    @pytest.fixture
    def client(self):
        """Create LibreTime client"""
        return LibreTimeClient()
    
    @pytest.mark.asyncio
    async def test_get_tracks_success(self, client):
        """Test successful track retrieval"""
        mock_response = {
            "tracks": [
                {
                    "id": "123",
                    "title": "Test Track",
                    "artist": "Test Artist",
                    "duration": 180
                }
            ]
        }
        
        with patch.object(client, '_make_request', new_callable=AsyncMock) as mock_request:
            mock_request.return_value.json.return_value = mock_response
            
            tracks = await client.get_tracks()
            
            assert len(tracks) == 1
            assert tracks[0]["title"] == "Test Track"
            mock_request.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_get_tracks_failure(self, client):
        """Test track retrieval failure"""
        with patch.object(client, '_make_request', new_callable=AsyncMock) as mock_request:
            mock_request.side_effect = Exception("API Error")
            
            tracks = await client.get_tracks()
            
            assert tracks == []
    
    @pytest.mark.asyncio
    async def test_get_smart_blocks(self, client):
        """Test smart blocks retrieval"""
        mock_response = {
            "smart_blocks": [
                {
                    "id": "block1",
                    "name": "Morning Block",
                    "description": "Morning programming"
                }
            ]
        }
        
        with patch.object(client, '_make_request', new_callable=AsyncMock) as mock_request:
            mock_request.return_value.json.return_value = mock_response
            
            blocks = await client.get_smart_blocks()
            
            assert len(blocks) == 1
            assert blocks[0]["name"] == "Morning Block"
    
    @pytest.mark.asyncio
    async def test_health_check(self, client):
        """Test health check"""
        with patch.object(client, '_make_request', new_callable=AsyncMock) as mock_request:
            mock_request.return_value.status_code = 200
            
            is_healthy = await client.health_check()
            
            assert is_healthy is True


class TestAzuraCastClient:
    """Test AzuraCast API client"""
    
    @pytest.fixture
    def client(self):
        """Create AzuraCast client"""
        return AzuraCastClient()
    
    @pytest.mark.asyncio
    async def test_get_now_playing(self, client):
        """Test now playing retrieval"""
        mock_response = {
            "title": "Current Song",
            "artist": "Current Artist",
            "duration": 240
        }
        
        with patch.object(client, '_make_request', new_callable=AsyncMock) as mock_request:
            mock_request.return_value.json.return_value = mock_response
            
            now_playing = await client.get_now_playing()
            
            assert now_playing["title"] == "Current Song"
            assert now_playing["artist"] == "Current Artist"
    
    @pytest.mark.asyncio
    async def test_get_listeners(self, client):
        """Test listener stats retrieval"""
        mock_response = {
            "total": 150,
            "unique": 120
        }
        
        with patch.object(client, '_make_request', new_callable=AsyncMock) as mock_request:
            mock_request.return_value.json.return_value = mock_response
            
            listeners = await client.get_listeners()
            
            assert listeners["total"] == 150
            assert listeners["unique"] == 120
    
    @pytest.mark.asyncio
    async def test_update_now_playing(self, client):
        """Test now playing update"""
        track_data = {
            "title": "New Song",
            "artist": "New Artist"
        }
        
        with patch.object(client, '_make_request', new_callable=AsyncMock) as mock_request:
            mock_request.return_value.status_code = 200
            
            success = await client.update_now_playing(track_data)
            
            assert success is True


class TestTokenManager:
    """Test token management"""
    
    @pytest.fixture
    def token_manager(self):
        """Create token manager"""
        return TokenManager()
    
    def test_store_and_get_token(self, token_manager):
        """Test token storage and retrieval"""
        token_manager.store_token("test_service", "test_token", 3600)
        
        token = token_manager.get_token("test_service")
        assert token == "test_token"
    
    def test_token_expiration(self, token_manager):
        """Test token expiration"""
        token_manager.store_token("test_service", "test_token", -1)  # Expired
        
        token = token_manager.get_token("test_service")
        assert token is None
    
    def test_invalid_service(self, token_manager):
        """Test invalid service token retrieval"""
        token = token_manager.get_token("nonexistent_service")
        assert token is None
    
    def test_auth_header(self, token_manager):
        """Test auth header generation"""
        token_manager.store_token("test_service", "test_token", 3600)
        
        headers = token_manager.get_auth_header("test_service")
        assert headers == {"Authorization": "Bearer test_token"}
    
    def test_api_key_fallback(self, token_manager):
        """Test API key fallback when no token"""
        token_manager.api_keys["test_service"] = "api_key"
        
        headers = token_manager.get_auth_header("test_service")
        assert headers == {"Authorization": "Bearer api_key"}
