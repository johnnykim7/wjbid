# CR-144 Aimbase 가 부르는 통로에 열쇠 확인 넣기

- CR-ID: CR-144
- 작성일: 2026-10-11
- 규모: 소(새 API, 화면, 표 없음. /mcp 로 들어오는 요청에 열쇠 확인 하나, Aimbase 에 건네는 내려받기 주소에 기한 서명). 승인 뒤 설계서(T1-3 BIZ-061, T3-2 MCP 절, T3-5 시험)를 먼저 고친다
- 상태: 진행중 (방법 승인 10/11 12:28, 설계 반영, 구현 전)
- 세션: 83a79947 (컨타 043a2d19, 주입 이름 bidding-agency-platform-45467)
- 착수: 2026-10-11 12:19
- 앞 CR: CR-143 (범위 1 ①에서 Aimbase 통로를 이 CR 로 나눔), CR-002 (Aimbase 연동, MCP 서버), CR-013 (샘플 내려받기), CR-019·CR-041 (첨부 내려받기)
- 같은 때: CR-143 (보안 설정 파일), CR-138~142 (백엔드 수정 중)

## 1. 근거

- 사용자 10/11 12:11 /전승: "남의 입찰 건 상세가 열리는 구멍 ①새 창에서 본인 확인을 넣어 바로 고침(권장)"
- CR-143 범위 1 ① (컨타 12:18 승인): "로그인 고객 8곳과 FlowGuard 통로를 이 CR 에서 막고, Aimbase 통로는 새 CR 로 바로 연다(Aimbase 쪽 등록에 열쇠를 실어야 해서 그쪽 확인과 배포가 따로 필요)"

## 2. 범위

- 이 플랫폼이 Aimbase 에 열어 둔 통로(/mcp, /mcp/sse, /mcp/message)와 그 아래 내려받기(/mcp/rfp-files, /mcp/opportunity-attachments)를 Aimbase 만 부를 수 있게 한다.
- 열쇠 값은 코드에 두지 않는다. 운영은 서버 환경변수로 둔다.
- Aimbase 등록에 열쇠를 싣는 운영 변경과 이 플랫폼 운영 배포, 운영 확인까지 이 창이 한다. Aimbase 소스는 고치지 않는다.

## 3. 실측한 것 (2026-10-11 12:20~12:40, 읽기만)

### 3.1 Aimbase 가 우리를 부르는 방식

| 순번 | 무엇 | 본 것 | 위치 |
|------|------|------|------|
| 1 | MCP 서버 등록 | 주소(config.url) 하나만 읽는다. 다른 설정 칸은 쓰지 않는다 | aimbase MCPServerClient.java:71 |
| 2 | 연결 요청 | SSE, Streamable 모두 주소와 연결 기한만 넣고 머리글을 싣지 않는다 | MCPServerClient.java:101~112 |
| 3 | 주소 나누기 | 경로만 쓰고 물음표 뒤 값은 버린다 | MCPServerClient.java:166~181 |
| 4 | 첨부 내려받기(download_file) | 주소를 그대로 부르고 머리글은 없다. 물음표 뒤 값은 그대로 간다 | DownloadFileTool.java:167~178 |
| 5 | 문서 읽기(parse_document url) | 같다(User-Agent 만) | python-sidecar parser.py:53~65 |
| 6 | 쓰는 라이브러리 | MCP SDK 에 요청마다 머리글을 싣는 기능(customizeRequest)이 있다. Aimbase 가 쓰지 않을 뿐이다 | aimbase backend/build/mcp-sdk-src/HttpClientSseClientTransport.java:298 |

Aimbase 안의 Claude CLI 는 Aimbase 자신의 MCP(aimbase-server)에만 붙고, 우리 도구는 Aimbase 를 거쳐 1~3 으로 부른다(ClaudeCliAdapterConfig.java:140~162).

### 3.2 운영 등록 (지금 값, 백업 [기록/Aimbase_MCP_등록_백업_CR-144_20261011.json](기록/Aimbase_MCP_등록_백업_CR-144_20261011.json))

- 이름 bidding-agency-mcp, id 2176f92d-a167-476d-8ced-9687eb598e76, 테넌트 bidding_system
- 전송 sse, 주소 http://59.8.160.12:8183/api/mcp, 자동 시작 켬, 상태 disconnected, 마지막 수정 2026-07-30
- 저장된 도구 20개: get_opportunity, search_opportunities, get_opportunity_requirements, save_requirements, get_bid_request, save_document_version, get_document_template, transition_bid_state, get_opportunity_analysis, save_opportunity_analysis, get_past_submissions, get_reference_samples, save_pattern_guide, get_proposal_design_input, save_proposal_structure, get_section_context, save_section_blocks, get_section_verify_input, get_notice_verify_input, save_verification_result

