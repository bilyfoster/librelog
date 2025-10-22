"""
Clock template service with validation and LibreTime export
"""

from typing import List, Dict, Any, Optional
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update, delete
from sqlalchemy.exc import IntegrityError
from backend.models.clock_template import ClockTemplate
from backend.integrations.libretime_client import libretime_client
import structlog
import json

logger = structlog.get_logger()


class ClockTemplateService:
    """Service for managing clock templates"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def create_template(
        self, 
        name: str, 
        description: Optional[str], 
        json_layout: Dict[str, Any],
        user_id: int
    ) -> ClockTemplate:
        """Create a new clock template with validation"""
        # Validate JSON layout
        self._validate_clock_layout(json_layout)
        
        template = ClockTemplate(
            name=name,
            description=description,
            json_layout=json_layout
        )
        
        try:
            self.db.add(template)
            await self.db.commit()
            await self.db.refresh(template)
            
            logger.info("Clock template created", template_id=template.id, name=name)
            return template
            
        except IntegrityError:
            await self.db.rollback()
            raise ValueError("Template name already exists")
    
    async def get_template(self, template_id: int) -> Optional[ClockTemplate]:
        """Get a clock template by ID"""
        result = await self.db.execute(
            select(ClockTemplate).where(ClockTemplate.id == template_id)
        )
        return result.scalar_one_or_none()
    
    async def list_templates(
        self, 
        skip: int = 0, 
        limit: int = 100,
        search: Optional[str] = None
    ) -> List[ClockTemplate]:
        """List clock templates with optional search"""
        query = select(ClockTemplate)
        
        if search:
            query = query.where(ClockTemplate.name.ilike(f"%{search}%"))
        
        query = query.offset(skip).limit(limit).order_by(ClockTemplate.created_at.desc())
        
        result = await self.db.execute(query)
        return result.scalars().all()
    
    async def update_template(
        self, 
        template_id: int, 
        name: Optional[str] = None,
        description: Optional[str] = None,
        json_layout: Optional[Dict[str, Any]] = None
    ) -> Optional[ClockTemplate]:
        """Update a clock template"""
        template = await self.get_template(template_id)
        if not template:
            return None
        
        if name is not None:
            template.name = name
        if description is not None:
            template.description = description
        if json_layout is not None:
            self._validate_clock_layout(json_layout)
            template.json_layout = json_layout
        
        try:
            await self.db.commit()
            await self.db.refresh(template)
            
            logger.info("Clock template updated", template_id=template_id)
            return template
            
        except IntegrityError:
            await self.db.rollback()
            raise ValueError("Template name already exists")
    
    async def delete_template(self, template_id: int) -> bool:
        """Delete a clock template"""
        template = await self.get_template(template_id)
        if not template:
            return False
        
        await self.db.delete(template)
        await self.db.commit()
        
        logger.info("Clock template deleted", template_id=template_id)
        return True
    
    async def generate_preview(self, template_id: int) -> Dict[str, Any]:
        """Generate a preview of what an hour would look like"""
        template = await self.get_template(template_id)
        if not template:
            raise ValueError("Template not found")
        
        layout = template.json_layout
        preview = {
            "template_id": template_id,
            "template_name": template.name,
            "hour": layout.get("hour", "00:00"),
            "elements": [],
            "total_duration": 0,
            "timeline": []
        }
        
        current_time = 0
        timeline = []
        
        for element in layout.get("elements", []):
            element_preview = {
                "type": element.get("type"),
                "title": element.get("title", ""),
                "count": element.get("count", 1),
                "duration": self._estimate_element_duration(element),
                "start_time": current_time,
                "fallback": element.get("fallback")
            }
            
            preview["elements"].append(element_preview)
            preview["total_duration"] += element_preview["duration"]
            
            # Add to timeline
            timeline.append({
                "time": f"{current_time // 60:02d}:{current_time % 60:02d}",
                "element": element_preview
            })
            
            current_time += element_preview["duration"]
        
        preview["timeline"] = timeline
        return preview
    
    async def export_to_libretime(self, template_id: int) -> bool:
        """Export clock template to LibreTime as Smart Block"""
        template = await self.get_template(template_id)
        if not template:
            return False
        
        # Convert to LibreTime Smart Block format
        smart_block_data = self._convert_to_smart_block(template.json_layout)
        
        # Push to LibreTime
        success = await libretime_client.create_smart_block(smart_block_data)
        
        if success:
            logger.info("Clock template exported to LibreTime", template_id=template_id)
        
        return success
    
    def _validate_clock_layout(self, layout: Dict[str, Any]) -> None:
        """Validate clock template layout"""
        if not isinstance(layout, dict):
            raise ValueError("Layout must be a dictionary")
        
        if "hour" not in layout:
            raise ValueError("Layout must include 'hour' field")
        
        if "elements" not in layout:
            raise ValueError("Layout must include 'elements' array")
        
        elements = layout["elements"]
        if not isinstance(elements, list):
            raise ValueError("Elements must be an array")
        
        valid_types = {"MUS", "ADV", "PSA", "LIN", "INT", "PRO", "BED"}
        
        for i, element in enumerate(elements):
            if not isinstance(element, dict):
                raise ValueError(f"Element {i} must be a dictionary")
            
            if "type" not in element:
                raise ValueError(f"Element {i} must have a 'type' field")
            
            if element["type"] not in valid_types:
                raise ValueError(f"Element {i} has invalid type: {element['type']}")
            
            if element["type"] == "MUS" and "count" not in element:
                raise ValueError(f"Music element {i} must have a 'count' field")
    
    def _estimate_element_duration(self, element: Dict[str, Any]) -> int:
        """Estimate duration for a clock element in seconds"""
        element_type = element.get("type")
        
        # Default durations by type
        durations = {
            "MUS": 180,  # 3 minutes average
            "ADV": 60,   # 1 minute
            "PSA": 45,   # 45 seconds
            "LIN": 30,   # 30 seconds
            "INT": 300,  # 5 minutes
            "PRO": 30,   # 30 seconds
            "BED": 15    # 15 seconds
        }
        
        base_duration = durations.get(element_type, 30)
        
        # Adjust for count (for music elements)
        if element_type == "MUS" and "count" in element:
            count = element["count"]
            if count > 1:
                base_duration = base_duration * count
        
        return base_duration
    
    def _convert_to_smart_block(self, layout: Dict[str, Any]) -> Dict[str, Any]:
        """Convert clock template to LibreTime Smart Block format"""
        smart_block = {
            "name": f"Clock Template - {layout.get('hour', '00:00')}",
            "description": f"Generated from LibreLog clock template",
            "type": "smart_block",
            "items": []
        }
        
        for element in layout.get("elements", []):
            item = {
                "type": element["type"],
                "title": element.get("title", ""),
                "count": element.get("count", 1),
                "fallback": element.get("fallback")
            }
            smart_block["items"].append(item)
        
        return smart_block
