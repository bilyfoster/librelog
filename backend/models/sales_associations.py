"""
Association tables for many-to-many relationships in sales models
"""

from sqlalchemy import Table, Column, Integer, ForeignKey
from backend.database import Base

# Association table for SalesRep <-> SalesTeam (many-to-many)
sales_rep_teams = Table(
    'sales_rep_teams',
    Base.metadata,
    Column('sales_rep_id', Integer, ForeignKey('sales_reps.id', ondelete='CASCADE'), primary_key=True),
    Column('sales_team_id', Integer, ForeignKey('sales_teams.id', ondelete='CASCADE'), primary_key=True),
)

# Association table for SalesRep <-> SalesOffice (many-to-many)
sales_rep_offices = Table(
    'sales_rep_offices',
    Base.metadata,
    Column('sales_rep_id', Integer, ForeignKey('sales_reps.id', ondelete='CASCADE'), primary_key=True),
    Column('sales_office_id', Integer, ForeignKey('sales_offices.id', ondelete='CASCADE'), primary_key=True),
)

# Association table for SalesRep <-> SalesRegion (many-to-many)
sales_rep_regions = Table(
    'sales_rep_regions',
    Base.metadata,
    Column('sales_rep_id', Integer, ForeignKey('sales_reps.id', ondelete='CASCADE'), primary_key=True),
    Column('sales_region_id', Integer, ForeignKey('sales_regions.id', ondelete='CASCADE'), primary_key=True),
)

# Association table for Station <-> Cluster (many-to-many)
station_clusters = Table(
    'station_clusters',
    Base.metadata,
    Column('station_id', Integer, ForeignKey('stations.id', ondelete='CASCADE'), primary_key=True),
    Column('cluster_id', Integer, ForeignKey('clusters.id', ondelete='CASCADE'), primary_key=True),
)

# Association table for User <-> Station (many-to-many)
user_stations = Table(
    'user_stations',
    Base.metadata,
    Column('user_id', Integer, ForeignKey('users.id', ondelete='CASCADE'), primary_key=True),
    Column('station_id', Integer, ForeignKey('stations.id', ondelete='CASCADE'), primary_key=True),
)

