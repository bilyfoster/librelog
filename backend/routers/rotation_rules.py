"""
Rotation Rules router for managing spot rotation
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc
from sqlalchemy.orm import selectinload
from typing import Optional, List, Dict, Any
from pydantic import BaseModel

from backend.database import get_db
from backend.models.rotation_rule import RotationRule, RotationType
from backend.routers.auth import get_current_user
from backend.models.user import User

router = APIRouter(prefix="/rotation-rules", tags=["rotation-rules"])


class RotationRuleCreate(BaseModel):
    name: str
    description: Optional[str] = None
    rotation_type: str = RotationType.SEQUENTIAL.value
    daypart_id: Optional[int] = None
    campaign_id: Optional[int] = None
    min_separation: int = 0
    max_per_hour: Optional[int] = None
    max_per_day: Optional[int] = None
    weights: Optional[Dict[str, Any]] = None
    exclude_days: Optional[List[int]] = None
    exclude_times: Optional[List[Dict[str, str]]] = None
    priority: int = 0


class RotationRuleUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    rotation_type: Optional[str] = None
    daypart_id: Optional[int] = None
    campaign_id: Optional[int] = None
    min_separation: Optional[int] = None
    max_per_hour: Optional[int] = None
    max_per_day: Optional[int] = None
    weights: Optional[Dict[str, Any]] = None
    exclude_days: Optional[List[int]] = None
    exclude_times: Optional[List[Dict[str, str]]] = None
    priority: Optional[int] = None
    active: Optional[bool] = None


class RotationRuleResponse(BaseModel):
    id: int
    name: str
    description: Optional[str]
    rotation_type: str
    daypart_id: Optional[int]
    campaign_id: Optional[int]
    min_separation: int
    max_per_hour: Optional[int]
    max_per_day: Optional[int]
    weights: Optional[Dict[str, Any]]
    exclude_days: Optional[List[int]]
    exclude_times: Optional[List[Dict[str, str]]]
    priority: int
    active: bool
    created_at: str
    updated_at: str
    daypart_name: Optional[str] = None
    campaign_name: Optional[str] = None

    class Config:
        from_attributes = True


@router.get("/", response_model=List[RotationRuleResponse])
async def list_rotation_rules(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(False),
    daypart_id: Optional[int] = None,
    campaign_id: Optional[int] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all rotation rules"""
    query = select(RotationRule).options(
        selectinload(RotationRule.daypart),
        selectinload(RotationRule.campaign)
    )
    
    if active_only:
        query = query.where(RotationRule.active == True)
    
    if daypart_id:
        query = query.where(RotationRule.daypart_id == daypart_id)
    
    if campaign_id:
        query = query.where(RotationRule.campaign_id == campaign_id)
    
    query = query.order_by(desc(RotationRule.priority), RotationRule.name).offset(skip).limit(limit)
    
    result = await db.execute(query)
    rules = result.scalars().all()
    
    # Build response with related names
    response = []
    for rule in rules:
        rule_dict = {
            **{c.name: getattr(rule, c.name) for c in rule.__table__.columns},
            "daypart_name": rule.daypart.name if rule.daypart else None,
            "campaign_name": rule.campaign.name if rule.campaign else None,
        }
        response.append(RotationRuleResponse(**rule_dict))
    
    return response


