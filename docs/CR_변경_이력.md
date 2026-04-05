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
| CR-003 | 2026-03-28 | 공고 사전 분석 파이프라인 + LLM 입력 3파이프라인 | BE (domain/opportunity, domain/bid, integration) + FE (admin, portal) | 대규모 | 설계중 |

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
