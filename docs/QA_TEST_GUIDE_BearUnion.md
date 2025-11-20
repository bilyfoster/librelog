# QA Testing Guide: BearUnion Campaign Setup

## Overview

This guide walks through setting up and testing the BearUnion campaign for MustachePHX client. This tests the complete traffic workflow from client creation through spot scheduling.

## Test Configuration

- **Client**: MustachePHX
- **Campaign**: BearUnion
- **Audio File**: BearUnion-Nov25.mp3 (already in system)
- **Spots**: 10 total spots
- **Schedule**: 3 spots per day starting November 19, 2024
- **Spot Length**: 30 seconds (actual file is 35 seconds)
- **Rate Type**: ROS (Run of Schedule)
- **Pricing**: Trade at $10 per spot = $100 total
- **Sales Rep**: Donny Demo
- **Agency**: None (direct advertiser)
- **Approval**: Yes (DRAFT → PENDING → APPROVED)
- **End Date**: November 22, 2024

---

## Quick Setup (Automated)

Run the automated setup script to create all prerequisites:

```bash
cd /home/jenkins/docker/librelog/backend
python -m scripts.create_qa_test_data
```

This will create:
- Standard dayparts and categories
- Donny Demo sales rep
- MustachePHX advertiser
- BearUnion order (DRAFT status)
- Link BearUnion-Nov25.mp3 copy to order (if found)

**Then proceed to Phase 5 (Approve Order) below.**

---

## Manual Setup (Step-by-Step)

### Phase 1: Create Standard Daypart Categories (Optional but Recommended)

**Purpose**: Organize dayparts into logical groups for easier management.

1. Navigate to **Traffic > Daypart Categories** (`/traffic/daypart-categories`)
2. Create the following categories:

   **Category 1: Drive Time**
   - Name: `Drive Time`
   - Description: `High-audience drive time periods`
   - Color: `#1976d2` (blue)
   - Sort Order: `1`
   - Click "Save"

   **Category 2: Prime Time**
   - Name: `Prime Time`
   - Description: `Prime listening hours`
   - Color: `#d32f2f` (red)
   - Sort Order: `2`
   - Click "Save"

   **Category 3: Off-Peak**
   - Name: `Off-Peak`
   - Description: `Lower audience periods`
   - Color: `#757575` (grey)
   - Sort Order: `3`
   - Click "Save"

   **Category 4: Weekend**
   - Name: `Weekend`
   - Description: `Weekend programming`
   - Color: `#388e3c` (green)
   - Sort Order: `4`
   - Click "Save"

---

### Phase 2: Create Standard Dayparts

**Purpose**: Define time periods for scheduling and pricing.

1. Navigate to **Traffic > Dayparts** (`/traffic/dayparts`)
2. Create the following dayparts:

   **Daypart 1: Morning Drive**
   - Name: `Morning Drive`
   - Start Time: `06:00`
   - End Time: `10:00`
   - Days of Week: `Monday, Tuesday, Wednesday, Thursday, Friday`
   - Category: `Drive Time` (if categories created)
   - Description: `Morning drive time - highest audience`
   - Active: `Yes`
   - Click "Save"

   **Daypart 2: Midday**
   - Name: `Midday`
   - Start Time: `10:00`
   - End Time: `15:00`
   - Days of Week: `Monday, Tuesday, Wednesday, Thursday, Friday`
   - Category: `Prime Time`
   - Description: `Midday programming`
   - Active: `Yes`
   - Click "Save"

   **Daypart 3: Afternoon Drive**
   - Name: `Afternoon Drive`
   - Start Time: `15:00`
   - End Time: `19:00`
   - Days of Week: `Monday, Tuesday, Wednesday, Thursday, Friday`
   - Category: `Drive Time`
   - Description: `Afternoon drive time - high audience`
   - Active: `Yes`
   - Click "Save"

   **Daypart 4: Evening**
   - Name: `Evening`
   - Start Time: `19:00`
   - End Time: `00:00` (midnight)
   - Days of Week: `Monday, Tuesday, Wednesday, Thursday, Friday`
   - Category: `Prime Time`
   - Description: `Evening programming`
   - Active: `Yes`
   - Click "Save"

   **Daypart 5: Overnight**
   - Name: `Overnight`
   - Start Time: `00:00` (midnight)
   - End Time: `06:00`
   - Days of Week: `Monday, Tuesday, Wednesday, Thursday, Friday`
   - Category: `Off-Peak`
   - Description: `Overnight programming`
   - Active: `Yes`
   - Click "Save"

   **Daypart 6: Weekend Morning**
   - Name: `Weekend Morning`
   - Start Time: `08:00`
   - End Time: `12:00`
   - Days of Week: `Saturday, Sunday`
   - Category: `Weekend`
   - Description: `Weekend morning programming`
   - Active: `Yes`
   - Click "Save"

   **Daypart 7: Weekend Afternoon**
   - Name: `Weekend Afternoon`
   - Start Time: `12:00`
   - End Time: `18:00`
   - Days of Week: `Saturday, Sunday`
   - Category: `Weekend`
   - Description: `Weekend afternoon programming`
   - Active: `Yes`
   - Click "Save"

   **Daypart 8: Weekend Evening**
   - Name: `Weekend Evening`
   - Start Time: `18:00`
   - End Time: `00:00` (midnight)
   - Days of Week: `Saturday, Sunday`
   - Category: `Weekend`
   - Description: `Weekend evening programming`
   - Active: `Yes`
   - Click "Save"

