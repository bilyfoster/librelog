"""
Reports router for traffic, billing, and sales reports
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func, or_
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.models.spot import Spot, SpotStatus
from backend.models.playback_history import PlaybackHistory
from backend.models.daily_log import DailyLog
from backend.models.track import Track
from backend.services.report_service import ReportService
from typing import Optional, List, Dict, Any
from datetime import date, datetime, timedelta

router = APIRouter()


@router.get("/")
async def list_available_reports(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List available report types"""
    return {
        "reports": {
            "traffic": [
                {"name": "reconciliation", "path": "/reports/reconciliation", "description": "Compare scheduled vs. aired spots"},
                {"name": "compliance", "path": "/reports/compliance", "description": "FCC and contract compliance report"},
                {"name": "playback", "path": "/reports/playback", "description": "Playback history report"},
                {"name": "daily-log", "path": "/reports/traffic/daily-log", "description": "Daily log report"},
                {"name": "missing-copy", "path": "/reports/traffic/missing-copy", "description": "Missing copy report"},
                {"name": "avails", "path": "/reports/traffic/avails", "description": "Avails report"},
                {"name": "conflicts", "path": "/reports/traffic/conflicts", "description": "Conflict report"},
                {"name": "expirations", "path": "/reports/traffic/expirations", "description": "Expiring copy/contracts report"}
            ],
            "billing": [
                {"name": "contract-actualization", "path": "/reports/billing/contract-actualization", "description": "Contract actualization report"},
                {"name": "revenue-summary", "path": "/reports/billing/revenue-summary", "description": "Revenue summary report"},
                {"name": "ar-aging", "path": "/reports/billing/ar-aging", "description": "AR aging report"},
                {"name": "makegoods", "path": "/reports/billing/makegoods", "description": "Makegood report"}
            ],
            "sales": [
                {"name": "revenue-by-rep", "path": "/reports/sales/revenue-by-rep", "description": "Revenue by rep report"},
                {"name": "revenue-by-advertiser", "path": "/reports/sales/revenue-by-advertiser", "description": "Revenue by advertiser report"},
                {"name": "pending-orders", "path": "/reports/sales/pending-orders", "description": "Pending orders report"},
                {"name": "expiring-contracts", "path": "/reports/sales/expiring-contracts", "description": "Expiring contracts report"}
            ],
            "audio": [
                {"name": "activity", "path": "/reports/audio/activity", "description": "Audio activity report"},
                {"name": "cut-rotation-performance", "path": "/reports/audio/cut-rotation-performance", "description": "Cut rotation performance report"},
                {"name": "live-read-performance", "path": "/reports/audio/live-read-performance", "description": "Live read performance report"},
                {"name": "fcc-compliance", "path": "/reports/audio/fcc-compliance", "description": "FCC compliance log for political ads"}
            ],
            "production": [
                {"name": "turnaround", "path": "/reports/production/turnaround", "description": "Production turnaround time report"},
                {"name": "workload", "path": "/reports/production/workload", "description": "Production workload report by user"},
                {"name": "missed-deadlines", "path": "/reports/production/missed-deadlines", "description": "Missed production deadlines report"}
            ]
        }
    }


