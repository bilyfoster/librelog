# Clock Builder Walkthrough Guide

## Overview
The Clock Builder is a visual tool for creating 60-minute broadcast clock templates that can be exported to LibreTime. This guide explains how to use it effectively.

## Understanding Clock Elements

### 1. **Breaks (Commercial Breaks/Stopsets)**
- **Purpose**: Time slots reserved for commercials, promos, or other scheduled content
- **Duration**: **REQUIRED** - Set in seconds (e.g., 60 seconds = 1 minute, 180 seconds = 3 minutes)
- **How to Set**: When adding a break, enter the duration in the "Duration (seconds)" field
- **Example**: A 3-minute commercial break = 180 seconds
- **LibreTime Export**: Breaks become "Smart Blocks" or "Playlists" in LibreTime

### 2. **Fixed Assets**
- **Purpose**: Audio elements that play at the same time every hour
- **Duration**: **OPTIONAL** - Only needed for timed content like songs or interviews
  - **No Duration**: For point-in-time elements (Legal IDs, Station IDs, News Intros)
  - **With Duration**: For songs, interviews, talk segments, or any content with a specific length
- **How to Set**: 
  - Leave duration empty for instant elements (plays and stops immediately)
  - Enter duration in seconds for timed content (e.g., 240 seconds = 4-minute song)
- **Examples**:
  - Legal ID at :00 → No duration (instant)
  - Song at :05 → Duration: 240 seconds (4 minutes)
  - Interview segment at :15 → Duration: 600 seconds (10 minutes)

### 3. **Automation Commands**
- **Purpose**: Non-audio commands (switch to satellite, start recording, etc.)
- **Duration**: Not applicable (instant triggers)
- **LibreTime Export**: Mapped to LibreTime playlists or live input switches

## Content Types: Music vs Interview/Talk Hours

### Music Hours
- **Content Type**: MUSIC
- **Structure**: Primarily music with short breaks
- **Breaks**: Short commercial breaks (60-180 seconds)
- **Fixed Assets**: Songs with durations, legal IDs without durations
- **Music Categories**: Use S1, S2, S3 to categorize music intensity
- **Example Clock**:
  - 00:00 - Legal ID (no duration)
  - 00:05 - Song (240 seconds)
  - 00:09 - Song (180 seconds)
  - 00:12 - Break (120 seconds)
  - 00:14 - Song (210 seconds)
  - ...continues

### Interview/Talk Hours
- **Content Type**: TALK or INTERVIEW
- **Structure**: Long-form talk content with breaks
- **Breaks**: Longer breaks for commercials (180-300 seconds)
- **Fixed Assets**: Interview segments with durations, talk segments, news intros
- **Music Categories**: Not typically used (leave empty)
- **Example Clock**:
  - 00:00 - Legal ID (no duration)
  - 00:01 - Interview Segment (600 seconds = 10 minutes)
  - 00:11 - Break (180 seconds)
  - 00:14 - Talk Segment (480 seconds = 8 minutes)
  - ...continues

### Mixed Hours
- **Content Type**: MIXED
- **Structure**: Combination of music and talk
- **Breaks**: Vary based on content
- **Fixed Assets**: Mix of songs (with duration) and talk segments (with duration)
- **Music Categories**: Use for music portions only
- **Example Clock**:
  - 00:00 - Legal ID (no duration)
  - 00:01 - Song (240 seconds)
  - 00:05 - Talk Segment (300 seconds)
  - 00:10 - Break (120 seconds)
  - 00:12 - Song (180 seconds)
  - 00:15 - Interview Segment (600 seconds)
  - ...continues

## Setting Up a Clock Template

### Step 1: Create the Clock Template
1. Go to "Clock Templates" tab
2. Click "+ Create Clock Template"
3. Enter name, select channel, add description
4. Click "Create"

### Step 2: Build the Clock
1. Click "Build" button next to your clock template
2. The clock builder opens showing a 60-minute vertical timeline

### Step 3: Add Elements

#### Adding a Break:
1. Click "+ Add Break"
2. Enter:
   - **Name**: e.g., "Morning Break 1"
   - **Start Time**: Auto-filled (00:00 for first, or end of last element)
   - **Duration (seconds)**: REQUIRED - e.g., 180 for 3 minutes
   - **Floating Break**: Check if break can move (optional)
   - **Timing Type**: Hard Start or Soft Start
   - **Transition Code**: Segue, Overlap, or Hard Start
   - **Asset Type**: CM (Commercials), PR (Promos), etc.
   - **Music Category**: S1, S2, S3 (for music hours)
