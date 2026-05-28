# SAM.gov Bidding Agency Platform 변경 이력

> 설계 문서 변경 시 CR(Change Request)을 여기에 기록한다.
> 구현 후 추가 요구사항이 발생하면 origins에 원본 보관 + 여기에 CR 등록.

---

## CR 목록

| CR-ID | 날짜 | 요약 | 영향 범위 | 규모 | 상태 |
|-------|------|------|----------|------|------|
| (초기 설계) | 2026-03-19 | 역설계 + 신규 요구사항 통합 설계 | 전체 | 대규모 | 완료 |
| CR-001 | 2026-03-19 | FlowGuard 솔루션 연동 규격 구현 | BE (config, controller, common, integration) | 중규모 | 완료 |
| CR-002 | 2026-03-28 | Aimbase 연동 구현 (AI 파이프라인 활성화) | BE (integration, mcp, config, domain/bid) | 대규모 | 진행중 |
| CR-003 | 2026-03-28 | 공고 사전 분석 파이프라인 + LLM 입력 3파이프라인 | BE (domain/opportunity, domain/bid, integration) + FE (admin, portal) | 대규모 | 진행중 |
| CR-003a | 2026-04-05 | 고객 공고 상세에 AI 분석 결과 연동 + 스키마 확정 | BE (dto, controller) + FE (customer-portal) | 중규모 | 완료 |
| CR-004 | 2026-05-16 | 정제 출력 포맷 정형화 + 스키마 검증 강화 | BE (mcp, domain/opportunity) | 소~중규모 | 계획/논의중 |
| CR-005+006 | 2026-05-16 | bp-notification 통합 알림 (관리자 통보 + 고객 공고 알림) | BE (domain/notification, integration/notification, controller, config) + FE (customer-portal) | 중규모 | 코드 완료, 설계 캐스케이드/FlowGuard 미수행 |
| CR-007 | 2026-05-16 | 수집→정제 자동 트리거 | BE (event, integration/samgov, domain/opportunity) | 소규모 | 보류 |
| CR-008 | 2026-05-16 | 과거 샘플 본문 참조 보강 | BE (mcp) + Aimbase 워크플로우 | 소규모 | 계획/논의중 |
| CR-009 | 2026-05-28 | 수집 카운트 의미 정확화 + 관리자 공고목록 정렬 보정 | BE (integration/samgov, domain/opportunity, controller/admin) | 소~중규모 | 진단 완료, 구현 대기 |
| CR-010 | 2026-05-28 | 공고 요구서류 ↔ 고객 업로드 슬롯 매칭 | BE (domain/bid, domain/compliance, controller, MinIO) + FE (customer-portal) | 중규모 | 간이 설계 진행 중, 코드 구현 대기 |
| CR-011 | 2026-05-27 | SAM.gov 수집 키 운영 주입 정상화 + 신규 0건 메일 발송 스킵 | 운영(docker-compose.prod.yml) + BE (domain/notification) | 소규모 | 완료 |

> CR-004~008 원본: `docs/origins/원본_운영플로우_추가요구_20260516.md`
> 위 5건은 계획 등재만 — 각 CR 상세 설계는 해당 CR 착수 세션에서 진행. 본질 검토 결과 이미 충족된 항목(Draft+승인 / Aimbase 정제 / cron 스케줄링 / 가입형 고객 / 작성의뢰 / 맞춤 제안서 생성)은 CR 불필요.

---

## CR 상세

### 초기 설계 (2026-03-19)

- **배경**: 기존 구현된 입찰 대행 플랫폼을 AI-SDLC 방법론으로 역설계 + 8가지 기능 추가
- **주요 변경**:
  1. AI 연동: 자체 LLM Platform → Aimbase MCP 서버 아키텍처
  2. 역할 모델: 6개 역할 → ADMIN + CUSTOMER 2개
  3. FSM: 12개 상태 → 9개 상태
  4. 신규 기능: 이메일 알림, 첨부문서 획득, 자격 진단, 요금 안내 등
- **원본**: `docs/origins/원본_요구사항_기능추가.md`, `docs/origins/원본_요구사항_추가기획_20260319.md`
- **산출물**: T1-1 ~ T1-8, T2-1 ~ T2-2, T3-1 ~ T3-5, execution-spec.md 전체 생성

---

### CR-001: FlowGuard 솔루션 연동 규격 구현 (2026-03-19)

- **배경**: FlowGuard 솔루션 연동 규격(v1.3)의 필수 항목을 구현하여 비즈니스 검증 인프라 연동
- **참조 규격**: `~/Documents/GitHub/bp-platform/flowguard/docs/guides/flowguard-integration-guide.md`
- **주요 변경**:
  1. **X-Flow-Id 필터** — `FlowIdFilter` + `FlowIdContext`(ThreadLocal) 추가. 모든 요청에서 X-Flow-Id 헤더를 수신하여 MDC+ThreadLocal에 전파
  2. **헬스체크 엔드포인트** — `GET /health` (DB 연결 상태 포함, 무인증)
  3. **Validation API** — `GET /validation/bid-requests/{id}`, `GET /validation/bid-requests?state=`, `GET /validation/opportunities/{id}`, `GET /validation/bid-documents?bidRequestId=` (읽기 전용, 무인증)
  4. **Evidence 로깅** — `EvidenceLogger` 유틸리티. SAMGovApiClient에 적용 (SUCCESS/FAILURE/TIMEOUT 구조화 JSON 로그)
  5. **SecurityConfig** — `/health`, `/validation/**` permitAll 추가
  6. **manifest.yaml** — `flowguard/manifest.yaml` 솔루션 계약 선언
  7. **Step DSL JSON** — `flowguard/steps/` 6개 Step 백업
- **영향 설계 문서**:
  - T2-1 기술스택 결정서 — FlowGuard 연동 항목 추가
  - T3-2 API 설계 — FlowGuard 섹션 추가 (Health, Validation API)
- **빌드/테스트**: 92개 테스트 전체 통과

---

### CR-002: Aimbase 연동 구현 — AI 파이프라인 활성화 (2026-03-28)

- **배경**: `LLMPlatformClient`가 `localhost:9000`(존재하지 않는 레거시)을 호출하고 있어 실제 AI 기능이 동작하지 않음. Aimbase(`14.63.25.49:8280`)를 AI 엔진으로 연결하여 RFP 분석 → 요구사항 추출 → 제안서 생성 → 문서 출력 파이프라인을 활성화
- **원본**: `docs/origins/원본_Aimbase연동_구현계획_20260328.md`
- **주요 변경**:
  1. **LLMPlatformClient 리팩토링** — base URL을 Aimbase로 변경, `X-API-Key` 인증 추가, 응답 파싱 Aimbase 규격으로 조정
  2. **MCP SSE 전송 추가** — `GET /mcp/sse` + `POST /mcp/message` 엔드포인트 추가. Aimbase가 SSE로 MCP 도구 호출
  3. **McpDispatcher 추출** — 기존 McpServerController에서 dispatch 로직을 분리하여 HTTP/SSE 양쪽에서 공유
  4. **AIWorkflowService 간소화** — Aimbase가 MCP 도구를 직접 호출하므로 후처리 저장 로직 제거
  5. **순환 트리거 방지** — Aimbase → transition_bid_state → triggerAIWorkflow 재실행 방지 가드
  6. **문서 출력 책임 이관** — 최종 문서(Word/PDF) 생성은 Aimbase에서 처리. 포맷은 RFP마다 다름
  7. **Aimbase 셋업 스크립트** — LLM Connection, MCP Server 등록, Workflow 생성 자동화
- **Aimbase 연동 정보**: 테넌트 `bidding_system`, 도메인앱 `bidding`, API Key `plat-20cf57fbc623424584eeda2e355cbb43`
- **영향 설계 문서**:
  - T1-1 기능요구사항 v1.1 — BID-MCP-001 개선(SSE), BID-MCP-005 신규(워크플로우 실행), BID-DOC-001 개선(Aimbase 연동)
  - T2-1 기술스택 v1.2 — AI 연동 섹션 전면 갱신, 변경 대상 기술 추가
  - T3-2 API 설계 v1.2 — MCP SSE 엔드포인트, Aimbase 워크플로우 호출 명세 추가

---

### CR-003: 공고 사전 분석 파이프라인 + LLM 입력 3파이프라인 (2026-03-28)

- **배경**: 현재 LLM 분석은 BidRequest(입찰 신청) 생성 후 트리거됨. 동일 공고에 여러 사용자가 신청해도 매번 공고를 재분석하는 비효율 구조. 또한 공고 수집 후 관리자 승인 없이 바로 사용자에게 노출되어 품질 통제 불가. LLM 문서 작성 시 사용자 고유 데이터와 과거 이력을 활용하지 못해 퀄리티 한계
- **원본**: `docs/origins/원본_공고사전분석_파이프라인_20260328.md`
- **주요 변경**:
  1. **공고 사전 분석 파이프라인 신설** — 공고 첨부파일(PWS/RFP) 다운로드 완료 시 자동으로 LLM 분석 트리거. 요약본, 문서양식, 필요서류 목록, LLM 프롬프트 프리셋을 공고 단위로 1회 생성하여 캐시
  2. **Opportunity 노출 상태 관리** — `visibility` 필드 추가 (HIDDEN/VISIBLE). 사전 분석 완료 후 관리자 승인(VISIBLE)을 거쳐야 사용자에게 노출
  3. **관리자 첨부파일 수동 업로드** — SAM.gov에서 첨부파일이 없거나 다운로드 실패 시, 관리자가 직접 첨부파일을 업로드하고 분석 트리거
  4. **LLM 입력 3파이프라인 통합** — 문서 작성 시 입력 = ①공고 사전 분석 결과(캐시) + ②사용자 제출 서류 + ③과거 제출 이력. 3번 파이프라인은 빈 배열이어도 동일 구조로 전달 (조건 분기 없음)
  5. **OpportunityAnalysis 엔티티 신설** — 공고별 사전 분석 결과 저장 (요약, 필요서류, 양식, 프롬프트 프리셋)
  6. **BID-DOC-001 개선** — AIWorkflowService가 3파이프라인 데이터를 조합하여 Aimbase에 전달. 과거 이력은 조회해서 넘기면 끝 (비어있으면 빈 배열)
  7. **MCP Tool 추가** — `get_opportunity_analysis` (사전 분석 결과 조회), `get_past_submissions` (과거 제출 이력 조회)
- **설계 원칙**:
  - 공고 분석은 공고당 1회 — 여러 사용자가 신청해도 재분석 불필요
  - 과거 이력 조회에 조건 분기 불필요 — 조회해서 넘기면 됨, 비어있으면 빈 배열
  - 관리자 승인 전까지 공고는 사용자에게 비노출
- **영향 설계 문서**:
  - T1-1 기능요구사항 v1.2 — BID-OPP-006~008 신규, BID-DOC-001 개선, BID-MCP-002 확장, BID-ADMIN-004 신규
  - T1-5 FSM v1.1 — OpportunityAnalysis 상태, Opportunity visibility 상태 추가
  - T1-6 이벤트 v1.1 — OpportunityAnalysisCompleted, OpportunityApproved 이벤트 추가
  - T3-1 데이터 모델 v1.1 — OpportunityAnalysis 엔티티, Opportunity.visibility 필드
  - T3-2 API 설계 v1.3 — 관리자 공고 관리 API, MCP Tool 추가

---

### CR-003a: 고객 공고 상세에 AI 분석 결과 연동 + 스키마 확정 (2026-04-05)

- **배경**: CR-003에서 OpportunityAnalysis(사전 분석 결과) 저장까지 구현됨. 그러나 고객 화면에서는 SAM.gov 원본만 표시되어, 분석 결과(요약, 필요서류, 문서양식)가 활용되지 않음. 또한 4개 JSON 필드의 스키마가 미정의 상태
- **주요 변경**:
  1. **분석 결과 JSON 스키마 확정** — summaryJson(overview/scope/eligibility/evaluationCriteria/keyDates/budgetInfo/specialNotes), requiredDocumentsJson(documents[{name,mandatory,format,pageLimit}]), documentFormatsJson(generalInstructions/formats[{section,pageLimit,fileFormat}]/submissionMethod)
  2. **AnalysisResultDto 신규 생성** — Map<String,Object>에서 타입 안전하게 추출하는 record DTO. SummaryDto, RequiredDocumentsDto, DocumentFormatsDto 내부 record 포함
  3. **OpportunityDto 확장** — `analysis` 필드 추가. `from(Opportunity, OpportunityAnalysis)` 오버로드. 목록 API는 analysis=null (N+1 방지)
  4. **OpportunityController 수정** — `GET /opportunities/{id}` 에서 분석 결과 조회하여 응답에 포함
  5. **MCP Tool inputSchema 상세화** — save_opportunity_analysis의 summary/documentFormats/requiredDocuments에 properties 정의 추가. AI 워크플로우가 일관된 구조로 저장하도록 유도
  6. **고객 BidDetailPage 조건부 렌더링** — 분석 완료 시: AI 분석 배지 + 공고 요약 + 주요 일정 + 필요 서류 체크리스트 + 문서 양식 안내 + 첨부 파일 + SAM.gov 원본(접이식). 미완료 시: 기존 원본 표시(fallback)
- **영향 설계 문서**:
  - T3-2 API 설계 v1.4 — `GET /opportunities/{id}` 응답에 사전 분석 결과 포함 명시
  - T3-3 화면 구조 v1.1 — 공고 상세 화면 영역 갱신 (AI 분석 7개 섹션 + fallback 설명)

---

### CR-005+006: bp-notification 통합 알림 (2026-05-16)

- **배경**: CR-005(관리자 통보)와 CR-006(고객 공고 알림 구독+발송)을 별도 항목으로 계획했으나, 발송 인프라(bp-notification 운영 서버 `59.8.160.12:8185`)·채널(이메일+인앱)·도메인(`bidding_system`)이 동일하므로 통합 구현. 사용자 지시로 설계 캐스케이드·FlowGuard 등록·CR 이력 등재는 코드 완료 후 일괄.
- **확정 사항**: 알림 대상=전체 CUSTOMER(`memberRepository.findByRole`), 채널=이메일+인앱(`NotificationLog`에 `readAt` 추가), 발송 인프라=bp-notification(SMTP 폐기, fallback 없음)
- **외부 의존 (bp-notification 운영, 처리 완료)**:
  - BIDDING 솔루션 id=6, API Key 재발급 `b8c793a1-...`(bcrypt 저장으로 재조회 불가, 분실 시 재재발급). `application.yml` 기본값에 박힘, 운영은 `${BP_NOTIFICATION_API_KEY}` 권장
  - 신규 템플릿 `BID_OPPORTUNITY_APPROVED`(id=50) 등록 + 실발송 검증 완료. 변수: customerName / opportunityTitle / noticeId / deadline / detailUrl
  - 기존 7종(BID_COLLECTION_DONE / BID_REQUEST_ADMIN / BID_DOC_GENERATED / BID_ANALYSIS_DONE / BID_DEADLINE_ALERT 등)에 `NotificationType` 매핑됨. `AI_WORKFLOW_FAILED`는 전용 템플릿 없어 `BID_ANALYSIS_DONE` 임시 매핑(실호출 경로 없음)
