# LibreTime Track Types Setup Guide for LibreLog Integration

**Document Version:** 1.0  
**Date:** January 1, 2026  
**Purpose:** Guide for configuring LibreTime track types to work seamlessly with LibreLog integration

---

## Overview

LibreLog uses specific asset types and content types that need to be mapped to LibreTime track types for proper file synchronization and clock template export. This guide outlines the required track types you should create in LibreTime to ensure full compatibility with LibreLog.

---

## Asset Types (Required)

LibreLog uses six primary asset types that correspond to different types of audio content. You should create track types in LibreTime that match these categories:

### 1. IM (Imaging) - **REQUIRED**
- **LibreLog Asset Type:** `IM`
- **Description:** Sweepers, stingers, and station imaging elements
- **LibreTime Track Type Name:** `Imaging` or `IM`
- **Use Case:** Short audio elements used for station branding, transitions, and imaging
- **Typical Duration:** 5-30 seconds
- **Content Type:** Usually `MUSIC` or `MIXED`

**Recommended LibreTime Configuration:**
- Track Type Name: `Imaging` or `IM`
- Code: `IM` (for compatibility)
- Description: "Station imaging, sweepers, and stingers"

### 2. ID (Legal ID) - **REQUIRED**
- **LibreLog Asset Type:** `ID`
- **Description:** Top-of-hour legal identification announcements
- **LibreTime Track Type Name:** `Legal ID` or `ID`
- **Use Case:** FCC-required station identification announcements
- **Typical Duration:** 5-10 seconds
- **Content Type:** Usually `TALK`

**Recommended LibreTime Configuration:**
- Track Type Name: `Legal ID` or `ID`
- Code: `ID` (for compatibility)
- Description: "FCC-required legal identification announcements"

### 3. CM (Commercials) - **REQUIRED**
- **LibreLog Asset Type:** `CM`
- **Description:** Paid commercial advertisements
- **LibreTime Track Type Name:** `Commercial` or `CM`
- **Use Case:** Commercial breaks and paid advertising spots
- **Typical Duration:** 15-60 seconds
- **Content Type:** Usually `ADVERT`

**Recommended LibreTime Configuration:**
- Track Type Name: `Commercial` or `CM`
- Code: `CM` (for compatibility)
- Description: "Paid commercial advertisements and spots"

### 4. PR (Promos) - **REQUIRED**
- **LibreLog Asset Type:** `PR`
- **Description:** Internal station promotional content
- **LibreTime Track Type Name:** `Promo` or `PR`
- **Use Case:** Promotional content for station events, shows, and programming
- **Typical Duration:** 15-30 seconds
- **Content Type:** Usually `ADVERT` or `MIXED`

**Recommended LibreTime Configuration:**
- Track Type Name: `Promo` or `PR`
- Code: `PR` (for compatibility)
- Description: "Internal station promotional content"

### 5. VT (Voice Tracks) - **REQUIRED**
- **LibreLog Asset Type:** `VT`
- **Description:** DJ talk segments and voice tracks
- **LibreTime Track Type Name:** `Voice Track` or `VT`
- **Use Case:** Recorded voice tracks from DJs, hosts, or announcers
- **Typical Duration:** 10-60 seconds
- **Content Type:** Usually `TALK`

**Recommended LibreTime Configuration:**
- Track Type Name: `Voice Track` or `VT`
- Code: `VT` (for compatibility)
- Description: "DJ talk segments and voice tracks"

### 6. SH (Show/Longform) - **REQUIRED**
- **LibreLog Asset Type:** `SH`
- **Description:** Long-form content like interviews and features
- **LibreTime Track Type Name:** `Show` or `SH` or `Longform`
- **Use Case:** Extended content like interviews, features, and long-form programming
- **Typical Duration:** 2-5 minutes or longer
- **Content Type:** Usually `TALK`, `INTERVIEW`, or `MIXED`

**Recommended LibreTime Configuration:**
- Track Type Name: `Show` or `SH` or `Longform`
- Code: `SH` (for compatibility)
- Description: "Long-form content including interviews and features"

---

## Content Types (Optional but Recommended)

In addition to asset types, LibreLog also uses content type classifications. While these are optional, creating corresponding track types or categories in LibreTime will improve organization and filtering:

### 1. MUSIC
- **Description:** Music content
- **Use Case:** Songs, music beds, instrumental tracks
- **Mapping:** Can be used with any asset type (IM, CM, PR, etc.)

### 2. TALK
- **Description:** Spoken word content
- **Use Case:** Voice tracks, legal IDs, talk segments
- **Mapping:** Typically used with VT, ID, SH asset types

### 3. INTERVIEW
- **Description:** Interview content
- **Use Case:** Interview segments, features
- **Mapping:** Typically used with SH asset type

### 4. MIXED
- **Description:** Mixed content (music and talk)
- **Use Case:** Content that combines music and spoken elements
- **Mapping:** Can be used with any asset type

### 5. ADVERT
- **Description:** Advertisement content
- **Use Case:** Commercials and promotional content
- **Mapping:** Typically used with CM and PR asset types

---

## Setup Instructions for LibreTime

### Step 1: Access Track Types Configuration

1. Log in to your LibreTime web interface
2. Navigate to **Settings** → **Track Types** (or **Library** → **Track Types**)
3. You should see a list of existing track types

