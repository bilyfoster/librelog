# Product Requirements Document: Professional Clock Template Builder

## Introduction/Overview

The Clock Template Builder is the **genetic code** of the broadcast station, defining exactly what happens every second of an hour. This feature must compete with and exceed WideOrbit (WO), the industry standard for major-market stations, while solving the user-experience pain points that have frustrated WO users for years.

**Problem Statement:** Current clock template functionality only allows basic metadata entry (name, description, channel). Users cannot actually build or configure the clock structure, commercial breaks, timing rules, or element types. This prevents the system from functioning as a professional traffic management system.

**Goal:** Build a comprehensive, visual clock template builder that enables traffic managers to create, edit, and manage broadcast clocks with professional-grade features including commercial breaks, fixed assets, timing rules, and real-time validation, all while maintaining WideOrbit compatibility for future migration.

## Goals

1. Enable users to visually build and configure 60-minute clock templates with drag-and-drop interface
2. Support all WideOrbit-equivalent element types (commercial breaks, fixed assets, avail types, automation commands)
3. Implement timing rulesets (hard/soft starts, stretch-and-squeeze, fill rules)
4. Create template hierarchy (Master Clock → Dayparts → Grid)
5. Provide real-time validation and conflict detection
6. Integrate revenue/inventory analysis during clock building
7. Support cross-platform clocks (Linear + Digital/Stream overlays)
8. Enable seamless export to LibreTime format
9. Maintain WideOrbit compatibility for future migration

## User Stories

1. **As a Traffic Manager**, I want to visually build a clock template by dragging commercial breaks onto a timeline, so that I can quickly see how breaks affect the hour structure without manual time calculations.

2. **As a Traffic Manager**, I want to define fixed assets (like Legal IDs) that play at exact times, so that compliance requirements are automatically met.

3. **As a Traffic Manager**, I want to set avail types on commercial breaks, so that the auto-placer knows which types of ads are allowed in each break.

4. **As a Traffic Manager**, I want to configure hard vs. soft starts for elements, so that timing requirements are properly handled (e.g., Legal ID must play exactly at :00, while music can start when previous track ends).

5. **As a Traffic Manager**, I want to see real-time warnings when I create timing conflicts, so that I can fix issues before deploying the clock.

6. **As a Traffic Manager**, I want to see revenue impact analysis while building a clock, so that I can optimize inventory allocation based on current rates.

7. **As a Traffic Manager**, I want to create daypart-based clock assignments, so that different hours of the day can use different clock templates.

8. **As a Traffic Manager**, I want to configure digital stream overlays, so that online streams can have different break structures than the main broadcast.

9. **As a Traffic Manager**, I want to export clock templates to LibreTime format, so that logs can be pushed to the automation system.

10. **As a Traffic Manager**, I want to copy/clone existing clock templates, so that I can create variations without starting from scratch.

## Functional Requirements

### 1. Element Types (Building Blocks)

#### 1.1 Commercial Breaks (Stopsets)
- **FR-1.1.1:** The system must allow users to add commercial breaks to a clock template.
- **FR-1.1.2:** Each commercial break must have a start time (e.g., 15:00 past the hour).
- **FR-1.1.3:** Each commercial break must have a hard duration (e.g., 3 minutes).
- **FR-1.1.4:** Commercial breaks must support floating vs. fixed positioning.
- **FR-1.1.5:** Commercial breaks must support avail types (categorization tags like "Weather Sponsor Only", "Sports Content Only").
- **FR-1.1.6:** The system must prevent commercial breaks from overlapping.
- **FR-1.1.7:** The system must validate that all commercial breaks fit within the 60-minute hour.

#### 1.2 Fixed Assets (Static Carts)
- **FR-1.2.1:** The system must allow users to add fixed assets to a clock template.
- **FR-1.2.2:** Fixed assets must have an exact start time (e.g., Legal ID at :00).
- **FR-1.2.3:** Fixed assets must reference an audio file or asset identifier.
- **FR-1.2.4:** Fixed assets must support hard start timing (must play at exact time).
- **FR-1.2.5:** Common fixed asset types must be supported: Legal ID, News Intro, Station ID, etc.

