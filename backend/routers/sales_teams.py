"""
Sales Teams router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, or_
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.sales_team import SalesTeam
from backend.models.sales_rep import SalesRep
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.schemas.sales_team import SalesTeamCreate, SalesTeamUpdate, SalesTeamResponse, SalesTeamWithReps
from typing import Optional, List

router = APIRouter()


@router.get("/", response_model=List[SalesTeamResponse])
async def list_sales_teams(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all sales teams with optional filtering"""
    query = select(SalesTeam)
    
    if active_only:
        query = query.where(SalesTeam.active == True)
    
    if search:
        search_term = f"%{search}%"
        query = query.where(
            or_(
                SalesTeam.name.ilike(search_term),
                SalesTeam.description.ilike(search_term)
            )
        )
    
    query = query.offset(skip).limit(limit).order_by(SalesTeam.name)
    
    result = await db.execute(query)
    teams = result.scalars().all()
    
    return [SalesTeamResponse.model_validate(team) for team in teams]


@router.get("/{team_id}", response_model=SalesTeamWithReps)
async def get_sales_team(
    team_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific sales team with sales reps"""
    result = await db.execute(
        select(SalesTeam)
        .where(SalesTeam.id == team_id)
        .options(selectinload(SalesTeam.sales_reps).selectinload(SalesRep.user))
    )
    team = result.scalar_one_or_none()
    
    if not team:
        raise HTTPException(status_code=404, detail="Sales team not found")
    
    team_data = SalesTeamWithReps.model_validate(team)
    team_data.sales_reps = [
        {
            "id": rep.id,
            "user_id": rep.user_id,
            "username": rep.user.username if rep.user else None,
            "employee_id": rep.employee_id,
            "active": rep.active
        }
        for rep in team.sales_reps
    ]
    
    return team_data


@router.post("/", response_model=SalesTeamResponse, status_code=status.HTTP_201_CREATED)
async def create_sales_team(
    team: SalesTeamCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new sales team"""
    # Check if team name already exists
    existing = await db.execute(select(SalesTeam).where(SalesTeam.name == team.name))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Sales team with this name already exists")
    
    new_team = SalesTeam(**team.model_dump())
    db.add(new_team)
    await db.commit()
    await db.refresh(new_team)
    
    return SalesTeamResponse.model_validate(new_team)


@router.put("/{team_id}", response_model=SalesTeamResponse)
async def update_sales_team(
    team_id: UUID,
    team_update: SalesTeamUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a sales team"""
    result = await db.execute(select(SalesTeam).where(SalesTeam.id == team_id))
    team = result.scalar_one_or_none()
    
    if not team:
        raise HTTPException(status_code=404, detail="Sales team not found")
    
    # Check name uniqueness if name is being updated
    if team_update.name and team_update.name != team.name:
        existing = await db.execute(select(SalesTeam).where(SalesTeam.name == team_update.name))
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Sales team with this name already exists")
    
    # Update fields
    update_data = team_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(team, field, value)
    
    await db.commit()
    await db.refresh(team)
    
    return SalesTeamResponse.model_validate(team)


@router.delete("/{team_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_sales_team(
    team_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Soft delete a sales team (set active=False)"""
    result = await db.execute(select(SalesTeam).where(SalesTeam.id == team_id))
    team = result.scalar_one_or_none()
    
    if not team:
        raise HTTPException(status_code=404, detail="Sales team not found")
    
    team.active = False
    await db.commit()
    
    return None


@router.post("/{team_id}/sales-reps/{sales_rep_id}", status_code=status.HTTP_204_NO_CONTENT)
async def add_sales_rep_to_team(
    team_id: UUID,
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Add a sales rep to a team"""
    from backend.models.sales_associations import sales_rep_teams
    
    team_result = await db.execute(select(SalesTeam).where(SalesTeam.id == team_id))
    team = team_result.scalar_one_or_none()
    if not team:
        raise HTTPException(status_code=404, detail="Sales team not found")
    
    rep_result = await db.execute(select(SalesRep).where(SalesRep.id == sales_rep_id))
    rep = rep_result.scalar_one_or_none()
    if not rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    # Check if already in team
    if rep in team.sales_reps:
        raise HTTPException(status_code=400, detail="Sales rep already in this team")
    
    team.sales_reps.append(rep)
    await db.commit()
    
    return None


@router.delete("/{team_id}/sales-reps/{sales_rep_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_sales_rep_from_team(
    team_id: UUID,
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Remove a sales rep from a team"""
    team_result = await db.execute(
        select(SalesTeam)
        .where(SalesTeam.id == team_id)
        .options(selectinload(SalesTeam.sales_reps))
    )
    team = team_result.scalar_one_or_none()
    if not team:
        raise HTTPException(status_code=404, detail="Sales team not found")
    
    rep_result = await db.execute(select(SalesRep).where(SalesRep.id == sales_rep_id))
    rep = rep_result.scalar_one_or_none()
    if not rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    if rep not in team.sales_reps:
        raise HTTPException(status_code=400, detail="Sales rep not in this team")
    
    team.sales_reps.remove(rep)
    await db.commit()
    
    return None

