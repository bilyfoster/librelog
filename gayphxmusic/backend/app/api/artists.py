from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List, Optional
from pydantic import BaseModel, EmailStr
from datetime import datetime

from app.core.database import get_db
from app.core.security import get_current_artist
from app.models.artist import Artist
from app.models.submission import Submission

router = APIRouter()

# Pydantic models
class ArtistBase(BaseModel):
    name: str
    pronouns: Optional[str] = None
    bio: Optional[str] = None
    social_links: Optional[dict] = None

class ArtistCreate(ArtistBase):
    pass

class ArtistUpdate(ArtistBase):
    pass

class ArtistResponse(ArtistBase):
    id: str
    email: str
    is_active: bool
    created_at: datetime
    updated_at: datetime
    submission_count: int = 0
    favorited_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class ArtistListResponse(BaseModel):
    artists: List[ArtistResponse]
    total: int

class ProfileResponse(BaseModel):
    id: str
    name: str
    email: str
    pronouns: Optional[str] = None
    bio: Optional[str] = None
    social_links: dict
    created_at: datetime
    updated_at: datetime
    submission_count: int = 0

class ProfileUpdate(BaseModel):
    name: str
    pronouns: Optional[str] = None
    bio: Optional[str] = None
    social_links: Optional[dict] = None

@router.get("/", response_model=ArtistListResponse)
async def get_user_artists(
    skip: int = 0,
    limit: int = 100,
    search: Optional[str] = None,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get all artists for the current user"""
    
    query = db.query(Artist).filter(Artist.email == current_artist.email)
    
    if search:
        query = query.filter(Artist.name.ilike(f"%{search}%"))
    
    total = query.count()
    artists = query.offset(skip).limit(limit).all()
    
    # Add submission count for each artist
    artist_responses = []
    for artist in artists:
        submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
        artist_data = ArtistResponse(
            id=str(artist.id),
            email=artist.email,
            name=artist.name,
            pronouns=artist.pronouns,
            bio=artist.bio,
            social_links=artist.social_links or {},
            is_active=artist.is_active,
            created_at=artist.created_at,
            updated_at=artist.updated_at,
            submission_count=submission_count
        )
        artist_responses.append(artist_data)
    
    return ArtistListResponse(artists=artist_responses, total=total)

@router.get("/{artist_id}", response_model=ArtistResponse)
async def get_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get a specific artist by ID"""
    
    artist = db.query(Artist).filter(
        Artist.id == artist_id,
        Artist.email == current_artist.email
    ).first()
    
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
    
    return ArtistResponse(
        id=str(artist.id),
        email=artist.email,
        name=artist.name,
        pronouns=artist.pronouns,
        bio=artist.bio,
        social_links=artist.social_links or {},
        is_active=artist.is_active,
        created_at=artist.created_at,
        updated_at=artist.updated_at,
        submission_count=submission_count
    )

# Favorites endpoints
@router.post("/favorites/{artist_id}")
async def favorite_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Add an artist to favorites"""
    from app.models.artist_favorite import ArtistFavorite
    
    # Check if artist exists
    artist = db.query(Artist).filter(Artist.id == artist_id).first()
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    # Check if already favorited
    existing_favorite = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email,
        ArtistFavorite.artist_id == artist_id
    ).first()
    
    if existing_favorite:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Artist is already in favorites"
        )
    
    # Create favorite
    favorite = ArtistFavorite(
        user_email=current_artist.email,
        artist_id=artist_id
    )
    
    db.add(favorite)
    db.commit()
    
    return {"message": "Artist added to favorites"}

@router.delete("/favorites/{artist_id}")
async def unfavorite_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Remove an artist from favorites"""
    from app.models.artist_favorite import ArtistFavorite
    
    favorite = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email,
        ArtistFavorite.artist_id == artist_id
    ).first()
    
    if not favorite:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found in favorites"
        )
    
    db.delete(favorite)
    db.commit()
    
    return {"message": "Artist removed from favorites"}

