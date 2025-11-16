"""
LibreLog FastAPI Application
Main entry point for the GayPHX Radio Traffic System
"""

import os
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from fastapi.responses import JSONResponse
import structlog

from backend.database import engine, Base
from backend.routers import (
    auth, tracks, campaigns, clocks, logs, voice_tracks, reports, setup, sync, activity,
    advertisers, agencies, sales_reps, orders, spots, dayparts, daypart_categories, rotation_rules, traffic_logs, break_structures, copy, copy_assignments,
    invoices, payments, makegoods, audit_logs, log_revisions, inventory, revenue, sales_goals,
    webhooks, notifications, collaboration, backups, settings, users,
    audio_cuts, live_reads, political_compliance, audio_delivery, audio_qc, help
)
from backend.middleware import AuthMiddleware, LoggingMiddleware
from backend.models import user, track, campaign, clock_template, daily_log, voice_track, playback_history
from backend.tasks.celery import celery

# Configure structured logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.processors.JSONRenderer()
    ],
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    wrapper_class=structlog.stdlib.BoundLogger,
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events"""
    # Startup
    logger.info("Starting LibreLog API server")
    
    # Create database tables
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    # Start background tasks
    # Note: Celery beat scheduler should be started separately as a process:
    # celery -A backend.tasks.celery beat --loglevel=info
    # Or add a beat service to docker-compose.yml
    # Beat schedule is configured in backend/tasks/celery.py
    logger.info("Celery beat schedule configured. Start beat with: celery -A backend.tasks.celery beat")
    
    # Run initial setup
    from backend.scripts.setup import setup_check
    await setup_check()
    
    logger.info("Database tables created")
    yield
    
    # Shutdown
    logger.info("Shutting down LibreLog API server")


# Create FastAPI application
app = FastAPI(
    title="LibreLog API",
    description="REST API for LibreLog — the GayPHX Radio traffic and automation manager",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",
        "http://frontend:3000",
        "https://log-dev.gayphx.com",
        "http://log-dev.gayphx.com",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Add trusted host middleware
app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=["localhost", "127.0.0.1", "frontend", "api", "log-dev.gayphx.com"]
)

# Add custom middleware
app.add_middleware(LoggingMiddleware)
app.add_middleware(AuthMiddleware)

# Include routers
app.include_router(auth.router, prefix="/api/auth", tags=["Authentication"])
app.include_router(setup.router, prefix="/api/setup", tags=["Setup"])
app.include_router(tracks.router, prefix="/api/tracks", tags=["Tracks"])
app.include_router(campaigns.router, prefix="/api/campaigns", tags=["Campaigns"])
app.include_router(clocks.router, prefix="/api/clocks", tags=["Clock Templates"])
app.include_router(logs.router, prefix="/api/logs", tags=["Logs"])
app.include_router(voice_tracks.router, prefix="/api/voice", tags=["Voice Tracks"])
app.include_router(reports.router, prefix="/api/reports", tags=["Reports"])
app.include_router(sync.router, prefix="/api/sync", tags=["Sync"])
app.include_router(activity.router, prefix="/api/activity", tags=["Activity"])
app.include_router(advertisers.router, prefix="/api/advertisers", tags=["Advertisers"])
app.include_router(agencies.router, prefix="/api/agencies", tags=["Agencies"])
app.include_router(sales_reps.router, prefix="/api/sales-reps", tags=["Sales Reps"])
app.include_router(orders.router, prefix="/api/orders", tags=["Orders"])
app.include_router(spots.router, prefix="/api/spots", tags=["Spots"])
app.include_router(dayparts.router, prefix="/api/dayparts", tags=["Dayparts"])
app.include_router(daypart_categories.router, prefix="/api", tags=["Daypart Categories"])
app.include_router(rotation_rules.router, prefix="/api", tags=["Rotation Rules"])
app.include_router(traffic_logs.router, prefix="/api", tags=["Traffic Logs"])
app.include_router(break_structures.router, prefix="/api/break-structures", tags=["Break Structures"])
app.include_router(copy.router, prefix="/api/copy", tags=["Copy"])
app.include_router(copy_assignments.router, prefix="/api/copy-assignments", tags=["Copy Assignments"])
app.include_router(invoices.router, prefix="/api/invoices", tags=["Invoices"])
app.include_router(payments.router, prefix="/api/payments", tags=["Payments"])
app.include_router(makegoods.router, prefix="/api/makegoods", tags=["Makegoods"])
app.include_router(audit_logs.router, prefix="/api/audit-logs", tags=["Audit Logs"])
app.include_router(log_revisions.router, prefix="/api", tags=["Log Revisions"])
app.include_router(inventory.router, prefix="/api/inventory", tags=["Inventory"])
app.include_router(revenue.router, prefix="/api/revenue", tags=["Revenue"])
app.include_router(sales_goals.router, prefix="/api/sales-goals", tags=["Sales Goals"])
app.include_router(webhooks.router, prefix="/api", tags=["Webhooks"])
app.include_router(notifications.router, prefix="/api", tags=["Notifications"])
app.include_router(collaboration.router, prefix="/api", tags=["Collaboration"])
app.include_router(backups.router, prefix="/api", tags=["Backups"])
app.include_router(settings.router, prefix="/api", tags=["Settings"])
app.include_router(users.router, prefix="/api", tags=["Users"])
app.include_router(audio_cuts.router, prefix="/api", tags=["Audio Cuts"])
app.include_router(live_reads.router, prefix="/api", tags=["Live Reads"])
app.include_router(political_compliance.router, prefix="/api", tags=["Political Compliance"])
app.include_router(audio_delivery.router, prefix="/api", tags=["Audio Delivery"])
app.include_router(audio_qc.router, prefix="/api", tags=["Audio QC"])
app.include_router(help.router, prefix="/api/help", tags=["Help"])


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler"""
    logger.error("Unhandled exception", exc_info=exc, path=request.url.path)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error"}
    )


@app.get("/api/health")
async def health_check():
    """Health check endpoint - no auth required"""
    try:
        # Simple health check - no database access needed
        return {"status": "healthy", "service": "librelog-api"}
    except Exception as e:
        logger.error("Health check failed", error=str(e))
        # Still return 200 even if there's an error, so monitoring doesn't think we're down
        return {"status": "degraded", "service": "librelog-api", "error": str(e)}


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "LibreLog API",
        "version": "1.0.0",
        "docs": "/docs"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )
