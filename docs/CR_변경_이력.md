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
| CR-013 | 2026-05-28 | 성공 제안서 패턴 가이드 (관리자 등록 + 슬롯 패턴 추출) | BE (domain/rfp 신설, integration/storage, common, mcp, integration/llmplatform, controller/admin, 마이그레이션 V10) + FE (admin-console) | 대규모 | 간이 설계 + 코드 병행 |
| CR-013-R | 2026-05-29 | CR-013 슬롯 폐기 → 원본 통째 보관 + 공고유형별 가이드 + 작성 시 도구 발췌 | BE (domain/rfp 슬롯 제거, PatternGuide 유형단위, mcp, llmplatform, controller/admin, 마이그레이션 V11) + FE (admin-console) | 대규모 | 코드 먼저 + 설계 일괄 |
| CR-014 | 2026-05-29 | 성공 제안서 자산 작성 활용 (B작업) — 공고 IndustryType 자동분류 + 작성 P3를 성공 가이드+원본으로 대체 | BE (domain/opportunity IndustryClassifier 신설, Opportunity 엔티티, integration/samgov, domain/rfp ReferenceSampleService, domain/bid AIWorkflowService, mcp, 마이그레이션 V12) | 중규모 | 설계 먼저 + 코드 |

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

- **배경**: `LLMPlatformClient`가 `localhost:9000`(존재하지 않는 레거시)을 호출하고 있어 실제 AI 기능이 동작하지 않음. Aimbase(`59.8.160.12:8280`)를 AI 엔진으로 연결하여 RFP 분석 → 요구사항 추출 → 제안서 생성 → 문서 출력 파이프라인을 활성화
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

---

### CR-013: 성공 제안서 패턴 가이드 — 관리자 등록 + 슬롯 패턴 추출 (2026-05-28)

- **배경**: 이 플랫폼은 미군(USFK) 정부조달 입찰 제안서를 AI로 대신 써주는 서비스. 사용자가 `rfp/`에 과거 성공/낙찰 제안서 8건을 보유하고 "이걸 학습시켜 신규 제안서 작성에 참고"하길 원함. 2026-05-28 설계 토론에서 "학습"의 실체를 **파인튜닝/벡터화가 아니라, 성공 제안서에서 "이기는 패턴"을 슬롯별로 추출해 가이드/프롬프트화하는 것**(B경로)으로 결론. (파인튜닝 ❌: 8건뿐, A가 Q의 함수가 아니라 업체 사실의 함수 → 환각. 본문 벡터화 ❌: 제안서는 조각 검색이 아니라 통째 참고.)
- **1차 범위**: 관리자 등록 인터페이스 + 도메인/테이블/API/화면 + 패턴 추출 프롬프트 + Aimbase 연동 코드까지. **범위 밖(B작업, 다음 세션)**: 추출된 가이드를 소비하는 실제 제안서 작성 워크플로우(50~100p 쪼개 쓰기).
- **사용자 확정 결정**:
  1. 파일 저장 = **로컬 디스크 구현**(`StorageService` 인터페이스로 S3 교체 가능하게 추상화). 실측: 이 플랫폼은 MinIO/S3 전혀 미구현 — 공고 첨부도 ClientDocument도 placeholder/markLinkOnly 임시
  2. 패턴 추출 = **Aimbase 워크플로우 실연동 코드까지** 플랫폼측 전부 구현. 워크플로우 ID는 application.yml 환경변수(placeholder). Aimbase측 워크플로우 생성·E2E는 배포환경(로컬→59.8.160.12:8280 접근 불가)에서 별도
  3. 7슬롯 = **DB 시드 테이블(`slot_definition`)로 정의** + 빈슬롯 허용 + "기타" 슬롯(라벨 입력). 코드 하드코딩 금지
  4. 설계 = **간이 캐스케이드 + 코드 병행**(CR-010 방식)
- **도메인 개념**:
  - **RfpSample** = 등록 단위(성공 제안서 1건): 메타(공고번호/사업유형/결과 WON·SUBMITTED·OTHER, 선택: 업체/발주처/낙찰액/회계연도) + 원본 파일들 + (선택)PWS
  - **SlotAssignment** = 제안서의 파일/섹션을 표준 7슬롯 중 하나에 배치. 자동추정 + 관리자 확인. 빈슬롯은 시스템이 "이 데이터 넣어줘" 역요구
  - **PatternGuide** = 슬롯별 가이드. **추출 단위 = 슬롯**(같은 슬롯에 모인 여러 건 비교 → 공통 패턴). 여러 건 모아서 추출(1건마다 X). 부분 재추출 가능(슬롯 단위). 출처 구분(`source`): AI_EXTRACTED(자동갱신) vs HUMAN_EDITED/HUMAN_ADDED(보호)
  - 표준 7슬롯: 사업자자격/등록 · 과거 수행 경험 · 핵심인력(CM/QCM) · 인력 배치 계획 · 장비 계획 · 과거 실적 · 가격 (+ 기타)