@router.get("/favorites")
async def get_favorite_artists(
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get user's favorite artists"""
    from app.models.artist_favorite import ArtistFavorite
    
    favorites = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email
    ).all()
    
    favorite_artists = []
    for favorite in favorites:
        artist = favorite.artist
        submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
        
        favorite_artists.append(ArtistResponse(
            id=str(artist.id),
            email=artist.email,
            name=artist.name,
            pronouns=artist.pronouns,
            bio=artist.bio,
            social_links=artist.social_links or {},
            is_active=artist.is_active,
            created_at=artist.created_at,
            updated_at=artist.updated_at,
            submission_count=submission_count,
            favorited_at=favorite.favorited_at
        ))
    
    return favorite_artists

@router.post("/", response_model=ArtistResponse)
async def create_artist(
    artist_data: ArtistCreate,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Create a new artist"""
    
    # Check if artist with same name already exists for this user
    existing_artist = db.query(Artist).filter(
        Artist.email == current_artist.email,
        Artist.name == artist_data.name
    ).first()
    
    if existing_artist:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="An artist with this name already exists"
        )
    
    artist = Artist(
        email=current_artist.email,
        name=artist_data.name,
        pronouns=artist_data.pronouns,
        bio=artist_data.bio,
        social_links=artist_data.social_links or {}
    )
    
    db.add(artist)
    db.commit()
    db.refresh(artist)
    
    return ArtistResponse(
        id=str(artist.id),
        email=artist.email,
        name=artist.name,
        pronouns=artist.pronouns,
        bio=artist.bio,
        social_links=artist.social_links or {},
        is_active=artist.is_active,
        created_at=artist.created_at,
        updated_at=artist.updated_at,
        submission_count=0
    )

@router.put("/{artist_id}", response_model=ArtistResponse)
async def update_artist(
    artist_id: str,
    artist_data: ArtistUpdate,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Update an existing artist"""
    
    artist = db.query(Artist).filter(
        Artist.id == artist_id,
        Artist.email == current_artist.email
    ).first()
    
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    # Check if another artist with same name already exists for this user
    if artist_data.name != artist.name:
        existing_artist = db.query(Artist).filter(
            Artist.email == current_artist.email,
            Artist.name == artist_data.name,
            Artist.id != artist_id
        ).first()
        
        if existing_artist:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="An artist with this name already exists"
            )
    
    # Update artist fields
    artist.name = artist_data.name
    artist.pronouns = artist_data.pronouns
    artist.bio = artist_data.bio
    artist.social_links = artist_data.social_links or {}
    artist.updated_at = datetime.utcnow()
    
    db.commit()
    db.refresh(artist)
    
    submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
    
    return ArtistResponse(
        id=str(artist.id),
        email=artist.email,
        name=artist.name,
        pronouns=artist.pronouns,
        bio=artist.bio,
        social_links=artist.social_links or {},
        is_active=artist.is_active,
        created_at=artist.created_at,
        updated_at=artist.updated_at,
        submission_count=submission_count
    )

# Favorites endpoints
@router.post("/favorites/{artist_id}")
async def favorite_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Add an artist to favorites"""
    from app.models.artist_favorite import ArtistFavorite
    
    # Check if artist exists
    artist = db.query(Artist).filter(Artist.id == artist_id).first()
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    # Check if already favorited
    existing_favorite = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email,
        ArtistFavorite.artist_id == artist_id
    ).first()
    
    if existing_favorite:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Artist is already in favorites"
        )
    
    # Create favorite
    favorite = ArtistFavorite(
        user_email=current_artist.email,
        artist_id=artist_id
    )
    
    db.add(favorite)
    db.commit()
    
    return {"message": "Artist added to favorites"}

@router.delete("/favorites/{artist_id}")
async def unfavorite_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Remove an artist from favorites"""
    from app.models.artist_favorite import ArtistFavorite
    
    favorite = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email,
        ArtistFavorite.artist_id == artist_id
    ).first()
    
    if not favorite:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found in favorites"
        )
    
    db.delete(favorite)
    db.commit()
    
    return {"message": "Artist removed from favorites"}

