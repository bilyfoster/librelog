# LibreLog Business Logic & Workflow Documentation

## Executive Summary

This document defines the complete business workflow for LibreLog, a broadcast traffic management system. Each module's relationships and data flow are documented to ensure the UI/UX optimizes the user's workflow.

---

## Module Hierarchy

```
Organization (1)
    ├── Markets (N)
    ├── Clusters (N)
    ├── Stations (N)
    │       ├── Channels (N)
    │       │       └── Clocks (N)
    │       ├── Dayparts (N)
    │       └── Campaigns (N)
    ├── Users (N)
    └── Advertisers (N)
            ├── Orders (N) → Campaigns (N) → Spots (N) → Daily Log
            └── Campaigns (N)
```

---

## Module-by-Module Business Logic

### 1. ORGANIZATION MODULE
**Purpose**: Top-level entity representing a broadcasting company/group

**Fields**:
- `name` - Display name
- `legalName` - Legal entity name
- `taxId`, `email`, `phone`, `address` - Business info
- `isActive` - Status

**Relationships**:
- Has many Markets
- Has many Clusters
- Has many Stations
- Has many Users

**Business Rules**:
- Organization cannot be deleted if it has active stations
- All data is scoped to an organization

---

### 2. MARKET MODULE
**Purpose**: Geographic market area (e.g., "Phoenix Metro", "New York City")

**Fields**:
- `name` - Market name
- `city`, `state`, `country` - Location
- `organizationId` - Parent organization

**Relationships**:
- Belongs to Organization
- Has many Stations
- Has many Clusters

**Business Rules**:
- Market is optional but recommended for reporting

---

### 3. CLUSTER MODULE
**Purpose**: Group of stations operated together (e.g., "Phoenix Urban Cluster")

**Fields**:
- `name` - Cluster name
- `organizationId`, `marketId` - Parents

**Relationships**:
- Belongs to Organization
- Belongs to Market (optional)
- Has many Stations

---

### 4. STATION MODULE
**Purpose**: Individual broadcast station (e.g., "KRDP 90.7 FM")

**Fields**:
- `callSign` - Unique identifier (e.g., "KRDP")
- `name` - Friendly name
- `frequency`, `band` - Technical info
- `format` - Music format
- `organizationId`, `marketId`, `clusterId` - Parents

**Relationships**:
- Belongs to Organization, Market, Cluster
- Has many Channels
- Has many Clocks
- Has many Orders
- Has many Campaigns
- Has many Voice Tracks
- Has many Daily Logs

**Business Rules**:
- Call sign must be unique
- Station must have at least one Channel
- Station cannot be deleted if it has orders or campaigns

---

### 5. CHANNEL MODULE
**Purpose**: Broadcast stream (e.g., "Main FM", "HD2", "Web Stream")

**Fields**:
- `name` - Channel name
- `formatType` - LINEAR, DIGITAL, PODCAST
- `stationId` - Parent station

**Relationships**:
- Belongs to Station
- Has Clocks

---

### 6. CLOCK MODULE
**Purpose**: 24-hour broadcast template defining when breaks occur

**Fields**:
- `name` - Clock name
- `description` - Purpose/notes
- `channelId` - Associated channel
- `isActive` - Status

**Relationships**:
- Belongs to Channel (which has Station)
- Has many Break Structures (breaks within the hour)
- Has many Daypart Assignments

**Business Rules**:
- Only one clock per channel can be active at a time
- Clock defines WHERE spots can be placed (breaks)

---

### 7. DAYPART MODULE
**Purpose**: Time period definitions (e.g., "Morning Drive 6a-10a")

**Fields**:
- `name` - Daypart name
- `startTime`, `endTime` - Time range
- `category` - MORNING, MIDDAY, AFTERNOON, EVENING, OVERNIGHT

**Relationships**:
- Used in Clock assignments
- Used for spot targeting (e.g., "place this spot in Morning Drive")

---

### 8. AGENCY MODULE
**Purpose**: Advertising agency that represents multiple advertisers

**Fields**:
- `name`, `legalName` - Agency name
- `email`, `phone`, `address` - Contact info
- `isActive` - Status

**Relationships**:
- Has many Advertisers

---

### 9. SALES REP MODULE
**Purpose**: Salesperson who manages advertiser relationships

**Fields**:
- `firstName`, `lastName` - Name
- `email`, `phone` - Contact
- `commissionRate` - Compensation
- `isActive` - Status

**Relationships**:
- Has many Advertisers

---

### 10. ADVERTISER MODULE ⭐ CRITICAL
**Purpose**: Client who purchases advertising airtime

**Fields**:
- `name`, `legalName` - Business name
- `email`, `phone`, `address` - Contact info
- `creditLimit`, `paymentTerms` - Billing info
- `agencyId` - Associated agency (optional)
- `salesRepId` - Assigned salesperson (optional)
- `isActive` - Status

**Relationships**:
- Belongs to Agency (optional)
- Has Sales Rep (optional)
- Has many Orders
- Has many Campaigns

**Business Rules**:
- If agency is set, agency's info can be used on orders
- If salesRep is set, that rep gets commission
- Credit limit affects if orders can be approved

