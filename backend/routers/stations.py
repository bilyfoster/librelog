"""
Stations router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, or_
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.station import Station
from backend.models.cluster import Cluster
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.schemas.station import StationCreate, StationUpdate, StationResponse
from typing import Optional, List

router = APIRouter()


@router.get("/", response_model=List[StationResponse])
async def list_stations(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    cluster_id: Optional[UUID] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all stations with optional filtering"""
    query = select(Station)
    
    if active_only:
        query = query.where(Station.active == True)
    
    if cluster_id:
        query = query.join(Station.clusters).where(Cluster.id == cluster_id)
    
    if search:
        search_term = f"%{search}%"
        query = query.where(
            or_(
                Station.call_letters.ilike(search_term),
                Station.frequency.ilike(search_term),
                Station.market.ilike(search_term),
                Station.format.ilike(search_term)
            )
        )
    
    query = query.options(selectinload(Station.clusters))
    query = query.offset(skip).limit(limit).order_by(Station.call_letters)
    
    result = await db.execute(query)
    stations = result.scalars().all()
    
    stations_data = []
    for station in stations:
        # Manually construct response to avoid lazy loading issues
        station_dict = {
            "id": station.id,
            "call_letters": station.call_letters,
            "frequency": station.frequency,
            "market": station.market,
            "format": station.format,
            "ownership": station.ownership,
            "contacts": station.contacts,
            "rates": station.rates,
            "inventory_class": station.inventory_class,
            "active": station.active,
            "created_at": station.created_at,
            "updated_at": station.updated_at,
            "libretime_config": station.libretime_config,
            "clusters": [
                {"id": cluster.id, "name": cluster.name}
                for cluster in station.clusters
            ]
        }
        stations_data.append(StationResponse(**station_dict))
    
    return stations_data


@router.get("/{station_id}", response_model=StationResponse)
async def get_station(
    station_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific station"""
    result = await db.execute(
        select(Station)
        .where(Station.id == station_id)
        .options(selectinload(Station.clusters))
    )
    station = result.scalar_one_or_none()
    
    if not station:
        raise HTTPException(status_code=404, detail="Station not found")
    
    # Manually construct response to avoid lazy loading issues
    station_dict = {
        "id": station.id,
        "call_letters": station.call_letters,
        "frequency": station.frequency,
        "market": station.market,
        "format": station.format,
        "ownership": station.ownership,
        "contacts": station.contacts,
        "rates": station.rates,
        "inventory_class": station.inventory_class,
        "active": station.active,
        "created_at": station.created_at,
        "updated_at": station.updated_at,
        "clusters": [
            {"id": cluster.id, "name": cluster.name}
            for cluster in station.clusters
        ]
    }
    return StationResponse(**station_dict)


@router.post("/", response_model=StationResponse, status_code=status.HTTP_201_CREATED)
async def create_station(
    station: StationCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new station"""
    # Check if call letters already exist
    existing = await db.execute(select(Station).where(Station.call_letters == station.call_letters))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Station with these call letters already exists")
    
    new_station = Station(**station.model_dump())
    db.add(new_station)
    await db.commit()
    await db.refresh(new_station, ["clusters"])
    
    # Manually construct response to avoid lazy loading issues
    station_dict = {
        "id": new_station.id,
        "call_letters": new_station.call_letters,
        "frequency": new_station.frequency,
        "market": new_station.market,
        "format": new_station.format,
        "ownership": new_station.ownership,
        "contacts": new_station.contacts,
        "rates": new_station.rates,
        "inventory_class": new_station.inventory_class,
        "active": new_station.active,
        "created_at": new_station.created_at,
        "updated_at": new_station.updated_at,
        "clusters": [
            {"id": cluster.id, "name": cluster.name}
            for cluster in new_station.clusters
        ]
    }
    return StationResponse(**station_dict)


@router.put("/{station_id}", response_model=StationResponse)
async def update_station(
    station_id: UUID,
    station_update: StationUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a station"""
    result = await db.execute(
        select(Station)
        .where(Station.id == station_id)
        .options(selectinload(Station.clusters))
    )
    station = result.scalar_one_or_none()
    
    if not station:
        raise HTTPException(status_code=404, detail="Station not found")
    
    # Check call letters uniqueness if being updated
    if station_update.call_letters and station_update.call_letters != station.call_letters:
        existing = await db.execute(select(Station).where(Station.call_letters == station_update.call_letters))
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Station with these call letters already exists")
    
    # Update fields
    update_data = station_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(station, field, value)
    
    await db.commit()
    await db.refresh(station, ["clusters"])
    
    # Manually construct response to avoid lazy loading issues
    station_dict = {
        "id": station.id,
        "call_letters": station.call_letters,
        "frequency": station.frequency,
        "market": station.market,
        "format": station.format,
        "ownership": station.ownership,
        "contacts": station.contacts,
        "rates": station.rates,
        "inventory_class": station.inventory_class,
        "active": station.active,
        "created_at": station.created_at,
        "updated_at": station.updated_at,
        "clusters": [
            {"id": cluster.id, "name": cluster.name}
            for cluster in station.clusters
        ]
    }
    return StationResponse(**station_dict)


@router.delete("/{station_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_station(
    station_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Soft delete a station (set active=False)"""
    result = await db.execute(select(Station).where(Station.id == station_id))
    station = result.scalar_one_or_none()
    
    if not station:
        raise HTTPException(status_code=404, detail="Station not found")
    
    station.active = False
    await db.commit()
    
    return None


@router.post("/{station_id}/clusters/{cluster_id}", status_code=status.HTTP_204_NO_CONTENT)
async def add_station_to_cluster(
    station_id: UUID,
    cluster_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Add a station to a cluster"""
    station_result = await db.execute(
        select(Station)
        .where(Station.id == station_id)
        .options(selectinload(Station.clusters))
    )
    station = station_result.scalar_one_or_none()
    if not station:
        raise HTTPException(status_code=404, detail="Station not found")
    
    cluster_result = await db.execute(select(Cluster).where(Cluster.id == cluster_id))
    cluster = cluster_result.scalar_one_or_none()
    if not cluster:
        raise HTTPException(status_code=404, detail="Cluster not found")
    
    if cluster in station.clusters:
        raise HTTPException(status_code=400, detail="Station already in this cluster")
    
    station.clusters.append(cluster)
    await db.commit()
    
    return None


@router.delete("/{station_id}/clusters/{cluster_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_station_from_cluster(
    station_id: UUID,
    cluster_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Remove a station from a cluster"""
    station_result = await db.execute(
        select(Station)
        .where(Station.id == station_id)
        .options(selectinload(Station.clusters))
    )
    station = station_result.scalar_one_or_none()
    if not station:
        raise HTTPException(status_code=404, detail="Station not found")
    
    cluster_result = await db.execute(select(Cluster).where(Cluster.id == cluster_id))
    cluster = cluster_result.scalar_one_or_none()
    if not cluster:
        raise HTTPException(status_code=404, detail="Cluster not found")
    
    if cluster not in station.clusters:
        raise HTTPException(status_code=400, detail="Station not in this cluster")
    
    station.clusters.remove(cluster)
    await db.commit()
    
    return None

