"""
WebSocket router for real-time collaboration
"""

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends, Query, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from backend.database import get_db
from backend.models.user import User
from backend.models.daily_log import DailyLog
from backend.routers.auth import get_current_user
from backend.services.collaboration_service import manager, CollaborationService
import structlog

logger = structlog.get_logger()

router = APIRouter(prefix="/collaboration", tags=["collaboration"])


@router.websocket("/ws/{log_id}")
async def websocket_endpoint(
    websocket: WebSocket,
    log_id: int,
    token: str = Query(...)
):
    """
    WebSocket endpoint for real-time collaboration on log editing
    
    Query params:
        - token: JWT authentication token
        - log_id: ID of the log being edited (from path)
    """
    # Verify user authentication
    try:
        from backend.auth.token_manager import verify_token
        from backend.database import AsyncSessionLocal
        
        payload = verify_token(token)
        user_id = payload.get("sub")
        
        if not user_id:
            await websocket.close(code=1008, reason="Invalid token")
            return
        
        # Get database session
        async with AsyncSessionLocal() as db:
            # Get user from database
            result = await db.execute(select(User).where(User.id == int(user_id)))
            user = result.scalar_one_or_none()
            
            if not user:
                await websocket.close(code=1008, reason="User not found")
                return
            
            # Verify log exists
            log_result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
            log = log_result.scalar_one_or_none()
            
            if not log:
                await websocket.close(code=1008, reason="Log not found")
                return
        
        # Connect user to collaboration session
        await manager.connect(websocket, log_id, user.id, user.username)
        
        logger.info(
            "websocket_connected",
            log_id=log_id,
            user_id=user.id,
            username=user.username
        )
        
        try:
            while True:
                # Receive message from client
                data = await websocket.receive_json()
                message_type = data.get("type")
                
                if message_type == "cursor_update":
                    await CollaborationService.handle_cursor_update(
                        log_id,
                        user.id,
                        user.username,
                        data.get("cursor", {})
                    )
                
                elif message_type == "spot_update":
                    await CollaborationService.handle_spot_update(
                        log_id,
                        user.id,
                        user.username,
                        data.get("spot", {})
                    )
                
                elif message_type == "log_lock":
                    await CollaborationService.handle_log_lock(
                        log_id,
                        user.id,
                        user.username,
                        data.get("locked", False)
                    )
                
                elif message_type == "typing":
                    await CollaborationService.handle_typing(
                        log_id,
                        user.id,
                        user.username,
                        data.get("field", "")
                    )
                
                elif message_type == "ping":
                    # Heartbeat to keep connection alive
                    await websocket.send_json({"type": "pong"})
                
                else:
                    logger.warning(
                        "unknown_message_type",
                        message_type=message_type,
                        log_id=log_id,
                        user_id=user.id
                    )
        
        except WebSocketDisconnect:
            manager.disconnect(websocket)
            logger.info(
                "websocket_disconnected",
                log_id=log_id,
                user_id=user.id
            )
        
        except Exception as e:
            logger.error(
                "websocket_error",
                log_id=log_id,
                user_id=user.id,
                error=str(e),
                exc_info=True
            )
            manager.disconnect(websocket)
            await websocket.close(code=1011, reason="Internal server error")
    
    except Exception as e:
        logger.error(
            "websocket_auth_error",
            error=str(e),
            exc_info=True
        )
        await websocket.close(code=1008, reason="Authentication failed")


@router.get("/users/{log_id}")
async def get_log_users(
    log_id: int,
    current_user: User = Depends(get_current_user)
):
    """Get list of users currently editing a log"""
    users = manager.get_log_users(log_id)
    return {"log_id": log_id, "users": users}


@router.get("/comments")
async def get_comments(
    log_id: int = Query(...),
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get comments for a log"""
    from backend.models.collaboration_comment import CollaborationComment
    from sqlalchemy import select
    from sqlalchemy.orm import selectinload
    
    query = select(CollaborationComment).options(
        selectinload(CollaborationComment.user)
    ).where(
        CollaborationComment.log_id == log_id
    ).order_by(
        CollaborationComment.created_at.desc()
    ).offset(skip).limit(limit)
    
    result = await db.execute(query)
    comments = result.scalars().all()
    
    return [
        {
            "id": c.id,
            "log_id": c.log_id,
            "user_id": c.user_id,
            "username": c.user.username if c.user else None,
            "comment": c.comment,
            "created_at": c.created_at.isoformat() if c.created_at else None,
        }
        for c in comments
    ]


@router.post("/comments")
async def create_comment(
    log_id: int = Query(...),
    comment: str = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a comment on a log"""
    from backend.models.collaboration_comment import CollaborationComment
    from datetime import datetime, timezone
    
    # Verify log exists
    log_result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    if not log_result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="Log not found")
    
    new_comment = CollaborationComment(
        log_id=log_id,
        user_id=current_user.id,
        comment=comment,
        created_at=datetime.now(timezone.utc)
    )
    
    db.add(new_comment)
    await db.commit()
    await db.refresh(new_comment)
    
    return {
        "id": new_comment.id,
        "log_id": new_comment.log_id,
        "user_id": new_comment.user_id,
        "username": current_user.username,
        "comment": new_comment.comment,
        "created_at": new_comment.created_at.isoformat() if new_comment.created_at else None,
    }

