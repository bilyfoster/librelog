"""
Proxy router for aggregating backend data
Allows frontend to get data without browser making direct API calls
"""

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Optional, Dict, Any
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
import httpx
import os

router = APIRouter()


@router.get("/dashboard")
async def get_dashboard_data(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Aggregate dashboard data from multiple sources
    Frontend calls this instead of making multiple API calls
    """
    from sqlalchemy import select, func
    from backend.models.track import Track
    from backend.models.campaign import Campaign
    from backend.models.clock_template import ClockTemplate
    from backend.models.daily_log import DailyLog
    
    try:
        # Get counts from database (server-side, no browser API calls)
        tracks_count = await db.execute(select(func.count(Track.id)))
        campaigns_count = await db.execute(select(func.count(Campaign.id)))
        clocks_count = await db.execute(select(func.count(ClockTemplate.id)))
        logs_count = await db.execute(select(func.count(DailyLog.id)))
        
        return {
            "tracks": {
                "count": tracks_count.scalar() or 0
            },
            "campaigns": {
                "count": campaigns_count.scalar() or 0
            },
            "clocks": {
                "count": clocks_count.scalar() or 0
            },
            "logs": {
                "count": logs_count.scalar() or 0
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch dashboard data: {str(e)}")


@router.get("/tracks/aggregated")
async def get_tracks_aggregated(
    track_type: Optional[str] = Query(None),
    limit: int = Query(999, ge=1, le=1000),
    skip: int = Query(0, ge=0),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Get tracks data aggregated with count
    Frontend calls this instead of making separate calls to /tracks and /tracks/count
    """
    from sqlalchemy import select, func, or_
    from backend.models.track import Track
    from backend.schemas.track import TrackResponse
    
    try:
        # Get count
        count_query = select(func.count(Track.id))
        if track_type:
            count_query = count_query.where(Track.type == track_type)
        if search:
            search_term = f"%{search}%"
            count_query = count_query.where(
                or_(
                    Track.title.ilike(search_term),
                    Track.artist.ilike(search_term)
                )
            )
        count_result = await db.execute(count_query)
        total_count = count_result.scalar() or 0
        
        # Get tracks
        tracks_query = select(Track)
        if track_type:
            tracks_query = tracks_query.where(Track.type == track_type)
        if search:
            search_term = f"%{search}%"
            tracks_query = tracks_query.where(
                or_(
                    Track.title.ilike(search_term),
                    Track.artist.ilike(search_term)
                )
            )
        tracks_query = tracks_query.offset(skip).limit(limit).order_by(Track.title)
        tracks_result = await db.execute(tracks_query)
        tracks = tracks_result.scalars().all()
        
        return {
            "count": total_count,
            "tracks": [TrackResponse.model_validate(track) for track in tracks],
            "limit": limit,
            "skip": skip
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch tracks: {str(e)}")


@router.get("/advertisers")
async def get_advertisers_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: Optional[bool] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Get advertisers - server-side proxy endpoint
    """
    from sqlalchemy import select, or_
    from backend.models.advertiser import Advertiser
    from backend.routers.advertisers import advertiser_to_response
    
    try:
        query = select(Advertiser)
        if active_only:
            query = query.where(Advertiser.active == True)
        if search:
            search_term = f"%{search}%"
            query = query.where(
                or_(
                    Advertiser.name.ilike(search_term),
                    Advertiser.contact_name.ilike(search_term)
                )
            )
        query = query.offset(skip).limit(limit).order_by(Advertiser.name)
        result = await db.execute(query)
        advertisers = result.scalars().all()
        
        return [advertiser_to_response(adv) for adv in advertisers]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch advertisers: {str(e)}")


@router.get("/agencies")
async def get_agencies_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: Optional[bool] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Get agencies - server-side proxy endpoint
    """
    from sqlalchemy import select, or_
    from backend.models.agency import Agency
    from backend.routers.agencies import agency_to_response
    
    try:
        query = select(Agency)
        if active_only:
            query = query.where(Agency.active == True)
        if search:
            search_term = f"%{search}%"
            query = query.where(
                or_(
                    Agency.name.ilike(search_term),
                    Agency.contact_name.ilike(search_term)
                )
            )
        query = query.offset(skip).limit(limit).order_by(Agency.name)
        result = await db.execute(query)
        agencies = result.scalars().all()
        
        return [agency_to_response(agency) for agency in agencies]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch agencies: {str(e)}")


@router.get("/users")
async def get_users_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    role: Optional[str] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Get users - server-side proxy endpoint
    """
    from sqlalchemy import select
    from backend.models.user import User
    from backend.routers.users import UserResponse
    
    try:
        query = select(User)
        if role:
            query = query.where(User.role == role)
        if search:
            search_term = f"%{search}%"
            query = query.where(User.username.ilike(search_term))
        query = query.offset(skip).limit(limit).order_by(User.username)
        result = await db.execute(query)
        users = result.scalars().all()
        
        # Convert datetime fields to strings for Pydantic validation
        users_data = []
        for user in users:
            user_dict = {
                "id": user.id,
                "username": user.username,
                "role": user.role,
                "created_at": user.created_at.isoformat() if user.created_at else "",
                "last_login": user.last_login.isoformat() if user.last_login else None,
                "last_activity": user.last_activity.isoformat() if user.last_activity else None,
            }
            users_data.append(UserResponse(**user_dict))
        
        return users_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch users: {str(e)}")


@router.get("/sales-reps")
async def get_sales_reps_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: Optional[bool] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Get sales reps - server-side proxy endpoint
    """
    from sqlalchemy import select
    from sqlalchemy.orm import selectinload
    from backend.models.sales_rep import SalesRep
    from backend.routers.sales_reps import SalesRepResponse
    
    try:
        query = select(SalesRep)
        if active_only:
            query = query.where(SalesRep.active == True)
        query = query.options(selectinload(SalesRep.user))
        query = query.offset(skip).limit(limit).order_by(SalesRep.id)
        result = await db.execute(query)
        sales_reps = result.scalars().all()
        
        # Load user data for each sales rep
        reps_data = []
        for rep in sales_reps:
            rep_dict = {
                "id": rep.id,
                "user_id": rep.user_id,
                "employee_id": rep.employee_id,
                "commission_rate": rep.commission_rate,
                "sales_goal": rep.sales_goal,
                "active": rep.active,
                "created_at": rep.created_at.isoformat() if rep.created_at else "",
                "updated_at": rep.updated_at.isoformat() if rep.updated_at else "",
                "username": rep.user.username if rep.user else None,
            }
            reps_data.append(SalesRepResponse(**rep_dict))
        
        return reps_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch sales reps: {str(e)}")


@router.get("/orders")
async def get_orders_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    status: Optional[str] = Query(None),
    advertiser_id: Optional[int] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Get orders - server-side proxy endpoint
    """
    from sqlalchemy import select
    from sqlalchemy.orm import selectinload
    from backend.models.order import Order
    from backend.routers.orders import order_to_response_dict
    
    try:
        query = select(Order)
        if status:
            query = query.where(Order.status == status)
        if advertiser_id:
            query = query.where(Order.advertiser_id == advertiser_id)
        if search:
            search_term = f"%{search}%"
            query = query.where(Order.name.ilike(search_term))
        query = query.options(
            selectinload(Order.advertiser),
            selectinload(Order.agency)
        )
        query = query.offset(skip).limit(limit).order_by(Order.created_at.desc())
        result = await db.execute(query)
        orders = result.scalars().all()
        
        return [order_to_response_dict(order) for order in orders]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch orders: {str(e)}")


@router.get("/dayparts")
async def get_dayparts_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    category_id: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get dayparts - server-side proxy endpoint"""
    from sqlalchemy import select
    from sqlalchemy.orm import selectinload
    from backend.models.daypart import Daypart
    from backend.routers.dayparts import DaypartResponse
    
    try:
        query = select(Daypart).options(selectinload(Daypart.category))
        if active_only:
            query = query.where(Daypart.active == True)
        if category_id:
            query = query.where(Daypart.category_id == category_id)
        query = query.offset(skip).limit(limit).order_by(Daypart.name)
        result = await db.execute(query)
        dayparts = result.scalars().all()
        
        dayparts_data = []
        for dp in dayparts:
            dp_dict = DaypartResponse.model_validate(dp).model_dump()
            dp_dict["start_time"] = str(dp.start_time)
            dp_dict["end_time"] = str(dp.end_time)
            dp_dict["category_name"] = getattr(dp.category, 'name', None) if hasattr(dp, 'category') and dp.category else None
            dayparts_data.append(dp_dict)
        
        return dayparts_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch dayparts: {str(e)}")


@router.get("/daypart-categories")
async def get_daypart_categories_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(False),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get daypart categories - server-side proxy endpoint"""
    from sqlalchemy import select
    from backend.models.daypart_category import DaypartCategory
    from backend.routers.daypart_categories import DaypartCategoryResponse
    
    try:
        query = select(DaypartCategory)
        if active_only:
            query = query.where(DaypartCategory.active == True)
        query = query.offset(skip).limit(limit).order_by(DaypartCategory.sort_order, DaypartCategory.name)
        result = await db.execute(query)
        categories = result.scalars().all()
        
        return [DaypartCategoryResponse.model_validate(cat) for cat in categories]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch daypart categories: {str(e)}")


@router.get("/rotation-rules")
async def get_rotation_rules_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get rotation rules - server-side proxy endpoint"""
    from sqlalchemy import select
    from sqlalchemy.orm import selectinload
    from backend.models.rotation_rule import RotationRule
    from backend.routers.rotation_rules import RotationRuleResponse
    
    try:
        query = select(RotationRule).options(
            selectinload(RotationRule.daypart),
            selectinload(RotationRule.campaign)
        )
        if active_only:
            query = query.where(RotationRule.active == True)
        query = query.offset(skip).limit(limit).order_by(RotationRule.priority.desc(), RotationRule.name)
        result = await db.execute(query)
        rules = result.scalars().all()
        
        rules_data = []
        for rule in rules:
            rule_dict = RotationRuleResponse.model_validate(rule).model_dump()
            rule_dict["daypart_name"] = getattr(rule.daypart, 'name', None) if rule.daypart else None
            rule_dict["campaign_name"] = getattr(rule.campaign, 'name', None) if rule.campaign else None
            rules_data.append(rule_dict)
        
        return rules_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch rotation rules: {str(e)}")


@router.get("/traffic-logs")
async def get_traffic_logs_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    log_type: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get traffic logs - server-side proxy endpoint"""
    from sqlalchemy import select, desc
    from sqlalchemy.orm import selectinload
    from backend.models.traffic_log import TrafficLog
    from backend.routers.traffic_logs import TrafficLogResponse
    
    try:
        query = select(TrafficLog).options(selectinload(TrafficLog.user))
        if log_type:
            query = query.where(TrafficLog.log_type == log_type)
        query = query.offset(skip).limit(limit).order_by(desc(TrafficLog.created_at))
        result = await db.execute(query)
        logs = result.scalars().all()
        
        logs_data = []
        for log in logs:
            log_dict = TrafficLogResponse.model_validate(log).model_dump()
            log_dict["username"] = getattr(log.user, 'username', None) if log.user else None
            logs_data.append(log_dict)
        
        return logs_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch traffic logs: {str(e)}")


@router.get("/copy")
async def get_copy_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    advertiser_id: Optional[int] = Query(None),
    order_id: Optional[int] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get copy - server-side proxy endpoint"""
    from sqlalchemy import select, or_
    from backend.models.copy import Copy
    from backend.routers.copy import CopyResponse, copy_to_response_dict
    
    try:
        query = select(Copy)
        if advertiser_id:
            query = query.where(Copy.advertiser_id == advertiser_id)
        if order_id:
            query = query.where(Copy.order_id == order_id)
        if search:
            search_term = f"%{search}%"
            query = query.where(
                or_(
                    Copy.title.ilike(search_term),
                    Copy.description.ilike(search_term)
                )
            )
        query = query.offset(skip).limit(limit).order_by(Copy.title)
        result = await db.execute(query)
        copies = result.scalars().all()
        
        return [CopyResponse(**copy_to_response_dict(c)) for c in copies]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch copy: {str(e)}")


@router.get("/campaigns")
async def get_campaigns_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get campaigns - server-side proxy endpoint"""
    from backend.services.campaign_service import CampaignService
    
    try:
        service = CampaignService(db)
        campaigns_data = await service.list_campaigns(
            skip=skip,
            limit=limit,
            active_only=active_only,
            date_filter=None
        )
        # Return campaigns array directly (extract from response if needed)
        if isinstance(campaigns_data, dict) and 'campaigns' in campaigns_data:
            return campaigns_data['campaigns']
        return campaigns_data if isinstance(campaigns_data, list) else []
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch campaigns: {str(e)}")


@router.get("/invoices")
async def get_invoices_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    status: Optional[str] = Query(None),
    status_filter: Optional[str] = Query(None),
    advertiser_id: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get invoices - server-side proxy endpoint"""
    from sqlalchemy import select
    from backend.models.invoice import Invoice, InvoiceStatus
    from backend.routers.invoices import InvoiceResponse, invoice_to_response_dict
    
    try:
        query = select(Invoice)
        # Handle both status and status_filter parameters
        status_param = status or status_filter
        if status_param:
            try:
                status_enum = InvoiceStatus[status_param.upper()]
                query = query.where(Invoice.status == status_enum)
            except KeyError:
                pass
        if advertiser_id:
            query = query.where(Invoice.advertiser_id == advertiser_id)
        query = query.offset(skip).limit(limit).order_by(Invoice.invoice_date.desc())
        result = await db.execute(query)
        invoices = result.scalars().all()
        
        return [InvoiceResponse(**invoice_to_response_dict(inv)) for inv in invoices]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch invoices: {str(e)}")


@router.get("/payments")
async def get_payments_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    invoice_id: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get payments - server-side proxy endpoint"""
    from sqlalchemy import select
    from backend.models.payment import Payment
    from backend.routers.payments import PaymentResponse
    
    try:
        query = select(Payment)
        if invoice_id:
            query = query.where(Payment.invoice_id == invoice_id)
        query = query.offset(skip).limit(limit).order_by(Payment.payment_date.desc())
        result = await db.execute(query)
        payments = result.scalars().all()
        
        return [PaymentResponse.model_validate(pay) for pay in payments]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch payments: {str(e)}")


@router.get("/makegoods")
async def get_makegoods_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    campaign_id: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get makegoods - server-side proxy endpoint"""
    from sqlalchemy import select
    from backend.models.makegood import Makegood
    from backend.routers.makegoods import MakegoodResponse
    
    try:
        query = select(Makegood)
        if campaign_id:
            query = query.where(Makegood.campaign_id == campaign_id)
        query = query.offset(skip).limit(limit).order_by(Makegood.created_at.desc())
        result = await db.execute(query)
        makegoods = result.scalars().all()
        
        return [MakegoodResponse.model_validate(mg) for mg in makegoods]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch makegoods: {str(e)}")


@router.get("/inventory")
async def get_inventory_proxy(
    start_date: str = Query(...),
    end_date: str = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get inventory - server-side proxy endpoint"""
    from datetime import date as date_type
    from backend.services.inventory_service import InventoryService
    
    try:
        start = date_type.fromisoformat(start_date)
        end = date_type.fromisoformat(end_date)
        inventory_service = InventoryService(db)
        result = await inventory_service.get_inventory_by_date_range(start, end)
        # Ensure we return the data in the expected format
        if isinstance(result, dict):
            return result
        return {"slots": result if isinstance(result, list) else []}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch inventory: {str(e)}")


@router.get("/sales-goals")
async def get_sales_goals_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    sales_rep_id: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get sales goals - server-side proxy endpoint"""
    from sqlalchemy import select
    from backend.models.sales_goal import SalesGoal
    from backend.routers.sales_goals import SalesGoalResponse
    
    try:
        query = select(SalesGoal)
        if sales_rep_id:
            query = query.where(SalesGoal.sales_rep_id == sales_rep_id)
        query = query.offset(skip).limit(limit).order_by(SalesGoal.target_date.desc())
        result = await db.execute(query)
        goals = result.scalars().all()
        
        return [SalesGoalResponse.model_validate(goal) for goal in goals]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch sales goals: {str(e)}")


@router.get("/settings")
async def get_settings_proxy(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get settings - server-side proxy endpoint"""
    from backend.services.settings_service import SettingsService
    import os
    
    try:
        categories = ["general", "branding", "smtp", "storage", "backup", "integrations"]
        result = {}
        for category in categories:
            result[category] = await SettingsService.get_category_settings(db, category)
        
        # Set default branding values only if not set in database
        branding_settings = result.get("branding", {})
        if not branding_settings.get("system_name") or not branding_settings["system_name"].get("value"):
            branding_settings["system_name"] = {
                "value": "GayPHX Radio Traffic System",
                "encrypted": False,
                "description": "Name of the traffic system displayed in the header"
            }
        if not branding_settings.get("header_color") or not branding_settings["header_color"].get("value"):
            branding_settings["header_color"] = {
                "value": "#424242",
                "encrypted": False,
                "description": "Header background color (hex code). Ensure API status indicators (green/red) remain visible."
            }
        if "logo_url" not in branding_settings:
            branding_settings["logo_url"] = {
                "value": "",
                "encrypted": False,
                "description": "URL to the logo image file (uploaded via logo upload endpoint)"
            }
        result["branding"] = branding_settings
        
        # Override integrations settings with environment variables if they exist
        libretime_url = os.getenv("LIBRETIME_URL", "").rstrip("/api/v2").rstrip("/")
        libretime_api_key = os.getenv("LIBRETIME_API_KEY", "")
        
        if libretime_url:
            if "integrations" not in result:
                result["integrations"] = {}
            result["integrations"]["libretime_url"] = {
                "value": libretime_url,
                "encrypted": False,
                "description": "LibreTime API URL (from environment variable)"
            }
        
        if libretime_api_key:
            if "integrations" not in result:
                result["integrations"] = {}
            result["integrations"]["libretime_api_key"] = {
                "value": libretime_api_key[:10] + "..." if len(libretime_api_key) > 10 else libretime_api_key,
                "encrypted": True,
                "description": "LibreTime API Key (from environment variable, masked)"
            }
        
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch settings: {str(e)}")


@router.get("/admin/audit-logs")
async def get_audit_logs_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    user_id: Optional[int] = Query(None),
    action: Optional[str] = Query(None),
    resource_type: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get audit logs - server-side proxy endpoint"""
    from sqlalchemy import select
    from sqlalchemy.orm import selectinload
    from backend.models.audit_log import AuditLog
    from backend.routers.audit_logs import AuditLogResponse
    import json
    
    try:
        query = select(AuditLog).options(selectinload(AuditLog.user))
        if user_id:
            query = query.where(AuditLog.user_id == user_id)
        if action:
            query = query.where(AuditLog.action == action)
        if resource_type:
            query = query.where(AuditLog.resource_type == resource_type)
        query = query.offset(skip).limit(limit).order_by(AuditLog.created_at.desc())
        result = await db.execute(query)
        logs = result.scalars().all()
        
        logs_data = []
        for log in logs:
            log_dict = AuditLogResponse.model_validate(log).model_dump()
            log_dict["username"] = getattr(log.user, 'username', None) if log.user else None
            if log.details:
                try:
                    log_dict["changes"] = json.loads(log.details) if isinstance(log.details, str) else log.details
                except:
                    log_dict["changes"] = None
            logs_data.append(log_dict)
        
        return logs_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch audit logs: {str(e)}")


@router.get("/webhooks")
async def get_webhooks_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(False),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get webhooks - server-side proxy endpoint"""
    from sqlalchemy import select
    from backend.models.webhook import Webhook
    from backend.routers.webhooks import WebhookResponse
    
    try:
        query = select(Webhook)
        if active_only:
            query = query.where(Webhook.active == True)
        query = query.offset(skip).limit(limit).order_by(Webhook.created_at.desc())
        result = await db.execute(query)
        webhooks = result.scalars().all()
        
        webhooks_data = []
        for wh in webhooks:
            wh_dict = WebhookResponse.model_validate(wh).model_dump()
            wh_dict["events"] = [e.value if hasattr(e, 'value') else str(e) for e in wh.events]
            webhooks_data.append(wh_dict)
        
        return webhooks_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch webhooks: {str(e)}")


@router.get("/notifications")
async def get_notifications_proxy(
    unread_only: bool = Query(False),
    skip: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get notifications - server-side proxy endpoint"""
    from backend.services.notification_service import NotificationService
    
    try:
        notifications = await NotificationService.get_user_notifications(
            db,
            current_user.id,
            unread_only=unread_only,
            limit=limit
        )
        return notifications
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch notifications: {str(e)}")


@router.get("/backups")
async def get_backups_proxy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get backups - server-side proxy endpoint"""
    from sqlalchemy import select
    from backend.models.backup import Backup
    from backend.routers.backups import BackupResponse
    
    try:
        query = select(Backup)
        query = query.offset(skip).limit(limit).order_by(Backup.created_at.desc())
        result = await db.execute(query)
        backups = result.scalars().all()
        
        return [BackupResponse.model_validate(backup) for backup in backups]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch backups: {str(e)}")


@router.get("/libretime/{path:path}")
async def proxy_libretime(
    path: str,
    current_user: User = Depends(get_current_user)
):
    """
    Proxy requests to LibreTime API
    Frontend calls this instead of calling LibreTime directly
    """
    libretime_url = os.getenv("LIBRETIME_URL", "").rstrip("/api/v2").rstrip("/")
    libretime_api_key = os.getenv("LIBRETIME_API_KEY", "")
    
    if not libretime_url:
        raise HTTPException(status_code=500, detail="LibreTime URL not configured")
    
    try:
        async with httpx.AsyncClient() as client:
            headers = {}
            if libretime_api_key:
                headers["Authorization"] = f"Api-Key {libretime_api_key}"
            
            url = f"{libretime_url}/api/v2/{path}"
            response = await client.get(url, headers=headers, timeout=30.0)
            response.raise_for_status()
            
            return response.json()
    except httpx.HTTPError as e:
        raise HTTPException(status_code=500, detail=f"LibreTime API error: {str(e)}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to proxy request: {str(e)}")

