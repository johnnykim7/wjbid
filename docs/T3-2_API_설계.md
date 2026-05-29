# SAM.gov Bidding Agency Platform API 설계

> 설계 버전: 1.4 | 최종 수정: 2026-04-05 | 관련 CR: CR-001, CR-002, CR-003, CR-003a

> 단계: 3. Detail Design | 실행스펙 섹션 3에 포함
>
> 경로 + 메서드 + 한줄 설명 수준으로 작성한다.
> Request/Response 상세 스키마는 작성하지 않는다 — Claude Code가 데이터 모델을 보고 구현 시 자동 도출.

---

## 공통 사항

### 공통 라이브러리 참조

- **공통 라이브러리**: 없음 — 아래 직접 정의
- **참조 문서**: 없음

| 영역 | 공통 라이브러리 제공 여부 | 적용 방식 |
|------|------------------------|----------|
| 응답 포맷 (래퍼) | 아니오 | ApiResponse<T> 직접 정의 |
| 에러 코드 체계 | 아니오 | HTTP 상태코드 + message 필드 |
| 페이징 | 아니오 | Spring Data Page<T> |
| 인증/인가 | 아니오 | JWT + Spring Security |
| 로깅 | 아니오 | SLF4J |
| 검색/필터 | 아니오 | 쿼리 파라미터 기반 |

### API 기본 규격

- **Base URL**: `/api` (버전 프리픽스 없음, 기존 구현 유지)
- **인증**: 🔒 표시된 엔드포인트에 JWT Bearer 토큰 필요
- **응답 래퍼**: `ApiResponse<T>` — `{ success, data, message }`
- **페이징**: `Page<T>` — `{ content[], totalElements, totalPages, number, size }`
- **에러**: `{ success: false, message, errorCode }`

---

## A. 인증 (Auth) — Sprint 1

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| POST | /auth/register | 이메일 회원가입 (CUSTOMER 역할) | | BID-AUTH-001 |
| POST | /auth/login | 로그인 (JWT Access+Refresh 발급) | | BID-AUTH-002 |
| POST | /auth/refresh | Refresh 토큰으로 Access 토큰 갱신 | | BID-AUTH-003 |
| GET | /members/me | 내 정보 조회 | 🔒 | BID-AUTH-004 |
| PATCH | /members/me | 내 정보 수정 | 🔒 | BID-AUTH-004 |

---

## B. 공고 수집 (Opportunity Collection) — Sprint 2

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| POST | /admin/collection/trigger | 수동 수집 트리거 (daysBack 파라미터) | 🔒 ADMIN | BID-OPP-002 |
| GET | /admin/collection/status | 수집 상태 조회 | 🔒 ADMIN | BID-OPP-005 |
| GET | /admin/collection/runs | 수집 이력 목록 (페이징) | 🔒 ADMIN | BID-OPP-005 |

> 스케줄 수집(BID-OPP-001)은 cron으로 자동 실행 — API 없음

---

## B-2. 공고 관리 — Admin (Opportunity Admin) — CR-003

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /admin/opportunities | 공고 목록 (첨부파일 수, 분석 상태, 노출 상태 포함) | 🔒 ADMIN | BID-ADMIN-004 |
| GET | /admin/opportunities/{id} | 공고 상세 + 사전 분석 결과 | 🔒 ADMIN | BID-ADMIN-004 |
| GET | /admin/opportunities/{id}/analysis | 사전 분석 결과 상세 | 🔒 ADMIN | BID-OPP-006 |
| POST | /admin/opportunities/{id}/attachments | 첨부파일 수동 업로드 (multipart). **CR-019**: StorageService 실제 저장 → SUCCESS. `MANUAL_FETCH_REQUIRED` 첨부 있으면 그 행 갱신, 없으면 신규 | 🔒 ADMIN | BID-OPP-007 |
| POST | /admin/opportunities/{id}/analyze | 사전 분석 트리거 (수동) | 🔒 ADMIN | BID-OPP-006 |
| POST | /admin/opportunities/{id}/approve | 노출 승인 (HIDDEN → VISIBLE) | 🔒 ADMIN | BID-OPP-008 |
| POST | /admin/opportunities/{id}/hide | 노출 해제 (VISIBLE → HIDDEN) | 🔒 ADMIN | BID-OPP-008 |
| PATCH | /admin/opportunities/{id}/analysis | 사전 분석 결과 보정 (관리자 수정) | 🔒 ADMIN | BID-OPP-006 |

