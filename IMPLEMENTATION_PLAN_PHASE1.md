# LibreLog Phase 1 Implementation Plan
## Goal: Achieve WideOrbit Core Competitiveness

**Duration**: 3 Months  
**Target**: Small commercial radio stations  
**Success Criteria**: 10 beta stations actively using traffic + billing

---

## P0 Feature: Commission System

### Why First?
- Critical for sales team motivation
- Relatively simple to implement
- High value, medium complexity
- Already have `commissionRate` field

### Implementation Tasks

#### Backend (Week 1)
1. **Database Schema**
   ```sql
   -- Commission transactions table
   CREATE TABLE commission_transactions (
       id UUID PRIMARY KEY,
       sales_rep_id UUID NOT NULL,
       order_id UUID,
       campaign_id UUID,
       invoice_id UUID,
       amount DECIMAL(19,2) NOT NULL,
       rate DECIMAL(5,2) NOT NULL,
       basis_amount DECIMAL(19,2) NOT NULL,
       transaction_type VARCHAR(20), -- EARNED, PAID, ADJUSTED
       status VARCHAR(20), -- PENDING, APPROVED, PAID
       period_month INT,
       period_year INT,
       created_at TIMESTAMP,
       FOREIGN KEY (sales_rep_id) REFERENCES sales_reps(id)
   );
   ```

2. **Commission Calculation Service**
   - Calculate on invoice payment
   - Support gross vs net commission
   - Handle commission splits
   - Handle draw/recoverable draws

3. **API Endpoints**
   ```
   GET /api/commissions/calculate/{period}  // Calculate for period
   GET /api/commissions/rep/{repId}         // Get rep's commissions
   POST /api/commissions/approve            // Approve commission batch
   POST /api/commissions/pay                // Mark as paid
   GET /api/commissions/reports/summary     // Summary report
   GET /api/commissions/reports/detailed    // Detailed report
   ```

#### Frontend (Week 2)
1. **Commission Dashboard**
   - Rep view: My commissions
   - Manager view: All reps
   - Payable amount this period
   - YTD earnings

2. **Commission Reports**
   - By rep, by period
   - By campaign/order
   - Export to Excel

---

## P0 Feature: Avails Management

### Why Critical?
- Core traffic function
- Prevents overselling
- Enables proposal generation

### Implementation Tasks

#### Backend (Week 2-3)
1. **Inventory Model**
   ```java
   @Entity
   public class InventoryAvail {
       private UUID id;
       private UUID stationId;
       private LocalDate date;
       private String daypart;
       private Integer totalSpots;
       private Integer bookedSpots;
       private Integer remainingSpots;
       private BigDecimal rate;
       
       // Calculated
       public Integer getRemainingSpots() {
           return totalSpots - bookedSpots;
       }
       
       public BigDecimal getSelloutPercent() {
           return bookedSpots / totalSpots * 100;
       }
   }
   ```

2. **Avails Calculation Engine**
   - Calculate based on clock templates
   - Subtract booked spots
   - Account for reserved inventory
   - Real-time updates on order changes

3. **API Endpoints**
   ```
   GET /api/avails?stationId={id}&startDate={}&endDate={}
   GET /api/avails/dayparts?stationId={id}&date={}
   GET /api/avails/summary?stationId={id}&month={}
   POST /api/avails/reserve  // Hold inventory for proposal
   DELETE /api/avails/reserve/{id} // Release hold
   ```

#### Frontend (Week 3-4)
1. **Avails Calendar View**
   - Grid view: dates × dayparts
   - Color-coded (green=available, yellow=limited, red=sold out)
   - Click to see detail

2. **Avails Search**
   - Find available slots
   - Filter by daypart, date range
   - Show pricing

---

## P0 Feature: Preemption & Makegoods

### Why Critical?
- Revenue protection
- Industry standard expectation
- Complex business logic

### Implementation Tasks

#### Backend (Week 4-5)
1. **Priority System**
   ```java
   public enum SpotPriority {
       CONTRACT(1),      // Annual contracts
       PREEMPTIBLE_1(2), // Lowest preemption
       PREEMPTIBLE_2(3),
       PREEMPTIBLE_3(4),
       PREEMPTIBLE_4(5), // Highest preemption
       MAKEGOOD(6)       // Makegoods are highest priority
   }
   ```

2. **Preemption Engine**
   - When higher priority spot booked
   - Identify lower priority spots to preempt
   - Create makegood records
   - Notify traffic manager

