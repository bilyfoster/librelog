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
            # Serialize details dict to JSON string for String field
            details_str = json.dumps(details) if details else None
            
            audit_log = AuditLog(
                user_id=user_id,
                action=action,
                resource_type=resource_type,
                resource_id=resource_id,
                details=details_str,  # String field - JSON serialized
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
    
    @staticmethod
    async def log_security_event(
        db_session,
        user_id: Optional[int],
        event_type: str,
        event_description: str,
        resource_type: Optional[str] = None,
        resource_id: Optional[int] = None,
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None
    ):
        """Log security-related events (failed logins, password changes, permission changes, etc.)"""
        try:
            details = {
                "event_type": event_type,
                "description": event_description,
                "metadata": metadata or {}
            }
            
            await AuditLogger.log_action(
                db_session=db_session,
                user_id=user_id or 0,  # Use 0 for system events
                action=f"SECURITY_{event_type}",
                resource_type=resource_type or "System",
                resource_id=resource_id,
                details=details,
                ip_address=ip_address,
                user_agent=user_agent
            )
        except Exception as e:
            logger.error("Failed to log security event", error=str(e), event_type=event_type)
    
    @staticmethod
    async def log_password_change(
        db_session,
        user_id: int,
        changed_by: Optional[int] = None,
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None
    ):
        """Log password change event"""
        await AuditLogger.log_security_event(
            db_session=db_session,
            user_id=changed_by or user_id,
            event_type="PASSWORD_CHANGE",
            event_description=f"Password changed for user {user_id}",
            resource_type="User",
            resource_id=user_id,
            ip_address=ip_address,
            user_agent=user_agent
        )
    
    @staticmethod
    async def log_permission_change(
        db_session,
        user_id: int,
        changed_by: int,
        permission_type: str,
        old_value: Any,
        new_value: Any,
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None
    ):
        """Log permission/role change event"""
        await AuditLogger.log_security_event(
            db_session=db_session,
            user_id=changed_by,
            event_type="PERMISSION_CHANGE",
            event_description=f"Permission {permission_type} changed for user {user_id}",
            resource_type="User",
            resource_id=user_id,
            ip_address=ip_address,
            user_agent=user_agent,
            metadata={
                "permission_type": permission_type,
                "old_value": str(old_value),
                "new_value": str(new_value)
            }
        )
    
    @staticmethod
    async def log_data_access(
        db_session,
        user_id: int,
        resource_type: str,
        resource_id: Optional[int] = None,
        action: str = "ACCESS",
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None
    ):
        """Log data access events"""
        await AuditLogger.log_action(
            db_session=db_session,
            user_id=user_id,
            action=action,
            resource_type=resource_type,
            resource_id=resource_id,
            details={"access_type": "data_access"},
            ip_address=ip_address,
            user_agent=user_agent
        )
    
    @staticmethod
    async def log_configuration_change(
        db_session,
        user_id: int,
        config_category: str,
        config_key: str,
        old_value: Any,
        new_value: Any,
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None
    ):
        """Log configuration change events"""
        await AuditLogger.log_action(
            db_session=db_session,
            user_id=user_id,
            action="CONFIG_CHANGE",
            resource_type="Configuration",
            resource_id=None,
            details={
                "category": config_category,
                "key": config_key,
                "old_value": str(old_value),
                "new_value": str(new_value)
            },
            ip_address=ip_address,
            user_agent=user_agent
        )
    
    @staticmethod
    async def log_file_operation(
        db_session,
        user_id: int,
        operation: str,  # UPLOAD, DOWNLOAD, DELETE
        file_path: str,
        file_type: Optional[str] = None,
        file_size: Optional[int] = None,
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None
    ):
        """Log file upload/download/delete operations"""
        await AuditLogger.log_action(
            db_session=db_session,
            user_id=user_id,
            action=f"FILE_{operation}",
            resource_type="File",
            resource_id=None,
            details={
                "file_path": file_path,
                "file_type": file_type,
                "file_size": file_size
            },
            ip_address=ip_address,
            user_agent=user_agent
        )


# Global audit logger instance
audit_logger = AuditLogger()
