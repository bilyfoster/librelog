# Next 2 Weeks: Ship the Core Loop

## Goal
Get **Order → Campaign → Spot → Daily Log** working end-to-end.

---

## Week 1: Order → Campaign Connection

### Day 1 (Today): Finish Order Advertiser Dropdown
**You**: Test current implementation  
**Me**: Fix any issues found

| Task | Status | Notes |
|------|--------|-------|
| Test Order creation with Advertiser dropdown | | |
| Verify agency auto-populates | | |
| Verify sales rep auto-populates | | |
| Test override checkbox | | |
| Deploy if working | | |

### Day 2: Campaign Modal
**Me**: Build Campaign creation modal

```
Campaign Modal
├── Name (text)
├── Order (dropdown - shows orders for this station)
├── Advertiser (auto-populated from Order)
├── Station (auto-populated from Order)
├── Start Date (auto-populated from Order)
├── End Date (auto-populated from Order)
├── Total Spots (auto-populated from Order)
├── Status (dropdown: DRAFT, ACTIVE, PAUSED)
└── Notes (textarea)
```

API Endpoint needed:
```
POST /api/campaigns/from-order/{orderId}
```

### Day 3: Order → Campaign Button
**Me**: Add "Create Campaign" button to Orders tab

| Task | File | Description |
|------|------|-------------|
| Add button | dashboard.html | "Create Campaign" in Order row |
| Pre-populate | campaign modal | Fill from Order data |
| Link Order-Campaign | backend | Store orderId on Campaign |

### Day 4: Campaign List Improvements
**Me**: Enhance Campaigns tab

| Task | Description |
|------|-------------|
| Show Order number | Link back to order |
| Progress bar | spotsScheduled / totalSpots |
| Status badges | Color-coded status |
| Quick actions | View Spots, Edit, Delete |

### Day 5: Testing & Polish
**You**: Test Order → Campaign flow  
**Me**: Fix bugs, polish UX

**Test Scenarios**:
1. Create Order → Create Campaign → Verify data copies correctly
2. Edit Campaign → Changes don't break Order
3. Delete Campaign → Order still exists
4. View Campaigns → See linked Order

---

## Week 2: Campaign → Spots → Daily Log

### Day 6: Spot Creation Modal
**Me**: Build Spot creation within Campaign

```
Spot Modal
├── Campaign (auto-populated, read-only)
├── Spot Name (text, required)
├── Spot Length (dropdown: 15s, 30s, 60s)
├── Scheduled Date (date picker)
├── Scheduled Time (time picker)
├── Status (dropdown: DRAFT, READY, SCHEDULED, AIRED)
├── Audio File (upload widget)
└── Notes (textarea)
```

### Day 7: Spot List in Campaign
**Me**: Show spots within Campaign view

```
Campaign Detail View
├── Campaign Info (header)
├── Progress: [████████░░] 80% (80/100 spots scheduled)
├── Spot List Table:
│   ├── Date | Time | Name | Length | Status | Actions
│   └── 6/1  | 8:00 | Spot 1 | 30s    | READY  | [Edit] [Delete]
└── [+ Add Spot] button
```

### Day 8: Simple Daily Log
**Me**: Create Daily Log view

```
Daily Log - June 1, 2024
┌─────────────────────────────────────────────┐
│ KRDP-FM                                     │
│ Monday, June 1, 2024                        │
├─────────────────────────────────────────────┤
│ 6:00 AM - Morning Drive                     │
│ [Song] Summer of '69 - Bryan Adams          │
│ [VT] Morning intro...                       │
│ [SPOT] Bob's Auto - 30s [READY]             │
│ [Song] Don't Stop Believin' - Journey       │
│ [SPOT] Pizza Palace - 15s [NEEDS AUDIO]     │
└─────────────────────────────────────────────┘
```

### Day 9: Drag-to-Schedule
**Me**: Allow dragging spots into daily log

| Task | Description |
|------|-------------|
| Unscheduled spots pool | Left sidebar: "Available Spots" |
| Daily log grid | Right side: time slots |
| Drag and drop | HTML5 drag/drop API |
| Save position | Update spot.scheduledDate/Time |

### Day 10: Integration & Testing
**You**: Test complete flow  
**Me**: Fix critical bugs

**End-to-End Test**:
1. Create Advertiser
2. Create Order for Advertiser
3. Create Campaign from Order
4. Create Spots in Campaign
5. Schedule Spots into Daily Log
6. Export log to LibreTime