### 3.3 지금 Aimbase 는 우리 서버에 닿지 못한다

- Aimbase 서버(aimbase-api-1) 안에서 59.8.160.12:8183, host.docker.internal:8183, 172.27.0.1:8183 이 모두 4초 시간초과다. 같은 서버의 8281(Aimbase 문서 처리)도 시간초과, 8280(Aimbase 자신)만 응답한다.
- Aimbase 는 10/10 15:00(재기동) 뒤 1분마다 다시 붙기를 실패한다("Client failed to initialize", 10일 동안 853번). 운영 BE 는 10/11 12:17 기동 뒤 Aimbase 의 연결을 한 번도 받지 않았다.
- 같은 서버 셸에서 부르면 열린다(12:21 /api/mcp/sse 가 세션을 내줌). 서버 방화벽으로 보이나 sudo 권한이 없어 규칙은 못 봤다. 서버 마지막 기동 2026-09-30, Docker 28.1.1.

### 3.4 그래서 Aimbase 공고 분석은 이미 멈춰 있다

| 흐름 | 최근 50건 | 마지막 실행 | 마지막 성공 |
|------|----------|-----------|-----------|
| opportunity-analysis (공고 분석) | 성공 29, 실패 9, 취소 12 | 2026-09-01 실패 | 2026-07-28 |
| proposal-design, proposal-write-section (제안서) | 성공 3, 4 | 2026-06-01 | 2026-06-01 |
| translate-opportunity-title (한글 제목) | 성공 50 | 2026-10-11 12:13 | 같음 |

- 공고 분석 마지막 다섯 번(8/18~9/1)은 모두 첫 단계에서 "알 수 없는 도구: get_opportunity" 로 실패했다. 우리 통로에 붙지 못한 것이다.
- 한글 제목 번역은 LLM 한 단계뿐이라 우리 통로를 쓰지 않는다.

### 3.5 바깥에서도 열려 있다

- 운영 BE 8183 은 0.0.0.0 으로 열려 있고, 운영 nginx 도 /api/ 를 BE 로 넘긴다(CR-143 3.2).
- 보안 설정은 /mcp/** 를 로그인 없이 연다(SecurityConfig.java:74~75). 열쇠 확인이 없다.
- 내려받기 주소는 번호만 바꾸면 다른 파일이 열린다(OpportunityAttachmentDownloadController.java:37, RfpFileDownloadController.java:35). 샘플(rfp-files)은 과거 제안서다.

## 4. 정할 것

승인요청 [기록/승인요청_CR-144_열쇠_싣는_방법_20261011.md](기록/승인요청_CR-144_열쇠_싣는_방법_20261011.md)

## 5. 결정

컨타 10/11 12:28 [승인] (사용자 10:05 권장 위임)

| 순번 | 정할 것 | 결정 |
|------|--------|------|
| 1 | 열쇠 싣는 방법 | ①Aimbase 에 "등록에 머리글 싣기" 를 그 레포 CR 로 요청하고, 우리는 X-API-Key 머리글을 본다 |
| 2 | 내려받기 주소 | ①우리가 건네는 내려받기 주소에 기한 있는 서명을 붙인다 |
| 3 | 끊긴 통로 | ①서버 망 복구를 새 CR 로 먼저 연다. 운영 확인은 그 뒤 |

Aimbase 창과 망 복구 창은 컨타가 연다. 규칙 번호 BIZ-061.

## 6. 설계

- 규칙: T1-3 BIZ-061
- API: T3-2 「MCP 서버」 절의 엔드포인트 표(내려받기 2줄)와 「열쇠 확인」
- 시험: T3-5 「CR-144: Aimbase 통로 열쇠」 TC-MCPKEY-001~010
- 만들 것
  - 열쇠와 서명을 다루는 McpAccessGuard: 열쇠 비교(같은 시간 비교), 서명 주소 만들기, 서명 확인. 시계는 ClockConfig 의 Clock
  - /mcp, /mcp/* 에만 거는 거름망 McpAccessFilter(FilterRegistrationBean). 보안 설정의 /mcp permitAll 은 그대로 두고 주석만 고친다
  - 내려받기 주소를 만드는 두 곳(NoticeService 첨부, ReferenceSampleService 샘플)이 McpAccessGuard 로 서명 주소를 받는다
  - 설정: app.mcp.api-key(${MCP_API_KEY:}), app.mcp.download-url-ttl(${MCP_DOWNLOAD_URL_TTL:PT1H}). 로컬은 application-local.yml 에 로컬 전용 시험 열쇠
- 반영 순서: 승인요청 문서의 1~6

## 이슈

- 3.3 의 끊긴 통로는 이 CR 이 만든 것이 아니다. 열쇠를 켜기 전에 이미 공고 분석이 멈춰 있어, 운영 확인(공고 분석이 도는지)은 통로가 다시 열려야 할 수 있다.
