# Task List: Remove WebAwesome and Replace with Material-UI

Based on PRD: `prd-remove-webawesome-replace-material-ui.md`

## Relevant Files

- `frontend/package.json` - Remove WebAwesome dependency, upgrade Material-UI
- `frontend/index.html` - Remove WebAwesome CDN script if present
- `frontend/src/main.tsx` - Remove WebAwesome imports, add Material-UI ThemeProvider
- `frontend/src/theme/` - New directory for Material-UI theme configuration
- `frontend/src/theme/theme.ts` - Material-UI theme configuration with light/dark mode
- `frontend/src/components/Layout.tsx` - Migrate layout components to Material-UI
- `frontend/src/pages/Dashboard.tsx` - Migrate dashboard to Material-UI
- `frontend/src/pages/clocks/ClockBuilder.tsx` - Migrate clock builder to Material-UI
- `frontend/src/components/clocks/HourlyTemplateBuilder.tsx` - Migrate to Material-UI
- `frontend/src/components/clocks/DailyTemplateBuilder.tsx` - Migrate to Material-UI
- `frontend/src/pages/traffic/Orders.tsx` - Migrate orders page to Material-UI
- `frontend/src/components/orders/OrderForm.tsx` - Migrate order form to Material-UI
- `frontend/src/components/webawesome/` - Directory to be deleted (WebAwesomeProvider.tsx, ComponentMap.tsx, components.ts)
- `frontend/src/types/webawesome.d.ts` - Type definitions to be deleted
- `frontend/src/styles/webawesome-theme.css` - Styles to be migrated or deleted
- `frontend/src/index.css` - Update to remove WebAwesome references
- All component files using WebAwesome components - Migrate to Material-UI
- All test files referencing WebAwesome - Update to use Material-UI test utilities
- `docs/migrations/WEB_AWESOME_*.md` - Documentation files to be archived/removed

### Notes

- Unit tests should be placed in the `tests/` directory mirroring the backend structure (e.g., `backend/routers/auth.py` → `tests/routers/test_auth.py`).
- Frontend tests should be placed alongside components (e.g., `MyComponent.tsx` and `MyComponent.test.tsx` in the same directory).
- Use `pytest [optional/path/to/test/file]` to run backend tests. Running without a path executes all tests found by pytest.
- Use `npm test` or `vitest` to run frontend tests.
- Follow the task completion protocol in `markdown/process-task-list.md`: complete one sub-task at a time, mark as complete, commit after parent task completion.

## Tasks

- [x] 1.0 Phase 1: Upgrade Material-UI and Remove WebAwesome Dependencies
  - [x] 1.1 Check latest stable version of Material-UI (v6 if available, otherwise latest v5.x) and verify compatibility with React 18
  - [x] 1.2 Update `frontend/package.json` to upgrade `@mui/material` and `@mui/icons-material` to latest stable version
  - [x] 1.3 Remove `@awesome.me/webawesome` package from `frontend/package.json` dependencies
  - [x] 1.4 Verify `@emotion/react` and `@emotion/styled` versions are compatible with upgraded MUI version, update if needed
  - [x] 1.5 Check `frontend/index.html` for WebAwesome CDN script tag and remove it if present
  - [x] 1.6 Remove `wa-theme-default` class from `<html>` tag in `frontend/index.html`
  - [x] 1.7 Run `npm install` to update dependencies and remove WebAwesome package
  - [x] 1.8 Verify build succeeds with `npm run build` after dependency changes

- [x] 2.0 Phase 2: Create Material-UI Theme Configuration
  - [x] 2.1 Create `frontend/src/theme/` directory
  - [x] 2.2 Create `frontend/src/theme/theme.ts` file with Material-UI theme configuration
  - [x] 2.3 Extract module colors from `Layout.tsx` and add to theme configuration (dashboard, library, clocks, traffic, production, logs, billing, analytics, admin, reports, help)
  - [x] 2.4 Configure light mode theme with preserved color palette, typography, and spacing
  - [x] 2.5 Configure dark mode theme variant with appropriate color adjustments
  - [x] 2.6 Preserve existing typography settings (font family, sizes, weights) in theme
  - [x] 2.7 Preserve existing spacing scale in theme configuration
  - [x] 2.8 Create theme utility functions for module colors if needed
  - [x] 2.9 Test theme configuration by importing and using in a test component

