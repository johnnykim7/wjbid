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
| CR-004 | 2026-05-16 | 정제 출력 포맷 정형화 + 스키마 검증 강화 | BE (domain/notice NoticeService) | 소규모 | 완료 (BE+테스트, 2026-05-29) |
| CR-005+006 | 2026-05-16 | bp-notification 통합 알림 (관리자 통보 + 고객 공고 알림) | BE (domain/notification, integration/notification, controller, config) + FE (customer-portal) | 중규모 | 코드 완료, 설계 캐스케이드/FlowGuard 미수행 |
| CR-007 | 2026-05-16 | 수집→정제 자동 트리거 | BE (event, integration/samgov, domain/opportunity) | 소규모 | 보류 |
| CR-008 | 2026-05-16 | 과거 샘플 본문 참조 보강 | BE (mcp) + Aimbase 워크플로우 | 소규모 | 계획/논의중 |
| CR-009 | 2026-05-28 | 수집 카운트 의미 정확화 + 관리자 공고목록 정렬 보정 | BE (integration/samgov, domain/opportunity, controller/admin) | 소~중규모 | 진단 완료, 구현 대기 |
| CR-010 | 2026-05-28 | 공고 요구서류 ↔ 고객 업로드 슬롯 매칭 | BE (domain/bid, domain/compliance, controller, StorageService) + FE (customer-portal) | 중규모 | 구현 완료 (BE+FE+테스트, 2026-05-29), 화면 E2E는 데이터 준비 후 |
| CR-011 | 2026-05-27 | SAM.gov 수집 키 운영 주입 정상화 + 신규 0건 메일 발송 스킵 | 운영(docker-compose.prod.yml) + BE (domain/notification) | 소규모 | 완료 |
| CR-013 | 2026-05-28 | 성공 제안서 패턴 가이드 (관리자 등록 + 슬롯 패턴 추출) | BE (domain/rfp 신설, integration/storage, common, mcp, integration/llmplatform, controller/admin, 마이그레이션 V10) + FE (admin-console) | 대규모 | 간이 설계 + 코드 병행 |
| CR-013-R | 2026-05-29 | CR-013 슬롯 폐기 → 원본 통째 보관 + 공고유형별 가이드 + 작성 시 도구 발췌 | BE (domain/rfp 슬롯 제거, PatternGuide 유형단위, mcp, llmplatform, controller/admin, 마이그레이션 V11) + FE (admin-console) | 대규모 | 코드 먼저 + 설계 일괄 |
| CR-014 | 2026-05-29 | 성공 제안서 자산 작성 활용 (B작업) — 공고 IndustryType 자동분류 + 작성 P3를 성공 가이드+원본으로 대체 | BE (domain/opportunity IndustryClassifier 신설, Opportunity 엔티티, integration/samgov, domain/rfp ReferenceSampleService, domain/bid AIWorkflowService, mcp, 마이그레이션 V12) | 중규모 | 설계 먼저 + 코드 |
| CR-019 | 2026-05-29 | 원본 공고 첨부 보강 — 수집 시 첨부 적재 + "가져와야 함" 표식 + 관리자 업로드 실제 저장 + 한글화 입력에 첨부 포함 | BE (integration/samgov, domain/opportunity OpportunityAttachment·status enum·repository, controller/admin, domain/notice, integration/llmplatform·mcp, 마이그레이션 V14) + FE (admin-console) | 중규모 | 설계 먼저 + 코드 |
| CR-017 ① | 2026-05-29 | 제안서 생성 완료 알림 발행 연결 — MCP 문서저장 콜백에서 DocumentGeneratedEvent 발행(기존 배선만 있고 발행 0건이던 것) + 멱등키에 버전 포함 | BE (mcp/DocumentMcpTool, domain/event, domain/notification) | 소규모 | 완료 (코드+테스트, 실발송 E2E 미수행) |
| CR-018 | 2026-05-29 | 고객 대상 진행 알림 3종(생성완료/접수/낙찰) + 입찰 결과 상태(AWARDED/NOT_AWARDED) 신설 | BE (domain/bid BidRequestState·BidFSMService·BidRequest, domain/notification NotificationType·NotificationEventListener, controller/admin, 마이그레이션 V16) + FE (admin-console, customer-portal) | 중규모 | 설계 먼저 + 코드 |
| CR-020 | 2026-05-30 | 대용량 제안서 섹션 루프 워크플로우 재설계 — 단일 LLM_CALL → plan_outline + FOREACH(섹션별 EVALUATOR_LOOP) + assemble. 50~백 페이지 안정 생성 + 품질 보강 | Aimbase 워크플로우 steps JSON 재작성 (엔진 무변경) + 소비앱 인터페이스 검토 (MCP/엔티티 무변경 전제) | 중규모 이상 | 설계 초안 (상세: CR-020_대용량_제안서_섹션루프_워크플로우_재설계.md) |
| CR-021 | 2026-05-31 | 공고문 표시 풍부화 — TipTap JSON 본문(contentJson) 신설 + NOTICE_VIEW 양식 템플릿 + Aimbase 워크플로우에 양식 입력/contentJson 출력 추가. 첨부 PDF 수준 PDF 양식으로 admin/customer 양쪽 렌더 | BE (마이그레이션 V18/V19, domain/notice Notice·NoticeService·MCP·DTO, domain/document DocumentType·DocumentTemplate API 활성/수정/재활성, application.yml polling 200) + FE (admin-console NoticeDocumentView 신규·NoticeAdminDetailPage·DocumentTemplatePage, customer-portal NoticeDocumentView·BidDetailPage·types) + Aimbase 워크플로우 PUT(noticeViewTemplate 입력·contentJson 출력·timeout_ms 600000) + docs/templates/·docs/workflows/ 자산화 | 중규모 | 1차 완료 (실제 노티 한글화 → contentJson 7768자 생성 → admin 화면 렌더 검증, 2026-05-31). 설계 캐스케이드는 사용자 지시로 보류 (나중 일괄). |
| CR-033 | 2026-06-02 | 공고 필수서류·자격요건 정밀추출 WF 분리 + Claude Code식 채팅 재생성 — ①요약 WF와 별도로 자격요건+필요서류를 묶은 정밀추출 WF 신설(Instructions to Offerors·PWS를 자율주행 파싱) ②슬롯 FACTOR>Subfactor 계층화 + 충족주체(AI생성/고객업로드/시스템양식) 분류, 고객 화면엔 고객업로드만 노출 ③관리자 검수=Aimbase Chat Widget 재사용, 현재 산출물+지시이력+첨부URL을 contextProvider로 주입해 자율주행이 부분 수정 | BE (domain/notice 추출/재생성 WF 분리·MCP 스키마, domain/compliance 슬롯 계층 모델, controller/admin 재생성 게이트, integration/llmplatform) + FE (admin-console 슬롯 검수 + aimbase-chat 위젯 + BFF 토큰 프록시, customer-portal 슬롯 고객업로드 필터) + Aimbase 워크플로우(정밀추출 신규) | 중규모 이상 | **1차 구현 완료 (2026-06-03)**: FACTOR>Subfactor+충족주체+자격요건 스키마(BE DTO·MCP), 고객/관리자 화면 렌더, opportunity-analysis WF에 추출 프롬프트+스키마 추가. 실측으로 설계 수정(아래 상세 §CR-033 참조) — 저장소=경로A(Notice.requiredDocumentsJson) 확장, 별도 WF 신설 대신 기존 한글화 WF에 추출 통합. **남음**: 실제 LLM 추출 정확도 E2E, 교정 채팅(BFF+위젯). 운영 배포 완료 |
| CR-034 | 2026-06-03 | 원본 공고 첨부 섹션 개선 + PIEE 안내 링크 — ①본문 piee.eb.mil contains 감지 방식 폐기(V30 컬럼 V31로 DROP): PIEE 단서는 search 응답에 없고 본문 fetch 의존이라 신뢰도 낮음 ②PIEE 입찰서류 안내 링크를 첨부 유무·본문과 무관하게 첨부 섹션에서 solicitationNumber 기반으로 항상 노출(정본/추가본이 PIEE에 있을 수 있음) ③저장된 첨부(storageUrl 보유) 다운로드 ④수동 업로드분(sourceUrl='admin-upload')만 삭제, SAM 수집 첨부는 403 거부(원본 보존) | BE (entity/Opportunity piee_available 필드·감지 제거, dto/OpportunityAdminDto pieeAvailable/pieeUrl 제거, OpportunityAdminController download GET·delete DELETE 신규, 마이그레이션 V31 DROP) + FE (admin-console OpportunityAdminDetailPage 첨부 섹션 PIEE 블록·다운로드/삭제 버튼·api client) | 중규모 | 구현·BE 컴파일·FE 타입체크·운영 배포·V31 적용·엔드포인트 매핑(401) 검증 완료 (2026-06-03). PIEE URL 형식은 브라우저 실확인 대기 |
| CR-035 | 2026-06-03 | 공고 본문(noticedesc) fetch 흐름 정비 — SAM 일일 쿼터 보호 + 깨진 본문 링크 해결. ①수집 시 자동번역은 제목(+type 라벨)만: noticedesc fetch를 수집 시점에 일괄 호출하지 않음(쿼터 절약). ②관리자 "한글 번역하기" 버튼이 그 공고 1건만 noticedesc fetch + 원문 본문 저장(applyDescriptionBody) + 번역. ③화면 본문 섹션: 깨진 noticedesc API 링크(api_key 없어 404) 제거 → 본문 텍스트(있으면) + "SAM.gov에서 원문 보기"(uiLink, 번역·본문 유무 무관 항상). ④번역문 줄바꿈 보존(stripHtml 블록태그→\n) + descriptionSummaryKo varchar(500)→TEXT 확대(V33, 500자 컷 제거) | BE (OpportunityTranslationService translateAsync 제목만·translateDescriptionOf 본문저장·stripHtml 줄바꿈, Opportunity 500자컷 제거, 마이그레이션 V33) + FE (admin-console OpportunityMetaPanel 본문섹션 uiLink 항상노출) | 소~중규모 | 구현·컴파일·FE 타입체크·운영 배포·V33 적용(TEXT) 검증 완료 (2026-06-03). 본문 번역 줄바꿈 표시 운영 확인 |
| CR-036 | 2026-06-03 | SAM API 호출 계측 & 쿼터 로깅 — 일일 한도가 추측(서드파티 블로그 "1,000")만 있고 공식 미확인이라 실측 체계 구축. ①신규 sam_api_call_log(V32): 일자(UTC)·엔드포인트별 success/error 카운트 + SAM X-RateLimit-* 헤더(실제 한도/잔량) + 에러 응답 본문 전문 UPSERT. ②SamQuotaLogger: 매 호출 [SAM-QUOTA] 로그 + DB 적재(REQUIRES_NEW). ③호출처 3곳 전부 계측: search/noticedesc/attachment(모두 api.sam.gov+api_key) | BE (integration/samgov/quota SamApiCallLog·Repository(native UPSERT)·SamQuotaLogger 신규, SAMGovApiClient search·noticedesc 계측, AttachmentAutoDownloadService 계측, 마이그레이션 V32) | 중규모 | 구현·컴파일·운영 배포·V32 적용·테이블 생성 검증 완료 (2026-06-03). 실제 X-RateLimit 한도는 다음 SAM 호출 시 sam_api_call_log에서 실측 예정 |
| CR-039 | 2026-06-14 | 공고문 한글화/분석 강제 중단 + stuck 자동 정리 — ANALYZING 상태로 멈춰 무한 폴링되는 공고문을 끊는 기능 부재. ①관리자 상세화면에 "강제 중단" 버튼: ANALYZING일 때만 노출(재생성은 ANALYZING 중 disabled이라 화면 잠금), 클릭 시 즉시 FAILED 전환 후 재생성 가능. ②`@Scheduled` stuck 정리 스케줄러: analysisStartedAt 기준 임계분(설정값, 기본 60분) 초과 ANALYZING 건 자동 FAILED. ③Aimbase 워크플로우 취소는 best-effort — workflowRunId로 cancel 호출하되 Aimbase에 취소 API 미존재(실측)이므로 404/실패/타임아웃 무관 우리 쪽은 무조건 FAILED(화면 복구 최우선). ④신규 컬럼 analysis_started_at(markAnalyzing 시점 기록) — updatedAt은 무관 update에 밀려 stuck 판정에 부정확하므로 전용 필드 | BE (entity/Notice markAnalyzing에 analysisStartedAt 기록·cancelAnalysis 메서드, NoticeService.cancelAnalysis·NoticeStuckCleanupScheduler 신규, NoticeAdminController POST /{id}/cancel, LLMPlatformClient.cancelWorkflowRun best-effort, application.yml notice.analysis.stuck-threshold-minutes·scheduler-interval-ms, 마이그레이션 Vxx analysis_started_at) + FE (admin-console NoticeAdminDetailPage 강제중단 버튼·noticeApi cancel) | 중규모 | 설계 캐스케이드 진행중 (2026-06-14) |
| CR-041 | 2026-06-14 | 공고문 분석 WF `build_workspace` 탈-LLM 재구성 — CR-038이 도입한 AGENT_CALL(LLM) 단일 STEP을 FOREACH+TOOL_CALL(download_file)로 교체. 첨부 적재는 LLM 판단 불필요한 결정론적 다운로드라 LLM 제거 → CLI turn 비용 0 + 병렬 + 타임라인 가시성(STEP_START/TOOL_RESULT 실시간) + retry 멱등화 복잡도 소거. PDF 원본 보존(extract_facts가 parse_document(file_path)로 비전 분석). CR-038 designNotes의 "FOREACH 대신 AGENT_CALL(메모리 합의)" 결정을 뒤집음 | Aimbase 워크플로우 steps JSON 재작성(build_workspace STEP만, 엔진 무변경) + Aimbase `download_file` 도구 신설 의존(타사) + extract_facts 프롬프트 조정. MCP/엔티티/BE/FE 무변경 | 중규모 | 운영 PUT 완료 (2026-06-15). Aimbase download_file 신설 + CR-107(TOOL_CALL ToolContext 전파) 블로커 해소. JSON v3 라이브(write_description+download_attachments FOREACH). E2E 검증 남음 |
| CR-040 | 2026-06-14 | 공고문 삭제 기능 — 잘못 만든/실패한 공고문을 목록에서 제거. ①hard delete(완전 삭제): Notice row + 고아 VerificationLog(targetType=NOTICE) 함께 제거. ②가드: VISIBLE(노출 중) 또는 ANALYZING(분석 중)이면 거부(409) — 고객이 보는 건·도는 건 실수 삭제 방지. ③원본 Opportunity 보존(BIZ-004) — Notice→Opportunity 단방향이라 원본 무영향, 재선별로 공고문 재생성 가능. ④실측: BidRequest는 Opportunity 참조(Notice 직접참조 아님)→고객신청·제안서 무영향. NotificationLog는 Notice UUID 미참조(referenceType=CollectorRun/BidRequest/Opportunity, noticeId는 SAM 공고번호 string)→정리 불필요(서브에이전트 추정 반박) | BE (NoticeService.deleteNotice 가드+VerificationLog deleteAll+Notice delete, NoticeAdminController DELETE /{id}) + FE (admin-console NoticeAdminDetailPage 삭제 버튼·확인 모달·noticeApi delete) | 소~중규모 | 설계 캐스케이드 진행중 (2026-06-14) |

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
  6. **BID-DOC-001 개선** — AIWorkflowService가 3파이프라인 데이터를 조합하여 Aimbase에 전달. 과거 이력은 조회해서 전달하면 끝 (비어있으면 빈 배열)
  7. **MCP Tool 추가** — `get_opportunity_analysis` (사전 분석 결과 조회), `get_past_submissions` (과거 제출 이력 조회)
- **설계 원칙**:
  - 공고 분석은 공고당 1회 — 여러 사용자가 신청해도 재분석 불필요
  - 과거 이력 조회에 조건 분기 불필요 — 조회해서 전달하면 됨, 비어있으면 빈 배열
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

### CR-004: 정제 출력 포맷 정형화 + 스키마 검증 강화 (2026-05-16 등재 / 2026-05-29 구현)

- **배경**: Aimbase 한글화/요약 콜백(`save_opportunity_analysis`)이 summary·requiredDocuments 등 본문이 비어 있어도 그대로 `COMPLETED`로 저장되어, 빈 한글화가 고객에게 "완료"로 노출될 수 있었음. (메모리 `cr004-format-standardization` 갭 1·2)
- **원본**: `docs/origins/원본_운영플로우_추가요구_20260516.md` 요구사항 1) "정재시 정해진 포맷에 따라 정재함"
- **결정 (사용자 합의 2026-05-29)**: 검증 위치 = `NoticeService.saveResult` (CLAUDE.md "로직은 Service" 규칙). 누락 정책 = **FAILED 전이** (부분 저장/예외 던지기 대신).
- **주요 변경**:
  1. `NoticeService.saveResult`에 `validateRequiredKeys` 추가 — 필수키 누락 시 `markCompleted` 대신 `markFailed(reason)` + `OpportunityAnalysisCompletedEvent(success=false)` 발행.
  2. 필수키: `koreanTitle`, `summary.overview`, `requiredDocuments.documents`(1건 이상). 보조 정보(`documentFormats`, `llmPromptPreset`)는 검증 제외.
  3. 갭 2(빈 완료 노출) 자동 해소 — FAILED는 `publish()`의 `isGenerationCompleted()` 게이트(NoticeService:206)에 막혀 노출 불가.
- **테스트**: `NoticeServiceTest` 5건 (충족→COMPLETED, koreanTitle/summary/documents 누락→FAILED, 실패 이벤트 발행).
- **규모**: 소규모(단일 Service, 검증 정책이 FSM/노출 게이트 구조를 바꾸지 않음) → 설계 캐스케이드 불요, CR 이력만 갱신.

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
- **영향 설계 문서**: T1-3, T1-5, T3-1, T3-2, T3-3 (간이 캐스케이드 commit d128df8 완료)
- **실제 구현 (2026-05-29)**:
  1. **마이그레이션**: `V15__add_client_document_fulfillment.sql` — `requirement_fulfillment_maps.client_document_id BINARY(16)` + FK `fk_fulfillment_client_document`(→client_documents, ON DELETE SET NULL) + 인덱스 + UNIQUE `(bid_request_id, requirement_item_id)`. (당초 V14로 작성했으나 CR-019 `V14__add_attachment_manual_fetch_status.sql`과 버전 충돌 → V15로 변경)
  2. **enum**: `FulfillmentType.CLIENT_DOCUMENT` 추가 + `isFulfilled()` 포함
  3. **엔티티**: `RequirementFulfillmentMap.clientDocument` FK 필드 + `fulfillWithClientDocument()` 메서드
  4. **서비스**: `ComplianceService`에 `getRequiredDocumentSlots` / `uploadToSlot`(StorageService 실저장 + ClientDocument 생성 + 슬롯 upsert, 단일 트랜잭션) / `unmapSlot` / `getUnfulfilledBlockerSlots`(게이트용) 추가
  5. **컨트롤러**: 신규 `RequiredDocumentSlotController` (GET 슬롯목록 / POST 슬롯업로드 / DELETE 매핑해제)
  6. **FSM 게이트 (BIZ-015)**: `BidFSMService.transition`에서 `DOCS_PENDING→DOCS_RECEIVED` 시 BLOCKER 슬롯 미충족이면 `RequirementSlotsNotFulfilledException`(409 `REQUIREMENT_SLOTS_NOT_FULFILLED` + unfulfilledSlots) 차단. 전용 `@RestControllerAdvice`로 매핑
  7. **FE**: `api/client.ts` 슬롯 3함수 + `ProposalDetailPage.tsx` "제출 서류" 탭을 슬롯 카드 UI로 교체(요구사항별 업로드/변경 + 충족률 바 + 전부 충족 시 활성화되는 "문서 제출 완료" 버튼)
  8. **저장소**: 당초 설계의 MinIO 대신 기존 `StorageService`(로컬 디스크, CR-013)에 연결. ApiResponse 래퍼는 코드베이스에 없어 기존 컨트롤러 패턴(객체 직접 반환) 사용
  9. **테스트**: `ComplianceServiceTest` 6건 + `BidFSMServiceTest` 게이트 2건 작성·통과, 전체 스위트 회귀 없음
- **검증 한계**: 운영 DB에 요구사항(req_items)·입찰의뢰(bid_requests) 0건 → 화면 슬롯 E2E 미실시. 로직은 단위 테스트로 검증. Flyway V15는 운영 DB 적용 확인(컬럼/FK/UNIQUE 생성)
- **별도 발견(범위 외)**: 기존 자유 업로드 API의 FE 경로(`/client-documents/{id}`)와 BE 경로(`/bid-requests/{id}/client-documents`)가 불일치 — 본 CR과 무관해 미수정
- **상태**: 구현 완료 (BE+FE+테스트), 화면 E2E는 요구사항 데이터 준비 후 별도 진행

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

---

### CR-019: 원본 공고 첨부 보강 (2026-05-29)

- **배경**: 2026-05-29 운영 점검(점검9)에서 진단. 사용자 기대 = SAM 원문 첨부가 외부 사이트에 있을 때 관리자가 외부서 직접 가져와 원문에 추가 → 한글 공고문 생성 시 참고. 원문에 "이건 가져와야 함" 표식 필요(내용 수정이 아니라 첨부 보강). 실측 결과: 첨부 엔티티(`OpportunityAttachment`)·관리자 업로드 API 뼈대만 있고 핵심 미구현 — (a) 수집이 `resourceLinks`를 rawJson에만 저장하고 첨부 행으로 적재 안 함(운영 0건), (b) "가져와야 함" 표식 enum 값 없음, (c) 관리자 업로드가 `markLinkOnly()`로 메타만 저장(StorageService 미사용, 실제 파일 저장 안 됨), (d) 한글화 입력(NoticeService.buildOpportunityText)이 본문 텍스트만 — 첨부 미반영.
- **CR-016 선반영 발견**: 메모리 진단 시점과 달리 CR-016(원본/공고문 분리, Notice 엔티티 신설, V13)이 이미 코드 반영됨. 한글화 입력 경로가 구 `OpportunityAnalysisService`가 아니라 `NoticeService.buildOpportunityText`로 이전됨 → CR-019 (c)·(d)는 NoticeService 기준으로 진행.
- **사용자 확정 결정**:
  1. **범위** = 4종 전부 (수집 적재+표식 / 관리자 업로드 실제 저장 / 한글화 입력 포함 / 관리자 화면 표식 UI).
  2. **파싱** = Aimbase `parse_document`. BE 사전 파싱 없이 첨부 다운로드 URL을 워크플로우 입력으로 전달해 Aimbase가 발췌(RfpSample fileUrls와 동일 패턴).
  3. 진행 = 설계 캐스케이드 먼저 + 코드 (중규모).
- **변경 사항**:
  1. **데이터 모델 (T3-1)**: 마이그레이션 V14. `AttachmentDownloadStatus`에 `MANUAL_FETCH_REQUIRED`(외부 수동수집 필요) 값 추가. OpportunityAttachment 적재/갱신 경로 정의.
  2. **비즈니스 규칙 (T1-3)**: BIZ-020(원본 첨부 보강) 신설 — 원본 보존(BIZ-004) 하에 첨부 행만 적재, 외부 사이트는 MANUAL_FETCH_REQUIRED, 한글화 시 SUCCESS 첨부만 입력 포함(미수집 첨부 누락·추정 섞임 방지).
  3. **API (T3-2)**: 관리자 업로드 동작 보강(StorageService 저장+SUCCESS, MANUAL_FETCH_REQUIRED 행 갱신). 관리자 첨부 목록 조회 신규(`GET /admin/opportunities/{id}/attachments`). OpportunityAdminDto에 `manualFetchRequiredCount` 추가.
- **영향 범위**:
  - BE: `integration/samgov`(OpportunityCollectorService — resourceLinks 첨부 적재), `domain/opportunity`(OpportunityAttachment markManualFetchRequired, AttachmentDownloadStatus enum, repository, OpportunityAdminDto), `controller/admin/OpportunityAdminController`(업로드 StorageService 연동 + 첨부 목록 조회), `domain/notice/NoticeService`(buildOpportunityText → 첨부 fileUrls 워크플로우 입력), `integration/llmplatform`(analyzeOpportunity 입력 확장)
  - FE: admin-console(원본 목록/상세 "가져와야 함" 표식 + 첨부 목록 표시)
  - 마이그레이션: `V14__add_attachment_manual_fetch_status.sql`
- **영향 설계 문서**: T1-3, T3-1, T3-2, execution-spec
- **범위 밖(별도)**: Aimbase analyze/document-generation 워크플로우가 `attachmentFiles[]`를 실제로 parse_document 발췌하도록 하는 것은 Aimbase 레포 밖 — BE는 데이터 전달까지만. SAM resourceLinks 자동 다운로드 성공/실패 판정 로직의 실제 HTTP 다운로드 시도는 후속(현재는 외부 링크면 MANUAL_FETCH_REQUIRED로 보수 적재).
- **상태**: 설계 캐스케이드 완료 + 코드 구현 진행.

---

### CR-017 ①: 제안서 생성 완료 알림 발행 연결 (2026-05-29)

- **배경**: 2026-05-29 운영 점검(점검7)에서 진단한 3축(① 완료 알림 발행 / ② 부분·섹션 재생성 / ③ 전이 권한) 중 ① 착수. 사용자 기대 = 관리자가 제안서 생성 → 완료 시 알림. 실측 결과 `DocumentGeneratedEvent` 클래스 + 리스너 `NotificationEventListener.onDocumentGenerated`(관리자 전원 이메일)는 있으나 `new DocumentGeneratedEvent` 발행처가 전수조사 0건 — 문서는 Aimbase MCP 콜백(`DocumentMcpTool.saveDocumentVersion`)으로 저장되지만 거기서 이벤트를 쏘지 않아 완료 알림이 실제로는 0건 발송.
- **사용자 확정 결정**:
  1. **착수 범위** = ①완료알림만 (②부분재생성은 cr013 B작업·생성단위 재정의와 직결돼 중~대규모 → 별도 설계).
  2. **발송 단위** = 타입별 N건 (`saveDocumentVersion`이 DocumentType마다 MCP 콜백되므로 현 구조 최소수정).
  3. **멱등키** = 버전 포함 → 같은 문서 재생성(v2+) 시에도 알림 발송 가능(향후 ②부분재생성 대비).
- **변경 사항**:
  1. `domain/event/DocumentGeneratedEvent` — `versionNo` 필드 추가(생성자 시그니처 변경).
  2. `mcp/tool/DocumentMcpTool.saveDocumentVersion` — 문서 저장 성공 후 `eventPublisher.publishEvent(new DocumentGeneratedEvent(documentId, bidRequestId, documentType, versionNo))` 발행(`ApplicationEventPublisher` 주입).
  3. `domain/notification/NotificationEventListener.onDocumentGenerated` — 멱등키 `DOC_GEN_{documentId}` → `DOC_GEN_{documentId}_v{versionNo}`. (BIZ-012 멱등성: documentId만이면 v2+ 재생성 시 중복 차단으로 알림 막힘.)
- **영향 범위**: BE (mcp/DocumentMcpTool, domain/event/DocumentGeneratedEvent, domain/notification/NotificationEventListener) + 테스트(DocumentMcpToolTest — 이벤트 발행/페이로드 검증 v1·v2).
- **규모**: 소규모(설계 캐스케이드 불요 — 기존 배선 연결 + 알림 도메인 내 변경).
- **참고**: 현 리스너는 관리자(ADMIN) 대상. 고객 대상 생성완료 알림은 CR-018 소관(별도).
- **검증**: 컴파일 + 전체 테스트 BUILD SUCCESSFUL. **실발송 E2E(Aimbase MCP 콜백→이벤트→bp-notification)는 미수행** — 운영 DB에 BidRequest 0건이고 정상 신청 경로가 CR-016 공고문 흐름 선행을 요구해 런타임 검증은 갈음(코드/테스트 레벨).
- **상태**: ① 완료 (코드+테스트). ②부분재생성·③전이권한은 미착수.