#### 1.3 Avail Types (Soft Categorization)
- **FR-1.3.1:** The system must allow users to assign avail types to commercial breaks.
- **FR-1.3.2:** Avail types must be configurable (e.g., "Weather Sponsor Only", "Sports Content Only", "General", "Premium").
- **FR-1.3.3:** The system must store avail type assignments with each break for use by auto-placer logic.

#### 1.4 Automation Commands
- **FR-1.4.1:** The system must allow users to add automation commands to a clock template.
- **FR-1.4.2:** Automation commands must have a trigger time.
- **FR-1.4.3:** Common automation commands must be supported: "Switch to Satellite Feed", "Start Recording", "Enable Live Mix", etc.
- **FR-1.4.4:** The system must validate that high-priority automation commands are not placed too close together (within 30 seconds).

### 2. Timing Rulesets

#### 2.1 Hard vs. Soft Starts
- **FR-2.1.1:** The system must support "Hard Start" timing for elements that must trigger at exactly a specified time (e.g., 10:00:00 AM).
- **FR-2.1.2:** The system must support "Soft Start" timing for elements that trigger when the previous element ends (approximately at a time).
- **FR-2.1.3:** Each clock element must have a timing type (Hard/Soft) property.
- **FR-2.1.4:** Fixed assets (Legal IDs, News Intros) must default to Hard Start.
- **FR-2.1.5:** Music/content blocks should default to Soft Start.

#### 2.2 Stretch-and-Squeeze (Time Mapping)
- **FR-2.2.1:** The system must support time mapping logic to handle content that is slightly too long or too short.
- **FR-2.2.2:** When content exceeds available time, the system must calculate speed adjustment needed to fit.
- **FR-2.2.3:** The system must warn users when stretch/squeeze adjustments exceed acceptable thresholds (e.g., >5% speed change).

#### 2.3 Fill Rules
- **FR-2.3.1:** The system must allow users to configure fill rules for unsold inventory.
- **FR-2.3.2:** Fill rules must specify what plays when breaks are not fully sold (e.g., PSAs, Promos, Filler music).
- **FR-2.3.3:** Fill rules must be assignable to specific commercial breaks or apply globally to a clock.

### 3. Visual Clock Builder UI

#### 3.1 Timeline Visualization
- **FR-3.1.1:** The system must display a visual timeline representing the 60-minute hour.
- **FR-3.1.2:** The timeline must show all clock elements (breaks, fixed assets, automation commands) as visual blocks.
- **FR-3.1.3:** The timeline must support drag-and-drop functionality to move elements.
- **FR-3.1.4:** The timeline must show time markers (every 5 minutes recommended).
- **FR-3.1.5:** The timeline must visually distinguish between different element types (breaks, assets, commands) using color coding.

#### 3.2 Drag-and-Drop Interface
- **FR-3.2.1:** Users must be able to drag commercial breaks to reposition them on the timeline.
- **FR-3.2.2:** When dragging a break, the system must show real-time feedback on how the move affects other elements.
- **FR-3.2.3:** The system must prevent dropping breaks in invalid positions (overlapping, outside hour bounds).
- **FR-3.2.4:** Users must be able to resize break durations by dragging break edges.
- **FR-3.2.5:** The system must automatically adjust content block durations when breaks are moved or resized.

#### 3.3 Element Properties Panel
- **FR-3.3.1:** Clicking on a clock element must open a properties panel for editing.
- **FR-3.3.2:** The properties panel must allow editing of all element properties (name, start time, duration, timing type, avail types, etc.).
- **FR-3.3.3:** Changes in the properties panel must update the timeline visualization in real-time.

#### 3.4 Alternative Views
- **FR-3.4.1:** The system must provide a list/table view of all clock elements as an alternative to the timeline.
- **FR-3.4.2:** The system must support switching between timeline and list views.
- **FR-3.4.3:** Changes in either view must be synchronized with the other view.