@router.get("/favorites")
async def get_favorite_artists(
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get user's favorite artists"""
    from app.models.artist_favorite import ArtistFavorite
    
    favorites = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email
    ).all()
    
    favorite_artists = []
    for favorite in favorites:
        artist = favorite.artist
        submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
        
        favorite_artists.append(ArtistResponse(
            id=str(artist.id),
            email=artist.email,
            name=artist.name,
            pronouns=artist.pronouns,
            bio=artist.bio,
            social_links=artist.social_links or {},
            is_active=artist.is_active,
            created_at=artist.created_at,
            updated_at=artist.updated_at,
            submission_count=submission_count,
            favorited_at=favorite.favorited_at
        ))
    
    return favorite_artists

@router.delete("/{artist_id}")
async def delete_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Delete an artist (soft delete by setting is_active to False)"""
    
    artist = db.query(Artist).filter(
        Artist.id == artist_id,
        Artist.email == current_artist.email
    ).first()
    
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    # Check if artist has submissions
    submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
    if submission_count > 0:
        # Soft delete - just deactivate
        artist.is_active = False
        artist.updated_at = datetime.utcnow()
        db.commit()
        return {"message": f"Artist deactivated (has {submission_count} submissions)"}
    else:
        # Hard delete - no submissions
        db.delete(artist)
        db.commit()
        return {"message": "Artist deleted successfully"}

@router.post("/{artist_id}/reactivate")
async def reactivate_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Reactivate a deactivated artist"""
    
    artist = db.query(Artist).filter(
        Artist.id == artist_id,
        Artist.email == current_artist.email
    ).first()
    
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    artist.is_active = True
    artist.updated_at = datetime.utcnow()
    db.commit()
    
    return {"message": "Artist reactivated successfully"}

@router.get("/dropdown/list")
async def get_artists_dropdown(
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get simplified artist list for dropdown selection"""
    
    artists = db.query(Artist).filter(
        Artist.email == current_artist.email,
        Artist.is_active == True
    ).order_by(Artist.name).all()
    
    return [
        {
            "id": str(artist.id),
            "name": artist.name,
            "pronouns": artist.pronouns
        }
        for artist in artists
    ]

@router.get("/profile", response_model=ProfileResponse)
async def get_current_profile(
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get current artist's profile information"""
    
    # Get submission count
    submission_count = db.query(Submission).filter(Submission.artist_id == current_artist.id).count()
    
    return ProfileResponse(
        id=str(current_artist.id),
        name=current_artist.name,
        email=current_artist.email,
        pronouns=current_artist.pronouns,
        bio=current_artist.bio,
        social_links=current_artist.social_links or {},
        created_at=current_artist.created_at,
        updated_at=current_artist.updated_at,
        submission_count=submission_count
    )

# Favorites endpoints
@router.post("/favorites/{artist_id}")
async def favorite_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Add an artist to favorites"""
    from app.models.artist_favorite import ArtistFavorite
    
    # Check if artist exists
    artist = db.query(Artist).filter(Artist.id == artist_id).first()
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    # Check if already favorited
    existing_favorite = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email,
        ArtistFavorite.artist_id == artist_id
    ).first()
    
    if existing_favorite:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Artist is already in favorites"
        )
    
    # Create favorite
    favorite = ArtistFavorite(
        user_email=current_artist.email,
        artist_id=artist_id
    )
    
    db.add(favorite)
    db.commit()
    
    return {"message": "Artist added to favorites"}

@router.delete("/favorites/{artist_id}")
async def unfavorite_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Remove an artist from favorites"""
    from app.models.artist_favorite import ArtistFavorite
    
    favorite = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email,
        ArtistFavorite.artist_id == artist_id
    ).first()
    
    if not favorite:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found in favorites"
        )
    
    db.delete(favorite)
    db.commit()
    
    return {"message": "Artist removed from favorites"}

