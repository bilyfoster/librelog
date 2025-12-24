# LibreLog Manual Testing Checklist
## Complete Spot Lifecycle Workflow

This checklist provides step-by-step instructions for manually testing the complete workflow from spot sale through billing reconciliation.

**Date:** _______________  
**Tester:** _______________  
**Environment:** _______________

---

## Prerequisites

- [ ] LibreLog system is running and accessible
- [ ] User account with appropriate permissions (admin recommended)
- [ ] LibreTime integration configured (if testing publishing)
- [ ] Test data prepared (or use test script)

---

## Step 1: Spot Sold (Order Entry)

### 1.1 Create Advertiser

**Via UI:**
- [ ] Navigate to **Traffic > Advertisers**
- [ ] Click **"Add Advertiser"** button
- [ ] Fill in required fields:
  - [ ] Name: "Test Advertiser - [Date]"
  - [ ] Contact Name: "Test Contact"
  - [ ] Email: "test@advertiser.com"
  - [ ] Phone: "555-1234"
  - [ ] Payment Terms: "Net 30"
  - [ ] Credit Limit: $10,000
- [ ] Click **"Save"**
- [ ] Verify advertiser appears in list

**Expected Result:** Advertiser created successfully with ID assigned

**Via API:**
```bash
POST /advertisers
{
  "name": "Test Advertiser",
  "contact_name": "Test Contact",
  "email": "test@advertiser.com",
  "phone": "555-1234",
  "payment_terms": "Net 30",
  "credit_limit": 10000.00
}
```

**Verify:**
- [ ] Response status: 200/201
- [ ] Response includes `id` field
- [ ] Advertiser data matches input

---

### 1.2 Create Order

**Via UI:**
- [ ] Navigate to **Traffic > Orders**
- [ ] Click **"New Order"** button
- [ ] Fill in order details:
  - [ ] Advertiser: Select test advertiser
  - [ ] Start Date: Tomorrow
  - [ ] End Date: 30 days from tomorrow
  - [ ] Total Spots: 10
  - [ ] Spot Lengths: 30 seconds
  - [ ] Rate Type: ROS
  - [ ] Total Value: $500.00
- [ ] Click **"Save"**
- [ ] Note the order number (format: YYYYMMDD-XXXX)

**Expected Result:** Order created with status DRAFT and auto-generated order number

**Via API:**
```bash
POST /orders
{
  "advertiser_id": <advertiser_id>,
  "start_date": "2024-01-15",
  "end_date": "2024-02-14",
  "total_spots": 10,
  "spot_lengths": [30],
  "rate_type": "ROS",
  "total_value": 500.00,
  "status": "DRAFT"
}
```

**Verify:**
- [ ] Response status: 200/201
- [ ] Order number format: YYYYMMDD-XXXX
- [ ] Order status: DRAFT
- [ ] All fields match input

---

### 1.3 Verify Order Number Generation

- [ ] Check order number format
- [ ] Verify it follows YYYYMMDD-XXXX pattern
- [ ] Verify date prefix matches current date
- [ ] Verify sequential number increments correctly

**Expected Result:** Order number in correct format

---

## Step 2: Produced In House (Production)

### 2.1 Create Production Order (Optional)

**Via UI:**
- [ ] Navigate to **Production > Production Orders**
- [ ] Click **"New Production Order"**
- [ ] Fill in production details:
  - [ ] Client Name: "Test Advertiser"
  - [ ] Spot Lengths: 30 seconds
  - [ ] Deadline: 7 days from today
  - [ ] Instructions: "Test production"
- [ ] Click **"Save"**

**Expected Result:** Production order created

---

### 2.2 Upload Copy

**Via UI:**
- [ ] Navigate to **Traffic > Copy Library**
- [ ] Click **"Upload Copy"** button
- [ ] Fill in copy details:
  - [ ] Order: Select test order
  - [ ] Advertiser: Auto-filled
  - [ ] Title: "Test Copy - [Date]"
  - [ ] Script Text: "This is a test commercial script."
  - [ ] Expiration Date: 60 days from today
- [ ] Click **"Save"**

**Expected Result:** Copy created and linked to order

**Via API:**
```bash
POST /copy
{
  "order_id": <order_id>,
  "advertiser_id": <advertiser_id>,
  "title": "Test Copy",
  "script_text": "Test commercial script",
  "expires_at": "2024-03-15"
}
```