### 4. Real-Time Validation and Conflict Detection

#### 4.1 Pre-Flight Validation
- **FR-4.1.1:** The system must validate clock templates in real-time as users build them.
- **FR-4.1.2:** The system must display validation warnings/errors immediately when issues are detected.
- **FR-4.1.3:** The system must prevent saving clock templates with critical errors (overlaps, timing conflicts).
- **FR-4.1.4:** The system must allow saving templates with warnings (non-critical issues).

#### 4.2 Conflict Detection
- **FR-4.2.1:** The system must detect when two high-priority automation commands are placed within 30 seconds of each other.
- **FR-4.2.2:** The system must detect overlapping commercial breaks.
- **FR-4.2.3:** The system must detect when fixed assets overlap with breaks or other elements.
- **FR-4.2.4:** The system must detect when total break time exceeds available inventory (e.g., >18 minutes per hour).

#### 4.3 Validation Messages
- **FR-4.3.1:** Validation errors must be displayed with clear, actionable messages.
- **FR-4.3.2:** Validation warnings must be displayed with explanations of potential issues.
- **FR-4.3.3:** The system must highlight problematic elements on the timeline.

### 5. Revenue and Inventory Analysis

#### 5.1 Real-Time Revenue Impact
- **FR-5.1.1:** While building a clock, the system must display a side panel showing revenue impact.
- **FR-5.1.2:** The revenue panel must calculate potential revenue based on current spot rates and break durations.
- **FR-5.1.3:** The system must show revenue changes when breaks are added, removed, or resized.
- **FR-5.1.4:** The system must display messages like "Reducing this break from 4 mins to 2 mins loses $400/hour in potential inventory."

#### 5.2 Inventory Summary
- **FR-5.2.1:** The system must display total commercial inventory (in minutes) for the clock.
- **FR-5.2.2:** The system must show breakdown by avail type (if configured).
- **FR-5.2.3:** The system must compare current clock inventory against station targets/goals.

### 6. Template Hierarchy

#### 6.1 Master Clock (60-Minute Template)
- **FR-6.1.1:** The system must support creating and managing 60-minute clock templates.
- **FR-6.1.2:** Each clock template must be associated with a channel.
- **FR-6.1.3:** Clock templates must have metadata: name, description, active status.

#### 6.2 Dayparts
- **FR-6.2.1:** The system must allow users to assign clock templates to dayparts.
- **FR-6.2.2:** A daypart (e.g., "Morning Drive 6 AM - 10 AM") must be able to use a specific clock template.
- **FR-6.2.3:** The system must support multiple dayparts using the same clock template.

#### 6.3 Grid (Weekly Schedule)
- **FR-6.3.1:** The system must support creating weekly grids that assign dayparts to specific days.
- **FR-6.3.2:** Grids must distinguish between weekday and weekend schedules.
- **FR-6.3.3:** The system must allow different daypart assignments for different days of the week.

### 7. Cross-Platform Support (Linear + Digital)

#### 7.1 Digital Stream Overlays
- **FR-7.1.1:** The system must support creating "shadow clocks" for digital streams.
- **FR-7.1.2:** Digital clocks must be able to reference a parent linear clock.
- **FR-7.1.3:** Digital clocks must allow different break structures (e.g., 2-minute ad break + 2-minute digital-only content instead of 4-minute broadcast break).
- **FR-7.1.4:** The system must support digital-only content blocks (weather updates, music, etc.).

#### 7.2 Unified Builder Interface
- **FR-7.2.1:** The clock builder must allow switching between Linear and Digital views.
- **FR-7.2.2:** Changes to linear clock should optionally propagate to digital clock (with user confirmation).
- **FR-7.2.3:** The system must clearly indicate which platform (Linear/Digital) is being edited.

### 8. Smart Segue and Transition Editors