---

### CR-017 ②: 개별 문서 재생성 (2026-05-29)

- **배경**: 점검7 ②부분/섹션 재생성. 사용자 기대 = 관리자가 생성된 제안서 검토 중 일부만 재생성("전부 재생성은 X"). 실측 결과 생성 단위 = `DocumentType` 7종 루프(`AIWorkflowService.generateDocumentsAsync`), 재생성은 `REVIEW→GENERATING` 전이로 전 문서 통째 재생성만 가능. 개별 재생성 API 없음.
- **실측으로 좁힌 그림**: "섹션 단위 생성"은 cr013 B작업(Aimbase 섹션 WF 미생성)에 막혀 있으나, **개별 DocumentType 재생성은 기존 단위·기존 Aimbase 문서생성 WF(`014e7de2-...`, 실제 ID 연결됨)·기존 버전 구조(saveVersion/rollback/lock 완비)를 그대로 재사용**하므로 신규 엔드포인트만 추가하면 됨. `LLMPlatformClient.generateDocument`는 단일 documentType 입력 → 개별 생성이 이미 가능한 단위.
- **사용자 확정 결정**: 재생성 단위 = **개별 DocumentType**(섹션까지 안 내려감). 허용 상태 = **REVIEW**(FSM 상태는 REVIEW 유지, 전체 GENERATING으로 안 되돌림). 결과 = **새 버전 추가**(BIZ-002 불변성 유지).
- **변경 사항**:
  1. `AIWorkflowService.regenerateSingleDocumentAsync(bidRequest, documentType)` — 단건 비동기 생성(기존 generateSingleDocument 재사용).
  2. `BidFSMService.regenerateDocument(id, documentType)` — REVIEW 상태 + 미LOCKED 검증 후 트리거. FSM 상태 불변.
  3. `BidRequestAdminController` — `POST /admin/bid-requests/{id}/documents/{documentType}/regenerate`(ADMIN).
  4. FE admin BidRequestDetailPage — 문서 목록 각 항목 "재생성" 버튼(REVIEW + 미LOCKED일 때만 노출).
- **CR-017 ①과의 시너지**: 재생성 결과는 Aimbase MCP save_document_version 콜백 → 새 버전 누적 → CR-017 ①의 멱등키(버전 포함) 덕분에 재생성 완료 알림도 발송됨.
- **영향 범위**: BE (domain/bid AIWorkflowService·BidFSMService, controller/admin) + FE (admin-console client.ts·BidRequestDetailPage) + 테스트(BidFSMServiceTest 3종).
- **규모**: 소~중규모(기존 단위/WF/버전 재사용, 신규 화면·테이블·이벤트 계약 없음).
- **검증**: BE 전체 테스트 + admin/customer tsc 통과. 런타임 E2E는 미수행(운영 BidRequest 0건).
- **범위 밖(별도)**: 섹션 단위 재생성(cr013 B작업, Aimbase 섹션 생성 WF 선행 필요).

### CR-017 ③: 전이 권한 경계 정리 — 고객 화이트리스트 (2026-05-29)

- **배경**: 점검6/7. 사용자 그림 = 관리자 검토 후 생성. 실측 결과 `/bid-requests/**`=hasAnyRole(CUSTOMER,ADMIN)(SecurityConfig:82)이고 고객 `PATCH /bid-requests/{id}/state`에 개별 권한 제약이 없어, **고객이 API 직접 호출로 GENERATING(AI 생성 트리거) 등 관리자 전용 전이를 임의로 밀 수 있는 구멍**. 고객 FE는 실제로 DOCS_RECEIVED(서류 제출 완료) 하나만 호출(ProposalDetailPage:154) — 즉 정당한 고객 전이는 그것뿐인데 BE가 다 열려 있었음.
- **사용자 확정 결정**: 고객 직접 전이 = **DOCS_RECEIVED만**(고객 취소 CLOSED 기능은 FE에 없어 미포함, YAGNI). 그 외 전이(ANALYZING/GENERATING/REVIEW/CONFIRMED/SUBMITTED/CLOSED)는 관리자 전용 → 관리자 검토 게이트를 코드로 강제.
- **변경 사항**:
  1. `BidFSMService.CUSTOMER_ALLOWED_TARGET_STATES`(=DOCS_RECEIVED) 화이트리스트 + `customerTransition()` — 화이트리스트 검증 후 transition 위임. 위반 시 `CustomerTransitionNotAllowedException`.
  2. `BidRequestController.transitionState` — `transition` → `customerTransition` 호출로 변경.
  3. `CustomerTransitionNotAllowedException` + `CustomerTransitionExceptionHandler`(403 매핑, 특정 예외만 처리해 기존 동작 불변 — RequirementSlotsExceptionHandler 패턴 동일).
- **FE 변경 없음**: 고객 FE는 이미 DOCS_RECEIVED만 호출 → 정상 동작. 이번 변경은 API 직접 호출 우회를 막는 서버측 방어.
- **영향 범위**: BE (domain/bid BidFSMService·신규 예외·핸들러, controller/BidRequestController) + 테스트(BidFSMServiceTest 2종).
- **규모**: 소규모(서버측 권한 가드 추가, 화면·모델 변경 없음).
- **검증**: BE 전체 테스트 + tsc 통과.
- **상태**: CR-017 ①②③ 모두 완료.

---

### CR-018: 고객 대상 진행 알림 3종 + 입찰 결과 상태 신설 (2026-05-29)

- **배경**: 점검8. 사용자 기대 = 제안서 생성 완료 시 고객에 "완료" 알림, 제출(접수) 시 "접수" 알림, 낙찰 시 "합격" 알림(자동 아니라 관리자 수동 업데이트 전제). 실측 결과 고객 대상 알림은 OPPORTUNITY_APPROVED(신규 공고) 1종뿐이고, ①생성완료 알림은 ADMIN 대상(DocumentGenerated), ②접수 알림은 타입/리스너 없음(BidRequestStateChanged 발행되나 알림 리스너 無), ③낙찰은 **FSM 상태 모델 자체에 없음**(SUBMITTED가 종착).
- **사용자 확정 결정**:
  - 낙찰 상태모델 = **FSM에 AWARDED/NOT_AWARDED 추가**(별도 결과 필드 분리 안 함). 전이 = **SUBMITTED → AWARDED / NOT_AWARDED**(둘 다 종착, 재도전 전이 없음). 관리자만 전이 가능(기존 CR-017 ③ 화이트리스트로 고객 차단 유지).
  - 알림 3종 발송 시점 = `BidRequestStateChangedEvent` 소비 — `toState=REVIEW`(생성완료), `SUBMITTED`(접수), `AWARDED`(낙찰). NOT_AWARDED는 MVP 무알림. (생성완료를 REVIEW 전이로 잡은 이유: GENERATING→REVIEW가 AI 문서 생성 완료를 의미하고 이미 StateChanged 이벤트로 발행됨 → DocumentGenerated의 DocumentType별 N건 발행을 고객에게 중복 전송하지 않음.)
  - bp-notification 템플릿 = **기존 템플릿 재사용**(신규 등록 운영작업 없이 즉시 발송). 생성완료=BID_DOC_GENERATED, 접수=BID_REQUEST_ADMIN, 낙찰=BID_OPPORTUNITY_APPROVED 재사용(문구 부적절 가능성은 감수, 후속 전용 템플릿 등록 가능).
  - 구현 범위 = **BE + FE 전체**.
- **변경 사항**:
  1. `domain/bid/entity/BidRequestState` — `AWARDED`, `NOT_AWARDED` 추가. `isTerminal()`에 둘 다 포함, `getStateDisplay()` 한글 라벨 추가.
  2. `domain/bid/service/BidFSMService.VALID_TRANSITIONS` — `SUBMITTED → [AWARDED, NOT_AWARDED]`. `CUSTOMER_ALLOWED_TARGET_STATES`는 불변(고객은 결과 전이 불가).
  3. `domain/bid/entity/BidRequest` — `transitionTo`에 AWARDED/NOT_AWARDED 시 `outcomeDecidedAt` 기록.
  4. `db/migration/V16` — `bid_requests`에 `outcome_decided_at` 컬럼 추가.
  5. `domain/notification/entity/NotificationType` — 고객 대상 `PROPOSAL_READY_CUSTOMER`/`BID_SUBMITTED_CUSTOMER`/`BID_AWARDED_CUSTOMER` 3종(기존 templateCode 재사용).
  6. `domain/notification/service/NotificationEventListener.onBidRequestStateChanged` — toState 분기로 고객 알림 발송(member 1명). 멱등키 `BR_STATE_{id}_{toState}`.
  7. `controller/admin/BidRequestAdminController` — 기존 `POST /{id}/transition` 으로 AWARDED/NOT_AWARDED 전이 처리(전용 API 신설 안 함, FSM 화이트리스트 재사용).
  8. FE admin `BidRequestDetailPage` — NEXT_STATES/STATE_LABELS/STATE_COLORS에 AWARDED/NOT_AWARDED 추가 → 기존 상태 전환 모달에서 낙찰/탈락 선택 가능.
  9. FE customer `types`(STATE_LABEL/STATE_BADGE) — AWARDED/NOT_AWARDED 라벨/배지 추가 → 제안서 상세·목록에 결과 표시.
- **영향 범위**: BE (domain/bid, domain/notification, controller/admin, 마이그레이션 V16) + FE (admin-console, customer-portal) + 테스트(BidFSMServiceTest, NotificationEventListener 검증).
- **규모**: 중규모(FSM 상태·데이터 모델·화면·알림 동시 변경 → 설계 캐스케이드 선행).
- **설계 캐스케이드**: T1-5 FSM(BidRequest 상태 + 다이어그램 + AWARDED/NOT_AWARDED 정의), T1-6 이벤트 계약(BidRequestStateChanged 고객 소비 확장) 갱신 완료.
- **검증**: (구현 후 기재) BE 컴파일+전체 테스트, admin/customer tsc. 실발송 E2E는 운영 BidRequest 0건이라 코드/테스트 레벨로 갈음.
- **상태**: 설계 캐스케이드 완료, 코드 구현 진행.

---

### CR-021: 공고문 표시 풍부화 — TipTap 본문 + NOTICE_VIEW 양식 (2026-05-31)

- **배경**: 운영 화면에서 공고문 상세가 `JSON.stringify` 원시값으로 노출되어 사람이 읽기 불가. 사용자 통과 기준 = 첨부 PDF(W90VN926QA034) 수준의 풍부도(표·계층·강조박스·메타그리드). 정형 JSON(summary/requiredDocuments/documentFormats)은 액션용으로 유지하면서, 사람이 읽는 본문을 별도 슬롯으로 신설.
- **사용자 확정 결정**:
  - 데이터 모델 = **`notices.content_json` 신설**(TipTap JSON 노드 트리). 액션용 정형 키는 그대로 유지.
  - 양식 = 기존 "문서 템플릿 관리" 메뉴에 **`NOTICE_VIEW` 타입 추가** — 별도 메뉴 분리 안 함.
  - 양식 생성 주체 = **사람이 1회 작성**(LLM 자동변환 안 함). 첨부 PDF의 10개 섹션을 TipTap JSON 골격으로 박아 등록.
  - LLM 출력 안정성 = **template 그대로 받아 채워서 반환**. 단순 자유 텍스트 아님.
  - 디자인 = **네이비 마스키 정부문서 계열**. admin/customer 공통 컴포넌트.
  - 설계 캐스케이드 = **사용자 지시로 본 CR에서는 보류**(나중 일괄 처리). 구현 우선.
- **변경 사항**:
  1. `db/migration/V18` — `notices.content_json LONGTEXT` 컬럼.
  2. `db/migration/V19` — `document_templates.description` 컬럼(엔티티/스키마 갭 보정).
  3. `domain/notice/entity/Notice` — `contentJsonRaw` 필드 + `getContentJson()` + `markCompleted`/`updateResult` 시그니처 확장.
  4. `domain/document/entity/DocumentType` — `NOTICE_VIEW` 값 추가.
  5. `mcp/tool/OpportunityAnalysisMcpTool` — inputSchema에 `contentJson` 슬롯 + `saveOpportunityAnalysis` 시그니처 확장.
  6. `domain/notice/service/NoticeService` — `DocumentTemplateService` 주입 + `generateAsync`에 `noticeViewTemplate` 입력 1줄 추가(`getActiveTemplate(NOTICE_VIEW)` 미등록 시 생략). saveResult/updateResult 시그니처 확장.
  7. `domain/opportunity/dto/AnalysisResultDto` — `contentJson` 필드 추가(TipTap JSON 통째 클라이언트 전달).
  8. `domain/document/service/DocumentTemplateService` — `findAllIncludingInactive`/`activate` 추가. `controller/admin/DocumentTemplateAdminController` — `GET ?includeInactive=true` + `POST /{id}/activate`.
  9. `application.yml` — `app.aimbase.polling.max-attempts: 60→200` (CR-021로 input 증가에 따른 워크플로우 시간 증가 대응).
  10. FE admin: `components/NoticeDocumentView.tsx` 신규(6종 커스텀 노드: noticeHeader/metaGrid/kvTable/dataTable/groupedList/calloutList — 객체/문자열 다중 형태 수용). `pages/NoticeAdminDetailPage` — `<pre>{JSON.stringify}` 제거 + 필요서류 카드. `pages/DocumentTemplatePage` — 비활성 포함 토글 + 재활성화 버튼 + 수정 모달. `api/client.ts` — 활성/수정 API.
  11. FE customer: `components/NoticeDocumentView.tsx` 신규(admin 복사). `pages/BidDetailPage` — contentJson 있으면 NoticeDocumentView 상단 표시. `types/index.ts` — `AnalysisResult.contentJson`.
  12. Aimbase 워크플로우 `opportunity-analysis` PUT 반영:
     - `analyze_freeform.config.prompt` — noticeViewTemplate 안내 + 담당자(POC) 추출 + contentJson 풍부도 지침
     - `analyze_freeform.config.timeout_ms: 600000` (Aimbase 기본 120초로는 부족 — 실측 6분 41초 소요)
     - `structure_output.config.response_schema` — `contentJson` 슬롯 + `summary.contactInfo` 추가
     - `save.config.input` — `contentJson` 전달
  13. 자산화: `docs/workflows/opportunity-analysis.steps.json`, `docs/templates/notice-view-tiptap-template-v1.json` + paste 본.
- **양식 등록 (운영 DB)**: `USFK_RFQ_표준양식_v1` (NOTICE_VIEW, 10섹션 + 메타그리드).
- **영향 범위**:
  - BE: 마이그레이션 V18/V19, domain/notice, domain/document, mcp/tool, controller/admin, application.yml, NoticeServiceTest
  - FE: admin-console(컴포넌트 1개 신규, 페이지 2개 수정, api), customer-portal(컴포넌트 1개 신규, 페이지 1개 수정, types)
  - Aimbase 워크플로우: `opportunity-analysis` 운영 PUT 반영(레포에 정의 자산화)
- **규모**: 중규모(BE 마이그레이션 + 다중 도메인 수정 + Aimbase 워크플로우 변경 + FE 컴포넌트 신설).
- **검증**:
  - BE compileJava/Test BUILD SUCCESSFUL, NoticeServiceTest 통과
  - FE admin/customer tsc 통과
  - 운영 배포 후 E2E: noticeId `9a697650-...` 한글화 재실행 → contentJson 7768자 생성 → admin 상세 PDF 양식 렌더 확인 (10섹션 모두 사람 글로 표시, 필요서류 체크리스트 카드)
  - 발견된 별 버그(메모리 등록): `NoticeService.markFailed` self-call 누락(워크플로우 폴링 타임아웃 시 상태 잔존), customer-portal 화면 검증은 publish 단계에서 본 흐름 E2E와 함께 진행 예정.
- **상태**: 1차 완료. 본 흐름 후속(publish→customer→제안서 생성)은 별 세션.

---

### CR-033: 공고 필수서류·자격요건 정밀추출 (FACTOR>Subfactor + 충족주체 + 자격요건) (2026-06-03)

- **배경**: 고객 제출서류 슬롯의 신뢰성 문제. `rfp/공고문/` 9개 정답지 교차검증 결과 요구 제출물·자격은 예외 없이 Instructions to Offerors(52.212-1 Addendum)에 있고 SAM description엔 없음. 정부조달 골격 = FACTOR I(Technical, Subfactor 세분)/II(Price)/행정서류.
- **착수 전 실측으로 설계 수정 (메모리 cr033 + 본 세션)**:
  1. 요구사항 추출 경로 2개 확인 — A: `save_opportunity_analysis`→`Notice.requiredDocumentsJson`(고객 화면이 읽는 유일 소스) / B: `requirement-extraction` WF→`OpportunityRequirementItem`(BLOCKER 검증용). **DB 실측 `opportunity_requirement_items` 0건 → 경로 B 사문**. → 저장소=경로 A 확장으로 확정(별도 WF·엔티티 신설 안 함).
  2. FACTOR/Subfactor 용어 출처 실측 — 제안서 전용 용어가 아니라 **Solicitation 원문 Instructions에 그대로 존재**(`W90VN926QA034`: `4.1 FACTOR I-TECHNICAL`/`Sub-Factor 1~4`/`4.2 FACTOR II-PRICE`). 제출물=평가축 이중 성격.
  3. 충족주체 분류 난이도 발견 — Sub-Factor 3(트럭 소유·인력 보유)은 **고객 실물 증빙(CLIENT_UPLOAD)**이지 플랫폼 생성 아님. WF 프롬프트에 명시 경고 추가.
- **변경 사항**:
  1. `mcp/tool/OpportunityAnalysisMcpTool` — save 스키마 `requiredDocuments`에 `factors[]`(factorId/factorTitle/subfactors[{subfactorId,name,description,fulfillmentParty(CLIENT_UPLOAD/PLATFORM_GENERATED/SYSTEM_FORM),mandatory,format,pageLimit,sourceRef,notes}]) + `eligibility[]`(title/description/mandatory/evidenceBy/isGate/sourceRef) 추가. `documents[]` 평면은 하위호환 유지. 저장 경로(String free-form)는 무변경.
  2. `domain/opportunity/dto/AnalysisResultDto` — `RequiredDocumentsDto`에 `factors`/`eligibility` 매핑 + `FactorDto`/`SubfactorDto`/`EligibilityDto` record 신설. (기존엔 `documents`만 매핑해 factors를 떨궈내던 것 — 응답 경로 버그성 누락 보정). `bool()` 헬퍼 추가.
  3. FE customer `pages/ProposalDetailPage` — Factor/Subfactor/Eligibility 타입, FACTOR 트리 렌더(CLIENT_UPLOAD만 업로드 슬롯·나머지 읽기전용 배지), 충족현황 CLIENT_UPLOAD 기준 재계산, 자격요건 안내(서류 아래), factors 없으면 평면 폴백.
  4. FE admin `components/NoticeDocumentView` — `eligibility` prop 추가, 본문 "자격 요건"(§6) heading 직후에 정밀추출 자격요건 인라인 렌더(중복·하단분리 방지). `EligibilityBlock` 컴포넌트.
  5. FE admin `pages/NoticeAdminDetailPage` — FACTOR 트리 섹션(충족주체 배지+sourceRef) + NoticeDocumentView에 eligibility 전달. 하단 별도 자격요건 섹션 제거.
  6. Aimbase `opportunity-analysis` WF (PUT) — `analyze_freeform.prompt`에 FACTOR>Subfactor 구조·충족주체 판단(트럭/인력/실적=CLIENT_UPLOAD 경고)·자격요건 추출 지침 추가. `structure_output.prompt`에 factors/eligibility 변환 규칙. `structure_output.response_schema.requiredDocuments`에 factors/eligibility 스키마(fulfillmentParty enum). save.input은 requiredDocuments 통째 매핑이라 무변경.
- **화면 표현 결정(사용자)**: 관리자·고객 둘 다 FACTOR 트리 노출 + 충족주체 필터(고객은 CLIENT_UPLOAD만 업로드). 자격요건은 관리자=본문 §6 인라인, 고객=서류 아래 안내.
- **영향 범위**: BE (mcp/tool, dto) + FE (admin-console 2파일, customer-portal 1파일) + Aimbase 워크플로우 `opportunity-analysis` PUT.
- **규모**: 중규모(MCP 스키마 + 응답 DTO + 다중 화면 + Aimbase WF). DB 마이그레이션 없음(required_documents_json = longtext free-form).
- **검증**:
  - BE compileJava BUILD SUCCESSFUL, FE admin/customer tsc + prod build 통과.
  - 운영 배포(BE+FE) 완료. WF PUT 200 반영 실측(factors/eligibility 스키마·프롬프트 확인).
  - 운영 API 응답에 factors/eligibility 정확히 실림 실측(테스트 데이터 주입→GET 확인→원복).
  - 관리자 화면 FACTOR 트리·자격요건 인라인 렌더 사용자 육안 확인.
- **E2E 추출 정확도 검증 (2026-06-03, 실측 통과)**:
  - 실공고 2건으로 "공고문 만들기" 실행 → factors/eligibility 실제 생성 확인.
    - W90VN826QA009: FACTOR I(기술/관리)/II(가격), 충족주체 정확(Business Authorization·SAM=CLIENT_UPLOAD, 기술/가격=PLATFORM_GENERATED). eligibility 3건 GATE.
    - W91QVN26QA019(관제탑 재도장): FACTOR I(기술접근=PLATFORM, 과거실적·인력=CLIENT_UPLOAD)/II(가격=PLATFORM). eligibility 3건(SAM·사업자·건설업면허). **우려했던 인력=PLATFORM 오분류 없음 — 인력을 CLIENT_UPLOAD로 정확 분류**.
  - 충족주체 분류 신뢰성 확인됨. WF 프롬프트의 "트럭/인력/실적=CLIENT_UPLOAD" 경고가 작동.
- **추가 정리 (WF + NOTICE_VIEW 양식, 2026-06-03)**:
  - WF `structure_output` — §6 자격요건 중복 제거 규칙: contentJson 의 section6.qualificationGroups("필수 자격"·"기술 요건")에 자격을 넣지 않고 eligibility 로만(제출 방식은 유지). 화면에서 자격이 정밀추출 인라인 + 본문 이중표시되던 중복 해소. 실측: 재생성 후 §6 groupedList 빈 배열로 확인.
  - NOTICE_VIEW 양식(운영 DB document_templates) — ① metaGrid 에 "발행일" metaCell 추가(마감일 옆, 6→7칸. 발행일이 noticeHeader 회색 줄에만 있어 안 보이던 것). ② "1. 공고 기본 정보" heading+kvTable 제거(metaGrid 와 6항목 완전 중복). WF 에 발행일 칸 채움 지침 추가.
  - 로컬 자산(docs/templates/notice-view-*.json)은 운영 v3 와 어긋난 옛 v1 — 본 CR 에서 동기화하지 않음(별도 정리). 운영 DB 가 SSOT.
- **남은 작업**:
  - 양식 변경(발행일 칸·기본정보 표 제거) 화면 육안 검증 — 기존 공고 재생성 필요(미수행, 사용자 "검증 나중").
  - 교정 채팅 (Aimbase Chat Widget BFF 토큰 프록시 + contextProvider(noticeId+현 산출물) + 교정 WF). 미착수 — fulfillmentParty 오분류를 자연어로 교정하는 본류 기능.
- **PUT 함정(메모리화)**: Aimbase WF PUT 시 GET 응답 통째(createdAt/updatedAt/createdBy 등 포함)면 400. 허용 필드(id/name/triggerConfig/steps/domain/inputSchema/outputSchema/errorHandling/graphMode)만 남겨야 200.
- **상태**: 1차 구현·운영 배포·E2E 추출 정확도 검증 완료. 양식 정리(발행일·중복제거) 운영 반영 완료(화면 육안검증·교정 채팅은 다음).

---

### CR-038: 공고문 분석 WF 재설계(Claude Code 구조) + descriptionBody 입력 보강 + facts 영속화 (2026-06-04)

- **배경**: 공고문 한글화/분석 환각 문제. 노출 공고문 6개 검증 결과 첨부 0개(관제탑)·이미지뿐(LSA floor plan) 공고에서 환각 심함. 근원 실측 3개: ① 현행 WF가 단일 AGENT_CALL에 읽기+추출+요약 다 맡겨 **검증 STEP 0개**, ② parse_document만 의존하고 이미지(ocr_image/image_analysis)·실본문(descriptionBody) 안 읽음, ③ 추출 사실의 근거(sourceQuote) 영속화 0. 추가 실측: `parse_document`가 한때 등록 도구 아니었음 → Aimbase에서 정식 Tool로 개발·배포(2026-06-03, built-in 48 라이브).
- **재설계 그림 (Claude Code 1:1 이식)**: 작업장에 첨부·본문을 텍스트로 깔고 grep/read로 자율 탐색 + 검증/누락점검 분리 + fact 근거 영속화.
- **변경 사항**:
  1. Aimbase WF `opportunity-analysis` — 현행 4-STEP → **6-STEP 재설계 운영 PUT**: fetch → `build_workspace`(AGENT_CALL: parse_document/ocr_image/image_analysis로 첨부·descriptionBody를 .txt 적재) → `extract_facts`(AGENT_CALL: 작업장 grep/read, 각 fact에 sourceQuote/sourceFile/page/confidence 부착) → `verify_and_gapcheck`(AGENT_CALL: fact↔작업장 원문 대조 환각 색출 + 누락 점검) → `structure_output`(facts[] 추가) → `save`(facts 동반). 로컬: docs/workflows/opportunity-analysis.steps.v2.json.
  2. BE `buildOpportunityText` 3곳(NoticeService/ProposalMcpTool/AIWorkflowService) — 실본문 `descriptionBody` 우선 + `rawJson.description` 폴백. 첨부 없는 공고 환각 방지.
  3. BE `OpportunityTranslationService.ensureDescriptionBody` 신규 — 공고문 만들기 직전 본문 비었으면 noticedesc fetch 보강(REQUIRES_NEW, 호출자 readOnly 트랜잭션 함정 회피). NoticeService.generateAsync에서 WF 입력 구성 전 호출.
  4. **facts 영속화 (T3-1 캐스케이드)** — 별도 테이블 아니라 **Notice.extracted_facts_json longtext 컬럼**(factor_tree_json 패턴). 사유: 교정 채팅(CR-033)이 Notice를 markCompleted로 전체치환 저장하므로, facts가 Notice에 속해야 치환 시 함께 보존(별도 테이블이면 동기화 누락). save/get MCP tool이 facts 수신·반환.
- **PUT 함정 정정(메모리화)**: WF PUT 400의 진짜 원인은 `PlatformWorkflowRequest` record에 없는 필드(domain/graphMode/active/projectId 등) 전송 시 @Valid 거부. 허용 9필드: id/name/description/category/triggerConfig/steps/errorHandling/outputSchema/inputSchema. (옛 메모 "domain/graphMode 포함"은 틀림)
- **CR-033 교정 채팅과의 묶음**: save가 전체치환이라 facts 켜는 순간 교정이 facts 안 실으면 소멸 → facts 영속화 + 교정 채팅 get/save facts 처리는 한 묶음으로 구현.
- **규모**: 중규모(WF 전면 재설계 + 새 컬럼 + MCP 스키마). 설계 캐스케이드 = T3-1(Notice.extracted_facts_json).
- **상태**: WF 6-STEP 운영 PUT 완료, descriptionBody 보강 BE 배포·커밋 완료(18911a4). facts 영속화(컬럼·마이그레이션·get/save 처리) + 교정 채팅 위젯 = 진행 중.

---

### CR-039: 공고문 한글화/분석 강제 중단 + stuck 자동 정리 (2026-06-14)