> 사전 분석은 첨부파일 다운로드 완료 시 자동 트리거됨. 수동 트리거는 재분석 또는 수동 업로드 후 사용.
> 승인(approve)은 OpportunityAnalysis 상태가 COMPLETED일 때만 가능.
> **CR-019 첨부 보강**: 관리자 원본 목록/상세 응답(OpportunityAdminDto)에 `manualFetchRequiredCount`(외부서 가져와야 할 첨부 수)를 포함해 "가져와야 함" 표식을 노출한다. 첨부 목록(`GET /admin/opportunities/{id}/attachments` 신규)은 각 첨부의 downloadStatus(SUCCESS/MANUAL_FETCH_REQUIRED 등)와 sourceUrl(외부 다운로드 링크)을 반환.

---

## C. 공고 열람 (Opportunity Browse) — Sprint 3

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /opportunities | 공고 목록 조회 (필터/검색/페이징). **사용자: visibility=VISIBLE만** (CR-003) | | BID-BROWSE-001 |
| GET | /opportunities/{id} | 공고 상세 조회 (사전 분석 결과 포함 — CR-003) | | BID-BROWSE-002 |
| GET | /opportunities/{id}/requirements | 공고 요구사항 목록 (카테고리별) | | BID-BROWSE-003 |
| GET | /opportunities/{id}/attachments | 공고 첨부문서 목록 | | BID-OPP-003 |
| GET | /opportunities/search | 키워드 검색 | | BID-BROWSE-001 |
| GET | /opportunities/search/organization | 기관명 검색 | | BID-BROWSE-001 |
| GET | /opportunities/near-deadline | 마감 임박 공고 | | BID-BROWSE-001 |
| GET | /opportunities/recent | 최근 등록 공고 | | BID-BROWSE-001 |

---

## D. 즐겨찾기 (Bookmark) — Sprint 3

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /bookmarks | 내 즐겨찾기 목록 (페이징) | 🔒 | BID-BROWSE-004 |
| GET | /bookmarks/{opportunityId}/status | 북마크 여부 확인 | 🔒 | BID-BROWSE-004 |
| POST | /bookmarks/{opportunityId} | 북마크 추가 | 🔒 | BID-BROWSE-004 |
| DELETE | /bookmarks/{opportunityId} | 북마크 삭제 | 🔒 | BID-BROWSE-004 |

---

## E. 자격 진단 (Qualification) — Sprint 3

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| POST | /opportunities/{id}/qualification-check | AI 자격 사전 진단 요청 | 🔒 | BID-QUAL-001 |

> Aimbase Workflow 비동기 호출. 결과는 폴링 또는 콜백으로 수신.

---

## F. 입찰 접수 (Bid Request) — Sprint 4

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| POST | /bid-requests | 입찰 참여 신청 (서비스 레벨 포함) | 🔒 | BID-REQ-001 |
| GET | /bid-requests/my | 내 입찰 목록 (페이징) | 🔒 | BID-REQ-003 |
| GET | /bid-requests/{id} | 입찰 상세 조회 | 🔒 | BID-REQ-004 |
| GET | /bid-requests/{id}/history | 상태 전이 이력 | 🔒 | BID-REQ-004 |
| GET | /bid-requests/{id}/documents | 입찰 문서 목록 | 🔒 | BID-REQ-004 |
| GET | /bid-requests/{id}/next-states | 허용 다음 상태 목록 | 🔒 | BID-FSM-001 |
| PATCH | /bid-requests/{id}/state | 상태 전이 실행 (고객: 취소만) | 🔒 | BID-FSM-001 |

