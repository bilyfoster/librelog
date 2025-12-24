# LibreLog Workflow Scenarios

## Table of Contents

1. [Scenario 1: Standard ROS Order Flow](#scenario-1-standard-ros-order-flow)
2. [Scenario 2: Daypart-Specific Order Flow](#scenario-2-daypart-specific-order-flow)
3. [Scenario 3: Fixed Time Order Flow](#scenario-3-fixed-time-order-flow)
4. [Scenario 4: Multi-Copy Rotation Flow](#scenario-4-multi-copy-rotation-flow)
5. [Scenario 5: Makegood Flow](#scenario-5-makegood-flow)
6. [Scenario 6: Campaign with Multiple Orders](#scenario-6-campaign-with-multiple-orders)
7. [Timeline Examples](#timeline-examples)
8. [Common Issues and Solutions](#common-issues-and-solutions)

---

## Scenario 1: Standard ROS Order Flow

This scenario walks through the complete workflow for a Run of Schedule (ROS) order from initial customer contact to on-air playback.

### Overview

**Order Details:**
- Advertiser: "Joe's Pizza"
- Order Type: ROS (Run of Schedule)
- Total Spots: 50
- Spot Length: 30 seconds
- Date Range: January 1-31, 2024
- Rate: $50 per spot
- Total Value: $2,500

**Timeline:** 3-5 business days from order entry to first air date

---

### Phase 1: Order Entry (Sales Person)

**Role:** Sales Person  
**Time Required:** 15-30 minutes  
**Prerequisites:** None

#### Step 1.1: Create Advertiser Record

1. Navigate to **Traffic > Advertisers**
2. Click **"Add Advertiser"** button
3. Fill in advertiser information:
   - **Name**: "Joe's Pizza"
   - **Contact Name**: "Joe Smith"
   - **Email**: "joe@joespizza.com"
   - **Phone**: "555-1234"
   - **Address**: "123 Main St, City, State 12345"
   - **Payment Terms**: "Net 30"
   - **Credit Limit**: $5,000
4. Click **"Save"**
5. ✅ Advertiser created successfully

**Screenshot Reference:** `../screenshots/traffic/advertisers/advertiser-form.png`

#### Step 1.2: Create Order

1. Navigate to **Traffic > Orders**
2. Click **"New Order"** button
3. Fill in order details:

   **Basic Information:**
   - **Order Number**: (Auto-generated: 20240101-0001)
   - **Advertiser**: Select "Joe's Pizza"
   - **Agency**: (Leave blank - direct advertiser)
   - **Sales Rep**: Select your name

   **Schedule:**
   - **Start Date**: January 1, 2024
   - **End Date**: January 31, 2024
   - **Total Spots**: 50

   **Spot Details:**
   - **Spot Lengths**: Check "30 seconds"
   - **Rate Type**: Select "ROS" (Run of Schedule)

   **Pricing:**
   - **Rates**: Enter rate structure
     - ROS 30-second: $50 per spot
   - **Total Value**: $2,500 (auto-calculated: 50 × $50)

4. Click **"Save"**
5. ✅ Order created with status: **DRAFT**

**Screenshot Reference:** `../screenshots/traffic/orders/order-form.png`

#### Step 1.3: Upload Copy

1. Navigate to **Traffic > Copy Library**
2. Click **"Upload Copy"** button
3. Fill in copy details:
   - **Order**: Select "20240101-0001 - Joe's Pizza"
   - **Advertiser**: "Joe's Pizza" (auto-filled)
   - **Title**: "Joe's Pizza - 30 Second - January 2024"
   - **Type**: Select "Audio File"
   - **File**: Upload "joes_pizza_30sec_jan2024.mp3"
   - **Version**: 1
   - **Expiration Date**: February 1, 2024
4. Click **"Save"**
5. ✅ Copy uploaded and assigned to order

**Screenshot Reference:** `../screenshots/traffic/copy/copy-upload-form.png`

#### Step 1.4: Submit for Approval

1. Navigate back to **Traffic > Orders**
2. Find order "20240101-0001"
3. Change status from **DRAFT** to **PENDING**
4. Click **"Save"** or status automatically updates
5. ✅ Order submitted for approval
6. 📧 Sales Manager receives notification (if configured)

**Expected Outcome:**
- Order status: **PENDING**
- Order visible to Sales Manager
- Copy available and assigned

---

### Phase 2: Order Approval (Sales Manager)

**Role:** Sales Manager  
**Time Required:** 5-10 minutes  
**Prerequisites:** Order in PENDING status

#### Step 2.1: Review Order

1. Navigate to **Traffic > Orders**
2. Filter by status: **PENDING**
3. Open order "20240101-0001 - Joe's Pizza"
4. Review order details:
   - ✅ Advertiser information correct
   - ✅ Dates are valid (Jan 1-31, 2024)
   - ✅ Spot count reasonable (50 spots over 31 days)
   - ✅ Pricing appropriate ($50/spot for ROS)
   - ✅ Copy uploaded and available

**Screenshot Reference:** `../screenshots/traffic/orders/order-detail-pending.png`

#### Step 2.2: Verify Copy

1. Navigate to **Traffic > Copy Library**
2. Filter by order "20240101-0001"
3. Verify copy:
   - ✅ Audio file uploaded
   - ✅ File plays correctly
   - ✅ Length matches spot length (30 seconds)
   - ✅ Expiration date is after campaign end

#### Step 2.3: Approve Order

1. Return to **Traffic > Orders**
2. Open order "20240101-0001"
3. Click **"Approve"** button
4. Confirm approval
5. ✅ Order status changes to **APPROVED**
6. 📧 Traffic Manager receives notification (if configured)

**Expected Outcome:**
- Order status: **APPROVED**
- Order available for scheduling
- Copy verified and ready

---

### Phase 3: Spot Scheduling (Traffic Manager)

**Role:** Traffic Manager  
**Time Required:** 15-30 minutes  
**Prerequisites:** Order in APPROVED status

#### Step 3.1: Review Approved Order

1. Navigate to **Traffic > Orders**
2. Filter by status: **APPROVED**
3. Open order "20240101-0001 - Joe's Pizza"
4. Review scheduling requirements:
   - ✅ ROS rate type (flexible scheduling)
   - ✅ 50 spots over 31 days (~1.6 spots per day)
   - ✅ 30-second spots
   - ✅ No daypart restrictions
   - ✅ No fixed time requirements

#### Step 3.2: Schedule Spots

1. Navigate to **Traffic > Spot Scheduler**
2. Select order: "20240101-0001 - Joe's Pizza"
3. Set date range:
   - **Start Date**: January 1, 2024
   - **End Date**: January 31, 2024
4. Configure spots:
   - **Spot Length**: 30 seconds
   - **Break Position**: (Leave blank for ROS)
   - **Daypart**: (Leave blank for ROS)
5. Click **"Schedule"** button
6. System automatically distributes 50 spots across 31 days
7. Review scheduled spots preview
8. Click **"Confirm"** to create spots
9. ✅ 50 spots created and scheduled

**Screenshot Reference:** `../screenshots/traffic/spot-scheduler/scheduling-form.png`

**System Behavior:**
- Distributes spots evenly across date range
- Avoids scheduling conflicts
- Respects ROS flexibility (any time of day)
- Assigns unique scheduled times

#### Step 3.3: Review Scheduled Spots

1. View scheduled spots list
2. Verify:
   - ✅ 50 spots total
   - ✅ Distributed across January 1-31
   - ✅ All spots are 30 seconds
   - ✅ No scheduling conflicts
   - ✅ Times are reasonable (6 AM - 11 PM)

#### Step 3.4: Assign Copy to Spots

1. Navigate to **Traffic > Copy Library**
2. Find copy "Joe's Pizza - 30 Second - January 2024"
3. Click **"Assign to Spots"** or use Copy Assignment component
4. Select all 50 spots for this order
5. Click **"Assign"**
6. ✅ Copy assigned to all spots

**Alternative Method:**
- Copy may be auto-assigned when spots are created
- Verify assignments in Copy Library

**Expected Outcome:**
- 50 spots scheduled
- All spots have copy assigned
- Spots ready for log generation
- Order status may update to **ACTIVE**

---

### Phase 4: Log Generation (Log Generator)

**Role:** Log Generator  
**Time Required:** 10-20 minutes per day  
**Prerequisites:** Spots scheduled, clock template exists

#### Step 4.1: Select Date and Clock Template

1. Navigate to **Logs > Log Generator**
2. Select **Target Date**: January 1, 2024
3. Select **Clock Template**: "Weekday Standard" (or appropriate template)
4. Review template structure

**Screenshot Reference:** `../screenshots/logs/log-generation-form.png`

#### Step 4.2: Preview Log (Optional)

1. Click **"Preview"** button
2. Review preview:
   - ✅ Scheduled spots appear in ADV slots
   - ✅ Music fills MUS slots
   - ✅ IDs, news, other elements placed
   - ✅ Timing looks correct
   - ✅ No obvious conflicts
3. Close preview

#### Step 4.3: Generate Daily Log

1. Click **"Generate Log"** button
2. System generates log:
   - Places scheduled spots in ADV slots
   - Selects music from library for MUS slots
   - Places IDs, news, other elements
   - Calculates precise timing
3. Wait for generation to complete
4. ✅ Log generated successfully

**System Process:**
- Retrieves all spots scheduled for January 1
- Matches spots to ADV slots in clock template
- Selects music tracks for MUS slots
- Calculates timing for all elements
- Creates complete hourly schedule

#### Step 4.4: Review Generated Log

1. Log opens automatically in Log Editor
2. Review timeline view:
   - ✅ Spots placed correctly
   - ✅ Music selected appropriately
   - ✅ Timing is accurate
   - ✅ Flow is good
3. Check for conflicts:
   - ✅ No conflict warnings
   - ✅ All spots have copy assigned
   - ✅ Timing is correct

**Screenshot Reference:** `../screenshots/logs/log-editor-timeline.png`

#### Step 4.5: Edit Log (If Needed)

If adjustments are needed:

1. **Move Elements**: Drag spots to different times
2. **Add Spots**: Add additional spots if needed
3. **Remove Elements**: Delete unwanted elements
4. **Adjust Timing**: Modify element timing
5. Save changes

#### Step 4.6: Publish to LibreTime

1. Review entire log one final time
2. Click **"Publish"** button
3. Confirm publication
4. System sends log to LibreTime
5. ✅ Log status: **PUBLISHED**
6. ⚠️ Log is now locked (cannot be edited)

**Screenshot Reference:** `../screenshots/logs/log-publish-dialog.png`

**Expected Outcome:**
- Log published to LibreTime
- Log appears in LibreTime schedule
- Log is locked for editing
- Ready for on-air playback

**Repeat for Each Day:**
- Generate logs for January 2, 3, 4, etc.
- Each day's log includes spots scheduled for that day
- Publish logs in advance (typically 1-7 days ahead)

---

### Phase 5: On-Air (Automation System)

**Role:** Automation System (LibreTime)  
**Time Required:** Automatic  
**Prerequisites:** Log published to LibreTime

#### Step 5.1: LibreTime Receives Log

1. LibreTime receives published log
2. Log appears in LibreTime schedule
3. Automation system prepares for playback

#### Step 5.2: Spots Air

1. At scheduled time, LibreTime plays spot
2. Spot audio file plays on-air
3. System records actual air time
4. Spot status updates to **AIRED**

**Example Timeline:**
- **Scheduled Time**: January 1, 2024 at 8:15:00 AM
- **Actual Air Time**: January 1, 2024 at 8:15:03 AM (3-second delay)
- **Status**: **AIRED**

#### Step 5.3: Track Airings

1. System tracks each spot as it airs
2. Records actual air times
3. Updates spot status from **SCHEDULED** to **AIRED**
4. Logs playback in playback history

**Expected Outcome:**
- All 50 spots air during January 1-31
- Each spot status: **AIRED**
- Actual air times recorded
- Order status: **COMPLETED** (after all spots air)

---

### Phase 6: Billing (Billing Specialist)

**Role:** Billing Specialist  
**Time Required:** 15-30 minutes  
**Prerequisites:** Order completed (all spots aired)

#### Step 6.1: Generate Invoice

1. Navigate to **Billing > Invoices**
2. Click **"Generate from Order"**
3. Select order: "20240101-0001 - Joe's Pizza"
4. System creates invoice:
   - Lists all 50 aired spots
   - Calculates subtotal: $2,500
   - Calculates tax (if applicable)
   - Sets total amount
5. Review invoice details
6. ✅ Invoice created with status: **DRAFT**

**Screenshot Reference:** `../screenshots/billing/invoices/invoice-detail.png`

#### Step 6.2: Review Invoice

1. Open invoice
2. Verify:
   - ✅ All 50 spots listed
   - ✅ Pricing is correct ($50 per spot)
   - ✅ Subtotal: $2,500
   - ✅ Tax calculated correctly
   - ✅ Total amount correct
   - ✅ Advertiser information correct
   - ✅ Payment terms: Net 30
   - ✅ Due date: 30 days from invoice date

#### Step 6.3: Send Invoice

1. Click **"Send Invoice"** button
2. System:
   - Generates PDF invoice
   - Emails to "joe@joespizza.com"
   - Updates status to **SENT**
   - Records send date
3. ✅ Invoice sent successfully

**Screenshot Reference:** `../screenshots/billing/invoices/invoice-sent.png`

#### Step 6.4: Track Payment

1. Wait for payment (Net 30 terms)
2. When payment received:
   - Navigate to **Billing > Payments**
   - Click **"Record Payment"**
   - Select invoice
   - Enter payment details:
     - Amount: $2,500
     - Payment Date: [Date received]
     - Payment Method: Check/ACH/Credit Card
     - Reference Number: [Check number or reference]
   - Click **"Save"**
3. ✅ Invoice status: **PAID**

**Expected Outcome:**
- Invoice generated and sent
- Payment received and recorded
- Order complete and paid
- Process complete

---

### Complete Timeline Summary

| Day | Phase | Action | Role | Status |
|-----|-------|--------|------|--------|
| Day 1 | Order Entry | Create advertiser, order, upload copy | Sales Person | DRAFT → PENDING |
| Day 1 | Approval | Review and approve order | Sales Manager | PENDING → APPROVED |
| Day 1-2 | Scheduling | Schedule 50 spots | Traffic Manager | Spots SCHEDULED |
| Day 2-31 | Log Generation | Generate daily logs | Log Generator | Logs PUBLISHED |
| Jan 1-31 | On-Air | Spots air on schedule | Automation | Spots AIRED |
| Feb 1 | Billing | Generate and send invoice | Billing | Invoice SENT |
| Mar 1 | Payment | Receive and record payment | Billing | Invoice PAID |

---

## Scenario 2: Daypart-Specific Order Flow

This scenario covers an order that requires spots to air only during specific dayparts (e.g., Morning Drive only).

### Overview

**Order Details:**
- Advertiser: "ABC Car Dealership"
- Order Type: DAYPART (Morning Drive only)
- Total Spots: 30
- Spot Length: 60 seconds
- Date Range: February 1-28, 2024
- Daypart: Morning Drive (6:00 AM - 10:00 AM)
- Rate: $150 per spot
- Total Value: $4,500

### Key Differences from ROS Flow

#### Step 2.1: Create Order with Daypart Rate Type

1. When creating order, select:
   - **Rate Type**: **DAYPART**
   - **Daypart**: Morning Drive (6:00 AM - 10:00 AM)
   - System validates daypart exists

#### Step 3.2: Schedule Spots with Daypart Restriction

1. In Spot Scheduler:
   - Select order
   - **Daypart**: Select "Morning Drive"
   - System only schedules spots between 6:00 AM - 10:00 AM
   - Spots distributed across Morning Drive hours only

**System Behavior:**
- Only schedules spots during Morning Drive hours (6 AM - 10 AM)
- Validates daypart compliance
- Rejects spots outside daypart hours
- Ensures proper distribution across Morning Drive

#### Validation

- System checks each scheduled spot is within daypart hours
- Warns if spot scheduled outside daypart
- Prevents publishing logs with daypart violations

---

## Scenario 3: Fixed Time Order Flow

This scenario covers an order requiring spots to air at exact, specified times.

### Overview

**Order Details:**
- Advertiser: "Lunch Special Restaurant"
- Order Type: FIXED_TIME
- Total Spots: 31 (one per day)
- Spot Length: 30 seconds
- Date Range: March 1-31, 2024
- Fixed Times: 12:00 PM (noon) every day
- Rate: $75 per spot
- Total Value: $2,325

### Key Differences

#### Step 2.1: Create Order with Fixed Time Rate Type

1. Select:
   - **Rate Type**: **FIXED_TIME**
   - **Fixed Times**: 12:00 PM (noon)
   - System records exact time requirements

#### Step 3.2: Schedule Spots at Fixed Times

1. In Spot Scheduler:
   - System schedules exactly at 12:00 PM each day
   - No flexibility in timing
   - Ensures no conflicts at fixed times

**System Behavior:**
- Schedules spots at exact specified times
- Checks for conflicts at fixed times
- Warns if fixed time slot unavailable
- Requires conflict resolution before scheduling

#### Conflict Resolution

- If another spot already scheduled at 12:00 PM:
  - System warns of conflict
  - Traffic Manager must resolve:
    - Move conflicting spot
    - Cancel conflicting spot
    - Negotiate with advertiser for different time

---

## Scenario 4: Multi-Copy Rotation Flow

This scenario covers an order with multiple copy versions that rotate automatically.

### Overview

**Order Details:**
- Advertiser: "Seasonal Store"
- Order Type: ROS
- Total Spots: 60
- Spot Length: 30 seconds
- Date Range: April 1-30, 2024
- Copy Versions: 3 different versions
- Rotation: Random rotation

### Key Steps

#### Step 1.3: Upload Multiple Copy Versions

1. Upload Copy Version 1:
   - Title: "Seasonal Store - Version 1 - Spring"
   - File: seasonal_store_v1_spring.mp3
   - Version: 1

2. Upload Copy Version 2:
   - Title: "Seasonal Store - Version 2 - Spring"
   - File: seasonal_store_v2_spring.mp3
   - Version: 2

3. Upload Copy Version 3:
   - Title: "Seasonal Store - Version 3 - Spring"
   - File: seasonal_store_v3_spring.mp3
   - Version: 3

#### Step 3.3: Create Rotation Rule

1. Navigate to **Traffic > Rotation Rules**
2. Create rule:
   - **Name**: "Seasonal Store Rotation"
   - **Rotation Type**: Random
   - **Campaign/Order**: Link to order
   - **Min Separation**: 30 minutes (spots don't play too close)
   - **Max Per Hour**: 2
   - **Max Per Day**: 3

#### Step 3.4: Assign Multiple Copy Versions

1. Assign all 3 versions to order
2. System rotates versions automatically
3. Ensures versions don't play too close together

**System Behavior:**
- Randomly selects from 3 versions for each spot
- Ensures 30-minute separation between spots
- Limits to 2 spots per hour, 3 per day
- Prevents listener fatigue

---

## Scenario 5: Makegood Flow

This scenario covers handling a missed spot and creating a makegood (free replacement).

### Overview

**Situation:**
- Spot scheduled for January 15, 2024 at 8:30 AM
- Spot did not air (technical issue)
- Need to create makegood

### Steps

#### Step 5.1: Identify Missed Spot

1. Navigate to **Traffic > Spot Scheduler** or **Library > Spots**
2. Find spot scheduled for Jan 15, 8:30 AM
3. Verify spot status: **MISSED** (or still **SCHEDULED** but didn't air)
4. Confirm spot did not air (check playback history)

#### Step 5.2: Create Makegood

1. Navigate to **Billing > Makegoods**
2. Click **"Create Makegood"**
3. Select missed spot: "Jan 15, 8:30 AM - Joe's Pizza"
4. Enter reason: "Technical issue - equipment failure"
5. System creates makegood record

#### Step 5.3: Schedule Replacement Spot

1. System automatically schedules replacement spot
2. Or manually schedule:
   - Navigate to **Traffic > Spot Scheduler**
   - Select order
   - Schedule replacement spot
   - Mark as makegood
3. Replacement spot is **FREE** (no charge)

**Makegood Requirements:**
- ✅ Comparable placement (similar daypart/time)
- ✅ Same spot length (30 seconds)
- ✅ Scheduled as soon as possible
- ✅ Free to advertiser ($0)

#### Step 5.4: Track Makegood

1. Makegood appears in Makegoods list
2. Status: **SCHEDULED** → **AIRED**
3. Makegood tracked for contract fulfillment
4. Appears on invoice with $0 amount (for records)

**Expected Outcome:**
- Makegood created and scheduled
- Replacement spot airs
- Advertiser receives free spot
- Contract fulfilled

---

## Scenario 6: Campaign with Multiple Orders

This scenario covers managing a campaign that spans multiple orders.

### Overview

**Campaign:** "Holiday Sale 2024"
- Order 1: General sale (50 spots, Dec 1-15)
- Order 2: Last minute deals (30 spots, Dec 15-24)
- Order 3: After-Christmas sale (20 spots, Dec 26-31)

### Steps

#### Step 1: Create Campaign

1. Navigate to campaign management (if available)
2. Create campaign: "Holiday Sale 2024"
3. Set campaign dates: December 1-31, 2024

#### Step 2: Create Multiple Orders

1. **Order 1**: General Sale
   - 50 spots, Dec 1-15
   - ROS rate type
   - Create and approve

2. **Order 2**: Last Minute Deals
   - 30 spots, Dec 15-24
   - Daypart: Afternoon Drive
   - Create and approve

3. **Order 3**: After-Christmas Sale
   - 20 spots, Dec 26-31
   - ROS rate type
   - Create and approve

#### Step 3: Link Orders to Campaign

1. Link all three orders to "Holiday Sale 2024" campaign
2. System tracks campaign performance

#### Step 4: Schedule Campaign Spots

1. Schedule spots for each order
2. System ensures campaign spots don't conflict
3. Rotate copy versions across campaign

#### Step 5: Track Campaign Performance

1. View campaign dashboard
2. See performance across all orders
3. Track total spots, revenue, etc.

---

## Timeline Examples

### Fast-Track Order (Rush)

**Timeline:** Same day or next day air

- **9:00 AM**: Sales creates order
- **9:30 AM**: Sales uploads copy
- **10:00 AM**: Sales Manager approves
- **10:30 AM**: Traffic Manager schedules spots
- **11:00 AM**: Log Generator generates today's log
- **11:30 AM**: Log published
- **12:00 PM**: Spot airs (if scheduled for today)

### Standard Order

**Timeline:** 3-5 business days

- **Day 1**: Order entry and approval
- **Day 2**: Spot scheduling
- **Day 3-5**: Log generation (for future dates)
- **Day 6+**: Spots begin airing

### Long-Term Campaign

**Timeline:** Weeks or months

- **Week 1**: Order entry, approval, initial scheduling
- **Ongoing**: Daily log generation
- **Throughout**: Spots air on schedule
- **End**: Billing and payment

---

## Common Issues and Solutions

### Issue: Order Not Appearing for Scheduling

**Problem:** Approved order not showing in Spot Scheduler

**Solutions:**
1. Verify order status is **APPROVED** (not PENDING)
2. Check date range - order must have valid start/end dates
3. Verify user has Traffic Manager permissions
4. Refresh page or clear cache
5. Check order hasn't been cancelled

### Issue: Spots Not Scheduling

**Problem:** Cannot schedule spots from approved order

**Solutions:**
1. Verify order is **APPROVED**
2. Check date range is valid (not in past)
3. Verify spot lengths match order requirements
4. Check for daypart restrictions
5. Verify copy is uploaded
6. Check system logs for errors

### Issue: Copy Not Assigning to Spots

**Problem:** Copy uploads but doesn't assign to spots

**Solutions:**
1. Verify copy is linked to order
2. Manually assign copy to spots
3. Check copy expiration date (must be after spot date)
4. Verify copy file is valid audio format
5. Re-upload copy if needed

### Issue: Log Generation Fails

**Problem:** Cannot generate daily log

**Solutions:**
1. Verify clock template exists and is valid
2. Check scheduled spots exist for target date
3. Verify music library has tracks
4. Check for system errors in logs
5. Verify LibreTime connection
6. Try preview first to identify issues

### Issue: Spots Not Airing

**Problem:** Spots scheduled but not playing on-air

**Solutions:**
1. Verify log was published to LibreTime
2. Check LibreTime schedule for log
3. Verify spot times are correct
4. Check copy file is accessible
5. Verify LibreTime automation is running
6. Check playback history for errors
7. Create makegood if spot missed

### Issue: Invoice Generation Errors

**Problem:** Cannot generate invoice from order

**Solutions:**
1. Verify order is **COMPLETED** (all spots aired)
2. Check spots have **AIRED** status
3. Verify order has valid pricing
4. Check advertiser information is complete
5. Verify billing permissions
6. Check system logs for errors

---

## Best Practices Summary

### For Sales People

- Create advertisers before orders
- Upload copy immediately after order creation
- Submit orders for approval promptly
- Keep order information accurate
- Communicate special requirements

### For Sales Managers

- Review orders promptly
- Verify copy availability before approving
- Check pricing appropriateness
- Document approval/rejection reasons
- Communicate with sales team

### For Traffic Managers

- Schedule spots well in advance
- Resolve conflicts immediately
- Verify copy assignments
- Check daypart compliance
- Balance inventory across dayparts

### For Log Generators

- Generate logs in advance (1-7 days)
- Review previews before generating
- Resolve conflicts before publishing
- Publish on schedule
- Keep backups of important logs

### For Billing Specialists

- Generate invoices promptly after order completion
- Verify all spots before invoicing
- Send invoices on schedule
- Track payments accurately
- Follow up on overdue accounts

---

*Last Updated: [Current Date]*