- [x] 3.0 Phase 3: Migrate Core Layout Components
  - [x] 3.1 Update `frontend/src/main.tsx` to remove WebAwesome imports (WebAwesomeProvider, webawesome CSS imports, components registration)
  - [x] 3.2 Add Material-UI ThemeProvider import to `frontend/src/main.tsx`
  - [x] 3.3 Wrap application with Material-UI ThemeProvider in `frontend/src/main.tsx`, replacing WebAwesomeProvider
  - [x] 3.4 Update `frontend/src/components/Layout.tsx` to remove WebAwesome component usage and replace with Material-UI components
  - [x] 3.5 Replace WebAwesome drawer/navigation components with MUI Drawer, List, ListItem, ListItemButton, ListItemIcon, ListItemText
  - [x] 3.6 Replace WebAwesome header/app bar with MUI AppBar, Toolbar, IconButton
  - [x] 3.7 Migrate navigation menu items to use MUI components while preserving module color system
  - [x] 3.8 Update responsive behavior (mobile drawer, desktop permanent drawer) using MUI breakpoints
  - [x] 3.9 Preserve all navigation functionality (active state highlighting, module colors, hover effects)
  - [x] 3.10 Test layout renders correctly and navigation works in both mobile and desktop views

- [x] 4.0 Phase 4: Migrate Dashboard and Navigation
  - [x] 4.1 Update `frontend/src/pages/Dashboard.tsx` to remove WebAwesome components
  - [x] 4.2 Replace WebAwesome components in Dashboard with MUI equivalents (Card, Grid, Typography, Button, etc.)
  - [x] 4.3 Preserve all dashboard functionality and data display
  - [x] 4.4 Update navigation menu icon rendering to use MUI icons (temporary - will be fully migrated in Phase 9)
  - [x] 4.5 Test dashboard page loads and displays correctly
  - [x] 4.6 Verify all dashboard interactions work (clicking cards, navigation, etc.)

- [x] 5.0 Phase 5: Migrate Clock Management Components
  - [x] 5.1 Update `frontend/src/pages/clocks/ClockBuilder.tsx` to remove WebAwesome components
  - [x] 5.2 Replace WebAwesome components in ClockBuilder with MUI equivalents (Button, TextField, Select, Dialog, Card, Tabs, etc.)
  - [x] 5.3 Update `frontend/src/components/clocks/HourlyTemplateBuilder.tsx` to use MUI components
  - [x] 5.4 Update `frontend/src/components/clocks/DailyTemplateBuilder.tsx` to use MUI components
  - [x] 5.5 Preserve all clock builder functionality (template creation, editing, saving, validation)
  - [x] 5.6 Test clock builder page loads and all features work correctly
  - [x] 5.7 Verify template creation and editing workflows function properly

- [x] 6.0 Phase 6: Migrate Order Management Components
  - [x] 6.1 Update `frontend/src/pages/traffic/Orders.tsx` to remove WebAwesome components
  - [x] 6.2 Replace WebAwesome components in Orders page with MUI equivalents (Table, TableRow, TableCell, Button, Dialog, TextField, etc.)
  - [x] 6.3 Update `frontend/src/components/orders/OrderForm.tsx` to remove WebAwesome components (this is a large component - migrate systematically)
  - [x] 6.4 Replace WebAwesome form components in OrderForm with MUI form components (TextField, Select, Checkbox, Radio, DatePicker if using MUI X, etc.)
  - [x] 6.5 Preserve all order form functionality (validation, field dependencies, submission, etc.)
  - [x] 6.6 Test orders page loads and displays order list correctly
  - [x] 6.7 Test order creation and editing workflows function properly
  - [x] 6.8 Verify all form validations work correctly

