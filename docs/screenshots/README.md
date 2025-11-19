# Screenshots Directory

This directory contains screenshots for the LibreLog documentation. Screenshots are organized by page/section to match the documentation structure.

## Directory Structure

```
screenshots/
├── auth/              # Authentication and setup pages
├── dashboard/         # Dashboard page
├── traffic/           # Traffic management pages
│   ├── advertisers/
│   ├── agencies/
│   ├── orders/
│   ├── sales-reps/
│   ├── spot-scheduler/
│   ├── dayparts/
│   ├── daypart-categories/
│   ├── rotation-rules/
│   ├── traffic-logs/
│   └── copy/
├── library/           # Library management pages
├── clocks/            # Clock builder
├── logs/              # Log generator and editor
├── voice/             # Voice recorder
├── reports/           # Reports hub
├── billing/           # Billing pages
│   ├── invoices/
│   ├── payments/
│   └── makegoods/
├── analytics/         # Analytics pages
│   ├── inventory/
│   ├── revenue/
│   └── sales-goals/
└── admin/             # Administration pages
    ├── users/
    ├── settings/
    ├── backups/
    ├── audit-logs/
    ├── webhooks/
    └── notifications/
```

## Screenshot Requirements

### General Guidelines

- **Resolution**: Minimum 1920x1080, recommended 2560x1440
- **Format**: PNG format preferred
- **Browser**: Use consistent browser (Chrome recommended)
- **Window Size**: Use consistent browser window size for all screenshots
- **Data**: Use realistic but anonymized test data
- **Privacy**: Remove or blur any sensitive information

### Required Screenshots by Page

#### Authentication & Setup

**auth/login-page.png**
- Full login page
- Show username and password fields
- Include "Sign In" button

**auth/setup-page.png**
- Setup wizard showing all steps
- Show progress indicators
- Include admin user creation form

#### Dashboard

**dashboard/dashboard-overview.png**
- Full dashboard view
- Show statistics cards
- Show recent activity list
- Include API health indicators

#### Traffic Management

**traffic/advertisers/advertisers-list.png**
- List of advertisers
- Show search box
- Include "Add Advertiser" button

**traffic/advertisers/advertiser-form.png**
- Advertiser creation/edit form
- Show all fields
- Include save button

**traffic/agencies/agencies-list.png**
- List of agencies
- Similar to advertisers list

**traffic/agencies/agency-form.png**
- Agency creation/edit form

**traffic/orders/orders-list.png**
- Orders list with filters
- Show status badges
- Include "New Order" button

**traffic/orders/order-form.png**
- Order creation form
- Show all sections (Basic Info, Schedule, Spot Details, Pricing)
- Include status dropdown

**traffic/orders/order-detail-pending.png**
- Order detail view with PENDING status
- Show approval button

**traffic/sales-reps/sales-reps-list.png**
- Sales reps list

**traffic/sales-reps/sales-rep-form.png**
- Sales rep creation form

**traffic/spot-scheduler/spot-scheduler.png**
- Spot scheduler main view
- Show order selection
- Show date range picker

**traffic/spot-scheduler/scheduling-form.png**
- Spot scheduling form
- Show order, dates, spot length, break position, daypart

**traffic/dayparts/dayparts-list.png**
- Dayparts list

**traffic/dayparts/daypart-form.png**
- Daypart creation form
- Show start/end times

**traffic/copy/copy-library-list.png**
- Copy library list
- Show filters and search

**traffic/copy/copy-upload-form.png**
- Copy upload form
- Show file upload, script text option

#### Library

**library/library-list.png**
- Music library list
- Show track information

**library/spots-library.png**
- Spots library view
- Show all scheduled spots

#### Clocks

**clocks/clock-builder.png**
- Clock builder interface
- Show template list and editor

**clocks/clock-element-dialog.png**
- Clock element configuration dialog
- Show element type selection

#### Logs

**logs/log-generator.png**
- Log generator page
- Show date picker and clock template selector

**logs/log-generation-form.png**
- Log generation form
- Show preview and generate buttons

**logs/log-editor-timeline.png**
- Log editor with timeline view
- Show hourly breakdown

**logs/log-publish-dialog.png**
- Publish confirmation dialog

#### Billing

**billing/invoices/invoices-list.png**
- Invoices list
- Show status filters

**billing/invoices/invoice-detail.png**
- Invoice detail view
- Show line items and totals

**billing/invoices/invoice-sent.png**
- Invoice with SENT status
- Show send confirmation

**billing/payments/payments-list.png**
- Payments list

**billing/makegoods/makegoods-list.png**
- Makegoods list

#### Analytics

**analytics/inventory/inventory-dashboard.png**
- Inventory dashboard
- Show heatmap if available

**analytics/revenue/revenue-dashboard.png**
- Revenue dashboard
- Show charts and metrics

**analytics/sales-goals/sales-goals.png**
- Sales goals page
- Show goal setting and tracking

#### Admin

**admin/users/users-list.png**
- Users list
- Show role badges

**admin/settings/settings-page.png**
- Settings page
- Show API configuration

**admin/backups/backups-list.png**
- Backups list

**admin/audit-logs/audit-logs-list.png**
- Audit logs list

## Capturing Screenshots

### Process

1. **Start Application**
   - Ensure LibreLog is running
   - Use development or production environment
   - Clear browser cache for clean views

2. **Prepare Data**
   - Create test data if needed
   - Use realistic but anonymized information
   - Ensure data is in appropriate states (pending, approved, etc.)

3. **Capture Screenshots**
   - Navigate to each page
   - Capture full page views
   - Capture dialogs and modals separately
   - Use consistent browser zoom (100%)

4. **Organize Files**
   - Save with descriptive filenames
   - Place in appropriate subdirectories
   - Use lowercase with hyphens (kebab-case)

5. **Review**
   - Verify screenshots are clear
   - Check for sensitive information
   - Ensure consistency across screenshots

### Tools

- **Browser DevTools**: Use for consistent window sizing
- **Screenshot Tools**: 
  - Built-in browser screenshot (F12 → Cmd/Ctrl+Shift+P → "Capture screenshot")
  - Third-party tools (Snagit, Greenshot, etc.)
- **Image Editing**: For annotations or blurring sensitive data

## Notes

- Screenshots should reflect current UI state
- Update screenshots when UI changes significantly
- Keep file sizes reasonable (compress if needed)
- Maintain consistent styling across all screenshots

## Future Updates

When UI changes:
1. Identify affected screenshots
2. Capture new screenshots
3. Update documentation references
4. Remove outdated screenshots


