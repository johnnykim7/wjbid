# Integration Testing Guide: Java ↔ Python AI Service

This guide explains how to test the integration between the Java Spring Boot backend and the Python FastAPI AI Service.

## Architecture Overview

```
┌────────────────────────────────────┐
│   Java Spring Boot Backend         │
│   (Port 8080)                      │
│                                    │
│   AIServiceClient                  │
│   └─> RestTemplate                 │
└─────────────┬──────────────────────┘
              │ REST API
              │ (JSON)
              ↓
┌────────────────────────────────────┐
│   Python FastAPI AI Service        │
│   (Port 8001)                      │
│                                    │
│   /api/v1/requirements/extract     │
│   └─> RequirementAnalyzerAgent     │
│        └─> LangChain + OpenAI      │
└────────────────────────────────────┘
```

## Prerequisites

### 1. Environment Setup

**Python AI Service:**
```bash
cd ai-service

# Create virtual environment (if not exists)
python3 -m venv venv
source venv/bin/activate  # macOS/Linux

# Install dependencies
pip install -r requirements.txt

# Configure environment
cp .env.example .env
nano .env  # Set OPENAI_API_KEY
```

**Java Backend:**
```bash
cd backend

# Add to application.yml or .env
export AI_SERVICE_URL=http://localhost:8001
export OPENAI_API_KEY=sk-your-openai-key
```

### 2. Start Services

**Terminal 1 - Python AI Service:**
```bash
cd ai-service
source venv/bin/activate
python -m app.main

# Expected output:
# INFO:     Uvicorn running on http://0.0.0.0:8001 (Press CTRL+C to quit)
# INFO:     Started reloader process [12345] using StatReload
# INFO:     Started server process [12346]
# INFO:     Application startup complete.
```

**Terminal 2 - Java Backend:**
```bash
cd backend
./mvnw spring-boot:run

# Or using Gradle:
./gradlew bootRun
```

## Testing Methods

### Method 1: Direct API Testing (Python Service Only)

Test the Python AI Service directly using curl:

```bash
# Health check
curl http://localhost:8001/health

# Expected:
# {"status":"healthy","service":"Bidding Agency AI Service","version":"1.0.0"}

# Extract requirements
curl -X POST http://localhost:8001/api/v1/requirements/extract \
  -H "Content-Type: application/json" \
  -d '{
    "opportunity_id": "123e4567-e89b-12d3-a456-426614174000",
    "opportunity_text": "SOLICITATION NUMBER: W91247-25-Q-0001\n\nREQUIREMENTS:\n1. Technical Proposal must be submitted in PDF format\n2. Past Performance references required (minimum 3 projects)\n3. Response deadline: March 31, 2025, 3:00 PM KST\n4. Company must have 8(a) certification",
    "metadata": {}
  }'

# Expected:
# {
#   "status": "success",
#   "requirements": [
#     {
#       "category": "FORMAT",
#       "title": "Technical Proposal PDF Format",
#       "description": "Technical Proposal must be submitted in PDF format",
#       "is_blocker": true,
#       "metadata": {}
#     },
#     ...
#   ],
#   "metadata": {
#     "tokens_used": 1250,
#     "duration_ms": 3542,
#     "tools_called": [],
#     "model": "gpt-4"
#   }
# }
```

### Method 2: Java Integration Test

Run the Java integration test:

```bash
cd backend

# Run specific test
./mvnw test -Dtest=AIServiceClientIntegrationTest

# Or with Gradle:
./gradlew test --tests AIServiceClientIntegrationTest

# Expected output:
# AIServiceClientIntegrationTest > testExtractRequirements_Success() PASSED
# AIServiceClientIntegrationTest > testHealthCheck() PASSED
```

### Method 3: End-to-End Test via Java API

If you've integrated `AIServiceClient` into your domain services, test via Java REST API:

```bash
# Example: Create opportunity with auto-extraction
curl -X POST http://localhost:8080/api/opportunities \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "noticeId": "W91247-25-Q-0001",
    "title": "Construction Services at Camp Casey",
    "fullText": "REQUIREMENTS:\n1. Technical Proposal in PDF...",
    "autoExtract": true
  }'

# Expected: Opportunity created with requirements auto-extracted
```

## Integration Test Output Examples

### Successful Extraction

```
=== Extracted Requirements ===
Total: 4
Duration: 3542ms
Tokens Used: 1250
Model: gpt-4

Requirements:
  - [FORMAT] Technical Proposal PDF Format
    Technical Proposal must be submitted in PDF format
    Blocker: true
  - [DOCUMENT] Past Performance References
    Past Performance references required (minimum 3 projects)
    Blocker: true
  - [DEADLINE] Response Deadline
    Response deadline: March 31, 2025, 3:00 PM KST
    Blocker: true
  - [ELIGIBILITY] 8(a) Certification Required
    Company must have 8(a) certification
    Blocker: true
```

### Failed Connection

```
AIServiceException: AI Service unavailable: Connection refused
```

**Solution**: Ensure Python AI Service is running on port 8001

### API Key Error

```
Python error: OpenAI API key not configured
```

**Solution**: Set `OPENAI_API_KEY` in `ai-service/.env`

## Troubleshooting

### Issue 1: Connection Refused

**Symptom:**
```
java.net.ConnectException: Connection refused
```