---

## G. 고객 문서 제출 (Client Documents) — Sprint 4

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| POST | /bid-requests/{id}/client-documents | 문서 업로드 (multipart) | 🔒 | BID-REQ-002 |
| GET | /bid-requests/{id}/client-documents | 제출 문서 목록 | 🔒 | BID-REQ-002 |
| DELETE | /bid-requests/{id}/client-documents/{docId} | 제출 문서 삭제 | 🔒 | BID-REQ-002 |

### G-1. 요구사항 슬롯 (CR-010)

공고 분석으로 도출된 BLOCKER 요구사항을 슬롯으로 펼쳐서 1:1 업로드 매핑. DOCS_PENDING → DOCS_RECEIVED 전이의 사전 조건(BIZ-015).

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /bid-requests/{id}/required-document-slots | 의뢰의 요구사항 슬롯 목록 (각 슬롯: requirementItemId, title, description, isBlocker, status, mappedClientDocument 요약) | 🔒 | BID-REQ-002 |
| POST | /bid-requests/{id}/required-document-slots/{requirementItemId}/upload | 슬롯에 직접 업로드 (multipart). 한 트랜잭션에서 ClientDocument 생성 + RequirementFulfillmentMap(`CLIENT_DOCUMENT`, `FULFILLED`) 생성/업데이트 | 🔒 | BID-REQ-002 |
| DELETE | /bid-requests/{id}/required-document-slots/{requirementItemId} | 슬롯 매핑 해제. FulfillmentMap 삭제 (ClientDocument는 보존, 별도 DELETE 호출 시 제거) | 🔒 | BID-REQ-002 |

**응답 스키마 (GET 슬롯 목록)**:
```json
{
  "data": [
    {
      "requirementItemId": "uuid",
      "title": "Business License",
      "description": "Valid US business license issued within 12 months",
      "isBlocker": true,
      "category": "REGISTRATION",
      "status": "FULFILLED|PENDING|MISSING",
      "fulfillmentType": "CLIENT_DOCUMENT|DOCUMENT_SECTION|ATTACHMENT|null",
      "mappedClientDocument": { "id": "uuid", "fileName": "...", "fileSize": 12345, "uploadedAt": "..." } | null
    }
  ],
  "summary": { "totalBlocker": 5, "fulfilledBlocker": 3, "canTransitionToDocsReceived": false }
}
```

**전이 차단 응답 (PATCH /bid-requests/{id}/state to DOCS_RECEIVED 미충족 시)**:
- HTTP 409 Conflict
- body: `{ "error": "REQUIREMENT_SLOTS_NOT_FULFILLED", "unfulfilledSlots": [{ "requirementItemId": "uuid", "title": "..." }] }`

---

## H. 입찰 관리 — Admin (Bid Admin) — Sprint 4

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /admin/bid-requests | 전체 입찰 목록 (상태 필터/페이징) | 🔒 ADMIN | BID-ADMIN-003 |
| GET | /admin/bid-requests/stats | 상태별 통계 | 🔒 ADMIN | BID-ADMIN-003 |
| GET | /admin/bid-requests/requiring-client-action | 고객 액션 필요 건 | 🔒 ADMIN | BID-ADMIN-003 |
| POST | /admin/bid-requests/{id}/transition | 상태 전이 실행 (관리자) | 🔒 ADMIN | BID-FSM-001 |
| POST | /admin/bid-requests/{id}/assign | 담당자 배정 | 🔒 ADMIN | BID-ADMIN-003 |
| GET | /admin/bid-requests/assigned-to-me | 내 담당 입찰 목록 | 🔒 ADMIN | BID-ADMIN-003 |

---

