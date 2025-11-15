"""
Revenue Service for revenue management
"""

from typing import Dict, Any, Optional
from datetime import date, timedelta
from decimal import Decimal
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func
from backend.models.invoice import Invoice, InvoiceStatus
from backend.models.sales_goal import SalesGoal, PeriodType
from backend.models.sales_rep import SalesRep
import structlog

logger = structlog.get_logger()


class RevenueService:
    """Service for revenue management"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def calculate_revenue(
        self,
        start_date: date,
        end_date: date
    ) -> Dict[str, Any]:
        """Calculate revenue for date range"""
        result = await self.db.execute(
            select(func.sum(Invoice.total)).where(
                and_(
                    Invoice.invoice_date >= start_date,
                    Invoice.invoice_date <= end_date,
                    Invoice.status.in_([InvoiceStatus.SENT, InvoiceStatus.PAID])
                )
            )
        )
        total_revenue = result.scalar() or Decimal("0.00")
        
        result = await self.db.execute(
            select(func.count(Invoice.id)).where(
                and_(
                    Invoice.invoice_date >= start_date,
                    Invoice.invoice_date <= end_date
                )
            )
        )
        invoice_count = result.scalar() or 0
        
        return {
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "total_revenue": float(total_revenue),
            "invoice_count": invoice_count
        }
    
    async def calculate_pacing(
        self,
        start_date: date,
        end_date: date
    ) -> Dict[str, Any]:
        """Calculate revenue pacing vs goals"""
        total_days = (end_date - start_date).days + 1
        days_elapsed = (date.today() - start_date).days + 1
        
        revenue_result = await self.calculate_revenue(start_date, end_date)
        total_revenue = revenue_result["total_revenue"]
        
        # Calculate projected revenue
        if days_elapsed > 0:
            daily_average = total_revenue / days_elapsed
            projected_revenue = daily_average * total_days
        else:
            projected_revenue = 0
        
        return {
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "total_days": total_days,
            "days_elapsed": days_elapsed,
            "current_revenue": total_revenue,
            "projected_revenue": projected_revenue,
            "pacing_percentage": (days_elapsed / total_days * 100) if total_days > 0 else 0
        }
    
    async def calculate_pacing_vs_goals(
        self,
        sales_rep_id: Optional[int] = None
    ) -> Dict[str, Any]:
        """Calculate revenue pacing vs sales goals"""
        query = select(SalesGoal)
        if sales_rep_id:
            query = query.where(SalesGoal.sales_rep_id == sales_rep_id)
        
        result = await self.db.execute(query)
        goals = result.scalars().all()
        
        progress_data = []
        for goal in goals:
            # Get actual revenue for goal period
            if goal.period == PeriodType.MONTHLY:
                start = goal.target_date.replace(day=1)
                end = (start + timedelta(days=32)).replace(day=1) - timedelta(days=1)
            elif goal.period == PeriodType.WEEKLY:
                start = goal.target_date - timedelta(days=goal.target_date.weekday())
                end = start + timedelta(days=6)
            else:
                start = goal.target_date
                end = goal.target_date
            
            revenue = await self.calculate_revenue(start, end)
            actual_revenue = revenue["total_revenue"]
            
            progress_percentage = (actual_revenue / float(goal.goal_amount) * 100) if goal.goal_amount > 0 else 0
            
            progress_data.append({
                "goal_id": goal.id,
                "sales_rep_id": goal.sales_rep_id,
                "period": goal.period.value,
                "target_date": goal.target_date.isoformat(),
                "goal_amount": float(goal.goal_amount),
                "actual_amount": actual_revenue,
                "progress_percentage": progress_percentage
            })
        
        return {
            "goals": progress_data,
            "total_goals": len(goals)
        }
    
    async def generate_forecast(
        self,
        months_ahead: int = 3
    ) -> Dict[str, Any]:
        """Generate revenue forecast"""
        # Simple forecasting based on historical averages
        end_date = date.today()
        start_date = end_date - timedelta(days=90)  # Last 3 months
        
        revenue_result = await self.calculate_revenue(start_date, end_date)
        historical_revenue = revenue_result["total_revenue"]
        
        # Calculate monthly average
        monthly_average = historical_revenue / 3
        
        forecast = []
        for i in range(1, months_ahead + 1):
            forecast_month = (end_date + timedelta(days=30 * i)).replace(day=1)
            forecast.append({
                "month": forecast_month.isoformat(),
                "forecasted_revenue": monthly_average,
                "confidence": "medium"  # Placeholder
            })
        
        return {
            "forecast_months": months_ahead,
            "historical_average": monthly_average,
            "forecast": forecast
        }

