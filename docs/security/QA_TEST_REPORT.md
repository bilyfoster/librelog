# QA Test Report: Sales Workflow - Founding 50 Package

**Test Date**: 2025-11-23  
**Tester**: QA Agent (Automated)  
**Test Scenario**: Sales person workflow - Create client with agency, create order, schedule spots

---

## Executive Summary

The QA test executed the complete sales workflow as specified in the test plan. **7 critical issues** were identified that prevent the workflow from completing successfully. Note: Testing was performed via direct API calls due to a critical frontend URL construction bug that blocks all browser-based testing.

**Update**: Issue #0 (URL construction) and Issue #0.5 (DELETE endpoint) have been fixed.

### Test Results
- ⚠️ **Phase 0**: Frontend API Access - BLOCKED (URL construction bug prevents all UI testing)
- ✅ **Phase 1**: Setup and Prerequisites - PASSED (via direct API)
- ✅ **Phase 2**: Create Agency - PASSED (via direct API)
- ⚠️ **Phase 3**: Create Client with Agency - PARTIAL (agency relationship not saved)
- ✅ **Phase 4**: Create Order - PASSED (minor formatting issue)
- ❌ **Phase 6**: Approve Order - FAILED (500 error)
- ❌ **Phase 7**: Schedule Spots - FAILED (database schema mismatch)
- ⚠️ **Phase 8**: Verify Workflow - PARTIAL (multiple issues)

**Note**: Testing was performed via direct API calls due to frontend URL construction bug blocking browser-based testing.

---

## Issues Found

### Issue #0: CRITICAL - Frontend API URL Construction Bug (BLOCKING) [NEW]
**Phase**: All Phases  
**Severity**: CRITICAL  
**Status**: BLOCKING

**Description**:  
The frontend API client is incorrectly constructing URLs, causing all API requests to fail or hit wrong endpoints. URLs are being concatenated without proper separators, resulting in malformed paths like `/apiauth/me` instead of `/api/auth/me`.

**Steps to Reproduce**:
1. Open browser console
2. Navigate to any page in the application
3. Observe console logs showing URL construction
4. See errors like: `GET /apiauth/me`, `GET /apiproxy/agencies`, `GET /apiorders/3`

**Expected Behavior**:  
URLs should be constructed as `/api/auth/me`, `/api/proxy/agencies`, `/api/orders/3` with proper `/` separators.

**Actual Behavior**:  
URLs are constructed as `/apiauth/me`, `/apiproxy/agencies`, `/apiorders/3` - missing the `/` separator between `/api` and the path.

**Error Details**:
```
[API] Adapter: Final URL construction {baseURL: '/api', url: 'auth/me', combined: '/apiauth/me'}
GET /apiauth/me 404 (Not Found)
```

**Root Cause**:  
In `/frontend/src/utils/api.ts`, the interceptor removes leading `/` from URLs (line 718), making `auth/me` from `/auth/me`. Then when axios combines `baseURL` (`/api`) + `url` (`auth/me`), it creates `/apiauth/me` instead of `/api/auth/me` because axios's URL combination logic isn't working as expected, or the adapter is interfering.

Additionally, there's a "FINAL FIX" code block (line 738-740) that also removes leading `/` from URLs, compounding the issue.

**Impact**:  
- **ALL API requests fail or hit wrong endpoints**
- Frontend cannot communicate with backend
- Complete system failure in browser
- Workflow testing impossible through UI

**Recommended Fix**:
1. Remove or fix the code that strips leading `/` from URLs in the interceptor
2. Ensure URLs keep their leading `/` when baseURL is `/api`
3. Axios should handle `/api` + `/auth/me` → `/api/auth/me` correctly
4. Update adapter to ensure proper URL combination
5. Test all API endpoints after fix

**Files to Fix**:
- `/frontend/src/utils/api.ts` - Lines 708-741 (interceptor logic)
- `/frontend/src/utils/api.ts` - Lines 253-257 (adapter URL combination)

**Status**: ✅ **FIXED** - URL construction now properly combines `/api` + `/path` → `/api/path`

---

### Issue #0.5: Missing DELETE Endpoint for Orders
**Phase**: Order Management  
**Severity**: MEDIUM  
**Status**: ✅ **FIXED**

**Description**:  
The frontend attempts to delete orders via `DELETE /orders/{id}`, but the backend does not have a DELETE endpoint, resulting in a 405 Method Not Allowed error.

**Steps to Reproduce**:
1. Navigate to Traffic > Orders
2. Click delete button on any order
3. See 405 Method Not Allowed error

**Expected Behavior**:  
Order should be deleted (or prevented if it has scheduled spots).

