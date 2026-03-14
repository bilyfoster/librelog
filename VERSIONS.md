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

### 0.1.5 (In Progress - March 14, 2026)
**Status**: Development 🔄
**Planned Changes**:
- Fix auth login field compatibility (accept username or email)
- Fix PUT /auth/profile 500 error
- Fix Order advertiser relationship

**Deployment Date**: TBD

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
