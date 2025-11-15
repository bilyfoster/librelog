"""
Billing Service for invoice generation and management
"""

from typing import Dict, Any, List, Optional
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func
from backend.models.invoice import Invoice, InvoiceStatus
from backend.models.invoice_line import InvoiceLine
from backend.models.spot import Spot, SpotStatus
from backend.models.order import Order
import structlog

logger = structlog.get_logger()


class BillingService:
    """Service for billing and invoice management"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def generate_invoice(
        self,
        advertiser_id: int,
        order_id: Optional[int] = None,
        invoice_date: Optional[date] = None,
        invoice_number: Optional[str] = None
    ) -> Invoice:
        """Auto generate invoice from order"""
        if not invoice_date:
            invoice_date = date.today()
        
        if not invoice_number:
            invoice_number = f"INV-{datetime.now().strftime('%Y%m%d%H%M%S')}"
        
        # Get order if provided
        order = None
        if order_id:
            result = await self.db.execute(select(Order).where(Order.id == order_id))
            order = result.scalar_one_or_none()
        
        # Get spots for this order
        spots = []
        if order:
            result = await self.db.execute(
                select(Spot).where(
                    and_(
                        Spot.order_id == order_id,
                        Spot.status == SpotStatus.AIRED
                    )
                )
            )
            spots = result.scalars().all()
        
        # Create invoice
        invoice = Invoice(
            invoice_number=invoice_number,
            advertiser_id=advertiser_id,
            order_id=order_id,
            invoice_date=invoice_date,
            due_date=invoice_date + timedelta(days=30),  # Default 30 days
            status=InvoiceStatus.DRAFT
        )
        
        self.db.add(invoice)
        await self.db.commit()
        await self.db.refresh(invoice)
        
        # Create invoice lines from spots
        if spots:
            spot_ids = [s.id for s in spots]
            line = InvoiceLine(
                invoice_id=invoice.id,
                description=f"Advertising spots for order {order.order_number if order else 'N/A'}",
                quantity=len(spots),
                unit_price=order.total_value / len(spots) if order and len(spots) > 0 else Decimal("0.00"),
                total=order.total_value if order else Decimal("0.00"),
                spot_ids=spot_ids
            )
            self.db.add(line)
            await self.db.commit()
        
        # Calculate totals
        invoice = await self.calculate_invoice_totals(invoice.id)
        
        logger.info("Invoice generated", invoice_id=invoice.id, order_id=order_id)
        
        return invoice
    
    async def calculate_invoice_totals(self, invoice_id: int) -> Invoice:
        """Calculate invoice subtotal, tax, and total"""
        result = await self.db.execute(select(Invoice).where(Invoice.id == invoice_id))
        invoice = result.scalar_one_or_none()
        
        if not invoice:
            raise ValueError("Invoice not found")
        
        # Sum line totals
        result = await self.db.execute(
            select(func.sum(InvoiceLine.total)).where(InvoiceLine.invoice_id == invoice_id)
        )
        subtotal = result.scalar() or Decimal("0.00")
        
        # Calculate tax (simple 8% for now)
        tax = subtotal * Decimal("0.08")
        total = subtotal + tax
        
        invoice.subtotal = subtotal
        invoice.tax = tax
        invoice.total = total
        
        await self.db.commit()
        await self.db.refresh(invoice)
        
        return invoice
    
    async def calculate_contract_actualization(
        self,
        order_id: int,
        start_date: date,
        end_date: date
    ) -> Dict[str, Any]:
        """Calculate scheduled vs aired spots"""
        # Get scheduled spots
        scheduled_result = await self.db.execute(
            select(Spot).where(
                and_(
                    Spot.order_id == order_id,
                    Spot.scheduled_date >= start_date,
                    Spot.scheduled_date <= end_date
                )
            )
        )
        scheduled_spots = scheduled_result.scalars().all()
        
        # Get aired spots
        aired_result = await self.db.execute(
            select(Spot).where(
                and_(
                    Spot.order_id == order_id,
                    Spot.scheduled_date >= start_date,
                    Spot.scheduled_date <= end_date,
                    Spot.status == SpotStatus.AIRED
                )
            )
        )
        aired_spots = aired_result.scalars().all()
        
        return {
            "order_id": order_id,
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "scheduled_count": len(scheduled_spots),
            "aired_count": len(aired_spots),
            "missed_count": len(scheduled_spots) - len(aired_spots),
            "completion_rate": (len(aired_spots) / len(scheduled_spots) * 100) if scheduled_spots else 0
        }
    
    async def create_makegood_invoice(
        self,
        makegood_id: int,
        invoice_date: Optional[date] = None
    ) -> Invoice:
        """Create invoice for makegood"""
        from backend.models.makegood import Makegood
        
        result = await self.db.execute(select(Makegood).where(Makegood.id == makegood_id))
        makegood = result.scalar_one_or_none()
        
        if not makegood:
            raise ValueError("Makegood not found")
        
        # Get original spot's order
        spot_result = await self.db.execute(select(Spot).where(Spot.id == makegood.original_spot_id))
        original_spot = spot_result.scalar_one_or_none()
        
        if not original_spot or not original_spot.order:
            raise ValueError("Cannot determine order for makegood")
        
        # Create invoice for makegood
        invoice_number = f"MG-{datetime.now().strftime('%Y%m%d%H%M%S')}"
        
        invoice = Invoice(
            invoice_number=invoice_number,
            advertiser_id=original_spot.order.advertiser_id,
            order_id=original_spot.order_id,
            invoice_date=invoice_date or date.today(),
            due_date=(invoice_date or date.today()) + timedelta(days=30),
            status=InvoiceStatus.DRAFT,
            notes=f"Makegood for spot {makegood.original_spot_id}"
        )
        
        self.db.add(invoice)
        await self.db.commit()
        await self.db.refresh(invoice)
        
        # Add line item
        line = InvoiceLine(
            invoice_id=invoice.id,
            description=f"Makegood spot {makegood.makegood_spot_id}",
            quantity=1,
            unit_price=Decimal("0.00"),  # Makegoods are typically free
            total=Decimal("0.00"),
            spot_ids=[makegood.makegood_spot_id]
        )
        self.db.add(line)
        await self.db.commit()
        
        return invoice
    
    async def calculate_aging(self) -> Dict[str, Any]:
        """Calculate AR aging report"""
        result = await self.db.execute(
            select(Invoice).where(
                Invoice.status.in_([InvoiceStatus.SENT, InvoiceStatus.OVERDUE])
            )
        )
        invoices = result.scalars().all()
        
        today = date.today()
        aging_buckets = {
            "current": Decimal("0.00"),  # 0-30 days
            "days_31_60": Decimal("0.00"),
            "days_61_90": Decimal("0.00"),
            "over_90": Decimal("0.00")
        }
        
        for invoice in invoices:
            days_past_due = (today - invoice.due_date).days
            amount = invoice.total - sum(p.amount for p in invoice.payments)
            
            if days_past_due <= 0:
                aging_buckets["current"] += amount
            elif days_past_due <= 30:
                aging_buckets["current"] += amount
            elif days_past_due <= 60:
                aging_buckets["days_31_60"] += amount
            elif days_past_due <= 90:
                aging_buckets["days_61_90"] += amount
            else:
                aging_buckets["over_90"] += amount
        
        return {
            "as_of_date": today.isoformat(),
            "aging_buckets": aging_buckets,
            "total_outstanding": sum(aging_buckets.values())
        }
    
    async def export_to_quickbooks(self, invoice_id: int) -> Dict[str, Any]:
        """Export invoice to QuickBooks format"""
        result = await self.db.execute(select(Invoice).where(Invoice.id == invoice_id))
        invoice = result.scalar_one_or_none()
        
        if not invoice:
            raise ValueError("Invoice not found")
        
        # Format for QuickBooks API
        return {
            "CustomerRef": {"value": str(invoice.advertiser_id)},
            "TxnDate": invoice.invoice_date.isoformat(),
            "DueDate": invoice.due_date.isoformat(),
            "Line": [
                {
                    "Amount": float(line.total),
                    "DetailType": "SalesItemLineDetail",
                    "SalesItemLineDetail": {
                        "ItemRef": {"value": "Advertising"},
                        "Qty": line.quantity,
                        "UnitPrice": float(line.unit_price)
                    }
                }
                for line in invoice.invoice_lines
            ]
        }

