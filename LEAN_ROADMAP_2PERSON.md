# LibreLog Lean Roadmap: 2-Person Team

## Reality Check

**Team**: You (product/business) + Me (development)  
**Resources**: Limited time, limited budget  
**Goal**: Ship fast, learn fast, stay lean

**Principles**:
1. **Ruthless prioritization** - Only build what moves the needle
2. **Automate or eliminate** - No manual processes
3. **Build in public** - Transparency = marketing
4. **Revenue first** - Pay the bills, then expand
5. **Community-powered** - Let users help

---

## Phase 1: MRR $0 → $2,000 (Months 1-3)

### Goal
Get **10 paying customers** at $199/month.

### Strategy
Target **small commercial radio stations** who are:
- Paying WideOrbit too much
- Frustrated with complexity
- Willing to try something new

### What We Build (Minimal Viable Product)

#### Core Loop: Order → Campaign → Spot → Invoice

```
Sales Rep creates Order
        ↓
Traffic creates Campaign  
        ↓
Production uploads Spot audio
        ↓
Traffic schedules Spots into Daily Log
        ↓
System generates Invoice
        ↓
Get paid 💰
```

#### Features (Ruthlessly Prioritized)

| Priority | Feature | Why | Effort |
|----------|---------|-----|--------|
| P0 | **Order → Advertiser dropdown** | You already started this | 1 day |
| P0 | **Order → Campaign workflow** | Critical handoff | 3 days |
| P0 | **Campaign → Spot creation** | Core traffic function | 3 days |
| P0 | **Spot → Daily Log scheduling** | Where the magic happens | 5 days |
| P0 | **Basic Invoice generation** | Get paid | 2 days |
| P1 | **Simple avails view** | See what's available | 3 days |
| P1 | **Email notifications** | Keep users informed | 2 days |
| P2 | **Basic reporting** | Simple revenue report | 2 days |

**Total: ~3 weeks of dev work**

#### What We DON'T Build (Yet)

| Feature | Why Not | When |
|---------|---------|------|
| Commission system | Can track manually | 10+ customers |
| Preemption/makegoods | Edge case | Customer asks |
| A/R aging | Use QuickBooks integration | Later |
| Advanced reporting | Basic is enough | 20+ customers |
| Mobile apps | Web works | 50+ customers |
| Agency portal | Direct sales first | Scale phase |

### Marketing (Zero Budget)

| Tactic | Effort | Expected Result |
|--------|--------|-----------------|
| **Radio forums** | 1hr/week | 5-10 interested stations |
| **Twitter/X build in public** | 30min/day | Developer community |
| **LPFM Facebook groups** | 1hr/week | Community radio interest |
| **Cold email 50 stations** | 1 day | 2-3 conversations |
| **Partner with radio consultants** | Ongoing | Referrals |

### Pricing (Simple)

| Tier | Price | Includes |
|------|-------|----------|
| **Starter** | $99/mo | 1 station, basic traffic |
| **Pro** | $199/mo | 1 station, full features |
| **Group** | $499/mo | Up to 5 stations |

### Success Metrics

| Metric | Target |
|--------|--------|
| Paying customers | 10 |
| MRR | $2,000 |
| Churn | <10% |
| Support tickets/week | <5 |

### Week-by-Week Plan

| Week | Focus | Deliverable |
|------|-------|-------------|
| 1 | Finish Order → Advertiser | Working dropdown, auto-populate |
| 2 | Campaign creation | Create campaign from order |
| 3 | Spot management | Add/edit spots in campaign |
| 4 | Daily Log | Schedule spots into daily log |
| 5 | Invoicing | Generate PDF invoice |
| 6 | Polish & bug fixes | Stable enough for beta |
| 7 | Beta launch | 3 beta stations |
| 8 | Feedback & iterate | Fix issues, improve UX |
| 9 | First paying customer | Close first $199/mo |
| 10 | Marketing push | 10 prospects in pipeline |
| 11 | Close more deals | 5 paying customers |
| 12 | Stabilize | 10 paying customers |

