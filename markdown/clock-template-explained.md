# Clock Templates Explained: How They Work

## What is a Clock Template?

A **Clock Template** is a 60-minute blueprint that defines what happens during every hour of your broadcast day. Think of it as a repeating pattern that tells your automation system:

- **When** commercial breaks occur
- **When** songs play
- **When** talk segments air
- **When** legal IDs and station IDs play
- **When** automation commands execute

## The 60-Minute Hour

**Important**: Clock templates work in a **60-minute window from 00:00 to 00:59**.

- **00:00** = Top of the hour (start of the hour)
- **00:15** = 15 minutes past the hour
- **00:30** = 30 minutes past the hour
- **00:45** = 45 minutes past the hour
- **00:59** = 59 minutes past the hour (end of the hour)

### Why 00:00 to 00:59?

The clock template **repeats every hour**. So:
- At 6:00 AM, the template starts at 00:00
- At 7:00 AM, the template starts at 00:00 again
- At 8:00 AM, the template starts at 00:00 again
- And so on...

The template doesn't care what time of day it is - it only cares about the **position within the hour**.

## Example: Building a 15-Second Legal ID

Let's say you want a Legal ID that plays for 15 seconds at the top of every hour:

1. **Click "+ Add Fixed Asset"**
2. **Name**: "Legal ID"
3. **Asset Type**: "ID (Legal ID)"
4. **Start Time**: **00:00** (top of the hour)
5. **Duration**: **Leave empty** (Legal IDs are instant - they play and stop immediately)
6. **Content Type**: "MIXED" (or leave empty)
7. Click "Add Fixed Asset"

**Result**: Every hour at :00, your Legal ID plays for 15 seconds (or whatever duration the actual audio file is).

## Example: Building a 3-Minute Commercial Break

1. **Click "+ Add Break"**
2. **Name**: "Break 1"
3. **Start Time**: **00:15** (15 minutes past the hour)
4. **Duration**: **180** seconds (3 minutes)
5. **Asset Type**: "CM (Commercials)"
6. Click "Add Break"

**Result**: Every hour at :15, a 3-minute commercial break slot is reserved.

## Example: Building a 4-Minute Song

1. **Click "+ Add Fixed Asset"**
2. **Name**: "Morning Song 1"
3. **Asset Type**: "SH (Show/Longform)"
4. **Start Time**: **00:05** (5 minutes past the hour)
5. **Duration**: **240** seconds (4 minutes)
6. **Content Type**: "MUSIC"
7. **Music Category**: "S1"
8. Click "Add Fixed Asset"

**Result**: Every hour at :05, a 4-minute song plays.

## Visual Timeline

The clock builder shows a vertical timeline:

```
00:00 ──────────────────── Legal ID (15 sec)
00:01 ──────────────────── 
00:02 ────────────────────
00:03 ────────────────────
00:04 ────────────────────
00:05 ──────────────────── Song 1 (4 min)
00:06 ────────────────────
00:07 ────────────────────
00:08 ────────────────────
00:09 ────────────────────
00:10 ────────────────────
00:11 ────────────────────
00:12 ────────────────────
00:13 ────────────────────
00:14 ────────────────────
00:15 ──────────────────── Break 1 (3 min)
00:16 ────────────────────
00:17 ────────────────────
00:18 ────────────────────
...continues to 00:59
```

## How LibreTime Uses Clock Templates

### What Gets Built in LibreTime?

When you export a clock template to LibreTime, it creates:

1. **Show Instances**: LibreTime schedules your clock template as a "show" that runs every hour
2. **Smart Blocks**: Commercial breaks become "smart blocks" that auto-populate with commercials based on your criteria
3. **Playlists**: Songs and fixed assets become playlists or direct file references
4. **Daily Logs**: LibreTime generates a daily log showing exactly what plays at each time

### Example LibreTime Export

Your clock template:
- 00:00 - Legal ID (15 sec)
- 00:05 - Song 1 (4 min)
- 00:15 - Break 1 (3 min)

Becomes in LibreTime:
```
06:00:00 - Legal ID (file: legal_id_001.mp3)
06:00:15 - Song 1 (file: song_001.mp3)
06:04:15 - Break 1 (smart block: commercials_60s)
06:07:15 - Song 2 (file: song_002.mp3)
...continues
```

At 7:00 AM, the same pattern repeats:
```
07:00:00 - Legal ID (file: legal_id_001.mp3)
07:00:15 - Song 1 (file: song_001.mp3)
07:04:15 - Break 1 (smart block: commercials_60s)
...continues
```

## Common Questions

### Q: Why does it show "12:00 AM" instead of "00:00"?

**A**: Your browser's locale settings may display times in 12-hour format. The system stores and uses 24-hour format (00:00 to 00:59) internally. The time input should show 00:00, but if it shows 12:00 AM, that's the same thing - just a display difference.

**Fix**: We've added `min="00:00" max="00:59"` to force the correct range. If you still see 12-hour format, try:
- Typing "00:00" directly
- Using the up/down arrows to adjust
- The system will convert it correctly

### Q: How do I make something that runs for 15 seconds?

**A**: 
- For **Fixed Assets** (Legal IDs, Station IDs): Leave duration **empty**. The actual audio file duration determines how long it plays.
- For **Breaks**: Enter **15** in the duration field (in seconds).

### Q: What's the difference between a Break and a Fixed Asset?

**A**:
- **Break**: A time slot reserved for commercials/promos. Duration is **required**. LibreTime fills it with content from your library.
- **Fixed Asset**: A specific audio file that plays at the same time every hour. Duration is **optional** (only needed for songs/interviews).

### Q: Can I have overlapping elements?

**A**: No. Elements cannot overlap. The validation panel will show an error if you try to create overlapping elements.

### Q: What happens if my clock doesn't fill the full 60 minutes?

**A**: That's okay! LibreTime will handle the gaps. You can leave time slots empty, or fill them with music/silence.

## Best Practices

1. **Start with breaks**: Add your commercial breaks first - they're the most important
2. **Use validation**: Always check the validation panel for errors
3. **Test export**: Export to LibreTime and verify the log looks correct
4. **Use content types**: Set MUSIC, TALK, INTERVIEW, or MIXED to help LibreTime categorize content
5. **Set durations**: Always set durations for timed content (songs, interviews, breaks)

## Quick Reference

| Element Type | Start Time Format | Duration Required? | Example |
|-------------|-------------------|-------------------|---------|
| Break | 00:00 to 00:59 | **Yes** (seconds) | 00:15, 180 sec = 3 min break |
| Fixed Asset (Song) | 00:00 to 00:59 | **Yes** (seconds) | 00:05, 240 sec = 4 min song |
| Fixed Asset (Legal ID) | 00:00 to 00:59 | **No** (instant) | 00:00, no duration |
| Automation Command | 00:00 to 00:59 | N/A | 00:00, triggers instantly |

Remember: **00:00 = top of hour, 00:59 = end of hour**. The template repeats every hour!



