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
    audio_cuts, live_reads, political_compliance, audio_delivery, audio_qc, help, proxy
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
    allowed_hosts=["localhost", "127.0.0.1", "frontend", "api", "log-dev.gayphx.com", "log.gayphx.com", ".gayphx.com"]
)

# Add custom middleware
app.add_middleware(LoggingMiddleware)
app.add_middleware(AuthMiddleware)

# Register health endpoint BEFORE routers with empty prefixes to avoid route conflicts
@app.get("/health")
@app.get("/api/health")  # Keep both for compatibility
async def health_check():
    """Health check endpoint - no auth required"""
    try:
        # Simple health check - no database access needed
        return {"status": "healthy", "service": "librelog-api"}
    except Exception as e:
        logger.error("Health check failed", error=str(e))
        # Still return 200 even if there's an error, so monitoring doesn't think we're down
        return {"status": "degraded", "service": "librelog-api", "error": str(e)}

# Include routers
# NOTE: Traefik strips /api prefix before forwarding, so routes are registered without /api
# The paths will be /auth/login, /setup, etc. when they reach the backend
app.include_router(auth.router, prefix="/auth", tags=["Authentication"])
app.include_router(setup.router, prefix="/setup", tags=["Setup"])
# NOTE: Traefik strips /api prefix, so all routes are registered without /api
app.include_router(tracks.router, prefix="/tracks", tags=["Tracks"])
app.include_router(campaigns.router, prefix="/campaigns", tags=["Campaigns"])
app.include_router(clocks.router, prefix="/clocks", tags=["Clock Templates"])
app.include_router(logs.router, prefix="/logs", tags=["Logs"])
app.include_router(voice_tracks.router, prefix="/voice", tags=["Voice Tracks"])
app.include_router(reports.router, prefix="/reports", tags=["Reports"])
app.include_router(sync.router, prefix="/sync", tags=["Sync"])
app.include_router(activity.router, prefix="/activity", tags=["Activity"])
app.include_router(advertisers.router, prefix="/advertisers", tags=["Advertisers"])
app.include_router(agencies.router, prefix="/agencies", tags=["Agencies"])
app.include_router(sales_reps.router, prefix="/sales-reps", tags=["Sales Reps"])
app.include_router(orders.router, prefix="/orders", tags=["Orders"])
app.include_router(spots.router, prefix="/spots", tags=["Spots"])
app.include_router(dayparts.router, prefix="/dayparts", tags=["Dayparts"])
app.include_router(daypart_categories.router, prefix="", tags=["Daypart Categories"])
app.include_router(rotation_rules.router, prefix="", tags=["Rotation Rules"])
app.include_router(traffic_logs.router, prefix="", tags=["Traffic Logs"])
app.include_router(break_structures.router, prefix="/break-structures", tags=["Break Structures"])
app.include_router(copy.router, prefix="/copy", tags=["Copy"])
app.include_router(copy_assignments.router, prefix="/copy-assignments", tags=["Copy Assignments"])
app.include_router(invoices.router, prefix="/invoices", tags=["Invoices"])
app.include_router(payments.router, prefix="/payments", tags=["Payments"])
app.include_router(makegoods.router, prefix="/makegoods", tags=["Makegoods"])
app.include_router(audit_logs.router, prefix="/admin/audit-logs", tags=["Audit Logs"])
app.include_router(log_revisions.router, prefix="", tags=["Log Revisions"])
app.include_router(inventory.router, prefix="/inventory", tags=["Inventory"])
app.include_router(revenue.router, prefix="/revenue", tags=["Revenue"])
app.include_router(sales_goals.router, prefix="/sales-goals", tags=["Sales Goals"])
app.include_router(webhooks.router, prefix="", tags=["Webhooks"])
app.include_router(notifications.router, prefix="", tags=["Notifications"])
app.include_router(collaboration.router, prefix="", tags=["Collaboration"])
app.include_router(backups.router, prefix="", tags=["Backups"])
app.include_router(settings.router, prefix="", tags=["Settings"])
app.include_router(users.router, prefix="", tags=["Users"])
app.include_router(audio_cuts.router, prefix="", tags=["Audio Cuts"])
app.include_router(live_reads.router, prefix="", tags=["Live Reads"])
app.include_router(political_compliance.router, prefix="", tags=["Political Compliance"])
app.include_router(audio_delivery.router, prefix="", tags=["Audio Delivery"])
app.include_router(audio_qc.router, prefix="", tags=["Audio QC"])
app.include_router(help.router, prefix="/help", tags=["Help"])
app.include_router(proxy.router, prefix="/proxy", tags=["Proxy"])


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler"""
    logger.error("Unhandled exception", exc_info=exc, path=request.url.path)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error"}
    )


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
