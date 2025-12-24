# Track Type Color Coding System

## Overview

All audio track types in LibreLog now have distinct visual colors to help users quickly identify different types of audio files throughout the system.

## Color Scheme

Each track type has been assigned a unique color with associated background and text colors:

| Type | Label | Color | Background | Use Case |
|------|-------|-------|------------|----------|
| **MUS** | Music | Blue (#1976d2) | Light Blue | Music tracks |
| **ADV** | Advertisement | Red (#d32f2f) | Light Red | Commercial advertisements |
| **PSA** | Public Service Announcement | Green (#388e3c) | Light Green | PSAs |
| **LIN** | Liner | Orange (#f57c00) | Light Orange | Station liners |
| **INT** | Interview | Purple (#7b1fa2) | Light Purple | Interview content |
| **PRO** | Promo | Pink (#c2185b) | Light Pink | Promotional content |
| **SHO** | Show | Teal (#00796b) | Light Teal | Show segments |
| **IDS** | ID/Station ID | Brown (#5d4037) | Light Brown | Station IDs |
| **COM** | Commercial | Light Blue (#0288d1) | Very Light Blue | Commercials |
| **NEW** | News | Yellow/Amber (#fbc02d) | Light Yellow | News segments |
| **VOT** | Voice Over Track | Grey (#616161) | Light Grey | Voice tracks |

## Where Colors Appear

### 1. Library Lists
- **Table Rows**: Each track row has a colored background matching its type
- **Color Bar**: A vertical color bar (4px wide) appears on the left of each track title
- **Type Chips**: Type badges use the type's color with white text
- **Tabs**: Type filter tabs show a colored dot indicator

### 2. Clock Templates
- **Element List**: Each clock element has a colored background
- **Color Bar**: Vertical color bar on the left of each element
- **Type Chips**: Colored type badges in element lists
- **Type Selector**: Dropdown shows colored dots next to each type option
- **Preview**: Preview timeline shows colored type chips

### 3. Track Dialogs
- **Edit Dialog**: Type selector shows colored chip and colored dots in dropdown
- **Play Dialog**: Type is displayed as a colored chip

### 4. Spots Library
- Same visual treatment as main library list

## Implementation

### Shared Utility
All color definitions are centralized in:
- `frontend/src/utils/trackTypes.ts`

### Functions Available
- `getTrackType(type)` - Get full type information
- `getTrackTypeColor(type)` - Get hex color code
- `getTrackTypeBackgroundColor(type)` - Get background color
- `getTrackTypeTextColor(type)` - Get text color
- `getTrackTypeChipColor(type)` - Get MUI Chip color prop (for compatibility)

### Files Updated
1. `frontend/src/utils/trackTypes.ts` - New shared utility
2. `frontend/src/pages/library/LibraryList.tsx` - Main library view
3. `frontend/src/pages/library/SpotsLibrary.tsx` - Spots library view
4. `frontend/src/pages/clocks/ClockBuilder.tsx` - Clock template builder
5. `frontend/src/components/tracks/TrackEditDialog.tsx` - Track edit dialog
6. `frontend/src/components/tracks/TrackPlayDialog.tsx` - Track play dialog

## Visual Features

### Table Rows
- Colored background (light tint of type color)
- Vertical color bar indicator on the left
- Hover effect with slightly darker background

### Type Chips/Badges
- Solid color background matching type
- White text for contrast
- Bold font weight for visibility

### Tabs
- Colored dot indicator (12px circle)
- Selected tab text color matches type color
- Bold font when selected

### Dropdowns
- Colored dot indicator next to each option
- Visual preview of type color

## Benefits

1. **Quick Visual Identification**: Instantly recognize track types at a glance
2. **Consistent Experience**: Same colors used throughout the system
3. **Better Organization**: Easier to scan and find specific types
4. **Template Clarity**: Clock templates clearly show what types are scheduled
5. **Reduced Errors**: Visual cues help prevent selecting wrong track types

## Customization

To change colors, edit `frontend/src/utils/trackTypes.ts`:

```typescript
{
  value: 'MUS',
  label: 'Music',
  color: '#1976d2',        // Main color (for chips, bars)
  backgroundColor: '#e3f2fd',  // Light background (for rows)
  textColor: '#0d47a1',   // Dark text (for contrast)
}
```

All components will automatically use the updated colors.