@router.get("/reconciliation")
async def get_reconciliation_report(
    start_date: str,
    end_date: str,
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate reconciliation report comparing scheduled vs. aired spots"""
    try:
        start = datetime.fromisoformat(start_date).date()
        end = datetime.fromisoformat(end_date).date()
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid date format. Use ISO 8601 format (YYYY-MM-DD)")
    
    # Get all scheduled spots in date range
    spots_query = select(Spot).where(
        and_(
            Spot.scheduled_date >= start,
            Spot.scheduled_date <= end,
            Spot.status.in_([SpotStatus.SCHEDULED, SpotStatus.AIRED, SpotStatus.MISSED])
        )
    )
    
    # Filter by station if provided
    if station_id is not None:
        spots_query = spots_query.where(Spot.station_id == station_id)
    
    spots_result = await db.execute(spots_query)
    scheduled_spots = spots_result.scalars().all()
    
    # Get all playback history in date range
    playback_query = select(PlaybackHistory).join(DailyLog).where(
        and_(
            DailyLog.date >= start,
            DailyLog.date <= end
        )
    )
    playback_result = await db.execute(playback_query)
    playback_records = playback_result.scalars().all()
    
    # Build reconciliation report
    discrepancies = []
    matched_count = 0
    missed_count = 0
    extra_plays = []
    
    # Create a map of scheduled spots by date/time
    scheduled_map = {}
    for spot in scheduled_spots:
        key = f"{spot.scheduled_date}_{spot.scheduled_time}"
        if key not in scheduled_map:
            scheduled_map[key] = []
        scheduled_map[key].append(spot)
    
    # Create a map of playback records by date/time
    playback_map = {}
    for playback in playback_records:
        play_date = playback.played_at.date()
        play_time = playback.played_at.time().strftime("%H:%M:%S")
        key = f"{play_date}_{play_time}"
        if key not in playback_map:
            playback_map[key] = []
        playback_map[key].append(playback)
    
    # Compare scheduled vs. played
    for spot in scheduled_spots:
        key = f"{spot.scheduled_date}_{spot.scheduled_time}"
        played = playback_map.get(key, [])
        
        if not played:
            # Spot was scheduled but not played
            missed_count += 1
            discrepancies.append({
                "type": "missed",
                "spot_id": spot.id,
                "order_id": spot.order_id,
                "scheduled_date": spot.scheduled_date.isoformat(),
                "scheduled_time": spot.scheduled_time,
                "spot_length": spot.spot_length,
                "status": spot.status.value,
                "message": "Spot scheduled but not played"
            })
        else:
            matched_count += 1
            # Mark spot as aired if it matches
            if spot.status == SpotStatus.SCHEDULED:
                spot.status = SpotStatus.AIRED
                spot.actual_air_time = played[0].played_at
    
    # Find extra plays (played but not scheduled)
    for playback in playback_records:
        play_date = playback.played_at.date()
        play_time = playback.played_at.time().strftime("%H:%M:%S")
        key = f"{play_date}_{play_time}"
        
        if key not in scheduled_map:
            extra_plays.append({
                "type": "extra",
                "playback_id": playback.id,
                "track_id": playback.track_id,
                "played_at": playback.played_at.isoformat(),
                "duration_played": playback.duration_played,
                "message": "Played but not scheduled"
            })
    
    await db.commit()
    
    return {
        "start_date": start_date,
        "end_date": end_date,
        "total_scheduled": len(scheduled_spots),
        "matched": matched_count,
        "missed": missed_count,
        "extra_plays": len(extra_plays),
        "total_discrepancies": len(discrepancies) + len(extra_plays),
        "discrepancies": discrepancies,
        "extra_plays": extra_plays
    }


@router.get("/compliance")
async def get_compliance_report(
    start_date: str,
    end_date: str,
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    format: str = "csv",
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate compliance report for FCC and contract requirements"""
    try:
        start = datetime.fromisoformat(start_date).date()
        end = datetime.fromisoformat(end_date).date()
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid date format. Use ISO 8601 format (YYYY-MM-DD)")
    
    # Get all spots in date range
    spots_query = select(Spot).where(
        and_(
            Spot.scheduled_date >= start,
            Spot.scheduled_date <= end
        )
    )
    
    # Filter by station if provided
    if station_id is not None:
        spots_query = spots_query.where(Spot.station_id == station_id)
    
    spots_result = await db.execute(spots_query)
    spots = spots_result.scalars().all()
    
    # Compliance checks
    compliance_issues = []
    
    for spot in spots:
        issues = []
        
        # Check if spot has copy assigned
        if not spot.copy_assignment:
            issues.append("No copy assigned")
        
        # Check if spot was scheduled but not aired (after date)
        if spot.scheduled_date < date.today() and spot.status == SpotStatus.SCHEDULED:
            issues.append("Scheduled but not aired after date")
        
        # Check if makegood is needed
        if spot.status == SpotStatus.MISSED:
            issues.append("Missed spot - makegood required")
        
        if issues:
            compliance_issues.append({
                "spot_id": spot.id,
                "order_id": spot.order_id,
                "scheduled_date": spot.scheduled_date.isoformat(),
                "scheduled_time": spot.scheduled_time,
                "status": spot.status.value,
                "issues": issues
            })
    
    # Format response based on requested format
    if format.lower() == "csv":
        # Return CSV-formatted data
        csv_lines = ["Spot ID,Order ID,Date,Time,Status,Issues"]
        for issue in compliance_issues:
            csv_lines.append(
                f"{issue['spot_id']},{issue['order_id']},{issue['scheduled_date']},"
                f"{issue['scheduled_time']},{issue['status']},\"{','.join(issue['issues'])}\""
            )
        return {
            "format": "csv",
            "data": "\n".join(csv_lines),
            "total_issues": len(compliance_issues)
        }
    else:
        return {
            "start_date": start_date,
            "end_date": end_date,
            "total_spots": len(spots),
            "total_issues": len(compliance_issues),
            "compliance_issues": compliance_issues
        }


@router.get("/playback")
async def get_playback_report(
    start_date: str,
    end_date: str,
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get playback history report"""
    try:
        start = datetime.fromisoformat(start_date).date()
        end = datetime.fromisoformat(end_date).date()
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid date format. Use ISO 8601 format (YYYY-MM-DD)")
    
    # Get playback history in date range
    playback_query = select(PlaybackHistory).join(DailyLog).where(
        and_(
            DailyLog.date >= start,
            DailyLog.date <= end
        )
    )
    
    # Filter by station if provided
    if station_id is not None:
        playback_query = playback_query.where(DailyLog.station_id == station_id)
    
    playback_query = playback_query.order_by(PlaybackHistory.played_at)
    
    playback_result = await db.execute(playback_query)
    playback_records = playback_result.scalars().all()
    
    # Get track information for each playback
    playback_history = []
    track_ids = set()
    
    for playback in playback_records:
        track_ids.add(playback.track_id)
        playback_history.append({
            "id": playback.id,
            "track_id": playback.track_id,
            "log_id": playback.log_id,
            "played_at": playback.played_at.isoformat(),
            "duration_played": playback.duration_played
        })
    
    # Get track details
    if track_ids:
        tracks_query = select(Track).where(Track.id.in_(track_ids))
        tracks_result = await db.execute(tracks_query)
        tracks = {t.id: t for t in tracks_result.scalars().all()}
        
        # Add track info to playback history
        for item in playback_history:
            track = tracks.get(item["track_id"])
            if track:
                item["track_title"] = track.title
                item["track_artist"] = track.artist
                item["track_type"] = track.type
    
    return {
        "start_date": start_date,
        "end_date": end_date,
        "playback_history": playback_history,
        "total_tracks": len(playback_history),
        "unique_tracks": len(track_ids)
    }


# Traffic Reports
@router.get("/traffic/daily-log")
async def get_daily_log_report(
    log_date: date = Query(...),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Daily log report"""
    report_service = ReportService(db)
    return await report_service.generate_daily_log_report(log_date, station_id)


@router.get("/traffic/missing-copy")
async def get_missing_copy_report(
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Missing copy report"""
    report_service = ReportService(db)
    return await report_service.generate_missing_copy_report(start_date, end_date, station_id)


@router.get("/traffic/avails")
async def get_avails_report(
    start_date: date = Query(...),
    end_date: date = Query(...),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Avails report"""
    report_service = ReportService(db)
    return await report_service.generate_avails_report(start_date, end_date, station_id)


@router.get("/traffic/conflicts")
async def get_conflicts_report(
    log_date: date = Query(...),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Conflict report"""
    report_service = ReportService(db)
    return await report_service.generate_conflicts_report(log_date, station_id)


@router.get("/traffic/expirations")
async def get_expirations_report(
    days_ahead: int = Query(30, ge=1, le=365),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Expiring copy/contracts report"""
    report_service = ReportService(db)
    return await report_service.generate_expirations_report(days_ahead)


# Billing Reports
@router.get("/billing/contract-actualization")
async def get_contract_actualization_report(
    order_id: UUID = Query(...),
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Contract actualization report"""
    from backend.services.billing_service import BillingService
    
    billing_service = BillingService(db)
    if not start_date or not end_date:
        from backend.models.order import Order
        result = await db.execute(select(Order).where(Order.id == order_id))
        order = result.scalar_one_or_none()
        if order:
            start_date = order.start_date
            end_date = order.end_date
    
    return await billing_service.calculate_contract_actualization(order_id, start_date, end_date)


@router.get("/billing/revenue-summary")
async def get_revenue_summary_report(
    start_date: date = Query(...),
    end_date: date = Query(...),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Revenue summary report"""
    report_service = ReportService(db)
    return await report_service.generate_revenue_summary(start_date, end_date, station_id)


@router.get("/billing/ar-aging")
async def get_ar_aging_report(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """AR aging report"""
    from backend.services.billing_service import BillingService
    
    billing_service = BillingService(db)
    return await billing_service.calculate_aging()


@router.get("/billing/makegoods")
async def get_makegoods_report(
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Makegood report"""
    report_service = ReportService(db)
    return await report_service.generate_makegood_report(start_date, end_date, station_id)


# Sales Reports
@router.get("/sales/revenue-by-rep")
async def get_revenue_by_rep_report(
    start_date: date = Query(...),
    end_date: date = Query(...),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Revenue by rep report"""
    report_service = ReportService(db)
    return await report_service.generate_revenue_by_rep(start_date, end_date, station_id)


@router.get("/sales/revenue-by-advertiser")
async def get_revenue_by_advertiser_report(
    start_date: date = Query(...),
    end_date: date = Query(...),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Revenue by advertiser report"""
    report_service = ReportService(db)
    return await report_service.generate_revenue_by_advertiser(start_date, end_date, station_id)


@router.get("/sales/pending-orders")
async def get_pending_orders_report(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Pending orders report"""
    report_service = ReportService(db)
    return await report_service.generate_pending_orders_report()


@router.get("/sales/expiring-contracts")
async def get_expiring_contracts_report(
    days_ahead: int = Query(30, ge=1, le=365),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Expiring contracts report"""
    report_service = ReportService(db)
    return await report_service.generate_expiring_contracts_report(days_ahead)


@router.get("/audio/activity")
async def get_audio_activity_report(
    start_date: date = Query(...),
    end_date: date = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Audio activity report"""
    report_service = ReportService(db)
    return await report_service.generate_audio_activity_report(start_date, end_date)


@router.get("/audio/cut-rotation-performance")
async def get_cut_rotation_performance(
    copy_id: Optional[UUID] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Cut rotation performance report"""
    report_service = ReportService(db)
    return await report_service.generate_cut_rotation_performance(copy_id=copy_id)


@router.get("/audio/live-read-performance")
async def get_live_read_performance(
    start_date: date = Query(...),
    end_date: date = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Live read performance report"""
    report_service = ReportService(db)
    return await report_service.generate_live_read_performance(start_date, end_date)


@router.get("/audio/fcc-compliance")
async def get_fcc_compliance_log(
    start_date: date = Query(...),
    end_date: date = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """FCC compliance log for political ads"""
    report_service = ReportService(db)
    return await report_service.generate_fcc_compliance_log(start_date, end_date)


@router.get("/production/turnaround")
async def get_production_turnaround_report(
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Production turnaround time report"""
    report_service = ReportService(db)
    return await report_service.generate_production_turnaround_report(start_date, end_date)


@router.get("/production/workload")
async def get_production_workload_report(
    user_id: Optional[UUID] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Production workload report by user"""
    report_service = ReportService(db)
    return await report_service.generate_production_workload_report(user_id)


@router.get("/production/missed-deadlines")
async def get_missed_deadlines_report(
    days_overdue: int = Query(0, ge=0),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Missed production deadlines report"""
    report_service = ReportService(db)
    return await report_service.generate_missed_deadlines_report(days_overdue)


@router.post("/export")
async def export_report(
    report_type: str = Query(...),
    format: str = Query("pdf", regex="^(pdf|excel|csv)$"),
    report_params: dict = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Export report to PDF/Excel/CSV"""
    from fastapi.responses import FileResponse
    
    report_service = ReportService(db)
    result = await report_service.export_report(report_type, format, report_params or {})
    
    # Return file response
    if "file_path" in result:
        media_type_map = {
            "pdf": "application/pdf",
            "excel": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "csv": "text/csv"
        }
        
        return FileResponse(
            path=result["file_path"],
            media_type=media_type_map.get(format.lower(), "application/octet-stream"),
            filename=f"{report_type}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.{format.lower() if format.lower() != 'excel' else 'xlsx'}"
        )
    
    return result
