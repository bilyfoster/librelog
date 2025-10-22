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
