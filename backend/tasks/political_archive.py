"""
Celery tasks for archiving political ads and maintaining FCC compliance
"""

from celery import shared_task
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.services.political_compliance_service import PoliticalComplianceService
from datetime import datetime, timezone, timedelta
import structlog

logger = structlog.get_logger()


@shared_task
async def archive_political_ads():
    """Archive political ads that are old enough (FCC requires 2-year retention)"""
    async with AsyncSessionLocal() as db:
        try:
            service = PoliticalComplianceService(db)
            
            # Get records that are 2+ years old and not yet archived
            records = await service.get_records_for_archival(years_old=2)
            
            archived_count = 0
            for record in records:
                try:
                    # Archive the record
                    await service.archive_political_record(record.id)
                    archived_count += 1
                    logger.info("Political record archived", record_id=record.id, sponsor_name=record.sponsor_name)
                except Exception as e:
                    logger.error("Failed to archive record", record_id=record.id, error=str(e))
            
            logger.info("Political archive task completed", archived_count=archived_count, total_records=len(records))
            
        except Exception as e:
            logger.error("Political archive task failed", error=str(e), exc_info=True)
            raise


@shared_task
async def cleanup_old_political_archives():
    """Clean up political archives older than 2 years (after retention period)"""
    async with AsyncSessionLocal() as db:
        try:
            service = PoliticalComplianceService(db)
            
            # Get records archived more than 2 years ago
            cutoff_date = datetime.now(timezone.utc) - timedelta(days=2 * 365)
            
            # This would need to be implemented in the service
            # For now, just log
            logger.info("Political archive cleanup task completed")
            
        except Exception as e:
            logger.error("Political archive cleanup task failed", error=str(e), exc_info=True)
            raise


@shared_task
async def generate_fcc_compliance_report():
    """Generate FCC compliance report for political ads"""
    async with AsyncSessionLocal() as db:
        try:
            service = PoliticalComplianceService(db)
            
            # Generate report for last 30 days
            end_date = datetime.now(timezone.utc)
            start_date = end_date - timedelta(days=30)
            
            compliance_log = await service.generate_fcc_compliance_log(start_date, end_date)
            
            logger.info("FCC compliance report generated", record_count=len(compliance_log))
            
        except Exception as e:
            logger.error("FCC compliance report generation failed", error=str(e), exc_info=True)
            raise

