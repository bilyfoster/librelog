"""
User roles and permissions
"""

from enum import Enum
from typing import List, Set, Dict
from functools import wraps
from fastapi import HTTPException, status
from backend.models.user import User


class Role(str, Enum):
    """User roles"""
    ADMIN = "admin"
    PRODUCER = "producer"
    DJ = "dj"
    SALES = "sales"


class Permission(str, Enum):
    """System permissions"""
    # Track management
    VIEW_TRACKS = "view_tracks"
    CREATE_TRACKS = "create_tracks"
    EDIT_TRACKS = "edit_tracks"
    DELETE_TRACKS = "delete_tracks"
    
    # Campaign management
    VIEW_CAMPAIGNS = "view_campaigns"
    CREATE_CAMPAIGNS = "create_campaigns"
    EDIT_CAMPAIGNS = "edit_campaigns"
    DELETE_CAMPAIGNS = "delete_campaigns"
    
    # Clock templates
    VIEW_CLOCKS = "view_clocks"
    CREATE_CLOCKS = "create_clocks"
    EDIT_CLOCKS = "edit_clocks"
    DELETE_CLOCKS = "delete_clocks"
    
    # Log management
    VIEW_LOGS = "view_logs"
    GENERATE_LOGS = "generate_logs"
    PUBLISH_LOGS = "publish_logs"
    
    # Voice tracking
    VIEW_VOICE = "view_voice"
    UPLOAD_VOICE = "upload_voice"
    DELETE_VOICE = "delete_voice"
    
    # Reports
    VIEW_REPORTS = "view_reports"
    EXPORT_REPORTS = "export_reports"
    
    # User management
    VIEW_USERS = "view_users"
    CREATE_USERS = "create_users"
    EDIT_USERS = "edit_users"
    DELETE_USERS = "delete_users"


# Role-based permissions mapping
ROLE_PERMISSIONS: Dict[Role, Set[Permission]] = {
    Role.ADMIN: {
        # Admin has all permissions
        Permission.VIEW_TRACKS,
        Permission.CREATE_TRACKS,
        Permission.EDIT_TRACKS,
        Permission.DELETE_TRACKS,
        Permission.VIEW_CAMPAIGNS,
        Permission.CREATE_CAMPAIGNS,
        Permission.EDIT_CAMPAIGNS,
        Permission.DELETE_CAMPAIGNS,
        Permission.VIEW_CLOCKS,
        Permission.CREATE_CLOCKS,
        Permission.EDIT_CLOCKS,
        Permission.DELETE_CLOCKS,
        Permission.VIEW_LOGS,
        Permission.GENERATE_LOGS,
        Permission.PUBLISH_LOGS,
        Permission.VIEW_VOICE,
        Permission.UPLOAD_VOICE,
        Permission.DELETE_VOICE,
        Permission.VIEW_REPORTS,
        Permission.EXPORT_REPORTS,
        Permission.VIEW_USERS,
        Permission.CREATE_USERS,
        Permission.EDIT_USERS,
        Permission.DELETE_USERS,
    },
    Role.PRODUCER: {
        # Producer can manage content and schedules
        Permission.VIEW_TRACKS,
        Permission.CREATE_TRACKS,
        Permission.EDIT_TRACKS,
        Permission.VIEW_CAMPAIGNS,
        Permission.CREATE_CAMPAIGNS,
        Permission.EDIT_CAMPAIGNS,
        Permission.VIEW_CLOCKS,
        Permission.CREATE_CLOCKS,
        Permission.EDIT_CLOCKS,
        Permission.VIEW_LOGS,
        Permission.GENERATE_LOGS,
        Permission.PUBLISH_LOGS,
        Permission.VIEW_VOICE,
        Permission.UPLOAD_VOICE,
        Permission.VIEW_REPORTS,
        Permission.EXPORT_REPORTS,
    },
    Role.DJ: {
        # DJ can view and upload content
        Permission.VIEW_TRACKS,
        Permission.VIEW_CAMPAIGNS,
        Permission.VIEW_CLOCKS,
        Permission.VIEW_LOGS,
        Permission.VIEW_VOICE,
        Permission.UPLOAD_VOICE,
        Permission.VIEW_REPORTS,
    },
    Role.SALES: {
        # Sales can manage campaigns
        Permission.VIEW_TRACKS,
        Permission.VIEW_CAMPAIGNS,
        Permission.CREATE_CAMPAIGNS,
        Permission.EDIT_CAMPAIGNS,
        Permission.VIEW_CLOCKS,
        Permission.VIEW_LOGS,
        Permission.VIEW_REPORTS,
        Permission.EXPORT_REPORTS,
    },
}


def get_user_permissions(user: User) -> Set[Permission]:
    """Get permissions for a user based on their role"""
    role = Role(user.role)
    return ROLE_PERMISSIONS.get(role, set())


def has_permission(user: User, permission: Permission) -> bool:
    """Check if user has a specific permission"""
    user_permissions = get_user_permissions(user)
    return permission in user_permissions


def require_permission(permission: Permission):
    """Decorator to require a specific permission"""
    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            # Find current_user in kwargs (from Depends)
            current_user = None
            for key, value in kwargs.items():
                if isinstance(value, User):
                    current_user = value
                    break
            
            # Also check args for User instance
            if not current_user:
                for arg in args:
                    if isinstance(arg, User):
                        current_user = arg
                        break
            
            if not current_user:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Authentication required"
                )
            
            # Check permission
            if not has_permission(current_user, permission):
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail=f"Permission denied: {permission.value} required"
                )
            
            return await func(*args, **kwargs)
        return wrapper
    return decorator