### Step 2: Create Required Track Types

For each of the six required asset types (IM, ID, CM, PR, VT, SH), create a corresponding track type:

#### Creating a Track Type:

1. Click **"Add New Track Type"** or **"Create Track Type"**
2. Fill in the following fields:
   - **Name:** Use the recommended name (e.g., "Imaging" for IM)
   - **Code:** Use the asset type code (e.g., "IM") - **This is critical for mapping**
   - **Description:** Add the description from this guide
   - **Color:** Assign a color for visual identification (optional)
3. Save the track type
4. Repeat for all six asset types

### Step 3: Verify Track Type Codes

**IMPORTANT:** The track type **Code** field in LibreTime must match the LibreLog asset type exactly:
- `IM` for Imaging
- `ID` for Legal ID
- `CM` for Commercial
- `PR` for Promo
- `VT` for Voice Track
- `SH` for Show/Longform

The integration uses these codes to map files between systems.

### Step 4: Optional - Create Content Type Categories

If LibreTime supports categories or tags, you can create categories for content types:
- MUSIC
- TALK
- INTERVIEW
- MIXED
- ADVERT

These can be used for additional filtering and organization.

---

## Mapping Table

| LibreLog Asset Type | LibreTime Track Type Name | LibreTime Code | Typical Content Types |
|---------------------|---------------------------|----------------|----------------------|
| IM | Imaging / IM | Imaging | `IM` | MUSIC, MIXED |
| ID | Legal ID / ID | Legal ID | `ID` | TALK |
| CM | Commercial / CM | Commercial | `CM` | ADVERT |
| PR | Promo / PR | Promo | `PR` | ADVERT, MIXED |
| VT | Voice Track / VT | Voice Track | `VT` | TALK |
| SH | Show / SH / Longform | Show | `SH` | TALK, INTERVIEW, MIXED |

---

## File Format Support

LibreLog supports the following audio file formats, which should also be supported in LibreTime:

- **MP3** (Most common)
- **WAV** (Uncompressed)
- **FLAC** (Lossless)
- **OGG** (Vorbis)
- **AAC** (Advanced Audio Coding)

Ensure LibreTime is configured to accept these formats when files are uploaded via the API.

---

## Integration Behavior

### File Upload from LibreLog to LibreTime

When LibreLog uploads a file to LibreTime:
1. The file's `assetType` (IM, ID, CM, PR, VT, or SH) is sent in the upload request
2. LibreTime should map this to the corresponding track type using the **Code** field
3. If no matching track type is found, LibreTime may:
   - Create a default track type
   - Return an error
   - Assign to a generic "Unknown" track type

### File Download from LibreTime to LibreLog

When LibreLog downloads a file from LibreTime:
1. LibreTime should return the track type code in the file metadata
2. LibreLog maps this code to its AssetType enum
3. If the code doesn't match, LibreLog may assign a default type or skip the file

### Clock Template Export

When exporting clock templates from LibreLog to LibreTime:
1. Each fixed asset in the clock has an `assetType`
2. The export service maps this to LibreTime track types
3. Files in the exported log should be assigned to the correct track type in LibreTime

---

## Troubleshooting

### Issue: Files Not Appearing with Correct Track Type

**Symptoms:**
- Files uploaded from LibreLog appear in LibreTime but with wrong or no track type
- Files downloaded from LibreTime lose their asset type classification

**Solutions:**
1. Verify track type codes match exactly (case-sensitive)
2. Check that track types are created in LibreTime with the correct codes
3. Verify the LibreTime API is returning/accepting the `assetType` field correctly
4. Check API logs for mapping errors

### Issue: Clock Export Fails

**Symptoms:**
- Clock templates export but files aren't assigned to correct track types
- Export completes but shows warnings about unknown track types

**Solutions:**
1. Ensure all six required track types exist in LibreTime
2. Verify track type codes match the mapping table above
3. Check that files referenced in the clock exist in LibreTime with correct track types
4. Review export logs for specific mapping errors

---

## Best Practices

1. **Use Standard Codes:** Always use the exact codes (IM, ID, CM, PR, VT, SH) for track types
2. **Documentation:** Add descriptions to track types for clarity
3. **Color Coding:** Use different colors for each track type for visual identification
4. **Testing:** After setup, test file upload/download to verify mapping works correctly
5. **Backup:** Document your track type configuration for reference

---

## Verification Checklist

Before using the LibreLog-LibreTime integration, verify:

- [ ] All six track types are created (IM, ID, CM, PR, VT, SH)
- [ ] Track type codes match exactly (case-sensitive)
- [ ] Track types are active/enabled in LibreTime
- [ ] Test file upload from LibreLog assigns correct track type
- [ ] Test file download from LibreTime preserves track type
- [ ] Clock template export maps asset types correctly

---

## Additional Resources

- **LibreTime Documentation:** [LibreTime Track Types Documentation](https://libretime.org/docs)
- **LibreLog Integration Guide:** See `prd-libretime-api-integration.md`
- **API Requirements:** See `prd-libretime-api-v2-requirements.md`

---

## Support

If you encounter issues with track type mapping:

1. Check the integration logs in LibreLog
2. Verify track type codes match the mapping table
3. Test API connectivity using the LibreTime Integration Settings page
4. Contact your system administrator or the LibreLog development team

---

**Document End**