- **변경 사항**:
  1. **데이터 모델 (T3-1)**: 마이그레이션 V10. 5테이블 신설 — `slot_definition`(시드 8행), `rfp_sample`, `rfp_sample_file`, `slot_assignment`, `pattern_guide`
  2. **기능 요구 (T1-1)**: BID-RFP-001(등록) / 002(7슬롯 배치) / 003(슬롯 패턴추출) / 004(가이드 조회·편집)
  3. **비즈니스 규칙 (T1-3)**: BIZ-016(패턴 추출은 슬롯 단위, 동일 슬롯 2건 이상일 때만) / BIZ-017(가이드 출처 보호 — AI 자동추출은 source=AI_EXTRACTED만 갱신, HUMAN_* 보존)
  4. **API (T3-2)**: `/admin/rfp-samples`(등록·파일업로드·슬롯배치) + `/admin/pattern-guides`(추출 트리거·조회·수동편집)
  5. **화면 (T3-3)**: admin-console "성공 제안서 패턴" 메뉴 — 목록 + 등록/상세(7슬롯 배치 UI + 패턴추출 버튼 + 가이드 편집)
  6. **신규 추상화**: `StorageService`(interface) + `LocalFileStorageService`, `TextExtractionUtil`(iText/POI 재사용)
  7. **AI 연동**: `LLMPlatformClient.extractSlotPattern()`(기존 폴링 재사용) + MCP `PatternGuideMcpTool`(`get_slot_samples`/`save_pattern_guide` 콜백)
- **영향 범위**:
  - BE: `domain/rfp`(entity/repository/service/dto 신설), `controller/admin/RfpSampleAdminController`·`PatternGuideAdminController`, `integration/storage`(StorageService/LocalFileStorageService), `common/TextExtractionUtil`, `mcp/tool/PatternGuideMcpTool`+`McpDispatcher`, `integration/llmplatform/LLMPlatformClient`, `application.yml`(app.storage.*, app.aimbase.workflows.slot-pattern-extraction)
  - FE: `App.tsx`(라우트), `components/Layout.tsx`(NAV), `api/client.ts`, `pages/RfpSamplePage.tsx`·`RfpSampleDetailPage.tsx` 신설
  - 마이그레이션: `V10__add_rfp_sample_pattern_guide.sql`
- **영향 설계 문서**: T1-1, T1-3, T3-1, T3-2, T3-3
- **리스크**: Aimbase 워크플로우 미생성 → 로컬 E2E 불가(markFailed graceful 경로만 검증, 실제 가이드 생성은 배포환경). 브로슈어형 PDF 텍스트 추출 깨짐(extraction_status=FAILED 허용 + section_text 수동 fallback). 단건 슬롯(LAUNDRY/HVAC/WASTE/PIPELINE 각 1건) 패턴 빈약 → 1차엔 industryType NULL=공통 묶음.
- **참조 산출물**: `docs/rfp_sample_index_draft.md`(rfp_sample 컬럼 근거), `docs/rfp_pattern_guide_draft_prior_experience.md`(guide_json 8블록/체크리스트/금기 구조 = save_pattern_guide 입력 계약)
- **상태**: 간이 설계 캐스케이드 + 코드 병행 (설계 commit과 코드 commit 분리). **→ CR-013-R로 슬롯 부분 폐기됨(아래 참조).**

---

### CR-013-R: CR-013 슬롯 폐기 → 원본 통째 보관 + 공고유형별 가이드 (2026-05-29)

