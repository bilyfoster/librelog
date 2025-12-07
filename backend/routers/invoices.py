"""
Invoices router for billing
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query, Request
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.invoice import Invoice, InvoiceStatus
from backend.models.invoice_line import InvoiceLine
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.billing_service import BillingService
from backend.services.notification_service import NotificationService
from backend.services.report_service import ReportService
from backend.models.notification import NotificationType
from backend.logging.audit import audit_logger
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import date
from decimal import Decimal
import structlog
import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from email.mime.base import MIMEBase
from email import encoders

logger = structlog.get_logger()

router = APIRouter()


class InvoiceLineCreate(BaseModel):
    description: str
    quantity: int = 1
    unit_price: Decimal
    spot_ids: Optional[List[int]] = None


class InvoiceCreate(BaseModel):
    invoice_number: str
    advertiser_id: int
    agency_id: Optional[int] = None
    order_id: Optional[int] = None
    campaign_id: Optional[int] = None
    invoice_date: date
    due_date: date
    payment_terms: Optional[str] = None
    notes: Optional[str] = None
    lines: Optional[List[InvoiceLineCreate]] = None


class InvoiceResponse(BaseModel):
    id: int
    invoice_number: str
    advertiser_id: int
    agency_id: Optional[int]
    order_id: Optional[int]
    campaign_id: Optional[int]
    invoice_date: date
    due_date: date
    subtotal: Decimal
    tax: Decimal
    total: Decimal
    status: str
    payment_terms: Optional[str]
    notes: Optional[str]
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


def invoice_to_response_dict(invoice: Invoice) -> dict:
    """Convert Invoice model to InvoiceResponse dict with proper datetime serialization"""
    return {
        "id": invoice.id,
        "invoice_number": invoice.invoice_number,
        "advertiser_id": invoice.advertiser_id,
        "agency_id": invoice.agency_id,
        "order_id": invoice.order_id,
        "campaign_id": invoice.campaign_id,
        "invoice_date": invoice.invoice_date,
        "due_date": invoice.due_date,
        "subtotal": invoice.subtotal,
        "tax": invoice.tax,
        "total": invoice.total,
        "status": invoice.status.value if invoice.status else None,
        "payment_terms": invoice.payment_terms,
        "notes": invoice.notes,
        "created_at": invoice.created_at.isoformat() if invoice.created_at else None,
        "updated_at": invoice.updated_at.isoformat() if invoice.updated_at else None
    }


@router.get("/", response_model=list[InvoiceResponse])
async def list_invoices(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    advertiser_id: Optional[int] = Query(None),
    status_filter: Optional[str] = Query(None),
    station_id: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all invoices with optional filtering"""
    query = select(Invoice)
    
    if advertiser_id:
        query = query.where(Invoice.advertiser_id == advertiser_id)
    
    if status_filter:
        try:
            status_enum = InvoiceStatus[status_filter.upper()]
            query = query.where(Invoice.status == status_enum)
        except KeyError:
            pass
    
    # Filter by station via invoice_lines
    if station_id is not None:
        from backend.models.invoice_line import InvoiceLine
        query = query.join(InvoiceLine).where(InvoiceLine.station_id == station_id).distinct()
    
    query = query.offset(skip).limit(limit).order_by(Invoice.invoice_date.desc())
    
    result = await db.execute(query)
    invoices = result.scalars().all()
    
    return [InvoiceResponse(**invoice_to_response_dict(inv)) for inv in invoices]