## I. 문서 템플릿 관리 (Document Template) — Sprint 6

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /admin/document-templates | 템플릿 목록 | 🔒 ADMIN | BID-TPL-002 |
| GET | /admin/document-templates/{documentType} | 문서 유형별 템플릿 | 🔒 ADMIN | BID-TPL-002 |
| POST | /admin/document-templates | 템플릿 등록 | 🔒 ADMIN | BID-TPL-001 |
| PATCH | /admin/document-templates/{id} | 템플릿 수정 | 🔒 ADMIN | BID-TPL-001 |
| DELETE | /admin/document-templates/{id} | 템플릿 비활성화 (BIZ-010) | 🔒 ADMIN | BID-TPL-001 |

---

## J. 문서 편집 (Document Edit) — Sprint 7

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /bid-documents/{id} | 문서 상세 (현재 버전 포함) | 🔒 ADMIN | BID-EDIT-001 |
| GET | /bid-documents/{id}/versions | 버전 목록 | 🔒 ADMIN | BID-DOC-003 |
| GET | /bid-documents/{id}/versions/{versionNo} | 특정 버전 조회 | 🔒 ADMIN | BID-DOC-003 |
| POST | /bid-documents/{id}/versions | 새 버전 저장 (편집) | 🔒 ADMIN | BID-EDIT-001 |
| POST | /bid-documents/{id}/lock | 문서 잠금 (DRAFT → LOCKED) | 🔒 ADMIN | BID-EDIT-002 |
| POST | /bid-documents/{id}/unlock | 잠금 해제 (LOCKED → DRAFT) | 🔒 ADMIN | BID-EDIT-002 |
| GET | /bid-documents/{id}/export/pdf | PDF 내보내기 | 🔒 ADMIN | BID-EDIT-003 |

---

## K. 회원 관리 — Admin (Member Admin)

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /admin/members | 회원 목록 (페이징) | 🔒 ADMIN | BID-ADMIN-001 |

---

## L. 요금 안내 (Pricing) — Sprint 7

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /pricing | 서비스 레벨별 요금 정보 | | BID-PRICE-001 |

> MVP에서는 정적 데이터. 향후 관리자 설정 API 추가 가능.

---

## P. 성공 제안서 패턴 — Admin (RFP Pattern) — CR-013

### P-1. 성공 제안서 등록/조회

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /admin/rfp-samples | 등록 목록 (페이징, 메타 + 파일수) | 🔒 ADMIN | BID-RFP-001 |
| POST | /admin/rfp-samples | 성공 제안서 메타 등록 | 🔒 ADMIN | BID-RFP-001 |
| GET | /admin/rfp-samples/{id} | 상세 (메타 + 원본 파일들) | 🔒 ADMIN | BID-RFP-001 |
| POST | /admin/rfp-samples/{id}/files | 원본 파일 업로드 (multipart, 로컬 저장). isPws 쿼리로 PWS 구분 | 🔒 ADMIN | BID-RFP-001 |
| DELETE | /admin/rfp-samples/{id} | 등록 삭제 (파일 CASCADE) | 🔒 ADMIN | BID-RFP-001 |
| DELETE | /admin/rfp-samples/{id}/files/{fileId} | 파일 삭제 | 🔒 ADMIN | BID-RFP-001 |

> 🔄 **CR-013-R**: 슬롯 자동추정 제거. 원본은 통째 보관(파일명=섹션 태그).

**POST /admin/rfp-samples 요청 본문**:
```json
{
  "opportunityNo": "W90VN725RA012",
  "industryType": "GROUND_MAINTENANCE",
  "outcome": "WON",
  "company": "녹화창조", "agency": "군산 AB",
  "awardAmount": 3800000000, "fiscalYear": 2025
}
```

### ~~P-2. 슬롯 배치~~ — **CR-013-R 폐기**