**Actual Behavior**:  
405 Method Not Allowed error returned from backend.

**Error Details**:
```
DELETE https://log.gayphx.com/api/orders/3 405 (Method Not Allowed)
{detail: 'Method Not Allowed'}
```

**Root Cause**:  
The `/backend/routers/orders.py` router does not have a `@router.delete("/{order_id}")` endpoint defined.

**Impact**:  
- Users cannot delete orders through the UI
- Delete functionality is broken

**Recommended Fix**:
1. Add DELETE endpoint to orders router
2. Check if order has scheduled spots before deletion
3. Prevent deletion if spots exist (return 400 error)
4. Log audit action for deletion
5. Return 204 No Content on successful deletion

**Files Fixed**:
- `/backend/routers/orders.py` - Added `@router.delete("/{order_id}")` endpoint (lines 382-435)

**Status**: ✅ **FIXED** - DELETE endpoint added with proper validation

---

### Issue #1: Agency Relationship Not Saved When Creating Client
**Phase**: Phase 3 - Create Client  
**Severity**: HIGH  
**Status**: BLOCKING

**Description**:  
When creating an advertiser (client) with an `agency_id`, the relationship is not being saved to the database. The API accepts the `agency_id` in the request, but the response shows `agency_id` as `None`.

**Steps to Reproduce**:
1. Create an agency via `POST /advertisers` with `agency_id: 1`
2. Create an advertiser with `agency_id: 1` in the request body
3. Check the response - `agency_id` is `None`

**Expected Behavior**:  
The advertiser should be linked to the agency, and `agency_id` should be saved and returned in the response.

**Actual Behavior**:  
The `agency_id` field is not saved. Response shows `agency_id: None`.

**Root Cause**:  
The `AdvertiserCreate` model in `/backend/routers/advertisers.py` does not include `agency_id` as a field. The model only includes:
- name, contact_first_name, contact_last_name, email, phone, address, tax_id, payment_terms, credit_limit

The `agency_id` field is not being accepted or processed.

**Impact**:  
- Cannot link clients to agencies
- Order creation cannot inherit agency from client
- Agency commission calculations will fail
- Data integrity issues

**Recommended Fix**:
1. Add `agency_id: Optional[int] = None` to `AdvertiserCreate` model
2. Ensure `Advertiser` model has `agency_id` column (verify database schema)
3. Update `advertiser_to_response` to include `agency_id` in response

---

### Issue #2: Total Value Format Mismatch
**Phase**: Phase 4 - Create Order  
**Severity**: LOW  
**Status**: COSMETIC

**Description**:  
The total value is returned as `100.00` (Decimal/string) instead of `100.0` (float). This is likely a type conversion issue.

**Expected Behavior**:  
Total value should be `100.0` (float) or consistently formatted.

**Actual Behavior**:  
Total value is returned as `100.00` (Decimal type).

**Impact**:  
Minor - this is a type/formatting issue that doesn't break functionality, but could cause issues in frontend comparisons.

**Recommended Fix**:  
Ensure consistent type handling (either always Decimal or always float) or update test to handle Decimal type.

---

### Issue #3: Order Approval Fails with 500 Error
**Phase**: Phase 6 - Approve Order  
**Severity**: CRITICAL  
**Status**: BLOCKING

**Description**:  
Attempting to update an order status to `APPROVED` results in a 500 Internal Server Error.

**Steps to Reproduce**:
1. Create an order with status `DRAFT`
2. Attempt to update order via `PUT /orders/{order_id}` with `{"status": "APPROVED", "approval_status": "APPROVED"}`
3. Server returns 500 error

**Expected Behavior**:  
Order status should update to `APPROVED` and return 200 OK.

**Actual Behavior**:  
Server returns 500 Internal Server Error.

**Error Details**:  
```
Status 500: {"detail":"Internal server error"}
```

**Root Cause**:  
Need to check server logs for specific error. Likely issues:
- Database constraint violation
- Missing required fields
- Validation error in order update logic

**Impact**:  
- Cannot approve orders
- Workflow cannot proceed past order creation
- Spots cannot be scheduled (require APPROVED orders)

**Recommended Fix**:
1. Check server logs for specific error
2. Review order update endpoint in `/backend/routers/orders.py`
3. Verify database constraints and required fields
4. Add proper error handling and validation

---

### Issue #4: Spot Scheduling Fails - Database Schema Mismatch
**Phase**: Phase 7 - Schedule Spots  
**Severity**: CRITICAL  
**Status**: BLOCKING

**Description**:  
Cannot schedule spots due to database schema mismatch. The `Spot` model expects a `station_id` column, but the database table does not have this column.

