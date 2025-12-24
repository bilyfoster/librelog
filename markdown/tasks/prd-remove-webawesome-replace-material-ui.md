# Product Requirements Document: Remove WebAwesome and Replace with Material-UI

## Introduction/Overview

The LibreLog frontend currently uses WebAwesome (@awesome.me/webawesome) as its primary component library. However, this library is causing several issues:

1. **Authentication Errors**: WebAwesome attempts to fetch icons from FontAwesome CDN, resulting in 403 errors that require console error suppression workarounds
2. **Licensing Issues**: FontAwesome Pro authentication requirements create deployment complications
3. **Inconsistency**: The codebase has both WebAwesome and Material-UI installed, leading to inconsistent UI patterns
4. **Bundle Size**: Maintaining multiple UI libraries increases bundle size unnecessarily

This PRD outlines the complete removal of WebAwesome and migration to Material-UI (MUI) as the sole component library. The goal is to standardize the UI framework, eliminate authentication errors, reduce bundle size, and improve maintainability while preserving all existing functionality and design.

## Goals

1. **Complete Removal**: Remove all WebAwesome dependencies, code, and files from the codebase
2. **Standardization**: Establish Material-UI as the single, standardized component library
3. **Error Elimination**: Remove all FontAwesome 403 errors and related error suppression code
4. **Bundle Optimization**: Reduce bundle size by removing WebAwesome dependencies
5. **Functionality Preservation**: Maintain 100% feature parity with existing functionality
6. **Design Preservation**: Preserve current visual design, color scheme, and module color system
7. **Theme Support**: Maintain light/dark mode support using Material-UI theming
8. **Testing**: Update all existing tests to work with Material-UI components

## User Stories

1. **As a developer**, I want a single, consistent UI library so that I can maintain and extend the codebase more easily
2. **As a developer**, I want to eliminate console errors so that debugging is more effective
3. **As a user**, I want the application to look and function exactly as it does now, so that my workflow is not disrupted
4. **As a developer**, I want to use Material-UI's comprehensive component library so that I have access to well-documented, maintained components
5. **As a system administrator**, I want to reduce bundle size so that the application loads faster
6. **As a developer**, I want updated tests so that I can verify the migration was successful

## Functional Requirements

### 1. Dependency Management
1. The system must remove `@awesome.me/webawesome` package from `package.json`
2. The system must upgrade `@mui/material` and `@mui/icons-material` to the latest stable version (v6 if available, otherwise latest v5)
3. The system must remove WebAwesome-related npm dependencies
4. The system must remove WebAwesome CDN script tag from `index.html` if present
5. The system must ensure all Material-UI peer dependencies are properly installed

### 2. Component Migration
6. The system must replace all WebAwesome components with Material-UI equivalents:
   - WebAwesome buttons → MUI Button
   - WebAwesome inputs → MUI TextField
   - WebAwesome selects → MUI Select
   - WebAwesome dialogs → MUI Dialog
   - WebAwesome cards → MUI Card
   - WebAwesome tabs → MUI Tabs
   - WebAwesome badges → MUI Badge
   - WebAwesome spinners → MUI CircularProgress
   - WebAwesome avatars → MUI Avatar
   - WebAwesome callouts → MUI Alert
   - WebAwesome details → MUI Accordion or Collapse
   - WebAwesome checkboxes → MUI Checkbox
   - WebAwesome textareas → MUI TextField (multiline)
7. The system must migrate all components in the following pages/components:
   - Layout component (navigation, header, drawer)
   - Dashboard page
   - ClockBuilder page and related components (HourlyTemplateBuilder, DailyTemplateBuilder)
   - Orders page and OrderForm component
   - All pages listed in WebAwesome migration status document
   - All components in `frontend/src/components/` that use WebAwesome
8. The system must preserve all existing component functionality during migration
9. The system must maintain all existing props, event handlers, and behaviors

### 3. Icon System Migration
10. The system must replace all WebAwesome/FontAwesome icons with Material-UI icons from `@mui/icons-material`
11. The system must map icon names appropriately (e.g., `fa-solid fa-user` → `Person` from `@mui/icons-material`)
12. The system must ensure icon sizes and styling match existing design
13. The system must remove all FontAwesome icon references

### 4. Theme and Styling
14. The system must create a Material-UI theme that matches the current design system
15. The system must preserve the module color system (traffic, production, billing, etc.)
16. The system must implement light and dark mode support using Material-UI ThemeProvider
17. The system must preserve existing spacing, typography, and color schemes
18. The system must migrate all custom CSS that was specific to WebAwesome
19. The system must ensure responsive design is maintained

### 5. File Removal
20. The system must delete the `frontend/src/components/webawesome/` directory and all its contents:
   - WebAwesomeProvider.tsx
   - ComponentMap.tsx
   - components.ts
21. The system must delete `frontend/src/types/webawesome.d.ts` if it exists
22. The system must delete `frontend/src/styles/webawesome-theme.css` or migrate its styles to Material-UI theme
23. The system must remove all WebAwesome imports from `main.tsx`
24. The system must remove WebAwesome-related error suppression code from components

### 6. Code Cleanup
25. The system must remove all imports of `@awesome.me/webawesome` packages
26. The system must remove all WebAwesome component registrations
27. The system must remove console error suppression code related to FontAwesome 403 errors
28. The system must update all component files to use Material-UI imports
29. The system must remove WebAwesome-specific type definitions

### 7. Testing
30. The system must update all existing unit tests to work with Material-UI components
31. The system must update all integration tests to use Material-UI test utilities
32. The system must ensure all tests pass after migration
33. The system must update test setup files if they reference WebAwesome

