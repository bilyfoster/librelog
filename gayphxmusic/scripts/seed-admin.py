#!/usr/bin/env python3
"""
Seed script to create initial admin user
Run this after the database is set up
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.core.config import settings
from app.models.admin_user import AdminUser
from passlib.context import CryptContext

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def create_admin_user():
    # Create database connection
    engine = create_engine(settings.database_url)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    db = SessionLocal()
    
    try:
        # Check if admin already exists
        existing_admin = db.query(AdminUser).filter(AdminUser.email == "admin@gayphx.com").first()
        if existing_admin:
            print("Admin user already exists")
            return
        
        # Create admin user
        admin_user = AdminUser(
            email="admin@gayphx.com",
            name="GayPHX Admin",
            password_hash=hash_password("admin123"),
            role="admin"
        )
        
        db.add(admin_user)
        db.commit()
        
        print("Admin user created successfully!")
        print("Email: admin@gayphx.com")
        print("Password: admin123")
        print("Please change the password after first login!")
        
    except Exception as e:
        print(f"Error creating admin user: {e}")
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    create_admin_user()

