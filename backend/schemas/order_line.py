"""
Pydantic schemas for OrderLine models - WideOrbit-compatible
"""

from pydantic import BaseModel, ConfigDict
from typing import Optional, Dict, Any
from datetime import date, datetime
from decimal import Decimal
from uuid import UUID
from backend.models.order_line import RevenueType, SelloutClass


class OrderLineBase(BaseModel):
    """Base order line schema"""
    line_number: int
    start_date: date
    end_date: date
    station_id: UUID  # Required - each line targets one station
    station: Optional[str] = None  # Legacy field - kept for backward compatibility
    product: Optional[str] = None
    revenue_type: Optional[RevenueType] = None
    length: Optional[int] = None
    daypart: Optional[str] = None
    days_of_week: Optional[str] = None
    rate: Optional[Decimal] = None
    rate_type: Optional[str] = None
    sellout_class: Optional[SelloutClass] = None
    priority_code: Optional[str] = None
    spot_frequency: Optional[int] = None
    estimated_impressions: Optional[int] = None
    cpm: Optional[Decimal] = None
    cpp: Optional[Decimal] = None
    makegood_eligible: bool = True
    guaranteed_position: bool = False
    preemptible: bool = True
    inventory_class: Optional[str] = None
    product_code: Optional[str] = None
    deal_points: Optional[str] = None
    notes: Optional[str] = None


class OrderLineDigital(BaseModel):
    """Digital/streaming add-on fields"""
    platform: Optional[str] = None
    impressions_booked: Optional[int] = None
    delivery_window: Optional[str] = None
    targeting_parameters: Optional[Dict[str, Any]] = None
    companion_banners: Optional[Dict[str, Any]] = None


class OrderLineCreate(OrderLineBase, OrderLineDigital):
    """Schema for creating a new order line"""
    order_id: UUID


class OrderLineUpdate(BaseModel):
    """Schema for updating an order line"""
    line_number: Optional[int] = None
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    station_id: Optional[UUID] = None
    station: Optional[str] = None  # Legacy field
    product: Optional[str] = None
    revenue_type: Optional[RevenueType] = None
    length: Optional[int] = None
    daypart: Optional[str] = None
    days_of_week: Optional[str] = None
    rate: Optional[Decimal] = None
    rate_type: Optional[str] = None
    sellout_class: Optional[SelloutClass] = None
    priority_code: Optional[str] = None
    spot_frequency: Optional[int] = None
    estimated_impressions: Optional[int] = None
    cpm: Optional[Decimal] = None
    cpp: Optional[Decimal] = None
    makegood_eligible: Optional[bool] = None
    guaranteed_position: Optional[bool] = None
    preemptible: Optional[bool] = None
    inventory_class: Optional[str] = None
    product_code: Optional[str] = None
    deal_points: Optional[str] = None
    notes: Optional[str] = None
    platform: Optional[str] = None
    impressions_booked: Optional[int] = None
    delivery_window: Optional[str] = None
    targeting_parameters: Optional[Dict[str, Any]] = None
    companion_banners: Optional[Dict[str, Any]] = None


class OrderLineResponse(OrderLineBase, OrderLineDigital):
    """Schema for order line response"""
    id: UUID
    order_id: UUID
    station_id: UUID
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)