@router.post("/", response_model=InvoiceResponse, status_code=status.HTTP_201_CREATED)
async def create_invoice(
    invoice: InvoiceCreate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new invoice (manual or auto)"""
    billing_service = BillingService(db)
    
    # Check if invoice number is unique
    existing = await db.execute(select(Invoice).where(Invoice.invoice_number == invoice.invoice_number))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Invoice number already exists")
    
    invoice_data = invoice.model_dump(exclude={"lines"})
    invoice_data["status"] = InvoiceStatus.DRAFT
    
    new_invoice = Invoice(**invoice_data)
    db.add(new_invoice)
    await db.commit()
    await db.refresh(new_invoice)
    
    # Add invoice lines if provided
    if invoice.lines:
        for line_data in invoice.lines:
            line = InvoiceLine(
                invoice_id=new_invoice.id,
                **line_data.model_dump()
            )
            db.add(line)
        
        await db.commit()
        # Recalculate totals
        new_invoice = await billing_service.calculate_invoice_totals(new_invoice.id)
    
    # Log audit action
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    await audit_logger.log_action(
        db_session=db,
        user_id=current_user.id,
        action="CREATE_INVOICE",
        resource_type="Invoice",
        resource_id=new_invoice.id,
        details={
            "invoice_number": new_invoice.invoice_number,
            "advertiser_id": new_invoice.advertiser_id,
            "total_amount": float(new_invoice.total_amount) if new_invoice.total_amount else 0
        },
        ip_address=client_ip,
        user_agent=user_agent
    )
    
    return InvoiceResponse(**invoice_to_response_dict(new_invoice))


@router.get("/{invoice_id}", response_model=InvoiceResponse)
async def get_invoice(
    invoice_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific invoice with PDF"""
    result = await db.execute(select(Invoice).where(Invoice.id == invoice_id))
    invoice = result.scalar_one_or_none()
    
    if not invoice:
        raise HTTPException(status_code=404, detail="Invoice not found")
    
    return InvoiceResponse(**invoice_to_response_dict(invoice))


@router.put("/{invoice_id}", response_model=InvoiceResponse)
async def update_invoice(
    invoice_id: int,
    invoice_update: Dict[str, Any],
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update an invoice"""
    result = await db.execute(select(Invoice).where(Invoice.id == invoice_id))
    invoice = result.scalar_one_or_none()
    
    if not invoice:
        raise HTTPException(status_code=404, detail="Invoice not found")
    
    if "status" in invoice_update:
        invoice_update["status"] = InvoiceStatus[invoice_update["status"]]
    
    for field, value in invoice_update.items():
        setattr(invoice, field, value)
    
    await db.commit()
    await db.refresh(invoice)
    
    # Log audit action
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    await audit_logger.log_action(
        db_session=db,
        user_id=current_user.id,
        action="UPDATE_INVOICE",
        resource_type="Invoice",
        resource_id=invoice.id,
        details={
            "invoice_number": invoice.invoice_number,
            "updated_fields": list(invoice_update.keys()),
            "status": invoice.status.value if invoice.status else None
        },
        ip_address=client_ip,
        user_agent=user_agent
    )
    
    return InvoiceResponse(**invoice_to_response_dict(invoice))


@router.post("/{invoice_id}/send")
async def send_invoice(
    invoice_id: int,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Email invoice"""
    from backend.models.advertiser import Advertiser
    
    result = await db.execute(select(Invoice).where(Invoice.id == invoice_id))
    invoice = result.scalar_one_or_none()
    
    if not invoice:
        raise HTTPException(status_code=404, detail="Invoice not found")
    
    # Get advertiser email
    advertiser_result = await db.execute(
        select(Advertiser).where(Advertiser.id == invoice.advertiser_id)
    )
    advertiser = advertiser_result.scalar_one_or_none()
    
    if not advertiser:
        raise HTTPException(status_code=404, detail="Advertiser not found")
    
    # Get email from advertiser (assuming email field exists)
    advertiser_email = getattr(advertiser, 'email', None) or getattr(advertiser, 'contact_email', None)
    
    if not advertiser_email:
        raise HTTPException(
            status_code=400,
            detail="Advertiser email not found. Please add email address to advertiser."
        )
    
    # Generate PDF invoice
    report_service = ReportService(db)
    try:
        # Generate invoice PDF
        pdf_result = await report_service.export_report(
            "invoice",
            "pdf",
            {"invoice_id": invoice_id}
        )
        
        pdf_path = pdf_result.get("file_path")
        
        # Send email with PDF attachment
        smtp_host = os.getenv("SMTP_HOST", "smtp.gmail.com")  # Default to Gmail SMTP instead of localhost
        smtp_port = int(os.getenv("SMTP_PORT", "587"))
        smtp_user = os.getenv("SMTP_USER")
        smtp_password = os.getenv("SMTP_PASSWORD")
        smtp_from = os.getenv("SMTP_FROM", "noreply@librelog.local")
        
        if not smtp_user or not smtp_password:
            raise HTTPException(
                status_code=500,
                detail="SMTP not configured. Please configure SMTP settings in admin panel."
            )
        
        # Create email message
        msg = MIMEMultipart()
        msg['From'] = smtp_from
        msg['To'] = advertiser_email
        msg['Subject'] = f"Invoice {invoice.invoice_number} from LibreLog"
        
        # Email body
        body = f"""
Dear {advertiser.name or 'Valued Customer'},

Please find attached invoice {invoice.invoice_number} for your records.

Invoice Details:
- Invoice Number: {invoice.invoice_number}
- Invoice Date: {invoice.invoice_date}
- Due Date: {invoice.due_date}
- Total Amount: ${invoice.total:,.2f}
- Payment Terms: {invoice.payment_terms or 'Net 30'}

{invoice.notes or ''}

Thank you for your business!

LibreLog Billing Team
"""
        msg.attach(MIMEText(body, 'plain'))
        
        # Attach PDF
        if pdf_path and os.path.exists(pdf_path):
            with open(pdf_path, "rb") as attachment:
                part = MIMEBase('application', 'octet-stream')
                part.set_payload(attachment.read())
                encoders.encode_base64(part)
                part.add_header(
                    'Content-Disposition',
                    f'attachment; filename=invoice_{invoice.invoice_number}.pdf'
                )
                msg.attach(part)
        
        # Send email
        with smtplib.SMTP(smtp_host, smtp_port) as server:
            server.starttls()
            server.login(smtp_user, smtp_password)
            server.send_message(msg)
        
        # Update invoice status
        invoice.status = InvoiceStatus.SENT
        await db.commit()
        
        # Log audit action
        client_ip = request.client.host if request.client else None
        user_agent = request.headers.get("user-agent")
        await audit_logger.log_action(
            db_session=db,
            user_id=current_user.id,
            action="SEND_INVOICE",
            resource_type="Invoice",
            resource_id=invoice.id,
            details={
                "invoice_number": invoice.invoice_number,
                "recipient_email": advertiser_email
            },
            ip_address=client_ip,
            user_agent=user_agent
        )
        
        # Create notification record
        await NotificationService.create_notification(
            db,
            current_user.id,
            f"Invoice {invoice.invoice_number} sent to {advertiser_email}",
            NotificationType.IN_APP,
            subject="Invoice Sent",
            metadata={
                "invoice_id": invoice.id,
                "invoice_number": invoice.invoice_number,
                "recipient_email": advertiser_email
            }
        )
        
        return {"message": f"Invoice sent successfully to {advertiser_email}"}
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Invoice email failed", invoice_id=invoice_id, error=str(e), exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to send invoice email: {str(e)}"
        )


@router.post("/{invoice_id}/mark-paid")
async def mark_invoice_paid(
    invoice_id: int,
    payment_data: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Record payment for invoice"""
    from backend.models.payment import Payment
    from datetime import datetime, timezone
    
    result = await db.execute(select(Invoice).where(Invoice.id == invoice_id))
    invoice = result.scalar_one_or_none()
    
    if not invoice:
        raise HTTPException(status_code=404, detail="Invoice not found")
    
    payment = Payment(
        invoice_id=invoice_id,
        amount=Decimal(str(payment_data.get("amount", invoice.total))),
        payment_date=datetime.now(timezone.utc),
        payment_method=payment_data.get("payment_method"),
        reference_number=payment_data.get("reference_number"),
        notes=payment_data.get("notes")
    )
    db.add(payment)
    
    # Update invoice status if fully paid
    total_paid = sum(p.amount for p in invoice.payments) + payment.amount
    if total_paid >= invoice.total:
        invoice.status = InvoiceStatus.PAID
    
    await db.commit()
    await db.refresh(invoice)
    
    return {"message": "Payment recorded", "invoice_status": invoice.status.value}


@router.delete("/{invoice_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_invoice(
    invoice_id: int,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete an invoice (soft delete by setting status to CANCELLED or hard delete)"""
    result = await db.execute(select(Invoice).where(Invoice.id == invoice_id))
    invoice = result.scalar_one_or_none()
    
    if not invoice:
        raise HTTPException(status_code=404, detail="Invoice not found")
    
    # Check if invoice can be deleted (only DRAFT invoices can be deleted)
    if invoice.status != InvoiceStatus.DRAFT:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot delete invoice with status {invoice.status.value}. Only DRAFT invoices can be deleted."
        )
    
    # Delete invoice lines first
    from sqlalchemy import delete
    await db.execute(delete(InvoiceLine).where(InvoiceLine.invoice_id == invoice_id))
    
    # Delete invoice
    await db.delete(invoice)
    await db.commit()
    
    # Log audit action
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    await audit_logger.log_action(
        db_session=db,
        user_id=current_user.id,
        action="DELETE_INVOICE",
        resource_type="Invoice",
        resource_id=invoice_id,
        details={
            "invoice_number": invoice.invoice_number,
            "advertiser_id": invoice.advertiser_id
        },
        ip_address=client_ip,
        user_agent=user_agent
    )
    
    return None


@router.get("/aging", response_model=Dict[str, Any])
async def get_aging_report(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get AR aging report"""
    from backend.services.billing_service import BillingService
    
    billing_service = BillingService(db)
    aging = await billing_service.calculate_aging()
    
    return aging

