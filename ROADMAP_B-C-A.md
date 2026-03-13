# LibreLog Strategic Roadmap: B → C → A

## Executive Summary

**Strategy**: Stabilize → Expand → Dominate

1. **Phase B** (Now): Stabilize MVP, target community stations
2. **Phase C** (Q2-Q3): Expand to podcasts, digital, international
3. **Phase A** (Q4+): Build WideOrbit-competitive enterprise features

This sequence minimizes risk, builds user base, and creates revenue before tackling the incumbent.

---

## Phase B: Stabilize (Months 1-2)

### Goal
Make the current MVP rock-solid for community/LPFM stations.

### Target Market
- **LPFM** (Low Power FM) stations
- **Community radio** (college, nonprofit)
- **Small religious broadcasters**
- **Internet radio stations**

### Why This Market First?
1. **Underserved**: WideOrbit too expensive ($1,500+/mo)
2. **Forgiving**: Willing to try new software
3. **Viral**: Tight-knit community, word spreads
4. **Mission-aligned**: Open-source ethos
5. **Low risk**: Free tier means no revenue pressure

### Phase B Feature Set

#### Core Features (Already Built)
| Feature | Status | Notes |
|---------|--------|-------|
| Station Management | ✅ | Working |
| Channel/Clock Management | ✅ | Working |
| Dayparts | ✅ | Working |
| Order Management | ✅ | Working with advertiser dropdown |
| Advertiser/Agency/Sales Rep | ✅ | Working |
| Campaigns | ✅ | Basic UI |
| Spots | ✅ | Basic UI |
| Voice Tracks | ✅ | Basic UI |
| User Management | ✅ | Working |
| Dashboard | ✅ | Working |

#### Critical Bug Fixes (Week 1-2)
- [ ] Fix Order data model (add advertiserId relationship)
- [ ] Fix Voice Track model (add song before/after)
- [ ] Test all CRUD operations end-to-end
- [ ] Fix any JavaScript errors in dashboard
- [ ] Ensure all modals work correctly
- [ ] Test mobile responsiveness

#### UX Polish (Week 3-4)
- [ ] Loading states on all async operations
- [ ] Better error messages
- [ ] Success confirmations
- [ ] Form validation feedback
- [ ] Keyboard shortcuts
- [ ] Dark mode refinement
- [ ] Help tooltips throughout

#### Documentation (Week 5-6)
- [ ] User manual (video + text)
- [ ] Quick start guide
- [ ] FAQ
- [ ] Troubleshooting guide
- [ ] API documentation

#### Onboarding (Week 7-8)
- [ ] Setup wizard for new stations
- [ ] Sample data seeding
- [ ] Interactive tutorials
- [ ] Email sequence for new users

### Success Metrics
| Metric | Target |
|--------|--------|
| Active stations (free tier) | 50 |
| Bug reports/month | <5 |
| User satisfaction | 4.5/5 |
| Uptime | 99.9% |
| Support tickets | <10/week |

### Pricing
**Free Tier** (Community/LPFM)
- Unlimited users
- 1 station
- Basic traffic
- Basic billing
- Email support

---

## Phase C: Expand (Months 3-6)

### Goal
Expand into adjacent markets where WideOrbit is weak.

### Target Market 1: Podcast Networks

#### Why Podcasts?
1. **Growth**: Podcast ad revenue growing 30%+ YoY
2. **WideOrbit weakness**: Their "WO Digital Hub" is clunky
3. **Different workflow**: Dynamic ad insertion, not scheduled
4. **Tech-forward**: Early adopters
5. **Revenue potential**: $49-199/month per show

#### Podcast-Specific Features
```
Podcast Traffic Management
├── Dynamic Ad Insertion (DAI)
│   ├── Pre-roll slots
│   ├── Mid-roll markers
│   ├── Post-roll slots
│   └── Campaign-based insertion
├── Campaign Management
│   ├── Episode-level targeting
│   ├── Geographic targeting
│   ├── Device targeting
│   └── Download-based billing
├── Ad Serving
│   ├── VAST tag support
│   ├── Ad waterfall
│   ├── Frequency capping
│   └── Competitive separation
├── Analytics
│   ├── Download stats
│   ├── Listener demographics
│   ├── Completion rates
│   └── Attribution
└── Marketplace
    ├── Self-serve advertiser portal
    ├── Programmatic integration
    └── Direct sales tools
```

#### Implementation (Month 3-4)
- [ ] Podcast RSS feed integration
- [ ] Dynamic ad insertion engine
- [ ] VAST ad serving
- [ ] Download analytics
- [ ] Advertiser self-serve portal