---

### Phase 3: Create Donny Demo Sales Rep

**Purpose**: Create a demo sales rep to assign to the order.

1. Navigate to **Traffic > Sales Reps** (`/traffic/sales-reps`)
2. Click "Add Sales Rep" button
3. Fill in the form:
   - **User**: 
     - If user "donnydemo" doesn't exist, you'll need to create it first in Admin > Users
     - Username: `donnydemo`
     - Password: `demo123`
     - Role: `sales`
     - Email: `donny@librelog.local`
   - **Employee ID**: `DEMO-001`
   - **Commission Rate**: `10` (10%)
   - **Sales Goal**: (optional, leave blank)
4. Click "Save"

**Note**: If creating the user first:
- Navigate to **Admin > Users** (`/admin/users`)
- Click "Add User"
- Fill in user details
- Save user
- Then return to Sales Reps to create the sales rep

---

### Phase 4: Create MustachePHX Advertiser (Client)

**Purpose**: Create the client/advertiser record.

1. Navigate to **Traffic > Advertisers** (`/traffic/advertisers`)
2. Click "Add Advertiser" button
3. Fill in the form:
   - **Name**: `MustachePHX` (required)
   - **Contact Name**: `Demo Contact`
   - **Email**: `contact@mustachephx.com`
   - **Phone**: `602-555-0000`
   - **Address**: `123 Demo St, Phoenix, AZ 85001`
   - **Tax ID**: (optional, leave blank)
   - **Payment Terms**: `Net 30`
   - **Credit Limit**: `5000`
   - **Active**: `Yes` (checked)
4. Click "Save"
5. Verify advertiser appears in the list

---

### Phase 5: Verify and Link BearUnion Copy

**Purpose**: Verify the copy exists and link it to the order (will be created in next phase).

1. Navigate to **Traffic > Copy Library** (`/traffic/copy`)
2. Search for "BearUnion" or "Nov25"
3. Verify copy exists:
   - Title should contain "BearUnion" or "Nov25"
   - Audio file should be BearUnion-Nov25.mp3
   - File should be accessible
4. Note the copy ID for reference
5. If copy doesn't exist, you'll need to upload it:
   - Click "Upload Copy"
   - Title: `BearUnion - November 2024`
   - Advertiser: `MustachePHX`
   - Audio File: Upload `BearUnion-Nov25.mp3`
   - Expiration Date: `2024-12-01` (or later)
   - Click "Save"

---

### Phase 6: Create BearUnion Order

**Purpose**: Create the advertising order/contract.

1. Navigate to **Traffic > Orders** (`/traffic/orders`)
2. Click "New Order" button
3. Fill in order details:

   **Basic Information:**
   - **Order Number**: Leave blank (will auto-generate as `20241119-0001`)
   - **Advertiser**: Select `MustachePHX` (required)
   - **Agency**: Leave blank (no agency)
   - **Sales Rep**: Select `Donny Demo` (donnydemo)

   **Schedule:**
   - **Start Date**: `2024-11-19` (November 19, 2024)
   - **End Date**: `2024-11-22` (November 22, 2024)
   - **Total Spots**: `10`

   **Spot Details:**
   - **Spot Lengths**: `30` (30 seconds)
   - **Rate Type**: Select `ROS` (Run of Schedule)

   **Pricing:**
   - **Rates**: Enter `10.00` (for ROS rate)
   - **Total Value**: `100.00` (10 spots × $10, or auto-calculated)

   **Status:**
   - **Status**: `DRAFT` (default)
   - **Approval Status**: `NOT_REQUIRED` (will change to PENDING when submitted)

