"""
Production reminder tasks for deadline notifications
"""

from celery import shared_task
import asyncio
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.services.production_notification_service import ProductionNotificationService
import structlog

logger = structlog.get_logger()


@shared_task
def send_production_deadline_reminders():
    """Send reminders for production orders with approaching deadlines"""
    async def _send_reminders():
        async with AsyncSessionLocal() as db:
            try:
                notification_service = ProductionNotificationService(db)
                
                # Send reminders for deadlines 1 day ahead
                await notification_service.notify_deadline_approaching(days_ahead=1)
                
                logger.info("Production deadline reminders sent")
                return {"status": "success", "message": "Deadline reminders sent"}
            except Exception as e:
                logger.error("Failed to send production deadline reminders", error=str(e))
                return {"status": "error", "message": str(e)}
    
    # Run async function in sync context
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    try:
        return loop.run_until_complete(_send_reminders())
    finally:
        loop.close()