**Solutions:**
1. Check if Python service is running: `curl http://localhost:8001/health`
2. Verify port 8001 is not blocked: `lsof -i :8001`
3. Check `application.yml`: `app.ai.service.base-url`

### Issue 2: Read Timeout

**Symptom:**
```
java.net.SocketTimeoutException: Read timed out
```

**Solutions:**
1. Increase timeout in `application.yml`:
   ```yaml
   app:
     ai:
       service:
         read-timeout: 180000  # 3 minutes
   ```
2. Check OpenAI API response time
3. Use faster model: `gpt-3.5-turbo` instead of `gpt-4`

### Issue 3: Invalid JSON Response

**Symptom:**
```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
```

**Solutions:**
1. Verify Python response matches Java DTOs
2. Check Pydantic model definitions in `ai-service/app/models/responses.py`
3. Compare with Java DTOs in `backend/.../client/dto/`

### Issue 4: Empty Requirements

**Symptom:**
```
requirements: []
```

**Solutions:**
1. Check OpenAI API key is valid
2. Verify prompt template is loaded correctly
3. Test with more detailed opportunity text
4. Review Python logs for errors

## Performance Benchmarks

Expected performance metrics:

| Operation | Duration | Tokens Used | Model |
|-----------|----------|-------------|-------|
| Simple text (< 500 chars) | 2-5s | 500-1000 | gpt-4 |
| Medium text (500-2000 chars) | 5-10s | 1000-2500 | gpt-4 |
| Long text (2000+ chars) | 10-20s | 2500-5000 | gpt-4 |

**Note**: GPT-4 is slower but more accurate. For faster responses, use `gpt-3.5-turbo`.

## Next Steps

1. **Week 3**: Implement DocumentGeneratorAgent with Tool Calling
2. **Week 4**: Add ComplianceValidatorAgent with Vector DB
3. **Week 5**: Deploy with Docker Compose and add monitoring

## Integration with Domain Services

### Example: Auto-extract requirements on opportunity creation

**OpportunityService.java:**
```java
@Service
public class OpportunityService {

    private final AIServiceClient aiServiceClient;
    private final OpportunityRequirementRepository requirementRepository;

    @Transactional
    public Opportunity createWithAutoExtraction(OpportunityDTO dto, UUID userId) {
        // 1. Create opportunity
        Opportunity opportunity = createOpportunity(dto);

        // 2. Auto-extract requirements if text is provided
        if (dto.getFullText() != null && !dto.getFullText().isEmpty()) {
            try {
                RequirementExtractionRequest request = RequirementExtractionRequest.builder()
                    .opportunityId(opportunity.getId())
                    .opportunityText(dto.getFullText())
                    .metadata(Map.of("source", "sam.gov"))
                    .build();

                RequirementExtractionResponse response =
                    aiServiceClient.extractRequirements(request);

                // 3. Save extracted requirements
                List<OpportunityRequirementItem> requirements =
                    convertToEntities(response.getRequirements(), opportunity);

                requirementRepository.saveAll(requirements);

                log.info("Auto-extracted {} requirements for opportunity {}",
                    requirements.size(), opportunity.getId());

            } catch (AIServiceException e) {
                log.error("Failed to auto-extract requirements", e);
                // Don't fail the whole operation
            }
        }

        return opportunity;
    }
}
```

## Monitoring & Logging

### Java Side Logs

```
2025-02-11 14:30:15 [http-nio-8080-exec-1] INFO  AIServiceClient - Calling AI Service for requirement extraction: opportunityId=123e4567-e89b-12d3-a456-426614174000
2025-02-11 14:30:19 [http-nio-8080-exec-1] INFO  AIServiceClient - Requirement extraction successful: extracted 4 requirements in 3542ms
```

### Python Side Logs

```
INFO:     127.0.0.1:52345 - "POST /api/v1/requirements/extract HTTP/1.1" 200 OK
INFO:app.api.v1.requirements:Received requirement extraction request for opportunity: 123e4567-e89b-12d3-a456-426614174000
INFO:app.api.v1.requirements:Successfully extracted 4 requirements
```

## Security Considerations

1. **API Key Protection**: Never commit OpenAI API keys to Git
2. **Network Security**: Use HTTPS in production
3. **Authentication**: Add JWT token validation to Python service (Phase 2)
4. **Rate Limiting**: Implement rate limiting on both services
5. **Input Validation**: Validate all inputs to prevent injection attacks

## Deployment

### Docker Compose (Recommended)

```yaml
version: '3.8'

services:
  ai-service:
    build: ./ai-service
    ports:
      - "8001:8001"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - DATABASE_URL=${DATABASE_URL}
    networks:
      - bidding-network

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - AI_SERVICE_URL=http://ai-service:8001
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      - ai-service
    networks:
      - bidding-network

networks:
  bidding-network:
    driver: bridge
```

Start both services:
```bash
docker-compose up -d
```

## Conclusion

The Java-Python integration is now complete and tested. Key achievements:

✅ **Week 1**: Python AI Service setup with FastAPI
✅ **Week 2**: RequirementAnalyzerAgent implementation
✅ **Week 2**: Java REST Client integration
✅ **Week 2**: Integration tests and documentation

**Ready for Week 3**: DocumentGeneratorAgent with LangChain Tools!
