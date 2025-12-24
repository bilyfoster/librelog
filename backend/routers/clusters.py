"""
Clusters router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, or_
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.cluster import Cluster
from backend.models.station import Station
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.schemas.cluster import ClusterCreate, ClusterUpdate, ClusterResponse
from typing import Optional, List

router = APIRouter()


@router.get("/", response_model=List[ClusterResponse])
async def list_clusters(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all clusters with optional filtering"""
    query = select(Cluster)
    
    if active_only:
        query = query.where(Cluster.active == True)
    
    if search:
        search_term = f"%{search}%"
        query = query.where(
            or_(
                Cluster.name.ilike(search_term),
                Cluster.description.ilike(search_term)
            )
        )
    
    query = query.options(selectinload(Cluster.stations))
    query = query.offset(skip).limit(limit).order_by(Cluster.name)
    
    result = await db.execute(query)
    clusters = result.scalars().all()
    
    clusters_data = []
    for cluster in clusters:
        cluster_dict = ClusterResponse.model_validate(cluster).model_dump()
        cluster_dict["stations"] = [
            {
                "id": station.id,
                "call_letters": station.call_letters,
                "frequency": station.frequency,
                "market": station.market
            }
            for station in cluster.stations
        ]
        clusters_data.append(ClusterResponse(**cluster_dict))
    
    return clusters_data


@router.get("/{cluster_id}", response_model=ClusterResponse)
async def get_cluster(
    cluster_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific cluster with stations"""
    result = await db.execute(
        select(Cluster)
        .where(Cluster.id == cluster_id)
        .options(selectinload(Cluster.stations))
    )
    cluster = result.scalar_one_or_none()
    
    if not cluster:
        raise HTTPException(status_code=404, detail="Cluster not found")
    
    cluster_dict = ClusterResponse.model_validate(cluster).model_dump()
    cluster_dict["stations"] = [
        {
            "id": station.id,
            "call_letters": station.call_letters,
            "frequency": station.frequency,
            "market": station.market
        }
        for station in cluster.stations
    ]
    return ClusterResponse(**cluster_dict)


@router.post("/", response_model=ClusterResponse, status_code=status.HTTP_201_CREATED)
async def create_cluster(
    cluster: ClusterCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new cluster"""
    # Check if cluster name already exists
    existing = await db.execute(select(Cluster).where(Cluster.name == cluster.name))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Cluster with this name already exists")
    
    new_cluster = Cluster(**cluster.model_dump())
    db.add(new_cluster)
    await db.commit()
    await db.refresh(new_cluster, ["stations"])
    
    cluster_dict = ClusterResponse.model_validate(new_cluster).model_dump()
    cluster_dict["stations"] = [
        {
            "id": station.id,
            "call_letters": station.call_letters,
            "frequency": station.frequency,
            "market": station.market
        }
        for station in new_cluster.stations
    ]
    return ClusterResponse(**cluster_dict)


@router.put("/{cluster_id}", response_model=ClusterResponse)
async def update_cluster(
    cluster_id: UUID,
    cluster_update: ClusterUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a cluster"""
    result = await db.execute(select(Cluster).where(Cluster.id == cluster_id))
    cluster = result.scalar_one_or_none()
    
    if not cluster:
        raise HTTPException(status_code=404, detail="Cluster not found")
    
    # Check name uniqueness if name is being updated
    if cluster_update.name and cluster_update.name != cluster.name:
        existing = await db.execute(select(Cluster).where(Cluster.name == cluster_update.name))
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Cluster with this name already exists")
    
    # Update fields
    update_data = cluster_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(cluster, field, value)
    
    await db.commit()
    await db.refresh(cluster, ["stations"])
    
    cluster_dict = ClusterResponse.model_validate(cluster).model_dump()
    cluster_dict["stations"] = [
        {
            "id": station.id,
            "call_letters": station.call_letters,
            "frequency": station.frequency,
            "market": station.market
        }
        for station in cluster.stations
    ]
    return ClusterResponse(**cluster_dict)


@router.delete("/{cluster_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_cluster(
    cluster_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Soft delete a cluster (set active=False)"""
    result = await db.execute(select(Cluster).where(Cluster.id == cluster_id))
    cluster = result.scalar_one_or_none()
    
    if not cluster:
        raise HTTPException(status_code=404, detail="Cluster not found")
    
    cluster.active = False
    await db.commit()
    
    return None