- **주요 변경**:
  1. **bp-notification 클라이언트 인프라** — `BpNotificationConfig`(RestTemplate + 인터셉터), `integration/notification/BpNotificationClient`(`sendEmail` REST 호출)
  2. **NotificationService 전환** — 기존 SMTP 직발송에서 bp-notification REST 호출로 전환. 멱등성(BIZ-012)은 그대로 유지
  3. **NotificationType 확장** — 9종 + `templateCode` 매핑 추가. `OPPORTUNITY_APPROVED` 신규
  4. **NotificationEventListener 5리스너 전환** — `onOpportunitiesCollected`, `onBidRequestCreated`, `onDeadlineApproaching`, `onDocumentGenerated`, `onOpportunityAnalysisCompleted` 모두 bp-notification 기반으로 전환 + `onOpportunityApproved` 신규(고객 공고 알림 — VISIBLE 승인 시 전체 CUSTOMER에게 발송)
  5. **인앱 알림 조회 API** — `NotificationController` + `NotificationQueryService` + `NotificationDto`. `GET /notifications`(미읽음/전체), `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all`. `NotificationLog.markAsRead()`로 readAt 기록
  6. **DB 마이그레이션 V9** — `notification_log` 테이블에 `read_at` 컬럼 + 인덱스 추가
  7. **FE customer-portal 인앱 알림 UI** — `NotificationPanel`(헤더 벨 드롭다운), `NotificationsPage`(목록 페이지), `useNotifications` 훅. `GlobalNav`의 벨 활성화
  8. **application.yml** — `app.bp-notification.*`(base-url / api-key / solution-id) + `app.customer-portal-url` 추가
- **영향 설계 문서 (갱신 예정, 미수행)**:
  - T1-1 기능요구사항 — `BID-NOTIF-*` 항목 갱신(템플릿 명세, 채널)
  - T1-6 이벤트 계약 — `OpportunityApproved` 이벤트 신규
  - T2-1 기술스택 — bp-notification 연동 항목 추가
  - T3-2 API 설계 — `/notifications/*` 엔드포인트
  - T3-3 화면 구조 — 헤더 벨, 알림 페이지
- **미수행 (의도적 보류 — 후속 작업)**:
  1. 설계 캐스케이드 (T1→T2→T3→execution-spec) — 중규모 규칙
  2. FlowGuard 등록 (신규 기능 권유 대상, /api/workspaces 500 재시도 필요)
  3. 운영 재배포 검증(이번 CR-011 배포에 포함됐을 가능성 — 추후 별도 확인)
- **참조**: 메모리 [[cr005-006-implemented-pending-cascade]] (2026-05-16 작성, 11일 후 본 항목으로 정본 등재)

---

### CR-009: 수집 카운트 의미 정확화 + 관리자 공고목록 정렬 보정 (2026-05-28)

- **배경**: 스케줄러가 발송한 수집 완료 메일에 "신규 52건 / 갱신 3850건"이 통보됐으나, 관리자 화면에서 신규/갱신 공고가 상단에 보이지 않음. 실측 결과 두 가지 결함 확인:
  1. `OpportunityCollectionScheduler.collectOpportunities()` 가 `totalDuplicates`(이미 DB에 존재했던 모든 건수)를 `OpportunitiesCollectedEvent.updatedCount` 자리에 그대로 넣어 메일 발송 → 사용자는 "실제로 내용이 변경된 건수"로 이해하지만 실제는 "이번 수집에서 기존에 존재했던 건수"
  2. `GET /admin/opportunities` 가 `@PageableDefault(size = 20)` 만 지정, FE도 sort 미전달 → JPA default order 미정의. 신규/갱신 공고가 목록 상단으로 올라오지 않음
- **사용자 인식과 실측 결과의 차이**:
  - 사용자 인식: "갱신 3850건이 비현실적" + "화면이 갱신 안됨"
  - 실측: 카운트 의미 자체가 잘못 매겨짐(실제 contentHash 변경 건수는 어디서도 안 셈) + 관리자 화면은 HIDDEN 포함 노출이 맞지만 정렬이 없어 신규/갱신이 묻힘
  - 사용자 포털(HomePage) 비노출은 CR-003 정책상 정상(승인 전 HIDDEN)