- [x] 7.0 Phase 7: Migrate Remaining Page Components
  - [x] 7.1 Identify all page components in `frontend/src/pages/` that use WebAwesome components
  - [x] 7.2 Migrate traffic management pages: Advertisers.tsx, Agencies.tsx, SalesReps.tsx, Dayparts.tsx, DaypartCategories.tsx, RotationRules.tsx, TrafficLogs.tsx, CopyLibrary.tsx, TrafficManager.tsx
  - [x] 7.3 Migrate production pages: ProductionOrders.tsx, ProducerDashboard.tsx, VoiceTalentPortal.tsx, ProductionArchive.tsx, ProductionOrderDetail.tsx
  - [x] 7.4 Migrate billing pages: Invoices.tsx, Payments.tsx, Makegoods.tsx
  - [x] 7.5 Migrate library pages: LibraryList.tsx, MusicManager.tsx, SpotsLibrary.tsx
  - [x] 7.6 Migrate log management pages: LogEditor.tsx, LogGenerator.tsx
  - [x] 7.7 Migrate voice pages: VoiceRecorder.tsx, VoiceTracksManager.tsx
  - [x] 7.8 Migrate analytics pages: InventoryDashboard.tsx, RevenueDashboard.tsx, SalesGoals.tsx
  - [x] 7.9 Migrate admin pages: Settings.tsx, Users.tsx, Stations.tsx, Clusters.tsx, AuditLogs.tsx, Backups.tsx, Notifications.tsx, Webhooks.tsx, SalesOffices.tsx, SalesRegions.tsx, SalesTeams.tsx
  - [x] 7.10 Migrate reports page: ReportsHub.tsx
  - [x] 7.11 Migrate auth page: Login.tsx
  - [x] 7.12 Migrate Setup.tsx and Profile.tsx pages
  - [x] 7.13 For each page, preserve all functionality and test after migration
  - [x] 7.14 Verify all pages load and function correctly

- [x] 8.0 Phase 8: Migrate Shared Components
  - [x] 8.1 Identify all shared components in `frontend/src/components/` that use WebAwesome components
  - [x] 8.2 Migrate billing components: InvoiceFormDialog.tsx, InvoiceDetailDialog.tsx, PaymentFormDialog.tsx, MakegoodFormDialog.tsx, ARAgingDashboard.tsx
  - [x] 8.3 Migrate copy components: CopyAssignment.tsx, CopyDetailDialog.tsx, CopyUpload.tsx, ExpiringCopyAlert.tsx
  - [x] 8.4 Migrate orders components: OrderTemplates.tsx
  - [x] 8.5 Migrate production components: ProductionOrderFormDialog.tsx
  - [x] 8.6 Migrate tracks components: TrackEditDialog.tsx, TrackPlayDialog.tsx
  - [x] 8.7 Migrate voice components: SharedVoiceRecorder.tsx, AudioTrimmer.tsx, WaveformDisplay.tsx, PreviewPanels.tsx, TimingDisplay.tsx
  - [x] 8.8 Migrate clocks components: (already done in Phase 5, verify)
  - [x] 8.9 Migrate analytics components: InventoryHeatmap.tsx, SalesGoalFormDialog.tsx
  - [x] 8.10 Migrate collaboration components: ActiveUsers.tsx
  - [x] 8.11 Migrate notifications component: NotificationBell.tsx
  - [x] 8.12 Migrate reports components: AvailsReport.tsx
  - [x] 8.13 Migrate scheduling components: ConflictResolver.tsx, SpotScheduler.tsx
  - [x] 8.14 Migrate Help components: HelpCenter.tsx, HelpModal.tsx, InfoButton.tsx, TerminologyDictionary.tsx
  - [x] 8.15 Migrate audio components: AudioPlayer.tsx, MultiCutManager.tsx
  - [x] 8.16 Update ErrorBoundary.tsx, ProtectedRoute.tsx, APIDiagnostics.tsx if they use WebAwesome
  - [x] 8.17 For each component, preserve all functionality and test after migration

- [x] 9.0 Phase 9: Replace All Icons with Material-UI Icons
  - [x] 9.1 Search codebase for all FontAwesome icon references (fa-solid, fa-regular, fa-brands patterns)
  - [x] 9.2 Create icon mapping document or utility function mapping FontAwesome icon names to MUI icon components
  - [x] 9.3 Update Layout.tsx navigation menu icons to use MUI icons from @mui/icons-material
  - [x] 9.4 Update all page components to replace FontAwesome icons with MUI icons
  - [x] 9.5 Update all shared components to replace FontAwesome icons with MUI icons
  - [x] 9.6 Ensure icon sizes match existing design (MUI icons default to 24px, adjust with fontSize prop if needed)
  - [x] 9.7 Verify icon colors and styling match existing design (use sx prop or theme colors)
  - [x] 9.8 Test all icons display correctly and maintain visual consistency
  - [x] 9.9 Remove all FontAwesome icon imports and references