#### 8.1 Transition Code Support
- **FR-8.1.1:** The system must support transition codes: Segue (S), Overlap (O), Hard Start (H).
- **FR-8.1.2:** Transition codes must be assignable to elements in the clock.
- **FR-8.1.3:** The system must provide visual indicators for transition types on the timeline.

#### 8.2 Crossfade Logic
- **FR-8.2.1:** The system must support configuring crossfade/ducking behavior between elements.
- **FR-8.2.2:** Users must be able to define how music "ducks" (lowers volume) when voice-overs or ads start.
- **FR-8.2.3:** Crossfade settings must be stored with clock templates.

### 9. Template Management

#### 9.1 CRUD Operations
- **FR-9.1.1:** Users must be able to create new clock templates.
- **FR-9.1.2:** Users must be able to view/edit existing clock templates.
- **FR-9.1.3:** Users must be able to delete clock templates (with confirmation if in use).
- **FR-9.1.4:** Users must be able to activate/deactivate clock templates.

#### 9.2 Copy/Clone Functionality
- **FR-9.2.1:** Users must be able to copy/clone existing clock templates.
- **FR-9.2.2:** Cloned templates must create a new template with all elements copied.
- **FR-9.2.3:** Cloned templates must have a default name indicating they are copies (e.g., "Morning Drive - Copy").

#### 9.3 Template Library
- **FR-9.3.1:** The system must provide a template library view showing all available clock templates.
- **FR-9.3.2:** Templates must be filterable by channel, active status, and searchable by name.
- **FR-9.3.3:** The system must show template usage (which dayparts/grids use each template).

### 10. WideOrbit Compatibility

#### 10.1 Track Categories and Asset Types
- **FR-10.1.1:** The system must support WideOrbit-style music categories: S1 (Power/Current), S2 (Secondary), S3 (New/Discovery).
- **FR-10.1.2:** The system must support WideOrbit-style asset types: IM (Imaging), ID (Legal ID), CM (Commercials), PR (Promos), VT (Voice Tracks), SH (Show/Longform).
- **FR-10.1.3:** Asset types must be assignable to clock elements where applicable.

#### 10.2 Show Segment Naming
- **FR-10.2.1:** The system must support WideOrbit-style show segment naming (e.g., `SH_MORNING_SEG1`, `SH_MORNING_SEG2`).
- **FR-10.2.2:** Show segments must be positionable at predictable times (e.g., :15, :35 past the hour).

#### 10.3 Data Export Format
- **FR-10.3.1:** Clock templates must be exportable in a format compatible with WideOrbit import.
- **FR-10.3.2:** The system must preserve all WideOrbit-compatible metadata during export.

### 11. LibreTime Integration

#### 11.1 Export to LibreTime
- **FR-11.1.1:** The system must support exporting clock templates to LibreTime format.
- **FR-11.1.2:** Export must convert clock structure to LibreTime's clock format.
- **FR-11.1.3:** Export must preserve break structures, timing, and element types.

#### 11.2 Log Generation
- **FR-11.2.1:** The system must generate daily logs from clock templates.
- **FR-11.2.2:** Generated logs must be pushable to LibreTime via API.
- **FR-11.2.3:** The system must support scheduled log generation and push.

#### 11.3 LibreTime API Client
- **FR-11.3.1:** The system must include a LibreTime API client for authentication and data transfer.
- **FR-11.3.2:** API client must support LibreTime's authentication mechanism (API key).
- **FR-11.3.3:** API client must handle errors and retries appropriately.

## Non-Goals (Out of Scope)

1. **Audio File Management:** This PRD does not include uploading, storing, or managing audio files. The system will reference audio assets by ID/name, but file management is handled elsewhere.

2. **Spot Scheduling:** This PRD focuses on clock template structure. Actual spot placement/scheduling within breaks is handled by a separate scheduling module.

3. **Revenue Calculation Engine:** While revenue impact analysis is included, detailed rate cards, pricing calculations, and billing are out of scope.