**Verify:**
- [ ] Response status: 200/201
- [ ] Copy ID assigned
- [ ] Copy linked to order

---

### 2.3 Verify Copy Assignment

- [ ] Navigate to order detail page
- [ ] Verify copy appears in copy list
- [ ] Check copy is assigned to order

**Expected Result:** Copy visible and assigned to order

---

## Step 3: Scheduled (Spot Scheduling)

### 3.1 Approve Order

**Via UI:**
- [ ] Navigate to **Traffic > Orders**
- [ ] Find test order
- [ ] Click **"Approve"** button
- [ ] Confirm approval

**Expected Result:** Order status changes to APPROVED

**Via API:**
```bash
POST /orders/{order_id}/approve
```

**Verify:**
- [ ] Response status: 200
- [ ] Order status: APPROVED
- [ ] Order visible in Spot Scheduler

---

### 3.2 Schedule Spots

**Via UI:**
- [ ] Navigate to **Traffic > Spot Scheduler**
- [ ] Select test order from dropdown
- [ ] Set date range (tomorrow to 10 days out)
- [ ] Select spot length: 30 seconds
- [ ] Click **"Schedule"** button
- [ ] Review preview of spots to be created
- [ ] Click **"Confirm"**

**Expected Result:** 10 spots created and scheduled

**Via API:**
```bash
POST /spots/bulk
{
  "order_id": <order_id>,
  "start_date": "2024-01-15",
  "end_date": "2024-01-25",
  "spot_length": 30,
  "count": 10
}
```

**Verify:**
- [ ] Response status: 200/201
- [ ] Response indicates spots created
- [ ] Spots visible in spots list

---

### 3.3 Verify Spots Created

- [ ] Navigate to **Library > Spots Library**
- [ ] Filter by order
- [ ] Verify 10 spots appear
- [ ] Check spot details:
  - [ ] Scheduled dates are correct
  - [ ] Spot lengths are 30 seconds
  - [ ] Status is SCHEDULED
  - [ ] Times are assigned

**Expected Result:** All 10 spots created with correct details

---

### 3.4 Verify Break Position Assignment

- [ ] Check spot details
- [ ] Verify break positions assigned (A, B, C, etc.)
- [ ] Verify positions match break structure

**Expected Result:** Break positions assigned correctly

---

## Step 4: Added to Log (Log Generation)

### 4.1 Select Date and Clock Template

**Via UI:**
- [ ] Navigate to **Logs > Log Generator**
- [ ] Select **Target Date**: Tomorrow
- [ ] Select **Station**: Choose a station
- [ ] Select **Clock Template**: Choose template for selected station
- [ ] Review template structure

**Expected Result:** Date and template selected

---

### 4.2 Preview Log (Optional)

- [ ] Click **"Preview"** button
- [ ] Review preview:
  - [ ] Scheduled spots appear in ADV slots
  - [ ] Music fills MUS slots
  - [ ] Timing looks correct
  - [ ] No obvious conflicts
- [ ] Close preview

**Expected Result:** Preview shows expected log structure

---

### 4.3 Generate Daily Log

**Via UI:**
- [ ] Click **"Generate Log"** button
- [ ] Wait for generation to complete
- [ ] Log opens in Log Editor

**Expected Result:** Log generated successfully

**Via API:**
```bash
POST /logs/generate
{
  "target_date": "2024-01-15",
  "clock_template_id": <clock_template_id>,
  "station_id": <station_id>
}
```

**Verify:**
- [ ] Response status: 200/201
- [ ] Log ID assigned
- [ ] Log includes scheduled spots

---

### 4.4 Review Generated Log

- [ ] Review timeline view:
  - [ ] Spots placed correctly
  - [ ] Music selected
  - [ ] Timing accurate
  - [ ] Flow is good
- [ ] Check for conflicts:
  - [ ] No conflict warnings
  - [ ] All spots have copy assigned
  - [ ] Timing is correct

**Expected Result:** Log looks correct with spots properly placed

---

### 4.5 Edit Log (If Needed)

- [ ] Make any necessary adjustments:
  - [ ] Move elements
  - [ ] Add spots
  - [ ] Remove elements
  - [ ] Adjust timing
- [ ] Save changes

**Expected Result:** Changes saved successfully

---

## Step 5: Pushed to Automation (LibreTime Publishing)

