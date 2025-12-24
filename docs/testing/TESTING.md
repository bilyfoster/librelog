# LibreLog Testing Guide

## Overview

This guide covers testing procedures for the LibreLog radio traffic and automation system. Comprehensive test data has been created covering all major features.

## Quick Start

### 1. Create Test Data

Run the test data creation script:

```bash
docker compose exec api python -m backend.scripts.create_test_data
```

This creates:
- 8 advertisers, 3 agencies, 3 sales reps
- 8 orders, 20 copy items, 5 invoices
- 22 tracks (all types: MUS, PRO, LIN, IDS, NEW, PSA, INT)
- 40 spots, 3 campaigns, 3 clock templates
- 4 voice tracks, 10 copy assignments, 3 payments, 3 makegoods

### 2. Test Authentication

```bash
# Login (use your admin credentials)
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

## Testing Features

### Sales Features

**Test Sales Reps**:
```bash
TOKEN="<your_token>"
curl -X GET http://localhost:8000/api/sales-reps \
  -H "Authorization: Bearer $TOKEN"
```

**Test Orders**:
```bash
curl -X GET http://localhost:8000/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

**Test Advertisers**:
```bash
curl -X GET http://localhost:8000/api/advertisers \
  -H "Authorization: Bearer $TOKEN"
```

### Billing Features

**Test Invoices**:
```bash
curl -X GET http://localhost:8000/api/invoices \
  -H "Authorization: Bearer $TOKEN"
```

**Test Payments**:
```bash
curl -X GET http://localhost:8000/api/payments \
  -H "Authorization: Bearer $TOKEN"
```

### Copy Management

**Test Copy Items**:
```bash
curl -X GET http://localhost:8000/api/copy \
  -H "Authorization: Bearer $TOKEN"
```

**Test Copy Assignments**:
```bash
curl -X GET http://localhost:8000/api/copy-assignments \
  -H "Authorization: Bearer $TOKEN"
```

### Spots Management

**Test Spots**:
```bash
curl -X GET http://localhost:8000/api/spots \
  -H "Authorization: Bearer $TOKEN"
```

**Filter by Daypart**:
```bash
curl -X GET "http://localhost:8000/api/spots?daypart=MORNING_DRIVE" \
  -H "Authorization: Bearer $TOKEN"
```

### Music Library

**Test All Track Types**:
```bash
# Music tracks
curl -X GET "http://localhost:8000/api/tracks?type=MUS" \
  -H "Authorization: Bearer $TOKEN"

# Promos
curl -X GET "http://localhost:8000/api/tracks?type=PRO" \
  -H "Authorization: Bearer $TOKEN"

# Liners
curl -X GET "http://localhost:8000/api/tracks?type=LIN" \
  -H "Authorization: Bearer $TOKEN"

# Station IDs
curl -X GET "http://localhost:8000/api/tracks?type=IDS" \
  -H "Authorization: Bearer $TOKEN"

# News
curl -X GET "http://localhost:8000/api/tracks?type=NEW" \
  -H "Authorization: Bearer $TOKEN"

# PSAs
curl -X GET "http://localhost:8000/api/tracks?type=PSA" \
  -H "Authorization: Bearer $TOKEN"

# Interviews
curl -X GET "http://localhost:8000/api/tracks?type=INT" \
  -H "Authorization: Bearer $TOKEN"
```

### Clock Templates

**Test Clock Templates**:
```bash
curl -X GET http://localhost:8000/api/clocks \
  -H "Authorization: Bearer $TOKEN"
```

**Get Specific Template**:
```bash
curl -X GET http://localhost:8000/api/clocks/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Log Generation

**Generate Log Preview**:
```bash
curl -X POST http://localhost:8000/api/logs/preview \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2025-11-16",
    "clock_template_id": 1,
    "preview_hours": ["06:00", "12:00", "18:00"]
  }'
```

**Generate Daily Log**:
```bash
curl -X POST http://localhost:8000/api/logs/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2025-11-16",
    "clock_template_id": 1
  }'
