# LibreLog User Manual

## Table of Contents

1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Authentication & Setup](#authentication--setup)
4. [Dashboard](#dashboard)
5. [Traffic Management](#traffic-management)
6. [Library Management](#library-management)
7. [Clock Templates](#clock-templates)
8. [Log Management](#log-management)
9. [Voice Tracking](#voice-tracking)
10. [Reports](#reports)
11. [Billing](#billing)
12. [Analytics](#analytics)
13. [Administration](#administration)
14. [Help Center](#help-center)

---

## Introduction

LibreLog is a professional radio traffic, scheduling, and automation system designed for radio stations. It manages the complete workflow from order entry through scheduling to on-air playback, integrating with LibreTime for automation.

### Key Features

- **Order Management**: Create and manage advertising orders
- **Spot Scheduling**: Schedule commercial spots with conflict detection
- **Copy Management**: Manage commercial audio files and scripts
- **Log Generation**: Generate daily programming logs
- **Billing**: Invoice generation and payment tracking
- **Analytics**: Inventory, revenue, and sales goal tracking
- **Integration**: Seamless integration with LibreTime and AzuraCast

### User Roles

- **Sales Person**: Creates orders, manages advertisers, uploads copy
- **Sales Manager**: Reviews and approves orders
- **Traffic Manager**: Schedules spots, manages inventory
- **Log Generator**: Creates and publishes daily logs
- **Billing Specialist**: Generates invoices and tracks payments
- **Administrator**: System configuration and user management

---

## Getting Started

### First-Time Setup

1. Navigate to `/setup` in your browser
2. Follow the setup wizard to:
   - Create an admin user
   - Configure API keys (LibreTime and AzuraCast)
   - Verify system configuration

### Accessing the Application

- **Frontend URL**: http://localhost:3000 (or your configured domain)
- **API Documentation**: http://localhost:8000/docs
- **Default Login**: Use the credentials created during setup

---

## Authentication & Setup

### Login Page (`/login`)

![Login Page](../screenshots/auth/login-page.png)

The login page provides secure authentication to access LibreLog.

#### Features

- Username and password authentication
- Session management
- Automatic redirect if already logged in
- Error handling for invalid credentials

#### How to Use

1. Navigate to the login page
2. Enter your username
3. Enter your password
4. Click "Sign In"
5. You'll be redirected to the dashboard upon successful login

#### Troubleshooting

- **Invalid credentials**: Verify your username and password are correct
- **Session expired**: Log in again
- **Redirect issues**: Clear browser cache and cookies

---

### Setup Page (`/setup`)

![Setup Page](../screenshots/auth/setup-page.png)

The setup page guides you through initial system configuration.

#### Features

- Step-by-step setup wizard
- Admin user creation
- API key configuration verification
- System status checks

#### Setup Steps

1. **Create Admin User**
   - Enter username (default: `admin`)
   - Enter password (default: `admin123`)
   - Confirm password
   - Click "Create Admin User"
   - ⚠️ **Important**: Change the default password after first login!

2. **Configure API Keys**
   - Edit your `.env` file with:
     ```
     LIBRETIME_URL=https://your-libretime-url/api
     LIBRETIME_API_KEY=your-libretime-api-key
     AZURACAST_URL=https://your-azuracast-url/api
     AZURACAST_API_KEY=your-azuracast-api-key
     ```
   - The system will verify configuration automatically

3. **Quick Setup**
   - Optionally run "Initial Setup" to create default configurations
   - This sets up basic system defaults

#### Status Indicators

- ✅ Green checkmark: Step completed
- ⚠️ Warning: Configuration needed
- ❌ Error: Action required

---

## Dashboard

![Dashboard](../screenshots/dashboard/dashboard-overview.png)

The dashboard provides an overview of system activity and key metrics.

### Overview Statistics

The dashboard displays:
- **Total Tracks**: Number of tracks in the music library
- **Active Campaigns**: Currently active advertising campaigns
- **Clock Templates**: Available clock templates for log generation
- **Reports Generated**: Total reports created

### Recent Activity

Shows the most recent system activities:
- Order creation and modifications
- Spot scheduling
- Log generation
- Copy uploads
- User actions

### API Health Monitoring

- Displays connection status to LibreTime and AzuraCast
- Shows API response times
- Alerts if connections fail

### Quick Navigation

Quick links to frequently used pages:
- Traffic Manager
- Orders
- Log Generator
- Reports

### Troubleshooting

- **API Connection Failed**: Check backend container status and API configuration
- **No Data**: Verify database connection and run migrations if needed

---

## Traffic Management

The Traffic Management section is the core of LibreLog, handling all aspects of advertising orders, spots, and scheduling.

### Traffic Manager Hub (`/traffic`)

![Traffic Manager Hub](../screenshots/traffic/traffic-manager-hub.png)

The Traffic Manager hub provides navigation to all traffic-related functions.

#### Available Sections

- **Advertisers**: Manage advertising clients
- **Agencies**: Manage advertising agencies
- **Orders**: View and manage advertising orders
- **Sales Reps**: Manage sales representatives
- **Spot Scheduler**: Schedule spots for orders
- **Dayparts**: Configure broadcast dayparts
- **Copy Library**: Manage commercial audio and scripts

---

### Advertisers (`/traffic/advertisers`)

![Advertisers List](../screenshots/traffic/advertisers/advertisers-list.png)

Manage advertising clients (businesses that purchase advertising).

#### Creating an Advertiser

1. Click "Add Advertiser" button
2. Fill in the form:
   - **Name** (required): Business or organization name
   - **Contact Name**: Primary contact person
   - **Email**: Billing and communication email
   - **Phone**: Contact number
   - **Address**: Billing address
   - **Tax ID**: Tax identification number
   - **Payment Terms**: e.g., "Net 30", "Net 15", "Due on Receipt"
   - **Credit Limit**: Maximum amount they can owe
3. Click "Save"

![Advertiser Form](../screenshots/traffic/advertisers/advertiser-form.png)

#### Managing Advertisers

- **Search**: Use the search box to find advertisers by name
- **Edit**: Click the edit icon to modify advertiser information
- **Delete**: Click the delete icon (with confirmation)
- **View Orders**: Navigate to orders filtered by this advertiser

#### Key Fields Explained

- **Payment Terms**: Defines when payment is due (Net 30 = 30 days after invoice)
- **Credit Limit**: Maximum outstanding balance allowed
- **Active Status**: Inactive advertisers won't appear in new order creation

#### Best Practices

- Always verify contact information before creating
- Set appropriate credit limits based on payment history
- Keep payment terms consistent with your billing practices
- Mark inactive when advertisers are no longer active

---

### Agencies (`/traffic/agencies`)

![Agencies List](../screenshots/traffic/agencies/agencies-list.png)

Manage advertising agencies that represent advertisers.

#### Creating an Agency

1. Click "Add Agency" button
2. Fill in the form:
   - **Name** (required): Agency name
   - **Contact Name**: Primary contact person
   - **Email**: Billing and communication email
   - **Phone**: Contact number
   - **Address**: Billing address
   - **Commission Rate**: Percentage (e.g., 15 for 15%)
3. Click "Save"

![Agency Form](../screenshots/traffic/agencies/agency-form.png)

#### Agency vs Advertiser

- **Advertiser**: The business being advertised (always required)
- **Agency**: The company representing the advertiser (optional)
- An order can have both an advertiser AND an agency
- Billing typically goes to the agency if one is specified

#### Commission Rates

- Set commission percentage for agency orders
- Used for commission calculations in reporting
- Typically 15% for standard agency relationships

---

### Orders (`/traffic/orders`)

![Orders List](../screenshots/traffic/orders/orders-list.png)

Orders are contracts for advertising that specify how many spots to run, when, and at what rate.

#### Creating an Order

1. Click "New Order" button
2. Fill in order details:

   **Basic Information**
   - **Order Number**: Auto-generated if left blank (format: YYYYMMDD-XXXX)
   - **Advertiser** (required): Select from existing advertisers
   - **Agency** (optional): Select if advertiser is represented by an agency
   - **Sales Rep** (optional): Assign to a sales representative

   **Schedule**
   - **Start Date**: When commercials should begin
   - **End Date**: When commercials should end
   - **Total Spots**: How many commercials to run

   **Spot Details**
   - **Spot Lengths**: Select 15, 30, or 60 seconds (can select multiple)
   - **Rate Type**: 
     - **ROS** (Run of Schedule): Any time, most flexible
     - **DAYPART**: Specific time periods
     - **PROGRAM**: Program-specific
     - **FIXED_TIME**: Exact times

   **Pricing**
   - **Rates**: Enter rate structure based on rate type
   - **Total Value**: Total cost of the order

3. Click "Save"

![Order Form](../screenshots/traffic/orders/order-form.png)

#### Order Statuses

- **DRAFT**: Still being created, not submitted
- **PENDING**: Submitted for approval
- **APPROVED**: Approved by sales manager, ready for scheduling
- **ACTIVE**: Spots are being scheduled and airing
- **COMPLETED**: All spots have aired
- **CANCELLED**: Order was cancelled

#### Order Actions

- **Approve**: Change status from PENDING to APPROVED (Sales Manager)
- **Duplicate**: Create a copy of the order
- **Edit**: Modify order details (limited once spots are scheduled)
- **Delete**: Remove order (only if no spots scheduled)

#### Filtering Orders

- Filter by status using the status dropdown
- Search by order number or advertiser name
- View orders by date range

#### Order Templates

- Save common order configurations as templates
- Use templates to quickly create similar orders
- Access via "Order Templates" button

---

### Sales Reps (`/traffic/sales-reps`)

![Sales Reps List](../screenshots/traffic/sales-reps/sales-reps-list.png)

Manage sales representatives who sell advertising.

#### Creating a Sales Rep

1. Click "Add Sales Rep" button
2. Fill in the form:
   - **User** (required): Select from system users
   - **Employee ID**: Optional employee identifier
   - **Commission Rate**: Percentage for commission calculations
   - **Sales Goal**: Monthly or quarterly sales goal
3. Click "Save"

![Sales Rep Form](../screenshots/traffic/sales-reps/sales-rep-form.png)

#### Assigning to Orders

- Select sales rep when creating an order
- Used for commission tracking
- Appears in sales reports

#### Sales Goals

- Set individual sales goals
- Track progress in Analytics > Sales Goals
- Used for performance reporting

---

### Spot Scheduler (`/traffic/spot-scheduler`)

![Spot Scheduler](../screenshots/traffic/spot-scheduler/spot-scheduler.png)

Schedule individual spots from approved orders into the daily schedule.

#### Scheduling Spots

1. **Select Order**: Choose an approved order from the dropdown
2. **Set Date Range**: Select start and end dates
3. **Configure Spots**:
   - **Spot Length**: Select 15, 30, or 60 seconds
   - **Break Position**: Optional (A, B, C, D, E)
   - **Daypart**: Optional daypart restriction
4. **Generate Spots**: Click "Schedule" to create spots
5. **Review**: Preview scheduled spots before confirming

![Spot Scheduling Form](../screenshots/traffic/spot-scheduler/scheduling-form.png)

#### Automatic Distribution

- System automatically distributes spots across the date range
- Respects rate type (ROS, Daypart, Fixed Time)
- Avoids conflicts where possible

#### Manual Scheduling

- Drag and drop spots into specific times
- Adjust times manually
- System validates for conflicts

#### Conflict Resolution

- View conflict warnings
- Adjust spot times to resolve
- Mark conflicts as resolved when fixed

#### Break Positions

- **A Position**: First in break (premium)
- **B Position**: Second in break
- **C, D, E**: Subsequent positions
- Assign based on order requirements and rates

---

### Dayparts (`/traffic/dayparts`)

![Dayparts List](../screenshots/traffic/dayparts/dayparts-list.png)

Define time periods used for scheduling and pricing (e.g., Morning Drive, Afternoon Drive).

#### Creating a Daypart

1. Click "Add Daypart" button
2. Fill in the form:
   - **Name**: Descriptive name (e.g., "Morning Drive")
   - **Start Time**: When daypart begins (e.g., 06:00)
   - **End Time**: When daypart ends (e.g., 10:00)
   - **Description**: Optional notes
   - **Category**: Optional category grouping
   - **Active**: Enable or disable
3. Click "Save"

![Daypart Form](../screenshots/traffic/dayparts/daypart-form.png)

#### Standard Dayparts

- **Morning Drive**: 6:00 AM - 10:00 AM (highest audience)
- **Midday**: 10:00 AM - 3:00 PM
- **Afternoon Drive**: 3:00 PM - 7:00 PM (high audience)
- **Evening**: 7:00 PM - Midnight
- **Overnight**: Midnight - 6:00 AM

#### Daypart Categories

- Group related dayparts together
- Used for organization and reporting
- Examples: "Prime Time", "Off-Peak", "Drive Time"

---

### Daypart Categories (`/traffic/daypart-categories`)

![Daypart Categories](../screenshots/traffic/daypart-categories/categories-list.png)

Organize dayparts into groups for better management.

#### Creating a Category

1. Click "Add Category" button
2. Enter:
   - **Name**: Category name
   - **Description**: What dayparts belong here
   - **Color**: Optional hex color for visual organization
   - **Sort Order**: Display order
3. Click "Save"

#### Using Categories

- Assign dayparts to categories when creating/editing
- Filter dayparts by category
- Use in reporting and analytics

---

### Rotation Rules (`/traffic/rotation-rules`)

![Rotation Rules](../screenshots/traffic/rotation-rules/rotation-rules-list.png)

Control how spots and copy versions rotate throughout the day.

#### Creating a Rotation Rule

1. Click "Add Rotation Rule" button
2. Configure:
   - **Name**: Descriptive name
   - **Rotation Type**: Sequential, Random, or Weighted
   - **Daypart**: Optional - apply only to specific daypart
   - **Campaign**: Optional - apply to specific campaign
   - **Min Separation**: Minimum minutes between spots
   - **Max Per Hour**: Maximum spots per hour
   - **Max Per Day**: Maximum spots per day
3. Click "Save"

#### Rotation Types

- **Sequential**: Spots play in order (Version 1, 2, 3, repeat)
- **Random**: Spots play in random order
- **Weighted**: Some versions play more often (based on weights)

#### Rule Parameters

- **Min Separation**: Prevents spots from playing too close together
- **Max Per Hour/Day**: Prevents over-saturation

---

### Traffic Logs (`/traffic/traffic-logs`)

![Traffic Logs](../screenshots/traffic/traffic-logs/traffic-logs-list.png)

View activity logs and audit trail for all traffic system actions.

#### Viewing Logs

- Filter by date range
- Filter by user
- Filter by activity type
- Search logs

#### Log Details

Each log entry shows:
- **Timestamp**: When it happened
- **User**: Who performed the action
- **Activity Type**: What type of action
- **Message**: Description
- **Related Items**: Links to orders, spots, etc.

#### Activity Types

- Order creation/modification
- Spot scheduling changes
- Copy uploads and assignments
- Log generation and publishing
- User actions

---

### Copy Library (`/traffic/copy`)

![Copy Library](../screenshots/traffic/copy/copy-library-list.png)

Manage commercial audio files and scripts (copy).

#### Uploading Copy

1. Click "Upload Copy" button
2. Fill in details:
   - **Order**: Select associated order (optional)
   - **Advertiser**: Select advertiser
   - **Title**: Descriptive name
   - **Type**: Audio file or Script
   - **File**: Upload audio file (if audio type)
   - **Script Text**: Enter script text (if script type)
   - **Version**: Version number
   - **Expiration Date**: When copy expires
3. Click "Save"

![Copy Upload Form](../screenshots/traffic/copy/copy-upload-form.png)

#### Copy Types

- **Pre-Produced Audio**: Recorded commercial (MP3, WAV, etc.)
- **Live Read Script**: Text for DJ/host to read on-air

#### Copy Versions

- Create multiple versions for A/B testing
- System rotates versions automatically
- Track which version is assigned to which spots

#### Copy Assignment

- Assign copy to specific spots
- View assignments in copy detail dialog
- Manage multi-cut rotations

#### Expiring Copy Alert

- System alerts when copy is expiring soon
- Review and update expiration dates
- Archive expired copy

---

## Library Management

### Library List (`/library`)

![Library List](../screenshots/library/library-list.png)

Manage the music library, synced from LibreTime.

#### Features

- View all tracks in library
- Search tracks by title, artist, album
- Filter by track type
- View track metadata
- Sync with LibreTime

#### Track Information

- Title, Artist, Album
- Duration
- Track Type (Music, PSA, etc.)
- LibreTime ID
- Last synced date

#### Syncing with LibreTime

- Click "Sync Tracks" to manually trigger sync
- System syncs track metadata from LibreTime
- Tracks are used in log generation

---

### Spots Library (`/library/spots`)

![Spots Library](../screenshots/library/spots-library.png)

View all scheduled spots across all orders.

#### Features

- View all spots in one place
- Filter by order, date, status
- Search spots
- View spot details
- Navigate to related order

#### Spot Status

- **SCHEDULED**: Spot is scheduled but hasn't aired yet
- **AIRED**: Spot has aired
- **MISSED**: Spot was scheduled but didn't air
- **MAKEGOOD**: Replacement spot for missed spot

---

## Clock Templates

### Clock Builder (`/clocks`)

![Clock Builder](../screenshots/clocks/clock-builder.png)

Create and manage clock templates that define the structure of each hour.

#### Creating a Clock Template

1. Click "New Template" or select existing template
2. Enter template details:
   - **Name**: Template name (e.g., "Weekday Morning")
   - **Description**: When this template is used
3. Add elements to the clock:
   - Click "Add Element"
   - Select element type:
     - **MUS**: Music
     - **ADV**: Commercial advertisements
     - **PSA**: Public service announcements
     - **IDS**: Station identification
     - **NEW**: News segments
     - **LIN**: Liners (station promos)
     - **INT**: Interviews
     - **PRO**: Promos
   - Set element properties:
     - **Count**: How many of this element
     - **Duration**: Length in seconds
     - **Scheduled Time**: Optional specific time
     - **Hard Start**: Must start at exact time
     - **Fallback**: What to use if primary unavailable
4. Arrange elements in order
5. Click "Save Template"

![Clock Element Dialog](../screenshots/clocks/clock-element-dialog.png)

#### Clock Structure

A clock template defines:
- What elements play in each hour
- When each element plays
- How long each element is
- What to use if primary content unavailable

#### Preview

- Preview clock structure before saving
- See timeline visualization
- Verify timing and flow

#### Using Clock Templates

- Select clock template when generating logs
- Different templates for different times/days
- System uses template to structure hourly logs

---

## Log Management

### Log Generator (`/logs`)

![Log Generator](../screenshots/logs/log-generator.png)

Generate daily programming logs that combine scheduled spots, music, and other programming elements.

#### Generating a Log

1. Select **Target Date**: Choose the date for the log
2. Select **Clock Template**: Choose which clock template to use
3. **Preview** (optional): Click "Preview" to see what will be generated
4. **Generate**: Click "Generate Log" to create the log
5. Review the generated log

![Log Generation Form](../screenshots/logs/log-generation-form.png)

#### What Gets Generated

- Complete schedule for the day
- All scheduled spots placed according to clock template
- Music selected from library
- IDs, news, and other elements
- Precise timing for each element

#### Preview Functionality

- Preview log before generating
- See hourly breakdown
- Check spot placement
- Verify timing

#### Log Status

- **DRAFT**: Generated but not published
- **PUBLISHED**: Sent to LibreTime, locked for editing

---

### Log Editor (`/logs/:logId/edit`)

![Log Editor](../screenshots/logs/log-editor.png)

Edit generated logs before publishing.

#### Editing a Log

1. Open the log you want to edit
2. Review the timeline view
3. Make changes:
   - **Add Spots**: Add additional spots
   - **Move Elements**: Drag and drop to new times
   - **Remove Elements**: Delete unwanted elements
   - **Adjust Timing**: Modify element timing
4. Resolve any conflicts
5. Save changes

#### Timeline View

- See entire day's schedule
- Hour-by-hour breakdown
- Element timing visualization
- Conflict indicators

#### Conflict Resolution

- View conflict warnings
- Adjust timing to resolve
- Replace conflicting elements
- Mark conflicts as resolved

#### Publishing to LibreTime

1. Review entire log one final time
2. Click "Publish"
3. Log is sent to LibreTime
4. Log status changes to PUBLISHED
5. Log is locked (cannot be edited)

⚠️ **Note**: Once published, logs cannot be edited. Create a new revision if changes are needed.

---

## Voice Tracking

### Voice Recorder (`/voice`)

![Voice Recorder](../screenshots/voice/voice-recorder.png)

Record voice tracks for inclusion in logs.

#### Recording Voice Tracks

1. Click "Start Recording"
2. Record your voice track
3. Click "Stop Recording"
4. Review the recording
5. Save or re-record

#### Voice Track Management

- View all recorded voice tracks
- Assign to specific logs
- Delete old tracks
- Download tracks

#### Assigning to Logs

- Select voice track
- Assign to specific log and time
- Voice track plays at scheduled time

---

## Reports

### Reports Hub (`/reports`)

![Reports Hub](../screenshots/reports/reports-hub.png)

Central hub for all reporting functions.

#### Available Reports

- **Avails Report**: Available inventory by date/time
- **Compliance Report**: Political and regulatory compliance
- **Playback Report**: What actually aired vs scheduled
- **Revenue Report**: Revenue by period, advertiser, etc.

#### Generating Reports

1. Select report type
2. Set date range
3. Configure filters
4. Generate report
5. Export (PDF, CSV, Excel)

---

## Billing

### Invoices (`/billing/invoices`)

![Invoices List](../screenshots/billing/invoices/invoices-list.png)

Generate and manage invoices for completed orders.

#### Generating an Invoice

1. Click "Generate from Order"
2. Select the completed order
3. System creates invoice with:
   - All aired spots
   - Line items
   - Pricing
   - Tax calculations
4. Review invoice
5. Send to advertiser/agency

![Invoice Detail](../screenshots/billing/invoices/invoice-detail.png)

#### Invoice Statuses

- **DRAFT**: Created but not sent
- **SENT**: Emailed to customer
- **PAID**: Fully paid
- **PARTIAL**: Partially paid
- **OVERDUE**: Past due date

#### Sending Invoices

1. Open invoice
2. Review details
3. Click "Send Invoice"
4. System:
   - Generates PDF
   - Emails to advertiser/agency
   - Updates status to SENT
   - Records send date

#### AR Aging Dashboard

- View invoices by age
- Current (0-30 days)
- 31-60 days
- 61-90 days
- Over 90 days
- Follow up on overdue accounts

---

### Payments (`/billing/payments`)

![Payments List](../screenshots/billing/payments/payments-list.png)

Record and track payments received.

#### Recording a Payment

1. Click "Record Payment"
2. Select invoice
3. Enter:
   - **Payment Amount**
   - **Payment Date**
   - **Payment Method** (Check, Credit Card, ACH, etc.)
   - **Reference Number**
4. Click "Save"

#### Payment Tracking

- View payment history
- See which invoices are paid
- Track partial payments
- Generate payment reports

---

### Makegoods (`/billing/makegoods`)

![Makegoods List](../screenshots/billing/makegoods/makegoods-list.png)

Manage makegoods (free replacement spots) for missed commercials.

#### Creating a Makegood

1. Click "Create Makegood"
2. Select the missed spot
3. Enter reason for makegood
4. System schedules replacement spot
5. Makegood is free to advertiser

#### Makegood Tracking

- View all makegoods
- Track completion status
- See which spots were replaced
- Generate makegood reports

---

## Analytics

### Inventory Dashboard (`/analytics/inventory`)

![Inventory Dashboard](../screenshots/analytics/inventory/inventory-dashboard.png)

View inventory availability and utilization.

#### Features

- **Heatmap**: Visual representation of availability by date/time
- **Utilization**: Percentage of inventory used
- **Availability**: Open slots by daypart
- **Forecasting**: Projected availability

#### Using Inventory Data

- Identify open inventory
- Plan sales efforts
- Optimize spot placement
- Balance inventory across dayparts

---

### Revenue Dashboard (`/analytics/revenue`)

![Revenue Dashboard](../screenshots/analytics/revenue/revenue-dashboard.png)

Track revenue and financial performance.

#### Features

- **Revenue by Period**: Daily, weekly, monthly, quarterly
- **Revenue by Advertiser**: Top advertisers
- **Revenue Trends**: Growth over time
- **Forecasting**: Projected revenue

#### Revenue Metrics

- Total revenue
- Revenue by daypart
- Revenue by rate type
- Average spot value

---

### Sales Goals (`/analytics/sales-goals`)

![Sales Goals](../screenshots/analytics/sales-goals/sales-goals.png)

Set and track sales goals.

#### Setting Goals

1. Click "Set Goal"
2. Enter:
   - **Period**: Month, Quarter, Year
   - **Target Amount**: Goal amount
   - **Sales Rep**: Individual or team goal
3. Click "Save"

#### Tracking Progress

- View progress toward goals
- See performance by sales rep
- Compare actual vs goal
- Generate performance reports

---

## Administration

### Users (`/admin/users`)

![Users List](../screenshots/admin/users/users-list.png)

Manage system users and their roles.

#### Creating a User

1. Click "Add User"
2. Enter:
   - **Username**: Login username
   - **Password**: Initial password
   - **Email**: User email
   - **Role**: Select role (admin, sales, traffic_manager, etc.)
3. Click "Save"

#### User Roles

- **Admin**: Full system access
- **Sales**: Order creation and management
- **Sales Manager**: Order approval
- **Traffic Manager**: Spot scheduling
- **Log Generator**: Log creation and publishing
- **Billing**: Invoice and payment management

#### Managing Users

- Edit user information
- Change passwords
- Assign/remove roles
- Activate/deactivate users

---

### Settings (`/admin/settings`)

![Settings](../screenshots/admin/settings/settings-page.png)

Configure system settings and integrations.

#### API Configuration

- **LibreTime URL**: LibreTime API endpoint
- **LibreTime API Key**: Authentication key
- **AzuraCast URL**: AzuraCast API endpoint
- **AzuraCast API Key**: Authentication key

#### System Settings

- General system configuration
- Email settings (for invoice sending)
- Notification preferences
- Default values

---

### Backups (`/admin/backups`)

![Backups](../screenshots/admin/backups/backups-list.png)

Manage system backups.

#### Creating Backups

1. Click "Create Backup"
2. System creates backup of:
   - Database
   - Configuration
   - Uploaded files
3. Download backup file

#### Restoring Backups

1. Upload backup file
2. Click "Restore"
3. System restores from backup
4. ⚠️ **Warning**: This will overwrite current data

---

### Audit Logs (`/admin/audit-logs`)

![Audit Logs](../screenshots/admin/audit-logs/audit-logs-list.png)

View system audit trail and security logs.

#### Features

- View all system actions
- Filter by user, date, action type
- Search audit logs
- Export logs

#### Logged Actions

- User logins/logouts
- Order creation/modification
- Spot scheduling
- Log generation
- Settings changes
- User management

---

### Webhooks (`/admin/webhooks`)

![Webhooks](../screenshots/admin/webhooks/webhooks-list.png)

Configure webhooks for external integrations.

#### Creating a Webhook

1. Click "Add Webhook"
2. Enter:
   - **URL**: Webhook endpoint
   - **Events**: Which events trigger webhook
   - **Secret**: Authentication secret
3. Click "Save"

#### Webhook Events

- Order created/updated
- Spot scheduled
- Log published
- Invoice sent
- Payment received

---

### Notifications (`/admin/notifications`)

![Notifications](../screenshots/admin/notifications/notifications-settings.png)

Configure notification settings and alerts.

#### Notification Types

- Order approval requests
- Spot conflicts
- Copy expiring
- Invoice overdue
- System errors

#### Notification Channels

- In-app notifications
- Email notifications
- Webhook notifications

---

## Help Center

### Help Center (`/help`)

![Help Center](../screenshots/help/help-center.png)

Access help documentation, terminology, and workflow guides.

#### Features

- **Terminology Dictionary**: Definitions of radio industry terms
- **Concept Explanations**: Detailed explanations of key concepts
- **Workflow Guides**: Step-by-step guides for common tasks
- **Search**: Search help content

#### Getting Help

- Browse help topics
- Search for specific information
- View workflow guides
- Access terminology dictionary

---

## Troubleshooting

### Common Issues

#### API Connection Issues

- **Problem**: Cannot connect to LibreTime/AzuraCast
- **Solution**: 
  - Verify API keys in Settings
  - Check network connectivity
  - Verify API endpoints are correct
  - Check backend logs

#### Order Not Appearing

- **Problem**: Order not showing in list
- **Solution**:
  - Check order status filter
  - Verify user has permissions
  - Check date range
  - Refresh page

#### Spots Not Scheduling

- **Problem**: Cannot schedule spots
- **Solution**:
  - Verify order is APPROVED
  - Check date range is valid
  - Verify copy is assigned
  - Check for conflicts

#### Log Generation Fails

- **Problem**: Cannot generate log
- **Solution**:
  - Verify clock template exists
  - Check scheduled spots exist
  - Verify music library has tracks
  - Check system logs

---

## Best Practices

### Order Management

- Create advertisers before orders
- Upload copy as soon as possible
- Submit orders for approval promptly
- Keep order information accurate

### Spot Scheduling

- Schedule spots well in advance
- Resolve conflicts immediately
- Verify copy assignments
- Check daypart compliance

### Log Generation

- Generate logs in advance
- Review previews before generating
- Resolve conflicts before publishing
- Publish on schedule

### Billing

- Generate invoices promptly
- Send invoices on schedule
- Track payments accurately
- Follow up on overdue accounts

---

## Support

For additional support:
- Check the Help Center (`/help`)
- Review workflow guides
- Contact your system administrator
- Check system logs for errors

---

*Last Updated: [Current Date]*