### 5.1 Publish Log to LibreTime

**Via UI:**
- [ ] Review entire log one final time
- [ ] Click **"Publish"** button
- [ ] Confirm publication
- [ ] Wait for publish to complete

**Expected Result:** Log published successfully

**Via API:**
```bash
POST /logs/{log_id}/publish
```

**Verify:**
- [ ] Response status: 200
- [ ] Log status: PUBLISHED
- [ ] Log is locked (cannot edit)

---

### 5.2 Verify LibreTime Receives Log

- [ ] Check LibreTime interface (if accessible)
- [ ] Verify log appears in LibreTime schedule
- [ ] Check log data matches LibreLog log

**Expected Result:** Log visible in LibreTime

---

### 5.3 Verify Log Status

- [ ] Check log status in LibreLog
- [ ] Verify status is PUBLISHED
- [ ] Attempt to edit log (should be locked)

**Expected Result:** Log locked and cannot be edited

---

## Step 6: Aired (On-Air Playback)

### 6.1 Verify Spot Airing

**Note:** This step requires LibreTime automation to actually play spots.

- [ ] Wait for scheduled spot time
- [ ] Verify spot plays on-air (if monitoring)
- [ ] Check LibreTime playback history
- [ ] Verify spot status updates

**Expected Result:** Spot airs at scheduled time

---

### 6.2 Track Playback

- [ ] Check playback history in LibreTime
- [ ] Verify actual air time recorded
- [ ] Check spot status in LibreLog (may still be SCHEDULED until sync)

**Expected Result:** Playback recorded in LibreTime

---

## Step 7: Reconciled Back (Playback Sync)

### 7.1 Sync Playback History

**Via UI:**
- [ ] Navigate to **Settings > Sync** (if available)
- [ ] Click **"Sync Playback History"**
- [ ] Select date range (yesterday to today)
- [ ] Click **"Sync"**
- [ ] Wait for sync to complete

**Expected Result:** Playback history synced

**Via API:**
```bash
POST /sync/playback-history
{
  "start_date": "2024-01-14",
  "end_date": "2024-01-15"
}
```

**Verify:**
- [ ] Response status: 200
- [ ] Response indicates records synced
- [ ] Playback records created in database

---

### 7.2 Run Reconciliation Report

**Via UI:**
- [ ] Navigate to **Reports > Reconciliation**
- [ ] Set date range
- [ ] Generate report
- [ ] Review report:
  - [ ] Scheduled spots listed
  - [ ] Aired spots matched
  - [ ] Unmatched spots flagged

**Expected Result:** Reconciliation report shows matched spots

**Via API:**
```bash
GET /reports/reconciliation?start_date=2024-01-14&end_date=2024-01-15
```

**Verify:**
- [ ] Response status: 200
- [ ] Report data includes scheduled and aired spots

---

### 7.3 Verify Spot Status Updates

- [ ] Navigate to **Library > Spots Library**
- [ ] Filter by test order
- [ ] Check spot statuses:
  - [ ] Aired spots show status: AIRED
  - [ ] Scheduled spots that didn't air: MISSED or still SCHEDULED
- [ ] Verify actual air times recorded

**Expected Result:** Spot statuses updated correctly

---

### 7.4 Verify Unmatched Spots Flagged

- [ ] Check reconciliation report
- [ ] Look for unmatched spots
- [ ] Verify system flags spots that didn't air

**Expected Result:** Unmatched spots properly flagged

---

## Step 8: Billing (Invoice Generation)

### 8.1 Generate Invoice from Order

**Via UI:**
- [ ] Navigate to **Billing > Invoices**
- [ ] Click **"Generate from Order"**
- [ ] Select test order
- [ ] Review invoice details
- [ ] Click **"Generate"**

**Expected Result:** Invoice created with all aired spots

**Via API:**
```bash
POST /invoices
{
  "order_id": <order_id>,
  "invoice_date": "2024-01-15",
  "due_date": "2024-02-14"
}
```

**Verify:**
- [ ] Response status: 200/201
- [ ] Invoice ID assigned
- [ ] Invoice includes aired spots

---

### 8.2 Verify Invoice Details

- [ ] Open invoice
- [ ] Verify:
  - [ ] All aired spots listed
  - [ ] Pricing is correct
  - [ ] Subtotal calculated correctly
  - [ ] Tax calculated (if applicable)
  - [ ] Total amount correct
  - [ ] Advertiser information correct
  - [ ] Payment terms: Net 30
  - [ ] Due date: 30 days from invoice date