#### Pricing
| Tier | Price | Features |
|------|-------|----------|
| Indie | $49/mo | 1 show, 10K downloads |
| Pro | $99/mo | 5 shows, 100K downloads |
| Network | $199/mo | Unlimited, 1M downloads |

---

### Target Market 2: Digital-Only Broadcasters

#### Why Digital?
1. **Cord-cutting**: Traditional radio declining
2. **Streaming growth**: Internet radio booming
3. **WideOrbit gap**: Their digital product is bolt-on
4. **Global reach**: Not limited by geography

#### Digital-Specific Features
```
Digital Broadcasting
├── Streaming Integration
│   ├── Icecast/Shoutcast
│   ├── HLS/DASH streaming
│   └── Listener analytics
├── Programmatic Ads
│   ├── Google Ad Manager
│   ├── SpotX
│   ├── Triton Digital
│   └── TargetSpot
├── Targeting
│   ├── Geographic (GeoIP)
│   ├── Demographic
│   ├── Behavioral
│   └── Contextual
└── Cross-Platform
    ├── Web player
    ├── Mobile apps
    ├── Smart speakers
    └── Connected TV
```

#### Implementation (Month 4-5)
- [ ] Streaming server integration
- [ ] Programmatic ad insertion
- [ ] Listener analytics
- [ ] Cross-platform scheduling

#### Pricing
| Tier | Price | Features |
|------|-------|----------|
| Starter | $99/mo | 1 stream, 1K concurrent |
| Growth | $299/mo | 3 streams, 10K concurrent |
| Enterprise | $799/mo | Unlimited, 100K concurrent |

---

### Target Market 3: International

#### Why International?
1. **WideOrbit US-centric**: Weak internationally
2. **Local requirements**: Different regulations
3. **Currency/language**: Need localization
4. **Growth markets**: Africa, Asia, Latin America

#### International Features
```
International Support
├── Localization
│   ├── Multi-language UI
│   ├── RTL support
│   ├── Local date formats
│   └── Local number formats
├── Multi-Currency
│   ├── 50+ currencies
│   ├── Real-time conversion
│   └── Local tax rules
├── Regulatory
│   ├── GDPR (EU)
│   ├── POPIA (South Africa)
│   ├── LGPD (Brazil)
│   └── Local broadcasting laws
└── Regional
    ├── Local payment methods
    ├── Regional ad networks
    └── Time zone handling
```

#### Priority Markets
1. **Canada** (similar to US, bilingual)
2. **UK/EU** (GDPR compliance needed)
3. **Australia/NZ** (English, similar market)
4. **South Africa** (English, growing market)
5. **Brazil** (Portuguese, large market)
6. **India** (English, massive market)

#### Implementation (Month 5-6)
- [ ] i18n framework
- [ ] Multi-currency support
- [ ] GDPR compliance
- [ ] Local payment methods
- [ ] Regional hosting

#### Pricing
| Market | Pricing Strategy |
|--------|------------------|
| Developed (UK, AU, CA) | Same as US |
| Emerging (BR, ZA, IN) | 50% discount |
| Developing (Rest) | Free tier only |

---

### Phase C Success Metrics
| Metric | Target |
|--------|--------|
| Podcast shows | 100 |
| Digital streams | 50 |
| International stations | 30 |
| MRR | $10,000 |
| Churn rate | <5%/month |

---

## Phase A: Dominate (Months 7-18)

### Goal
Build enterprise features to compete head-to-head with WideOrbit.

### Trigger: When Phase C MRR reaches $10K/month

### Features from IMPLEMENTATION_PLAN_PHASE1.md

#### P0 Features (Months 7-10)
1. **Commission System** (Month 7)
   - Sales rep commission calculation
   - Commission reports
   - Payout tracking

2. **Avails Management** (Month 7-8)
   - Real-time inventory
   - Sellout tracking
   - Rate cards

3. **Preemption & Makegoods** (Month 8-9)
   - Priority-based preemption
   - Automatic makegood generation
   - Makegood workflow

4. **A/R Management** (Month 9-10)
   - Aging reports
   - Collections workflow
   - Payment processing

5. **Advanced Reporting** (Month 10)
   - Pacing reports
   - Sales performance
   - Inventory analysis

#### P1 Features (Months 11-14)
6. **Proposal Generator** (Month 11)
   - Drag-and-drop proposal builder
   - Avails lookup
   - Digital delivery

7. **Rate Cards** (Month 11-12)
   - Flexible pricing
   - Volume discounts
   - Package deals

8. **Separation Rules** (Month 12)
   - Competitive separation
   - Category rules
   - Time-based separation

9. **As-Run Reconciliation** (Month 13)
   - Automation log import
   - Variance reporting
   - Auto-billing adjustments

