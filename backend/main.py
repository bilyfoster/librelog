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
from backend.routers import auth, tracks, campaigns, clocks, logs, voice_tracks, reports, setup
from backend.middleware import AuthMiddleware, LoggingMiddleware
from backend.models import user, track, campaign, clock_template, daily_log, voice_track, playback_history
from backend.logging.audit import AuditLog
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
    # TODO: Start Celery beat scheduler for periodic tasks
    
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
    allow_origins=["http://localhost:3000", "http://frontend:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Add trusted host middleware
app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=["localhost", "127.0.0.1", "frontend", "api"]
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
    """Health check endpoint"""
    return {"status": "healthy", "service": "librelog-api"}


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