---

## Technical Tasks

### Backend APIs Needed

```java
// CampaignController additions
@PostMapping("/from-order/{orderId}")
public CampaignResponseDTO createFromOrder(@PathVariable UUID orderId)

@GetMapping("/by-order/{orderId}")
public List<CampaignResponseDTO> getByOrder(@PathVariable UUID orderId)

// SpotController additions  
@GetMapping("/by-campaign/{campaignId}")
public List<SpotResponseDTO> getByCampaign(@PathVariable UUID campaignId)

@PostMapping
public SpotResponseDTO create(@RequestBody SpotRequestDTO request)

// DailyLogController (new)
@GetMapping("/api/daily-logs/{stationId}/{date}")
public DailyLogResponseDTO getDailyLog(
    @PathVariable UUID stationId,
    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
)

@PostMapping("/api/daily-logs/{logId}/spots")
public void addSpotToLog(
    @PathVariable UUID logId,
    @RequestBody AddSpotToLogRequest request
)
```

### Database Migrations

```sql
-- Link Campaign to Order
ALTER TABLE campaigns ADD COLUMN order_id UUID;
ALTER TABLE campaigns ADD CONSTRAINT fk_campaign_order 
    FOREIGN KEY (order_id) REFERENCES orders(id);

-- Create indexes
CREATE INDEX idx_campaigns_order ON campaigns(order_id);
CREATE INDEX idx_spots_campaign ON spots(campaign_id);
CREATE INDEX idx_spots_date ON spots(scheduled_date);
```

### Frontend Changes

```javascript
// New functions needed
createCampaignFromOrder(orderId)
loadCampaignSpots(campaignId)
openSpotModal(campaignId)
saveSpot(spotData)
loadDailyLog(stationId, date)
addSpotToLog(spotId, logId, time)
removeSpotFromLog(spotId, logId)
exportLogToLibreTime(logId)
```

---

## Success Criteria (End of Week 2)

| Criteria | How to Verify |
|----------|---------------|
| ✅ Order → Campaign works | Create order, click "Create Campaign", verify data |
| ✅ Campaign → Spots works | Add spots, see them in list |
| ✅ Spots → Daily Log works | Drag spot to log, see it scheduled |
| ✅ LibreTime export works | Export button generates valid file |
| ✅ No critical bugs | You can demo without crashes |
| ✅ UI is understandable | You can explain it to someone |

---

## If We Finish Early

### Stretch Goals (Priority Order)

1. **Spot audio upload** 
   - File upload widget
   - Store in S3/local storage
   - Play button to preview

2. **Invoice generation**
   - Create invoice from aired spots
   - PDF generation
   - Email to advertiser

3. **Simple avails view**
   - Calendar grid
   - Green/yellow/red for availability

4. **Help tooltips**
   - Contextual help
   - Reduce confusion

---

## If We Run Behind

### Cut Scope (In Order)

1. **Keep**: Order → Campaign → Spot creation (core)
2. **Simplify**: Daily log = simple list view (no drag/drop)
3. **Defer**: LibreTime export (manual CSV for now)
4. **Defer**: Audio upload (external file management)

**Minimum Viable**: 
- Can create Order
- Can create Campaign from Order  
- Can create Spots in Campaign
- Can view spots in list

This alone is **valuable** for traffic management.

---

## Communication

### Daily Check-ins (Async)

**Morning (from you)**:
- What I tested yesterday
- Bugs found
- Questions

**Evening (from me)**:
- What I built today
- Screenshots/demo
- What's next

### If Blocked

| Blocker | Solution |
|---------|----------|
| Technical issue | I research/stack overflow |
| Design decision | You decide quickly |
| Scope creep | We cut, not expand |
| Bug in production | Fix immediately |

---

## The Goal Post

**End of 2 weeks**: 

You should be able to demo to a radio station:

> "Here's how you traffic ads. Create an order for your advertiser, 
> convert it to a campaign, add the spots, schedule them in your daily 
> log, and export to your automation system."

**This is the core value proposition.** Everything else is bonus.

---

## Let's Do This! 🚀

**Today**: You test Order dropdown, I fix any issues  
**Tomorrow**: I build Campaign modal  
**This week**: Order → Campaign working  
**Next week**: Campaign → Spots → Log working

Ready? Let's ship! 

---

*Task List Version: 1.0*  
*Sprint: 2 weeks*  
*Goal: Core loop working*  
*Created: 2026-03-13*
