"""
Collaboration service for real-time multi-user editing
"""

from typing import Dict, Set, Optional
from datetime import datetime
import json
import structlog
from fastapi import WebSocket, WebSocketDisconnect
from sqlalchemy.ext.asyncio import AsyncSession

logger = structlog.get_logger()


class ConnectionManager:
    """Manages WebSocket connections for real-time collaboration"""

    def __init__(self):
        # Map of log_id -> Set of WebSocket connections
        self.active_connections: Dict[int, Set[WebSocket]] = {}
        # Map of WebSocket -> (user_id, username, log_id)
        self.connection_info: Dict[WebSocket, tuple] = {}

    async def connect(self, websocket: WebSocket, log_id: int, user_id: int, username: str):
        """Connect a user to a log editing session"""
        await websocket.accept()
        
        if log_id not in self.active_connections:
            self.active_connections[log_id] = set()
        
        self.active_connections[log_id].add(websocket)
        self.connection_info[websocket] = (user_id, username, log_id)
        
        # Notify other users that someone joined
        await self.broadcast_to_log(
            log_id,
            {
                "type": "user_joined",
                "user_id": user_id,
                "username": username,
                "timestamp": datetime.utcnow().isoformat(),
            },
            exclude=websocket
        )
        
        # Send current users list to the new user
        current_users = [
            {"user_id": uid, "username": uname}
            for uid, uname, lid in self.connection_info.values()
            if lid == log_id
        ]
        await websocket.send_json({
            "type": "users_list",
            "users": current_users,
        })
        
        logger.info(
            "user_connected",
            log_id=log_id,
            user_id=user_id,
            username=username,
            total_connections=len(self.active_connections[log_id])
        )

    def disconnect(self, websocket: WebSocket):
        """Disconnect a user from a log editing session"""
        if websocket not in self.connection_info:
            return
        
        user_id, username, log_id = self.connection_info[websocket]
        
        if log_id in self.active_connections:
            self.active_connections[log_id].discard(websocket)
            if not self.active_connections[log_id]:
                del self.active_connections[log_id]
        
        del self.connection_info[websocket]
        
        # Notify other users that someone left
        self.broadcast_to_log_sync(
            log_id,
            {
                "type": "user_left",
                "user_id": user_id,
                "username": username,
                "timestamp": datetime.utcnow().isoformat(),
            },
            exclude=websocket
        )
        
        logger.info(
            "user_disconnected",
            log_id=log_id,
            user_id=user_id,
            username=username
        )

    async def broadcast_to_log(
        self,
        log_id: int,
        message: dict,
        exclude: Optional[WebSocket] = None
    ):
        """Broadcast a message to all users editing a specific log"""
        if log_id not in self.active_connections:
            return
        
        disconnected = set()
        for connection in self.active_connections[log_id]:
            if connection == exclude:
                continue
            
            try:
                await connection.send_json(message)
            except Exception as e:
                logger.warning(
                    "broadcast_failed",
                    log_id=log_id,
                    error=str(e),
                    exc_info=True
                )
                disconnected.add(connection)
        
        # Clean up disconnected connections
        for conn in disconnected:
            self.disconnect(conn)

    def broadcast_to_log_sync(
        self,
        log_id: int,
        message: dict,
        exclude: Optional[WebSocket] = None
    ):
        """Synchronous version for use in disconnect (fire and forget)"""
        import asyncio
        if log_id in self.active_connections:
            asyncio.create_task(self.broadcast_to_log(log_id, message, exclude))

    async def send_personal_message(self, websocket: WebSocket, message: dict):
        """Send a message to a specific connection"""
        try:
            await websocket.send_json(message)
        except Exception as e:
            logger.warning("personal_message_failed", error=str(e))
            self.disconnect(websocket)

    def get_log_users(self, log_id: int) -> list:
        """Get list of users currently editing a log"""
        return [
            {"user_id": uid, "username": uname}
            for uid, uname, lid in self.connection_info.values()
            if lid == log_id
        ]


# Global connection manager instance
manager = ConnectionManager()


class CollaborationService:
    """Service for handling collaboration events"""

    @staticmethod
    async def handle_cursor_update(
        log_id: int,
        user_id: int,
        username: str,
        cursor_data: dict
    ):
        """Handle cursor position updates"""
        await manager.broadcast_to_log(
            log_id,
            {
                "type": "cursor_update",
                "user_id": user_id,
                "username": username,
                "cursor": cursor_data,
                "timestamp": datetime.utcnow().isoformat(),
            }
        )

    @staticmethod
    async def handle_spot_update(
        log_id: int,
        user_id: int,
        username: str,
        spot_data: dict
    ):
        """Handle spot updates (create, update, delete)"""
        await manager.broadcast_to_log(
            log_id,
            {
                "type": "spot_update",
                "user_id": user_id,
                "username": username,
                "spot": spot_data,
                "timestamp": datetime.utcnow().isoformat(),
            }
        )

    @staticmethod
    async def handle_log_lock(
        log_id: int,
        user_id: int,
        username: str,
        locked: bool
    ):
        """Handle log lock/unlock events"""
        await manager.broadcast_to_log(
            log_id,
            {
                "type": "log_lock",
                "user_id": user_id,
                "username": username,
                "locked": locked,
                "timestamp": datetime.utcnow().isoformat(),
            }
        )

    @staticmethod
    async def handle_typing(
        log_id: int,
        user_id: int,
        username: str,
        field: str
    ):
        """Handle typing indicators"""
        await manager.broadcast_to_log(
            log_id,
            {
                "type": "typing",
                "user_id": user_id,
                "username": username,
                "field": field,
                "timestamp": datetime.utcnow().isoformat(),
            }
        )