3. Click "Add Break"

#### Adding a Fixed Asset (Song/Interview):
1. Click "+ Add Fixed Asset"
2. Enter:
   - **Name**: e.g., "Morning Song 1" or "Interview Segment 1"
   - **Asset Type**: SH (Show/Longform) for interviews, CM for songs
   - **Start Time**: Auto-filled
   - **Duration (seconds)**: 
     - **For Songs/Interviews**: Enter duration (e.g., 240 for 4-minute song)
     - **For Legal IDs/Station IDs**: Leave empty (instant)
   - **Asset Identifier**: Cart number or file name
   - **Content Type**: MUSIC, TALK, INTERVIEW, or MIXED
   - **Music Category**: S1, S2, S3 (only for music content)
3. Click "Add Fixed Asset"

#### Adding an Automation Command:
1. Click "+ Add Command"
2. Enter:
   - **Command Type**: SWITCH_TO_SATELLITE, START_RECORDING, etc.
   - **Trigger Time**: When command executes
   - **Priority**: High, Medium, Low
3. Click "Add Command"

### Step 4: Adjust Elements
- **Drag**: Click and drag elements to change start time
- **Resize**: Drag top/bottom edges to change duration (for breaks and timed fixed assets)
- **Edit Properties**: Click an element to edit its properties

### Step 5: Validate
- Check the "Validation" panel at the bottom for:
  - Timing conflicts
  - Overlaps
  - Missing required fields
  - Business rule violations

### Step 6: Review Revenue Impact
- Check the "Revenue Analysis" panel to see:
  - Total break time
  - Potential revenue based on rates
  - Available inventory

## LibreTime Export

### How It Works
1. **Breaks** → LibreTime Smart Blocks or Playlists
   - Duration determines block length
   - Asset Type and Music Category help LibreTime select content
   - Smart Blocks auto-populate based on criteria

2. **Fixed Assets** → LibreTime Carts/Files
   - Cart ID links to actual audio file in LibreTime
   - Cue points (cue in/out) control playback start/end
   - Fade in/out control transitions

3. **Automation Commands** → LibreTime Playlists or Live Inputs
   - Commands map to LibreTime's automation system
   - Playlist IDs reference specific playlists

### Export Process
1. Build your clock template
2. Validate it (no errors)
3. Click "Export to LibreTime" (when available)
4. Clock template syncs to LibreTime
5. LibreTime generates daily logs from the template

### Best Practices for LibreTime Compatibility
- **Use Content Types**: Set MUSIC, TALK, INTERVIEW, or MIXED for proper categorization
- **Set Durations**: Always set durations for timed content (songs, interviews)
- **Use Music Categories**: S1, S2, S3 help LibreTime's smart blocks select appropriate music
- **Link Cart IDs**: Connect fixed assets to actual LibreTime cart/file IDs
- **Avoid Overlaps**: LibreTime doesn't handle overlapping elements well

## Common Questions

**Q: Do all elements need durations?**
A: No. Only breaks (always) and fixed assets that are timed content (songs, interviews). Legal IDs, station IDs, and automation commands don't need durations.

**Q: How do I create a music hour?**
A: Use primarily fixed assets with durations (songs), set Content Type to MUSIC, use Music Categories (S1, S2, S3), and keep breaks short (60-180 seconds).

**Q: How do I create an interview/talk hour?**
A: Use fixed assets with long durations (interviews/talk segments), set Content Type to TALK or INTERVIEW, don't use Music Categories, and use longer breaks (180-300 seconds).

**Q: Can I mix music and talk in one hour?**
A: Yes! Set Content Type to MIXED, use durations for both songs and talk segments, and use Music Categories only for music portions.

**Q: What happens when I export to LibreTime?**
A: Your clock template becomes a show template in LibreTime. Breaks become smart blocks that auto-populate, fixed assets link to actual audio files, and the system generates daily logs.

## Tips
- Start with breaks first (they're the most important)
- Use the validation panel to catch errors early
- Drag elements to adjust timing visually
- Use the revenue panel to optimize break placement
- Test export to LibreTime before going live

