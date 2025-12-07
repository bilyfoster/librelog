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

from backend.database import engine, Base, get_db
from sqlalchemy.ext.asyncio import AsyncSession
from fastapi import Depends
from backend.routers import (
    auth, tracks, campaigns, clocks, logs, voice_tracks, reports, setup, sync, activity,
    advertisers, agencies, sales_reps, sales_teams, sales_offices, sales_regions, stations, clusters,
    orders, order_lines, order_attachments, spots, dayparts, daypart_categories, rotation_rules, traffic_logs, break_structures, copy, copy_assignments,
    invoices, payments, makegoods, audit_logs, log_revisions, inventory, revenue, sales_goals,
    webhooks, notifications, collaboration, backups, settings, users,
    audio_cuts, live_reads, political_compliance, audio_delivery, audio_qc, help, proxy,
    production_orders, voice_talent, production_assignments, production_archive
)
from backend.middleware import AuthMiddleware, LoggingMiddleware, RequestSizeLimitMiddleware
from backend.middleware.rate_limit import RateLimitMiddleware
from backend.middleware.security_headers import SecurityHeadersMiddleware
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

# Validate critical environment variables at startup
def validate_environment():
    """Validate required environment variables at startup"""
    jwt_secret = os.getenv("JWT_SECRET_KEY")
    if not jwt_secret or len(jwt_secret) < 32:
        raise ValueError(
            "JWT_SECRET_KEY environment variable must be set and at least 32 characters long. "
            "Generate a strong secret key for production use. "
            "Example: python -c 'import secrets; print(secrets.token_urlsafe(32))'"
        )

# Validate environment before creating app
validate_environment()


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
    lifespan=lifespan,
    redirect_slashes=True,  # Enable automatic trailing slash redirects for compatibility
    swagger_ui_init_oauth={
        "clientId": "librelog-api",
        "appName": "LibreLog API",
    }
)

# Configure OpenAPI schema to include Bearer token authentication
from fastapi.openapi.utils import get_openapi

def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    openapi_schema = get_openapi(
        title=app.title,
        version=app.version,
        description=app.description,
        routes=app.routes,
    )
    # CRITICAL: Ensure OpenAPI version is set first (required by Swagger UI)
    if "openapi" not in openapi_schema or not openapi_schema.get("openapi"):
        openapi_schema["openapi"] = "3.1.0"
    
    # Ensure info section exists
    if "info" not in openapi_schema:
        openapi_schema["info"] = {
            "title": app.title,
            "version": app.version,
            "description": app.description
        }
    
    # Ensure components section exists
    if "components" not in openapi_schema:
        openapi_schema["components"] = {}
    
    # Add security scheme for Bearer token
    if "securitySchemes" not in openapi_schema["components"]:
        openapi_schema["components"]["securitySchemes"] = {}
    
    openapi_schema["components"]["securitySchemes"]["Bearer"] = {
        "type": "http",
        "scheme": "bearer",
        "bearerFormat": "JWT",
        "description": "Enter your JWT token (get it from /auth/login endpoint)"
    }
    
    # Ensure paths section exists
    if "paths" not in openapi_schema:
        openapi_schema["paths"] = {}
    
    app.openapi_schema = openapi_schema
    return app.openapi_schema

app.openapi = custom_openapi

# Configure CORS
# Get environment to determine CORS settings
app_env = os.getenv("APP_ENV", "development").lower()
is_production = app_env == "production"

# CORS origins - remove HTTP in production
if is_production:
    cors_origins = [
        "https://log.gayphx.com",
    ]
else:
    cors_origins = [
        "http://frontend:3000",  # Internal container communication
        "https://log-dev.gayphx.com",
        "http://log-dev.gayphx.com",
        "https://log.gayphx.com",
    ]

app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allow_headers=["Content-Type", "Authorization", "X-Requested-With"],
)

# Add trusted host middleware
# Use container names and domain names only - no localhost references
allowed_hosts = ["frontend", "api", "log-dev.gayphx.com", "log.gayphx.com", ".gayphx.com"]

app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=allowed_hosts
)

# Add custom middleware
# Order matters: security headers should be last (outermost), rate limiting early
app.add_middleware(SecurityHeadersMiddleware)  # Outermost - adds headers to all responses
app.add_middleware(RequestSizeLimitMiddleware)  # Check request size early
app.add_middleware(RateLimitMiddleware)  # Rate limiting
app.add_middleware(LoggingMiddleware)  # Logging
app.add_middleware(AuthMiddleware)  # Authentication

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

# Register public branding endpoint at app level to handle /api path
# This is a duplicate of /settings/branding/public but needed for /api path compatibility
@app.get("/api/settings/branding/public")
async def get_public_branding_api_path(db = Depends(get_db)):
    """Get public branding settings via /api path - no auth required"""
    from backend.services.settings_service import SettingsService
    from pathlib import Path
    
    try:
        branding_settings = await SettingsService.get_category_settings(db, "branding")
    except Exception as e:
        logger.error("Failed to fetch branding settings", error=str(e))
        branding_settings = {}
    
    # Apply defaults if not set
    system_name = "GayPHX Radio Traffic System"
    header_color = "#424242"
    logo_url = ""
    
    if branding_settings.get("system_name") and branding_settings["system_name"].get("value"):
        system_name = branding_settings["system_name"]["value"]
    if branding_settings.get("header_color") and branding_settings["header_color"].get("value"):
        header_color = branding_settings["header_color"]["value"]
    if branding_settings.get("logo_url") and branding_settings["logo_url"].get("value"):
        logo_url = branding_settings["logo_url"]["value"]
        # Verify the logo file actually exists
        if logo_url:
            import re
            filename_match = re.search(r'/([^/]+\.(png|jpg|jpeg|gif|svg|webp))$', logo_url)
            if filename_match:
                filename = filename_match.group(1)
                logo_dir = Path(os.getenv("LOGO_DIR", "/var/lib/librelog/logos"))
                try:
                    logo_dir.mkdir(parents=True, exist_ok=True)
                except (PermissionError, FileNotFoundError, OSError):
                    logo_dir = Path("/tmp/librelog/logos")
                file_path = logo_dir / filename
                if not file_path.exists():
                    fallback_dir = Path("/tmp/librelog/logos")
                    file_path = fallback_dir / filename
                if not file_path.exists() or not file_path.is_file():
                    logo_url = ""
    
    return {
        "system_name": system_name,
        "header_color": header_color,
        "logo_url": logo_url
    }