10. **Multi-Station Management** (Month 14)
    - Corporate dashboards
    - Cross-station reporting
    - Shared inventory

#### P2 Features (Months 15-18)
11. **Agency Integrations** (Month 15)
    - API for agencies
    - Electronic order entry
    - Self-serve portal

12. **Business Intelligence** (Month 16-17)
    - Predictive forecasting
    - Pricing optimization
    - Churn prediction

13. **Mobile Apps** (Month 17-18)
    - iOS/Android apps
    - Offline mode
    - Push notifications

### Pricing (Phase A)
| Tier | Price | Target |
|------|-------|--------|
| Commercial | $499/mo | Small stations |
| Enterprise | $1,499/mo | Mid-market |
| Broadcast Group | $4,999/mo | Large groups |

### Success Metrics
| Metric | Target |
|--------|--------|
| Commercial stations | 100 |
| Enterprise clients | 20 |
| ARR | $500,000 |
| WideOrbit migrations | 10 |
| Market share | 2% |

---

## Financial Projections

### Revenue by Phase

| Phase | Timeline | Monthly Recurring Revenue |
|-------|----------|---------------------------|
| B | Months 1-2 | $0 (free tier only) |
| C | Months 3-6 | $10,000 |
| A | Months 7-12 | $50,000 |
| A | Months 13-18 | $150,000 |
| Growth | Year 2 | $500,000 |
| Scale | Year 3 | $1,500,000 |

### Cumulative Investment

| Phase | Development Cost | Infrastructure | Total |
|-------|------------------|----------------|-------|
| B | $20,000 | $2,000 | $22,000 |
| C | $60,000 | $5,000 | $65,000 |
| A (P0) | $80,000 | $10,000 | $90,000 |
| A (P1) | $100,000 | $15,000 | $115,000 |
| A (P2) | $120,000 | $25,000 | $145,000 |
| **Total Year 1** | **$380,000** | **$57,000** | **$437,000** |

### Break-Even Analysis
- **Break-even**: Month 14 (when MRR covers costs)
- **Payback period**: 18 months
- **ROI Year 2**: 250%

---

## Risk Mitigation by Phase

### Phase B Risks
| Risk | Mitigation |
|------|------------|
| Not enough free users | Partner with LPFM organizations |
| Stability issues | Extensive testing, CI/CD |
| Support burden | Community forum, documentation |

### Phase C Risks
| Risk | Mitigation |
|------|------------|
| Market doesn't convert | Keep free tier, add value |
| Technical complexity | Modular architecture |
| Competition | Focus on underserved niches |

### Phase A Risks
| Risk | Mitigation |
|------|------------|
| WideOrbit price war | Maintain cost advantage |
| Feature parity takes too long | Prioritize critical features |
| Sales cycle too long | Freemium + self-service |

---

## Key Milestones

| Date | Milestone | Success Criteria |
|------|-----------|------------------|
| Month 2 | Phase B Complete | 50 active free stations |
| Month 4 | Podcast Launch | 25 podcast shows signed up |
| Month 6 | Phase C Complete | $10K MRR |
| Month 10 | Phase A P0 Complete | Commission, avails, A/R working |
| Month 14 | Phase A P1 Complete | Proposal, rate cards, separation |
| Month 18 | Phase A P2 Complete | Full WideOrbit parity |
| Month 24 | $500K ARR | Sustainable growth |
| Month 36 | $1.5M ARR | Market leadership |

---

## Team Growth Plan

### Phase B (Months 1-2)
- 1 Full-stack developer (you)
- 1 DevOps (part-time)
- 1 Support (part-time)

### Phase C (Months 3-6)
- 2 Backend developers
- 1 Frontend developer
- 1 DevOps
- 1 Support

### Phase A (Months 7-18)
- 4 Backend developers
- 2 Frontend developers
- 1 Mobile developer
- 1 DevOps
- 1 QA engineer
- 2 Support
- 1 Sales/Marketing

---

## Conclusion

**B → C → A is the optimal sequence** because:

1. **B (Stabilize)**: Reduces risk, builds foundation
2. **C (Expand)**: Generates revenue, finds product-market fit
3. **A (Dominate)**: Uses revenue to fund WideOrbit competition

This approach:
- ✅ Minimizes initial investment
- ✅ Validates product before major build
- ✅ Generates revenue to fund growth
- ✅ Builds user base for network effects
- ✅ Creates optionality (can stay in B/C if A is too hard)

**Next Step**: Focus on Phase B - stabilize the current MVP and get 50 community stations using it.

---

*Roadmap Version: 1.0*  
*Strategy: B → C → A*  
*Created: 2026-03-13*
