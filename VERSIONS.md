# LibreLog Version History & Deployment Log

## Version Format
- Format: `MAJOR.MINOR.PATCH`
- Example: `0.1.4`

## Deployment Workflow

### 1. Make Code Changes
Fix bugs, add features, etc.

### 2. Update Version Numbers
Update version in BOTH files:
- `pom.xml` - Parent POM
- `librelog-api/pom.xml` - API module POM
- `Dockerfile` - JAR file reference

### 3. Build & Test Locally
```bash
cd /home/jenkins/docker/librelog
mvn clean compile
mvn test
```

### 4. Commit Changes
```bash
git add .
git commit -m "vX.Y.Z: Description of changes"
```

### 5. Deploy
```bash
# Build the JAR
mvn clean package -DskipTests

# Deploy with Docker
docker-compose up -d --build

# Verify deployment
docker logs librelog-api --tail 20
curl https://log.gayphx.com/actuator/health
```

---

## Version History

### 0.1.4 (Current - March 13, 2026)
**Status**: Deployed ✅
**Changes**:
- Base MVP with campaign management
- Station/Clock/Daypart infrastructure
- LibreTime integration foundation

**Known Issues**:
- Auth login expects `email` but some tests send `username`
- PUT /auth/profile returns 500 error
- Order model missing advertiserId relationship

---

### 0.1.5 (March 14, 2026)
**Status**: Deployed ✅
**Changes**:
- Fix auth login field compatibility (accept username or email)
- Fix PUT /auth/profile 500 error (improved error handling)

**Deployment Date**: March 14, 2026

---

### 0.1.6 (March 14, 2026)
**Status**: Deployed ✅
**Changes**:
- Fix JWT filter - /api/auth/me and /api/auth/profile now properly authenticated
- Previous fix incorrectly allowed all /api/auth/* to skip JWT validation

**Deployment Date**: March 14, 2026

---

### 0.1.7 (March 14, 2026)
**Status**: Deployed ✅
**Changes**:
- Added advertiser_id foreign key to orders table
- Order model now has @ManyToOne relationship to Advertiser entity
- OrderRequestDTO accepts advertiserId (auto-populates name, agency, sales rep)
- OrderResponseDTO includes advertiserId, agencyId, salesRepId
- Backward compatible - manual entry still works without advertiserId

**Deployment Date**: March 14, 2026

---

### 0.1.8 (March 14, 2026)
**Status**: Deployed ✅
**Changes**:
- Added song_before_id and song_after_id to voice_tracks table
- VoiceTrack entity has @ManyToOne relationships to Track for song context
- Added denormalized songBeforeTitle and songAfterTitle for display
- VoiceTrackRequestDTO accepts songBeforeId and songAfterId
- VoiceTrackResponseDTO includes full song context (ID, title, artist)
- DJs can now see song context when recording voice tracks

**Deployment Date**: March 14, 2026

---

### 0.1.9 (March 14, 2026)
**Status**: Deployed ✅ MVP COMPLETE 🎉
**Changes**:
- POST /api/campaigns/from-order/{orderId} creates campaign from order
- Auto-populates campaign: name, dates, spots, advertiser from order
- GET /api/campaigns/by-order/{orderId} lists campaigns for an order
- Prevents duplicate campaigns per order
- Order → Campaign workflow now fully functional

**MVP Status**: All critical fixes complete ✅
**Deployment Date**: March 14, 2026

---

## Deployment Checklist

- [ ] Version updated in pom.xml (parent)
- [ ] Version updated in librelog-api/pom.xml
- [ ] Version updated in Dockerfile
- [ ] Tests pass locally
- [ ] Changes committed to git
- [ ] Docker image builds successfully
- [ ] Container starts without errors
- [ ] Health check passes
- [ ] Manual smoke test completed
- [ ] VERSIONS.md updated