**Steps to Reproduce**:
1. Create an approved order
2. Attempt to create spots via `POST /spots/bulk`
3. Database error occurs

**Expected Behavior**:  
Spots should be created successfully with `station_id` populated.

**Actual Behavior**:  
Database error: `column spots.station_id does not exist`

**Error Details**:
```
sqlalchemy.exc.ProgrammingError: column spots.station_id does not exist
```

**Root Cause**:  
The `Spot` model in `/backend/models/spot.py` defines:
```python
station_id = Column(Integer, ForeignKey("stations.id"), nullable=False, index=True)
```

However, the database table `spots` does not have a `station_id` column. The migration `003_add_spot_scheduling_tables.py` may not have included this column, or a migration to add it was never created/run.

**Database Schema** (actual):
- `id`, `order_id`, `campaign_id`, `scheduled_date`, `scheduled_time`, `spot_length`, `break_position`, `daypart`, `status`, `actual_air_time`, `makegood_of_id`, `conflict_resolved`, `created_at`, `updated_at`

**Missing**: `station_id` column

**Impact**:  
- Cannot schedule spots
- Complete workflow failure
- System cannot fulfill core functionality

**Recommended Fix**:
1. Create Alembic migration to add `station_id` column to `spots` table
2. Migration should:
   - Add `station_id INTEGER NOT NULL` column
   - Add foreign key constraint to `stations.id`
   - Add index on `station_id`
   - Handle existing data (may need to set default station or make nullable temporarily)
3. Run migration
4. Verify model matches database schema

---

### Issue #5: Client Not Linked to Agency (Verification)
**Phase**: Phase 8 - Verify Workflow  
**Severity**: HIGH  
**Status**: RELATED TO ISSUE #1

**Description**:  
When verifying the workflow, the client's `agency_id` is `None`, confirming that the agency relationship was not saved (same as Issue #1).

**Impact**:  
Same as Issue #1 - data integrity problem.

---

### Issue #6: Failed to Get Spots (500 Error)
**Phase**: Phase 8 - Verify Workflow  
**Severity**: CRITICAL  
**Status**: RELATED TO ISSUE #4

**Description**:  
Attempting to retrieve spots via `GET /spots?order_id={order_id}` returns a 500 error.

**Expected Behavior**:  
Should return list of spots for the order.

**Actual Behavior**:  
Returns 500 Internal Server Error.

**Root Cause**:  
Likely the same database schema issue as Issue #4 - the spots query is trying to access `station_id` column that doesn't exist.

**Impact**:  
- Cannot retrieve spots
- Cannot verify spot scheduling
- Frontend cannot display spots

---

## Test Data Created

During testing, the following data was created:
- **Agency ID**: 1 (QA Test Agency)
- **Client ID**: 2 (QA Test Client - Founding 50)
- **Order ID**: 2 (Founding 50 Sales Package)

**Note**: Due to the blocking issues, spots were not successfully created.

---

## Recommendations

### Immediate Actions Required:
1. **Fix Issue #0 (Frontend URL Construction)**: This is the MOST CRITICAL blocker - prevents all UI testing
   - Fix URL combination logic in `api.ts`
   - Remove code that strips leading `/` from URLs
   - Test all API endpoints work correctly
   - Verify browser-based testing can proceed

2. **Fix Issue #4 (Database Schema)**: This is a critical blocker
   - Create and run migration to add `station_id` to `spots` table
   - Verify all spot-related queries work

2. **Fix Issue #1 (Agency Relationship)**: Required for workflow
   - Add `agency_id` to `AdvertiserCreate` model
   - Verify `Advertiser` model has `agency_id` column
   - Update response model to include `agency_id`

3. **Fix Issue #3 (Order Approval)**: Required for workflow
   - Investigate 500 error in order update endpoint
   - Fix validation/constraint issues
   - Add proper error handling

### Follow-up Actions:
4. Fix Issue #2 (Total Value Format) - Low priority
5. Re-run full test after fixes
6. Test production manager workflow

---

## Test Environment

- **API URL**: http://localhost:8000
- **Database**: PostgreSQL (librelog)
- **Test User**: admin
- **Test Method**: Automated Python script via API

---

## Files Generated

- `qa_test_sales_workflow.py` - Test script
- `qa_test_issues.json` - Detailed issues in JSON format
- `QA_TEST_REPORT.md` - This report

---

## Next Steps

1. Fix critical issues (Issues #1, #3, #4)
2. Re-run QA test
3. Proceed with production manager workflow testing
4. Document fixes in changelog

---

*Report generated by automated QA test script*
