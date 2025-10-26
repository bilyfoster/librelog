from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.submission import Submission
from app.models.isrc import ISRC
from typing import List
import csv
import io
import json
from datetime import datetime

router = APIRouter()


@router.get("/csv")
async def export_csv(db: Session = Depends(get_db)):
    """Export catalog as CSV"""
    
    # Get all approved submissions with ISRCs
    submissions = db.query(Submission).filter(
        Submission.status.in_(["approved", "isrc_assigned"])
    ).all()
    
    # Create CSV in memory
    output = io.StringIO()
    writer = csv.writer(output)
    
    # Write header
    writer.writerow([
        "Artist Name",
        "Song Title", 
        "Genre",
        "ISRC Code",
        "Status",
        "Submitted At",
        "Radio Permission",
        "Public Display",
        "Duration (seconds)",
        "Bitrate",
        "Sample Rate"
    ])
    
    # Write data
    for submission in submissions:
        isrc_code = submission.isrc.isrc_code if submission.isrc else ""
        
        writer.writerow([
            submission.artist.name,
            submission.song_title,
            submission.genre or "",
            isrc_code,
            submission.status,
            submission.submitted_at.isoformat(),
            "Yes" if submission.radio_permission else "No",
            "Yes" if submission.public_display else "No",
            submission.duration_seconds or "",
            submission.bitrate or "",
            submission.sample_rate or ""
        ])
    
    # Return CSV as response
    output.seek(0)
    csv_content = output.getvalue()
    
    return Response(
        content=csv_content,
        media_type="text/csv",
        headers={"Content-Disposition": f"attachment; filename=gayphx_catalog_{datetime.now().strftime('%Y%m%d')}.csv"}
    )


@router.get("/json")
async def export_json(db: Session = Depends(get_db)):
    """Export catalog as JSON for LibreTime integration"""
    
    # Get all approved submissions with ISRCs
    submissions = db.query(Submission).filter(
        Submission.status.in_(["approved", "isrc_assigned"])
    ).all()
    
    catalog = []
    
    for submission in submissions:
        track = {
            "id": str(submission.id),
            "artist": submission.artist.name,
            "title": submission.song_title,
            "genre": submission.genre,
            "isrc": submission.isrc.isrc_code if submission.isrc else None,
            "status": submission.status,
            "submitted_at": submission.submitted_at.isoformat(),
            "radio_permission": submission.radio_permission,
            "public_display": submission.public_display,
            "duration": submission.duration_seconds,
            "bitrate": submission.bitrate,
            "sample_rate": submission.sample_rate,
            "lufs": float(submission.lufs_reading) if submission.lufs_reading else None,
            "true_peak": float(submission.true_peak) if submission.true_peak else None
        }
        
        # Add file URL if available
        if submission.file_path:
            from app.services.storage import storage_service
            try:
                track["file_url"] = storage_service.generate_presigned_download_url(submission.file_path, expires=3600)
            except:
                track["file_url"] = None
        
        catalog.append(track)
    
    return {
        "catalog": catalog,
        "exported_at": datetime.now().isoformat(),
        "total_tracks": len(catalog),
        "platform": "GayPHX Music Platform"
    }


@router.get("/libretime")
async def export_libretime(db: Session = Depends(get_db)):
    """Export in LibreTime-compatible format"""
    
    # Get radio-permission approved submissions
    submissions = db.query(Submission).filter(
        Submission.status.in_(["approved", "isrc_assigned"]),
        Submission.radio_permission == True
    ).all()
    
    libretime_tracks = []
    
    for submission in submissions:
        track = {
            "id": str(submission.id),
            "name": f"{submission.artist.name} - {submission.song_title}",
            "artist": submission.artist.name,
            "title": submission.song_title,
            "genre": submission.genre or "Unknown",
            "isrc": submission.isrc.isrc_code if submission.isrc else None,
            "duration": submission.duration_seconds or 0,
            "bitrate": submission.bitrate or 0,
            "sample_rate": submission.sample_rate or 0,
            "channels": submission.channels or 2,
            "lufs": float(submission.lufs_reading) if submission.lufs_reading else None,
            "true_peak": float(submission.true_peak) if submission.true_peak else None,
            "submitted_at": submission.submitted_at.isoformat(),
            "public_display": submission.public_display
        }
        
        # Add file URL
        if submission.file_path:
            from app.services.storage import storage_service
            try:
                track["file_url"] = storage_service.generate_presigned_download_url(submission.file_path, expires=3600)
            except:
                track["file_url"] = None
        
        libretime_tracks.append(track)
    
    return {
        "tracks": libretime_tracks,
        "exported_at": datetime.now().isoformat(),
        "total_tracks": len(libretime_tracks),
        "format": "libretime_v1"
    }


@router.get("/stats")
async def export_stats(db: Session = Depends(get_db)):
    """Export platform statistics"""
    
    from sqlalchemy import func
    
    # Basic counts
    total_submissions = db.query(Submission).count()
    approved_submissions = db.query(Submission).filter(Submission.status == "approved").count()
    isrc_assigned = db.query(Submission).filter(Submission.status == "isrc_assigned").count()
    radio_permission = db.query(Submission).filter(Submission.radio_permission == True).count()
    public_display = db.query(Submission).filter(Submission.public_display == True).count()
    
    # Genre breakdown
    genre_stats = db.query(
        Submission.genre,
        func.count(Submission.id).label('count')
    ).filter(Submission.genre.isnot(None)).group_by(Submission.genre).all()
    
    # Monthly submission counts (last 12 months)
    monthly_stats = db.query(
        func.date_trunc('month', Submission.submitted_at).label('month'),
        func.count(Submission.id).label('count')
    ).group_by(func.date_trunc('month', Submission.submitted_at)).order_by('month').all()
    
    return {
        "total_submissions": total_submissions,
        "approved_submissions": approved_submissions,
        "isrc_assigned": isrc_assigned,
        "radio_permission": radio_permission,
        "public_display": public_display,
        "genre_breakdown": {genre: count for genre, count in genre_stats},
        "monthly_submissions": [
            {
                "month": month.isoformat(),
                "count": count
            }
            for month, count in monthly_stats
        ],
        "exported_at": datetime.now().isoformat()
    }

