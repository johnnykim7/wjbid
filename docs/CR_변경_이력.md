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
| CR-018 | 2026-05-29 | 고객 대상 진행 알림 3종(생성완료/접수/합격) + 입찰 결과 상태(AWARDED/NOT_AWARDED) 신설 | BE (domain/bid BidRequestState·BidFSMService·BidRequest, domain/notification NotificationType·NotificationEventListener, controller/admin, 마이그레이션 V16) + FE (admin-console, customer-portal) | 중규모 | 설계 먼저 + 코드 |
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
  2. **파싱** = Aimbase `parse_document`. BE 사전 파싱 없이 첨부 다운로드 URL을 워크플로우 입력으로 넘겨 Aimbase가 발췌(RfpSample fileUrls와 동일 패턴).
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

- **배경**: 점검8. 사용자 기대 = 제안서 생성 완료 시 고객에 "완료" 알림, 제출(접수) 시 "접수" 알림, 합격 시 "합격" 알림(자동 아니라 관리자 수동 업데이트 전제). 실측 결과 고객 대상 알림은 OPPORTUNITY_APPROVED(신규 공고) 1종뿐이고, ①생성완료 알림은 ADMIN 대상(DocumentGenerated), ②접수 알림은 타입/리스너 없음(BidRequestStateChanged 발행되나 알림 리스너 無), ③합격은 **FSM 상태 모델 자체에 없음**(SUBMITTED가 종착).
- **사용자 확정 결정**:
  - 합격 상태모델 = **FSM에 AWARDED/NOT_AWARDED 추가**(별도 결과 필드 분리 안 함). 전이 = **SUBMITTED → AWARDED / NOT_AWARDED**(둘 다 종착, 재도전 전이 없음). 관리자만 전이 가능(기존 CR-017 ③ 화이트리스트로 고객 차단 유지).
  - 알림 3종 발송 시점 = `BidRequestStateChangedEvent` 소비 — `toState=REVIEW`(생성완료), `SUBMITTED`(접수), `AWARDED`(합격). NOT_AWARDED는 MVP 무알림. (생성완료를 REVIEW 전이로 잡은 이유: GENERATING→REVIEW가 AI 문서 생성 완료를 의미하고 이미 StateChanged 이벤트로 발행됨 → DocumentGenerated의 DocumentType별 N건 발행을 고객에게 중복 전송하지 않음.)
  - bp-notification 템플릿 = **기존 템플릿 재사용**(신규 등록 운영작업 없이 즉시 발송). 생성완료=BID_DOC_GENERATED, 접수=BID_REQUEST_ADMIN, 합격=BID_OPPORTUNITY_APPROVED 재사용(문구 부적절 가능성은 감수, 후속 전용 템플릿 등록 가능).
  - 구현 범위 = **BE + FE 전체**.
- **변경 사항**:
  1. `domain/bid/entity/BidRequestState` — `AWARDED`, `NOT_AWARDED` 추가. `isTerminal()`에 둘 다 포함, `getStateDisplay()` 한글 라벨 추가.
  2. `domain/bid/service/BidFSMService.VALID_TRANSITIONS` — `SUBMITTED → [AWARDED, NOT_AWARDED]`. `CUSTOMER_ALLOWED_TARGET_STATES`는 불변(고객은 결과 전이 불가).
  3. `domain/bid/entity/BidRequest` — `transitionTo`에 AWARDED/NOT_AWARDED 시 `outcomeDecidedAt` 기록.
  4. `db/migration/V16` — `bid_requests`에 `outcome_decided_at` 컬럼 추가.
  5. `domain/notification/entity/NotificationType` — 고객 대상 `PROPOSAL_READY_CUSTOMER`/`BID_SUBMITTED_CUSTOMER`/`BID_AWARDED_CUSTOMER` 3종(기존 templateCode 재사용).
  6. `domain/notification/service/NotificationEventListener.onBidRequestStateChanged` — toState 분기로 고객 알림 발송(member 1명). 멱등키 `BR_STATE_{id}_{toState}`.
  7. `controller/admin/BidRequestAdminController` — 기존 `POST /{id}/transition` 으로 AWARDED/NOT_AWARDED 전이 처리(전용 API 신설 안 함, FSM 화이트리스트 재사용).
  8. FE admin `BidRequestDetailPage` — NEXT_STATES/STATE_LABELS/STATE_COLORS에 AWARDED/NOT_AWARDED 추가 → 기존 상태 전환 모달에서 합격/불합격 선택 가능.
  9. FE customer `types`(STATE_LABEL/STATE_BADGE) — AWARDED/NOT_AWARDED 라벨/배지 추가 → 제안서 상세·목록에 결과 표시.
- **영향 범위**: BE (domain/bid, domain/notification, controller/admin, 마이그레이션 V16) + FE (admin-console, customer-portal) + 테스트(BidFSMServiceTest, NotificationEventListener 검증).
- **규모**: 중규모(FSM 상태·데이터 모델·화면·알림 동시 변경 → 설계 캐스케이드 선행).
- **설계 캐스케이드**: T1-5 FSM(BidRequest 상태 + 다이어그램 + AWARDED/NOT_AWARDED 정의), T1-6 이벤트 계약(BidRequestStateChanged 고객 소비 확장) 갱신 완료.
- **검증**: (구현 후 기재) BE 컴파일+전체 테스트, admin/customer tsc. 실발송 E2E는 운영 BidRequest 0건이라 코드/테스트 레벨로 갈음.
- **상태**: 설계 캐스케이드 완료, 코드 구현 진행.

