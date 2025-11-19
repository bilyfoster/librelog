# LibreLog Documentation

Welcome to the LibreLog documentation. This directory contains comprehensive documentation for the LibreLog radio traffic system.

## Documentation Files

### [USER_MANUAL.md](./USER_MANUAL.md)

Complete user manual covering all pages and sections of LibreLog:

- **Authentication & Setup**: Login and initial system setup
- **Dashboard**: Overview and system monitoring
- **Traffic Management**: Orders, spots, scheduling, copy management
- **Library Management**: Music library and spots library
- **Clock Templates**: Creating and managing clock templates
- **Log Management**: Generating and editing daily logs
- **Voice Tracking**: Recording and managing voice tracks
- **Reports**: Generating various reports
- **Billing**: Invoices, payments, and makegoods
- **Analytics**: Inventory, revenue, and sales goals
- **Administration**: Users, settings, backups, and system configuration
- **Help Center**: Accessing help and documentation

Each page includes:
- Purpose and when to use it
- Step-by-step instructions
- Field descriptions
- Common workflows
- Troubleshooting tips
- Screenshot references

### [WORKFLOW_SCENARIOS.md](./WORKFLOW_SCENARIOS.md)

Detailed end-to-end scenarios walking through complete workflows:

1. **Standard ROS Order Flow**: Complete workflow from order entry to payment
2. **Daypart-Specific Order Flow**: Order with daypart restrictions
3. **Fixed Time Order Flow**: Order with exact time requirements
4. **Multi-Copy Rotation Flow**: Order with multiple copy versions
5. **Makegood Flow**: Handling missed spots
6. **Campaign with Multiple Orders**: Managing multi-order campaigns

Each scenario includes:
- Complete step-by-step walkthrough
- Role assignments for each step
- Expected outcomes
- Timeline examples
- Common issues and solutions

### [DEVELOPMENT.md](./DEVELOPMENT.md)

Developer documentation for setting up and working with the LibreLog codebase.

## Screenshots

Screenshots are organized in the `screenshots/` directory. See [screenshots/README.md](./screenshots/README.md) for details on:
- Screenshot organization
- Required screenshots
- Capturing guidelines
- File naming conventions

**Note**: Screenshots should be captured and added to the documentation. Placeholder references are included in the documentation where screenshots should appear.

## Quick Start

### For New Users

1. Start with the [USER_MANUAL.md](./USER_MANUAL.md) - Overview section
2. Review the [Authentication & Setup](#authentication--setup) section
3. Follow the [Dashboard](#dashboard) section to understand the interface
4. Read relevant sections for your role (Sales, Traffic Manager, etc.)

### For Learning Workflows

1. Read [WORKFLOW_SCENARIOS.md](./WORKFLOW_SCENARIOS.md)
2. Start with Scenario 1: Standard ROS Order Flow
3. Follow along with the step-by-step instructions
4. Practice with test data

### For Specific Tasks

1. Use the table of contents in [USER_MANUAL.md](./USER_MANUAL.md)
2. Navigate to the relevant page section
3. Follow the step-by-step instructions
4. Refer to troubleshooting if needed

## Documentation Structure

```
docs/
├── README.md                 # This file - documentation index
├── USER_MANUAL.md            # Complete user manual
├── WORKFLOW_SCENARIOS.md     # End-to-end workflow scenarios
├── DEVELOPMENT.md            # Developer documentation
└── screenshots/              # Screenshot directory
    ├── README.md             # Screenshot guidelines
    ├── auth/                 # Authentication screenshots
    ├── dashboard/            # Dashboard screenshots
    ├── traffic/              # Traffic management screenshots
    ├── library/              # Library screenshots
    ├── clocks/               # Clock builder screenshots
    ├── logs/                 # Log management screenshots
    ├── voice/                # Voice tracking screenshots
    ├── reports/              # Reports screenshots
    ├── billing/              # Billing screenshots
    ├── analytics/            # Analytics screenshots
    └── admin/                # Administration screenshots
```

## Key Concepts

Before diving into the documentation, understand these key concepts:

- **Orders**: Contracts for advertising (how many spots, when, pricing)
- **Spots**: Individual commercial placements at specific dates/times
- **Logs**: Daily programming schedules combining spots, music, and other elements
- **Copy**: Commercial audio files or scripts
- **Dayparts**: Time periods (Morning Drive, Afternoon Drive, etc.)
- **Clock Templates**: Blueprints for hourly programming structure

See the Help Center in the application (`/help`) for detailed concept explanations.

## User Roles

Documentation is organized by user role:

- **Sales Person**: Creates orders, manages advertisers, uploads copy
- **Sales Manager**: Reviews and approves orders
- **Traffic Manager**: Schedules spots, manages inventory
- **Log Generator**: Creates and publishes daily logs
- **Billing Specialist**: Generates invoices and tracks payments
- **Administrator**: System configuration and user management

## Getting Help

1. **In-App Help**: Use the Help Center (`/help`) in the application
2. **User Manual**: Refer to [USER_MANUAL.md](./USER_MANUAL.md)
3. **Workflow Scenarios**: See [WORKFLOW_SCENARIOS.md](./WORKFLOW_SCENARIOS.md) for step-by-step guides
4. **Troubleshooting**: Each section includes troubleshooting tips
5. **System Administrator**: Contact your system administrator for system-level issues

## Contributing to Documentation

When updating documentation:

1. Keep screenshots up to date with UI changes
2. Update step-by-step instructions if workflows change
3. Add new scenarios for new features
4. Maintain consistent formatting
5. Test instructions with actual system

## Version Information

- **Documentation Version**: 1.0
- **Last Updated**: [Current Date]
- **LibreLog Version**: See application version in Settings

---

For questions or suggestions about the documentation, contact your system administrator or development team.