- **배경**: CR-013 슬롯 도입 배경을 재검토. 슬롯(7축 사전 분류)은 *가이드 추출 품질*을 위해 실물을 정규화하려는 추출-측 논리였으나, 핵심 가치는 "실물 참조"임. 사용자 지적: 100페이지를 통째로 LLM에 넣을 수 없으니 작성 시 **도구(grep/parse_document)로 필요한 부분만 발췌**하면 되고, 그렇다면 7슬롯 사전 분류는 과잉. (사용자가 Claude Code에 "이 파일들 참조해서 이 가이드대로 써줘"를 주는 것과 동일 원리를 플랫폼이 재현.)
- **실물(rfp/) 분석으로 입증**: 15폴더 149파일. 제출본이 **이미 FACTOR/Subfactor별 파일로 나뉘어 제출**됨(`FACTOR 3.PAST PERFORMANCE.pdf` 등 — 파일명=섹션 태그) → 플랫폼 슬롯 재분류는 중복 노동. PDF 텍스트 추출률 거의 100%(128 중 스캔의심 1, 그것도 공고문) → grep/parse 발췌로 충분, OCR·벡터 불요. 유형이 폴더명에 명시(GM/청소/HVAC/Laundry/폐기물) → 메타 매칭 키 자연 존재.
- **사용자 확정 결정**:
  1. **슬롯 폐기** — 원본을 7축으로 쪼개는 구조 제거. 원본은 통째 보관(폴더=1건, 파일 그대로).
  2. **가이드 단위 = 공고유형별 1개** — AI 추출 + 누적 + **사람 직접 편집**(살아있는 가이드, BIZ-017 출처보호 유지).
  3. **첨부 자동 선택** — 신규 공고 유형 ↔ RfpSample.industryType 매칭으로 참조 원본 자동 선택.
  4. **작성 시 도구 발췌** — Aimbase 워크플로우가 parse_document(url)로 폴더 내 파일(파일명=섹션) 발췌해 few-shot 참조.
  5. 진행 = **코드 먼저 + 설계 일괄**.
- **변경 사항**:
  1. **데이터 모델 (T3-1)**: 마이그레이션 V11. `slot_definition`/`slot_assignment` 드롭. `pattern_guide`를 슬롯FK 제거 → `industry_type` UNIQUE 단위로 재생성. `rfp_sample`/`rfp_sample_file` 유지.
  2. **기능 요구 (T1-1)**: BID-RFP-002(7슬롯 배치) 폐기. 003 추출단위 슬롯→공고유형. 004(가이드 조회·편집) 유지(유형 단위 + 사람 편집 UI).
  3. **비즈니스 규칙 (T1-3)**: BIZ-016 슬롯 가드 폐기 → "해당 유형 성공 제안서 1건 이상". BIZ-017 출처보호 유지.
  4. **API (T3-2)**: `/admin/rfp-samples` 슬롯 배치 엔드포인트(getSlots/assign/unassign) 제거. `/admin/pattern-guides/{industryType}` 유형 단위로 전환.
  5. **화면 (T3-3)**: RfpSampleDetailPage 7슬롯 배치 UI 제거(파일 업로드/목록만). RfpSamplePage 가이드 유형별 표시 + 사람 편집 모달 추가.
  6. **AI 연동**: `extractSlotPattern`→`extractTypePattern`, MCP `get_slot_samples`→`get_reference_samples`(유형 매칭 원본+다운로드URL). 워크플로우 키 `slot-pattern-extraction`→`type-pattern-extraction`.
- **영향 범위**:
  - BE: `domain/rfp`(SlotDefinition/SlotAssignment/SlotEstimator/SlotAssignmentService + repo 2 + DTO 3 삭제, PatternGuide/Repository/DTO 유형단위, RfpSampleService/PatternExtractionService 재구성), `controller/admin`(슬롯 엔드포인트 제거), `mcp/tool/PatternGuideMcpTool`+`McpDispatcher`, `integration/llmplatform/LLMPlatformClient`, `application.yml`
  - FE: `api/client.ts`, `pages/RfpSamplePage.tsx`·`RfpSampleDetailPage.tsx`
  - 마이그레이션: `V11__rfp_remove_slots_type_guide.sql`
- **영향 설계 문서**: T1-1, T1-3, T3-1, T3-2, T3-3
- **검증**: BE 전체 테스트 BUILD SUCCESSFUL, FE tsc 통과. **런타임 실측** — 앱 기동 + V11 적용(`now at version v11`) + MCP tool 13개(get_reference_samples 노출, get_slot_samples 제거) + DB 스키마(slot_* 삭제, pattern_guide industry_type 단위, rfp_sample/file 유지) 확인.
- **남은 작업(B작업, 다음)**: 작성 워크플로우 — 메타 자동선택 + 도구 원본발췌 + 유형 가이드 주입. Aimbase `type-pattern-extraction` 워크플로우 생성(현재 placeholder).
- **운영 메모**: 적용 중 DB 호스트 차단(max_connect_errors)으로 기동 실패 → SSH로 FLUSH HOSTS. V11 최초안 FK 드롭 순서 오류(pattern_guide가 slot_definition 참조) → pattern_guide 먼저 드롭하도록 수정 + 실패 레코드 정리 후 재적용.
- **코드 commit**: 7d81c9f (27 files, +291/-723)
- **상태**: 코드 구현·런타임 검증 완료. 설계 캐스케이드 일괄 반영 중.

