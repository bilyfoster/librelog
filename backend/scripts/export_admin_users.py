"""
Export admin users from database to JSON file for backup before UUID migration.

This script exports all users with role='admin' to a JSON file that can be
restored after the database migration to UUID primary keys.

Usage:
    python export_admin_users.py [--database-url DATABASE_URL] [--output OUTPUT_FILE]
"""
import asyncio
import json
import os
import sys
import argparse
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy import select, text
import structlog

logger = structlog.get_logger()


async def export_admin_users(
    db_url: str,
    output_file: str = "admin_users_backup.json"
) -> None:
    """
    Export all admin users from database to JSON file.
    
    Args:
        db_url: Database connection URL
        output_file: Path to output JSON file
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
        async with AsyncSessionLocal() as session:
            # Query all admin users
            result = await session.execute(
                text("""
                    SELECT id, username, password_hash, role, permissions,
                           created_at, last_login, last_activity
                    FROM users
                    WHERE role = 'admin'
                    ORDER BY username
                """)
            )
            
            rows = result.fetchall()
            
            # Convert to list of dictionaries
            users_data = []
            for row in rows:
                user_dict = {
                    "id": row[0],
                    "username": row[1],
                    "password_hash": row[2],
                    "role": row[3],
                    "permissions": row[4] if row[4] else None,
                    "created_at": row[5].isoformat() if row[5] else None,
                    "last_login": row[6].isoformat() if row[6] else None,
                    "last_activity": row[7].isoformat() if row[7] else None,
                }
                users_data.append(user_dict)
            
            # Create backup metadata
            backup_data = {
                "exported_at": datetime.now().isoformat(),
                "total_users": len(users_data),
                "users": users_data
            }
            
            # Write to JSON file
            output_path = Path(output_file)
            with open(output_path, "w", encoding="utf-8") as f:
                json.dump(backup_data, f, indent=2, ensure_ascii=False)
            
            logger.info(
                "Admin users exported successfully",
                output_file=str(output_path),
                total_users=len(users_data)
            )
            
            print(f"✓ Exported {len(users_data)} admin users to {output_path}")
            
            if len(users_data) == 0:
                print("⚠ Warning: No admin users found in database", file=sys.stderr)
            
    except Exception as e:
        logger.error("Failed to export admin users", error=str(e), exc_info=True)
        print(f"✗ Error exporting admin users: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        await engine.dispose()


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(
        description="Export admin users from database to JSON file"
    )
    parser.add_argument(
        "--database-url",
        type=str,
        default=os.getenv("POSTGRES_URI", "postgresql://librelog:password@db:5432/librelog"),
        help="Database connection URL (default: from POSTGRES_URI env var)"
    )
    parser.add_argument(
        "--output",
        type=str,
        default="admin_users_backup.json",
        help="Output JSON file path (default: admin_users_backup.json)"
    )
    
    args = parser.parse_args()
    
    asyncio.run(export_admin_users(args.database_url, args.output))


if __name__ == "__main__":
    main()