```

**List Generated Logs**:
```bash
curl -X GET http://localhost:8000/api/logs \
  -H "Authorization: Bearer $TOKEN"
```

**Publish Log to LibreTime**:
```bash
curl -X POST http://localhost:8000/api/logs/1/publish \
  -H "Authorization: Bearer $TOKEN"
```

### Campaign Integration

**Test Campaigns**:
```bash
curl -X GET http://localhost:8000/api/campaigns \
  -H "Authorization: Bearer $TOKEN"
```

**Get Active Campaigns Only**:
```bash
curl -X GET "http://localhost:8000/api/campaigns?active_only=true" \
  -H "Authorization: Bearer $TOKEN"
```

## Testing Element Placement

### Test NEWS Placement
1. Create a clock template with NEWS element
2. Generate a log
3. Verify NEWS appears at scheduled time (typically top of hour)

### Test IDS Placement
1. Create a clock template with IDS element
2. Set `position: "top"` for top of hour or `position: "bottom"` for bottom
3. Set `hard_start: true` for exact timing
4. Generate a log
5. Verify IDS appears at correct position

### Test Timing Control
1. Create a clock template with mixed hard_start and flexible elements
2. Generate a log
3. Verify hard_start elements start exactly on time
4. Verify flexible elements adjust for timing drift
5. Check timing_drift values in log elements

### Test Element Rotation
1. Create multiple tracks of same type
2. Generate multiple logs
3. Verify tracks rotate (different tracks selected)

### Test Fallback Logic
1. Create a clock template with elements that may not have tracks
2. Generate a log
3. Verify fallback logic works (uses alternative element types if configured)

## Testing End-to-End Workflows

### Sales to Billing
1. Create an order
2. Schedule spots for the order
3. Generate invoice from order
4. Record payment for invoice
5. Verify all relationships are correct

### Copy to Spots
1. Create copy item
2. Assign copy to spot
3. Schedule spot
4. Generate log
5. Verify copy appears in log

### Campaign to Log
1. Create campaign
2. Generate ad schedule from campaign
3. Include in log generation
4. Verify ads appear in log

### Music to Log
1. Add tracks to library
2. Create clock template with music elements
3. Generate log
4. Verify music tracks appear in log

## Testing LibreTime Integration

### Publish Log
1. Generate a log
2. Publish to LibreTime
3. Verify log is marked as published
4. Check LibreTime for scheduled entries

### Track Sync
1. Verify tracks have libretime_id
2. Check track sync functionality
3. Verify playback history sync

## Known Issues

### BED Track Type
- BED type is supported in log generator but not in Track model
- Cannot create BED tracks until model is updated
- Workaround: Use other track types or add BED to model constraint

### Authentication
- Most endpoints require authentication
- Use login endpoint first to get token
- Token expires after configured time

## Test Data Summary

The test data script creates:
- **8 Advertisers**: Various local businesses
- **3 Agencies**: Media agencies
- **3 Sales Reps**: Sales representatives
- **8 Orders**: Active orders with various configurations
- **20 Copy Items**: Scripts and copy
- **5 Invoices**: Various statuses
- **22 Tracks**: All supported types
- **40 Spots**: Scheduled spots
- **3 Campaigns**: Active campaigns
- **3 Clock Templates**: Morning, Afternoon, Evening
- **4 Voice Tracks**: Show voice tracks
- **10 Copy Assignments**: Copy linked to spots
- **3 Payments**: Payment records
- **3 Makegoods**: Makegood records

## API Documentation

Full API documentation available at:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## Troubleshooting

### Services Not Running
```bash
docker compose ps
docker compose up -d
```

### Database Issues
```bash
docker compose exec db psql -U librelog -d librelog
```

### API Health Check
```bash
curl http://localhost:8000/api/health
```

## Next Steps

1. ✅ Test data created
2. ✅ All features implemented
3. ⏳ Manual testing by testers
4. ⏳ Fix any issues found
5. ⏳ Production deployment

---

For detailed test report, see `COMPREHENSIVE_TEST_REPORT.md`

