"""
Payments router for billing
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
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
    invoice_id: UUID
    amount: Decimal
    payment_date: datetime
    payment_method: Optional[str] = None
    reference_number: Optional[str] = None
    notes: Optional[str] = None


class PaymentResponse(BaseModel):
    id: UUID
    invoice_id: UUID
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
    invoice_id: Optional[UUID] = Query(None),
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
    
    # Convert datetime objects to strings for response
    payments_data = []
    for p in payments:
        payment_dict = {
            "id": p.id,
            "invoice_id": p.invoice_id,
            "amount": p.amount,
            "payment_date": p.payment_date.isoformat() if p.payment_date else "",
            "payment_method": p.payment_method,
            "reference_number": p.reference_number,
            "notes": p.notes,
            "created_at": p.created_at.isoformat() if p.created_at else "",
        }
        payments_data.append(PaymentResponse(**payment_dict))
    
    return payments_data


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
    
    # Convert datetime objects to strings for response
    payment_dict = {
        "id": new_payment.id,
        "invoice_id": new_payment.invoice_id,
        "amount": new_payment.amount,
        "payment_date": new_payment.payment_date.isoformat() if new_payment.payment_date else "",
        "payment_method": new_payment.payment_method,
        "reference_number": new_payment.reference_number,
        "notes": new_payment.notes,
        "created_at": new_payment.created_at.isoformat() if new_payment.created_at else "",
    }
    return PaymentResponse(**payment_dict)


@router.get("/{payment_id}", response_model=PaymentResponse)
async def get_payment(
    payment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific payment"""
    result = await db.execute(select(Payment).where(Payment.id == payment_id))
    payment = result.scalar_one_or_none()
    
    if not payment:
        raise HTTPException(status_code=404, detail="Payment not found")
    
    # Convert datetime objects to strings for response
    payment_dict = {
        "id": payment.id,
        "invoice_id": payment.invoice_id,
        "amount": payment.amount,
        "payment_date": payment.payment_date.isoformat() if payment.payment_date else "",
        "payment_method": payment.payment_method,
        "reference_number": payment.reference_number,
        "notes": payment.notes,
        "created_at": payment.created_at.isoformat() if payment.created_at else "",
    }
    return PaymentResponse(**payment_dict)


@router.put("/{payment_id}", response_model=PaymentResponse)
async def update_payment(
    payment_id: UUID,
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
    
    # Convert datetime objects to strings for response
    payment_dict = {
        "id": payment.id,
        "invoice_id": payment.invoice_id,
        "amount": payment.amount,
        "payment_date": payment.payment_date.isoformat() if payment.payment_date else "",
        "payment_method": payment.payment_method,
        "reference_number": payment.reference_number,
        "notes": payment.notes,
        "created_at": payment.created_at.isoformat() if payment.created_at else "",
    }
    return PaymentResponse(**payment_dict)


@router.delete("/{payment_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_payment(
    payment_id: UUID,
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