### 8. Documentation
34. The system must update any documentation that references WebAwesome
35. The system must remove or update migration status documents related to WebAwesome
36. The system must document the Material-UI theme configuration

## Non-Goals (Out of Scope)

1. **UI Redesign**: This migration is purely a component library replacement. No visual redesigns, layout changes, or UX improvements are included.
2. **New Features**: No new Material-UI features will be added that were not present in the WebAwesome implementation.
3. **Functionality Changes**: No changes to business logic, workflows, or application functionality.
4. **Performance Optimization**: While bundle size reduction is a goal, this migration does not include broader performance optimization efforts beyond removing WebAwesome.
5. **Accessibility Improvements**: While Material-UI has better accessibility, this migration does not include comprehensive accessibility audits or improvements beyond what Material-UI provides by default.
6. **Component Customization**: Deep customization of Material-UI components beyond theme configuration is out of scope unless required to match existing design.

## Design Considerations

### Theme Configuration
- Create a custom Material-UI theme in `frontend/src/theme/` directory
- Preserve existing color palette and module colors
- Configure light and dark mode variants
- Maintain existing typography settings
- Preserve spacing scale

### Component Mapping
- Use Material-UI's component API documentation to find equivalent components
- Maintain existing component props and behaviors where possible
- Use Material-UI's `sx` prop or `styled` API for custom styling when needed
- Preserve responsive breakpoints and behavior

### Icon Migration
- Create an icon mapping document or utility if needed
- Use Material-UI's icon library which is comprehensive and well-maintained
- Ensure icon sizes match existing design (Material-UI icons are typically 24px by default)

## Technical Considerations

### Dependencies
- **Current State**: Both `@awesome.me/webawesome` (v3.0.0) and `@mui/material` (v5.15.0) are installed
- **Target State**: Remove WebAwesome, upgrade Material-UI to latest stable version (v6 if available, otherwise latest v5.x)
- **Peer Dependencies**: Ensure `@emotion/react` and `@emotion/styled` are compatible with upgraded MUI version

### Migration Strategy
1. **Phase 1**: Set up Material-UI theme and ThemeProvider
2. **Phase 2**: Migrate core layout components (Layout, Navigation, Header)
3. **Phase 3**: Migrate page components systematically
4. **Phase 4**: Migrate shared components
5. **Phase 5**: Remove WebAwesome dependencies and files
6. **Phase 6**: Update tests
7. **Phase 7**: Final cleanup and verification

### Files to Modify
- `frontend/package.json` - Remove WebAwesome, upgrade MUI
- `frontend/src/main.tsx` - Remove WebAwesome imports, add MUI ThemeProvider
- `frontend/index.html` - Remove WebAwesome CDN script if present
- All component files using WebAwesome components
- All test files referencing WebAwesome
- Theme/styling files

### Files to Delete
- `frontend/src/components/webawesome/` (entire directory)
- `frontend/src/types/webawesome.d.ts`
- `frontend/src/styles/webawesome-theme.css` (or migrate to MUI theme)
- Any WebAwesome migration documentation that is no longer relevant

### Potential Challenges
1. **Component API Differences**: WebAwesome and Material-UI have different APIs - careful mapping required
2. **Styling Differences**: May need custom styling to match existing design exactly
3. **Icon Mapping**: Need to find appropriate Material-UI icon equivalents
4. **Theme Complexity**: Preserving module color system may require custom theme configuration
5. **Test Updates**: All tests using WebAwesome components need updates

## Success Metrics

1. **Zero WebAwesome Dependencies**: `package.json` contains no WebAwesome packages
2. **Zero WebAwesome Files**: No WebAwesome-related files exist in the codebase
3. **Zero Console Errors**: No FontAwesome 403 errors in browser console
4. **100% Test Pass Rate**: All existing tests pass with Material-UI components
5. **Visual Parity**: Application looks identical to current implementation (screenshot comparison)
6. **Functionality Parity**: All features work exactly as before migration
7. **Bundle Size Reduction**: Measurable reduction in bundle size (target: 10-20% reduction)
8. **Build Success**: Application builds without errors or warnings related to WebAwesome
9. **Runtime Success**: Application runs without WebAwesome-related runtime errors

## Open Questions

1. **Material-UI Version**: Should we upgrade to v6 if available, or stay on v5? (Answer: Upgrade to latest stable, v6 if available)
2. **Theme Migration**: Should we create a comprehensive theme file or migrate styles incrementally? (To be determined during implementation)
3. **Icon Mapping**: Do we need a utility function to map old icon names, or can we do direct replacements? (Likely direct replacements during component migration)
4. **Migration Order**: Should we migrate high-traffic pages first or start with simpler components? (Recommend starting with Layout/Navigation, then pages)
5. **Testing Strategy**: Should we update tests incrementally or all at once? (Recommend incremental updates alongside component migration)

## Target Audience

This PRD is written for junior to mid-level developers who will implement the migration. Requirements are explicit and actionable, with clear component mappings and file locations specified.

## Related Documents

- `docs/migrations/WEB_AWESOME_MIGRATION_STATUS.md` - Current migration status (to be updated/removed)
- `docs/migrations/WEB_AWESOME_SETUP_COMPLETE.md` - Setup documentation (to be archived)
- `docs/migrations/WEB_AWESOME_FIXES_APPLIED.md` - Fix documentation (to be archived)
- `markdown/coding_guidelines.md` - Coding standards to follow during migration
- Material-UI Documentation: https://mui.com/material-ui/getting-started/

