"""
Payments router for billing
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.payment import Payment
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, Dict, Any
from datetime import datetime
from decimal import Decimal

router = APIRouter()


class PaymentCreate(BaseModel):
    invoice_id: int
    amount: Decimal
    payment_date: datetime
    payment_method: Optional[str] = None
    reference_number: Optional[str] = None
    notes: Optional[str] = None


class PaymentResponse(BaseModel):
    id: int
    invoice_id: int
    amount: Decimal
    payment_date: str
    payment_method: Optional[str]
    reference_number: Optional[str]
    notes: Optional[str]
    created_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=list[PaymentResponse])
async def list_payments(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    invoice_id: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all payments"""
    query = select(Payment)
    
    if invoice_id:
        query = query.where(Payment.invoice_id == invoice_id)
    
    query = query.offset(skip).limit(limit).order_by(Payment.payment_date.desc())
    
    result = await db.execute(query)
    payments = result.scalars().all()
    
    return [PaymentResponse.model_validate(p) for p in payments]


@router.post("/", response_model=PaymentResponse, status_code=status.HTTP_201_CREATED)
async def create_payment(
    payment: PaymentCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new payment"""
    new_payment = Payment(**payment.model_dump())
    db.add(new_payment)
    await db.commit()
    await db.refresh(new_payment)
    
    return PaymentResponse.model_validate(new_payment)


@router.get("/{payment_id}", response_model=PaymentResponse)
async def get_payment(
    payment_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific payment"""
    result = await db.execute(select(Payment).where(Payment.id == payment_id))
    payment = result.scalar_one_or_none()
    
    if not payment:
        raise HTTPException(status_code=404, detail="Payment not found")
    
    return PaymentResponse.model_validate(payment)


@router.put("/{payment_id}", response_model=PaymentResponse)
async def update_payment(
    payment_id: int,
    payment_update: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a payment"""
    result = await db.execute(select(Payment).where(Payment.id == payment_id))
    payment = result.scalar_one_or_none()
    
    if not payment:
        raise HTTPException(status_code=404, detail="Payment not found")
    
    for field, value in payment_update.items():
        setattr(payment, field, value)
    
    await db.commit()
    await db.refresh(payment)
    
    return PaymentResponse.model_validate(payment)


@router.delete("/{payment_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_payment(
    payment_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a payment"""
    result = await db.execute(select(Payment).where(Payment.id == payment_id))
    payment = result.scalar_one_or_none()
    
    if not payment:
        raise HTTPException(status_code=404, detail="Payment not found")
    
    await db.delete(payment)
    await db.commit()
    
    return None