- [x] 10.0 Phase 10: Remove WebAwesome Files and Cleanup Code
  - [x] 10.1 Delete `frontend/src/components/webawesome/` directory and all its contents (WebAwesomeProvider.tsx, ComponentMap.tsx, components.ts)
  - [x] 10.2 Delete `frontend/src/types/webawesome.d.ts` if it exists
  - [x] 10.3 Review `frontend/src/styles/webawesome-theme.css` and migrate any necessary styles to Material-UI theme, then delete the file
  - [x] 10.4 Remove all WebAwesome imports from `frontend/src/main.tsx` (verify none remain)
  - [x] 10.5 Search codebase for any remaining `@awesome.me/webawesome` imports and remove them
  - [x] 10.6 Search codebase for any remaining `webawesome` or `WebAwesome` string references and remove/update them
  - [x] 10.7 Remove console error suppression code related to FontAwesome 403 errors (from WebAwesomeProvider and api.ts)
  - [x] 10.8 Update `frontend/src/index.css` to remove WebAwesome-related CSS imports or references
  - [x] 10.9 Remove any WebAwesome component registration code
  - [x] 10.10 Search for `wa-` CSS class prefixes and remove or migrate to MUI equivalents
  - [x] 10.11 Verify no WebAwesome references remain in codebase using grep/search
  - [x] 10.12 Run build to verify no WebAwesome-related errors

- [x] 11.0 Phase 11: Update All Tests
  - [x] 11.1 Identify all test files that reference WebAwesome components or utilities
  - [x] 11.2 Update test setup files to use Material-UI test utilities instead of WebAwesome
  - [x] 11.3 Update unit tests for Layout component to use MUI test utilities
  - [x] 11.4 Update unit tests for Dashboard page to use MUI test utilities
  - [x] 11.5 Update unit tests for ClockBuilder and related components to use MUI test utilities
  - [x] 11.6 Update unit tests for Orders page and OrderForm to use MUI test utilities
  - [x] 11.7 Update unit tests for all migrated page components to use MUI test utilities
  - [x] 11.8 Update unit tests for all migrated shared components to use MUI test utilities
  - [x] 11.9 Update integration tests to work with Material-UI components
  - [x] 11.10 Update test utilities and helpers if they reference WebAwesome
  - [ ] 11.11 Run full test suite with `npm test` and fix any failing tests
  - [ ] 11.12 Verify all tests pass (100% pass rate goal)

- [ ] 12.0 Phase 12: Update Documentation and Final Verification
  - [x] 12.1 Archive or remove `docs/migrations/WEB_AWESOME_MIGRATION_STATUS.md`
  - [x] 12.2 Archive or remove `docs/migrations/WEB_AWESOME_SETUP_COMPLETE.md`
  - [x] 12.3 Archive or remove `docs/migrations/WEB_AWESOME_FIXES_APPLIED.md`
  - [x] 12.4 Archive or remove `docs/migrations/WEB_AWESOME_NPM_MIGRATION.md`
  - [x] 12.5 Update any README files that reference WebAwesome (verified - no references found)
  - [x] 12.6 Document Material-UI theme configuration in appropriate documentation file
  - [x] 12.7 Verify zero WebAwesome dependencies in package.json
  - [x] 12.8 Verify zero WebAwesome files exist in codebase
  - [x] 12.9 Verify zero console errors related to FontAwesome/WebAwesome
  - [ ] 12.10 Run full application and verify all pages load correctly
  - [ ] 12.11 Test critical user workflows (login, navigation, order creation, clock building, etc.)
  - [ ] 12.12 Verify visual parity with screenshots or visual comparison
  - [ ] 12.13 Verify bundle size reduction (compare before/after bundle sizes)
  - [ ] 12.14 Verify build succeeds without warnings: `npm run build`
  - [ ] 12.15 Verify application runs without runtime errors: `npm run dev`
  - [x] 12.16 Create migration completion summary document

