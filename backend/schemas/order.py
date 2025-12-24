"""
Pydantic schemas for Order models - WideOrbit-compatible
"""

from pydantic import BaseModel, Field, ConfigDict
from typing import Optional, List, Dict, Any
from datetime import date, datetime
from decimal import Decimal
from uuid import UUID
from backend.models.order import OrderType, BillingCycle, InvoiceType, PoliticalClass, OrderStatus, ApprovalStatus, RateType


class OrderBase(BaseModel):
    """Base order schema"""
    order_name: Optional[str] = None
    campaign_id: Optional[UUID] = None
    advertiser_id: UUID
    agency_id: Optional[UUID] = None
    sales_rep_id: Optional[UUID] = None
    sales_team: Optional[str] = None
    sales_office: Optional[str] = None
    sales_region: Optional[str] = None
    stations: Optional[List[str]] = None
    cluster: Optional[str] = None
    order_type: Optional[OrderType] = None
    start_date: date
    end_date: date
    billing_address: Optional[str] = None
    billing_contact: Optional[str] = None
    billing_contact_email: Optional[str] = None
    billing_contact_phone: Optional[str] = None


class OrderFinancial(BaseModel):
    """Financial terms schema"""
    gross_amount: Optional[Decimal] = Decimal("0.00")
    net_amount: Optional[Decimal] = Decimal("0.00")
    total_value: Optional[Decimal] = Decimal("0.00")
    agency_commission_percent: Optional[Decimal] = None
    agency_commission_amount: Optional[Decimal] = None
    agency_discount: Optional[Decimal] = None
    cash_discount: Optional[Decimal] = None
    trade_barter: bool = False
    trade_value: Optional[Decimal] = None
    taxable: bool = False
    billing_cycle: Optional[BillingCycle] = None
    invoice_type: Optional[InvoiceType] = None


class OrderCoop(BaseModel):
    """Co-op fields schema"""
    coop_sponsor: Optional[str] = None
    coop_percent: Optional[Decimal] = None


class OrderLegal(BaseModel):
    """Legal/compliance fields schema"""
    political_class: Optional[PoliticalClass] = None
    political_window_flag: bool = False
    contract_reference: Optional[str] = None
    insertion_order_number: Optional[str] = None
    regulatory_notes: Optional[str] = None
    fcc_id: Optional[str] = None
    required_disclaimers: Optional[str] = None
    client_po_number: Optional[str] = None


class OrderCreate(OrderBase, OrderFinancial, OrderCoop, OrderLegal):
    """Schema for creating a new order"""
    order_number: Optional[str] = None  # Auto-generated if not provided
    spot_lengths: Optional[List[int]] = None
    total_spots: int = 0
    rate_type: RateType = RateType.ROS
    rates: Optional[Dict[str, Any]] = None
    status: OrderStatus = OrderStatus.DRAFT
    approval_status: ApprovalStatus = ApprovalStatus.NOT_REQUIRED


class OrderUpdate(BaseModel):
    """Schema for updating an order"""
    order_name: Optional[str] = None
    campaign_id: Optional[UUID] = None
    advertiser_id: Optional[UUID] = None
    agency_id: Optional[UUID] = None
    sales_rep_id: Optional[UUID] = None
    sales_team: Optional[str] = None
    sales_office: Optional[str] = None
    sales_region: Optional[str] = None
    stations: Optional[List[str]] = None
    cluster: Optional[str] = None
    order_type: Optional[OrderType] = None
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    gross_amount: Optional[Decimal] = None
    net_amount: Optional[Decimal] = None
    total_value: Optional[Decimal] = None
    agency_commission_percent: Optional[Decimal] = None
    agency_commission_amount: Optional[Decimal] = None
    agency_discount: Optional[Decimal] = None
    cash_discount: Optional[Decimal] = None
    trade_barter: Optional[bool] = None
    trade_value: Optional[Decimal] = None
    taxable: Optional[bool] = None
    billing_cycle: Optional[BillingCycle] = None
    invoice_type: Optional[InvoiceType] = None
    coop_sponsor: Optional[str] = None
    coop_percent: Optional[Decimal] = None
    client_po_number: Optional[str] = None
    billing_address: Optional[str] = None
    billing_contact: Optional[str] = None
    billing_contact_email: Optional[str] = None
    billing_contact_phone: Optional[str] = None
    political_class: Optional[PoliticalClass] = None
    political_window_flag: Optional[bool] = None
    contract_reference: Optional[str] = None
    insertion_order_number: Optional[str] = None
    regulatory_notes: Optional[str] = None
    fcc_id: Optional[str] = None
    required_disclaimers: Optional[str] = None
    spot_lengths: Optional[List[int]] = None
    total_spots: Optional[int] = None
    rate_type: Optional[RateType] = None
    rates: Optional[Dict[str, Any]] = None
    status: Optional[OrderStatus] = None
    approval_status: Optional[ApprovalStatus] = None
    traffic_ready: Optional[bool] = None
    billing_ready: Optional[bool] = None
    locked: Optional[bool] = None


class OrderResponse(OrderBase, OrderFinancial, OrderCoop, OrderLegal):
    """Schema for order response"""
    id: UUID
    order_number: str
    status: OrderStatus
    approval_status: ApprovalStatus
    approved_by: Optional[UUID] = None
    approved_at: Optional[datetime] = None
    traffic_ready: bool = False
    billing_ready: bool = False
    locked: bool = False
    revision_number: int = 1
    created_by: Optional[UUID] = None
    created_at: datetime
    updated_at: datetime
    advertiser_name: Optional[str] = None
    agency_name: Optional[str] = None
    sales_rep_name: Optional[str] = None
    spot_lengths: Optional[List[int]] = None
    total_spots: int = 0
    rate_type: RateType
    rates: Optional[Dict[str, Any]] = None

    model_config = ConfigDict(from_attributes=True)


class OrderListResponse(BaseModel):
    """Schema for order list response"""
    id: UUID
    order_number: str
    order_name: Optional[str] = None
    advertiser_id: UUID
    advertiser_name: Optional[str] = None
    agency_id: Optional[UUID] = None
    agency_name: Optional[str] = None
    sales_rep_id: Optional[UUID] = None
    start_date: date
    end_date: date
    total_value: Optional[Decimal] = Decimal("0.00")
    status: OrderStatus
    approval_status: ApprovalStatus
    order_type: Optional[OrderType] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