- **주요 변경 (계획)**:
  1. **카운트 의미 분리** — `OpportunityCollectorService.CollectionResult` 에 `changed`(contentHash 변경 발생) 카운트 추가. `OpportunityService.createOrUpdate()` 반환을 boolean isNew → enum/record(NEW/CHANGED/UNCHANGED)로 변경
  2. **이벤트 페이로드 의미 정확화** — `OpportunitiesCollectedEvent.updatedCount` 가 실제 "내용 변경 발생 건수"가 되도록 수정. 필요시 `fetchedCount`(전체 받아온 건수) 별도 필드 추가
  3. **관리자 컨트롤러 정렬 기본값** — `OpportunityAdminController.listOpportunities` 의 `@PageableDefault` 에 `sort = "lastModifiedAt", direction = DESC` 추가. 신규+실제 변경분이 자연스럽게 상단에 오도록
  4. **메일 템플릿 변수 의미 재확인** — bp-notification 의 `COLLECTION_COMPLETE` 템플릿이 신규/갱신 의미를 정확히 전달하는지 검증
- **영향 범위 (예상)**:
  - BE: `OpportunityCollectorService`, `OpportunityService`, `OpportunityCollectionScheduler`, `OpportunitiesCollectedEvent`, `NotificationEventListener`, `OpportunityAdminController`
- **영향 설계 문서 (갱신 예정)**:
  - T1-6 이벤트 계약 — `OpportunitiesCollected` 이벤트 필드 의미 명확화
  - T3-2 API 설계 — `/admin/opportunities` 기본 정렬 명시
- **상태**: 진단 완료, 구현 대기 (사용자 승인 후 착수)

---

### CR-010: 공고 요구서류 ↔ 고객 업로드 슬롯 매칭 (2026-05-28)