# Include routers
# NOTE: Traefik strips /api prefix before forwarding, so routes are registered without /api
# The paths will be /auth/login, /setup, etc. when they reach the backend
# However, when accessing directly (not through Traefik), we need /api prefix
app.include_router(auth.router, prefix="/auth", tags=["Authentication"])
app.include_router(setup.router, prefix="/setup", tags=["Setup"])
# Include tracks router with both /tracks and /api/tracks for compatibility
app.include_router(tracks.router, prefix="/tracks", tags=["Tracks"])
app.include_router(tracks.router, prefix="/api/tracks", tags=["Tracks"])  # For direct API access
app.include_router(campaigns.router, prefix="/campaigns", tags=["Campaigns"])
app.include_router(campaigns.router, prefix="/api/campaigns", tags=["Campaigns"])  # For direct API access
app.include_router(clocks.router, prefix="/clocks", tags=["Clock Templates"])
app.include_router(clocks.router, prefix="/api/clocks", tags=["Clock Templates"])  # For direct API access
app.include_router(logs.router, prefix="/logs", tags=["Logs"])
app.include_router(logs.router, prefix="/api/logs", tags=["Logs"])  # For direct API access
app.include_router(voice_tracks.router, prefix="/voice", tags=["Voice Tracks"])
app.include_router(reports.router, prefix="/reports", tags=["Reports"])
app.include_router(sync.router, prefix="/sync", tags=["Sync"])
app.include_router(activity.router, prefix="/activity", tags=["Activity"])
app.include_router(advertisers.router, prefix="/advertisers", tags=["Advertisers"])
app.include_router(advertisers.router, prefix="/api/advertisers", tags=["Advertisers"])  # For direct API access
app.include_router(agencies.router, prefix="/agencies", tags=["Agencies"])
app.include_router(agencies.router, prefix="/api/agencies", tags=["Agencies"])  # For direct API access
app.include_router(sales_reps.router, prefix="/sales-reps", tags=["Sales Reps"])
app.include_router(sales_teams.router, prefix="/sales-teams", tags=["Sales Teams"])
app.include_router(sales_offices.router, prefix="/sales-offices", tags=["Sales Offices"])
app.include_router(sales_regions.router, prefix="/sales-regions", tags=["Sales Regions"])
app.include_router(stations.router, prefix="/stations", tags=["Stations"])
app.include_router(clusters.router, prefix="/clusters", tags=["Clusters"])
app.include_router(orders.router, prefix="/orders", tags=["Orders"])
app.include_router(orders.router, prefix="/api/orders", tags=["Orders"])  # For direct API access
app.include_router(order_lines.router, prefix="/order-lines", tags=["Order Lines"])
app.include_router(order_attachments.router, prefix="/order-attachments", tags=["Order Attachments"])
app.include_router(spots.router, prefix="/spots", tags=["Spots"])
app.include_router(spots.router, prefix="/api/spots", tags=["Spots"])  # For direct API access
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
app.include_router(production_orders.router, prefix="/production-orders", tags=["Production Orders"])
app.include_router(production_orders.router, prefix="/api/production-orders", tags=["Production Orders"])  # For direct API access
app.include_router(voice_talent.router, prefix="/voice-talent", tags=["Voice Talent"])
app.include_router(production_assignments.router, prefix="/production-assignments", tags=["Production Assignments"])
app.include_router(production_assignments.router, prefix="/api/production-assignments", tags=["Production Assignments"])  # For direct API access
app.include_router(production_archive.router, prefix="/production-archive", tags=["Production Archive"])


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler with environment-aware error messages"""
    import traceback
    
    # Get environment
    app_env = os.getenv("APP_ENV", "development").lower()
    is_production = app_env == "production"
    
    # Log full error details (always log internally)
    logger.error(
        "Unhandled exception",
        exc_info=exc,
        path=request.url.path,
        method=request.method,
        client_ip=request.client.host if request.client else None,
        error_type=type(exc).__name__,
        error_message=str(exc)
    )
    
    # Sanitize error message for client response
    if is_production:
        # In production: generic error message, no stack traces
        error_detail = "An internal server error occurred. Please contact support if the problem persists."
        status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    else:
        # In development: more detailed error message
        error_detail = f"{type(exc).__name__}: {str(exc)}"
        status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    
    # Handle specific exception types
    if isinstance(exc, HTTPException):
        # FastAPI HTTPExceptions are already properly formatted
        return JSONResponse(
            status_code=exc.status_code,
            content={"detail": exc.detail}
        )
    
    # Handle validation errors
    if isinstance(exc, ValueError):
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={"detail": str(exc) if not is_production else "Invalid input provided"}
        )
    
    # Generic exception - return sanitized response
    return JSONResponse(
        status_code=status_code,
        content={"detail": error_detail}
    )
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