4. Click "Save"
5. Verify order is created with status **DRAFT**
6. Note the order number (e.g., `20241119-0001`)

---

### Phase 7: Link Copy to Order

**Purpose**: Associate the BearUnion-Nov25.mp3 copy with the order.

1. Navigate to **Traffic > Copy Library** (`/traffic/copy`)
2. Find the BearUnion copy (search for "BearUnion" or "Nov25")
3. Click the edit/view icon to open copy details
4. Verify or update:
   - **Order**: Select the BearUnion order (`20241119-0001`)
   - **Advertiser**: Should be `MustachePHX`
   - **Title**: `BearUnion - November 2024`
   - **Audio File**: BearUnion-Nov25.mp3 should be uploaded
5. Click "Save" to update copy with order link

**Alternative Method:**
- Open the order detail page
- Navigate to copy section
- Assign copy to order from there

---

### Phase 8: Submit Order for Approval

**Purpose**: Change order status to PENDING for approval workflow.

1. Navigate to **Traffic > Orders** (`/traffic/orders`)
2. Find the BearUnion order (`20241119-0001`)
3. Click the edit icon to open order
4. Change **Status** from `DRAFT` to `PENDING`
5. Click "Save"
6. Verify order status is now **PENDING**

---

### Phase 9: Approve Order

**Purpose**: Approve the order so spots can be scheduled.

1. Navigate to **Traffic > Orders** (`/traffic/orders`)
2. Find the BearUnion order with status **PENDING**
3. Click on the order to view details
4. Click the **"Approve"** button
5. Confirm approval
6. Verify order status changes to **APPROVED**
7. Verify approval status also changes to **APPROVED**

---

### Phase 10: Schedule Spots

**Purpose**: Schedule the 10 spots across the date range.

1. Navigate to **Traffic > Spot Scheduler** (`/traffic/spot-scheduler`)
2. **Select Order**: Choose `20241119-0001 - BearUnion - MustachePHX` from dropdown
3. **Set Date Range**:
   - **Start Date**: `2024-11-19`
   - **End Date**: `2024-11-22`
4. **Configure Spots**:
   - **Spot Length**: `30` (30 seconds)
   - **Break Position**: Leave blank (ROS - any position)
   - **Daypart**: Leave blank (ROS - any time)
5. Click **"Schedule"** button
6. System will generate spots and show preview
7. Review the preview:
   - Should show 10 spots total
   - Distributed across 11/19, 11/20, 11/21, 11/22
   - Approximately 3 spots per day (may vary)
   - All spots should be 30 seconds
8. Click **"Confirm"** to create the spots
9. Verify success message

---

### Phase 11: Verify Spots

**Purpose**: Verify spots were created correctly with copy assigned.

1. Navigate to **Library > Spots Library** (`/library/spots`) or **Traffic > Spot Scheduler**
2. Filter by:
   - **Order**: BearUnion order
   - **Date Range**: 11/19 to 11/22
3. Verify:
   - ✅ **10 spots total** are listed
   - ✅ Spots are distributed across dates (11/19, 11/20, 11/21, 11/22)
   - ✅ Each spot has a **scheduled_date** and **scheduled_time**
   - ✅ All spots are **30 seconds** length
   - ✅ Spot status is **SCHEDULED**
   - ✅ Copy is assigned to spots (check copy assignment)
4. Check spot distribution:
   - Should be approximately 3 spots per day
   - Times should be spread throughout the day (ROS allows any time)
5. Verify copy assignment:
   - Each spot should have BearUnion-Nov25.mp3 copy assigned
   - Check in spot details or copy assignment view

---

### Phase 12: Generate Test Log (Optional)

**Purpose**: Test log generation with scheduled spots.

1. Navigate to **Logs > Log Generator** (`/logs`)
2. Select **Target Date**: `2024-11-19` (first day with spots)
3. Select **Clock Template**: Choose your clock template
4. Click **"Preview"** button (optional):
   - Review preview to see how spots will be placed
   - Verify spots appear in ADV slots
   - Check timing looks correct
5. Click **"Generate Log"** button
6. Wait for generation to complete
7. Verify log is created:
   - Log ID is generated
   - Status is **DRAFT**
   - Total duration is calculated
