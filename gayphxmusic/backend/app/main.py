from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from app.core.config import settings
from app.api import submissions, admin, auth, exports, rights, play_tracking, artists, system_config
import uvicorn

# Create FastAPI app
app = FastAPI(
    title="GayPHX Music Platform API",
    description="API for music submission and ISRC management",
    version="1.0.0",
    docs_url="/api/docs",
    redoc_url="/api/redoc"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:80", "https://music.gayphx.com"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Add trusted host middleware for production
app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=["localhost", "127.0.0.1", "music.gayphx.com", "frontend", "backend"]
)

# Include routers
app.include_router(submissions.router, prefix="/api/submissions", tags=["submissions"])
app.include_router(admin.router, prefix="/api/admin", tags=["admin"])
app.include_router(auth.router, prefix="/api/auth", tags=["auth"])
app.include_router(exports.router, prefix="/api/exports", tags=["exports"])
app.include_router(rights.router, prefix="/api/rights", tags=["rights"])
app.include_router(play_tracking.router, prefix="/api/plays", tags=["play-tracking"])
app.include_router(artists.router, prefix="/api/artists", tags=["artists"])
app.include_router(system_config.router, prefix="/api/config", tags=["config"])


@app.get("/")
async def root():
    return {"message": "GayPHX Music Platform API", "version": "1.0.0"}


@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "gayphx-music-api"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