---

### 11. ORDER MODULE ⭐ CRITICAL - NEEDS FIX
**Purpose**: Sales contract for advertising

**Current Issues**:
1. ❌ Only has `advertiserName` (text) - NOT linked to Advertiser entity
2. ❌ Missing `advertiserId` (UUID reference)
3. ⚠️ Agency and Sales Rep are free text - should inherit from Advertiser

**Fields (Current)**:
- `orderNumber` - Auto-generated
- `stationId` - Station booking the order
- `advertiserName` - ❌ TEXT ONLY (PROBLEM)
- `agencyName` - Free text
- `salesRepName` - Free text
- `startDate`, `endDate` - Flight dates
- `totalSpots`, `totalAmount` - Order totals
- `status` - DRAFT, PENDING, APPROVED, ACTIVE, COMPLETED, CANCELLED
- `notes` - Internal notes

**Fields (NEEDED)**:
- ✅ `advertiserId` - UUID reference to Advertiser entity
- ✅ `advertiserName` - Denormalized for display (from Advertiser)
- ✅ `agencyName` - Auto-populated from Advertiser's Agency
- ✅ `salesRepName` - Auto-populated from Advertiser's Sales Rep

**Relationships**:
- Belongs to Station
- Should belong to Advertiser (NEEDS FIX)
- Has many Order Lines (specific requirements)
- Generates Campaigns

**Business Workflow**:
1. Sales creates Advertiser (or selects existing)
2. Sales creates Order
3. Order shows Advertiser DROPDOWN (not free text)
4. When Advertiser selected:
   - Auto-populate Agency from Advertiser.agency
   - Auto-populate Sales Rep from Advertiser.salesRep
5. Sales enters flight dates, spots, amount
6. Order is approved → Creates Campaign

---

### 12. CAMPAIGN MODULE
**Purpose**: Groups spots for scheduling and tracks progress

**Fields**:
- `name` - Campaign name
- `stationId` - Station
- `advertiserId` - ✅ UUID reference (GOOD)
- `advertiserName` - Denormalized
- `startDate`, `endDate` - Flight dates
- `status` - DRAFT, ACTIVE, PAUSED, COMPLETED
- `totalSpots`, `spotsScheduled`, `spotsAired` - Progress tracking
- `budget` - Total budget

**Relationships**:
- Belongs to Station
- Belongs to Advertiser (proper reference ✅)
- Has many Spots
- Created from Orders

**Business Rules**:
- Campaign cannot have spots outside its date range
- Status updates automatically based on spots

---

### 13. SPOT MODULE
**Purpose**: Individual scheduled advertisement

**Fields**:
- `campaignId` - Parent campaign
- `stationId` - Station
- `scheduledDate`, `scheduledTime` - When it airs
- `spotLength` - Duration in seconds (15, 30, 60)
- `status` - SCHEDULED, AIRED, MISSED, MAKEGOOD
- `assetId` - Reference to audio asset
- `breakName`, `breakPosition` - Where in clock it plays

**Relationships**:
- Belongs to Campaign
- Belongs to Station
- Has Asset (audio file)

**Business Workflow**:
1. Traffic creates Spots from Campaign
2. Production assigns audio asset
3. Traffic places spots into Daily Log
4. Spot airs → Status changes to AIRED
5. If missed → Status MAKEGOOD → New spot created

---

### 14. VOICE TRACK MODULE ⭐ NEEDS FIX
**Purpose**: DJ recorded segment between songs

**Current Issues**:
1. ❌ Missing `songBeforeId` and `songAfterId`
2. ❌ UI expects song context but model doesn't have it

**Fields (Current)**:
- `title` - Voice track title
- `stationId` - Station
- `showName` - Show it belongs to
- `segmentType` - INTRO, OUTRO, TRANSITION, LINER, TEASER
- `fileUrl`, `filePath` - Audio file
- `durationSeconds` - Length
- `scheduledDate`, `scheduledTime` - When to air
- `scriptText` - What to say
- `recordedText` - What was actually said
- `status` - DRAFT, RECORDED, APPROVED, SCHEDULED, AIRED
- `createdBy`, `recordedBy` - Users

**Fields (NEEDED)**:
- ✅ `songBeforeId` - UUID of song before this voice track
- ✅ `songAfterId` - UUID of song after this voice track
- ✅ `songBeforeTitle` - Denormalized for display
- ✅ `songAfterTitle` - Denormalized for display

**Business Workflow**:
1. Traffic creates Voice Track slot in log
2. System sets songBefore and songAfter
3. DJ views Voice Track with context:
   - "Coming up: [Song A] → YOUR VOICE → [Song B]"
4. DJ records voice track (in browser or upload)
5. Voice track scheduled into log

---

## Complete Business Workflow

### SALES WORKFLOW
```
Create Agency → Create Sales Rep → Create Advertiser
                                    (link to Agency & Sales Rep)
                                          ↓
                                    Create Order
                                    (select Advertiser from dropdown)
                                    (auto-fill Agency & Sales Rep)
                                          ↓
                                    Order Approved
                                          ↓
                                    Create Campaign
```

