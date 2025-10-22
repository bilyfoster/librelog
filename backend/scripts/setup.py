"""
Initial setup and admin user creation
"""

import os
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import AsyncSessionLocal
from backend.models.user import User
from backend.auth.oauth2 import get_password_hash
import structlog

logger = structlog.get_logger()


async def create_initial_admin():
    """Create initial admin user if none exists"""
    async with AsyncSessionLocal() as db:
        # Check if any admin users exist
        result = await db.execute(
            select(User).where(User.role == "admin")
        )
        admin_users = result.scalars().all()
        
        if admin_users:
            logger.info("Admin users already exist, skipping initial setup")
            return
        
        # Create default admin user
        admin_user = User(
            username="admin",
            password_hash=get_password_hash("admin123"),
            role="admin"
        )
        
        db.add(admin_user)
        await db.commit()
        await db.refresh(admin_user)
        
        logger.info("Initial admin user created", username="admin")
        print("=" * 50)
        print("INITIAL ADMIN USER CREATED")
        print("Username: admin")
        print("Password: admin123")
        print("Please change this password after first login!")
        print("=" * 50)


async def check_api_keys():
    """Check if API keys are configured"""
    libretime_key = os.getenv("LIBRETIME_API_KEY")
    azuracast_key = os.getenv("AZURACAST_API_KEY")
    
    missing_keys = []
    if not libretime_key or libretime_key == "changeme":
        missing_keys.append("LIBRETIME_API_KEY")
    if not azuracast_key or azuracast_key == "changeme":
        missing_keys.append("AZURACAST_API_KEY")
    
    if missing_keys:
        print("=" * 50)
        print("API KEYS NOT CONFIGURED")
        print("Please set the following environment variables:")
        for key in missing_keys:
            print(f"- {key}")
        print("=" * 50)
        return False
    
    logger.info("API keys configured")
    return True


async def setup_check():
    """Run initial setup checks"""
    await create_initial_admin()
    await check_api_keys()