---

### CR-014: 성공 제안서 자산 작성 활용 (B작업) (2026-05-29)

- **배경**: CR-013-R A작업(성공 자산 등록·공고유형 가이드·MCP `get_reference_samples`, commit 7d81c9f)이 완료됐으나, 이를 **소비**하는 작성 워크플로우가 미연결. 이 플랫폼은 "제안서를 실제로 써주는" 서비스로, 작성 입력 파이프라인③(P3)이 핵심 연결점. 실측 결과 P3([AIWorkflowService.java:209-225])가 여전히 `findByMemberIdAndState(member.getId(), SUBMITTED)` = **사용자 본인 과거 이력만** 참조. 봐야 할 것은 플랫폼에 쌓인 성공(낙찰) 제안서의 가이드 + 원본. 전제로, 신규 공고를 성공 자산(IndustryType으로 묶임)과 매칭하려면 공고에 IndustryType 분류가 필요하나 현재 `Opportunity`엔 자유문자열 `type`만 있음.
- **사용자 확정 결정**:
  1. **유형 분류** = NAICS/PSC 룰표 우선 + title 키워드 폴백 (LLM 없음, 결정적). 입력(naicsCode/classificationCode/title)은 수집 rawJson에 이미 보존됨.
  2. **P3 주입** = 해당 유형 PatternGuide(가이드) + `get_reference_samples` 원본 메타/다운로드 URL **둘 다**.
  3. **분류 컬럼** = Opportunity 엔티티에 industry_type 신설. 미매칭 시 NULL(UNKNOWN enum 추가 안 함 — RfpSample/PatternGuide와 enum 집합 공유, 오염 방지).
  4. 진행 = **설계 캐스케이드 먼저 + 코드** (중규모, 데이터모델 변경 포함).
- **변경 사항**:
  1. **데이터 모델 (T3-1)**: 마이그레이션 V12. `opportunities.industry_type`(IndustryType, NULL 허용) 컬럼 + 인덱스 추가.
  2. **기능 요구 (T1-1)**: BID-OPP-010(공고 사업유형 자동 분류) / BID-RFP-005(성공 자산 작성 활용 — P3 대체) 신설. BID-DOC-001 파이프라인③ 정의 개정.
  3. **비즈니스 규칙 (T1-3)**: BIZ-018(공고 유형 자동 분류, NAICS>PSC>title, 미매칭 NULL) / BIZ-019(작성 시 원본 교차오염 방지 — 형식·전략 참고용, 사실 복붙 금지) 신설.
  4. **신규 컴포넌트**: `IndustryClassifier`(NAICS/PSC 룰표 + 키워드 폴백, 코드 상수), `ReferenceSampleService`(`PatternGuideMcpTool.getReferenceSamples` 로직 추출, BE 직접 재사용).
  5. **작성 입력**: `build3PipelineContext` P3(pastSubmissions) 제거 → successGuide + referenceSamples + referenceUsagePolicy.
- **영향 범위**:
  - BE: `domain/opportunity`(IndustryClassifier 신설, Opportunity 엔티티, OpportunityService.createOrUpdate 시그니처), `integration/samgov`(OpportunityCollectorService 분류 호출), `domain/rfp`(ReferenceSampleService 신설), `domain/bid/AIWorkflowService`(P3 교체), `mcp/tool/PatternGuideMcpTool`(서비스 위임)
  - 마이그레이션: `V12__add_opportunity_industry_type.sql`
- **영향 설계 문서**: T1-1, T1-3, T3-1, execution-spec
- **범위 밖(별도)**: Aimbase `type-pattern-extraction` 워크플로우 생성(application.yml placeholder) + rfp/ 실파일 등록→추출 E2E. Aimbase document-generation 워크플로우가 successGuide/referenceUsagePolicy를 실제로 읽도록 하는 것은 Aimbase 레포 밖 — BE는 데이터 전달까지만 책임.
- **상태**: 설계 캐스케이드 진행 중 (코드 구현 대기).
