# Task List: Professional Clock Template Manager

Based on requirements for a WideOrbit-level clock template management system with LibreTime integration.

## Relevant Files

### Backend - Models & Entities
- `librelog-api/src/main/java/com/onelpro/librelog/models/ClockTemplate.java` - Clock template entity (exists, may need updates)
- `librelog-api/src/main/java/com/onelpro/librelog/models/BreakStructure.java` - Commercial break entity (exists, may need updates)
- `librelog-api/src/main/java/com/onelpro/librelog/models/Channel.java` - Channel entity (exists)

### Backend - Repositories
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/ClockTemplateRepository.java` - Clock template repository (exists, may need additional methods)
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/BreakStructureRepository.java` - Break structure repository (exists, may need additional methods)

### Backend - DTOs
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ClockTemplateRequestDTO.java` - Request DTO (exists, may need updates)
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ClockTemplateResponseDTO.java` - Response DTO (exists, may need updates)
- `librelog-api/src/main/java/com/onelpro/librelog/dto/BreakStructureRequestDTO.java` - Break structure request DTO (to be created)
- `librelog-api/src/main/java/com/onelpro/librelog/dto/BreakStructureResponseDTO.java` - Break structure response DTO (to be created)
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ClockTemplateWithBreaksDTO.java` - Combined clock template with breaks DTO (to be created)

### Backend - Services
- `librelog-api/src/main/java/com/onelpro/librelog/services/ClockService.java` - Clock service interface (exists, may need updates)
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/ClockServiceImpl.java` - Clock service implementation (exists, may need updates)
- `librelog-api/src/main/java/com/onelpro/librelog/services/BreakStructureService.java` - Break structure service interface (to be created)
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/BreakStructureServiceImpl.java` - Break structure service implementation (to be created)
- `librelog-api/src/main/java/com/onelpro/librelog/services/ClockBuilderService.java` - Clock builder/validation service (to be created)
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/ClockBuilderServiceImpl.java` - Clock builder implementation (to be created)

### Backend - Controllers
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/ClockController.java` - Clock template controller (exists, may need updates)
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/BreakStructureController.java` - Break structure controller (to be created)

### Backend - Integration
- `librelog-api/src/main/java/com/onelpro/librelog/integrations/LibreTimeClient.java` - LibreTime API client (to be created/updated)
- `librelog-api/src/main/java/com/onelpro/librelog/services/LibreTimeSyncService.java` - Service for syncing clocks to LibreTime (to be created)
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeSyncServiceImpl.java` - LibreTime sync implementation (to be created)

### Frontend - UI Components
- `librelog-api/src/main/resources/static/dashboard.html` - Main dashboard (exists, needs clock builder UI)
- `librelog-api/src/main/resources/static/css/clock-builder.css` - Clock builder styles (to be created)
- `librelog-api/src/main/resources/static/js/clock-builder.js` - Clock builder JavaScript (to be created)

### Database Migrations
- `librelog-api/src/main/resources/db/changelog/006-enhance-break-structures.xml` - Enhancements to break_structures table if needed (to be created)

### Tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/ClockServiceImplTest.java` - Clock service tests (may need updates)
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/BreakStructureServiceImplTest.java` - Break structure service tests (to be created)
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/ClockBuilderServiceImplTest.java` - Clock builder service tests (to be created)
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/BreakStructureControllerTest.java` - Break structure controller tests (to be created)
- `librelog-api/src/test/java/com/onelpro/librelog/integrations/LibreTimeClientTest.java` - LibreTime client tests (to be created)

### Notes

- Unit tests should be placed alongside the code files they are testing
- Use `mvn test` to run all tests
- Use `mvn jacoco:report` to generate coverage reports
- Clock templates represent 24-hour broadcast structures with commercial breaks
- Break structures define where and how long commercial breaks occur within a clock
- Integration with LibreTime requires API authentication and proper data format conversion

### WideOrbit Compatibility Requirements

The system must follow WideOrbit logic for track organization to enable future migration to higher-end software:

**Music Categories (Rotation):**
- **S1 (Power/Current):** Hottest new tracks, play every 3-4 hours
- **S2 (Secondary):** New tracks that aren't "hits" yet, play every 6-8 hours
- **S3 (New/Discovery):** Brand new uploads being tested

**Asset Types (Non-Music):**
- **IM (Imaging):** Sweepers and Stingers
- **ID (Legal ID):** Top-of-hour legal identification
- **CM (Commercials/Spots):** Paid ads
- **PR (Promos):** Internal station advertisements
- **VT (Voice Tracks):** DJ talk segments
- **SH (Show/Longform):** 5-minute interview segments

**Transition Codes:**
- **Segue (S):** Next track starts immediately as first ends
- **Overlap (O):** Next track starts while first is fading (for Liners over song intros)
- **Hard Start (H):** Forces track to play at exact time (for Legal IDs)

**Show Segment Naming:**
- Format: `SH_MORNING_SEG1`, `SH_MORNING_SEG2`, `SH_MORNING_SEG3`
- Creates predictable, major-market structure with segments at :15, :35 past the hour

## Tasks

- [ ] 1.0 Enhance Break Structure Backend Infrastructure
- [ ] 2.0 Implement WideOrbit-Style Track Categories and Asset Types
- [ ] 3.0 Create Visual Clock Builder UI
- [ ] 4.0 Implement Clock Template Management Features
- [ ] 5.0 Add Clock Validation and Business Logic
- [ ] 6.0 Implement Transition Code Support
- [ ] 7.0 Implement LibreTime Integration
- [ ] 8.0 Add Advanced Clock Template Features
- [ ] 9.0 Testing and Quality Assurance