### TRAFFIC WORKFLOW
```
Create Campaign (or from Order)
        ↓
Create Spots (with flight dates)
        ↓
Create Daily Log
        ↓
Place Spots into Log slots
        ↓
Export to automation system
```

### PRODUCTION WORKFLOW
```
Campaign created
        ↓
Spots need audio
        ↓
Voice Talent views Spots
        ↓
Record audio / Upload file
        ↓
Mark spot as READY
        ↓
Traffic can schedule spot
```

### DJ/VJ WORKFLOW
```
Voice Track created by Traffic
        ↓
System sets Song Before/After
        ↓
DJ views Voice Track
        ↓
Sees context: "Song A → YOU → Song B"
        ↓
Records voice track
        ↓
Saved & scheduled
```

---

## Critical Fixes Needed

### 1. Order Module (HIGH PRIORITY)
**Problem**: Order only has `advertiserName` (text), not `advertiserId` (reference)

**Impact**:
- Cannot traverse Advertiser → Orders relationship
- If advertiser name changes, orders are orphaned
- Cannot reliably auto-populate agency/sales rep

**Fix**:
1. Add `advertiserId` column to Order entity
2. Add `@ManyToOne` relationship to Advertiser
3. Update OrderRequestDTO to accept `advertiserId`
4. Update OrderService to lookup Advertiser and set name/agency/salesRep
5. Update UI to send advertiserId

### 2. Voice Track Module (MEDIUM PRIORITY)
**Problem**: Missing song before/after context

**Impact**:
- DJ cannot see what songs surround their voice track
- No context for recording

**Fix**:
1. Add `songBeforeId`, `songAfterId` to VoiceTrack entity
2. Add `songBeforeTitle`, `songAfterTitle` for display
3. Update UI to show song context
4. Update Daily Log placement to set these values

### 3. Campaign Creation from Order (MEDIUM PRIORITY)
**Problem**: No UI/workflow to create Campaign from Order

**Impact**:
- Orders exist but don't generate campaigns
- Traffic has to manually recreate campaigns

**Fix**:
1. Add "Create Campaign from Order" button
2. Pre-populate Campaign with Order data
3. Link Campaign back to Order

---

## UI/UX Optimizations

### Order Creation Form (Fixed ✅)
```
Station: [Dropdown]
Advertiser: [Dropdown] ← Selected from system
Agency: [Read-only] ← Auto-filled from Advertiser
Sales Rep: [Read-only] ← Auto-filled from Advertiser
[ ] Override agency/sales rep ← Checkbox to edit

Start Date: [Date]
End Date: [Date]
Total Spots: [Number]
Total Amount: [Currency]
Notes: [Textarea]
```

### Spot Production View (TODO)
```
+--------------------------------------------------+
| SPOT PRODUCTION WORKFLOW                         |
+--------------------------------------------------+
|                                                  |
| Campaign: "Summer Sale 2024"                     |
| Advertiser: "Bob's Auto Shop"                    |
|                                                  |
| Spots needing audio:                             |
| ☐ Mon 6/1 8:00am - Morning Drive (30s)          |
| ☐ Tue 6/2 12:00pm - Midday (30s)                |
| ☐ Wed 6/3 5:00pm - Afternoon Drive (30s)        |
|                                                  |
| [Record] [Upload Audio] [Mark Ready]             |
+--------------------------------------------------+
```

### DJ Voice Track View (TODO)
```
+--------------------------------------------------+
| VOICE TRACKS FOR TODAY                           |
+--------------------------------------------------+
|                                                  |
| ⏭️ COMING UP:                                    |
|    "Summer of '69" by Bryan Adams                |
|                                                  |
| 🎙️ YOUR VOICE TRACK:                            |
|    "That was a classic! Coming up next..."       |
|                                                  |
| ⏭️ THEN:                                         |
|    "Don't Stop Believin'" by Journey             |
|                                                  |
| [Record Now] [View Script]                       |
+--------------------------------------------------+
```

---

## Data Integrity Rules

1. **Advertiser**:
   - Name must be unique within organization
   - If linked to Agency, Agency must be active
   - Cannot delete if has active Orders or Campaigns

2. **Order**:
   - Must have valid Station
   - Must have valid Advertiser (after fix)
   - End date must be after start date
   - Cannot modify if status COMPLETED or CANCELLED

3. **Campaign**:
   - Must have valid Station and Advertiser
   - Spots cannot be scheduled outside date range
   - Status updates based on spot statuses

4. **Spot**:
   - Must have valid Campaign and Station
   - Scheduled date must be within Campaign range
   - Cannot have status AIRED without actualAirTime

---

## Next Steps

1. ✅ Fix Order UI - Auto-populate agency/sales rep from Advertiser
2. 🔧 Fix Order Data Model - Add advertiserId relationship
3. 🔧 Fix Voice Track Model - Add song before/after
4. 🔧 Create Campaign from Order workflow
5. 🔧 Add Spot Production UI
6. 🔧 Add DJ Voice Track recording interface
7. 🔧 Add Traffic Log placement UI

---

*Document Version: 1.0*
*Last Updated: 2026-03-13*