8. Open the generated log:
   - Navigate to **Logs** list
   - Click on the log for 11/19
   - Review timeline view
9. Verify in log:
   - ✅ Scheduled spots appear in the log
   - ✅ Spots are placed in ADV slots according to clock template
   - ✅ Spot times match scheduled times
   - ✅ Copy is linked (BearUnion-Nov25.mp3)
   - ✅ Timing is correct
10. (Optional) Publish log to LibreTime:
    - Click "Publish" button
    - Confirm publication
    - Verify status changes to **PUBLISHED**

---

## Verification Checklist

After completing all phases, verify:

### Advertiser
- [ ] MustachePHX advertiser exists
- [ ] Contact information is complete
- [ ] Payment terms set to Net 30
- [ ] Credit limit set appropriately

### Sales Rep
- [ ] Donny Demo sales rep exists
- [ ] Linked to user "donnydemo"
- [ ] Commission rate set to 10%

### Dayparts
- [ ] 8 standard dayparts created (5 weekday + 3 weekend)
- [ ] Daypart categories created (optional)
- [ ] Dayparts assigned to categories (if created)
- [ ] All dayparts are active

### Order
- [ ] BearUnion order created (20241119-0001)
- [ ] Advertiser: MustachePHX
- [ ] Sales Rep: Donny Demo
- [ ] Dates: 11/19/2024 to 11/22/2024
- [ ] Total Spots: 10
- [ ] Spot Length: 30 seconds
- [ ] Rate Type: ROS
- [ ] Total Value: $100.00
- [ ] Status: APPROVED

### Copy
- [ ] BearUnion-Nov25.mp3 copy exists
- [ ] Copy is linked to BearUnion order
- [ ] Copy is linked to MustachePHX advertiser
- [ ] Audio file is accessible

### Spots
- [ ] 10 spots scheduled
- [ ] Spots distributed across 11/19-11/22
- [ ] Approximately 3 spots per day
- [ ] All spots are 30 seconds
- [ ] All spots have copy assigned (BearUnion-Nov25.mp3)
- [ ] Spot status: SCHEDULED
- [ ] Spot times are reasonable (6 AM - 11 PM)

### Log (if generated)
- [ ] Log generated for 11/19
- [ ] Spots appear in log
- [ ] Spots placed in ADV slots
- [ ] Timing is correct
- [ ] Copy is linked

---

## Expected Results Summary

✅ **8 Standard Dayparts** created (5 weekday, 3 weekend)  
✅ **4 Daypart Categories** created (optional)  
✅ **Donny Demo Sales Rep** created  
✅ **MustachePHX Advertiser** created  
✅ **BearUnion Order** created (10 spots, $100 total)  
✅ **Copy Linked** to order  
✅ **Order Approved** (status: APPROVED)  
✅ **10 Spots Scheduled** across 11/19-11/22  
✅ **Copy Assigned** to all 10 spots  
✅ **Log Generated** (optional) with spots included  

---

## Troubleshooting

### Copy Not Found
- **Problem**: BearUnion-Nov25.mp3 not found in system
- **Solution**: 
  1. Check if file exists in library
  2. Upload copy manually in Copy Library
  3. Link to order after upload

### Order Not Appearing in Spot Scheduler
- **Problem**: Order doesn't show in Spot Scheduler dropdown
- **Solution**:
  1. Verify order status is **APPROVED** (not PENDING or DRAFT)
  2. Check date range - order must have valid start/end dates
  3. Refresh page
  4. Check user has Traffic Manager permissions

### Spots Not Scheduling
- **Problem**: Cannot schedule spots
- **Solution**:
  1. Verify order is **APPROVED**
  2. Check date range is within order dates
  3. Verify copy is uploaded and linked
  4. Check for system errors in console

### Copy Not Assigning to Spots
- **Problem**: Spots created but copy not assigned
- **Solution**:
  1. Manually assign copy in Copy Assignment component
  2. Verify copy expiration date is after spot dates
  3. Check copy is linked to order

---

## Next Steps for Full Testing

After completing this basic test:

1. **Test Makegoods**: Create a makegood for a missed spot
2. **Test Log Publishing**: Publish log to LibreTime
3. **Test Billing**: Generate invoice from completed order
4. **Test Reports**: Generate avails and compliance reports
5. **Test Daypart Restrictions**: Create order with DAYPART rate type
6. **Test Fixed Time**: Create order with FIXED_TIME rate type

---

*Last Updated: November 2024*