3. **Makegood Management**
   ```java
   @Entity
   public class Makegood {
       private UUID id;
       private UUID preemptedSpotId;
       private UUID makegoodSpotId; // The replacement
       private MakegoodStatus status; // PENDING, APPROVED, SCHEDULED
       private String reason;
       private LocalDate deadline; // Must air by
   }
   ```

4. **API Endpoints**
   ```
   GET /api/preemptions?stationId={id}
   POST /api/preemptions/{id}/resolve
   GET /api/makegoods?stationId={id}
   POST /api/makegoods/{id}/approve
   POST /api/makegoods/{id}/schedule
   ```

#### Frontend (Week 5-6)
1. **Preemption Dashboard**
   - List of preempted spots
   - Priority comparison
   - One-click makegood creation

2. **Makegood Workflow**
   - Pending makegoods queue
   - Drag to schedule
   - Approval workflow

---

## P0 Feature: A/R Management

### Why Critical?
- Cash flow management
- Industry requirement
- WideOrbit strength

### Implementation Tasks

#### Backend (Week 6-7)
1. **Enhanced Invoice Model**
   ```java
   @Entity
   public class Invoice {
       // Existing fields...
       private InvoiceStatus status; // DRAFT, SENT, PARTIAL_PAID, PAID, OVERDUE
       private LocalDate dueDate;
       private BigDecimal amountPaid;
       private BigDecimal balanceDue;
       private Integer daysOverdue;
       
       @OneToMany
       private List<Payment> payments;
       
       @OneToMany
       private List<InvoiceAging> agingHistory;
   }
   ```

2. **Aging Calculation**
   - Daily batch job
   - Calculate 30/60/90/120+ buckets
   - Update invoice status

3. **Payment Processing**
   ```java
   @Entity
   public class Payment {
       private UUID id;
       private UUID advertiserId;
       private BigDecimal amount;
       private PaymentMethod method; // CHECK, ACH, CC
       private LocalDate paymentDate;
       
       @ManyToMany
       private List<Invoice> appliedTo;
   }
   ```

4. **Collections Workflow**
   ```java
   @Entity
   public class CollectionsActivity {
       private UUID id;
       private UUID advertiserId;
       private ActivityType type; // CALL, EMAIL, LETTER
       private LocalDate activityDate;
       private String notes;
       private UUID performedBy;
       private CollectionsResult result;
   }
   ```

5. **API Endpoints**
   ```
   GET /api/invoices/aging?asOfDate={}
   GET /api/invoices/overdue
   POST /api/payments              // Record payment
   POST /api/payments/{id}/apply   // Apply to invoices
   GET /api/collections/queue      // Collections work queue
   POST /api/collections/activity  // Log activity
   GET /api/reports/ar-aging       // A/R aging report
   GET /api/reports/dso            // Days sales outstanding
   ```

#### Frontend (Week 7-8)
1. **A/R Dashboard**
   - Total A/R balance
   - Aging buckets (30/60/90/120+)
   - DSO trend
   - Top overdue accounts

2. **Collections Queue**
   - Prioritized list
   - Last activity date
   - Quick actions (call, email, letter)

3. **Payment Entry**
   - Apply payments to invoices
   - Partial payment support
   - Overpayment handling

---

## P0 Feature: Advanced Reporting

### Why Critical?
- Management visibility
- WideOrbit differentiator
- Data-driven decisions

### Implementation Tasks

#### Backend (Week 8-9)
1. **Reporting Framework**
   - JasperReports or similar
   - Report templates
   - Scheduled report generation
   - Export (PDF, Excel, CSV)

2. **Core Reports**
   | Report | Description |
   |--------|-------------|
   | Pacing | Revenue vs budget by month |
   | Inventory | Sellout % by daypart |
   | Sales Rep | Performance ranking |
   | Advertiser | Lifetime value |
   | Aging | A/R aging buckets |
   | Cash Flow | Projected collections |

3. **API Endpoints**
   ```
   GET /api/reports/pacing?month={}&stationId={}
   GET /api/reports/inventory?startDate={}&endDate={}
   GET /api/reports/sales-rep?repId={}&period={}
   GET /api/reports/export/{reportId}?format={pdf|excel}
   ```

#### Frontend (Week 9-10)
1. **Report Library**
   - List of available reports
   - Filter by category
   - Favorite reports

2. **Report Viewer**
   - In-browser viewing
   - Date range selector
   - Station selector
   - Export buttons

