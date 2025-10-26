# ISRC Compliance Guide for GayPHX Music Platform

## ⚠️ CRITICAL: ISRC Registration Required

**Before using this system in production, you MUST register as an ISRC manager with the US ISRC agency.**

### 🔗 Registration Process:

1. **Visit**: https://usisrc.org/how-it-works/#isrc-managers
2. **Apply**: Submit application for ISRC manager status
3. **Get Code**: Receive your official 3-character registrant code
4. **Update Config**: Replace "GPH" with your official code

## ✅ Current System Compliance

### **Fixed Issues:**
- ✅ **Format**: Now generates proper `CC-XXX-YY-NNNNN` format
- ✅ **Registrant Code**: Changed to 3 characters (GPH - placeholder)
- ✅ **Validation**: Proper ISRC format validation
- ✅ **Sequencing**: Correct 5-digit sequence numbers

### **Example Generated ISRC:**
```
US-GPH-25-00001
US-GPH-25-00002
US-GPH-25-00003
```

## 🚨 What You Need to Do:

### 1. **Register with US ISRC Agency**
- Apply at: https://usisrc.org/
- Get your official 3-character code
- Update the system configuration

### 2. **Update Configuration**
```bash
# In your .env file or environment variables:
ISRC_REGISTRANT=YOUR_OFFICIAL_CODE  # Replace GPH with your code
```

### 3. **Database Migration**
If you've already generated ISRCs with the old format, you'll need to:
```sql
-- Update existing ISRCs to proper format
UPDATE isrcs SET isrc_code = 
  CONCAT(country_code, '-', registrant_code, '-', 
         RIGHT(year::text, 2), '-', 
         LPAD(sequence_number::text, 5, '0'))
WHERE isrc_code NOT LIKE '%-%-%-%';
```

## 📋 ISRC Assignment Rules

### **Format Requirements:**
- **Total Length**: 12 characters + 3 hyphens = 15 characters
- **Country Code**: 2 letters (US)
- **Registrant Code**: 3 letters (must be registered)
- **Year**: 2 digits (last 2 digits of year)
- **Sequence**: 5 digits (00001-99999)

### **Assignment Rules:**
1. **One ISRC per track** - Never reuse
2. **Sequential numbering** - Within each year
3. **Year-based reset** - Sequence resets each year
4. **Permanent assignment** - Never change once assigned

### **What Gets an ISRC:**
- ✅ **Master recordings** (final mixed tracks)
- ✅ **Different versions** (radio edit, extended mix, etc.)
- ✅ **Remixes** (if significantly different)
- ❌ **NOT for** individual stems, samples, or works-in-progress

## 🔧 System Configuration

### **Current Settings:**
```python
isrc_country: str = "US"
isrc_registrant: str = "GPH"  # ⚠️ PLACEHOLDER - MUST BE REPLACED
```

### **Required Changes:**
1. Register with US ISRC agency
2. Get official 3-character code
3. Update `isrc_registrant` in config
4. Test ISRC generation
5. Verify format compliance

## 📊 ISRC Management Features

### **Admin Dashboard:**
- View all assigned ISRCs
- Track ISRC usage statistics
- Generate ISRC reports
- Export ISRC data

### **Artist Dashboard:**
- View assigned ISRCs
- Download ISRC certificates
- Track ISRC usage

### **API Endpoints:**
- `GET /api/admin/isrcs` - List all ISRCs
- `POST /api/admin/submissions/{id}/assign-isrc` - Assign ISRC
- `GET /api/admin/isrc-stats` - ISRC statistics

## ⚖️ Legal Compliance

### **Rights Management:**
- ISRCs are assigned to **sound recordings**
- Copyright holders must be properly identified
- Rights must be cleared before ISRC assignment
- Commercial use requires proper licensing

### **Record Keeping:**
- Maintain ISRC assignment logs
- Track commercial usage
- Document rights holders
- Keep audit trails

## 🚀 Next Steps

1. **Immediate**: Register with US ISRC agency
2. **Update**: Replace placeholder registrant code
3. **Test**: Verify ISRC generation works correctly
4. **Deploy**: Use in production with proper registration

## 📞 Support

- **US ISRC Agency**: https://usisrc.org/
- **Technical Issues**: Check system logs
- **Format Questions**: Refer to ISRC specification

---

**⚠️ WARNING**: Using unregistered ISRC codes may result in:
- Invalid ISRC assignments
- Legal compliance issues
- Rejection by distribution platforms
- Loss of royalty tracking capabilities

**✅ SOLUTION**: Register with US ISRC agency before production use.