4. **Automation System Integration:** The system will generate commands and timing data, but direct integration with broadcast automation systems (beyond LibreTime) is out of scope for this phase.

5. **Multi-Station Management:** This PRD focuses on single-station clock building. Cross-station template sharing and management is out of scope.

6. **Historical Log Editing:** This PRD covers template building and log generation, but editing historical logs after they've been executed is out of scope.

7. **User Permissions/Roles:** Basic authentication is assumed. Detailed role-based permissions for clock editing (e.g., "Traffic Manager can edit, DJ can only view") is out of scope for this phase.

## Design Considerations

### UI/UX Requirements

1. **Timeline Visualization:**
   - Use a horizontal timeline representing 0-60 minutes
   - Color code elements: Commercial breaks (red/orange), Fixed assets (blue), Automation commands (yellow), Content blocks (gray/green)
   - Show time markers every 5 minutes
   - Support zoom in/out for detailed editing

2. **Drag-and-Drop:**
   - Provide visual feedback during drag (ghost element, drop zones)
   - Show snap-to-grid for common break times (:00, :15, :30, :45)
   - Display tooltips showing exact time and duration while dragging

3. **Properties Panel:**
   - Slide-out panel from right side
   - Form-based editing with validation
   - Real-time preview of changes on timeline

4. **Responsive Design:**
   - Timeline should be scrollable horizontally on smaller screens
   - Properties panel should be collapsible/modal on mobile
   - Touch-friendly drag-and-drop for tablet users

### Visual Design Guidelines

- Follow existing dashboard design system (colors, fonts, spacing)
- Use consistent iconography for element types
- Provide clear visual hierarchy (timeline prominent, properties secondary)
- Use status colors for validation (green=valid, yellow=warning, red=error)

## Technical Considerations

### Backend Architecture

1. **Data Model:**
   - Extend `ClockTemplate` entity to include all element types
   - Create `BreakStructure` entity (exists, may need enhancements)
   - Create `FixedAsset` entity for static carts
   - Create `AutomationCommand` entity
   - Create `AvailType` entity/enum
   - Create `DaypartAssignment` entity for template hierarchy
   - Create `Grid` entity for weekly schedules

2. **Services:**
   - `ClockBuilderService`: Core logic for building/validating clocks
   - `BreakStructureService`: CRUD for commercial breaks
   - `ClockValidationService`: Real-time validation and conflict detection
   - `RevenueAnalysisService`: Calculate revenue impact
   - `LibreTimeSyncService`: Export and sync to LibreTime
   - `ClockTemplateService`: Enhanced CRUD with template management features

3. **API Endpoints:**
   - `GET /api/clock-templates/{id}/structure` - Get full clock with all elements
   - `POST /api/clock-templates/{id}/breaks` - Add commercial break
   - `PUT /api/clock-templates/{id}/breaks/{breakId}` - Update break
   - `DELETE /api/clock-templates/{id}/breaks/{breakId}` - Remove break
   - `POST /api/clock-templates/{id}/validate` - Validate clock structure
   - `POST /api/clock-templates/{id}/clone` - Clone template
   - `GET /api/clock-templates/{id}/revenue-analysis` - Get revenue impact
   - `POST /api/clock-templates/{id}/export/libretime` - Export to LibreTime

### Frontend Architecture

1. **Components:**
   - `ClockTimeline`: Main visual timeline component
   - `BreakElement`: Draggable break block
   - `FixedAssetElement`: Fixed asset marker
   - `PropertiesPanel`: Element editing panel
   - `ValidationPanel`: Real-time validation display
   - `RevenuePanel`: Revenue impact analysis

2. **State Management:**
   - Use JavaScript/vanilla JS (no framework requirement per coding guidelines)
   - Maintain clock state in memory during editing
   - Sync with backend on save
   - Handle optimistic updates for drag-and-drop

3. **Libraries:**
   - Consider lightweight drag-and-drop library (e.g., interact.js, dragula)
   - Use Canvas or SVG for timeline rendering (performance for many elements)
   - Consider D3.js for advanced timeline visualization (optional)