@router.get("/{rule_id}", response_model=RotationRuleResponse)
async def get_rotation_rule(
    rule_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific rotation rule"""
    result = await db.execute(
        select(RotationRule)
        .options(selectinload(RotationRule.daypart), selectinload(RotationRule.campaign))
        .where(RotationRule.id == rule_id)
    )
    rule = result.scalar_one_or_none()
    
    if not rule:
        raise HTTPException(status_code=404, detail="Rotation rule not found")
    
    rule_dict = {
        **{c.name: getattr(rule, c.name) for c in rule.__table__.columns},
        "daypart_name": rule.daypart.name if rule.daypart else None,
        "campaign_name": rule.campaign.name if rule.campaign else None,
    }
    return RotationRuleResponse(**rule_dict)


@router.post("/", response_model=RotationRuleResponse, status_code=status.HTTP_201_CREATED)
async def create_rotation_rule(
    rule: RotationRuleCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new rotation rule"""
    try:
        # Validate rotation type
        if rule.rotation_type not in [rt.value for rt in RotationType]:
            raise HTTPException(status_code=400, detail=f"Invalid rotation_type: {rule.rotation_type}")
        
        # Validate daypart if provided
        if rule.daypart_id:
            from backend.models.daypart import Daypart
            result = await db.execute(select(Daypart).where(Daypart.id == rule.daypart_id))
            daypart = result.scalar_one_or_none()
            if not daypart:
                raise HTTPException(status_code=404, detail="Daypart not found")
        
        # Validate campaign if provided
        if rule.campaign_id:
            from backend.models.campaign import Campaign
            result = await db.execute(select(Campaign).where(Campaign.id == rule.campaign_id))
            campaign = result.scalar_one_or_none()
            if not campaign:
                raise HTTPException(status_code=404, detail="Campaign not found")
        
        new_rule = RotationRule(**rule.dict())
        db.add(new_rule)
        await db.flush()  # Get ID without committing
        rule_id = new_rule.id
        
        try:
            await db.commit()
        except Exception as commit_error:
            # If commit fails due to relationship loading, refresh and continue
            if "relationship" in str(commit_error).lower() or "Multiple rows" in str(commit_error):
                await db.rollback()
                # Re-fetch the rule with relationships
                result = await db.execute(
                    select(RotationRule)
                    .options(selectinload(RotationRule.daypart), selectinload(RotationRule.campaign))
                    .where(RotationRule.id == rule_id)
                )
                new_rule = result.scalar_one_or_none()
                if not new_rule:
                    raise HTTPException(status_code=500, detail="Rule created but could not be retrieved")
            else:
                await db.rollback()
                raise
        
        # Load relationships for response using eager loading
        result = await db.execute(
            select(RotationRule)
            .options(selectinload(RotationRule.daypart), selectinload(RotationRule.campaign))
            .where(RotationRule.id == rule_id)
        )
        new_rule = result.scalar_one_or_none()
        
        if not new_rule:
            raise HTTPException(status_code=500, detail="Rule created but could not be retrieved")
        
        rule_dict = {
            **{c.name: getattr(new_rule, c.name) for c in new_rule.__table__.columns},
            "daypart_name": new_rule.daypart.name if new_rule.daypart else None,
            "campaign_name": new_rule.campaign.name if new_rule.campaign else None,
        }
        return RotationRuleResponse(**rule_dict)
    except HTTPException:
        raise
    except Exception as e:
        await db.rollback()
        raise HTTPException(status_code=500, detail=f"Failed to create rotation rule: {str(e)}")


@router.put("/{rule_id}", response_model=RotationRuleResponse)
async def update_rotation_rule(
    rule_id: int,
    rule_update: RotationRuleUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a rotation rule"""
    result = await db.execute(select(RotationRule).where(RotationRule.id == rule_id))
    rule = result.scalar_one_or_none()
    
    if not rule:
        raise HTTPException(status_code=404, detail="Rotation rule not found")
    
    # Validate rotation type if provided
    if rule_update.rotation_type and rule_update.rotation_type not in [rt.value for rt in RotationType]:
        raise HTTPException(status_code=400, detail=f"Invalid rotation_type: {rule_update.rotation_type}")
    
    # Validate daypart if provided
    if rule_update.daypart_id is not None:
        from backend.models.daypart import Daypart
        result = await db.execute(select(Daypart).where(Daypart.id == rule_update.daypart_id))
        daypart = result.scalar_one_or_none()
        if not daypart:
            raise HTTPException(status_code=404, detail="Daypart not found")
    
    # Validate campaign if provided
    if rule_update.campaign_id is not None:
        from backend.models.campaign import Campaign
        result = await db.execute(select(Campaign).where(Campaign.id == rule_update.campaign_id))
        campaign = result.scalar_one_or_none()
        if not campaign:
            raise HTTPException(status_code=404, detail="Campaign not found")
    
    # Update fields
    update_data = rule_update.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(rule, field, value)
    
    try:
        await db.commit()
    except Exception as commit_error:
        if "relationship" in str(commit_error).lower() or "Multiple rows" in str(commit_error):
            await db.rollback()
            # Re-fetch with relationships
            result = await db.execute(
                select(RotationRule)
                .options(selectinload(RotationRule.daypart), selectinload(RotationRule.campaign))
                .where(RotationRule.id == rule_id)
            )
            rule = result.scalar_one_or_none()
            if not rule:
                raise HTTPException(status_code=500, detail="Rule updated but could not be retrieved")
        else:
            await db.rollback()
            raise HTTPException(status_code=500, detail=f"Failed to update rotation rule: {str(commit_error)}")
    
    # Re-fetch with relationships for response
    result = await db.execute(
        select(RotationRule)
        .options(selectinload(RotationRule.daypart), selectinload(RotationRule.campaign))
        .where(RotationRule.id == rule_id)
    )
    rule = result.scalar_one_or_none()
    
    if not rule:
        raise HTTPException(status_code=500, detail="Rule updated but could not be retrieved")
    
    rule_dict = {
        **{c.name: getattr(rule, c.name) for c in rule.__table__.columns},
        "daypart_name": rule.daypart.name if rule.daypart else None,
        "campaign_name": rule.campaign.name if rule.campaign else None,
    }
    return RotationRuleResponse(**rule_dict)


@router.delete("/{rule_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_rotation_rule(
    rule_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a rotation rule"""
    result = await db.execute(select(RotationRule).where(RotationRule.id == rule_id))
    rule = result.scalar_one_or_none()
    
    if not rule:
        raise HTTPException(status_code=404, detail="Rotation rule not found")
    
    await db.delete(rule)
    await db.commit()

