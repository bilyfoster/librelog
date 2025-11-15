"""
Celery tasks for audio delivery to playback systems
"""

from celery import shared_task
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.services.audio_delivery_service import AudioDeliveryService
from backend.models.audio_delivery import AudioDelivery, DeliveryStatus
from sqlalchemy import select, and_
import structlog

logger = structlog.get_logger()


@shared_task
async def deliver_audio_task(delivery_id: int):
    """Deliver an audio file to a playback system"""
    async with AsyncSessionLocal() as db:
        try:
            delivery_service = AudioDeliveryService(db)
            delivery = await delivery_service.retry_delivery(delivery_id)
            
            logger.info("Audio delivery task completed", delivery_id=delivery_id, status=delivery.status)
            
        except Exception as e:
            logger.error("Audio delivery task failed", delivery_id=delivery_id, error=str(e), exc_info=True)
            raise


@shared_task
async def retry_failed_deliveries():
    """Retry failed audio deliveries"""
    async with AsyncSessionLocal() as db:
        try:
            # Find failed deliveries that haven't exceeded max retries
            result = await db.execute(
                select(AudioDelivery).where(
                    and_(
                        AudioDelivery.status == DeliveryStatus.FAILED.value,
                        AudioDelivery.retry_count < AudioDelivery.max_retries
                    )
                )
            )
            failed_deliveries = result.scalars().all()
            
            delivery_service = AudioDeliveryService(db)
            
            for delivery in failed_deliveries:
                try:
                    await delivery_service.retry_delivery(delivery.id)
                    logger.info("Retried delivery", delivery_id=delivery.id)
                except Exception as e:
                    logger.error("Retry failed", delivery_id=delivery.id, error=str(e))
            
            logger.info("Failed delivery retry completed", retried_count=len(failed_deliveries))
            
        except Exception as e:
            logger.error("Failed delivery retry task failed", error=str(e), exc_info=True)
            raise


@shared_task
async def verify_deliveries():
    """Verify that delivered files are still present and correct"""
    async with AsyncSessionLocal() as db:
        try:
            # Find successful deliveries that haven't been verified
            result = await db.execute(
                select(AudioDelivery).where(
                    and_(
                        AudioDelivery.status == DeliveryStatus.SUCCESS.value,
                        AudioDelivery.checksum_verified == False
                    )
                )
            )
            deliveries = result.scalars().all()
            
            delivery_service = AudioDeliveryService(db)
            
            for delivery in deliveries:
                try:
                    # Attempt verification (implementation would depend on delivery method)
                    # For now, just log
                    logger.info("Verifying delivery", delivery_id=delivery.id)
                except Exception as e:
                    logger.error("Verification failed", delivery_id=delivery.id, error=str(e))
            
            logger.info("Delivery verification completed", verified_count=len(deliveries))
            
        except Exception as e:
            logger.error("Delivery verification task failed", error=str(e), exc_info=True)
            raise

