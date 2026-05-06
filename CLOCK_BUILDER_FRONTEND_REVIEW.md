# Clock Builder Frontend Engineering Review

**Reviewer**: AI Frontend Engineer  
**Date**: March 14, 2026  
**Scope**: Clock Builder UI/UX, Tour System, Setup Workflow

---

## Executive Summary

The current clock builder has **significant usability issues** that will block user adoption:

1. **Tour blocks interaction** - The modal overlay approach prevents users from actually using the interface during "guided" setup
2. **Overly complex for simple use cases** - Setting up a basic music hour requires ~15+ manual steps
3. **No template/quick-start system** - Every clock must be built from scratch
4. **Vertical timeline is unwieldy** - 3600px height for one hour requires excessive scrolling

**Recommendation**: Implement a "Quick Start Wizard" with presets before showing the full builder.

---

## Detailed Findings

### 1. Tour System Issues (Critical)

#### Problem: Overlay Blocks Interaction
```javascript
// Current implementation - BLOCKS the UI
overlay.style.cssText = `
    position: fixed;
    top: 0; left: 0;
    width: 100%; height: 100%;
    background: rgba(0, 0, 0, 0.7);  // <-- Blocks everything
    pointer-events: none;
    z-index: 10000;
`;
```

The tour creates a full-screen semi-transparent overlay with `pointer-events: none`, then tries to poke holes for specific elements. This approach:
- **Breaks on window resize** (highlight positions get stale)
- **Breaks on scroll** (highlight doesn't follow element)
- **Makes users feel trapped** (can't explore outside tour path)
- **Fails on dynamic content** (elements that load async aren't highlighted)

#### Tour Flow Problems
The tour has **14 steps** that force users through a rigid sequence:
1. Welcome
2. Timeline explanation
3. Buttons explanation
4. Add Legal ID (forced action)
5. Fill Legal ID form (forced values)
6. Add Song (forced action)
7. Fill Song form (forced values)
8. Add Talk (forced action)
9. Fill Talk form (forced values)
10. Add Break (forced action)
11. Fill Break form (forced values)
12. Element types explanation
13. Validation explanation
14. Revenue explanation

**Issues:**
- Users can't experiment or deviate
- No "skip to my needs" option
- Must complete entire tour or dismiss entirely
- No way to reference tour later without restarting

---

### 2. Clock Setup Complexity (Major)

#### Current Workflow for Basic Music Hour:
1. Create clock template
2. Open clock builder
3. Start tour (optional but prominent)
4. Add Legal ID at 00:00
5. Add Song 1 at 00:01
6. Add Song 2 at 00:05
7. Add Break at 00:10
8. Add Song 3 at 00:13
9. Add Song 4 at 00:17
10. Add Break at 00:21
11. Add Song 5 at 00:24
12. Add Song 6 at 00:28
13. Add Break at 00:32
14. Add Song 7 at 00:35
15. Add Song 8 at 00:39
16. Add Legal ID at 00:43
17. Add Song 9 at 00:44
18. Add Song 10 at 00:48
19. Add Break at 00:52
20. Add Song 11 at 00:55
21. Validate
22. Save

**~20+ clicks** for a standard rotation hour!

#### Comparison: Competitor Approach
WideOrbit and similar tools offer:
- **Clock Templates**: "Music Hour", "Talk Hour", "Mixed Hour" presets
- **Auto-fill**: "Fill remaining time with songs from category X"
- **Copy/Paste**: Duplicate existing clocks and modify
- **Spreadsheet Import**: CSV upload of clock structure

---

### 3. Timeline Visualization Issues (Medium)

#### Vertical Layout Problems
```javascript
timelineScale = 60; // pixels per minute
// 60 minutes × 60px = 3600px height
```

- **3600px height** = 4+ screen heights of scrolling
- Users can't see the full hour at once
- Drag-and-drop becomes imprecise at distance
- No zoom in/out capability

#### Alternative: Horizontal Layout
Many broadcast tools use horizontal timeline:
- Left to right = Time (like a calendar)
- Top to bottom = Days/Channels
- More natural for reading time
- Fits better on widescreen monitors

---

### 4. Missing Quick-Start Features (Major)

#### No Template System
Users should be able to:
1. "Start from template: Standard Music Hour"
2. "Start from template: Morning Drive Mix"
3. "Start from template: Talk Radio Hour"
4. "Copy from existing clock: [dropdown]"

#### No Auto-Fill
- "Fill with 3 songs, 1 break, repeat"
- "Add Legal IDs at top and :45"
- "Insert station IDs every 15 minutes"

#### No Bulk Operations
- Can't select multiple elements
- Can't copy/paste element groups
- Can't "shift all elements after 00:15 by +2 minutes"

---

## Recommended Solutions

### Option A: Quick-Start Wizard (Recommended)

Replace the tour with a **proactive wizard**:

```
┌─────────────────────────────────────────────────────────┐
│  🕐 Clock Setup Wizard                                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Step 1: Choose Your Starting Point                     │
│                                                         │
│  [📻 Standard Music Hour]  [🎙️ Talk/Interview Hour]     │
│  [🎵 Mixed Music/Talk]     [📋 Copy Existing Clock]     │
│  [📄 Import from CSV]      [🔧 Start from Scratch]      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Standard Music Hour** would auto-generate:
- Legal ID at 00:00
- 3 songs (category: Current Hits)
- 1 break (3 min)
- 3 songs
- 1 break
- 3 songs
- 1 break
- 2 songs
- Legal ID at 00:59

Then open the builder for fine-tuning.

### Option B: Inline Help (Alternative)

Instead of blocking tour, use **contextual tooltips**:

```javascript
// Replace tour with inline help
const helpTooltips = {
    'timelineContainer': {
        title: 'Timeline',
        tip: 'Drag elements to adjust timing. Breaks = commercials, Assets = songs/IDs',
        showOnce: true
    },
    'addBreakBtn': {
        title: 'Add Break',
        tip: 'Breaks hold commercial spots. Must have duration.',
        videoUrl: '/help/breaks.mp4'
    }
};
```

### Option C: Simplified Builder View

Add a **"Simple Mode"** toggle:

| Feature | Current | Simple Mode |
|---------|---------|-------------|
| Timeline | Vertical, 60px/min | Horizontal, 10px/min |
| Fields | 10+ per element | Name, Time, Duration only |
| WO Fields | All visible | Hidden behind "Advanced" |
| Drag/Resize | Precise | Snap to 15-min increments |

---

## Implementation Priority

### Phase 1: Fix Tour (1-2 days)
- [ ] Remove blocking overlay
- [ ] Convert to sidebar help panel
- [ ] Add "Quick Start" button that generates template

### Phase 2: Templates (3-5 days)
- [ ] Create 5 standard clock templates in DB
- [ ] Add "Start from Template" modal
- [ ] Add "Copy from Existing Clock" option

### Phase 3: Auto-Fill (2-3 days)
- [ ] "Fill with pattern" feature
- [ ] "Distribute evenly" for breaks
- [ ] Bulk copy/paste

### Phase 4: Timeline Redesign (1-2 weeks)
- [ ] Horizontal layout option
- [ ] Zoom controls
- [ ] Mini-map overview

---

## Code Quality Notes

### Current Issues:

1. **Mixed concerns in clock-builder.js** (~1500+ lines)
   - State management
   - DOM manipulation
   - API calls
   - Event handlers
   - Drag-and-drop logic
   
   **Recommend**: Split into modules:
   ```
   clock-builder/
   ├── state.js         # Current state management
   ├── timeline.js      # Render & update timeline
   ├── dragdrop.js      # DnD handlers
   ├── api.js           # Backend communication
   └── templates.js     # Template/quick-start logic
   ```

2. **Global state pollution**
   ```javascript
   // All globals - hard to test, easy to collide
   let currentClockTemplate = null;
   let clockElements = [];
   let selectedElement = null;
   let isDragging = false;
   ```

3. **No debouncing on drag**
   ```javascript
   // Called on EVERY mouse move during drag
   function handleDrag(e) {
       updateDragTooltip(e, newTop);  // <-- DOM update every frame!
   }
   ```

4. **Accessibility issues**
   - Timeline elements have `role="button"` but no `aria-pressed`
   - No keyboard-only navigation for drag operations
   - Color coding without text labels (breaks vs assets)

---

## Conclusion

**The clock builder is technically functional but UX-hostile.**

The tour system tries to compensate for complexity but makes it worse by blocking interaction. Users need:

1. **Presets** over tours (start with something working)
2. **Progressive disclosure** over exhaustive forms (hide WO fields by default)
3. **Bulk operations** over one-by-one element creation
4. **Contextual help** over forced walkthroughs

**Immediate action**: Add a "Quick Start" button that generates a working clock template with one click. This provides more value than the entire tour system.

---

*End of Review*
