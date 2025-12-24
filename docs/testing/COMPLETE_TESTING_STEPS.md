# Complete Testing Steps for LibreLog
## End-to-End Workflow: Spot Sold → Produced → Scheduled → Logged → Published → Aired → Reconciled → Billed

This document provides comprehensive testing steps to walk through the complete workflow and get a spot "sold" into the system, produced in house, scheduled, added to the log, pushed to automation, aired, and then reconciled back into the system for billing.

---

## Prerequisites

- [ ] LibreLog system is running and accessible
- [ ] User account with admin permissions
- [ ] LibreTime integration configured (for publishing and reconciliation)
- [ ] Test environment ready (or use production with test data)

---

## Complete Workflow Testing Steps

### Phase 1: Spot Sold (Order Entry)

**Goal:** Create an order for a new advertiser

#### Step 1.1: Create Advertiser

**Via UI:**
1. Navigate to **Traffic > Advertisers**
2. Click **"Add Advertiser"**
3. Fill in:
   - Name: "Test Advertiser - [Your Name]"
   - Contact Name: "Test Contact"
   - Email: "test@advertiser.com"
   - Phone: "555-1234"
   - Payment Terms: "Net 30"
   - Credit Limit: $10,000
4. Click **"Save"**
5. **Verify:** Advertiser appears in list with ID

**Via API (with tokenized request):**
```bash
# Get token first
TOKEN=$(curl -X POST http://api:8000/auth/login \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.access_token')

# Create advertiser
curl -X POST http://api:8000/advertisers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "name": "Test Advertiser",
    "contact_name": "Test Contact",
    "email": "test@advertiser.com",
    "phone": "555-1234",
    "payment_terms": "Net 30",
    "credit_limit": 10000.00
  }'
```

**Verification:**
- [ ] Advertiser created successfully
- [ ] Advertiser ID assigned
- [ ] All fields saved correctly

---

#### Step 1.2: Create Order

**Via UI:**
1. Navigate to **Traffic > Orders**
2. Click **"New Order"**
3. Fill in order details:
   - **Advertiser:** Select "Test Advertiser"
   - **Start Date:** Tomorrow
   - **End Date:** 30 days from tomorrow
   - **Total Spots:** 10
   - **Spot Lengths:** 30 seconds
   - **Rate Type:** ROS
   - **Total Value:** $500.00
4. Click **"Save"**
5. **Note the Order Number** (format: YYYYMMDD-XXXX)

**Via API:**
```bash
curl -X POST http://api:8000/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "advertiser_id": <advertiser_id>,
    "start_date": "2024-01-16",
    "end_date": "2024-02-15",
    "total_spots": 10,
    "spot_lengths": [30],
    "rate_type": "ROS",
    "total_value": 500.00,
    "status": "DRAFT"
  }'
```

**Verification:**
- [ ] Order created with status DRAFT
- [ ] Order number format: YYYYMMDD-XXXX
- [ ] Order number date prefix matches today's date
- [ ] Sequential number increments correctly

---

### Phase 2: Produced In House (Production)

**Goal:** Create production order and upload copy

#### Step 2.1: Create Production Order (Optional)

**Via UI:**
1. Navigate to **Production > Production Orders**
2. Click **"New Production Order"**
3. Fill in:
   - Client Name: "Test Advertiser"
   - Spot Lengths: 30 seconds
   - Deadline: 7 days from today
   - Instructions: "Test production order"
4. Click **"Save"**

**Via API:**
```bash
curl -X POST http://api:8000/production-orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "client_name": "Test Advertiser",
    "spot_lengths": [30],
    "deadline": "2024-01-22",
    "instructions": "Test production order"
  }'
```

**Verification:**
- [ ] Production order created (if using production workflow)

---

#### Step 2.2: Upload Copy

**Via UI:**
1. Navigate to **Traffic > Copy Library**
2. Click **"Upload Copy"**
3. Fill in:
   - **Order:** Select test order
   - **Advertiser:** Auto-filled
   - **Title:** "Test Copy - 30 Second"
   - **Script Text:** "This is a test commercial script for workflow testing. Visit our store today!"
   - **Expiration Date:** 60 days from today
4. Click **"Save"**

**Via API:**
```bash
curl -X POST http://api:8000/copy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "order_id": <order_id>,
    "advertiser_id": <advertiser_id>,
    "title": "Test Copy - 30 Second",
    "script_text": "This is a test commercial script.",
    "expires_at": "2024-03-15"
  }'
```

**Verification:**
- [ ] Copy created and linked to order
- [ ] Copy visible in Copy Library
- [ ] Copy assigned to order

---

### Phase 3: Scheduled (Spot Scheduling)

**Goal:** Approve order and schedule spots