- **배경**: 공고문 상세화면에서 한글화/분석이 ANALYZING(분석 중) 상태로 멈춰 "LLM이 공고를 한글화/요약하고 있습니다…" 스피너만 무한 폴링되는 현상. Aimbase 워크플로우 오류·타임아웃·네트워크 단절 시 발생. **실측 결과 이를 끊는 기능 부재** — BE에 cancel/abort 엔드포인트·메서드 없음([NoticeAdminController](../backend/src/main/java/com/biddingagency/controller/admin/NoticeAdminController.java), [NoticeService](../backend/src/main/java/com/biddingagency/domain/notice/service/NoticeService.java)), FE에 강제중단 버튼 없음([NoticeAdminDetailPage.tsx](../frontend/admin-console/src/pages/NoticeAdminDetailPage.tsx)). **함정**: 재생성 버튼은 `generationStatus === 'ANALYZING'`이면 disabled([NoticeAdminDetailPage.tsx:115](../frontend/admin-console/src/pages/NoticeAdminDetailPage.tsx#L115))라 stuck 상태에선 재생성으로도 빠져나올 수 없음 → 화면 영구 잠금.
- **Aimbase 실측(초기)**: 1차 구현 시점엔 Aimbase에 WF run 취소 REST API **미존재** → cancel 호출은 404 삼키는 best-effort로 선구현.
- **Aimbase cancel API 신설(CR-105, v3.10.0/v3.10.1, 2026-06-14)**: `POST /api/v1/workflows/runs/{runId}/cancel` **신설됨** — 우리가 추측으로 박은 경로와 정확히 일치. **협조적 중지**라 즉시 안 멈춤: `running`은 중지 표식만 세우고 현재 스텝은 끝까지 수행, 다음 스텝 경계에서 `cancelled` 전이. cancel 응답 status는 표식 직후라 아직 `running`일 수 있음. `pending_approval`은 즉시 `cancelled`, terminal은 멱등, 미존재 404. (가이드 §4-7 + 소비앱 UI 권장 패턴)
- **보강 구현 (가이드 §4-7 반영)**: `LLMPlatformClient.cancelWorkflowRun`을 `@Async`로 전환 — cancel 호출 후 응답이 `running`이면 `GET /api/v1/workflows/runs/{runId}`를 2.5초 간격 폴링해 terminal(cancelled/completed/failed) 확정까지 백그라운드에서 대기(경계 케이스: 중지보다 완료가 빨라 completed로 끝날 수 있음 → terminal 자체를 로깅). 화면 잠금 해제(우리 generationStatus 즉시 FAILED)는 비동기라 폴링에 막히지 않음 — "화면 복구 최우선" 유지하면서 백그라운드 run 실제 종료까지 확인. WorkflowRunResponse에 isCancelled()/isTerminal() 추가.
- **변경 사항**:
  1. **수동 강제 중단** — `NoticeService.cancelAnalysis(noticeId)`: ANALYZING이 아니면 거부(idempotent하게 무시 가능), Aimbase `cancelWorkflowRun(workflowRunId)` best-effort 호출 후 결과 무관 `Notice.markFailed("관리자 강제 중단")`. Controller `POST /api/admin/notices/{id}/cancel`. FE 상세화면 ANALYZING 시 "강제 중단" 버튼 노출 → 호출 후 상태 FAILED 되어 재생성 버튼 disabled 해제.
  2. **stuck 자동 정리** — `NoticeStuckCleanupScheduler`(`@Scheduled` fixedDelay=`notice.analysis.scheduler-interval-ms`, 기본 600000=10분): `analysisStartedAt`이 임계분(`notice.analysis.stuck-threshold-minutes`, 기본 60) 초과한 ANALYZING 건 조회 → 각각 cancelAnalysis 경로로 FAILED. 임계값은 설정값 — 운영에서 코드 수정 없이 조정.
  3. **Aimbase 취소 best-effort** — `LLMPlatformClient.cancelWorkflowRun(runId)`: cancel 엔드포인트 호출하되 404/실패/타임아웃을 삼키고 로그만 남김(우리 쪽 흐름 차단 안 함). Aimbase가 API를 추가하면 자동 동작.
  4. **신규 컬럼 `analysis_started_at`** — markAnalyzing 시점 기록. `updatedAt`(@LastModifiedDate)은 markAnalyzing 후 무관한 update에 밀려 경과시간 판정에 부정확 → 전용 필드. 마이그레이션 Vxx(다음 번호).
- **정책 신설**: POL-xxx 공고문 분석 stuck 임계 — `stuck-threshold-minutes` 기본 60(설정값). 초과 시 자동 FAILED.
- **상태 전이**: ANALYZING → FAILED 경로 추가(수동 cancel / 자동 cleanup 둘 다 동일 markFailed). FSM이 아닌 generationStatus enum 전이이나 화이트리스트 정신 따라 ANALYZING에서만 cancel 허용.
- **규모**: 중규모(화면·API·새 컬럼·스케줄러). 설계 캐스케이드 = T1-3(비즈니스 규칙)·T1-4(정책)·T3-1(데이터 모델: analysis_started_at)·T3-2(API: cancel)·T3-3(화면: 강제중단 버튼).
- **상태**: 설계 캐스케이드 + 1차 구현·커밋(ec45f3b)·배포·검증 완료 (2026-06-14). cancel 엔드포인트 매핑 401 확인, NoticeServiceTest 8건 통과. Aimbase CR-105 cancel API 신설 반영해 협조적 중지+@Async 폴링 보강 구현 완료(가이드 §4-7).

---

### CR-040: 공고문 삭제 기능 (2026-06-14)

- **배경**: 잘못 만든/실패(FAILED)한 공고문을 목록에서 치울 방법이 없음. CR-039로 "강제 중단(→FAILED 전환)"은 됐지만 row 제거(삭제)는 별개 — 사용자 요청.
- **실측(연관 데이터 안전성)**:
  - **BidRequest(고객 신청)는 Opportunity를 참조**([BidRequest.java:43](../backend/src/main/java/com/biddingagency/domain/bid/entity/BidRequest.java#L43)), Notice 직접 참조 아님 → 공고문 삭제해도 고객 신청·제안서 문서 무영향.
  - **Notice → Opportunity 단방향**(역참조 FK 없음, V13 FK는 opportunity 삭제 시 notice cascade — 역방향 아님) → Notice 삭제 시 원본 Opportunity 보존(BIZ-004).
  - **VerificationLog**(targetType=NOTICE, targetId=noticeId)는 FK 아닌 느슨한 참조 → 함께 삭제(고아 방지).
  - **NotificationLog 정리 불필요(실측 반박)**: referenceType은 CollectorRun/BidRequest/Opportunity 등 엔티티명, referenceId에 우리 Notice UUID를 기록하는 알림 없음. 알림 vars의 `noticeId`는 SAM 공고번호(string, `opp.getNoticeId()`)이지 Notice 엔티티 PK 아님. → 삭제 대상 아님.
- **변경 사항**:
  1. `NoticeService.deleteNotice(noticeId)` — 가드 후 hard delete. 가드: `isVisible()`이면 409(노출 중 삭제 금지), `isAnalyzing()`이면 409(분석 중 삭제 금지). 통과 시 VerificationLog(NOTICE,noticeId) deleteAll → noticeRepository.delete.
  2. Controller `DELETE /admin/notices/{id}`.
  3. FE 상세화면 "삭제" 버튼 — 확인 모달 후 호출, 성공 시 목록으로 이동. VISIBLE/ANALYZING이면 버튼 비활성 또는 서버 409 메시지 노출.
- **삭제 정책**: 완전 삭제(hard) — 소프트 삭제 안 함(복구는 원본 재선별로 공고문 재생성). 노출 중·분석 중만 금지, 그 외(PENDING/FAILED/COMPLETED+HIDDEN) 허용.
- **규모**: 소~중규모(새 테이블·FSM·이벤트·마이그레이션 없음, 엔드포인트 1개 + 화면 버튼 1개). 캐스케이드 = T1-3(BIZ 삭제 가드)·T3-2(API DELETE)·T3-3(화면 삭제 버튼).
- **상태**: 설계 캐스케이드 진행중 (2026-06-14).

---

### CR-041: 공고문 분석 WF `build_workspace` 탈-LLM 재구성 (FOREACH+download_file) (2026-06-14)

- **배경**: CR-038이 `build_workspace`를 AGENT_CALL(LLM) 단일 STEP으로 도입했으나, 이 스텝의 실제 일은 "첨부·본문을 텍스트로 변환해 작업장에 적재"하는 **결정론적 변환·복사**다(프롬프트에 "분석/요약 하지 마세요, 변환·저장만"이라 못박힘). LLM 판단이 필요 없는 일을 AGENT_CALL(CLI 두뇌)로 보내면서 발생한 비용:
  - CLI turn 비용 발생
  - CLI가 도구를 순차 자율 호출 → 느리고, 어디서 막혔는지 안 보임(가시성 0)
  - turn timeout / retry 멱등화(Aimbase CR-106) 같은 복잡도까지 끌고 옴
- **Aimbase와의 역할 분담(아임베이스 대화)**: (A) ParseDocumentTool url 비전 분기 = Aimbase가 처리(우리 무관). (B) `build_workspace` 탈-LLM = **소비앱(우리) 워크플로우 영역** → 본 CR. 둘은 독립이 아니라 보완(LLM을 빼면 가시성 문제도 함께 해소 — TOOL_CALL은 STEP_START/TOOL_USE/TOOL_RESULT가 이미 타임라인에 실시간 적재, Aimbase CR-090).
- **사용자와 확정한 구조(실측 기반)**:
  - **2번 적재(build_workspace)**: AGENT_CALL(LLM) → **FOREACH + TOOL_CALL{tool: download_file}**. 각 첨부 URL을 결정론적으로 workspace 파일로 다운로드. LLM 0, 병렬(`mode: parallel`), `on_item_error: continue`로 1건 실패해도 나머지 진행. (운영 동테넌트 `type-pattern-extraction`의 FOREACH+parse_document가 검증된 레퍼런스 패턴 — [opportunity-analysis.steps.v2.json 신버전 예정])
  - **3번 분석(extract_facts)**: AGENT_CALL 유지(여기는 grep 키워드·FACTOR 계층화 등 LLM 판단 필요). 프롬프트만 조정 — workspace의 PDF는 `read`(바이너리는 메타만 반환)가 아니라 **`parse_document(file_path=)`로 비전 분석**(Aimbase CR-095: PDF 3MB↓이면 base64 document block을 Claude에 직접 주입, 텍스트추출 손실 없음). PDF 원본 보존.
  - 나머지 STEP(fetch / verify_and_gapcheck / structure_output / save) 무변경.
- **실측 근거**:
  - 운영 `opportunity-analysis`에 v2(AGENT_CALL build_workspace)가 **이미 배포됨**(GET 확인) — "검토용 초안"이라던 v2가 라이브였음.
  - 첨부 `downloadUrl` = `http://59.8.160.12:8183/api/mcp/opportunity-attachments/{id}/download`([NoticeService.buildAttachmentFiles](../backend/src/main/java/com/biddingagency/domain/notice/service/NoticeService.java)) — 공인 IP, JWT 불필요한 server-to-server 경로. Aimbase(59.8.160.12:8280)에서 직접 GET 가능. (MinIO 아닌 로컬 스토리지)
  - 순수 "URL→workspace 파일" 단일 도구는 Aimbase에 부재(실측). `parse_document`/`http_request`는 다운로드하나 파일로 안 떨굼, `file_write`는 텍스트 content만, `bash`는 `curl -o` 가능하나 부자연. → **Aimbase가 `download_file` 도구 신설**(요청 수락, 구현 중).
- **CR-038 결정 뒤집기**: CR-038 designNotes 라인 13 "작업장 구성을 FOREACH 분기 대신 AGENT_CALL 단일 STEP으로 ... 파일타입 분기 같은 세부는 LLM 자율 (메모리 합의)" — 본 CR이 이 합의를 폐기. 사유: 파일타입 분기는 download_file이 contentType 무관 바이트 복사로 흡수하고, PDF 비전 판단은 3번 extract_facts(LLM)가 parse_document로 처리하므로 2번에 LLM이 있을 이유가 소멸.
- **변경 사항**:
  1. `docs/workflows/opportunity-analysis.steps.v2.json` — `build_workspace` STEP을 AGENT_CALL → FOREACH(items={{input.attachmentFiles}}, body=TOOL_CALL{download_file}) + 본문(opportunityText) file_write로 재작성. `_meta.designNotes`의 FOREACH 반대 메모 갱신, `stepSummary` 유지.
  2. `extract_facts` 프롬프트 — "parse_document 다시 호출하지 마라" → "workspace의 PDF는 parse_document(file_path=)로 비전 분석"으로 조정.
  3. 운영 Aimbase에 PUT(`/api/v1/workflows/opportunity-analysis`). PUT 허용 9필드 준수(CR-038 함정 정정).
  4. **BE/FE/MCP/엔티티 무변경** — input.attachmentFiles는 이미 downloadUrl 포함(CR-019), save 콜백 스키마 동일.
- **규모**: 중규모(WF STEP 구조 변경). 단 데이터 모델·API·화면·이벤트 무변경 → 캐스케이드는 본 CR 이력 + workflows JSON 자산만(T1~T3 본문 영향 없음 — WF 내부 재구성이라 기능요구사항/API/화면 불변).
- **선행 의존(해소됨)**: ①Aimbase `download_file` 도구 신설(EnhancedToolExecutor, url+file_path+overwrite, 바이너리 무손실 Files.write, 50MB상한, 부모디렉토리 자동생성). ②**블로커 실측·해소**: 초기엔 TOOL_CALL 스텝이 `toolRegistry.execute(ToolCall)` **1-인자** 호출→EnhancedToolExecutor default bridge가 `ToolContext.minimal(null,null)` 합성→WorkspaceResolver가 `default/general` 폴백→run 격리 깨짐(download_file이 엉뚱한 workspace에 저장, extract_facts가 못 찾음). type-pattern-extraction(FOREACH+parse_document)이 멀쩡한 이유는 parse_document(url)이 workspace에 파일 안 떨구고 output으로만 반환해 경로의존 0이라 폴백 무해였음(모순 해소). → **Aimbase CR-107**로 ToolCallStepExecutor가 StepContext의 workspacePath를 담은 ToolContext로 **2-인자** execute 호출하도록 수정. StepContext.workspacePath 필드 신설(withStepResult 등 복제 시 보존), WorkflowEngine이 resolveWorkspacePath(sessionId)로 세팅, FOREACH injectItem도 보존. 실측 확정(2026-06-14).
- **구현(JSON)**: build_workspace(AGENT_CALL) 삭제 → `write_description`(TOOL_CALL file_write: 본문→description.txt) + `download_attachments`(FOREACH+download_file: 첨부 원본→attachments/{{item.fileName}}, parallel/max_concurrency=4/max_items=60/on_item_error=continue) 분리. 둘 다 depends_on fetch_opportunity라 병행. `extract_facts` 프롬프트: PDF는 read(메타만) 아닌 `parse_document(file_path="attachments/<파일명>")` 비전, 첨부 메타({{input.attachmentFiles}}) 직접 제공, _INDEX.md 제거→glob. `verify_and_gapcheck` 작업장 안내도 attachments/ 구조로 갱신. _meta version v3.
- **상태**: **운영 PUT 완료(2026-06-15)** — `PUT /api/v1/workflows/opportunity-analysis` success, GET 재확인(7-STEP 라이브). 허용 7필드(id/name/triggerConfig/steps/errorHandling/outputSchema/inputSchema)만 전송. 운영 백업 /tmp/opp-analysis-backup-20260615.json. **남음**: 실제 공고로 다운로드→PDF비전분석 E2E 검증.

---

### CR-114: 공고문 분석 WF `structure_output`(LLM JSON 변환) 제거 → BE 결정론적 매핑 (2026-06-16)

- **배경**: 공고문 분석 run이 save에서 FAILED. **5층 진단(실측 확정)**: 마지막 STEP `structure_output`(LLM_CALL, connection cli-runner-bidding-001)이 검증된 분석을 save 스키마 JSON으로 변환하는데, **sonnet-4.5가 CLI 경로에서 ```json 코드펜스로 감싼 텍스트**로 응답 → Aimbase `LlmCallStepExecutor.extractStructuredData`가 `ContentBlock.Structured`만 수집하므로 못 줍음 → `structured_data=null` → save STEP이 `{{structure_output.structured_data.koreanTitle}}` 등을 빈 값으로 받아 FAILED.
- **실측 근거 (20개 run 전수조사 + CLI 격리테스트)**:
  - structure_output이 `structured_data`를 채운 run: **1/20**. 그 1건만 모델 `claude-sonnet-4-20250514`(4.0), 실패 19건은 전부 `claude-sonnet-4-5`(4.5) 또는 socket error.
  - 모델 교체 테스트(CLI `-p`): sonnet-4.5·**haiku-4.5 = ```json 펜스 붙임(❌)**, opus-4.8 = 순수 JSON(✅, 단 49초/$1.10). → **모델로 못 고침**. 싼 모델일수록 오히려 펜스. opus는 과·비싸·근본해결 아님(원복함).
  - run 4437ec80: sonnet-4.5가 **올바른 JSON을 ```json으로 다 만들어냈는데도** 텍스트라 버려짐 = "모델은 일했고 받는 계층이 못 받음".
- **본질 판단(사용자 합의)**: "이미 만들어놓은 분석을 JSON으로 정리"는 LLM이 **잘 못하는**(펜스/키틀림/환각) 일이고 **결정론적 코드가 가장 잘하는** 일. 증상 처리(펜스 닦기)·모델 교체(opus)는 다 근원(LLM에게 정형출력 위임)을 안 건드린 우회. → **본질 = structure_output(LLM_CALL) 제거, 변환은 BE 코드**.
  - 실측: NOTICE_VIEW `contentJson`은 DB `DocumentTemplate`(관리자 등록 TipTap JSON)의 `{{변수}}` 자리를 fact로 **치환**하는 것(`NoticeService.buildNoticeViewTemplate` 481~489). 치환 = 코드의 본업. LLM의 "골격 전수 유지(heading 빼먹지 마라)" 같은 불안정 지시가 통째로 소멸 — 코드는 골격을 절대 안 건드림.
  - verify(AGENT_CALL)는 검증 본업 유지하되 출력을 **자유 텍스트가 아닌 정형 JSON**(verifiedFacts + 스키마 필드)으로 받아 BE가 파싱 가능하게 함.
- **변경 사항(설계)**:
  1. **WF**: `structure_output` STEP 제거. `verify_and_gapcheck` 출력 계약을 깔끔한 JSON으로 정형화(koreanTitle/summary/requiredDocuments·factors·eligibility/facts[] — contentJson은 BE가 채움). `save` STEP의 input을 verify 출력 직결로 변경.
  2. **BE**: `OpportunityAnalysisMcpTool.saveOpportunityAnalysis` 또는 신규 매퍼가 verify JSON을 받아 (a)save 스키마 필드 매핑 (b)NOTICE_VIEW 템플릿 `{{변수}}` 치환으로 contentJson 결정론적 생성. LLM 변환 0.
  3. **방어**: verify 출력이 혹시 ```json 펜스로 오면 BE가 벗겨 파싱(stripCodeFence fallback) — 어떤 모델이든 받아냄.
- **규모**: 중규모(WF STEP 제거 + verify 출력 계약 변경 + BE 신규 매핑/치환 로직). 데이터 모델·화면 무변경(save 스키마·notice_extracted_fact 동일). 캐스케이드 = 본 CR 이력 + workflows JSON + BE 매퍼.
- **선행 관계**: CR-112(3층 value유실 수정, verify에 value필수)·4층(CLI 단절 둔갑, aimbase 수정완료) 이미 해소 → 이번 run에서 extract/verify는 완주·value 정상 확인됨. 남은 단일 차단요인이 본 CR(5층).
- **상태**: **구현 완료(2026-06-16)**. 진행 경과:
  1. **5층 1차 해소 (structure_output 제거)**: structure_output(LLM_CALL)을 verify_and_gapcheck(AGENT_CALL)에 response_schema로 흡수. AGENT_CALL은 structured_output 가상tool 호출(CR-088)이라 ```json 펜스 원천소멸. 운영 PUT 완료. → E2E run(b0a690f8/f13393e4)에서 save COMPLETED, facts 37개 정상. (별도 [[cr114-structure-output-removed-into-verify]] 메모리)
  2. **contentJson 추가 발견·제거 (본 CR 본질)**: verify가 facts/summary/requiredDocuments는 다 채우는데 contentJson만 null로 회피(실측). 원인=response_schema가 contentJson을 `["object","null"]`로 null허용 → 무거운 골격 29노드 치환을 모델이 생략. **본질 판단**: contentJson에 들어갈 값은 전부 facts/summary/requiredDocuments의 재배열(새 정보 0) → LLM이 아니라 화면이 데이터로 직접 그릴 일.
  3. **구현**: ①WF — verify response_schema·프롬프트·save input에서 contentJson 전면 제거(extract/verify 프롬프트의 noticeViewTemplate 골격 입력도 제거). 운영 PUT v7 완료(connection_id 덮어쓰기 없음 검증). ②FE admin — `NoticeDocumentView`를 contentJson(TipTap) 렌더러 → summary/requiredDocuments 데이터 직접 렌더러로 교체. 노드 컴포넌트(KvTable/DataTable/CalloutList/Heading/NoticeHeader/EligibilityBlock) 재사용, 메인이 8섹션 결정론 조립. 빌드 성공. ③FE customer — 무변경(BidDetailPage는 contentJson 조건부+summary 자체렌더라 안 깨짐, Proposal*의 contentJson은 제안서 도메인 별개).
- **규모 정정**: BE 매퍼 불필요(화면이 직접 렌더). 변경 = WF 정의 + FE admin 컴포넌트. 데이터 모델·API 무변경.
- **남음**: 사용자 E2E 테스트(새 렌더러로 공고문 화면 표시 확인). NOTICE_VIEW 양식(DocumentTemplate)·notice.content_json 컬럼은 이제 미사용 → 추후 정리 가능(당장은 무해).

---

### CR-115: 공고분석 진행 STEP 실시간 표시 + 분석 완료 자동 전환 (2026-06-18)

- **배경**: 공고문 상세 화면이 한글화/분석 중(ANALYZING)일 때 "LLM이 공고를 한글화/요약하고 있습니다…" 스피너 한 줄만 무한히 돌아 답답함. 게다가 **전 STEP이 끝나(COMPLETED) 본문이 준비됐어도 화면은 ANALYZING 스피너에 멈춰** 새로고침을 해야만 정상 노출됨(실측). 화면이 단계 진행 상황을 전혀 모름.
- **원인(실측)**:
  1. FE `NoticeAdminDetailPage`가 `useEffect(()=>{fetchData()},[id])`로 **1회만 조회**, 폴링 없음 → ANALYZING→COMPLETED 전이를 화면이 감지 못함(새로고침 버그).
  2. 진행 STEP을 화면에 줄 경로 자체가 없었음. (Aimbase run 응답에 `currentStep`(id)·`stepResults`는 있으나 소비앱이 안 씀, BE 폴링은 동기 블로킹이라 화면 노출 경로 없음.)
  3. 운영 WF step.name에 개발용어(`(탈-LLM, CR-041)`, `opportunityText를 description.txt로`, `fact↔작업장 근거 일치`)가 섞여 사람이 읽을 라벨로 부적합. **step.name이 본래 사람 라벨용**인데 디버깅 주석처럼 쓰였음(사용자 지적).
- **변경 사항**:
  1. **WF 정의(소비앱 소유)**: `opportunity-analysis` 6개 step의 `name`을 사용자 라벨로 교체 — 공고 정보 불러오는 중 / 공고 본문 준비 중 / 첨부파일 받는 중 / 공고 내용 분석 중 / 분석 결과 검증 중 / 저장 중. **로직(config/depends_on/connection_id) 무손실** — 운영 현행 정의를 받아 name만 교체해 PUT.
  2. **BE**: `WorkflowRunResponse`에 `currentStep`/`currentStepName`/`steps[]` 수신 필드 추가(Aimbase 합의 계약, 2026-06-18). `LLMPlatformClient.getRun(runId)` 단발 GET(폴링 아님). `NoticeAnalysisProgressDto` — Aimbase가 steps[] 미반영이어도 BE가 `stepResults`+`currentStep`으로 status 합성하는 **fallback 내장**(Aimbase 완성 전에도 동작). `NoticeService.getAnalysisProgress()` + `GET /admin/notices/{id}/progress`.
  3. **FE(admin)**: ANALYZING 동안 4초 폴링 → 스피너 1줄을 **6단계 체크리스트**(완료 ✓/진행 ⟳/대기 ○/실패 ✗)+현재 단계명 헤더로 교체. 폴링 중 상태가 ANALYZING이 아니게 되면 폴링 중단 + `fetchData()` 자동 호출 → **새로고침 버그 해결**(완료 시 본문 자동 노출).
- **Aimbase 합의(별도 작업)**: run 단건 조회(`/{id}/runs/{runId}`, `/runs/{runId}`) 응답에 `currentStepName`(WF 정의 step.name lookup) + `steps[]`(`{id,name,status}`) 추가. WorkflowController에 `WorkflowRunDetail` record 추가, 빌더가 run+WF정의로 status 도출. 소비앱은 이 필드를 그대로 역직렬화 → 미반영 기간엔 BE fallback 합성으로 동작.
- **규모**: 중규모(신규 조회 API 1개 + FE 폴링 + WF name 정리). 데이터 모델·이벤트·FSM 무변경. 캐스케이드 = 본 CR 이력 + workflows JSON 동기화 + (필요 시 T3 API/화면 갱신).
  4. **BE(runId 저장 — 진행조회 전제)**: `markAnalyzing(null)` + 동기 폴링 구조라 ANALYZING 중 `workflow_run_id`가 NULL → progress가 run 조회를 못 해 단계가 안 떴음. `analyzeOpportunity(input, onRunStarted)` 오버로드로 run POST 성공(runId 확보) 직후 콜백 → `NoticeService.saveWorkflowRunId`(REQUIRES_NEW 즉시 커밋) + `Notice.attachWorkflowRunId`(상태·시작시각 불변).
- **상태**: **구현·운영배포·검증 완료(2026-06-18)**. 진행 경과:
  1. WF name 운영 PUT(허용 8필드만 — `projectId/graphMode/active` 섞이면 400, 제거 후 200. connection_id 무손실 검증).
  2. Aimbase API 배포 확인 — 단건 조회에 `currentStepName`+`steps[]` 정상, 소비앱 필드명과 일치(코드 수정 불필요).
  3. **함정①(FE/BE 배포 비대칭)**: 첫 `./deploy.sh be`가 운영 jar(6/14자)를 실제 갱신 못한 채 FE만 배포돼, 새 FE의 `/progress`를 구 BE가 401 거부 → 인터셉터 로그아웃. BE 재배포로 해소. **교훈**: 배포 후 jar 시각·컨테이너 기동시각 실측 필수.
  4. **함정②(runId NULL → 단계 안 뜸)**: 위 경과 4의 수정. 운영 검증 완료(W91QVN26QA022, 08:09 시작 → runId e1926069 저장 로그·DB 확인).
  5. **부수 해결**: 분석 완료 후 ANALYZING 스피너에 멈춰 새로고침해야 했던 문제 — FE 폴링이 종료 감지 시 `fetchData()` 자동 호출로 해소.
- **남음**: (선택) T3 캐스케이드 — WF 내부 재구성+조회 API라 기능요구사항/화면 본질 불변, 우선순위 낮음.

---

### CR-116: 원본 공고 목록 첨부 표시 + 수동 업로드 ZIP 파일명 오업로드 경고 (2026-06-18)

- **배경(사용자 요청)**:
  1. 원본 공고 목록(`OpportunityAdminPage`)의 "기관" 컬럼이 전부 동일(411th CSB 등)해 식별 정보가 안 됨 → 목록에서 제거(상세에서만 표시).
  2. 첨부파일 유무를 목록에서 바로 알 수 없음 → **첨부 없는 공고 표시** 요청.
  3. PIEE에서 입찰서류를 받으면 `W91QVN26QA030.zip`처럼 **공고번호.zip** 형태로 저장됨(중복 다운로드 시 `W91QVN26QA022 (1).zip`). 관리자가 무의식적으로 그대로 올리는데, 다운로드 폴더에 섞인 **다른 공고의 zip을 잘못 올리는 사고**를 막을 장치 요청.
- **변경 사항(FE only — BE는 이미 `attachmentCount` 제공, 변경 없음)**:
  1. [OpportunityAdminPage.tsx](../frontend/admin-console/src/pages/OpportunityAdminPage.tsx) — "기관" 컬럼 제거 + "첨부" 컬럼 추가: `attachmentCount>0`이면 📎 개수, `0`이면 노란 `없음` 배지.
  2. [OpportunityAdminDetailPage.tsx `handleFileUpload`](../frontend/admin-console/src/pages/OpportunityAdminDetailPage.tsx#L137) — 업로드 파일명이 **공고번호 패턴**(`.zip`·`(n)`·공백 제거 후 `^[A-Z0-9]{10,}$`)인데 현재 공고 `solicitationNumber`와 다르면 `confirm` 경고. 취소 시 업로드 중단, 확인 시 진행. **일반 파일명(report.zip 등)은 경고 안 함**(PIEE 산출물이 아니므로). 업로드 자체는 막지 않음(경고만).
- **규모**: 소규모(단일 화면 2개 UI, 새 API·테이블·FSM 없음). BE 무변경.
- **상태**: **구현·운영 배포 완료(2026-06-18)** — `./deploy.sh fe`, FE tsc 통과, 사용자 운영 화면 확인 완료.

---

### CR-117: 공고문 분석 화면 — 옛 NOTICE_VIEW 10섹션 골격 + 고정 번호 복원 (2026-06-18)

- **배경(사용자 지적)**: CR-114에서 contentJson(LLM이 NOTICE_VIEW 템플릿 골격을 채운 TipTap JSON) 렌더를 폐기하고 화면이 summary/requiredDocuments로 직접 8섹션을 조립하게 바꿨는데, 이 과정에서 ①섹션이 10→8개로 줄고 ②번호 매김이 사라지고 ③"1.공고 기본 정보" 표 행 구성이 바뀜(옛 6행→공고번호/예상금액/평가방식 3행). 사용자가 "예전엔 10개·번호 있었음", "6번이 자격요건이었음", "기본정보가 과거와 다름"을 지적.
- **실측 근거(운영 DB document_templates, active=1 NOTICE_VIEW)**: 옛 골격 = 10개 H2 섹션(1.공고 기본 정보 / 2.내용(Scope) / 3.계약 기간 / 4.현장 설명회 / 5.담당자 / **6.자격 요건** / 7.낙찰 기준 / 8.참고 사항 / 9.특별 유의 사항 / 10.타임라인). 1번 표 kvTable = 6행 고정 골격(공고번호/공고유형/조달방식/발주기관/마감일/NAICS), LLM은 `{{meta.*}}` 변수 자리에 값만 치환(표 구조는 공고 무관 고정). → 사용자 기억 정확.
- **결정(사용자 합의)**: ①화면 섹션 골격만 복원(CR-114의 FE 직접조립 구조 유지, LLM 골격치환은 부활 안 함). ②**고정 번호 1~10**, 데이터 없는 섹션도 골격 유지("해당 없음"). ③옛 8.참고사항+9.특별유의사항은 하나로 합침(특이사항=specialNotes). ④1번 표는 옛 골격 복원하되 **값 있는 행만 동적 노출**(조달방식은 전용 컬럼 없어 제거, 평가방식은 1번 표에서 빼 8.낙찰기준 전용 섹션으로 이관).
- **소급 문제(사용자 질문 "다시 생성해야 하나")**: 둘로 나뉨.
  - `contractPeriod`/`siteVisit`(4·5번 신규 섹션 값) = LLM 추출 필드라 **기존 분석본엔 없음 → 재분석해야 채워짐**(실측: 완료 8건 전부 0). 미재분석 시 "해당 없음" 표시.
  - 1번 표 6행 값(공고유형/NAICS 등) = **opportunity 메타라 재분석 불필요**, 기존 8건도 즉시 채워짐.
- **변경 사항**:
  1. **WF 정의([opportunity-analysis.steps.v2.json](../workflows/opportunity-analysis.steps.v2.json))**: verify `response_schema.summary`에 `contractPeriod`/`siteVisit`(string) 추가. extract_facts 프롬프트 분석 항목에 계약기간(contractPeriod)·현장설명회(siteVisit) 명시. verify Gap Check 체크리스트에 계약기간 추가. _meta version v8. **운영 PUT 완료**(허용 필드만, connection_id·adapter 무손실 diff 검증 — summary 필드 추가 2개/제거 0, AGENT_CALL connection/timeout 무변경).
  2. **BE([NoticeAdminDto.java](../backend/src/main/java/com/biddingagency/domain/notice/dto/NoticeAdminDto.java))**: `noticeTypeKo`(opp.typeKo ?? opp.type)·`naicsLabelKo`(opp.naicsLabelKo) 2필드 추가. opportunity 직접 매핑(LLM 무관) → 소급 즉시 반영.
  3. **FE([NoticeDocumentView.tsx](../frontend/admin-console/src/components/NoticeDocumentView.tsx))**: NoticeSummary에 contractPeriod·siteVisit, Props에 noticeTypeKo·naicsLabelKo·responseDeadline 추가. 렌더를 10섹션 고정 번호 골격으로 재작성(EmptySection "해당 없음"). basicRows를 옛 6행(값 있는 행만)으로 재구성. 죽은 withIssuedDate 헬퍼 삭제. [NoticeAdminDetailPage.tsx](../frontend/admin-console/src/pages/NoticeAdminDetailPage.tsx) 호출부 새 필드 전달 + NoticeDetail 타입 확장.
- **규모**: 중규모(화면 구조 + 응답포맷 schema + DTO 변경). 데이터 모델·이벤트·FSM 무변경. 사용자 합의로 설계 캐스케이드(T1~T3) 생략, 본 CR 이력 + workflows JSON으로 갈음.
- **상태**: **구현·운영 배포 완료(2026-06-18)** — WF PUT(v8) + `./deploy.sh all`(BE jar 교체·컨테이너 재기동 health 200 + FE 2종). BE compileJava·FE admin tsc 통과. **남음**: ①사용자 운영 화면 확인(1번 표 6행·10섹션 번호) ②`contractPeriod`/`siteVisit` 채우려면 공고 재분석 1건 E2E(기존본은 "해당 없음").

---

### CR-118: 원본 공고 목록 검색·필터 (제목·본문 키워드 + 공고유형 + 첨부유무) (2026-06-18)

- **배경(사용자 요청)**: 원본 공고 목록(`OpportunityAdminPage`)에 검색 기능이 전혀 없어 공고가 쌓이면 찾기 어려움. 제목·내용 키워드 검색, 공고유형(셀렉트박스), 첨부유무 필터 요청.
- **결정(사용자 합의)**:
  - "내용" 검색 범위 = **원문 본문(descriptionBody) + 한글 본문(descriptionSummaryKo)** 둘 다(미수집 공고는 본문이 비어 검색 안 됨 — 설계상 정상).
  - 공고유형 셀렉트 옵션 = **DB DISTINCT 동적**(실제 수집된 type/typeKo, 새 유형 자동 반영).
- **변경 사항**:
  1. **BE [OpportunityRepository.java](../backend/src/main/java/com/biddingagency/domain/opportunity/repository/OpportunityRepository.java)** — `searchAdminFiltered(keyword,type,hasAttachment,pageable)` 통합 쿼리(모든 파라미터 nullable, null이면 조건 무시). keyword = title/titleKo/descriptionBody/descriptionSummaryKo/solicitationNumber/noticeId LIKE. type = 정확 일치. hasAttachment = `OpportunityAttachment` EXISTS/NOT EXISTS 서브쿼리. 정렬은 기존과 동일(postedDate DESC, createdAt DESC). + `findDistinctTypes()`(type/typeKo DISTINCT).
  2. **BE [OpportunityService.java](../backend/src/main/java/com/biddingagency/domain/opportunity/service/OpportunityService.java)** — 위임 메서드 + 빈 문자열 → null 정규화 + 유형 목록 [{type,typeKo}] 매핑.
  3. **BE [OpportunityAdminController.java](../backend/src/main/java/com/biddingagency/controller/admin/OpportunityAdminController.java)** — `GET /admin/opportunities`에 `keyword`/`type`/`hasAttachment` 쿼리파라미터 추가(기존 `findAllActive` → `searchAdminFiltered` 교체). + `GET /admin/opportunities/types`(셀렉트 옵션).
  4. **FE [client.ts](../frontend/admin-console/src/api/client.ts)** — `getAdminOpportunities(page, filters)` 시그니처 변경(옛 `q` 폐기, BE 미수신이라 무동작이었음) + `getAdminOpportunityTypes()`.
  5. **FE [OpportunityAdminPage.tsx](../frontend/admin-console/src/pages/OpportunityAdminPage.tsx)** — 검색바: 키워드 입력(엔터·버튼 확정), 공고유형 셀렉트(동적), 첨부유무 셀렉트(전체/있음/없음), 초기화 버튼. 필터·키워드 확정 시 첫 페이지로 리셋.
- **규모**: 중규모(조회 API 파라미터 확장 + 신규 types API + FE 검색바). 데이터 모델·이벤트·FSM·마이그레이션 무변경. 사용자 승인 후 설계 캐스케이드 생략, 본 CR 이력으로 갈음.
- **상태**: **구현·운영 배포 완료(2026-06-18)** — `./deploy.sh all`(BE 재기동 health 200, /types 라우팅 401=정상 + FE 2종). BE compileJava·FE admin tsc 통과.

---

### CR-119: 공고분석 — 계약기간(contractPeriod) 빈칸 보정 (키 생략 차단 + PWS 원문 추출 원칙) (2026-06-18)

- **배경(사용자 지적)**: CR-117에서 4번 "계약 기간" 섹션(contractPeriod) 골격을 복원했는데, 실제 분석본(W91QVN26QA022 — Purchase NTVs for DLA)에서 계약기간이 빈칸으로 표시됨. 사용자 "계약기간이 누락된 것 같다".
- **실측 진단(소스+운영DB+Aimbase WF 직접 확인)**:
  - WF·FE·BE 모두 contractPeriod **구조는 정상**. 운영 `opportunity-analysis` WF verify `response_schema.summary`에 contractPeriod 정의됨(CR-117 v8 PUT 반영, updatedAt 06-17). FE [NoticeDocumentView.tsx:175](../frontend/admin-console/src/components/NoticeDocumentView.tsx#L175) 4번 섹션이 `summary.contractPeriod` 렌더. BE는 summaryJson Map 통째 저장(검증은 overview만 필수).
  - **진범 = verify step structured_output이 contractPeriod/siteVisit 키 자체를 생략**. 해당 run summaryJson 키 8개뿐(2개 누락). `summary.required:['overview']`라 누락돼도 스키마 통과 → FE None → 빈칸.
  - 이 공고 원문 사실: 별도 period of performance 없음(물품구매). 실질 기간=납품 후 90일(`No Later Than 90 calendar days after receipt of purchase order`, p46)이나 이는 keyDates에 존재. 모델이 contractPeriod를 비운 판단 자체는 원문과 일치.
- **결정(사용자 합의)**:
  - **계약기간은 PWS/원문에 있는 그대로** — 납품기한 등 다른 항목에서 끌어와 변환·매핑하지 않는다(사용자 "계약기간은 그냥 PWS 있는 그대로"). 원문에 PoP 문구 없으면 빈 문자열, 단 **키는 반드시 출력**.
  - "전체 1년씩 표기"(용역 가정)는 적용 안 함 — 사용자가 무시 지시.
  - required 강제 범위 = **summary 10개 전체**(어떤 공고든 10섹션 골격이 항상 동일하게 채워지도록 근본 고정).
- **변경 사항(운영 Aimbase WF만, BE/FE 코드 무변경)**:
  1. **WF 정의([opportunity-analysis.steps.v2.json](../workflows/opportunity-analysis.steps.v2.json))** — verify `response_schema.summary.required`: `[overview]` → **10개 전체**(키 생략 원천 차단). extract_facts·verify 프롬프트에 "★계약기간 추출 원칙★: PWS/원문 문구 그대로 추출, 변환·추측 금지, 없으면 빈 문자열이되 키 생략 금지" 추가. _meta version v9. **운영 PUT 완료**(HTTP 200, updatedAt 06-18 05:25). domain·connection_id(cli-runner-bidding-001) 무손실 검증.
- **함정 교훈(CR-112 갱신)**: WF PUT 400의 실제 원인 규명. `/api/v1/workflows`는 `WorkflowController`(DTO 8필드: id/name/**domain**/triggerConfig/steps/errorHandling/outputSchema/inputSchema)이지 PlatformWorkflowController(category/description)가 아님. **GET 응답을 그대로 PUT하면 GET 전용 추가필드(_meta/projectId/graphMode/active/category/description/updatedAt)가 unknown property로 400**. DTO 8필드만 추려 보내야 통과.
- **규모**: 소~중(WF 프롬프트·스키마 보정, CR-117 후속 버그보정). BE/FE/데이터모델/마이그레이션 무변경. 설계 캐스케이드 생략, 본 CR 이력 + workflows JSON으로 갈음.
- **상태**: **WF 수정·검증 완료(2026-06-18)**. 사용자 결정으로 이 공고 재분석(E2E)은 **보류** — 다음 분석되는 공고부터 자동 적용. 기존 분석본 contractPeriod는 재분석 시 채워짐(원문에 PoP 있으면 값, 없으면 빈 문자열+키 유지).
- **후속(2차 결함 — DB→화면 경로)**: W90VN926RA065(Repair Victory Field, 시설보수)를 WF 수정 후 재분석하니 **DB summaryJson엔 contractPeriod='150 calendar days after NTP' 정상 저장**됐으나 **화면 여전히 빈칸**. 실측: WF/DB는 정상, 진범은 **BE [AnalysisResultDto.java](../backend/src/main/java/com/biddingagency/domain/opportunity/dto/AnalysisResultDto.java) `SummaryDto` record에 contractPeriod/siteVisit/contactInfo 필드 누락** → DB→API 변환에서 값 버려짐(FE는 렌더 준비 완료였음). CR-117에서 FE 4번 섹션 골격은 만들었으나 BE DTO 필드 추가를 빠뜨린 것. **수정**: SummaryDto에 3필드 추가(contractPeriod/siteVisit=string, contactInfo=List<Map>) + from() 매핑. 기존 분석본도 재분석 없이 즉시 노출(메타 변환이라). compileJava 통과 → `./deploy.sh be`(재기동 health UP 06:26) → 운영 API 실측 검증: contractPeriod/siteVisit/contactInfo(2건) 정상 노출 확인. 교훈: **WF→DB와 DB→화면은 별개 경로 — 한쪽 고쳐도 다른쪽 DTO 누락이 남을 수 있음.**
- **후속(3차 — 줄바꿈)**: 사용자 지적 — 낙찰기준 등 긴 텍스트가 `(1) xxx (2) xxx (3) xxx` 한 줄로 붙어 표시됨. 실측: **DB 원문 자체에 줄바꿈 없음**(LLM이 열거를 평문 한 줄로 출력). 결정=**FE 자동 줄바꿈**(기존본 즉시 적용, 재분석 불필요). **수정**: [NoticeDocumentView.tsx](../frontend/admin-console/src/components/NoticeDocumentView.tsx) `Paragraph`에 `whitespace-pre-line` + `splitEnumerations()` 헬퍼 — 앞에 공백 있는 `(숫자)`/`Phase One:~Four:`/한글라벨(`장소:/POC:/일시:/필수 여부:/연락처:`) 앞에 `\n` 삽입. 보수적 패턴(조항 내 `16.507-2(c)(2)`처럼 앞 공백 없는 괄호숫자는 미분할)이라 정상 문장 보존. 3개 실데이터 검증 통과. tsc 통과 → `./deploy.sh fe`(새 번들 index-CkML_2fl.js·whitespace-pre-line 서버 반영 확인).
- **후속(3-1차 — ` | ` 구분 열거 줄바꿈)**: 계약기간·현장설명회 분석값이 `Base Period: ... | 1st Option Year: ...`, `필수여부: ... | 신청방법: ... | 출처: ...` 형태로 저장되는 사례는 기존 `splitEnumerations()` 패턴에 해당하지 않아 여전히 한 줄로 표시됨. [NoticeDocumentView.tsx](../frontend/admin-console/src/components/NoticeDocumentView.tsx)의 분할 규칙에 **양쪽 공백이 있는 파이프(` | `)**를 줄바꿈으로 변환하는 처리를 추가. 코드·식별자 내부의 공백 없는 `|`는 건드리지 않으며, 기존 분석본도 재분석 없이 즉시 적용. **운영 FE 배포 완료(2026-06-19)** — 관리자 번들 `index-KW6Bb24w.js`, 서버 배치 파일과 로컬 빌드 SHA-256 일치, 서버 내부 nginx/BE health HTTP 200 확인.
- **후속(4차 — 최초 요구사항 10항목 순서 정렬)**: 사용자가 최초 요구사항(1.입찰번호 2.내용 3.계약기간 4.현장설명회 5.담당자 6.자격요건 7.낙찰기준 8.참고사항 9.특별유의사항 10.타임라인) 재확인. 기존 CR-117 골격은 순서·제목이 어긋나 있었음(개요/작업범위 분리, 담당자 9번, 낙찰 8번, 제출서류 7번 끼어듦, 참고사항 누락). **FE만 재배치**(데이터 소스 그대로): 2.내용=overview+scope 통합, 5.담당자(POC)↑, 7.낙찰기준, 8.참고사항=골격만(전용 데이터 없음, 향후 WF notes 추가 시), 9.특별유의사항=specialNotes(옛 10번에 흡수돼있던 것 분리), 10.타임라인, **11.제출서류=요구사항 밖 부가정보로 별도 섹션**(데이터 보존, 사용자 합의). tsc 통과 → `./deploy.sh fe`(번들 index-DRpGeUkj.js·"11.제출 서류" 서버 반영 확인). **미착수**: 3번 "1년씩 표기"(사용자 무시 지시), 6번 "국내 인허가 명칭 포함"(WF/LLM 수정 필요, 별도).
- **후속(5차 — `/` 구분자 + 테이블 셀 줄바꿈 + LLM 근본 지시)**: 사용자 지적 — 현장설명회(`일시: ... / 장소: ... / 비고: ...`)와 제출서류 테이블 **비고 컬럼**(`... / Attachment #4로 제공됨.`, `(1)...(2)...(3)`)이 여전히 한 줄. 실측 2건 진단: ① `splitEnumerations()`가 ` | `만 처리하고 ` / ` 미처리 + `비고:`/`형식:` 라벨 누락. ② **`DataTable`/`KvTable` 셀은 `splitEnumerations`도 `whitespace-pre-line`도 안 거침**(`Paragraph`만 변환) → 테이블 비고는 LLM이 `\n`을 넣어줘도 화면에서 뭉개짐. 사용자 결정(질문)=**LLM 지시 + FE 안전망 둘 다**. **변경**: (FE) [NoticeDocumentView.tsx](../frontend/admin-console/src/components/NoticeDocumentView.tsx) — `splitEnumerations` 구분자 `[|/]`(양쪽 공백 슬래시 추가, URL `a/b`·날짜 `7/1`은 공백 없어 보존)·`비고:`/`형식:` 라벨 추가 + `DataTable`/`KvTable` 셀에 `splitEnumerations`+`whitespace-pre-line` 적용. (LLM) 운영 `opportunity-analysis` WF `extract_facts` 프롬프트에 ★줄바꿈 규칙★ 추가 — string 필드에 여러 정보 담을 때 `/`·`|`·` - ` 대신 실제 `\n`으로 분리(예: siteVisit 일시/장소/비고). _meta v10. **운영 PUT 완료**(HTTP 200, 8필드 DTO `id/name/domain/triggerConfig/steps/errorHandling/outputSchema/inputSchema`, connection_id=cli-runner-bidding-001·6 STEP 무손실 검증). tsc 통과 → `./deploy.sh fe`(admin 번들 index-CUq9hB2r.js 서버 반영). FE 안전망=기존 DB 공고 즉시 적용, LLM 지시=신규 공고 근본 해결.

### CR-120: 테스트용 수동 공고 등록 (SAM 수집 없이 PWS 기반 제안서 시뮬레이션) (2026-06-21)

- **배경(사용자 요청)**: 손에 있는 과거 자료(PIEE에서 다운받은 SAM 공고 첨부 PWS + 실제 고객이 그때 제출했던 각종 서류)로 "공고문 분석 → 제안서 생성" 파이프라인을 끝까지 시뮬레이션하고 싶다. SAM API 메타(공고번호 등)는 없을 수 있어 PWS 기반 또는 수동 입력.
- **실측 진단(소스 직접 확인)**: 첨부 업로드([OpportunityAdminController.java:123](../backend/src/main/java/com/biddingagency/controller/admin/OpportunityAdminController.java#L123))·공고문 분석이 첨부를 입력으로 사용([NoticeService.java:284](../backend/src/main/java/com/biddingagency/domain/notice/service/NoticeService.java#L284), SAM rawJson 미사용)·고객서류 업로드([ClientDocumentController.java](../backend/src/main/java/com/biddingagency/controller/ClientDocumentController.java))·제안서 생성이 고객서류를 입력으로 사용([AIWorkflowService.java:426](../backend/src/main/java/com/biddingagency/domain/bid/service/AIWorkflowService.java#L426)) — **이미 다 존재**. 유일한 갭 = **SAM 수집 없이 Opportunity 1건을 만드는 수동 생성 경로가 없음**(Opportunity는 OpportunityCollectorService 단일 INSERT 경로).
- **결정(사용자 합의)**: ① 입력 형태=**관리자 화면 버튼+폼**(공고번호 자동 기본값 제공·제목 입력). ② 공고번호는 PWS 자동추출까지 안 가고 수동/자동기본값으로 충분(제안서 본문은 첨부 PWS에서 Aimbase가 읽음). ③ 절차=경량(본 CR 이력 + 바로 구현, T1/T3/spec 캐스케이드 생략 — 운영기능 아닌 테스트 전용).
- **변경 사항**:
  1. **BE 신규 API** `POST /admin/opportunities/manual` ([OpportunityAdminController.java](../backend/src/main/java/com/biddingagency/controller/admin/OpportunityAdminController.java)) — body `{noticeId?, title}`. noticeId 미입력 시 `TEST-{epochMillis}` 자동 생성. 중복 noticeId면 409.
  2. **BE 서비스** `OpportunityService.createManual(noticeId, title)` — 필수 4필드(noticeId/title/firstSeenAt/lastModifiedAt)+active=true 채워 INSERT. rawJson에 `{"source":"MANUAL_TEST"}` 표식(운영 수집분과 구분).
  3. **FE** admin-console 원본공고 리스트 상단 "테스트 공고 추가" 버튼+모달(공고번호 기본값·제목 입력) + services API 함수.
- **이후 흐름**: 생성된 Opportunity에 기존 화면으로 PWS 첨부 업로드 → 공고문 만들기/한글화 → (노출) → 고객 신청 → 고객서류 업로드 → 제안서 생성. **전부 기존 화면·API 재사용**.
- **규모**: 중(신규 API 1 + 신규 화면 1). 데이터모델/마이그레이션 무변경. 설계 캐스케이드는 사용자 합의로 경량 처리(본 CR 이력 갈음).
- **상태**: 구현 진행.

### CR-121: 고객 화면 — "제출 서류" 본문 중복 제거 (체크리스트 한 곳으로 통합) (2026-06-21)

- **배경(사용자 지적)**: 고객이 보는 입찰 상세 화면에서 같은 서류 목록이 **두 번** 노출됨 — ① 공고문 본문(`NoticeDocumentView`) 안의 "11. 제출 서류" 표 + ② 하단 별도 "필요 서류 체크리스트". 고객 입장에서 중복·혼란. "고객은 자기가 제출해야 할 서류만 정확히 알면 된다."
- **실측 진단(소스 직접 확인)**:
  - 고객 [BidDetailPage.tsx:175](../frontend/customer-portal/src/pages/BidDetailPage.tsx#L175)가 `NoticeDocumentView`로 본문 contentJson 렌더 — 그 안에 `heading`("11. 제출 서류") + `dataTable` 노드가 들어 있어 표로 표시됨.
  - 같은 페이지 [BidDetailPage.tsx:261-289](../frontend/customer-portal/src/pages/BidDetailPage.tsx#L261-L289)가 `requiredDocuments.documents[]`를 "필요 서류 체크리스트"(체크박스/필수배지/페이지제한 포함)로 별도 렌더.
  - 두 곳이 같은 서류를 다른 데이터 소스로 보여주는 중복. 체크리스트가 정보가 더 풍부(고객이 "내가 올릴 것" 보기 좋음).
  - 고객/관리자 `NoticeDocumentView`는 **별도 파일**(공유 컴포넌트 아님) — 고객 파일만 고치면 관리자 무영향.
- **결정(사용자 합의)**:
  - 고객 화면은 **하단 "필요 서류 체크리스트" 한 곳만** 유지. 본문 내 "11. 제출 서류" 표는 제거.
  - FACTOR 정밀추출(`factors[]`)·충족주체·SOW 출처 등 **근거는 관리자 화면에만**. 고객은 평면 `documents[]` 체크리스트만.
  - 관리자 콘솔은 현행 유지(11번 표 + FACTOR 정밀추출 + 근거).
- **변경 사항(고객 FE 1파일만)**:
  1. **FE** [NoticeDocumentView.tsx](../frontend/customer-portal/src/components/NoticeDocumentView.tsx) (customer-portal) — 렌더 단계에서 `stripSubmissionDocsSection()`으로 "제출 서류" heading(번호 prefix 변동 대응: "제출 서류" 포함 판정) + 그 직후 `dataTable`을 본문 노드 목록에서 제외. **저장된 contentJson 원본·관리자 화면 무영향**(렌더 시점 필터링).
- **규모**: 소(고객 FE 1파일, 본문 섹션 1개 렌더 제외). 데이터모델/API/마이그레이션 무변경. 설계 캐스케이드는 사용자 합의로 경량 처리(본 CR 이력 갈음).
- **상태**: 구현 완료. 빌드/배포 별도 승인 대기.

---

### CR-122: 공고분석 extract_facts 분리 — 단일 컨텍스트 1M 초과 대응(파일별 FOREACH) (2026-06-27)

- **배경(실측)**: `opportunity-analysis` STEP3 `extract_facts`가 단일 AGENT_CALL(Claude CLI 자율주행)로 첨부 전체를 순차 Read하는 구조. 대용량 공고에서 **단일 컨텍스트 1M 토큰 한계 초과** → 압축으로 앞부분 디테일 손실/타임아웃. 실측: opp `9E97ECE1...`(11첨부 73.3MB) 핵심 첨부만 추출 토큰 합산 ~1,147K(645p 전기사양 ~443K + 35MB 도면 ~605K + …) → 이미 1M 초과. 작은 공고(P-6120 ~8K)는 여유라 분리는 "정확도 개선"이 아니라 **대용량 처리 필수 구조**.
- **폐기된 오판(재발방지)**: ①"공고는 토큰 여유"=작은 공고만 보고 일반화한 오류 ②"extract_facts 네이티브도구 봉인"=틀림. Claude CLI **자율주행**이라 Read/Grep 열려있음, 프롬프트가 "전체 순차 Read"로 몰아넣었을 뿐.
- **책임 경계**: 분할 제어 메커니즘(CLI subagent 옵션)은 **Aimbase 플랫폼이 처리**(어댑터 옵션 따라 동작하도록 정리 완료). bidding-agency는 그 위에서 WF 정의 분리 + input 구성만.
- **결정(분리 축 A — 자료/파일축)**: 첨부+본문을 파일별로 FOREACH 분할, 각 에이전트가 **10항목 전원 공유 + 자기 파일 1개만 Read**. 파일 수만큼 1M씩 분산 → 안 터짐. 단일 파일 1M 초과는 운영에 없음(최대 33.8MB 도면도 ~600K). 항목축(B)·단계분리(C)는 기각/보류.
- **변경 사항**:
  1. **WF 정의 v3** [opportunity-analysis.steps.v3.json](../workflows/opportunity-analysis.steps.v3.json) — STEP3 단일 AGENT_CALL→**FOREACH(`items={{input.analysisTargets}}`, parallel, collect=append)**, body 에이전트는 `{{item.targetPath}}` 한 파일만 분석. STEP4 verify 프롬프트에 **①배열 입력 안내**(FOREACH append 출력=파일별 결과 List, ForeachStepExecutor 실측) **②항목(key)별 머지 지시**(중복 통합·근거 보존·충돌 시 권위원문 채택). STEP5 save 무변경(verify.structured_data만 참조). PUT용 WorkflowController 8필드만(CR-112 함정 회피).
  2. **BE** [NoticeService.java](../backend/src/main/java/com/biddingagency/domain/notice/service/NoticeService.java) — analyze input 에 `analysisTargets` 추가 + `buildAnalysisTargets()`(description.txt + attachments/<fileName>, 작업장 적재 경로와 일치). 기존 attachmentFiles/opportunityText 유지. compileJava 통과.
  3. **백업** [opportunity-analysis.backup.json](../backend/docs/aimbase-workflows/backup/opportunity-analysis.backup.json) — 운영 현행(updatedAt 06-20) 롤백용 보존.
- **규모**: 중(WF 구조 변경). 새 화면/API/테이블 없음, 데이터모델/마이그레이션 무변경. 설계서 [CR-122_extract_facts_분리_설계.md](../workflows/CR-122_extract_facts_분리_설계.md).
- **상태**: 설계·v3·BE코드 완료(compileJava 통과). **미착수**: ①Aimbase WF v3 PUT ②BE 배포 ③E2E 실측(대용량 공고로 1M 안 터지는지·verify 머지·save 단일구조 검증). 운영 PUT/배포는 사용자 승인 대기.

### CR-123: 공고분석 verify anchor 기반 grep 재설계 (2026-06-28)

- **선행 조치(완료)**: 11c2ce7 — verify 입력을 `{{extract_facts.output}}`(플랫폼이 200자 프리뷰로 축소) → `{{extract_facts.structured_data}}`(fact 본체)로 교체. context 238% 초과 해소. E2E(SMYU 26-1050, run 6102b7eb): 직전 동일 공고 failed였으나 verify 391s 정상 완료·run completed.
- **배경(실측, run 6102b7eb)**: verify_and_gapcheck(AGENT_CALL) **검증 방식이 비효율·불완전**. extract 원본 fact 1,840개 전부 `sourceFile`(1840/1840)·`page`(1840/1840) 채워졌으나 `sourceQuote`는 0/1840(extract 출력단축 원칙으로 의도적 비움). 그런데 verify는 이 좌표 메타를 안 쓰고 ①`ls`/`find` 맨땅 탐색 ②큰 파일 통째 Read ③12파일 일부만 열람(샘플링) ④1,840→12 과도축소. 원인=verify 프롬프트가 "원문 직접 열어 대조" 자율 지시만 하고 좌표·grep 미명시.
- **핵심 통찰**: extract는 fragment 원문을 보며 fact를 뽑으므로 그 순간 근거가 눈앞에 있음. 그때 fact에 **anchor(원문 영어 5~10단어)** 를 남기면 verify는 "그 anchor가 그 파일에 있나" 단순 기계 grep으로 축소. anchor가 영어라 영어 원문에 그대로 grep(한↔영 매칭 문제 소멸), extract가 본 걸 받아쓰는 거라 extra 비용 거의 없음. → 사용자가 제시한 "코드 grep 정석"이 비로소 성립(이전엔 grep 대상 sourceQuote가 비어 불가능).
- **결정(2-track)**: ①extract: 각 fact에 `anchor`(원문 영어 짧은 구절, 통째인용 금지) 추가. ②verify Track1=환각검증을 코드(기계) grep으로 — anchor를 sourceFile에 `grep -F`, 1,840개 전수, 샘플링·축소 없음, LLM 불필요. ③verify Track2=gap-check만 경량 LLM 유지(원문엔 있는데 빠진 항목 점검 + 최종 summary/requiredDocuments 통합).
- **grep 수행 위치(실측)**: 작업장(description.txt/attachments/)은 Aimbase 컨테이너 내부, BE는 작업장 파일 미접근(LLMPlatformClient는 실행/폴링만) → **anchor grep은 WF 안(Aimbase 도구)에서 수행**. BE 무변경.
- **규모**: 중(WF 정의 + extract/verify 프롬프트 재설계). 새 화면/API/테이블 없음, 데이터모델/마이그레이션 무변경, BE 무변경(예상). 설계서 [CR-123_verify_anchor_grep_재설계.md](../workflows/CR-123_verify_anchor_grep_재설계.md).
- **상태**: 선행 조치(11c2ce7) 완료·E2E 통과. 재설계 설계 문서 작성 완료. **미착수**: ①extract anchor 추가 ②verify grep 전환(STEP 구조는 Aimbase 도구 역량 실측 후 확정) ③WF v4 PUT ④E2E 검증. 구현은 사용자 승인 대기.

### CR-124: Customer Portal UI 전면 개편 — Design System 도입 (2026-06-29)

- **배경/목표**: customer-portal UI 전면 교체. 컨셉 정본 = [customer-portal-military-poc.html](../ui-mockup/customer-portal-military-poc.html)(낙점) — **상단 GNB형 + 나라장터식 차분 톤 + Noto Sans KR + 풀 밀리터리(네이비) 리브랜딩**. 메인(랜딩)은 **현 운영 HomePage 유지**(poc 메인 폐기, 독수리 네이비 히어로 유지). 구현은 **Design System(`@therecommerce-develop/ds-ui` v0.3.0) 컴포넌트로 재구축** — 소비앱은 DS만 import(메트로닉 직접 복사 금지).
- **DS 도입(실측)**: GitHub Packages(`.npmrc`+`GITHUB_TOKEN`), 설치 정본=`bp-backoffice2`. entry에 `dist/style.css` import + `@theme inline` CSS변수로 **WJbid 네이비 테마(wjbid.css)** 신규. 스택 호환 확인(React19/TW4.1/Radix/TanStack/lucide). 직결 컴포넌트: Header·**CategoryNav(horizontal=GNB)**·Footer·Breadcrumb / DataGrid·PageHeader·StatSummary·StatusFlow·Timeline·Pagination·Accordion(FAQ)·Calendar(입찰캘린더).
- **IA 확정(약 30화면, 기존12+신규~18)**: 공개=홈/소개/프로세스/가이드/요금/**공지(/notices)**/**FAQ(/faq)**/**고객센터(/support)**/로그인/가입. 로그인후 GNB=공고검색(검색·상세·**마감임박**·관심·**캘린더**·**저장검색**)/내제안서(목록·상세·**진행현황FSM**)/**서류함(/documents)**/알림. 계정드롭다운=프로필·**결제내역(/billing)**·로그아웃. **법적⚖️=/terms·/privacy·/refund·/disclaimer**(4개 개별)+푸터 사업자정보.
- **회원가입/회원 정책(확정)**: 가입 필수 = 이메일·비번 + **회사명·사업자등록번호·담당자명·연락처**(핵심4). 가입 후 프로필 = 주소·**UEI·CAGE**. BE Member 실측: company_name(필수)·business_registration_number/contact_person/phone(nullable) **이미 존재** → 가입 검증 레벨에서 필수화. **데이터모델 변경**: member에 `uei`(VARCHAR20)·`cage_code`(VARCHAR10) 컬럼 신규(Flyway). 가입 동의=이용약관(필수)·개인정보(필수)·마케팅(선택).
- **법적 페이지**: 4개 개별. 면책 핵심문구="낙찰 보장 안 함 / 입찰서 최종 제출·서명은 고객 책임". 환불=건당 과금 단계별 기준. 푸터 사업자정보(전상법 표기). ⚠️ 문구 원문은 운영/법무 확정 필요(설계는 골격+플레이스홀더).
- **규모**: **대규모**(전 화면 + DS 도입 + member 데이터모델 변경 + 빌드/설치 설정). 설계서 [CR-124_UI전면개편_DS도입_설계.md](CR-124_UI전면개편_DS도입_설계.md).
- **상태**: **설계** 작성 완료(본 문서+마스터 설계서). T3-3 화면구조·T3-1 데이터모델(member uei/cage) 갱신 예정. **미착수**: Phase1 DS셋업 → Phase2 공개화면 → Phase3 로그인후 → Phase4 법적 → Phase5 검증. 구현은 설계 커밋 + 사용자 승인 후 착수.

### CR-125: 회사 공통 서류함 — Member 종속 보관함 + 제안서 슬롯 자동채움 (2026-06-30)

- **배경(사용자)**: 사업자등록증·CAGE 증빙 등 **회사 공통 서류는 매 공고마다 다시 올리기 귀찮다.** 한 번 넣어두고 재사용하고 싶다.
- **현황 실측**:
  - 현 `/documents` [DocumentsPage.tsx](../frontend/customer-portal/src/pages/DocumentsPage.tsx) = `DUMMY_SLOTS` 하드코딩 + **BE 미연동 껍데기**(버튼 다 죽음, "시연용 더미" 자인).
  - `ClientDocument` = **공고/입찰 종속**(`@ManyToOne BidRequest nullable=false`). "이 공고에 제출할 서류"이지 회사 공통 보관함 아님. **공통 서류함(member 종속)은 BE에 없음** → 신규 엔티티 필요.
  - 슬롯 매칭은 CR-010 `ComplianceService.uploadToSlot` / `RequirementFulfillmentMap`(`client_document_id` FK 보유)에 존재 → 2단계가 이 경로 재사용.
- **결정(확정 정책)**:
  - **범위 = 1+2단계 둘 다.** 1단계=`MemberDocument`(member 종속 보관함) CRUD + `/documents` 실연동. 2단계=제안서 슬롯에 서류함 같은 카테고리 자동채움.
  - **카테고리 = 고정 + 자유 병행.** 고정 6종(사업자등록증·CAGE·회사소개서·재무제표·SAM등록증·과거실적, 자동매칭 대상) + `OTHER`(기타, 자동매칭 제외).
  - **고정 카드** = 제목 고정, 파일만 업로드/교체/삭제(모달 없음). **기타** = 등록 모달(문서제목 필수 / 파일 필수 / 설명 선택), 다건 누적.
- **데이터 모델**: 신규 테이블 `member_documents`(V39) — member_id FK / category / file_name / file_size / content_type / storage_url / display_name / description. 고정=member당 1건(교체), OTHER=다건. **기존 모델(ClientDocument·RequirementFulfillmentMap·member) 무변경.**
- **API(신규 5)**: GET/POST/PUT/DELETE `/member-documents` + POST `/bid-requests/{id}/required-document-slots/auto-fill`.
- **자동채움 동작**: 미충족 슬롯의 category ↔ 서류함 고정 category 매칭 → MemberDocument를 ClientDocument로 **복제**(원본 보존) + `fulfillWithClientDocument`. 이미 충족 슬롯 미덮어씀(BIZ-125-4).
- **FSM/이벤트**: 무변경. 서류함은 상태기계 밖, 자동채움은 기존 슬롯 충족 경로(BIZ-015) 그대로.
- **규모**: 중(신규 테이블1·API5·화면2). 설계서 [CR-125_회사공통서류함_설계.md](CR-125_회사공통서류함_설계.md). T3-1/T3-2/T3-3 갱신.
- **상태**: 설계 완료 → 구현 진행. FlowGuard 등록은 사용자 결정 보류.

### CR-126: SAM 수집 실패 알림 — 키 만료 무경보 해소 (2026-08-13)

- **배경(사건)**: SAM.gov API 키가 **2026-08-03 만료**되어 수집이 **9일간 401 전패**(하루 4콜 × 9일 = 36콜)했으나 **아무 알림도 없어 방치**됨. `sam_api_call_log` 실측: 8/2까지 200 정상 → 8/3 부분실패 → 8/4~8/12 전부 `401 {"code":"900901","message":"Invalid Credentials"}`.
- **무경보 원인 3중(실측)**:
  1. **실패 전용 알림 경로 부재** — 이벤트가 `OpportunitiesCollected`(성공) 하나뿐 ([OpportunityCollectionScheduler.java:77](../backend/src/main/java/com/biddingagency/integration/samgov/scheduler/OpportunityCollectionScheduler.java#L77)).
  2. **0건 스킵이 실패를 삼킴** — CR-015가 넣은 `targetNewCount == 0` 스킵 ([NotificationEventListener.java:62](../backend/src/main/java/com/biddingagency/domain/notification/service/NotificationEventListener.java#L62))이 **"401로 0건"과 "원래 신규 없음"을 구분 못 함**.
  3. **에러가 로그에만 남음** — `log.error` 후 종료([:80-82](../backend/src/main/java/com/biddingagency/integration/samgov/scheduler/OpportunityCollectionScheduler.java#L80-L82)). 아무도 로그를 보지 않음.
  - 추가: 401은 [OpportunityCollectorService.java:145-148](../backend/src/main/java/com/biddingagency/integration/samgov/client/OpportunityCollectorService.java#L145-L148) 광범위 catch에서 `errors++`로만 집계되어 **HTTP status 유실** → errors 카운트만으로 키 만료 특정 불가.
- **결정(설계)**: 실패를 **독립 이벤트 `CollectionFailed`**로 분리 발행(0건 스킵 분기를 타지 않음) + `NotificationType.COLLECTION_FAILED` 신설. status 유실은 **CR-036 자산 재사용**으로 우회 — `sam_api_call_log.last_status`(당일 UTC search 행)를 조회해 401/403이면 `authFailure=true`. 인증 실패 시 메일 문구를 **"키 만료 추정 + 재발급 절차"로 특정**한다(조치 비용 5분). 멱등키 `COLLECTION_FAIL_{keyword}_{yyyy-MM-dd}`로 **하루 1통**(BIZ-012 재사용) — 9일 지속 시 36통이 아니라 9통.
- **영향 범위**: BE only — `domain/event/CollectionFailedEvent`(신규), `NotificationType`, `NotificationEventListener.onCollectionFailed`(신규), `OpportunityCollectionScheduler`(실패 발행), `SamApiCallLogRepository`(당일 status 조회). **마이그레이션·FE·MCP·FSM 무변경.**
- **함께 처리된 운영 조치(본 CR 범위 밖, 완료)**: 키 교체(`SAM-2f62eafc…`, 89일) + **시크릿 `.env` 분리**(커밋 `ec42e63`) — compose 평문 → 서버 `.env` 주입(`${VAR:?}`, 미설정 시 기동 실패). 교체 절차가 ".env 한 줄 + 재기동"으로 단순화됨.
- **백필**: **불필요**(실측). 스케줄러가 매회 최근 30일(`daysBack=30`)을 훑는 구조라 키 교체 직후 수집에서 공백기 **29건 소급 적재 완료**(`first_seen` 전부 2026-08-12 23:00:01). 단 **공백이 30일을 넘으면 영구 손실** — 본 CR의 알림이 그 방어선.
- **규모**: 중(신규 이벤트1·알림타입1·리스너1, 마이그레이션 없음). 설계서 [CR-126_수집실패_알림_설계.md](CR-126_수집실패_알림_설계.md). T1-1(BID-OPP-009 신설)/T1-6(CollectionFailed 계약)/T3-5(TC-NOTI-004~008) 갱신.
- **상태**: 설계 완료(2026-08-13) → 구현 대기.

### CR-127: 홈 개편 AIDA 첫 화면과 공통 틀·색 이름표 (2026-10-10)

- **배경(사용자)**: IR-001 토론과 그 뒤 시안 작업에서 사용자가 고른 AIDA 홈 시안(신한은행식 정돈, 색만 파랑)으로 고객 포털 홈을 바꾼다. 10/10 16:13 "저 목업똑같이 해야해요", 16:16 본 구현 승인(목업과 1440x900 캡처를 화면 정본으로, 같은 크기 캡처 대조를 완료 조건으로, 색을 공통 값 한 곳으로 모으기), 16:34 두 창 분할.
- **범위**: customer-portal 의 홈 첫 화면과 아래 구역, 모든 화면에 걸리는 머리말·메뉴 띠·전체메뉴·꼬리말, 색 이름표 정의(src/index.css, src/wjbid-theme.css, tailwind.config.js, components/layout/, pages/HomePage.tsx). 홈 밖 화면 색 전환은 CR-128.
- **규모**: 중(화면 구조 변경, API·테이블 없음). 설계서 [CR-127_홈_개편_AIDA_첫_화면과_공통_틀_색_이름표.md](CR-127_홈_개편_AIDA_첫_화면과_공통_틀_색_이름표.md).
- **상태**: 운영 배포 완료(2026-10-10, 커밋 98c9696). 운영 홈 실측 머리말 192, 두 칸 768, 다섯 칸 띠 880. 정본 캡처와 같은 크기 대조 통과(머리말 192, 두 칸 768, 회원 765, 다섯 칸 띠 880, 회원 877).

### CR-128: 고객 화면 색 이름표 전환 (2026-10-10)

- **배경(사용자)**: 색을 아직 정하지 않았고 나중에 설정으로 바꾸려 한다. 색을 한 곳에서 바꾸면 모든 고객 화면이 함께 바뀌어야 한다. 2026-10-10 16:34 결정으로 CR-127(홈과 공통 틀, 색 이름표 정의)과 나눴다.
- **범위**: customer-portal 의 src/pages/ 중 HomePage.tsx 를 제외한 화면, src/components/ui/. 직접 기록된 16진 색, Tailwind 팔레트 색을 CR-127 이 정의하는 색 이름표로 바꾼다.
- **실태(2026-10-10)**: 대상 파일에 16진 56건, 팔레트 525건, 흰검 58건, rgb 12건, 이름표 사용 60건.
- **규모**: 소(새 화면·API·테이블 없음, 화면 구조 변경 없음). 설계서 [CR-128_고객_화면_색_이름표_전환.md](CR-128_고객_화면_색_이름표_전환.md).
- **상태**: 운영 배포(2026-10-10 21:33, CR-127 고객 화면 배포에 함께 실림). 전환 d02fafd, e4fab7f. 운영 공개 화면 확인 완료, 로그인 뒤 화면은 로컬 확인까지.

### CR-129: 회원 요금 월 5만원 반영 (2026-10-10)

- **배경(사용자)**: 10/10 15:31 원문 "회원가입해서 5만원정도..월.. 공고문 적격성 서비스도 해주고, 공고문 상세번역도 해주고. 무료 가입자는 SAM번역수준만."(docs/origins/원본_사이트목적과서비스_20261010.md 4번). 17:13 "5만원은 그냥 제가 툭던진말이고". 17:40 월 구독은 무료·회원 두 단계, 등록 대행·제안서 자문은 건별 견적으로 따로, 회원 금액은 화면에 고정하지 않고 설정값으로 두고 발주사와 정한 뒤 넣는다. 18:50 결제 내역·약관·환불 문구도 이 CR 에서 고치고, 관리자 금액 입력 화면은 만들지 않는다.
- **대상 기능 ID**: BID-PRICE-001
- **변경 타입**: 변경
- **영향 범위**: BID-PRICE-001, POL-010
- **영향 모듈**: M. 서비스 요금 (백엔드 요금 조회, 고객 포털 요금 안내·결제 내역·이용약관·환불 정책·이용 안내·공지사항)
- **영향 설계서**: T1-1, T1-4, T3-1(system_settings 신설), T3-2(GET /pricing 응답), T3-3(요금 안내, 결제 내역), T3-5(TC-PRICE-001~004). 확인하고 영향 없음: T1-3, T1-5, T1-6, T1-7, T2-1, T3-4, execution-spec
- **규모**: 중(설정 테이블 신설, 요금 조회 API 응답 변경, 화면 구조 변경). 설계서 [CR-129_회원_요금_월_5만원_반영.md](CR-129_회원_요금_월_5만원_반영.md).
- **상태**: 완료(10/10 22:00 완료 인정). 백엔드 21:31, 고객 화면 CR-127 과 함께 21:33 운영 배포.

### CR-130: 서버 오류가 로그인으로 튕기는 결함 (2026-10-10)

- **배경**: CR-129 로컬 확인 중 발견(10/10 19:28). 고객 화면에서 서버 오류가 나면 오류 안내 대신 로그인 화면으로 간다. 사용자 10/10 21:29 전승으로 별도 CR 로 고친다.
- **변경 타입**: 결함 수정
- **영향 모듈**: 백엔드 보안 설정(오류 재요청 허용), 백엔드 DB 오류 문구 감춤(사용자 10/10 22:00 승인), 고객 포털 공통 오류 처리(401 재시도 한 번)
- **영향 설계서**: 없음(기존 설계 범위 안의 결함)
- **규모**: 소(새 API·화면·테이블 없음). 문서 [CR-130_서버_오류가_로그인으로_튕기는_결함.md](CR-130_서버_오류가_로그인으로_튕기는_결함.md).
- **상태**: 완료(10/10 22:12 사용자 승인). 운영 배포 10/10 22:06(all).

### CR-131: 로그인·회원가입 화면 홈 컨셉 개편 (2026-10-10)

- **배경(사용자)**: 10/10 22:17 "로그인화면은 구버전이네요 여기도 트렌드 컨셉에 맞게 수정필요", 22:20 /전승 "로그인과 회원가입을 함께(같은 입구라 따로 가면 둘이 달라 보임)".
- **변경 타입**: 화면 개편
- **영향 모듈**: 고객 포털 로그인·회원가입 화면(pages/LoginPage.tsx, pages/RegisterPage.tsx), 라우트(App.tsx). 공통 틀(components/layout/)은 그대로 쓴다. 백엔드 회원가입의 이미 가입된 이메일 응답(409, EMAIL_ALREADY_EXISTS).
- **결정(사용자 10/10 22:41 /전승)**: 컨셉+목업으로 정한다. 비밀번호 찾기는 고객센터 이메일 안내, 메일 재설정 기능은 백로그 [BL-001](CR/BACKLOG.md). 이미 가입된 이메일은 서버 오류 코드와 한글 안내.
- **영향 설계서**: T1-1 BID-AUTH-001·002·BROWSE-006, T3-2 /auth/register, T3-3 「공통 틀」 적용 범위·「로그인」·「회원가입」, T3-4 「로그인·회원가입」, T3-5 TC-AUTH-002
- **규모**: 중(화면 구조 변경, 기존 API 의 오류 응답 하나 추가, 테이블 없음). 문서 [CR-131_로그인_회원가입_화면_홈_컨셉_개편.md](CR-131_로그인_회원가입_화면_홈_컨셉_개편.md).
- **상태**: 완료(운영 배포 10/10 23:13, 사용자 23:12 /전승. 완료 인정 23:17 /전승)

### CR-132: 고객센터 1:1 문의 실제 접수 (2026-10-11)

- **배경(사용자)**: 10/11 00:51 /전승 "고객센터 1:1 문의가 '접수되었습니다'만 띄우고 실제로는 아무 데도 보내지 않음(운영에서 고객 문의가 사라지는 중) ①새 창에서 바로 고침: 서버에 저장하고 관리자에게 알림(권장)".
- **대상 기능 ID**: BID-SUPPORT-001, BID-SUPPORT-002, BID-SUPPORT-003 (신규 모듈 Q. 고객센터)
- **변경 타입**: 신규 (결함 수정 겸)
- **결정(사용자 10/11 06:01, 06:06 /전승)**: 컨셉+목업, 목업 확정. 비로그인 문의 허용(로그인이면 회원과 묶음). 관리자가 콘솔에서 쓴 답변을 시스템이 고객 메일로 보냄. bp-notification 에 문의용 메일 틀 새로 등록(등록할 때 따로 승인). 관리자 콘솔에 디자인시스템 부품 도입.
- **영향 모듈**: 백엔드 문의 도메인·API·알림, 고객 포털 고객센터 화면, 관리자 콘솔 1:1 문의 목록·상세와 메뉴
- **영향 설계서**: T1-1, T1-2, T1-3(BIZ-023·024), T1-4(POL-013·014), T1-5, T1-6(InquiryReceived), T2-1, T3-1, T3-2, T3-3, T3-4, T3-5(TC-INQ-001~014), execution-spec. 확인하고 영향 없음: T1-7
- **계약**: CR-133 상담형 화면이 `/support?type=<유형코드>&title=<제목>` 로 문의를 연다(CR 문서 6장)
- **규모**: 중(테이블·API·관리자 화면 신설). 문서 [CR-132_고객센터_1_1_문의_실제_접수.md](CR-132_고객센터_1_1_문의_실제_접수.md).
- **상태**: 완료(2026-10-11 09:26 사용자 승인). 백엔드 07:15, 화면 07:16(CR-133 과 함께) 운영 배포, 운영 실제 접수와 답변 시험 성공. 문의 메일 값 이스케이프(TC-INQ-013·014). 찾은 결함 두 건(운영 시각 9시간, 관리자 주소 바로 열기)은 새 창으로 넘김(CR 문서 7.7장)

### CR-133: 메뉴 속 화면 계획과 첫 묶음 구현 (2026-10-11)

- **배경(사용자)**: 10/11 00:46 "메뉴들 속페이지까지 다한건가요?", 00:48 "서류함 등록, 컨설팀 입찰교실 고객센터 수주분석 맞출판정 등등", 00:51 /전승 "묶음마다 화면에 담을 것과 필요한 기능·데이터를 한 장으로 정리해 보여드리고 순서를 정해 창을 나눠 만든다".
- **대상 기능 ID**: BID-PAGE-001·002, BID-SVC-001, BID-EDU-001~004 (신규 모듈 R. 메뉴 속 화면), BID-SUPPORT-004·005, BID-PRICE-001(틀만)
- **변경 타입**: 신규 (화면)
- **결정**: 순서 1번, 첫 묶음은 등록·컨설팅, 입찰 교실, 고객센터 속페이지 새 모양, 나머지는 항목별 안내(사용자 10/11 06:01 /전승). 컨셉+목업, 견본 공지 6건 삭제, 제안서 자문은 법무 확인 전 항목별 안내, 안내 글은 공식 안내 근거로 쓰고 발주사 확인은 배포 뒤(06:06 /전승). 목업 확정(06:19 /전승).
- **영향 모듈**: 고객 포털만. 속페이지 틀 부품(공통 틀 폴더 밖), 서비스 안내·입찰 교실·항목별 준비 안내·공지사항·자주 묻는 질문 화면, 요금 안내와 1:1 문의의 틀, 메뉴 정의(항목 키와 목적지), 홈 도움 구역 목록과 "준비 중" 링크, 회원가입 안내의 등록 대행 링크. 서버 변경 없음
- **영향 설계서**: T1-1, T1-2, T3-3, T3-4, execution-spec. 확인하고 영향 없음: T1-3~T1-7(규칙·정책·상태·이벤트 없음), T2, T3-1·T3-2(테이블·API 없음), T3-5(서버 서비스 없음)
- **계약**: 상담·참가 신청은 CR-132 의 `/support?type=<유형코드>&title=<제목>` 으로 연다
- **규모**: 중(새 화면 12개와 속페이지 틀, 테이블·API 없음). 문서 [CR-133_메뉴_속_화면_계획과_첫_묶음_구현.md](CR-133_메뉴_속_화면_계획과_첫_묶음_구현.md), 정리표 [기록/CR-133_메뉴_속_화면_정리표.md](../기록/CR-133_메뉴_속_화면_정리표.md).
- **상태**: 완료(배포 10/11 07:16, 완료 인정 사용자 07:25 /전승, 구현 1aabc77·7eb3669)

### CR-134: 홈 첫 화면 시선 분산 정리 (2026-10-11)

- **배경(사용자)**: 10/11 06:59 "딱 메인에 들어오면 글자가 너무 많아 보이는 그런게 있느거 같기도하고 뭐가 부족할가요?", 07:00 "아 2군데로 분산된다는거군요". 공고 0건 홈에서 같은 크기 큰 제목이 좌우 두 곳, 누를 것이 여섯 개, 관심 공고·요금 안내·1:1 문의가 머리·메뉴 줄·떠 있는 줄에 겹쳐 눈이 멈출 곳이 없었다.
- **대상 기능 ID**: BID-BROWSE-005
- **변경 타입**: 화면 수정
- **결정(사용자 10/11 07:00 "예")**: 대표 문장을 왼쪽 큰 자리 한 곳으로 옮기고 "준비 중"은 머리 한 줄로 내린다. 왼쪽 단추 둘(새 공고 알림 받기, 공고문 예시 전체 보기)은 예시 카드와 무료로 시작이 같은 곳으로 가므로 뺀다. 오른쪽은 제목을 빼고 사진, 무료 회원 혜택, 가입·로그인만 둔다(07:24 "빼니까 어색하잖아요 떵그러니 흰색으로 남고" 로 사진은 되살리고 남는 높이를 채우게 함). 오른쪽 바깥 떠 있는 빠른 메뉴는 없앤다. 공고 있음 상태의 첫걸음 상자는 그대로 둔다.
- **영향 모듈**: 고객 포털 홈(pages/HomePage.tsx, wjbid-aida.css). 서버 변경 없음
- **영향 설계서**: T1-1 BID-BROWSE-005, T3-3 「홈」 첫 화면. 확인하고 영향 없음: T3-4(첫걸음 상자 금액 조회는 공고 있음 상태에 남음), T1-3~T1-7, T2, T3-1·T3-2·T3-5
- **규모**: 소(한 화면 안 배치 조정, 새 화면·API·테이블 없음)
- **상태**: 진행중(로컬 확인 완료, 배포 대기)

### CR-135: 메뉴 속 화면 2묶음 있는 데이터로 여는 6곳 (2026-10-11)

- **배경(사용자)**: 10/11 00:48 "서류함 등록, 컨설팀 입찰교실 고객센터 수주분석 맞출판정 등등", 06:01 /전승 첫 묶음 뒤 있는 데이터 6곳, 수집 보강 3곳, 큰 기능 9곳 순, 07:25 /전승 남은 묶음 창을 나눈다.
- **대상**: 정리표 순번 2 사전 공고, 3 변경 공고(목록만), 5 공고문 예시, 9 최근 낙찰, 13 월별 동향, 14 업종별 발주
- **변경 타입**: 신규 (화면, 조회 API, 설정 키)
- **영향 설계서**: T1-1(BID-MKT-001~006), T1-2, T1-3(BIZ-025~027), T1-4(POL-015), T3-1(설정 행 V43), T3-2(C-2), T3-3, T3-4, T3-5(TC-MKT-001~018), execution-spec. 확인하고 영향 없음: T1-5, T1-6, T1-7, T2
- **계약**: 화면 주소 /bids/*, /awards/recent, /analysis/* (CR-136 과 앞부분 맞춤). 낙찰 공지 업체 이름·금액 숫자 칸은 CR-136 이 더한 뒤 이 목록이 읽는다
- **규모**: 중(고객 화면 6개, 조회 API 6개, 설정 키 1개, 새 표 없음). 문서 [CR-135_메뉴_속_화면_2묶음_있는_데이터로_여는_6곳.md](CR-135_메뉴_속_화면_2묶음_있는_데이터로_여는_6곳.md)
- **상태**: 진행중(운영 반영 10/11 10:36 8422e70, 시험 TC-MKT-001~018 19건 통과 10/11, 완료 승인 대기. 운영 시간대 9시간 문제는 CR-138)

### CR-136: 메뉴 속 화면 3묶음 칸과 수집 보강 3곳 (2026-10-11)

- **배경(사용자)**: 10/11 00:48 "서류함 등록, 컨설팀 입찰교실 고객센터 수주분석 맞출판정 등등", 06:01 /전승 첫 묶음 뒤 있는 데이터 6곳, 수집 보강 3곳, 큰 기능 9곳 순, 07:25 /전승 남은 묶음 창을 나눈다.
- **대상**: 정리표 순번 20 유효기간 알림, 10 낙찰 업체, 12 기지별 지도
- **대상 기능 ID**: BID-MKT-007~014
- **변경 타입**: 신규 (칸, 표, 조회 API, 화면, 수집 보강, 메일 알림)
- **영향 모듈**: B. 공고 수집(낙찰 칸 저장, 지난 낙찰 다시 받기), H. 알림(만료 메일), P. 메뉴 속 화면, M. 관리자(기지 관리), 공통 서류함(만료일)
- **영향 설계서**: T1-1(BID-MKT-007~014), T1-2, T1-3(BIZ-028~031), T1-4(POL-016), T1-6(DocumentExpiryApproaching), T3-1(V44 낙찰 칸 3개와 military_bases, V45 member_documents.expires_on 과 설정 행), T3-2(C-3, B-4, N), T3-3, T3-4, T3-5(TC-MKT-019~050), execution-spec. 확인하고 영향 없음: T1-5(상태 전이 없음), T1-7, T2-1(지도는 화면에 싣는 그림, 날짜 고르기 칸은 ds-ui, 새 라이브러리 없음)
- **계약**: 낙찰 공지(opportunities)에 awardee_name, awardee_uei, award_amount_usd 를 더한다. 기존 award_amount 표시 문자열은 그대로(CR-135 목록이 읽음). CR-135 최근 낙찰 응답의 awardeeKnownCount, awardeeName 을 이 CR 이 채운다(CR-135 [계약] 10/11). 만료 메일 틀 BID_DOC_EXPIRY 는 알림 서버(bp-notification)에 새로 만들어야 한다(컨타 경유 요청)
- **규모**: 중(서류 만료일 칸, 낙찰 업체 칸, 기지 목록 표, 조회 API, 고객 화면 3개와 관리자 화면 1개). 문서 [CR-136_메뉴_속_화면_3묶음_칸과_수집_보강_3곳.md](CR-136_메뉴_속_화면_3묶음_칸과_수집_보강_3곳.md).
- **상태**: 진행중(화면 컨셉 승인요청)

### CR-137: 메뉴 속 화면 4묶음 큰 기능 9곳 계획과 첫 항목 (2026-10-11)

- **배경(사용자)**: 10/11 00:48 "서류함 등록, 컨설팀 입찰교실 고객센터 수주분석 맞출판정 등등", 06:01 /전승 첫 묶음 뒤 있는 데이터 6곳, 수집 보강 3곳, 큰 기능 9곳 순, 07:25 /전승 남은 묶음 창을 나눈다.
- **대상**: 정리표 순번 16 적격성 진단, 17 공고별 판정, 21 제출 전 검사, 6 다음 달 공고, 7 재발주 달력, 8 옵션 연장 알림, 11 낙찰가 범위, 3 변경 공고 비교, 38 카카오톡 알림. 이 CR 은 9곳 계획과 첫 항목 구현
- **대상 기능 ID**: BID-QUAL-001 (첫 항목 적격성 진단. 기존 번호를 고쳐 쓴다)
- **변경 타입**: 계획 + 변경(첫 항목. 접수 응답만 주던 자격 진단 요청을 지우고 회원 답 저장으로 바꿈)
- **영향 모듈**: N. 자격 진단, C. 공고 열람(공고 상세 단추), P. 메뉴 속 화면(맞춤 판정 메뉴 목적지)
- **영향 설계서**: T1-1(BID-QUAL-001), T1-2, T1-3(BIZ-032~033), T1-7, T3-1(V42 eligibility_answers), T3-2(E), T3-3, T3-4, T3-5(TC-QUAL-001~015), execution-spec. 확인하고 영향 없음: T1-4(정책값 없음), T1-5(상태 전이 없음), T1-6(이벤트 없음), T2-1(새 라이브러리 없음)
- **규모**: 계획은 문서만. 첫 항목은 중(새 표 1개, API 3개, 고객 화면 2개). 문서 [CR-137_메뉴_속_화면_4묶음_큰_기능_9곳_계획과_첫_항목.md](CR-137_메뉴_속_화면_4묶음_큰_기능_9곳_계획과_첫_항목.md), 계획 [기록/CR-137_큰_기능_9곳_계획.md](../기록/CR-137_큰_기능_9곳_계획.md)
- **상태**: 진행중(순서 승인 10/11 09:26, 컨셉 승인 09:45, 목업 확정 09:54, 구현과 로컬 확인 끝, 운영 배포 10:28, 완료 인정 대기)

### CR-138: 운영 시각 9시간 어긋남과 관리자 주소 바로 열기 (2026-10-11)

- **배경(사용자)**: 10/11 09:26 /전승 "운영 서버 시간대가 없어 모든 화면·메일 시각이 9시간 이르게 보임 ①새 창에서 운영 서버 시간대를 서울로 맞춤, 정해진 시각에 도는 작업이 함께 옮겨지는지도 확인(권장)", "관리자 콘솔 아래 주소로 바로 들어가거나 새로고침하면 고객 사이트가 뜸 ①새 창에서 운영 웹서버 설정을 고침(권장)". 컨타 11:30 배정 공고 마감 9시간 늦게 저장(CR-137 창 10:16 발견)
- **원인**: 운영 백엔드 컨테이너만 UTC(compose 에 TZ 없음). 지금 시각으로 쓴 칸은 바르게 저장되고 읽을 때 9시간 이르다. 수집이 만든 게시일과 마감은 9시간 늦게 저장돼 공고 목록에서는 맞물려 맞게 보인다. 공고 상세 응답은 마감을 날짜만 줘 화면이 늘 오전 09:00. 서버 nginx 의 관리자 경로가 없는 주소를 고객 포털 첫 파일로 전달한다
- **대상**: docker-compose.prod.yml 시간대, 수집 날짜 읽기, SAM 호출 기록 시각, 운영 공고 게시일과 마감 보정(386건 예상), 공고 상세 응답 마감 시각 칸, 서버 nginx 한 줄
- **변경 타입**: 결함 수정, 운영 데이터 보정, 서버 설정
- **영향 설계서**: T3-2(공고문 상세 responseDeadlineAt), T1-3(BIZ-004 원본 보존 대상). 확인하고 영향 없음: T1-1, T1-4, T1-5, T1-6, T1-7, T2-1, T3-1, T3-3, T3-4, T3-5
- **규모**: 소(설정, 변환 함수, 응답 칸 하나, 새 화면·API·표 없음). 문서 [CR-138_운영_시각_9시간_어긋남과_관리자_주소_바로_열기.md](CR-138_운영_시각_9시간_어긋남과_관리자_주소_바로_열기.md)
- **상태**: 완료(방향 승인 10/11 11:40, 백엔드와 화면 운영 반영 10/11 12:17, 새 운영 서버 nginx 반영 10/12 00:18)

### CR-139: 수집 때 공고 한글 제목이 빠지는 결함 (2026-10-11)

- **배경(사용자)**: 10/11 09:26 /전승 "한글 제목이 없는 공고(394건 중 344건) ①한글 제목이 없으면 원문 제목을 보이고, 수집 때 한글 제목이 빠지는 결함은 따로 고침(고치면 화면은 저절로 한글)(권장)"
- **원인**: 수집 때 제목 번역 실행이 저장 단위 없이 번역 결과를 공고에 넣기만 해 버려진다. 저장되는 경로는 관리자 단추뿐(6/2 41fdcc2 뒤 수집이 부르지 않음). Aimbase 는 7월 뒤로도 번역을 완료해 돌려주었다
- **대상**: OpportunityTranslationService 제목 번역 저장. 이미 쌓인 344건(낙찰 공지 94) 채우기
- **변경 타입**: 결함 수정, 운영 데이터 채우기
- **영향 설계서**: 없음(CR-022 설계대로 저장되게 고침). 확인하고 영향 없음: T1-1(한글 제목 없으면 원문, 그대로), T3-2(titleKo 응답 그대로), T3-4(원문 대체 표시 그대로)
- **규모**: 소(메서드 하나, 새 API·화면·표 없음). 문서 [CR-139_수집_때_공고_한글_제목이_빠지는_결함.md](CR-139_수집_때_공고_한글_제목이_빠지는_결함.md)
- **상태**: 완료(운영 배포 10/11 11:51 801c471, 채우기 344행 12:15, 운영 한글 제목 394건 중 394건, 시험 TC-OPP-006~011, 완료 인정 15:49)

### CR-140: 메뉴 속 화면 4묶음 공고별 판정 (2026-10-11)

- **배경(사용자)**: 10/11 00:48 "서류함 등록, 컨설팀 입찰교실 고객센터 수주분석 맞출판정 등등", 09:26 /전승 CR-137 계획 4절 순서, 10:30 적격성 진단 완료 뒤 컨타가 창을 나눔.
- **대상**: 정리표 순번 17 공고별 판정. 공고 목록과 상세에 회사 기준 판정(참여 가능, 서류 부족, 어려움, 판정 전)과 걸린 자격 근거. CR-137 적격성 진단 서비스 시험(TC-QUAL-001~015) 코드도 함께 씀(컨타 11:29)
- **대상 기능 ID**: BID-MKT-015 공고별 판정, BID-MKT-016 공고 검색 판정 칸, BID-MKT-017 공고 상세 판정 상자, BID-MKT-018 증빙 서류 붙이기, BID-MKT-019 판정 계산과 근거. 고친 것 BID-QUAL-001
- **변경 타입**: 신규 + 변경(BIZ-032 를 "자격 유무는 회원이 답하고 판정은 BIZ-040 규칙으로" 로 고침)
- **영향 모듈**: N. 자격 진단, C. 공고 열람(공고 검색 칸, 공고 상세 상자), 서류함(CR-125, 붙인 서류)
- **영향 설계서**: T1-1, T1-2, T1-3(BIZ-032 고침, BIZ-040~044), T1-7, T3-1(eligibility_answers.document_id), T3-2(E절), T3-3, T3-4, T3-5(TC-QUAL-016~033, TC-QUAL-001·008 입력 고침), execution-spec. 확인하고 영향 없음: T1-4(정책 값 없음, POL-025~027 쓰지 않음), T1-5, T1-6, T2-1
- **규모**: 중(새 고객 화면 1, 고친 고객 화면 3, 적격성 진단 API 응답 확장, 서류 붙이기 API 1, 답 표 칸 1). 문서 [CR-140_메뉴_속_화면_4묶음_공고별_판정.md](CR-140_메뉴_속_화면_4묶음_공고별_판정.md)
- **상태**: 진행중(컨셉 승인 10/11 11:35, 설계 캐스케이드 끝, 목업 확정 대기)

### CR-141: 메뉴 속 화면 4묶음 제출 전 검사 (2026-10-11)

- **배경(사용자)**: 10/11 00:48 "서류함 등록, 컨설팀 입찰교실 고객센터 수주분석 맞출판정 등등", 09:26 /전승 CR-137 계획 4절 순서, 10:30 적격성 진단 완료, 나머지는 컨타가 창을 나눔
- **대상**: 정리표 순번 21 제출 전 검사. 입찰 건마다 요구 서류와 올린 서류를 맞춰 빠진 것, PDF 쪽수, 파일 형식을 공고 조건과 대조. 요구 서류 두 길(제출 막기 검사의 요구 항목 표, 입찰 상세 화면의 공고 분석 직접 읽기) 정리
- **대상 기능 ID**: BID-MKT-020 요구 항목 채우기, BID-MKT-021 내 입찰 건 제출 전 검사, BID-MKT-022 입찰 건별 제출 전 검사, BID-MKT-023 요구 서류 올리기와 서류함에서 고르기, BID-MKT-024 형식·쪽수 판정. 규칙 BIZ-045~049, 정책 POL-028(배정 POL-028~030 중 029·030 은 쓰지 않음)
- **변경 타입**: 신규 + 변경(요구 항목을 AI 요구사항 추출 대신 공고 분석에서 채움, MCP save_requirements 와 분석 단계 진입 추출 호출 걷어냄, 슬롯 API 본인 확인, 슬롯 목록 조회 걷어냄)
- **영향 모듈**: U. 제출 전 검사(신설), H. 컴플라이언스(제출 막기가 실제로 동작), D. 입찰 접수(입찰 상세 제출 서류 탭), K. MCP 서버(도구 1개 걷어냄), 서류함(CR-125, 읽기만)
- **영향 설계서**: T1-1(BID-MKT-020~024, BID-BROWSE-003·BID-REQ-002·BID-COMP-003·BID-MCP-002 변경 줄), T1-2, T1-3(BIZ-045~049, BIZ-015 보충), T1-4(POL-028), T1-5(DOCS_RECEIVED 가드, ANALYZING), T1-7, T3-1(client_documents.page_count, 요구 항목 requirement_json 키, 설정 행), T3-2(G-1 변경, G-2 신설, MCP 표, 합계), T3-3, T3-4, T3-5(TC-PRE-001~027), execution-spec. T1-6, T2-1 은 영향 없음 확인(요구 항목 이벤트 없음, iText 8.0.2 이미 있음)
- **규모**: 중(새 고객 화면 2, 고친 고객 화면 1, 조회 API 2, 요구 항목 표 채우기, 올린 서류 쪽수 칸). 문서 [CR-141_메뉴_속_화면_4묶음_제출_전_검사.md](CR-141_메뉴_속_화면_4묶음_제출_전_검사.md)
- **상태**: 운영 반영(10/11 12:53 BE, FE 이어서, 컨타 승인 12:52), 완료 보고 대기

### CR-142: 메뉴 속 화면 4묶음 미 연방 계약 자료와 4곳 (2026-10-11)

- **배경(사용자)**: 10/11 00:48 "서류함 등록, 컨설팀 입찰교실 고객센터 수주분석 맞출판정 등등", 06:01 /전승 큰 기능 9곳 순, 09:26 /전승 CR-137 계획 4절 순서, 10:30 적격성 진단 완료 뒤 컨타가 창을 나눔
- **대상**: 정리표 순번 6 다음 달 공고, 7 재발주 달력, 8 옵션 연장 알림, 11 낙찰가 범위. USAspending.gov(무료, 키 없음)에서 한국 내 미군 계약을 매일 받아 바깥 계약 표에 쌓고 그 위에 4곳을 연다(CR-137 계획 3.4~3.7)
- **대상 기능 ID**: BID-MKT-025~034, BIZ-050~059, POL-031~036 (배정)
- **규모**: 대(바깥 자료 수집 신설, 새 표 3, 고객 화면 4, 조회·등록 API, 계약 끝 메일 틀은 알림 서버 레포). 문서 [CR-142_메뉴_속_화면_4묶음_미_연방_계약_자료와_4곳.md](CR-142_메뉴_속_화면_4묶음_미_연방_계약_자료와_4곳.md)
- **상태**: 완료(컨셉과 정할 것 승인 10/11 11:46, 설계 반영, 목업 확정 10/11 12:16, 구현과 로컬 확인 끝, 운영 배포 10/11 13:08, 첫 전체 받기 10/12 05:00 성공, 운영 낙찰가 범위 화면 확인. 재발주 세 화면과 계약 끝 메일은 CR-147 에서 지움)
- **영향 설계서**: T1-1, T1-2, T1-3, T1-4, T1-6, T1-7, T2-1, T3-1, T3-2, T3-3, T3-4, T3-5, execution-spec

### CR-143: 남의 입찰 건이 열리는 구멍 막기 (2026-10-11)

- **배경(사용자)**: 10/11 12:11 /전승 "남의 입찰 건 상세가 열리는 구멍 ①새 창에서 본인 확인을 넣어 바로 고침(권장)". CR-141 창이 찾음
- **대상**: 로그인한 고객이 남의 입찰 건을 열거나 바꾸던 8곳(상세, 다음 상태, 이력, 문서 목록, 상태 전환, 올린 서류 목록, 올리기, 지우기)에 본인 확인. 관리자는 모든 건을 본다. 로그인 없이 열리던 FlowGuard 확인용 통로는 설정값으로 운영에서 끈다. Aimbase 가 부르는 통로(/mcp)는 새 CR 로 나눔
- **대상 규칙**: BIZ-060 (배정)
- **변경 타입**: 변경(보안 결함 수정)
- **영향 모듈**: D. 입찰 접수(입찰 건 API, 고객 상태 전환, 올린 서류), M. FlowGuard 연동
- **영향 설계서**: T1-3(BIZ-060), T3-2(F, G, M 절), T3-5(TC-OWN-001~013), execution-spec(8-3)
- **규모**: 소(새 API, 화면, 표 없음). 문서 [CR-143_남의_입찰_건이_열리는_구멍_막기.md](CR-143_남의_입찰_건이_열리는_구멍_막기.md)
- **상태**: 완료(구현 062af06, 운영 be 10/11 12:35, 운영 확인 끝)

### CR-144: Aimbase 가 부르는 통로에 열쇠 확인 넣기 (2026-10-11)

- **배경(사용자)**: 10/11 12:11 /전승 "남의 입찰 건 상세가 열리는 구멍 ①새 창에서 본인 확인을 넣어 바로 고침(권장)". CR-143 범위 1 ①(컨타 12:18 승인)에서 Aimbase 통로를 새 CR 로 나눔
- **대상**: Aimbase 가 부르는 통로(/mcp, /mcp/sse, /mcp/message, 내려받기 2곳)를 Aimbase 만 부르게 열쇠 확인. Aimbase 등록에 열쇠를 싣는 운영 변경 포함
- **대상 규칙**: BIZ-061 (배정)
- **변경 타입**: 변경(보안 결함 수정)
- **영향 모듈**: K. MCP 서버(Aimbase 연동)
- **영향 설계서**: T1-3(BIZ-061), T3-2(MCP 절), T3-5
- **규모**: 소(새 API, 화면, 표 없음). 문서 [CR-144_Aimbase_가_부르는_통로에_열쇠_확인_넣기.md](CR-144_Aimbase_가_부르는_통로에_열쇠_확인_넣기.md)
- **상태**: 완료 10/12 00:49(컨타 승인, 커밋 a05e1cb, 운영 BE 배포 10/12 00:46, 운영 확인 00:50)

### CR-145: Aimbase 가 우리 서버에 닿지 못하는 끊김 복구 (2026-10-11)

- **배경(사용자)**: CR-144 정할 것 3 ①(컨타 10/11 12:28 승인, 사용자 10:05 권장 위임) "서버 망 복구를 새 CR 로 먼저 열고, CR-144 운영 확인은 그 뒤에 한다"
- **대상**: 운영 서버 방화벽(ufw)이 Aimbase 컨테이너에서 같은 서버 8183 으로 오는 연결을 막는 것. Aimbase 등록과 소스는 고치지 않는다
- **변경 타입**: 운영 설정 변경(서버 방화벽)
- **영향 모듈**: K. MCP 서버(Aimbase 연동) 운영 경로
- **영향 설계서**: 없음(소스, API, 화면, 표 변경 없음)
- **규모**: 소(서버 방화벽 규칙만). 문서 [CR-145_Aimbase_가_우리_서버에_닿지_못하는_끊김_복구.md](CR-145_Aimbase_가_우리_서버에_닿지_못하는_끊김_복구.md)
- **상태**: 완료(새 서버에서 Aimbase 다시 붙기와 공고 분석 1건 확인 10/12 00:45, 완처리 컨타 06:18 승인)

### CR-146: 회원가입 이메일 인증번호와 SNS 가입 (2026-10-11)

- **배경(사용자)**: 10/11 16:58 "요즘 보안때문에 인증번호는 필수라", 16:58 "인증부분과 sns가입까지 같이 설계를 해주세요", 16:59 네이버, 카카오, 구글. 19:47 /전승으로 정할 것 셋 모두 1. 원본 docs/origins/원본_회원가입_인증과_SNS가입_20261011.md
- **대상**: 이메일 가입에 6자리 인증번호(알림 서버 틀 BID_EMAIL_CODE), 네이버, 카카오, 구글 SNS 가입과 로그인. SNS 이메일로 이미 이메일 가입한 회원은 잇지 않고 이메일 로그인을 안내. SNS 가입도 회사 정보 네 칸과 약관 동의를 받는다
- **대상 기능 ID**: BID-AUTH-001(변경), BID-AUTH-005, BID-AUTH-006, BID-AUTH-007(신규)
- **대상 규칙**: BIZ-062, BIZ-063, BIZ-064, POL-037, POL-038
- **변경 타입**: 신규 + 변경
- **영향 모듈**: A. 인증(가입, 로그인 API와 화면), 회원 표
- **영향 설계서**: T1-1, T1-2, T1-3, T1-4, T1-7, T2-1, T3-1(members 두 칸, member_social_accounts, email_verification_codes, sns_tickets, member_consents 누락 보정), T3-2(A 절), T3-3, T3-4, T3-5(TC-AUTH-009~030), execution-spec. T1-5, T1-6, T2-2 영향 없음
- **다른 곳**: 알림 서버 레포에 틀 BID_EMAIL_CODE 요청(원본 docs/templates/mail/BID_EMAIL_CODE.json). woojinusbid.com https(다른 창). 사업자의 SNS 세 곳 앱 등록과 키
- **규모**: 대(외부 연동 세 곳, 새 표 셋, 새 API 여덟, 새 화면 둘). 문서 [CR-146_회원가입_이메일_인증번호와_SNS_가입.md](CR-146_회원가입_이메일_인증번호와_SNS_가입.md)
- **설계 보정(10/12)**: 마이그레이션 V50 을 V52 로, 오류 코드 SNS_ACCOUNT_IN_USE 추가(T3-2, T3-4 6-3), T3-4 5-9 추가. 구글은 보류로 표시(사용자 06:09 "1", T1-1, T1-7, T2-1, T3-1, T3-2, execution-spec)
- **상태**: 설계 완료. 뼈대 구현과 로컬 확인 완료(10/12, 사용자 00:12 "우선 SNS회원가입 뼈대만", 네이버와 카카오, 키 대기, 구글 보류). 이메일 가입 인증번호 강제(사용자 07:28 "1")까지 운영 배포 완료(10/12 14:04, e5b34ef, 알림 서버 틀은 알림 CR-017). SNS 는 키가 없어 화면에 안 보이고, 키는 화면에 내보낼 때 함께 넣는다(사용자 14:01). 키, 개인정보처리방침, 네이버 검수 남음

### CR-147: 우진비드 긴급 점검 화면 정리 (2026-10-11)

- **배경(사용자)**: 10/11 19:30 "우진비드 긴급 점검.hwp 분석좀 해주세요", 19:47 "우선 AWS에 신규서버 설치중이니. 저런건 그때 하시고 수정할것만 먼저 수정하시죠", 20:01 /전승으로 정할 것 여섯 모두 1. 원본 docs/origins/원본_우진비드_긴급점검_20261011.md
- **대상**: 재발주 예측 묶음(다음 달 공고, 재발주 달력, 옵션 연장 알림)과 그 전용 서버 기능, 회원 계약 표, 계약 끝 메일 삭제. 입찰 교실 묶음 삭제(무료 입찰 교실 화면 삭제, 교육 자료·용어 사전·서식 모음은 고객센터 밑으로). 서류함 고정 다섯 칸과 국문·영문 칸. 마감 전 공고 화면 틀과 빈 상태. SAM 등록 대행 비용 문구
- **대상 기능 ID**: BID-MKT-028~033, BID-EDU-001(삭제), BID-MKT-026, BID-BROWSE-001, BID-PAGE-001, BID-SVC-001(변경), BID-MKT-035(신규)
- **대상 규칙**: BIZ-050, BIZ-051, BIZ-055, BIZ-056(삭제), BIZ-065, BIZ-066(신규), POL-035 삭제, POL-036 변경(한 번짜리 공사 앞자리만 삭제), POL-033 변경(상세 받기 범위로만 쓴다). 이벤트 ContractEndApproaching 삭제
- **변경 타입**: 삭제 + 변경 + 신규
- **영향 모듈**: V. 미 연방 계약 자료(재발주 조회, 회원 계약, 계약 끝 메일 삭제), R. 메뉴 속 화면(입찰 교실, 메뉴, SAM 비용 문구), C. 공고 열람(마감 전 공고), W. 서류함 고정 칸(신규), 회원 표 member_contracts 삭제, member_documents.language
- **영향 설계서**: T1-1, T1-2, T1-3, T1-4, T1-6, T1-7, T3-1(member_contracts 삭제, member_documents.language, 분류 값 목록, 설정 둘 삭제), T3-2(C-4 재발주 조회 둘, C-5 회원 계약 다섯 삭제, N 서류함), T3-3, T3-4, T3-5(TC-FED 25건 폐기, TC-DOC-201~209), execution-spec. T1-5, T2-1, T2-2 영향 없음
- **다른 곳**: 알림 서버 레포에 BID_CONTRACT_END 틀을 더는 부르지 않는다고 알린다(지우는 것은 그 레포가 정한다). 마이그레이션 V51 은 CR-146 의 V50 과 순서를 맞춘다. 이 CR 을 먼저 배포하면 CR-146 창에 V50 을 V52 로 바꾸라고 알린다
- **규모**: 대(메뉴 두 묶음 정리, 화면 셋과 전용 서버 기능 삭제, 표 하나 삭제, 서류함 분류와 국문·영문 칸). 문서 [CR-147_우진비드_긴급점검_화면_정리.md](CR-147_우진비드_긴급점검_화면_정리.md)
- **상태**: 구현과 로컬 확인 완료(10/12, 커밋 a848aa3, 서버 시험 347건 통과, V51 은 Flyway 로 적용 확인). 상용(AWS) 배포 완료(10/12 00:25, 사용자 00:24 승인, deploy_aws.sh 로 커밋 62e53d6, 상용 DB 에 V51 적용, 탭 제목과 아이콘 확인)

### CR-148: AWS 이전 뒤 관리자 공고문 수정 채팅 복구 (2026-10-12)

- **배경(사용자)**: 10/11 운영을 AWS(54.116.150.247)로 옮기고 https 를 붙인 뒤, 관리자 공고문 수정 채팅이 옛 서버 http 주소에서 위젯을 불러 https 화면에서 막히는 것을 확인. 10/12 00:01 "ㄱ1"(채팅을 https 주소로 열고 관리자 화면이 그 주소를 부르게 고침), 00:06 /전승
- **대상**: 관리자 공고문 수정 채팅. 원인 셋. (1) 위젯을 옛 서버 http://59.8.160.12:8280 에서 불러 https 화면에서 막힘 (2) Aimbase widget.allowed-origins 와 앱의 위젯 토큰 origin 이 http://woojinusbid.com 뿐 (3) 화면이 위젯 전역의 createWidget 을 부르지만 UMD 로더는 init 만 내놓아 옮기기 전부터 열리지 않음
- **변경**: 관리자 화면 NoticeCorrectionChat 은 위젯 주소 기본값을 같은 주소 /aimbase 로 바꾸고 createWidget 대신 init 을 부른다. AWS 웹 서버는 /aimbase/widget/ 과 채팅 경로(chat, workflows/runs/{id}/subscribe, knowledge-sources/{id}/chunks)만 Aimbase 로 전달하고 그 밖의 /aimbase 는 404. AWS Aimbase global_config widget.allowed-origins 에 https://woojinusbid.com 추가. AWS 앱 실행 설정 WIDGET_ALLOWED_ORIGIN=https://woojinusbid.com
- **변경 타입**: 변경(결함 수정과 운영 설정)
- **영향 모듈**: 관리자 공고문 관리(교정 채팅), Aimbase 위젯 연동
- **영향 설계서**: 없음(새 API, 화면, 표 없음). 설계서의 59.8.160.12 서버 주소 표기는 AWS 이전 기록에서 함께 정리한다
- **다른 곳**: 59.8.160.12 는 이제 시험 서버다. 그 서버에서 이 채팅을 쓰려면 그 서버 웹 설정에도 같은 /aimbase 경로가 필요하다. AWS 는 배포 파이프라인이 아직 없어 관리자 화면 파일을 직접 복사해 올린다
- **규모**: 소(화면 파일 하나와 운영 설정)
- **상태**: 운영 반영 완료(10/12 00:12 승인). 공개 경로, 허용 주소, 위젯 토큰 발급, 채팅 요청이 Aimbase 까지 닿는 것 확인. 답은 Anthropic API 키 크레딧 부족(400 credit balance is too low)으로 비어 온다, 별건

### CR-149: 상용(AWS) 배포 스크립트 (2026-10-12)

- **배경(사용자)**: 10/12 00:06 "이젠 aws는 상용이기때문에 CI/CD배포로 가야할거 같습니다. 59번은 테스트용이고", 00:17 "우선 그럼 ci/cd구축전에는 배포 스크립트를 만들어주세요 deploy_aws.sh과 같이"
- **변경**: deploy_aws.sh 신설. 고른 커밋(기본 HEAD)을 임시 worktree 로 꺼내 빌드하므로 커밋된 것만 올라간다. 빌드를 모두 끝낸 뒤 BE 를 먼저 올려 기동을 확인하고 FE 를 올린다. 바꾸기 전 jar, compose, 화면 폴더는 .prev 로 한 벌 남기고, 배포마다 서버 deploy-history.log 에 커밋을 적는다. 상용 서버 compose 를 docker-compose.aws.yml 로 레포에 두고 배포 때 올린다(비밀값은 서버 .env 에만). deploy.sh 는 테스트 서버(59.8.160.12)용으로 머리 주석과 끝 안내만 고침
- **변경 타입**: 신규(배포 도구)
- **영향 모듈**: 배포
- **영향 설계서**: 없음(앱 소스, API, 화면, 표 변경 없음)
- **다른 곳**: 실행 설정(환경변수)을 더하는 CR(예: CR-144 의 MCP_API_KEY)은 이제 docker-compose.aws.yml 에도 넣어야 상용에 반영된다. 서버 compose 를 직접 고치면 다음 배포에 덮인다
- **추가(사용자 10/12 00:25)**: 상용 앱에 CUSTOMER_PORTAL_URL 이 빠져 고객 메일 링크가 localhost 로 나갔다. 사용자가 서버 .env 에 https://woojinusbid.com 을 넣었고, 비밀값이 아니라 docker-compose.aws.yml 에도 넣는다
- **규모**: 소(배포 스크립트와 설정 사본)
- **상태**: 완료. 첫 배포 10/12 00:25(커밋 62e53d6, all), BE 기동 확인 뒤 화면 교체, .prev 와 deploy-history.log 남는 것 확인. CUSTOMER_PORTAL_URL 줄(4ad3e8d)은 다음 배포에 올라간다

### CR-150: Aimbase AI 작업을 Max 구독 연결로 옮김 (2026-10-12)

- **배경(사용자)**: CR-148 확인 중 API 키 연결(bidding-claude-sonnet, haiku)이 Anthropic 크레딧 부족으로 거절되는 것을 확인. 10/12 00:22 "2번이죠 아까 키값 다 옮겨놧어요 붙이면되요"(9개 작업을 Max 구독 연결로), 06:10 "1"(4개는 바로 옮기고 도구가 필요한 5개와 수정 채팅은 시험 뒤 옮김), 07:23 "1"(Aimbase 결함을 Aimbase 레포에서 고치고 AWS 에 올린 뒤 5개와 수정 채팅 이동)
- **대상**: AWS Aimbase bidding_system 테넌트의 워크플로 연결과 관리자 공고문 수정 채팅.
  - 글만 주고받는 4개(correct-notice-analysis, requirement-extraction, summarize-opportunity-for-selection, type-pattern-extraction)는 cli-runner-bidding-001 로 옮김(06:11)
  - 앱 도구를 부르는 5개(proposal-design, proposal-write-section, proposal-assemble, proposal-verify-fidelity, bid-document-generation)는 도구를 켠 새 연결 cli-runner-bidding-tools-001(tool_mode AIMBASE, 같은 계정과 모델)로 옮김(08:28). 테넌트에 API 키 연결을 쓰는 단계는 0개
  - 관리자 공고문 수정 채팅의 연결 값을 cli-runner-bidding-tools-001 로 바꿈(NoticeCorrectionChat.tsx)
- **Aimbase 결함**: 새 연결로 앱 도구 호출이 "Client failed to initialize" 로 실패했다. Aimbase 도구 서버(/mcp)가 처음 노출할 때의 연결 객체를 붙잡고 있어 앱 재기동 뒤에도 옛 객체로 불렀다. Aimbase CR-146(커밋 9b0b67e)으로 고쳐 AWS 에 08:22 반영, 앱 재기동과 재연결 뒤 도구 호출 성공 확인
- **남은 것**: Aimbase 러너가 18분쯤 쉰 CLI 세션을 정리한 뒤 같은 세션의 첫 호출을 1회 실패시킨다(그다음은 정상). 워크플로는 해당 없고 수정 채팅만 걸린다. Aimbase 별건으로 처리 결정 대기
- **변경 타입**: 운영 설정 변경(Aimbase 워크플로 연결, 새 연결 하나) + 관리자 화면 연결 값 한 줄
- **영향 모듈**: 제안서 생성, 공고 요약, 유형 패턴 추출, 공고문 수정
- **영향 설계서**: 없음(우리 앱 API, 표 변경 없음)
- **되돌리기**: /opt/wjbid/aimbase/backup/workflows-connections-20261012-0611.sql(4개 전), workflows-20261012-pre-move5.sql(5개 전), 화면은 /opt/wjbid/app/admin-console.prev
- **규모**: 소(운영 설정과 화면 값 한 줄)
- **상태**: 진행중(화면 배포와 수정 채팅, 제안서 작업 확인 남음)

### CR-151: 메인과 곳곳 사진 교체 (2026-10-12)

- **배경(사용자)**: 10/12 07:32 "메인화면하고 간간히 나오는 사진들을 보면 중국인가 하는 이상한 사진들이 잇어요 그거좀 세련된 유럽 미국인으로 변경해주시구요. 그리고 메인화면은. 아무래도 군사 관련이니 비행기나 배.. 뭐 그런것으로 해주세요" [origins/원본_사진교체_20261012.md](../origins/원본_사진교체_20261012.md)
- **대상**: 고객 포털 사진 4장(2025-04 한미 조달설명회 현장)을 바꾼다. 홈은 군용기와 함정 4장, 로그인, 회원가입, 서비스 안내는 유럽·미국인 사진 4장. 사진 자리와 문구는 그대로. 출처는 [기록/고객화면_사진_출처.md](../기록/고객화면_사진_출처.md)
- **변경 타입**: 화면 사진 교체
- **영향 모듈**: 고객 포털 홈, 로그인, 회원가입, SNS 가입, 서비스 안내, 항목별 준비 안내
- **영향 설계서**: T3-3 화면 구조(홈, 서비스 안내, 로그인, 회원가입의 사진 줄)
- **규모**: 소(사진 파일과 화면 정의 파일 두 곳의 경로). 문서 [CR-151_메인과_곳곳_사진_교체.md](CR-151_메인과_곳곳_사진_교체.md)
- **상태**: 완료(상용 배포 10/12 07:53 커밋 a6513bd fe, 운영 화면 62e53d6 에 사진만 얹음. 옛 사진 4장 삭제 b69c82b)

### CR-152: 모은 공고를 누르면 우리 공고 상세로 (2026-10-12)

- **배경(사용자)**: 10/12 07:51 "그런데 공고를 클릭하면 우리 번역 상세화면으로 가야지", 07:52 "왜 SAM연결인가요?", 07:54 공고문이 없는 공고도 우리 상세 화면으로 여는 1번 선택 "그렇죠 우리 상세화면에서 SAM가기를 누르면 되죠" [origins/원본_모은공고_상세연결_20261012.md](../origins/원본_모은공고_상세연결_20261012.md)
- **현황(상용 실측 10/12 07:50)**: 모은 공고 398건 중 공개 한글 공고문이 있는 것 21건. 나머지 377건은 줄을 누르면 SAM.gov 원문이 새 창으로 열렸다(CR-135 규칙). 한글 제목 395건, 모아 둔 원문 본문 51건
- **대상 기능 ID**: BID-BROWSE-002, BID-MKT-001~006 과 기지 지도(줄 누름 규칙)
- **변경 타입**: 변경
- **대상**: 모은 공고 화면 7곳(사전 공고, 변경 공고, 최근 낙찰, 낙찰 업체, 월별 동향, 업종별 발주, 기지 지도)의 줄 누름을 우리 공고 상세로 바꾼다. 공고문이 없는 공고는 새 원본 공고 상세(`/search/origin/{opportunityId}`, `GET /opportunities/origin/{opportunityId}`)로 열고, 공고문이 공개되면 공고문 상세로 전환한다. 공고 상세 머리에 SAM.gov 원문 보기 단추. 공고 상세는 지금처럼 로그인해야 연다
- **영향 모듈**: 고객 포털 모은 공고 화면, 공고 상세 / BE 공고 조회
- **영향 설계서**: T1-1 1.15, T3-2 1.14, T3-3 1.12, T3-4 1.9, T3-5 1.9(TC-BROWSE-006~009). T1-3, T1-5, T1-6, T2, T3-1 은 영향 없음(새 규칙, 상태, 이벤트, 표 없음)
- **규모**: 중(새 화면 1, 새 조회 API 1, 화면 7곳 줄 누름). 문서 [CR-152_모은_공고를_누르면_우리_공고_상세로.md](CR-152_모은_공고를_누르면_우리_공고_상세로.md)
- **상태**: 완료. 상용 배포 10/12 08:22(배포분 3ea5700, 상용에 올라 있던 것에 이 건만 얹음), 상용 화면 확인 08:26. 상세 화면의 SAM.gov 링크는 로그인 없이 열리는 공개 주소(samUrl)로 바꿨다

### CR-153: 고객 화면 사진 자동 교체(관리자 사진 묶음) (2026-10-12)

- **배경(사용자)**: 10/12 07:58 "메인사진하고, 간간히 들어가는 사진들은 주기적으로 자동으로 변경하게 할수 없나요?", 08:00 "1"(관리자 화면에서 자리별 사진 묶음을 올리고 고객 화면은 방문마다 돌려 보이기) [origins/원본_사진자동교체_20261012.md](../origins/원본_사진자동교체_20261012.md)
- **대상 기능 ID**: BID-SITE-001(관리자 사진 묶음 관리), BID-SITE-002(고객 화면 사진 고르기)
- **변경 타입**: 신규
- **대상**: 고객 화면 사진 자리 10곳(홈 4, 로그인, 회원가입, 서비스 안내 4묶음)이 코드에 고정된 경로 대신 DB 의 사진 묶음을 읽고 방문마다 한 장을 고른다. 관리자 콘솔에 「고객 화면 사진」 화면을 더해 자리별로 올리고 숨기고 지운다. 올린 사진은 가로 1600, 400KB 안팎 JPEG 로 줄여 저장한다. 처음 배포 때 지금 사진 8장이 자리마다 들어 있어 화면은 그대로다
- **영향 모듈**: 고객 포털 홈, 로그인, 회원가입, SNS 가입, 서비스 안내, 항목별 준비 안내 / 관리자 콘솔 / BE 사진 묶음, 파일 저장, 운영 설정
- **영향 설계서**: T1-1 1.16, T1-2 1.7, T1-3 1.6(BIZ-067~068), T1-4 1.6(POL-039), T2-1 1.7, T3-1 1.10, T3-2 1.15, T3-3 1.13, T3-4 1.10, T3-5 1.10(TC-SITE-001~012), execution-spec 1.9. T1-5, T1-6 은 영향 없음(상태, 이벤트 없음)
- **규모**: 중(새 표 2, 새 API 6, 관리자 새 화면 1). 문서 [CR-153_고객_화면_사진_자동_교체_관리자_사진_묶음.md](CR-153_고객_화면_사진_자동_교체_관리자_사진_묶음.md)
- **다른 CR 과 이어짐**: 운영에 V53 을 V52(CR-146)보다 먼저 올리려 spring.flyway.out-of-order 를 켠다. CR-146 을 운영에 올릴 때 이 설정이 있어야 V52 가 들어간다
- **상태**: 상용 배포 완료(10/12 08:43 `./deploy_aws.sh all ed3840b`, 브랜치 deploy/cr153 은 운영 6ec8dd7 에 이 CR 만 얹음, V53 적용, 운영 화면과 공고 분석 1건 확인 09:05). 바뀌는 방식 선택 칸과 순서 화살표는 사용자 답이 오기 전까지 뺌

### CR-154: 용어표 도입과 입찰 결과 이름 낙찰, 탈락 (2026-10-12)

- **배경(사용자)**: 10/12 08:01 용어 적용 지시 "시스템 전반적으로 검수좀", 08:04 "1"(설계서와 CR 문서를 검사가 보는 자리로 옮김), 09:54 "1"(입찰 결과 이름을 원문대로 낙찰, 탈락으로, 관리자 화면 상태 이름을 설계서와 같게, 원문에 없는 상태 이름 14개 승인)
- **대상 기능 ID**: 없음(이름 변경)
- **변경 타입**: 변경
- **대상**: 설계서 자리(docs/AI-SDLC)와 CR 문서 자리(docs/CR), 용어표 신설, 입찰 상태 AWARDED, NOT_AWARDED 의 이름, 관리자 콘솔 입찰 상태 이름표
- **영향 모듈**: 고객 포털 입찰 상태 / 관리자 콘솔 입찰 관리, 대시보드, 입찰 상세 / BE 입찰 상태 이름, 낙찰 알림 메일 제목
- **영향 설계서**: T1-10 1.0(신설), T1-5 1.4, T1-6 1.6. T3 는 영향 없음(입찰 결과 이름 없음)
- **규모**: 소(이름 변경과 문서 자리 이동). 문서 [CR-154_용어표_도입과_입찰_결과_이름_낙찰_탈락.md](CR-154_용어표_도입과_입찰_결과_이름_낙찰_탈락.md)
- **상태**: 완료. 10/12 14:12 "1=2, 2=1" 로 로컬 화면 확인은 하지 않고, 설계서와 CR 문서 31개의 대화체 낱말 131곳을 고침. 상용 배포 10/12 14:22(191cb62, deploy/cr154-prod = e5b34ef 위에 cefbc86)

### CR-155: 모은 공고 전체 공개와 본문 한글화 (2026-10-12)

- **배경(사용자)**: 10/12 12:07 "1) SAM에서 긁어온건 무조건 다 공개한다...(SAM내용만 번역해서 2) SAM에서 긁어온것중 공고문을 작성한것은 관리자가 오픈여부를 결정해서 오픈한다. 대신 무료회원은 공고문 작성한것은 안보이고 1)번 긁어온거 기본만 보인다.", 12:12 "진행하시죠", 12:17 "1"(회원은 관리자가 기간을 넣는다, CR-156) [origins/원본_모은공고_전체공개와_공고문_회원전용_20261012.md](../origins/원본_모은공고_전체공개와_공고문_회원전용_20261012.md)
- **현황(상용 실측 10/12 12:32)**: 마감 전 공고와 검색은 공개 공고문만 보이는데 공개 공고문 21건이 모두 마감이 지나 0건으로 보였다. 모은 공고 394건 중 마감 전 21건(공고번호로 13건), 원문 본문 52건, 한글 본문 12건
- **대상 기능 ID**: BID-OPP-001, BID-BROWSE-001, 002, 004, 005
- **변경 타입**: 변경
- **대상**: 고객 마감 전 공고 목록, 검색, 첫 화면 목록과 건수를 모은 공고 중 마감 전인 것 전부로(BIZ-069). 원본 공고 상세에 한글 본문. 새로 모은 공고는 수집 때 SAM.gov 본문을 받아 한글로 옮긴다(BIZ-070). 관심 공고 번호를 모은 공고 번호로 맞춘다. 공고문 공개는 그대로 관리자가 정한다
- **영향 모듈**: 고객 포털 마감 전 공고, 홈, 공고 상세, 원본 공고 상세, 관심 공고 / BE 모은 공고 조회, 수집 번역, 공고 조회
- **영향 설계서**: T1-1 1.17, T1-3 1.7(BIZ-069~070), T3-1 1.11(있던 칸 보정), T3-2 1.16, T3-3 1.14, T3-4 1.11, T3-5 1.11(TC-BROWSE-010~017, TC-OPP-015~019), execution-spec 1.10. T1-4, T1-5, T1-6, T1-7, T1-10, T2 는 영향 없음(새 정책 값, 상태, 이벤트, 이름, 라이브러리 없음)
- **규모**: 대(고객 목록 세 곳의 자료 기준 변경, 수집 흐름에 본문 조회와 번역). 문서 [CR-155_모은_공고_전체_공개와_본문_한글화.md](CR-155_모은_공고_전체_공개와_본문_한글화.md)
- **다른 CR 과 이어짐**: 공고문 열람을 회원으로 묶는 것과 숨겨 둔 공고문 2건 공개는 CR-157(처음에 CR-156 으로 적었다)
- **상태**: 완료. 상용 배포 10/12 13:00(`./deploy_aws.sh all d065ebd`, 브랜치 deploy/cr155-prod 는 운영 ed3840b 에 이 CR 만 얹음, CR-146 제외), 마감 전 13건 한글 본문 채움, 운영 화면 확인 13:06

### CR-156: 입찰 요청 마감 임박 알림 예약 작업 오류 (2026-10-12)

- **배경**: 컨타가 CR-153 배포 확인 중 10/12 09:00 운영 로그에서 마감 알림 예약 작업 오류(LazyInitializationException)를 발견, 사용자 10/12 14:01 /전승
- **대상 기능 ID**: BID-NOTIFY-002, POL-009
- **변경 타입**: 결함 수정
- **대상**: 마감 7, 3, 1일 전 알림 예약 작업이 공고를 늦게 불러 매일 멈추던 것, 알림 날짜를 설정으로, 한국 날짜 기준 남은 날, 받는 사람마다 한 번만 보내는 멱등 키
- **영향 모듈**: BE 예약 작업, 알림 수신, 설정
- **규모**: 소. 문서 [CR-156_입찰_요청_마감_임박_알림_예약_작업_오류.md](CR-156_입찰_요청_마감_임박_알림_예약_작업_오류.md)
- **상태**: 완료(10/12 21:42 상용 BE 배포 1317033 = 44a2af9 + 이 수정, 브랜치 deploy/cr156-prod. 10/13 09:00 운영 로그 "마감 알림 0건", 오류 없음). 받는 사람은 관리자 전원과 요청 고객(결정 2 ①)

### CR-157: 공고문 회원 전용과 회원 기간 (2026-10-12)

- **배경(사용자)**: 10/12 12:07 "2) SAM에서 긁어온것중 공고문을 작성한것은 관리자가 오픈여부를 결정해서 오픈한다. 대신 무료회원은 공고문 작성한것은 안보이고 1)번 긁어온거 기본만 보인다.", 12:17 "1"(결제 연동 전까지 관리자가 회원 기간을 넣는다), 14:30 "1=1, 2=1 3=1", 14:35 /전승(입찰 신청은 회원만) [origins/원본_모은공고_전체공개와_공고문_회원전용_20261012.md](../origins/원본_모은공고_전체공개와_공고문_회원전용_20261012.md)
- **현황(상용 실측 10/12 14:40)**: 회원 구분이 서버에 없고 공고문 상세 API 가 로그인 없이 열린다. 고객 7명, 공고문 47건 중 공개 21건(모두 마감 지남), 숨긴 완성 공고문 중 마감 전 2건(10/15 마감)
- **대상 기능 ID**: BID-AUTH-008(신규), BID-BROWSE-002, BID-REQ-001, BID-ADMIN-002, BID-PRICE-001, BID-QUAL-001, BID-MKT-015, 016, 017
- **변경 타입**: 신규, 변경
- **대상**: 회원 기간 표와 관리자 회원 기간 넣기, 지우기, 이력. 공고문 상세, 요구 서류, 적격성 진단, 공고별 판정, 입찰 신청을 회원과 관리자만(BIZ-071, BIZ-072). 원본 공고 상세에 관심 공고 단추와 회원 전용 안내. 요금 안내 견줌 표를 실제 열람 조건에 맞춤. 숨긴 공고문 2건은 상용 배포와 함께 공개
- **영향 모듈**: BE 회원, 공고 조회, 적격성, 입찰 신청, 관리자 회원 / 고객 포털 원본 공고 상세, 공고 상세, 모은 공고 줄 누름, 마감 전 공고, 적격성 진단, 공고별 판정, 요금 안내, 내 프로필 / 관리자 콘솔 회원 관리
- **영향 설계서**: T1-1 1.18(BID-AUTH-008 신규와 8개 기능), T1-2 1.8(A. 인증 8, 합계 91), T1-3 1.8(BIZ-071~072), T1-10 1.1(회원, 무료회원, 회원 기간), T3-1 1.12(member_memberships, V55), T3-2 1.17, T3-3 1.15, T3-4 1.12, T3-5 1.12(TC-MEM-001~014, 합계 288), execution-spec 1.11. T1-4, T1-5, T1-6, T1-7, T2 는 영향 없음(정책 값, 상태, 이벤트, Sprint 항목, 라이브러리 없음)
- **규모**: 대. 문서 [CR-157_공고문_회원_전용과_회원_기간.md](CR-157_공고문_회원_전용과_회원_기간.md)
- **번호**: CR-155 에는 이 일을 CR-156 으로 적었으나 CR-156 은 10/12 14:01 마감 알림 오류에 먼저 붙었다
- **상태**: 완료(10/12 15:11 상용 배포 e193275, 15:52 숨긴 공고문 2건 공개)

### CR-158: 새 공고 알림 메일의 상세 주소 (2026-10-12)

- **배경(사용자)**: CR-157 공고문 공개 확인 중 찾은 결함. 10/12 15:57 "예"(다음 CR 로 고친다)
- **현황(소스 실측)**: 공고문을 노출 승인하면 고객 전원에게 나가는 새 공고 알림 메일의 상세 주소가 `/search/{모은 공고 번호}` 다. 이 주소는 공고문 번호를 받아 공개 공고문에서만 찾으므로 회원이 눌러도 404 다. 주소는 CR-006(5/28)에서 들어왔다
- **대상 기능 ID**: BID-NOTI(고객 새 공고 알림, CR-006)
- **변경 타입**: 결함 수정
- **대상**: 상세 주소를 원본 공고 상세 `/search/origin/{모은 공고 번호}` 로. 누구나 열리고 회원은 거기서 공고문으로 간다(CR-157). 메일 문안 틀(bp-notification BID_OPPORTUNITY_APPROVED)은 그대로
- **영향 모듈**: BE 알림(NotificationEventListener)
- **영향 설계서**: 없음(주소 값 하나, API 와 표 변경 없음). 시험은 NotificationEventListenerTest 에 하나 더함
- **규모**: 소
- **상태**: 완료(10/12 16:07 상용 BE 배포 44a2af9 = e193275 + 이 수정, 브랜치 deploy/cr158-prod). 서버 jar 에 새 주소가 든 것과 원본 공고 상세 주소가 열리는 것을 확인, Aimbase 도구 연결 다시 붙임(19개)

### CR-159: 컴플라이언스 매트릭스와 커버리지 검증 (2026-10-12)

- **배경(사용자)**: 10/12 21:46 ax-proposal 스킬셋을 우리 쪽에 적용할 것 검토, 21:55 "1~5까지 다 괜찮은거 아닌가요? 순서대로 넣어주시고", 22:06 "예 그렇게하세요", 22:17~22:20 스킬은 지시문으로 풀지 않고 다른 LLM 과 다른 스킬도 같은 방법으로(Aimbase 에 요청, 22:25 "시켰습니다."), 22:29 "1"(서버와 화면 먼저, 워크플로우는 Aimbase 범용 스킬 뒤). [origins/원본_제안스킬_적용_20261012.md](../origins/원본_제안스킬_적용_20261012.md) 적용 목록 1
- **현황(소스 실측 10/12)**: 제안서 절의 요구 ID 는 설계 단계가 그때그때 지어 쓰고, 절 작성은 요구 원문을 받지 않는다. 요구 하나하나가 답해졌는지 세는 곳이 없다. 컴플라이언스 모듈의 매트릭스 생성, 매핑, 제출 준비 검사 함수는 부르는 곳이 없고 제안서 절 충족 매핑을 만드는 곳도 없다. 평가 항목 구조 칸(factor_tree_json)은 늘 비어 있다
- **대상 기능 ID**: BID-OPP-006, BID-DOC-001, BID-COMP-001, BID-COMP-002
- **변경 타입**: 변경
- **대상**: 공고 분석이 컴플라이언스 매트릭스(요구 하나에 한 줄, 원문 그대로, 출처와 인용, Shall/Should/May, 요구 동사, 질의 후보)와 평가 항목 구조를 낸다. 매트릭스 줄은 요구 항목 표에 REQ ID 를 달고 들어가고, 고객 화면과 지금 워크플로우 입력은 제출 서류 줄만 읽는다(BIZ-073~075). 제안서 설계는 매트릭스의 REQ ID 만 쓰고(BIZ-076), 절 검증이 답한 REQ ID 를 충족 매핑에 남긴다(BIZ-077). 커버리지 검증을 관리자 제안서 Section 관리에 보이고 막지는 않는다(BIZ-078). 관리자 공고문 관리 상세에 매트릭스 영역
- **영향 모듈**: Aimbase 공고 분석, 제안서 설계, 절 작성, 검증 워크플로우(Aimbase 범용 스킬 묶음 뒤) / BE 요구 항목 맞추기, 요구 항목을 읽는 다섯 곳, 공고 분석 저장, 제안서 MCP 도구 다섯, 충족 매핑, 커버리지 계산, 관리자 API 둘, V56 / 관리자 콘솔 공고문 관리 상세, 제안서 Section 관리
- **영향 설계서**: T1-1 1.19(BID-OPP-006, BID-DOC-001, BID-COMP-001, 002), T1-3 1.9(BIZ-073~078, BIZ-045 덧붙임), T1-10 1.2(컴플라이언스 매트릭스 용어 12개, 매트릭스에 없는 ID 는 10/12 22:49 승인), T3-1 1.15(notices.compliance_matrix_json, opportunity_requirement_items.req_id·req_level, opportunities.last_req_no, V56, 설계를 다시 저장하면 그 문서의 제안서 절 충족 매핑을 지움), T3-2 1.18(관리자 API 둘, MCP 도구 다섯, 빠져 있던 공고문 관리와 제안서 트리와 MCP 도구를 옮겨 적어 REST 140, MCP 19), T3-3 1.16, T3-4 1.13, T3-5 1.15(TC-COMP-001~027, 합계 321), execution-spec 1.12. T1-2 는 기능 수가 그대로라 바뀌지 않는다. T1-4, T1-5, T1-6, T1-7, T2 는 영향 없음(정책 값, 상태, 이벤트, Sprint 항목, 라이브러리 없음)
- **규모**: 대. 문서 [CR-159_컴플라이언스_매트릭스와_커버리지_검증.md](CR-159_컴플라이언스_매트릭스와_커버리지_검증.md)
- **선행**: Aimbase 범용 스킬 묶음(Aimbase 레포 `docs/origins/원본_요구사항_스킬묶음_범용화_20261012.md`). 스킬 원문은 [origins/ax-proposal 스킬셋/](../origins/) 에 그대로 풀어 두었다
- **상태**: 서버와 관리자 화면 구현 완료, 로컬 확인(10/12 V56 적용, 관리자 API 둘, MCP 도구로 매트릭스 저장과 설계 거부, 절 검증 기록, 커버리지 화면). 상용 배포 10/20 05:29 커밋 365329b(deploy/cr159-prod = d91bc23 위에 CR-159, all, 시험 515건 통과). 상용 DB V56 적용(순서 밖, V57 뒤), Aimbase 다시 연결 connected 도구 19개, 관리자 매트릭스 API 200 확인. 워크플로우 변경은 SAM.gov판 제안 스킬 묶음을 우진비드 AWS Aimbase 에 올린 뒤

### CR-160: PIEE 입찰서류 자동 받기 (2026-10-14)

- **배경(사용자)**: 10/14 05:07 "원본공고에 들어가면. 하단에  PIEE입찰서류보기해서 원문 다운로드 받는곳이 있습니다. 이거 자동화하는 방법 고민", 06:25 받는 때는 "공고모을때", 07:28 정정 때 다시 받아 새 파일만 더한다("1"), 07:45 정정 새 판에도 PIEE 서류 전부를 붙이고 같은 파일은 저장을 같이 쓴다("1"). 원문 [origins/원본_PIEE입찰서류_자동받기_20261014.md](../origins/원본_PIEE입찰서류_자동받기_20261014.md)
- **현황(실측 10/14)**: 관리자는 원본 공고 상세의 PIEE 입찰서류 보기(CR-034)로 PIEE 를 열어 Download All Attachments 로 압축을 받고 그 압축을 손으로 올린다(CR-037). CR-034 때 적은 "PIEE 는 자동으로 못 받는다" 는 틀렸다. 로그인과 브라우저 없이 요청 두 번으로 받는다. 공고번호 주소(`oppMgmtLink.xhtml?solNo=`)를 열면 쿠키를 받고 공개 공고 화면(`viewPublicNotice.xhtml`)으로 넘어가고, 같은 쿠키로 그 화면의 양식을 Download All Attachments 단추 값으로 보내면 `{공고번호}.zip` 이 온다. W9128A23R0021 은 원본 7개와 정정 폴더 0001~0005 를 합쳐 26개 파일 222MB 였다. PIEE 에 없는 공고번호는 PIEE 첫 화면(`index.xhtml`)으로 넘어가고 단추가 없다. PIEE 의 정식 API 는 찾지 못했다(공개 문서 검색). 정정은 같은 공고번호의 새 판(새 notice_id)으로 들어와 수집에서는 새 공고다. 지금 첨부는 새 공고에서만 SAM 첨부를 받는다. 시험은 개발자 맥(한국 망)에서 했고 상용 서버 망에서는 아직 안 했다
- **대상 기능 ID**: BID-OPP-003
- **변경 타입**: 변경
- **대상**: 수집 때 새로 들어온 공고(정정 새 판 포함)와 내용이 바뀐 공고마다 서버가 PIEE 압축을 받아 풀고 파일마다 첨부로 넣는다(BIZ-079). 같은 공고에 내용이 같은 파일이 있으면 넣지 않고, 같은 공고번호의 다른 판이 이미 저장한 파일이면 저장을 같이 쓴다(BIZ-080). 받기 결과와 받은 파일 수, 시각을 공고에 남기고 관리자 원본 공고 상세 PIEE 칸에 한 줄로 보인다. 손으로 올리기(CR-037)는 그대로 두고 압축 풀기를 서비스로 옮겨 같이 쓴다. PIEE 입찰서류 보기 주소를 설정값(POL-040)으로 옮긴다. CR-160 전에 모은 공고는 받지 않는다
- **영향 모듈**: BE 수집 적재(OpportunityService), PIEE 받기(신규 PieeFetchService, PieeFetchWorker), 첨부 넣기(신규 OpportunityAttachmentIngestService), SAM 첨부 자동 받기, 관리자 첨부 올리기, 관리자 원본 응답, V57 / 관리자 콘솔 원본 공고 상세
- **영향 설계서**: T1-1 1.20(BID-OPP-003), T1-3 1.10(BIZ-079, BIZ-080), T1-4 1.8(POL-040), T1-10 1.3(PIEE 입찰서류 받기 용어 5개, 신조 4개 10/14 12:40 승인), T3-1 1.16(opportunities.piee_*, opportunity_attachments.content_sha256, PieeFetchStatus, V57), T3-2 1.19(OpportunityAdminDto piee 칸), T3-3 1.17(원본 공고 관리 상세), T3-5 1.16(TC-PIEE-001~021), execution-spec 1.13. T1-5, T1-6, T1-7, T2-1, T3-4 는 영향 없음 확인(받기 결과는 공고 한 칸이고 이벤트를 새로 내지 않으며, 기능 ID 가 그대로이고, 새 라이브러리가 없고, 입력 칸이 없다)
- **규모**: 중
- **상태**: 서버와 관리자 화면 구현, 시험 503건 통과(TC-PIEE-001~021 포함). 맥에서 실제 PIEE 로 받기 확인(10/14, W91QVN26QA030 압축 11.7MB 파일 2개 6초, 없는 공고번호는 PIEE 에 없음). 상용 배포 10/14 17:08 (deploy/cr160-on-cr161-prod e0b883d = CR-161 2dfa2e2 위에 CR-160, all, 시험 478건 통과). 상용 DB V57 적용, 받기 설정 POL-040 들어감, 상용 서버에서 PIEE 공개 화면이 열림. 상용 첫 받기 확인: 10/15 18:00 에 모은 W90VN626QA018 은 42초 뒤 PIEE 파일 15개가 첨부로 들어갔고, 12:00 에 모은 W90VN626QA017 은 PIEE 에 없음으로 남았다. 10/16 확인: 그 15개는 PIEE 압축 원본과 이름, 크기, 해시가 모두 같고 서버 저장소에 그대로 있다. 관리자 원본 공고 상세에 'PIEE 입찰서류 15개 받음 (10/15 18:00)' 줄과 15개 수집됨 첨부가 보이고 내려받기도 된다. 완료. 받기 일꾼이 도는 동안 번역 같은 다른 저장이 옛 받기 결과로 덮어쓰지 않게 공고는 바뀐 칸만 쓴다(@DynamicUpdate). PIEE 에서 받은 첨부는 출처가 주소가 아니라 관리자 화면에서 외부 원본 링크를 걸지 않는다

### CR-161: 새 공고 알림 결제 회원만 (2026-10-14)

- **배경(사용자)**: 10/14 16:26 "신규공고는 결재 가입자한테만 날라가야하는데 지금 다 날라가나보네", 16:46 "결재 회원만 하게 해주세요"
- **현황(실측 10/14)**: 공고 노출 승인 알림이 회원 기간을 보지 않고 고객 전원에게 나간다. 10/14 07:41 고객 8명 전원에게 나갔고 그중 회원은 0명. 요금 안내 견줌 표는 새 공고 알림을 무료에도 된다고 보인다(CR-157)
- **대상 기능 ID**: BID-NOTIFY-001, BID-PRICE-001
- **변경 타입**: 변경
- **대상**: 새 공고 알림을 오늘 회원 기간 안의 고객(BIZ-072)에게만 보낸다. 견줌 표의 새 공고 알림을 회원만으로 고친다
- **영향 모듈**: BE NotificationEventListener / 고객 포털 PricingPage
- **영향 설계서**: T1-1 BID-NOTIFY-001, BID-PRICE-001
- **규모**: 소. 문서 [CR-161_새_공고_알림_결제_회원만.md](CR-161_새_공고_알림_결제_회원만.md)
- **상태**: 완료. 상용 배포 10/14 17:03 (deploy/cr161-prod 2dfa2e2, all)

### CR-162: 고객 화면 문구와 배치 (2026-10-15)

- **배경(사용자)**: 발주사 개선 요청 장표 "우진비드 개선사항"(origins/원본_우진비드_개선사항_20261014.md) 3장 1(메인 글자 크기), 4장 1(회원 구분 표시), 5장(AI 공고문 문구), 6장 1(맞춤 판정 위치와 이름), 6장 2(우진이엔씨 운영 표시), 7장(등록·컨설팅 안내 간략화). 10/15 00:22 가상 이용자 수 요청, 00:26 "실제 숫자로 대신", "입찰 자격 진단", "AI스러운 용어.구어체 등등", "고객에게는 AI가 작성해준다라는걸 심어주면안됩니다", 뒤이어 설계안 승인 "1"
- **현황(실측 10/15)**: 고객 화면에 AI 글자 2곳, 요술봉 아이콘 6곳, 로봇과 로켓 아이콘 각 1곳. 메뉴 다섯째에 "맞춤 판정". 유틸 띠에 운영사 이름이 없다. 홈 공고 줄 제목 18px 700. 등록·컨설팅 안내 8개가 단계 설명, 안내 상자, 준비할 것을 모두 보인다. 고객 포털 문구 약 1,500줄과 고객 알림 문구를 점검해 217줄을 고칠 곳으로 골랐다
- **대상 기능 ID**: BID-SITE-003(신설), BID-BROWSE-005, BID-BROWSE-006, BID-AUTH-008, BID-QUAL-001, BID-SVC-001, BID-PRICE-001
- **변경 타입**: 변경, 신규(BID-SITE-003)
- **대상**: 홈 공고 줄 글자 축소, 맞춤 판정을 입찰 자격 진단으로 바꿔 메뉴 맨 뒤로, 유틸 띠 "(주)우진이엔씨가 운영하는 미군·연방 조달 입찰 정보 서비스", 대표 문장 아래 실제 자료 규모 한 줄(공개 조회 신설), 유틸 띠 회원 구분 꼬리표, 고객 화면 문구 217줄과 아이콘, 등록·컨설팅 안내 간략화, 고객 알림 제목 머리말 정리. 고객 화면 이름 9개를 정한다
- **영향 모듈**: BE 사이트 자료 규모 조회(신규), 고객 알림 제목 / 고객 포털 메뉴 정의, 사이트 정보, 유틸 띠, 홈, 화면 정의 파일 문구, 아이콘, 등록·컨설팅 안내
- **영향 설계서**: T1-1 1.21(BID-SITE-003 신설, BID-BROWSE-005, BID-BROWSE-006, BID-AUTH-008, BID-QUAL-001, BID-SVC-001, BID-PRICE-001 변경), T1-3 1.11(BIZ-081, BIZ-082), T1-10 1.4(고객 화면 이름 10개, 원문 1개, 신조 9개 10/15 승인), T3-2 1.20(C-3a 사이트 자료 규모), T3-3 1.18(공통 틀, 메뉴 목적지, 홈, 등록·컨설팅 서비스 안내, 입찰 건별 제출 전 검사, 적격성 진단, 공고별 진단, 공고별 판정, 미 연방 계약 자료 공통, 공고 상세, 입찰 상세, 회원 전용 화면), T3-4 1.14(상태별 UI 분기, 적격성 진단), T3-5 1.17(TC-SITE-013~016), execution-spec 1.14(Sprint 3, 화면 API 매핑), T1-5 1.5(GENERATING 표시 이름 문서 작성 중, 10/15 구현 중 추가 승인). T1-6, T2, T3-1 은 영향 없음(이벤트, 기술, 테이블을 바꾸지 않는다)
- **규모**: 중. 문서 [CR-162_고객화면_문구와_배치.md](CR-162_고객화면_문구와_배치.md)
- **상태**: 완료. 상용 배포 10/15 06:04 커밋 f3d0ae3(deploy/cr162-prod = e0b883d 위에 d31984b, f217213). 배포 뒤 Aimbase 재연결 connected 도구 19개, 상용 홈 자료 규모 줄과 회원 구분 꼬리표 확인

### CR-163: 수집 회차 기록과 수집 완료 메일 (2026-10-15)

- **배경(사용자)**: 10/15 06:40 스모크 세트에 SAM 수집 확인이 없다는 지적, 06:41 수집 이력을 읽기만 하는 확인으로 결정, 08:17 수집 회차가 이력에 남도록 서버를 고치기로 결정, 08:26 수집 완료 메일도 되살리기로 결정
- **현황(실측 10/15)**: 상용 collector_runs 0건. 서버 어디에도 수집 회차를 저장하는 곳이 없어 수집 이력 조회(GET /admin/collection/runs)가 늘 비어 있다. 수집 완료 이벤트의 회차 ID 가 비어 멱등키가 COLLECTION_null 하나로 고정되어, 6/1 12:00 첫 한 통 뒤로 수집 완료 메일이 모두 이미 보냄으로 막혔다. 같은 키를 관리자 전원이 나눠 쓰는 것도 같은 결함이다. 실제 수집은 sam_api_call_log 에만 남는다(10/15 06:00 search 200)
- **대상 기능 ID**: BID-OPP-001, BID-OPP-002, BID-OPP-004, BID-OPP-005
- **변경 타입**: 변경(설계대로 동작하게)
- **대상**: 스케줄 수집과 수동 수집이 회차마다 수집 이력 한 줄(시작 시각, 끝난 시각, 건수, 성공 여부, SCHEDULED/MANUAL)을 남긴다. 수집 완료 이벤트에 그 회차 ID 를 싣고, 수집 완료 메일 멱등키를 회차와 관리자별로 만든다. 스모크 IT-16-009 가 이 이력으로 마지막 수집을 확인한다
- **영향 모듈**: BE OpportunityCollectionScheduler, CollectorRun, NotificationEventListener / 시험항목 items_16, server_vars
- **영향 설계서**: 없음. T3-1 CollectorRun, T3-2 수집 이력 조회, T1-1 BID-OPP-004 에 이미 있는 동작이다
- **규모**: 소
- **상태**: 서버 구현, 전체 시험 514건 통과(10/15). 상용 배포 10/15 09:33 커밋 d4097e9(deploy/cr163-prod = f3d0ae3 위에 130ff7e 의 서버 수정만, be). 배포 뒤 Aimbase 재연결 connected 도구 19개. 상용 확인(10/16 02:40): 수집 이력 12:00, 18:00, 23:00 세 회차 모두 SCHEDULED 성공으로 남음(새 공고 1, 2, 0건). 완료

### CR-164: 낙찰정보 채우기와 수집 번역 한 줄 (2026-10-15)

- **배경(사용자)**: 발주사 개선 요청 장표 "우진비드 개선사항"(origins/원본_우진비드_개선사항_20261014.md) 2장 "낙찰정보 기능 되도록 업데이트 해주세요". 최근 낙찰의 업체 칸이 비고, 낙찰 업체 화면이 비고, 낙찰가 범위 첫 줄이 0건으로 보인다. 10/15 08:26 "순서대로 진행하세요", 09:25 상용에서 지난 1년 낙찰 공지 다시 받기 승인 "예"
- **현황(실측 10/15)**: 상용 낙찰 공지 100건 모두 업체 이름이 비어 있었다. 업체 이름 받기(CR-136, 10/11) 전에 모은 공지이고 지난 낙찰 공지 다시 받기를 상용에서 돌린 적이 없었다. 09:26 다시 받기(2025-10-16~2026-10-15) 결과 SAM 138건, 이름 채움 85건, 새 공지 50건. 새 공지 50건의 번역이 공용 LLM 대기열(동시 2~5, 대기 20)에 한꺼번에 올라 25건은 대기열에 못 들어갔고, 그 25건은 공고 넣기가 그 자리에서 멈춰 업체 이름 저장을 건너뛰었다. 들어간 번역은 번역기(Aimbase 실행기, 메모리 1GB)에 최대 5건이 동시에 들어가 실행기가 메모리 부족으로 두 번 죽었다(09:27:58, 09:30). SAM 은 이 업체들의 고유번호(UEI)를 주지 않아 낙찰 업체는 이름으로 묶인다. 옛 공지 15건은 SAM 에서 낙찰 공지가 지워져 다시 받을 수 없다(예: CCTV 설치 건은 입찰 공고만 남음). 낙찰가 범위는 계산이 맞지만 0건인 "상위 계약 없는 계약" 줄이 먼저 나와 첫 줄이 "0건이라 범위를 내지 않습니다"로 보였다(311812 근거 계약 1,274건은 모두 상위 계약 안 주문)
- **대상 기능 ID**: BID-MKT-004, BID-MKT-007, BID-MKT-008, BID-MKT-009, 낙찰가 범위(CR-142), BID-OPP-001(수집 때 새 공고 번역)
- **변경 타입**: 변경(설계대로 동작하게)
- **대상**: (1) 수집 때 새 공고 번역을 전용 실행 줄에서 한 번에 한 건씩 돌린다. 대기 칸은 설정값(app.async.translation.queue-capacity)이다. (2) 번역 대기에 못 넣어도 공고 넣기와 낙찰 정보 저장은 이어 간다. (3) 낙찰가 범위는 0건인 줄을 보이지 않고 범위를 낸 줄을 먼저 보인다. (4) 배포 뒤 상용에서 지난 낙찰 공지 다시 받기를 한 번 더 돌려 25건 업체 이름을 채우고, 한글 제목이 빈 낙찰 공지를 관리자 제목 번역으로 한 건씩 채운다
- **영향 모듈**: BE AsyncConfig, OpportunityTranslationService, OpportunityCollectorService / 고객 포털 낙찰가 범위
- **영향 설계서**: T1-3 1.12(BIZ-070), T3-3 1.19(낙찰가 범위 범위 두 줄)
- **규모**: 소
- **상태**: 상용 배포(10/15 09:44, d91bc23 = d4097e9 + 2ea6845, deploy/cr164-prod, all). 배포 뒤 Aimbase 다시 연결, 지난 낙찰 공지 다시 받기로 25건 채움(낙찰 공지 150건 중 업체 이름 135건, 15건은 SAM.gov 에서 공지가 내려감). 한글 제목이 빈 낙찰 공지 38건은 남음

### CR-165: 상용 매일 점검 (2026-10-15)

- **배경(사용자)**: 10/15 "상용 스모크는 주요항목 몇 개 뽑아서 매일 업무시작전에 또는 자정쯤 돌려서 문제없게", "테스트는 상용인데 수행하는 로봇은 테스트서버가 맞겠네요", AI 다시 분석까지 매일 돌리기로 선택(1), "진행하세요"로 상용 점검용 관리자 계정, 숨긴 시험 공고문, 알림 틀 생성 승인
- **현황(실측 10/15)**: 스모크 세트(영역 16, 9건)는 상용에서 돈 적이 없었다. 상용에 점검용 관리자 계정과 AI 다시 분석용 시험 공고문이 없었다. 로그인 확인(IT-16-002)은 비밀번호 환경변수가 비어, 관리자 대시보드 확인(IT-16-007)은 판정식이 쓸 수 없는 함수를 불러 실행 오류였다
- **대상 기능 ID**: 영역 16 기본 스모크 IT-16-001~009
- **변경 타입**: 신규(운영 도구)
- **대상**: 테스트 서버(59.8.160.12) cron 이 매일 상용(woojinusbid.com)에 스모크 세트를 서버 판정으로 돌린다. 한 항목이라도 통과하지 못하면(원래 실패, 자동으로 못 봄 포함) 알림 서버 틀 BID_SMOKE_ALERT 로 관리자에게 메일을 보낸다. 점검 결과가 남지 않아도 알린다. 상용 준비물은 점검용 관리자 smoke-admin@example.com(메일이 닿지 않는 주소), 고객에게 숨긴 시험 공고문 TEST-SMOKE-AI-0001(018d62d8-3fe3-46d2-9ee4-83b39eaadd02)이다
- **영향 모듈**: tools/smoke-daily(run_smoke.sh, alert.py), 시험항목 items_16(IT-16-007 판정식). 테스트 서버 /home/therecommerce/wjbid-smoke(비밀값은 smoke.env, 권한 600). 알림 서버 틀 BID_SMOKE_ALERT(id 97)
- **영향 설계서**: 없음
- **규모**: 소
- **상태**: 테스트 서버 설치와 cron 등록(10/15 09:59). 첫 점검 09:57 9건 중 통과 7건, 알림 메일 1통 SMTP 발송 성공. 남은 실패는 IT-16-005(시험 고객에게 회원 기간이 없어 공고문 상세가 막힘), IT-16-009(첫 수집 이력이 12:00 회차에 생김). 10/16 02:40 사용자 승인("1")으로 시험 고객 qa-cr143 에 회원 기간(2026-10-16~2099-12-31, 메모 상용 점검용, 기간은 끝나는 날이 필수라 먼 날로 둠)을 넣었고, 02:41 다시 돌려 9건 중 통과 8건. IT-16-005 는 상용의 공개 공고문 23건이 모두 마감 지나(마지막 마감 10/15 11:00) 회원 공고문 목록이 비어 실패. 10/16 04:17 사용자 결정("1"): AI 다시 분석 확인(IT-16-008)이 완료 상태만 보면 다시 분석이 시작되지 않아도 전날 결과로 통과하므로, 분석 끝난 시각이 이번 점검 시작 뒤인지도 본다(server_vars run_started). AI 서버 실행 기록상 매일 점검의 다시 분석은 실제 공고문과 같은 opportunity-analysis 워크플로우로 돈다. 테스트 서버에 올려 그 항목만 다시 돌려 통과. 같은 시각 사용자 결정("1"): 회원 공고문 확인(IT-16-005)은 마감 지난 공고문까지 목록에 넣어 회원이 상세와 AI 요약을 여는지만 본다. 마감 전 공고문이 있는지는 관리자가 공개할 일이다. 10/16 04:20 테스트 서버 전체 점검 9건 모두 통과, 메일 없음. 10/16 06:50 사용자 "당분간은 성공 메일도": 알림 서버 틀 BID_SMOKE_OK(id 98)를 더하고, smoke.env 의 SMOKE_NOTIFY_SUCCESS=true 이면 전부 통과한 날 통과 메일을 보낸다(끌 때는 이 값만 지운다). 06:51 통과 메일 1통 발송 성공. 10/16 06:53 사용자 "08:30 은 오류가 나면 고칠 시간이 없다, 밤에 봐야": 점검 시각을 매일 23:30 으로 옮김(23시 수집과 그 번역이 끝난 뒤), 실패 메일 틀의 "매일 아침" 문구를 뺌. 10/16~10/19 매일 23:30 점검 네 번 모두 9건 통과, 통과 메일 발송. 10/20 사용자 결정("1"): 한 번도 git 에 없던 시험 항목 폴더 tools/testbook(시험 항목, 시험 파일, 설정)을 올림, 다시 만드는 화면 콘솔은 뺌

### CR-166: 마감 전 공고 회원 전용 꼬리표 (2026-10-20)

- **배경(사용자)**: 발주사 개선 요청 장표 추가 8장(origins/원본_우진비드_개선사항_추가_20261020.md) "노출 승인 후 회원이라고 뜸", "회원이라고 뜬 상태에서 어떤 걸 눌러야 요약 내용을 볼 수 있는지", "마스터가 어떤 회원에게 보여줘..라고 체크 해줘야 하는지". 10/20 05:26 /전승으로 꼬리표를 "회원 전용"으로 바꾸고, 회원 기간은 발주사가 관리자 회원 관리에서 넣도록 장표에 답하기로 결정
- **현황(실측 10/20)**: 장표의 테크마이닝 계정은 상용 회원 기간이 없는 무료회원이다(상용 회원 기간 1건). 마감 전 공고 우리 회사 칸은 무료회원에게 공개 공고문이 있는 줄마다 "회원" 꼬리표를 보인다(CR-157). 꼬리표는 공고 표시인데 발주사는 자기 등급으로 읽었다. 공개 공고문은 회원 모두에게 보이고 회원별로 고르는 기능은 없다
- **대상 기능 ID**: BID-MKT-016
- **변경 타입**: 변경(문구)
- **대상**: 무료회원 줄의 꼬리표 글 "회원" 을 "회원 전용" 으로. 풀이("한글 공고문이 있는 공고입니다. 회원 전용")와 동작은 그대로
- **영향 모듈**: 고객 포털 회원 전용 문구(membershipContent)
- **영향 설계서**: T1-1 1.22(BID-MKT-016), T3-3 1.20(회원 전용 화면 표), T3-4 1.15(시나리오 1-1)
- **규모**: 소
- **상태**: 완료. 상용 배포 10/20 05:32 커밋 c27c30c(deploy/cr166-prod = 365329b(CR-159) 위에 376bd76, fe). 상용 화면 묶음에서 꼬리표 "회원 전용" 확인. 장표 8장에 답 상자(꼬리표 뜻, 관리자 회원 관리에서 회원 기간 넣는 법, 회원별 고르기 없음)