**Expected Result:** Invoice details are accurate

---

### 8.3 Send Invoice

**Via UI:**
- [ ] Open invoice
- [ ] Review one final time
- [ ] Click **"Send Invoice"** button
- [ ] Confirm send

**Expected Result:** Invoice sent successfully

**Via API:**
```bash
POST /invoices/{invoice_id}/send
```

**Verify:**
- [ ] Response status: 200
- [ ] Invoice status: SENT
- [ ] Send date recorded

---

### 8.4 Record Payment (Simulated)

**Via UI:**
- [ ] Navigate to **Billing > Payments**
- [ ] Click **"Record Payment"**
- [ ] Select invoice
- [ ] Enter payment details:
  - [ ] Amount: Full invoice amount
  - [ ] Payment Date: Today
  - [ ] Payment Method: Check
  - [ ] Reference Number: "TEST-001"
- [ ] Click **"Save"**

**Expected Result:** Payment recorded

**Via API:**
```bash
POST /payments
{
  "invoice_id": <invoice_id>,
  "amount": 500.00,
  "payment_date": "2024-01-15",
  "payment_method": "Check",
  "reference_number": "TEST-001"
}
```

**Verify:**
- [ ] Response status: 200/201
- [ ] Payment ID assigned
- [ ] Invoice status: PAID

---

## Verification Checklist

### API Token Authentication

- [ ] All API requests include `Authorization: Bearer <token>` header
- [ ] Token obtained from `/auth/login` endpoint
- [ ] Token refresh works via `/auth/refresh`
- [ ] Invalid tokens are rejected (401)
- [ ] Missing tokens are rejected (401)

### Data Integrity

- [ ] Order number format correct (YYYYMMDD-XXXX)
- [ ] Spots linked to correct order
- [ ] Copy linked to correct order/spots
- [ ] Log includes all scheduled spots
- [ ] Invoice includes all aired spots
- [ ] Payment linked to correct invoice

### Status Transitions

- [ ] Order: DRAFT → PENDING → APPROVED → ACTIVE → COMPLETED
- [ ] Spots: SCHEDULED → AIRED
- [ ] Log: DRAFT → PUBLISHED
- [ ] Invoice: DRAFT → SENT → PAID

---

## Common Issues and Solutions

### Issue: Order Not Appearing for Scheduling

**Solutions:**
- Verify order status is APPROVED
- Check date range is valid
- Verify user has Traffic Manager permissions
- Refresh page

### Issue: Spots Not Scheduling

**Solutions:**
- Verify order is APPROVED
- Check date range is valid
- Verify copy is uploaded
- Check for conflicts

### Issue: Log Generation Fails

**Solutions:**
- Verify clock template exists
- Check scheduled spots exist for target date
- Verify music library has tracks
- Check station is selected
- Verify LibreTime connection

### Issue: Log Publishing Fails

**Solutions:**
- Verify LibreTime API configuration
- Check LibreTime API key
- Verify LibreTime is accessible
- Check network connectivity
- Review backend logs

### Issue: Playback Sync Fails

**Solutions:**
- Verify LibreTime connection
- Check date range is valid
- Verify playback history exists in LibreTime
- Check API credentials

### Issue: Invoice Generation Fails

**Solutions:**
- Verify order is COMPLETED
- Check spots have AIRED status
- Verify order has valid pricing
- Check advertiser information is complete

---

## Test Results Summary

**Date Completed:** _______________

**Steps Completed:**
- [ ] Step 1: Spot Sold
- [ ] Step 2: Produced In House
- [ ] Step 3: Scheduled
- [ ] Step 4: Added to Log
- [ ] Step 5: Pushed to Automation
- [ ] Step 6: Aired
- [ ] Step 7: Reconciled Back
- [ ] Step 8: Billing

**Issues Found:** _______________

**Notes:** _______________

---

## Screenshot Capture Points

Capture screenshots at these points:
1. Order created (with order number visible)
2. Order approved
3. Spots scheduled (showing all spots)
4. Log generated (timeline view)
5. Log published (confirmation)
6. Reconciliation report
7. Invoice generated
8. Invoice sent

---

*Last Updated: [Current Date]*