#### Step 3.1: Approve Order

**Via UI:**
1. Navigate to **Traffic > Orders**
2. Find test order
3. Click **"Approve"** button
4. Confirm approval

**Via API:**
```bash
curl -X POST http://api:8000/orders/<order_id>/approve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com"
```

**Verification:**
- [ ] Order status changed to APPROVED
- [ ] Order visible in Spot Scheduler

---

#### Step 3.2: Schedule Spots

**Via UI:**
1. Navigate to **Traffic > Spot Scheduler**
2. **Select Station:** Choose a station (required)
3. Select test order from dropdown
4. Set date range:
   - **Start Date:** Tomorrow
   - **End Date:** 10 days from tomorrow
5. Configure:
   - **Spot Length:** 30 seconds
   - **Break Position:** (Leave blank for ROS)
   - **Daypart:** (Leave blank for ROS)
6. Click **"Schedule"** button
7. Review preview of spots to be created
8. Click **"Confirm"**

**Via API:**
```bash
curl -X POST http://api:8000/spots/bulk \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "order_id": <order_id>,
    "start_date": "2024-01-16",
    "end_date": "2024-01-26",
    "spot_length": 30,
    "count": 10
  }'
```

**Verification:**
- [ ] 10 spots created
- [ ] Spots distributed across date range
- [ ] Spots have scheduled dates and times
- [ ] Break positions assigned (if applicable)

---

#### Step 3.3: Verify Spots Created

1. Navigate to **Library > Spots Library**
2. Filter by test order
3. Verify:
   - [ ] 10 spots appear
   - [ ] All spots are 30 seconds
   - [ ] Status is SCHEDULED
   - [ ] Dates are within order date range
   - [ ] Times are assigned

---

### Phase 4: Added to Log (Log Generation)

**Goal:** Generate daily log with scheduled spots

#### Step 4.1: Select Date and Clock Template

**Via UI:**
1. Navigate to **Logs > Log Generator**
2. **Select Station:** Choose the same station used for spots
3. Select **Target Date:** Tomorrow
4. Select **Clock Template:** Choose template for selected station
5. Review template structure

**Verification:**
- [ ] Station selected
- [ ] Clock template selected (belongs to station)
- [ ] Template structure visible

---

#### Step 4.2: Preview Log (Recommended)

1. Click **"Preview"** button
2. Review preview:
   - [ ] Scheduled spots appear in ADV slots
   - [ ] Music fills MUS slots
   - [ ] Timing looks correct
   - [ ] No obvious conflicts
3. Close preview

---

#### Step 4.3: Generate Daily Log

**Via UI:**
1. Click **"Generate Log"** button
2. Wait for generation to complete
3. Log opens in Log Editor

**Via API:**
```bash
curl -X POST http://api:8000/logs/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "target_date": "2024-01-16",
    "clock_template_id": <clock_template_id>,
    "station_id": <station_id>
  }'
```

**Verification:**
- [ ] Log generated successfully
- [ ] Log ID assigned
- [ ] Log includes scheduled spots

---

#### Step 4.4: Review Generated Log

1. Review timeline view:
   - [ ] Spots placed correctly in ADV slots
   - [ ] Music selected for MUS slots
   - [ ] Timing is accurate
   - [ ] Flow is good
2. Check for conflicts:
   - [ ] No conflict warnings
   - [ ] All spots have copy assigned
   - [ ] Timing is correct

---

#### Step 4.5: Edit Log (If Needed)

If adjustments are needed:
1. **Move Elements:** Drag spots to different times
2. **Add Spots:** Add additional spots if needed
3. **Remove Elements:** Delete unwanted elements
4. **Adjust Timing:** Modify element timing
5. Save changes

---

### Phase 5: Pushed to Automation (LibreTime Publishing)

**Goal:** Publish log to LibreTime automation system

#### Step 5.1: Publish Log to LibreTime

**Via UI:**
1. Review entire log one final time
2. Click **"Publish"** button
3. Confirm publication
4. Wait for publish to complete

**Via API:**
```bash
curl -X POST http://api:8000/logs/<log_id>/publish \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com"
```

**Verification:**
- [ ] Publish request succeeds
- [ ] Log status changes to PUBLISHED
- [ ] Log is locked (cannot edit)

---

#### Step 5.2: Verify LibreTime Receives Log

1. Check LibreTime interface (if accessible):
   - [ ] Log appears in LibreTime schedule
   - [ ] Log data matches LibreLog log
   - [ ] Spots are in correct positions

2. Check LibreLog:
   - [ ] Log status: PUBLISHED
   - [ ] Log is locked
   - [ ] Cannot edit log

**Note:** If LibreTime is not configured, this step will show a warning but test can continue.

---

### Phase 6: Aired (On-Air Playback)