---

## Phase 2: MRR $2,000 → $10,000 (Months 4-6)

### Goal
**Grow to 50 customers** by adding:
1. Word-of-mouth from happy customers
2. Simple podcast module
3. Content marketing

### What We Build

#### Podcast Module (2 weeks)

Why podcasts? 
- Exploding market ($2B ad spend)
- WideOrbit doesn't serve well
- Simpler than radio (no FCC, no dayparts)

**Features**:
- Upload episodes
- Mark ad insertion points
- Basic download analytics
- Simple advertiser management

#### Self-Service Onboarding (1 week)
- Setup wizard
- Sample data
- Help tooltips
- Reduces support burden

### Marketing (Still Zero Budget)

| Tactic | Why |
|--------|-----|
| **Customer case studies** | Social proof |
| **Podcast industry newsletters** | Reach podcasters |
| **Product Hunt launch** | Developer/startup audience |
| **Referral program** | $50 credit for referrals |
| **Webinars** | "How to traffic radio ads" |

### Success Metrics

| Metric | Target |
|--------|--------|
| Radio customers | 30 |
| Podcast customers | 20 |
| MRR | $10,000 |
| NPS | 50+ |

---

## Phase 3: MRR $10,000 → $30,000 (Months 7-12)

### Goal
**Sustainable business** - you can pay yourself.

### Strategy
- Hire part-time support (customer success)
- Add features customers actually ask for
- Consider small funding or stay bootstrapped

### What We Build (Customer-Driven)

**Don't guess - ask customers:**
- Survey: "What feature would make you pay 2x?"
- Top requests get built
- Everything else: "Not on roadmap"

#### Likely Features

| Feature | Why | Effort |
|---------|-----|--------|
| **QuickBooks integration** | #1 request | 1 week |
| **Basic commission tracking** | Sales teams need it | 1 week |
| **Simple avails calendar** | Visual inventory | 1 week |
| **API access** | Power users | 1 week |
| **White-label** | Resellers want it | 1 week |

### Team Growth

| Role | When | Cost |
|------|------|------|
| **Customer support (part-time)** | $15K MRR | $2K/mo |
| **Junior developer (part-time)** | $25K MRR | $3K/mo |

---

## The "Don't Do" List

### Things That Waste Time for 2-Person Team

| Don't Do | Why Not | Instead |
|----------|---------|---------|
| **Mobile apps** | Web works fine | Responsive web |
| **AI/ML features** | Overkill | Simple rules |
| **Agency integrations** | Too complex | CSV export |
| **Advanced reporting** | Basic charts work | Export to Excel |
| **Multi-currency** | USD first | Add later if needed |
| **On-premise deployment** | Cloud only | Managed hosting |
| **24/7 support** | Business hours | Async support |
| **Custom dev for 1 customer** | Productize | Say no |

---

## Automation Strategy

### Automate Everything

| Task | Tool | Cost |
|------|------|------|
| **Email sequences** | Mailgun/Mailchimp | $50/mo |
| **Error monitoring** | Sentry | Free tier |
| **Analytics** | Plausible | $9/mo |
| **CI/CD** | GitHub Actions | Free |
| **Backups** | AWS RDS automated | Included |
| **Status page** | Instatus | $20/mo |
| **Support chat** | Crisp | Free tier |
| **Documentation** | GitBook/Notion | Free |

### Reduce Support Burden

| Strategy | How |
|----------|-----|
| **Great documentation** | Video tutorials, FAQs |
| **In-app help** | Tooltips, context help |
| **Self-service** | Password reset, data export |
| **Prevent errors** | Validation, warnings |
| **Status page** | Proactive communication |

---

## Tech Stack (Stay Lean)

### Current (Keep It)
- Java 21 + Spring Boot ✅
- PostgreSQL ✅
- Docker + Docker Compose ✅
- Single server (DigitalOcean/AWS Lightsail) ✅

### Add Only If Needed
- Redis (caching) - only if slow
- Elasticsearch (search) - only if needed
- Message queue - only for async jobs
- CDN - only if global