---

### CR-021: 공고문 표시 풍부화 — TipTap 본문 + NOTICE_VIEW 양식 (2026-05-31)

- **배경**: 운영 화면에서 공고문 상세가 `JSON.stringify` 원시값으로 노출되어 사람이 읽기 불가. 사용자 합격선 = 첨부 PDF(W90VN926QA034) 수준의 풍부도(표·계층·강조박스·메타그리드). 정형 JSON(summary/requiredDocuments/documentFormats)은 액션용으로 유지하면서, 사람이 읽는 본문을 별도 슬롯으로 신설.
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
  - **NotificationLog 정리 불필요(실측 반박)**: referenceType은 CollectorRun/BidRequest/Opportunity 등 엔티티명, referenceId에 우리 Notice UUID를 박는 알림 없음. 알림 vars의 `noticeId`는 SAM 공고번호(string, `opp.getNoticeId()`)이지 Notice 엔티티 PK 아님. → 삭제 대상 아님.
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
  1. **WF 정의([opportunity-analysis.steps.v2.json](workflows/opportunity-analysis.steps.v2.json))**: verify `response_schema.summary`에 `contractPeriod`/`siteVisit`(string) 추가. extract_facts 프롬프트 분석 항목에 계약기간(contractPeriod)·현장설명회(siteVisit) 명시. verify Gap Check 체크리스트에 계약기간 추가. _meta version v8. **운영 PUT 완료**(허용 필드만, connection_id·adapter 무손실 diff 검증 — summary 필드 추가 2개/제거 0, AGENT_CALL connection/timeout 무변경).
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
  1. **WF 정의([opportunity-analysis.steps.v2.json](workflows/opportunity-analysis.steps.v2.json))** — verify `response_schema.summary.required`: `[overview]` → **10개 전체**(키 생략 원천 차단). extract_facts·verify 프롬프트에 "★계약기간 추출 원칙★: PWS/원문 문구 그대로 추출, 변환·추측 금지, 없으면 빈 문자열이되 키 생략 금지" 추가. _meta version v9. **운영 PUT 완료**(HTTP 200, updatedAt 06-18 05:25). domain·connection_id(cli-runner-bidding-001) 무손실 검증.
- **함정 교훈(CR-112 갱신)**: WF PUT 400의 실제 원인 규명. `/api/v1/workflows`는 `WorkflowController`(DTO 8필드: id/name/**domain**/triggerConfig/steps/errorHandling/outputSchema/inputSchema)이지 PlatformWorkflowController(category/description)가 아님. **GET 응답을 그대로 PUT하면 GET 전용 추가필드(_meta/projectId/graphMode/active/category/description/updatedAt)가 unknown property로 400**. DTO 8필드만 추려 보내야 통과.
- **규모**: 소~중(WF 프롬프트·스키마 보정, CR-117 후속 버그보정). BE/FE/데이터모델/마이그레이션 무변경. 설계 캐스케이드 생략, 본 CR 이력 + workflows JSON으로 갈음.
- **상태**: **WF 수정·검증 완료(2026-06-18)**. 사용자 결정으로 이 공고 재분석(E2E)은 **보류** — 다음 분석되는 공고부터 자동 적용. 기존 분석본 contractPeriod는 재분석 시 채워짐(원문에 PoP 있으면 값, 없으면 빈 문자열+키 유지).
- **후속(2차 결함 — DB→화면 경로)**: W90VN926RA065(Repair Victory Field, 시설보수)를 WF 수정 후 재분석하니 **DB summaryJson엔 contractPeriod='150 calendar days after NTP' 정상 저장**됐으나 **화면 여전히 빈칸**. 실측: WF/DB는 정상, 진범은 **BE [AnalysisResultDto.java](../backend/src/main/java/com/biddingagency/domain/opportunity/dto/AnalysisResultDto.java) `SummaryDto` record에 contractPeriod/siteVisit/contactInfo 필드 누락** → DB→API 변환에서 값 버려짐(FE는 렌더 준비 완료였음). CR-117에서 FE 4번 섹션 골격은 만들었으나 BE DTO 필드 추가를 빠뜨린 것. **수정**: SummaryDto에 3필드 추가(contractPeriod/siteVisit=string, contactInfo=List<Map>) + from() 매핑. 기존 분석본도 재분석 없이 즉시 노출(메타 변환이라). compileJava 통과 → `./deploy.sh be`(재기동 health UP 06:26) → 운영 API 실측 검증: contractPeriod/siteVisit/contactInfo(2건) 정상 노출 확인. 교훈: **WF→DB와 DB→화면은 별개 경로 — 한쪽 고쳐도 다른쪽 DTO 누락이 남을 수 있음.**