- **배경**: 입찰 의뢰 시 공고가 요구하는 서류(예: Business License, Past Performance, SAM Registration)가 화면에 슬롯으로 펼쳐지지 않고, 고객이 자유 카테고리 문자열로 추측 업로드하는 구조. 어느 파일이 어느 요구사항을 충족했는지 추적 불가하여 BLOCKER 게이트가 동작하지 않음.
- **실측 진단 (이전 인식 정정)**:
  - 신규 구조 추가가 아니라 **기존 `RequirementFulfillmentMap`에 `ClientDocument` 연결을 더하는 작업**임이 코드 실측으로 확인됨
  - `OpportunityRequirementItem`(`is_blocker`), `RequirementFulfillmentMap`(`status`, `FulfillmentType`), `ComplianceService.validateBidRequest()` 모두 이미 구현됨. 단, 현재 `RequirementFulfillmentMap.document`는 `BidDocument`(AI 생성 문서)에만 연결 가능하고 `ClientDocument`(고객 업로드)에는 연결 못 함 ([RequirementFulfillmentMap.java:44-46](../backend/src/main/java/com/biddingagency/domain/compliance/entity/RequirementFulfillmentMap.java#L44-L46))
  - FE도 `ProposalDetailPage.tsx`에 "제출 서류" 탭이 이미 있으나 슬롯이 아닌 단순 파일 리스트 ([ProposalDetailPage.tsx:241-300](../frontend/customer-portal/src/pages/ProposalDetailPage.tsx#L241-L300))
  - 공고 요구서류 분석 결과(`OpportunityAnalysis.requiredDocumentsJson`)는 이미 채워지나 의뢰 화면에서 슬롯화되지 않음
- **본 CR 범위 (사용자 승인 옵션)**:
  - 서류 모델: **공고 요구사항 슬롯에만 매핑** (회사 공용 서류 자동 첨부는 별도 CR로 분리)
  - 게이트 시점: **DOCS_PENDING → DOCS_RECEIVED 전이 시 강제** — 모든 BLOCKER 슬롯 충족 미달이면 전이 차단
  - 작업 범위: **BE 구조 + 게이트 + FE 슬롯 UI 전부** (MinIO 실업로드 포함)
- **변경 사항 (예정)**:
  1. **데이터 모델 (T3-1)**: `requirement_fulfillment_maps`에 `client_document_id BINARY(16)` 컬럼 추가 (nullable, FK→`client_documents.id`). `FulfillmentType` enum에 `CLIENT_DOCUMENT` 값 추가
  2. **비즈니스 규칙 (T1-3)**: BIZ-015 신설 — "DOCS_PENDING → DOCS_RECEIVED 전이는 BLOCKER 요구사항 슬롯이 모두 CLIENT_DOCUMENT/DOCUMENT_SECTION/ATTACHMENT로 충족되어야 허용"
  3. **FSM (T1-5)**: DOCS_PENDING → DOCS_RECEIVED 전이에 ComplianceService.validateClientDocumentSlots() 사전 가드 명시
  4. **API (T3-2)**:
     - `GET /bid-requests/{id}/required-document-slots` — 의뢰의 BLOCKER 요구사항 슬롯 + 현재 매핑 상태 조회
     - `POST /bid-requests/{id}/required-document-slots/{requirementId}/upload` — 슬롯에 직접 업로드 (multipart, 업로드와 동시에 FulfillmentMap 생성/업데이트)
     - `DELETE /bid-requests/{id}/required-document-slots/{requirementId}` — 슬롯 매핑 해제 (파일은 ClientDocument로 남음)
  5. **화면 (T3-3)**: ProposalDetailPage Uploads 탭을 슬롯 기반 UI로 개편 — 요구사항별 카드, 미충족 BLOCKER 강조, "DOCS_RECEIVED로 진행" 버튼은 모든 BLOCKER 충족 시에만 활성화
  6. **MinIO 실업로드**: `ClientDocumentController.upload`의 placeholder를 실제 MinIO 업로드로 교체 (storage_url placeholder 제거)
- **영향 범위 (예상)**:
  - BE: `RequirementFulfillmentMap`, `FulfillmentType`, `ComplianceService`(슬롯 검증 메서드 신설), `BidFSMService`(DOCS_PENDING→DOCS_RECEIVED 가드), `ClientDocumentController`(MinIO 연동 + 슬롯 업로드 엔드포인트), 새 `RequiredDocumentSlotController`
  - FE: `ProposalDetailPage.tsx` Uploads 탭 슬롯화, `api/client.ts` 신규 엔드포인트, 신규 슬롯 카드 컴포넌트
  - 마이그레이션: Flyway V{n}__add_client_document_to_fulfillment.sql
- **영향 설계 문서 (갱신 예정)**: T1-3, T1-5, T3-1, T3-2, T3-3
- **상태**: 진단 완료, 간이 설계 캐스케이드 진행 중 (T1-3/T1-5/T3-1/T3-2/T3-3 갱신 후 commit, 코드 구현은 별도 승인)

---

### CR-011: SAM.gov 수집 키 운영 주입 정상화 + 신규 0건 메일 발송 스킵 (2026-05-27)

- **배경**: 사용자가 "스케줄러는 도는데 SAM.gov에서 항상 0건만 가져온다"고 보고. 실측 결과 두 가지 결함이 동시에 작용:
  1. 운영 컨테이너(`docker-compose.prod.yml`)의 `environment:` 블록에 `SAM_GOV_API_KEY` 환경변수 자체가 누락되어 있어, `application.yml`의 기본값 `your-api-key-here`가 적용되며 SAM.gov 호출이 항상 401 `API_KEY_INVALID` 응답 → 수집 실패 → DB 신규 0건
  2. 신규 0건이어도 ADMIN에게 "공고 수집 완료" 메일이 매번 발송됨 (사용자 정책: "이미 DB에 있는 건만 잡혔으면 메일 안 보낸다")
- **실측 근거**:
  - 기존 키 `SAM-b3661f0d-...` 로 SAM.gov 직접 호출 → `HTTP 401 API_KEY_INVALID`
  - 신규 발급 키 `SAM-cc4923d9-...` 로 동일 호출 → `HTTP 200`, `totalRecords: 24966`
  - `docker-compose.prod.yml` 실측: `environment:` 블록에 SAM 관련 변수 없음
  - SAM.gov UI에서 `All Words "411th csb"` 검색 시 진짜 한국 주둔 미군 공고 250건 잡힘 (W90VN926QA040 CJLOTS, W90VN926QA015 Camps Henry/George/Walker, W91QVN26QA019 USAG Humphreys 등)
- **주요 변경**:
  1. **`docker-compose.prod.yml`** — `environment:` 블록에 `SAM_GOV_API_KEY: SAM-cc4923d9-011d-4a7f-9202-c60fcf028572` 추가 (1줄). 옵션 A(현행 평문 관행, 기존 `DB_PASSWORD`도 평문이라 일관성 우선) 적용
  2. **`NotificationEventListener.onOpportunitiesCollected`** — 메서드 맨 앞에 `newCount == 0` 가드 추가. 신규 0건이면 `log.info("Skipping COLLECTION_COMPLETE notification: 0 new opportunities")` 찍고 즉시 return. ADMIN 조회·발송 모두 스킵
  3. **단위 테스트** — `NotificationEventListenerTest.onOpportunitiesCollected_신규0건_스킵` 추가. `OpportunitiesCollectedEvent(null, 0, 12, 12)` 입력 시 `memberRepository`/`notificationService` 모두 `shouldHaveNoInteractions()` 검증
- **정책 결정 (사용자 확정)**: "신규 0건이면 무조건 미발송" (옵션 A) 채택. `updatedCount`(이미 있던 공고의 변경분)는 별도 판단 없이 함께 스킵 — 변경분 알림이 필요하면 별도 이벤트 종류로 분리하는 것이 깔끔하다는 판단. CR-009에서 "변경 카운트 의미 자체"를 다시 정의할 예정
- **CR-009와의 관계**: CR-009는 "수집 카운트 의미 자체"를 고치는 별개 작업(진단 완료/구현 대기). 본 CR은 "현재 의미 그대로의 newCount=0 가드"만 추가. CR-009 본 구현 시 본 가드 로직을 재검토하여 `newCount + changedCount == 0` 형태로 확장 여부 결정
- **검증**:
  - 컨테이너 내부 환경변수 주입 확인: `docker compose exec ... printenv SAM_GOV_API_KEY` → `SAM-cc4923d9-...` 정상 출력
  - 운영 서버 health: `curl http://localhost:8183/api/actuator/health` → `{"status":"UP"}`
  - 단위 테스트: `./gradlew test --tests NotificationEventListenerTest.onOpportunitiesCollected_신규0건_스킵` → BUILD SUCCESSFUL
  - 컨테이너 재기동 완료, 다음 자동 스케줄(23:00 KST)부터 신규 코드 동작 시작
- **영향 설계 문서**:
  - 별도 캐스케이드 없음 (소규모 — 운영 환경변수 보정 + 메서드 앞 1행 가드. T1~T3 설계 변경 없음)
- **별건으로 식별 (본 CR 범위 밖, 추후 CR 후보)**:
  - SAM.gov REST API `q=411th csb` 결과가 UI(250건) 대비 100배(24,966건) 노이즈 매칭 — exact phrase / federal organization 필터 도입 검토
  - 외부에서 `woojinusbid.com/api/actuator/health` 미접근 (서버 내부에서만 200) — 리버스 프록시/방화벽 점검
  - NotificationEventListenerTest의 기존 4개 케이스가 `findByIdWithDetails` mock 누락으로 NPE — 본 CR에서 컴파일 통과만 시키고 검증 강도 일부 다운그레이드(`contains` → `any`). 단위 테스트 보강 별도 CR 필요
- **참여 도구/시간**: 사용자가 SAM.gov 신규 키 발급(`SAM-cc4923d9-...`) → Claude가 `.env` + `docker-compose.prod.yml` 갱신 → `./deploy.sh be` 2회 (1차: 키 주입, 2차: 0건 가드) → health check + 컨테이너 env 확인까지 완료