> ❌ **폐기됨 (2026-05-29, CR-013-R).** `/admin/rfp-samples/{id}/slots/*` (getSlots/assign/unassign) 엔드포인트 전부 제거. 원본을 슬롯에 배치하지 않고 통째 보관.

### P-3. 패턴 추출/가이드 — **CR-013-R (공고유형 단위)**

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| POST | /admin/pattern-guides/{industryType}/extract | 공고유형 단위 패턴 추출 트리거 | 🔒 ADMIN | BID-RFP-003 |
| GET | /admin/pattern-guides | 전체 유형 가이드 목록 (status/source/sampleCount) | 🔒 ADMIN | BID-RFP-004 |
| GET | /admin/pattern-guides/{industryType} | 가이드 상세 (guideJson + markdown) | 🔒 ADMIN | BID-RFP-004 |
| PUT | /admin/pattern-guides/{industryType} | 수동 편집 → source=HUMAN_EDITED (자동추출 보호) | 🔒 ADMIN | BID-RFP-004 |

> 🔄 **CR-013-R**: 경로 키 `{slotCode}` → `{industryType}`. `POST .../extract`: 해당 유형 성공 제안서 1건 미만이면 400. 비동기 시작 → status=EXTRACTING 반환.
> Aimbase가 `save_pattern_guide` MCP 콜백으로 결과 저장.

---

## MCP 서버 (Aimbase 연동) — Sprint 5, CR-002 갱신

### 엔드포인트

| 메서드 | 경로 | 설명 | 비고 |
|--------|------|------|------|
| POST | /mcp | JSON-RPC 2.0 (HTTP 전송) | 기존. 직접 호출용 |
| GET | /mcp/sse | SSE 스트림 연결 | **CR-002 신규**. Aimbase 연결용 |
| POST | /mcp/message?sessionId={id} | SSE 세션 메시지 수신 | **CR-002 신규**. SSE 응답 반환 |

> SSE 전송: Aimbase가 `GET /mcp/sse`로 연결 → `endpoint` 이벤트로 메시지 URL 수신 → `POST /mcp/message`로 JSON-RPC 메시지 전송 → SSE `message` 이벤트로 응답 수신

### JSON-RPC 메서드

| 메서드 | 설명 |
|--------|------|
| initialize | MCP 서버 초기화 + 서버 정보 반환 |
| tools/list | 사용 가능한 Tool 목록 반환 |
| tools/call | 특정 Tool 실행 |

### MCP Tool 목록

| Tool 이름 | 설명 | 주요 입력 | 기능 ID |
|-----------|------|----------|---------|
| get_opportunity | 공고 상세 조회 | opportunityId | BID-MCP-002 |
| search_opportunities | 키워드로 공고 검색 | keyword, limit | BID-MCP-002 |
| get_opportunity_requirements | 공고 요구사항 목록 | opportunityId | BID-MCP-002 |
| save_requirements | LLM 추출 요구사항 저장 | opportunityId, requirements[] | BID-MCP-002 |
| get_bid_request | 입찰 요청 상세 + 문서 목록 | bidRequestId | BID-MCP-004 |
| save_document_version | AI 생성 문서 저장 | bidRequestId, documentType, contentJson | BID-MCP-003 |
| get_document_template | 문서 템플릿 조회 | documentType | BID-MCP-003 |
| transition_bid_state | 입찰 상태 전이 | bidRequestId, targetState | BID-MCP-004 |
| get_opportunity_analysis | 공고 사전 분석 결과 조회 (CR-003) | opportunityId | BID-MCP-002 |
| save_opportunity_analysis | 공고 사전 분석 결과 저장 (CR-003) | opportunityId, summary, documentFormats, requiredDocuments, llmPromptPreset | BID-MCP-002 |
| get_past_submissions | 과거 제출 이력 조회 (CR-003) | memberId, limit | BID-MCP-002 |
| get_reference_samples | 공고유형 매칭 성공 제안서 원본 파일 메타+다운로드URL 조회 (CR-013-R) | industryType | BID-RFP-003 |
| save_pattern_guide | 공고유형별 패턴 가이드 저장 — Aimbase 콜백 (CR-013-R) | industryType, guide{골격/체크리스트/금기} | BID-RFP-003 |

