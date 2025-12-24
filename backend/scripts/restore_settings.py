"""
Restore settings from JSON backup file to database after UUID migration.

This script reads settings from a JSON backup file and restores them to the
database with new UUID primary keys. The original integer IDs are not preserved.

Usage:
    python restore_settings.py [--database-url DATABASE_URL] [--input INPUT_FILE]
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


async def restore_settings(
    db_url: str,
    input_file: str = "settings_backup.json"
) -> None:
    """
    Restore settings from JSON file to database with new UUID primary keys.
    
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
        
        settings = backup_data.get("settings", [])
        
        if not settings:
            print("⚠ Warning: No settings found in backup file", file=sys.stderr)
            return
        
        async with AsyncSessionLocal() as session:
            restored_count = 0
            skipped_count = 0
            
            for setting in settings:
                try:
                    # Insert setting with new UUID primary key
                    # PostgreSQL will generate UUID automatically via gen_random_uuid()
                    await session.execute(
                        text("""
                            INSERT INTO settings (category, key, value, encrypted, description, created_at, updated_at)
                            VALUES (:category, :key, :value, :encrypted, :description, 
                                    COALESCE(:created_at::timestamp with time zone, NOW()),
                                    COALESCE(:updated_at::timestamp with time zone, NOW()))
                            ON CONFLICT (category, key) DO UPDATE SET
                                value = EXCLUDED.value,
                                encrypted = EXCLUDED.encrypted,
                                description = EXCLUDED.description,
                                updated_at = EXCLUDED.updated_at
                        """),
                        {
                            "category": setting["category"],
                            "key": setting["key"],
                            "value": setting["value"],
                            "encrypted": setting["encrypted"],
                            "description": setting.get("description"),
                            "created_at": setting.get("created_at"),
                            "updated_at": setting.get("updated_at"),
                        }
                    )
                    restored_count += 1
                except Exception as e:
                    logger.warn(
                        "Failed to restore setting",
                        category=setting.get("category"),
                        key=setting.get("key"),
                        error=str(e)
                    )
                    skipped_count += 1
                    continue
            
            await session.commit()
            
            logger.info(
                "Settings restored successfully",
                restored=restored_count,
                skipped=skipped_count,
                total=len(settings)
            )
            
            print(f"✓ Restored {restored_count} settings (skipped {skipped_count})")
            
    except Exception as e:
        logger.error("Failed to restore settings", error=str(e), exc_info=True)
        print(f"✗ Error restoring settings: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        await engine.dispose()


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(
        description="Restore settings from JSON backup file to database"
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
        default="settings_backup.json",
        help="Input JSON file path (default: settings_backup.json)"
    )
    
    args = parser.parse_args()
    
    asyncio.run(restore_settings(args.database_url, args.input))


if __name__ == "__main__":
    main()