### Development Efficiency
- **Single codebase** - no microservices
- **Monorepo** - everything in one place
- **Feature flags** - deploy anytime
- **Automated testing** - prevent regressions
- **One-click deploy** - reduce ops burden

---

## Financial Model (Conservative)

### Costs (Monthly)

| Expense | Phase 1 | Phase 2 | Phase 3 |
|---------|---------|---------|---------|
| Hosting | $100 | $200 | $500 |
| Tools/SaaS | $100 | $200 | $300 |
| Your salary | $0 | $0 | $3,000 |
| Support (PT) | $0 | $0 | $2,000 |
| **Total** | **$200** | **$400** | **$5,800** |

### Revenue

| Phase | Customers | Avg $/mo | MRR | Profit |
|-------|-----------|----------|-----|--------|
| 1 | 10 | $150 | $1,500 | $1,300 |
| 2 | 50 | $150 | $7,500 | $7,100 |
| 3 | 150 | $150 | $22,500 | $16,700 |

### Break-Even
- **You**: Month 6 (part-time salary)
- **Full-time**: Month 9 ($5K/mo salary)
- **Hire #1**: Month 12

---

## Decision Framework

### "Should We Build This?" Checklist

| Question | If Yes | If No |
|----------|--------|-------|
| Does a customer need it to pay? | Build it | Don't build |
| Will 3+ customers use it? | Build it | Don't build |
| Can we build it in <1 week? | Build it | Defer |
| Does it reduce support burden? | Build it | Don't build |
| Is it required for legal/compliance? | Build it | Don't build |
| Can we charge more for it? | Build it | Don't build |

### Default Answer: NO

Unless there's a clear "yes" to multiple questions, the answer is **NO**.

---

## Success Factors for 2-Person Team

### 1. Move Fast
- Ship weekly
- Perfect is enemy of good
- Iterate based on feedback

### 2. Talk to Customers
- 1 customer call per week minimum
- Build what they ask for
- Ignore everything else

### 3. Stay Healthy
- Don't burn out
- Sustainable pace
- Celebrate small wins

### 4. Build in Public
- Share progress
- Transparent about challenges
- Community helps

### 5. Say No
- Feature requests: 90% no
- Custom work: 99% no
- Partnerships: 95% no

---

## Immediate Next Steps (This Week)

### Day 1-2: Finish Order → Advertiser
- [ ] Auto-populate agency/sales rep from advertiser
- [ ] Test end-to-end
- [ ] Deploy

### Day 3-5: Campaign from Order
- [ ] "Create Campaign" button on Order
- [ ] Pre-populate campaign data
- [ ] Link Order → Campaign

### Week 2: Spot Management
- [ ] Create spots within campaign
- [ ] Upload spot audio
- [ ] Spot status tracking

### Week 3: Daily Log
- [ ] Simple daily log view
- [ ] Drag spots into log
- [ ] Export to LibreTime

### Week 4: Invoicing
- [ ] Generate invoice from aired spots
- [ ] PDF export
- [ ] Email to advertiser

---

## The Lean Promise

**We will**:
- ✅ Ship fast
- ✅ Stay focused
- ✅ Talk to customers
- ✅ Automate everything
- ✅ Stay lean until $10K MRR

**We will NOT**:
- ❌ Build features no one asked for
- ❌ Chase enterprise deals too early
- ❌ Hire before we can afford it
- ❌ Burn out
- ❌ Build a WideOrbit clone (yet)

---

## The Dream

**Month 12**: 
- 150 customers
- $22,500 MRR
- $16,700 profit
- You working full-time
- Part-time support help
- Growing organically

**Month 24**:
- 500 customers
- $75,000 MRR
- Small team (5 people)
- Considering WideOrbit competition

**Month 36**:
- Market leader in small/mid-market
- WideOrbit acquisition offer? 😎

---

*Lean Roadmap Version: 1.0*  
*Team: 2 people*  
*Philosophy: Ship fast, stay lean, grow sustainably*  
*Created: 2026-03-13*