**Goal:** Verify spots air and playback is tracked

#### Step 6.1: Wait for Spot to Air

**Note:** This requires LibreTime automation to actually play spots at scheduled times.

1. Wait for scheduled spot time
2. Monitor on-air (if possible)
3. Verify spot plays at scheduled time

**Verification:**
- [ ] Spot airs at scheduled time (if monitoring)
- [ ] Playback recorded in LibreTime

---

#### Step 6.2: Verify Playback Tracking

1. Check LibreTime playback history:
   - [ ] Spot appears in playback history
   - [ ] Actual air time recorded
   - [ ] Duration recorded

2. Check LibreLog:
   - [ ] Spot status may still be SCHEDULED (until sync)
   - [ ] Playback history not yet synced

**Note:** Status will update after reconciliation sync.

---

### Phase 7: Reconciled Back (Playback Sync)

**Goal:** Sync playback history and reconcile spots

#### Step 7.1: Sync Playback History

**Via UI:**
1. Navigate to **Settings > Sync** (if available)
2. Click **"Sync Playback History"**
3. Select date range (yesterday to today)
4. Click **"Sync"**
5. Wait for sync to complete

**Via API:**
```bash
curl -X POST http://api:8000/sync/playback-history \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "start_date": "2024-01-15",
    "end_date": "2024-01-16"
  }'
```

**Verification:**
- [ ] Sync request succeeds
- [ ] Response indicates records synced
- [ ] Playback records created in database

---

#### Step 7.2: Run Reconciliation Report

**Via UI:**
1. Navigate to **Reports > Reconciliation**
2. Set date range (yesterday to today)
3. Generate report
4. Review report:
   - [ ] Scheduled spots listed
   - [ ] Aired spots matched
   - [ ] Unmatched spots flagged (if any)

**Via API:**
```bash
curl -X GET "http://api:8000/reports/reconciliation?start_date=2024-01-15&end_date=2024-01-16" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Host: log.gayphx.com"
```

**Verification:**
- [ ] Report generated successfully
- [ ] Report shows scheduled vs aired spots
- [ ] Matched spots identified
- [ ] Unmatched spots flagged

---

#### Step 7.3: Verify Spot Status Updates

1. Navigate to **Library > Spots Library**
2. Filter by test order
3. Check spot statuses:
   - [ ] Aired spots show status: **AIRED**
   - [ ] Scheduled spots that didn't air: **MISSED** or still **SCHEDULED**
   - [ ] Actual air times recorded

**Verification:**
- [ ] Spot statuses updated correctly
- [ ] AIRED spots have actual air times
- [ ] Status transition: SCHEDULED → AIRED

---

#### Step 7.4: Verify Unmatched Spots Flagged

1. Check reconciliation report
2. Look for unmatched spots section
3. Verify:
   - [ ] Spots that didn't air are flagged
   - [ ] Reason for mismatch is clear
   - [ ] Makegood recommendations shown (if applicable)

---

### Phase 8: Billing (Invoice Generation)

**Goal:** Generate invoice from completed order and record payment

#### Step 8.1: Generate Invoice from Order

**Via UI:**
1. Navigate to **Billing > Invoices**
2. Click **"Generate from Order"**
3. Select test order
4. Review invoice details
5. Click **"Generate"**

**Via API:**
```bash
curl -X POST http://api:8000/invoices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "order_id": <order_id>,
    "invoice_date": "2024-01-16",
    "due_date": "2024-02-15"
  }'
```

**Verification:**
- [ ] Invoice created successfully
- [ ] Invoice ID assigned
- [ ] Invoice includes all aired spots

---

#### Step 8.2: Verify Invoice Details

1. Open invoice
2. Verify:
   - [ ] All aired spots listed
   - [ ] Pricing is correct ($50 per spot = $500 total)
   - [ ] Subtotal calculated correctly
   - [ ] Tax calculated (if applicable)
   - [ ] Total amount correct
   - [ ] Advertiser information correct
   - [ ] Payment terms: Net 30
   - [ ] Due date: 30 days from invoice date

**Verification:**
- [ ] All invoice details are accurate
- [ ] Only aired spots included
- [ ] Pricing matches order

---

#### Step 8.3: Send Invoice

**Via UI:**
1. Open invoice
2. Review one final time
3. Click **"Send Invoice"** button
4. Confirm send

**Via API:**
```bash
curl -X POST http://api:8000/invoices/<invoice_id>/send \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com"
```

**Verification:**
- [ ] Invoice send request succeeds
- [ ] Invoice status: SENT
- [ ] Send date recorded
- [ ] Email sent (if SMTP configured)

---

#### Step 8.4: Record Payment (Simulated)