> 모든 MCP Tool은 무상태(BIZ-013). 각 호출은 독립적으로 처리.
> requestId를 멱등 키로 사용.

### Aimbase 워크플로우 호출 (CR-002 신규, BID-MCP-005)

플랫폼 → Aimbase 방향 호출. `LLMPlatformClient`가 담당.

| 용도 | Aimbase API | 메서드 | 비고 |
|------|-------------|--------|------|
| 워크플로우 실행 | `/api/v1/workflows/{id}/run` | POST | 비동기 시작 |
| 실행 결과 폴링 | `/api/v1/workflows/{id}/runs/{runId}` | GET | 3초 간격, 최대 60회 |

**인증**: `X-API-Key: plat-20cf57fbc623424584eeda2e355cbb43`

**설정 프로퍼티**:
```yaml
app.aimbase:
  base-url: ${AIMBASE_URL:http://59.8.160.12:8280}
  api-key: ${AIMBASE_API_KEY:plat-...}
  polling.interval-ms: 3000
  polling.max-attempts: 60
  workflows:
    requirement-extraction: requirement-extraction
    document-generation: bid-document-generation
    opportunity-analysis: ${AIMBASE_WF_OPP_ANALYSIS:...}
    slot-pattern-extraction: ${AIMBASE_WF_SLOT_PATTERN:placeholder-slot-pattern-workflow-id}  # CR-013
```

---

## M. FlowGuard 연동 (CR-001) — 인프라

| 메서드 | 경로 | 설명 | 인증 | 비고 |
|--------|------|------|------|------|
| GET | /health | 헬스체크 (DB 연결 포함) | | FlowGuard 5분 폴링 |
| GET | /validation/bid-requests/{id} | BidRequest 상태 조회 | | Probe용 읽기 전용 |
| GET | /validation/bid-requests?state= | 상태별 BidRequest 목록 | | Probe용 읽기 전용 |
| GET | /validation/opportunities/{id} | Opportunity 조회 | | Probe용 읽기 전용 |
| GET | /validation/bid-documents?bidRequestId= | BidDocument 목록 조회 | | Probe용 읽기 전용 |

> Validation API는 내부 네트워크 전용. 운영 환경에서는 방화벽/네트워크 정책으로 보호.
> X-Flow-Id 헤더는 FlowIdFilter에서 전체 요청에 대해 자동 수신·전파.

---

## API 요약

| 영역 | 엔드포인트 수 | Sprint |
|------|-------------|--------|
| A. 인증 | 5 | 1 |
| B. 공고 수집 (Admin) | 3 | 2 |
| C. 공고 열람 | 8 | 3 |
| D. 즐겨찾기 | 4 | 3 |
| E. 자격 진단 | 1 | 3 |
| F. 입찰 접수 | 7 | 4 |
| G. 고객 문서 제출 | 3 | 4 |
| H. 입찰 관리 (Admin) | 6 | 4 |
| I. 문서 템플릿 (Admin) | 5 | 6 |
| J. 문서 편집 | 7 | 7 |
| K. 회원 관리 (Admin) | 1 | — |
| L. 요금 안내 | 1 | 7 |
| M. FlowGuard 연동 | 5 | — (CR-001) |
| B-2. 공고 관리 (Admin) | 8 | CR-003 |
| P. 성공 제안서 패턴 (Admin) | 13 | CR-013 |
| MCP Tools | 13 | 5, CR-003, CR-013 |
| MCP SSE 엔드포인트 | 2 | CR-002 |
| Aimbase 워크플로우 호출 | 2 | CR-002 |
| **합계** | **REST 77 + MCP 13 + SSE 2 + Aimbase 2** | |