@router.get("/favorites")
async def get_favorite_artists(
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get user's favorite artists"""
    from app.models.artist_favorite import ArtistFavorite
    
    favorites = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email
    ).all()
    
    favorite_artists = []
    for favorite in favorites:
        artist = favorite.artist
        submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
        
        favorite_artists.append(ArtistResponse(
            id=str(artist.id),
            email=artist.email,
            name=artist.name,
            pronouns=artist.pronouns,
            bio=artist.bio,
            social_links=artist.social_links or {},
            is_active=artist.is_active,
            created_at=artist.created_at,
            updated_at=artist.updated_at,
            submission_count=submission_count,
            favorited_at=favorite.favorited_at
        ))
    
    return favorite_artists

@router.put("/profile", response_model=ProfileResponse)
async def update_current_profile(
    profile_data: ProfileUpdate,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Update current artist's profile information"""
    
    # Update artist fields
    current_artist.name = profile_data.name
    current_artist.pronouns = profile_data.pronouns
    current_artist.bio = profile_data.bio
    current_artist.social_links = profile_data.social_links or {}
    current_artist.updated_at = datetime.utcnow()
    
    db.commit()
    db.refresh(current_artist)
    
    # Get updated submission count
    submission_count = db.query(Submission).filter(Submission.artist_id == current_artist.id).count()
    
    return ProfileResponse(
        id=str(current_artist.id),
        name=current_artist.name,
        email=current_artist.email,
        pronouns=current_artist.pronouns,
        bio=current_artist.bio,
        social_links=current_artist.social_links or {},
        created_at=current_artist.created_at,
        updated_at=current_artist.updated_at,
        submission_count=submission_count
    )

# Favorites endpoints
@router.post("/favorites/{artist_id}")
async def favorite_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Add an artist to favorites"""
    from app.models.artist_favorite import ArtistFavorite
    
    # Check if artist exists
    artist = db.query(Artist).filter(Artist.id == artist_id).first()
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    # Check if already favorited
    existing_favorite = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email,
        ArtistFavorite.artist_id == artist_id
    ).first()
    
    if existing_favorite:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Artist is already in favorites"
        )
    
    # Create favorite
    favorite = ArtistFavorite(
        user_email=current_artist.email,
        artist_id=artist_id
    )
    
    db.add(favorite)
    db.commit()
    
    return {"message": "Artist added to favorites"}

@router.delete("/favorites/{artist_id}")
async def unfavorite_artist(
    artist_id: str,
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Remove an artist from favorites"""
    from app.models.artist_favorite import ArtistFavorite
    
    favorite = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email,
        ArtistFavorite.artist_id == artist_id
    ).first()
    
    if not favorite:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found in favorites"
        )
    
    db.delete(favorite)
    db.commit()
    
    return {"message": "Artist removed from favorites"}

@router.get("/favorites")
async def get_favorite_artists(
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get user's favorite artists"""
    from app.models.artist_favorite import ArtistFavorite
    
    favorites = db.query(ArtistFavorite).filter(
        ArtistFavorite.user_email == current_artist.email
    ).all()
    
    favorite_artists = []
    for favorite in favorites:
        artist = favorite.artist
        submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
        
        favorite_artists.append(ArtistResponse(
            id=str(artist.id),
            email=artist.email,
            name=artist.name,
            pronouns=artist.pronouns,
            bio=artist.bio,
            social_links=artist.social_links or {},
            is_active=artist.is_active,
            created_at=artist.created_at,
            updated_at=artist.updated_at,
            submission_count=submission_count,
            favorited_at=favorite.favorited_at
        ))
    
    return favorite_artists