**Via UI:**
1. Navigate to **Billing > Payments**
2. Click **"Record Payment"**
3. Select invoice
4. Enter payment details:
   - Amount: $500.00 (full invoice amount)
   - Payment Date: Today
   - Payment Method: Check
   - Reference Number: "TEST-001"
5. Click **"Save"**

**Via API:**
```bash
curl -X POST http://api:8000/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Host: log.gayphx.com" \
  -d '{
    "invoice_id": <invoice_id>,
    "amount": 500.00,
    "payment_date": "2024-01-16",
    "payment_method": "Check",
    "reference_number": "TEST-001"
  }'
```

**Verification:**
- [ ] Payment recorded successfully
- [ ] Invoice status: PAID
- [ ] Payment linked to invoice
- [ ] Payment amount matches invoice

---

## Complete Workflow Verification

### Final Verification Checklist

- [ ] **Order:** Created, approved, spots scheduled
- [ ] **Copy:** Uploaded and assigned to order
- [ ] **Spots:** 10 spots scheduled across date range
- [ ] **Log:** Generated with spots included
- [ ] **Published:** Log published to LibreTime
- [ ] **Aired:** Spots aired (or simulated)
- [ ] **Synced:** Playback history synced
- [ ] **Reconciled:** Spots matched and status updated
- [ ] **Invoiced:** Invoice generated with aired spots
- [ ] **Paid:** Payment recorded

### Data Flow Verification

- [ ] Order → Spots: Spots linked to order
- [ ] Spots → Log: Spots appear in log
- [ ] Log → LibreTime: Log published successfully
- [ ] LibreTime → Playback: Playback history recorded
- [ ] Playback → Reconciliation: Spots matched
- [ ] Reconciliation → Invoice: Only aired spots invoiced
- [ ] Invoice → Payment: Payment linked to invoice

---

## Common Issues and Solutions

### Issue: Order Not Appearing for Scheduling

**Solutions:**
- Verify order status is APPROVED (not PENDING)
- Check date range is valid (not in past)
- Verify user has Traffic Manager permissions
- Refresh page or clear cache

### Issue: Spots Not Scheduling

**Solutions:**
- Verify order is APPROVED
- Check date range is valid
- Verify copy is uploaded
- Check for conflicts
- Verify station is selected

### Issue: Log Generation Fails

**Solutions:**
- Verify clock template exists and belongs to selected station
- Check scheduled spots exist for target date
- Verify music library has tracks
- Check station is selected
- Verify LibreTime connection (for publishing)

### Issue: Log Publishing Fails

**Solutions:**
- Verify LibreTime API configuration
- Check LibreTime API key is valid
- Verify LibreTime is accessible from backend
- Check network connectivity
- Review backend logs for errors

### Issue: Playback Sync Fails

**Solutions:**
- Verify LibreTime connection
- Check date range is valid
- Verify playback history exists in LibreTime
- Check API credentials
- Review sync logs

### Issue: Reconciliation Doesn't Match Spots

**Solutions:**
- Verify playback history was synced
- Check date/time matching logic
- Verify spot times are accurate
- Check for timezone issues
- Review reconciliation report details

### Issue: Invoice Generation Fails

**Solutions:**
- Verify order is COMPLETED (all spots aired)
- Check spots have AIRED status
- Verify order has valid pricing
- Check advertiser information is complete
- Verify billing permissions

---

## Testing Tips

1. **Use Test Data:** Create test advertisers and orders with clear naming
2. **Document Everything:** Note order numbers, IDs, and key data points
3. **Take Screenshots:** Capture key steps for documentation
4. **Test Both UI and API:** Verify both interfaces work correctly
5. **Verify Token Usage:** Ensure all API requests use Bearer tokens
6. **Check Status Transitions:** Verify status changes at each step
7. **Test Error Cases:** Try invalid data, missing fields, etc.
8. **Clean Up:** Remove test data after testing (or mark as test)

---

## Success Criteria

The workflow test is successful when:

1. ✅ Order created and approved
2. ✅ Copy uploaded and assigned
3. ✅ Spots scheduled correctly
4. ✅ Log generated with spots
5. ✅ Log published to LibreTime
6. ✅ Playback history synced
7. ✅ Spots reconciled (status: AIRED)
8. ✅ Invoice generated with aired spots only
9. ✅ Invoice sent successfully
10. ✅ Payment recorded

---

## Next Steps After Testing

1. **Document Results:** Use `TEST_RESULTS_REPORT_TEMPLATE.md`
2. **Report Issues:** Document any bugs or issues found
3. **Update Documentation:** Update docs if workflows changed
4. **Clean Up:** Remove or archive test data
5. **Schedule Regular Testing:** Set up regular test schedule

---

*Last Updated: 2025-01-15*




