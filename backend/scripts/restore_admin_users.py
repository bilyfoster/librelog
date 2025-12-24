"""
Restore admin users from JSON backup file to database after UUID migration.

This script reads admin users from a JSON backup file and restores them to the
database with new UUID primary keys. The original integer IDs are not preserved,
but password hashes are preserved so users can log in with their original passwords.

Usage:
    python restore_admin_users.py [--database-url DATABASE_URL] [--input INPUT_FILE]
"""
import asyncio
import json
import os
import sys
import argparse
from pathlib import Path
from typing import List, Dict, Any
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy import text
import structlog

logger = structlog.get_logger()


async def restore_admin_users(
    db_url: str,
    input_file: str = "admin_users_backup.json"
) -> None:
    """
    Restore admin users from JSON file to database with new UUID primary keys.
    
    Args:
        db_url: Database connection URL
        input_file: Path to input JSON file
    """
    # Convert to async URL if needed
    if db_url.startswith("postgresql://"):
        db_url = db_url.replace("postgresql://", "postgresql+asyncpg://", 1)
    
    # Create async engine
    engine = create_async_engine(
        db_url,
        echo=False,
        pool_pre_ping=True,
    )
    
    # Create session factory
    AsyncSessionLocal = async_sessionmaker(
        engine,
        class_=AsyncSession,
        expire_on_commit=False
    )
    
    try:
        # Read JSON file
        input_path = Path(input_file)
        if not input_path.exists():
            raise FileNotFoundError(f"Backup file not found: {input_path}")
        
        with open(input_path, "r", encoding="utf-8") as f:
            backup_data = json.load(f)
        
        users = backup_data.get("users", [])
        
        if not users:
            print("⚠ Warning: No admin users found in backup file", file=sys.stderr)
            return
        
        async with AsyncSessionLocal() as session:
            restored_count = 0
            skipped_count = 0
            
            for user in users:
                try:
                    # Check if user already exists (by username)
                    check_result = await session.execute(
                        text("SELECT id FROM users WHERE username = :username"),
                        {"username": user["username"]}
                    )
                    existing = check_result.fetchone()
                    
                    if existing:
                        logger.info(
                            "User already exists, skipping",
                            username=user["username"]
                        )
                        skipped_count += 1
                        continue
                    
                    # Insert user with new UUID primary key
                    # PostgreSQL will generate UUID automatically via gen_random_uuid()
                    await session.execute(
                        text("""
                            INSERT INTO users (username, password_hash, role, permissions, 
                                             created_at, last_login, last_activity)
                            VALUES (:username, :password_hash, :role, :permissions,
                                    COALESCE(:created_at::timestamp with time zone, NOW()),
                                    :last_login::timestamp with time zone,
                                    :last_activity::timestamp with time zone)
                        """),
                        {
                            "username": user["username"],
                            "password_hash": user["password_hash"],
                            "role": user["role"],
                            "permissions": json.dumps(user["permissions"]) if user.get("permissions") else None,
                            "created_at": user.get("created_at"),
                            "last_login": user.get("last_login"),
                            "last_activity": user.get("last_activity"),
                        }
                    )
                    restored_count += 1
                    logger.info("Restored admin user", username=user["username"])
                    
                except Exception as e:
                    logger.error(
                        "Failed to restore user",
                        username=user.get("username"),
                        error=str(e),
                        exc_info=True
                    )
                    skipped_count += 1
                    continue
            
            await session.commit()
            
            logger.info(
                "Admin users restored successfully",
                restored=restored_count,
                skipped=skipped_count,
                total=len(users)
            )
            
            print(f"✓ Restored {restored_count} admin users (skipped {skipped_count})")
            print("✓ Users can log in with their original passwords")
            
    except Exception as e:
        logger.error("Failed to restore admin users", error=str(e), exc_info=True)
        print(f"✗ Error restoring admin users: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        await engine.dispose()


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(
        description="Restore admin users from JSON backup file to database"
    )
    parser.add_argument(
        "--database-url",
        type=str,
        default=os.getenv("POSTGRES_URI", "postgresql://librelog:password@db:5432/librelog"),
        help="Database connection URL (default: from POSTGRES_URI env var)"
    )
    parser.add_argument(
        "--input",
        type=str,
        default="admin_users_backup.json",
        help="Input JSON file path (default: admin_users_backup.json)"
    )
    
    args = parser.parse_args()
    
    asyncio.run(restore_admin_users(args.database_url, args.input))


if __name__ == "__main__":
    main()

