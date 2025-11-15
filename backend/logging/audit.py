"""
Audit logging system for LibreLog
"""

import json
from datetime import datetime
from typing import Optional, Dict, Any
from backend.models.audit_log import AuditLog
import structlog

logger = structlog.get_logger()


class AuditLogger:
    """Audit logging service"""
    
    @staticmethod
    async def log_action(
        db_session,
        user_id: int,
        action: str,
        resource_type: str,
        resource_id: Optional[int] = None,
        details: Optional[Dict[str, Any]] = None,
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None
    ):
        """Log a user action"""
        try:
            audit_log = AuditLog(
                user_id=user_id,
                action=action,
                resource_type=resource_type,
                resource_id=resource_id,
                changes=details,  # JSONB field
                ip_address=ip_address,
                user_agent=user_agent
            )
            
            db_session.add(audit_log)
            await db_session.commit()
            
            logger.info(
                "Audit log created",
                user_id=user_id,
                action=action,
                resource_type=resource_type,
                resource_id=resource_id
            )
            
        except Exception as e:
            logger.error("Failed to create audit log", error=str(e))
            await db_session.rollback()
    
    @staticmethod
    def log_login(user_id: int, ip_address: str, user_agent: str):
        """Log user login"""
        logger.info(
            "User login",
            user_id=user_id,
            ip_address=ip_address,
            user_agent=user_agent
        )
    
    @staticmethod
    def log_logout(user_id: int, ip_address: str):
        """Log user logout"""
        logger.info(
            "User logout",
            user_id=user_id,
            ip_address=ip_address
        )
    
    @staticmethod
    def log_api_access(user_id: int, endpoint: str, method: str, ip_address: str):
        """Log API access"""
        logger.info(
            "API access",
            user_id=user_id,
            endpoint=endpoint,
            method=method,
            ip_address=ip_address
        )


# Global audit logger instance
audit_logger = AuditLogger()
