"""
Inventory Service for inventory management
"""

from typing import Dict, Any, List
from datetime import date, timedelta
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func
from backend.models.inventory_slot import InventorySlot
from backend.models.spot import Spot, SpotStatus
import structlog

logger = structlog.get_logger()


class InventoryService:
    """Service for inventory management"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def calculate_sellout(
        self,
        start_date: date,
        end_date: date
    ) -> Dict[str, Any]:
        """Calculate sellout percentages"""
        result = await self.db.execute(
            select(
                InventorySlot.date,
                InventorySlot.hour,
                func.sum(InventorySlot.booked).label("total_booked"),
                func.sum(InventorySlot.available).label("total_available")
            ).where(
                and_(
                    InventorySlot.date >= start_date,
                    InventorySlot.date <= end_date
                )
            ).group_by(InventorySlot.date, InventorySlot.hour)
        )
        
        sellout_data = []
        for row in result.all():
            sellout_percentage = (row.total_booked / row.total_available * 100) if row.total_available > 0 else 0
            sellout_data.append({
                "date": row.date.isoformat(),
                "hour": row.hour,
                "sellout_percentage": sellout_percentage,
                "booked": row.total_booked,
                "available": row.total_available
            })
        
        return {
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "sellout_data": sellout_data
        }
    
    async def generate_heatmap(
        self,
        start_date: date,
        end_date: date
    ) -> Dict[str, Any]:
        """Generate inventory heatmap data"""
        result = await self.db.execute(
            select(InventorySlot).where(
                and_(
                    InventorySlot.date >= start_date,
                    InventorySlot.date <= end_date
                )
            )
        )
        slots = result.scalars().all()
        
        # Organize by date and hour
        heatmap_data = {}
        for slot in slots:
            date_key = slot.date.isoformat()
            if date_key not in heatmap_data:
                heatmap_data[date_key] = {}
            
            hour_key = f"{slot.hour:02d}:00"
            sellout_percentage = (slot.booked / slot.available * 100) if slot.available > 0 else 0
            
            heatmap_data[date_key][hour_key] = {
                "sellout_percentage": sellout_percentage,
                "booked": slot.booked,
                "available": slot.available,
                "revenue": float(slot.revenue)
            }
        
        return {
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "heatmap": heatmap_data
        }
    
    async def forecast_inventory(
        self,
        start_date: date,
        days_ahead: int = 30
    ) -> Dict[str, Any]:
        """Forecast inventory availability"""
        # Simple forecasting based on historical data
        end_date = start_date - timedelta(days=30)
        
        result = await self.db.execute(
            select(
                InventorySlot.hour,
                func.avg(InventorySlot.booked).label("avg_booked"),
                func.avg(InventorySlot.available).label("avg_available")
            ).where(
                and_(
                    InventorySlot.date >= end_date,
                    InventorySlot.date < start_date
                )
            ).group_by(InventorySlot.hour)
        )
        
        forecast = {}
        for row in result.all():
            forecast[f"{row.hour:02d}:00"] = {
                "forecasted_booked": int(row.avg_booked),
                "forecasted_available": int(row.avg_available),
                "forecasted_sellout": (row.avg_booked / row.avg_available * 100) if row.avg_available > 0 else 0
            }
        
        return {
            "start_date": start_date.isoformat(),
            "days_ahead": days_ahead,
            "forecast": forecast
        }
    
    async def get_inventory_by_date_range(
        self,
        start_date: date,
        end_date: date
    ) -> Dict[str, Any]:
        """Get inventory by date range"""
        result = await self.db.execute(
            select(InventorySlot).where(
                and_(
                    InventorySlot.date >= start_date,
                    InventorySlot.date <= end_date
                )
            ).order_by(InventorySlot.date, InventorySlot.hour)
        )
        slots = result.scalars().all()
        
        return {
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "slots": [
                {
                    "id": s.id,
                    "date": s.date.isoformat(),
                    "hour": s.hour,
                    "break_position": s.break_position,
                    "daypart": s.daypart,
                    "available": s.available,
                    "booked": s.booked,
                    "sold_out": s.sold_out,
                    "revenue": float(s.revenue)
                }
                for s in slots
            ]
        }

