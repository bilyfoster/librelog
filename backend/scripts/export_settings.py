"""
Export settings from database to JSON file for backup before UUID migration.

This script exports all settings from the settings table to a JSON file
that can be restored after the database migration to UUID primary keys.

Usage:
    python export_settings.py [--database-url DATABASE_URL] [--output OUTPUT_FILE]
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


async def export_settings(
    db_url: str,
    output_file: str = "settings_backup.json"
) -> None:
    """
    Export all settings from database to JSON file.
    
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
            # Query all settings
            result = await session.execute(
                text("""
                    SELECT id, category, key, value, encrypted, description, 
                           created_at, updated_at
                    FROM settings
                    ORDER BY category, key
                """)
            )
            
            rows = result.fetchall()
            
            # Convert to list of dictionaries
            settings_data = []
            for row in rows:
                setting_dict = {
                    "id": row[0],
                    "category": row[1],
                    "key": row[2],
                    "value": row[3],
                    "encrypted": row[4],
                    "description": row[5],
                    "created_at": row[6].isoformat() if row[6] else None,
                    "updated_at": row[7].isoformat() if row[7] else None,
                }
                settings_data.append(setting_dict)
            
            # Create backup metadata
            backup_data = {
                "exported_at": datetime.now().isoformat(),
                "total_settings": len(settings_data),
                "settings": settings_data
            }
            
            # Write to JSON file
            output_path = Path(output_file)
            with open(output_path, "w", encoding="utf-8") as f:
                json.dump(backup_data, f, indent=2, ensure_ascii=False)
            
            logger.info(
                "Settings exported successfully",
                output_file=str(output_path),
                total_settings=len(settings_data)
            )
            
            print(f"✓ Exported {len(settings_data)} settings to {output_path}")
            
    except Exception as e:
        logger.error("Failed to export settings", error=str(e), exc_info=True)
        print(f"✗ Error exporting settings: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        await engine.dispose()


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(
        description="Export settings from database to JSON file"
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
        default="settings_backup.json",
        help="Output JSON file path (default: settings_backup.json)"
    )
    
    args = parser.parse_args()
    
    asyncio.run(export_settings(args.database_url, args.output))


if __name__ == "__main__":
    main()