### Database Considerations

1. **Schema Updates:**
   - Enhance `break_structures` table with avail_type, timing_type fields
   - Create `fixed_assets` table
   - Create `automation_commands` table
   - Create `avail_types` table/enum
   - Create `daypart_assignments` table
   - Create `grids` table

2. **Performance:**
   - Index clock_template_id on all element tables
   - Consider JSON column for complex element properties (PostgreSQL JSONB)
   - Cache frequently accessed templates

### Integration Points

1. **LibreTime API:**
   - Research LibreTime clock format and API endpoints
   - Implement authentication (API key from config)
   - Handle rate limiting and retries
   - Support both push (real-time) and export (manual) modes

2. **Existing Modules:**
   - Integrate with Channel management (clocks are channel-specific)
   - Integrate with Daypart management (daypart assignments)
   - Integrate with Order management (for revenue analysis, if available)

## Success Metrics

1. **Functionality:**
   - 100% of WideOrbit-equivalent features implemented
   - Zero critical validation errors in production clocks
   - <2 second response time for timeline updates during drag-and-drop

2. **User Experience:**
   - Users can build a complete clock template in <10 minutes (vs. 30+ minutes in WideOrbit)
   - 90%+ user satisfaction with drag-and-drop interface
   - <5% of clock templates require manual corrections after validation

3. **Integration:**
   - 100% successful export rate to LibreTime format
   - <1% data loss during LibreTime sync
   - Real-time validation catches 95%+ of conflicts before save

4. **Adoption:**
   - All active channels have at least one clock template within 30 days of launch
   - Average of 3+ clock templates per channel (indicating active use)

## Open Questions

1. **Audio Asset Management:** How are audio files/assets referenced? Do we need to integrate with an existing asset management system, or will assets be referenced by name/ID only?

2. **Rate Card Integration:** For revenue analysis, do we have rate card data available, or should this be a future enhancement?

3. **Automation System Compatibility:** Beyond LibreTime, are there other automation systems we need to support (e.g., ENCO, RadioDJ)? Should we design for extensibility?

4. **Template Versioning:** Should we implement version history for clock templates (ability to roll back to previous versions)? This is a common enterprise feature.

5. **Multi-Hour Clocks:** Should we support clocks that span multiple hours (e.g., a 2-hour morning show template), or keep it strictly 60-minute templates?

6. **Break Content Assignment:** Should users be able to assign specific spots/content to breaks within the clock builder, or is this purely structural (content assignment happens later)?

7. **Real-Time Sync:** Should changes to clock templates automatically push to LibreTime, or is manual export sufficient for Phase 1?

8. **Digital Stream Priority:** For digital overlays, should digital clocks be able to override linear clocks completely, or always reference a parent linear clock?

---

## Appendix: WideOrbit Compatibility Reference

### Music Categories (Rotation)
- **S1 (Power/Current):** Hottest new tracks, play every 3-4 hours
- **S2 (Secondary):** New tracks that aren't "hits" yet, play every 6-8 hours  
- **S3 (New/Discovery):** Brand new uploads being tested

### Asset Types (Non-Music)
- **IM (Imaging):** Sweepers and Stingers
- **ID (Legal ID):** Top-of-hour legal identification
- **CM (Commercials/Spots):** Paid ads
- **PR (Promos):** Internal station advertisements
- **VT (Voice Tracks):** DJ talk segments
- **SH (Show/Longform):** 5-minute interview segments

### Transition Codes
- **Segue (S):** Next track starts immediately as first ends
- **Overlap (O):** Next track starts while first is fading (for Liners over song intros)
- **Hard Start (H):** Forces track to play at exact time (for Legal IDs)

### Show Segment Naming Convention
- Format: `SH_MORNING_SEG1`, `SH_MORNING_SEG2`, `SH_MORNING_SEG3`
- Creates predictable, major-market structure with segments at :15, :35 past the hour