3. **Dashboard Widgets**
   - Mini charts on main dashboard
   - Key metrics at a glance
   - Trend indicators

---

## Technical Architecture Requirements

### New Infrastructure Components

```
┌─────────────────────────────────────────────────────────────┐
│                    LibreLog Architecture                    │
├─────────────────────────────────────────────────────────────┤
│  Frontend (React/Vue)                                       │
│  ├── Dashboard                                              │
│  ├── Traffic UI                                             │
│  ├── Sales UI                                               │
│  └── Reporting UI                                           │
├─────────────────────────────────────────────────────────────┤
│  API Gateway (Spring Cloud Gateway)                         │
├─────────────────────────────────────────────────────────────┤
│  Services                                                   │
│  ├── Traffic Service (avails, preemptions, makegoods)      │
│  ├── Billing Service (invoicing, payments, A/R)            │
│  ├── Sales Service (orders, commissions, proposals)        │
│  └── Reporting Service (reports, analytics)                │
├─────────────────────────────────────────────────────────────┤
│  Data Layer                                                 │
│  ├── PostgreSQL (primary)                                   │
│  ├── Redis (cache, sessions)                                │
│  ├── Elasticsearch (search, reporting)                      │
│  └── RabbitMQ (async jobs)                                  │
└─────────────────────────────────────────────────────────────┘
```

### Database Migrations Needed

1. **Commission tables**
2. **Inventory/avails tables**
3. **Makegood tables**
4. **Payment tables**
5. **Collections tables**
6. **Report configuration tables**

### Async Jobs (RabbitMQ)

1. **Daily**: Calculate aging, generate reports
2. **Hourly**: Update avails, commission calculations
3. **Real-time**: Preemption processing

---

## Testing Strategy

### Unit Tests
- Commission calculation engine
- Avails calculation
- Preemption logic
- Payment application

### Integration Tests
- End-to-end order to invoice flow
- Makegood workflow
- Payment reconciliation

### Beta Testing
- 3-5 real radio stations
- 30-day beta period
- Daily feedback calls
- Issue tracking

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Feature Completeness | 100% P0 | Checklist |
| Test Coverage | 80%+ | JaCoCo |
| Beta Sign-ups | 10 stations | Count |
| Beta Active Usage | 70% daily | Analytics |
| Beta Satisfaction | 4+ / 5 | Survey |
| Performance | <2s load | APM |
| Bugs | <10 critical | Jira |

---

## Risk Mitigation

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Complex preemption logic | High | Start simple, iterate |
| Performance at scale | Medium | Load testing early |
| User adoption | Medium | UX focus, training |
| Data migration | Medium | Professional services |

---

## Week-by-Week Schedule

| Week | Focus | Deliverable |
|------|-------|-------------|
| 1 | Commission backend | Commission calculation API |
| 2 | Commission frontend | Commission dashboard |
| 3 | Avails backend | Avails calculation engine |
| 4 | Avails frontend | Avails calendar view |
| 5 | Preemption backend | Preemption engine |
| 6 | Preemption/Makegoods UI | Makegood workflow |
| 7 | A/R backend | Payment processing |
| 8 | A/R frontend | Collections queue |
| 9 | Reporting backend | Report engine |
| 10 | Reporting frontend | Report library |
| 11 | Integration testing | All P0 features working |
| 12 | Beta launch | 3 stations onboarded |

---

## Resources Needed

### Development
- 2 Backend developers (Java/Spring)
- 1 Frontend developer (React/Vue)
- 1 DevOps engineer (AWS/K8s)
- 1 QA engineer

### Infrastructure
- AWS/GCP environment
- PostgreSQL (RDS/Cloud SQL)
- Redis (ElastiCache/Memorystore)
- RabbitMQ (Amazon MQ/CloudAMQP)
- S3/Cloud Storage for reports

### External Services
- SendGrid/Mailgun (email)
- Stripe (payment processing)
- Twilio (SMS for collections)

---

## Conclusion

Phase 1 delivers the **core competencies** needed to compete with WideOrbit:
- Commission tracking (sales motivation)
- Avails management (inventory control)
- Preemption/makegoods (revenue protection)
- A/R management (cash flow)
- Advanced reporting (management visibility)

With these features, LibreLog becomes a **viable alternative** for small-to-mid market stations.

---

*Plan Version: 1.0*  
*Created: 2026-03-13*  
*Review Cycle: Weekly*  
