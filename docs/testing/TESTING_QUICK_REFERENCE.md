# LibreLog Testing Quick Reference

## Quick Start Testing

### Run Complete Workflow Test
```bash
export LIBRELOG_API_URL=http://api:8000
export TEST_USERNAME=admin
export TEST_PASSWORD=admin123
python3 test_complete_workflow.py
```

### Run All Endpoints Test
```bash
export LIBRELOG_API_URL=http://api:8000
export TEST_USERNAME=admin
export TEST_PASSWORD=admin123
python3 test_all_endpoints.py
```

### Manual Testing
Follow `MANUAL_TESTING_CHECKLIST.md` or `COMPLETE_TESTING_STEPS.md`

---

## Complete Workflow Steps (8 Phases)

1. **Spot Sold** → Create advertiser, create order (DRAFT)
2. **Produced In House** → Create production order, upload copy
3. **Scheduled** → Approve order, schedule spots (10 spots)
4. **Added to Log** → Generate daily log with spots
5. **Pushed to Automation** → Publish log to LibreTime
6. **Aired** → Spots play on-air (LibreTime automation)
7. **Reconciled Back** → Sync playback history, run reconciliation
8. **Billing** → Generate invoice, send invoice, record payment

---

## Key API Endpoints for Workflow

### Authentication
- `POST /auth/login` - Get JWT token
- `POST /auth/refresh` - Refresh token

### Order Entry
- `POST /advertisers` - Create advertiser
- `POST /orders` - Create order
- `POST /copy` - Upload copy

### Scheduling
- `POST /orders/{id}/approve` - Approve order
- `POST /spots/bulk` - Schedule spots

### Log Generation
- `POST /logs/generate` - Generate log
- `POST /logs/{id}/publish` - Publish to LibreTime

### Reconciliation
- `POST /sync/playback-history` - Sync playback
- `GET /reports/reconciliation` - Reconciliation report

### Billing
- `POST /invoices` - Generate invoice
- `POST /invoices/{id}/send` - Send invoice
- `POST /payments` - Record payment

---

## Token Usage

All API requests require:
```
Authorization: Bearer <jwt_token>
```

Get token:
```bash
curl -X POST http://api:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## Test Files

- `test_complete_workflow.py` - Automated workflow test
- `test_all_endpoints.py` - All endpoints test
- `MANUAL_TESTING_CHECKLIST.md` - Manual testing guide
- `COMPLETE_TESTING_STEPS.md` - Detailed step-by-step
- `API_TESTING_GUIDE.md` - API testing reference
- `TEST_RESULTS_REPORT_TEMPLATE.md` - Results template

---

## Help Center

Access at: `/help` in application

Content verified and synced with markdown documentation.

---

*Quick Reference - Last Updated: 2025-01-15*




