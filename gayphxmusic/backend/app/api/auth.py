from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.artist import Artist
from app.models.magic_link_token import MagicLinkToken
from app.models.admin_user import AdminUser
from app.services.email import email_service
from app.core.security import verify_password, create_access_token
from pydantic import BaseModel, EmailStr
from typing import Optional
import uuid
import secrets
from datetime import datetime, timedelta
from app.core.config import settings

router = APIRouter()


class ArtistSignup(BaseModel):
    name: str
    email: EmailStr
    pronouns: Optional[str] = None
    bio: Optional[str] = None
    social_links: Optional[dict] = {}


class AdminLogin(BaseModel):
    email: EmailStr
    password: str


class AdminResponse(BaseModel):
    access_token: str
    token_type: str
    admin: dict


@router.post("/signup")
async def artist_signup(
    signup_data: ArtistSignup,
    db: Session = Depends(get_db)
):
    """Create new artist account"""
    
    # Check if artist already exists
    existing_artist = db.query(Artist).filter(Artist.email == signup_data.email).first()
    if existing_artist:
        raise HTTPException(
            status_code=400,
            detail="An account with this email already exists"
        )
    
    # Create new artist
    artist = Artist(
        name=signup_data.name,
        email=signup_data.email,
        pronouns=signup_data.pronouns,
        bio=signup_data.bio,
        social_links=signup_data.social_links or {}
    )
    
    db.add(artist)
    db.commit()
    db.refresh(artist)
    
    # Send welcome email with magic link
    token = secrets.token_urlsafe(32)
    expires_at = datetime.utcnow() + timedelta(minutes=settings.magic_link_expire_minutes)
    
    magic_token = MagicLinkToken(
        email=signup_data.email,
        token=token,
        expires_at=expires_at
    )
    
    db.add(magic_token)
    db.commit()
    
    # Send welcome email
    email_service.send_welcome_email(signup_data.email, signup_data.name, token)
    
    return {
        "message": "Account created successfully! Check your email for a magic link to sign in.",
        "artist_id": str(artist.id)
    }


class MagicLinkRequest(BaseModel):
    email: EmailStr


class MagicLinkVerify(BaseModel):
    token: str


class AuthResponse(BaseModel):
    access_token: str
    token_type: str
    artist: dict


@router.post("/request-magic-link")
async def request_magic_link(
    request_data: MagicLinkRequest,
    db: Session = Depends(get_db)
):
    """Request magic link for artist login"""
    
    # Check if artist exists
    artist = db.query(Artist).filter(Artist.email == request_data.email).first()
    if not artist:
        raise HTTPException(
            status_code=404,
            detail="No submissions found for this email address"
        )
    
    # Generate secure token
    token = secrets.token_urlsafe(32)
    expires_at = datetime.utcnow() + timedelta(minutes=settings.magic_link_expire_minutes)
    
    # Create magic link token record
    magic_token = MagicLinkToken(
        email=request_data.email,
        token=token,
        expires_at=expires_at
    )
    
    db.add(magic_token)
    db.commit()
    
    # Send magic link email
    email_service.send_magic_link(request_data.email, token)
    
    return {"message": "Magic link sent to your email address"}


@router.get("/verify", response_model=AuthResponse)
async def verify_magic_link_get(
    token: str,
    db: Session = Depends(get_db)
):
    """Verify magic link token via GET (for email links)"""
    return await verify_magic_link_internal(token, db)

@router.post("/verify", response_model=AuthResponse)
async def verify_magic_link_post(
    verify_data: MagicLinkVerify,
    db: Session = Depends(get_db)
):
    """Verify magic link token via POST (for API calls)"""
    return await verify_magic_link_internal(verify_data.token, db)

async def verify_magic_link_internal(
    token: str,
    db: Session
):
    """Verify magic link token and create session"""
    
    # Find valid token
    magic_token = db.query(MagicLinkToken).filter(
        MagicLinkToken.token == token,
        MagicLinkToken.expires_at > datetime.utcnow(),
        MagicLinkToken.used_at.is_(None)
    ).first()
    
    if not magic_token:
        raise HTTPException(
            status_code=400,
            detail="Invalid or expired token"
        )
    
    # Mark token as used
    magic_token.used_at = datetime.utcnow()
    
    # Get artist
    artist = db.query(Artist).filter(Artist.email == magic_token.email).first()
    if not artist:
        raise HTTPException(status_code=404, detail="Artist not found")
    
    # Generate proper JWT token
    from app.core.security import create_access_token
    access_token = create_access_token(
        data={"sub": str(artist.id), "is_artist": True, "email": artist.email}
    )
    
    db.commit()
    
    return AuthResponse(
        access_token=access_token,
        token_type="bearer",
        artist={
            "id": str(artist.id),
            "name": artist.name,
            "email": artist.email,
            "pronouns": artist.pronouns,
            "bio": artist.bio,
            "social_links": artist.social_links or {}
        }
    )


@router.get("/me")
async def get_current_artist(
    # TODO: Add JWT token validation
    artist_id: str = "placeholder",
    db: Session = Depends(get_db)
):
    """Get current artist information"""
    
    artist = db.query(Artist).filter(Artist.id == artist_id).first()
    if not artist:
        raise HTTPException(status_code=404, detail="Artist not found")
    
    return {
        "id": str(artist.id),
        "name": artist.name,
        "email": artist.email,
        "pronouns": artist.pronouns,
        "bio": artist.bio,
        "social_links": artist.social_links or {}
    }


@router.get("/profile/{email}")
async def get_artist_profile(
    email: str,
    db: Session = Depends(get_db)
):
    """Get artist profile by email for auto-filling submission form"""
    
    artist = db.query(Artist).filter(Artist.email == email).first()
    if not artist:
        raise HTTPException(status_code=404, detail="Artist not found")
    
    return {
        "id": str(artist.id),
        "name": artist.name,
        "email": artist.email,
        "pronouns": artist.pronouns,
        "bio": artist.bio,
        "social_links": artist.social_links or {}
    }


@router.post("/admin/login", response_model=AdminResponse)
async def admin_login(
    login_data: AdminLogin,
    db: Session = Depends(get_db)
):
    """Admin login with email and password"""
    
    # Find admin user
    admin = db.query(AdminUser).filter(AdminUser.email == login_data.email).first()
    if not admin:
        raise HTTPException(
            status_code=401,
            detail="Invalid credentials"
        )
    
    # Verify password
    if not verify_password(login_data.password, admin.password_hash):
        raise HTTPException(
            status_code=401,
            detail="Invalid credentials"
        )
    
    # Check if admin is active
    if not admin.is_active:
        raise HTTPException(
            status_code=401,
            detail="Account is disabled"
        )
    
    # Update last login
    admin.last_login = datetime.utcnow()
    db.commit()
    
    # Create JWT token
    access_token = create_access_token(
        data={"sub": str(admin.id), "is_admin": True, "email": admin.email}
    )
    
    return AdminResponse(
        access_token=access_token,
        token_type="bearer",
        admin={
            "id": str(admin.id),
            "name": admin.name,
            "email": admin.email,
            "role": admin.role,
            "last_login": admin.last_login.isoformat() if admin.last_login else None
        }
    )
