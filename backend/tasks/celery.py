"""
Celery configuration and tasks
"""

import os
from celery import Celery
from backend.integrations.azuracast_sync import azuracast_sync

# Celery configuration
redis_url = os.getenv("REDIS_URL", "redis://localhost:6379/0")

celery = Celery(
    "librelog",
    broker=redis_url,
    backend=redis_url,
    include=[
        "backend.tasks.nightly_sync",
        "backend.tasks.playback_sync",
        "backend.tasks.azuracast_sync_task",
        "backend.tasks.audio_processing",
        "backend.tasks.audio_delivery",
        "backend.tasks.political_archive",
        "backend.tasks.libretime_sync",
    ]
)

celery.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="UTC",
    enable_utc=True,
    task_track_started=True,
    task_time_limit=30 * 60,  # 30 minutes
    task_soft_time_limit=25 * 60,  # 25 minutes
    worker_prefetch_multiplier=1,
    worker_max_tasks_per_child=1000,
    beat_schedule={
        'nightly-sync': {
            'task': 'backend.tasks.nightly_sync.nightly_sync',
            'schedule': 86400.0,  # Run daily at midnight UTC
        },
        'sync-playback-history': {
            'task': 'backend.tasks.playback_sync.sync_playback_history',
            'schedule': 3600.0,  # Run hourly
        },
        'sync-media-library-from-libretime': {
            'task': 'backend.tasks.libretime_sync.sync_media_library_from_libretime',
            'schedule': 3600.0,  # Run hourly
        },
        'cleanup-old-tracks': {
            'task': 'backend.tasks.nightly_sync.cleanup_old_tracks',
            'schedule': 604800.0,  # Run weekly
        },
        'check-copy-expiration': {
            'task': 'backend.tasks.nightly_sync.check_copy_expiration',
            'schedule': 3600.0,  # Run hourly to check for expiring copy
        },
        'retry-failed-deliveries': {
            'task': 'backend.tasks.audio_delivery.retry_failed_deliveries',
            'schedule': 1800.0,  # Run every 30 minutes
        },
        'verify-deliveries': {
            'task': 'backend.tasks.audio_delivery.verify_deliveries',
            'schedule': 3600.0,  # Run hourly
        },
        'archive-political-ads': {
            'task': 'backend.tasks.political_archive.archive_political_ads',
            'schedule': 86400.0,  # Run daily
        },
        'generate-fcc-compliance-report': {
            'task': 'backend.tasks.political_archive.generate_fcc_compliance_report',
            'schedule': 604800.0,  # Run weekly
        },
    },
)


@celery.task
async def start_azuracast_sync():
    """Start the AzuraCast sync service"""
    await azuracast_sync.start_sync()


@celery.task
async def stop_azuracast_sync():
    """Stop the AzuraCast sync service"""
    azuracast_sync.stop_sync()


@celery.task
async def get_listener_stats():
    """Get listener statistics from AzuraCast"""
    return await azuracast_sync.get_listener_stats()


@celery.task
async def get_song_history(limit: int = 10):
    """Get song history from AzuraCast"""
    return await azuracast_sync.get_song_history(limit)
