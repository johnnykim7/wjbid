# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# 프로젝트 개요

SAM.gov Bidding Agency Platform - 미군(USFK) 정부 조달 입찰 AI 대행 서비스
프로젝트 유형: 풀스택 (BE + FE × 2)

## 빌드/실행 명령어

### 백엔드
```bash
cd backend
./gradlew compileJava          # 컴파일
./gradlew test                  # 전체 테스트
./gradlew test --tests "*.BidFSMServiceTest"  # 단일 테스트 클래스
SERVER_PORT=8183 ./gradlew bootRun             # 앱 기동 (포트 8183, context-path /api)
```

### 프론트엔드
```bash
cd frontend/customer-portal && npm install && npm run dev   # 고객 포털 (:3183)
cd frontend/admin-console && npm install && npm run dev     # 관리자 콘솔 (:3184)
```

### 인프라 의존성
- MariaDB: `59.8.160.12:3306` (DB: bidding_agency) — 별도 Docker 불필요
- Redis: `59.8.160.12:6379`
- Aimbase: `59.8.160.12:8280` (테넌트: bidding_system)
- 서버 전체 이전: 14.63.25.49 → 59.8.160.12 (2026-05-28)

## 기술 스택

### 백엔드
- 프레임워크: Java 17 + Spring Boot 3.2
- DB: MariaDB 11.2
- 캐시/큐: Redis 7.2
- 인증: JWT (HS256) — Access 15min + Refresh 7day
- ORM: Spring Data JPA + Hibernate
- 빌드: Gradle 8.5
- 파일 스토리지: MinIO (S3 호환)
- 이메일: Spring Boot Starter Mail + Thymeleaf 템플릿
- PDF: iText 8.0.2 / Office: Apache POI 5.2.5
- API 문서: SpringDoc OpenAPI
- DB 마이그레이션: Flyway

### AI 연동 (Aimbase) — CR-002

**아키텍처**: 이 플랫폼은 MCP 서버 + Aimbase 워크플로우 클라이언트 이중 역할
- **MCP 서버**: Aimbase가 이 플랫폼의 8개 Tool을 SSE로 호출 (HTTP POST도 지원)
- **워크플로우 클라이언트**: `LLMPlatformClient`가 Aimbase Workflow API 호출 + 폴링
- **인증**: `X-API-Key: plat-20cf57fbc623424584eeda2e355cbb43`
- **MCP 전송**: HTTP POST (`/mcp`) + SSE (`/mcp/sse` + `/mcp/message`)
- **핵심 흐름**: FSM 상태 전이 → AIWorkflowService → LLMPlatformClient → Aimbase 워크플로우 → Aimbase가 MCP 콜백으로 데이터 저장
- **문서 출력(Word/PDF)**: Aimbase에서 처리. 이 플랫폼은 검수용 PDF export만 보유
- **RAG**: MVP 미사용. 향후 Aimbase Knowledge Source로 확장 가능

### 프론트엔드 (customer-portal + admin-console)
- 프레임워크: React 19 + TypeScript 5
- 빌드: Vite 7
- 스타일: TailwindCSS 4.1
- UI: Radix UI + Lucide Icons
- HTTP: Axios
- 테이블: TanStack React Table
- 라우팅: React Router v7 (portal) / v6 (admin)
- 상태관리: Context API (AuthContext)
- 에디터: TipTap (문서 편집)

## BE 아키텍처 규칙

### 폴더 구조
```
backend/src/main/java/com/biddingagency/
├── config/              # 설정 (Security, Redis, MinIO, MCP 등)
├── controller/          # REST Controller (요청/응답만)
├── domain/
│   ├── bid/             # 입찰 요청 (BidRequest, FSM)
│   ├── bookmark/        # 즐겨찾기
│   ├── compliance/      # 컴플라이언스 검증
│   ├── document/        # 문서 생성/관리 (BidDocument, Template)
│   ├── member/          # 회원 (ADMIN, CUSTOMER)
│   └── opportunity/     # 공고 수집/조회
├── dto/                 # Request/Response DTO
├── integration/         # 외부 연동 (SAM.gov, 이메일)
├── mcp/                 # MCP 서버 Tool 구현
├── security/            # JWT, Spring Security
└── common/              # 공통 유틸
```

### 규칙
- **로직은 Service에만** — Controller는 요청 수신 + 응답 반환만
- **ApiResponse<T> 래퍼 사용** — 모든 API 응답은 통일된 래퍼
- **도메인별 패키지 구조** — domain/{name}/entity, service, repository
- **FSM 전이는 화이트리스트 방식** — 허용 전이 목록에 없으면 거부
- **문서 버전 불변성** — 생성된 버전은 수정 불가, 새 버전 생성
- **MCP Tool은 무상태** — 각 호출 독립 처리

## FE 아키텍처 규칙

### 폴더 구조
```
frontend/{app}/src/
├── components/          # 재사용 컴포넌트
├── pages/               # 페이지 컴포넌트
├── contexts/            # Context Provider (Auth 등)
├── services/            # API 호출 서비스
├── types/               # TypeScript 타입 정의
├── hooks/               # 커스텀 훅
└── utils/               # 유틸리티 함수
```

### 규칙
- **customer-portal** (:5173) — 고객용, React Router v7
- **admin-console** (:5174) — 관리자용, React Router v6
- **API 호출은 services/ 에 집중** — 컴포넌트에서 직접 Axios 호출 금지
- **인증 상태는 AuthContext** — JWT 토큰 관리, 자동 갱신
- **TailwindCSS 유틸리티 클래스** — CSS 파일 최소화

## 네이밍 규칙

- 엔티티: PascalCase (`BidRequest`) / 테이블: snake_case (`bid_request`)
- API: kebab-case (`/api/bid-requests`) / 변수: camelCase
- FE 파일: kebab-case (`bid-request-list.tsx`) / 컴포넌트: PascalCase (`BidRequestList`)
- Enum: UPPER_SNAKE_CASE (`DOCS_PENDING`)
- 기능 ID: `BID-{MODULE}-{NNN}` (예: BID-OPP-001)

## 핵심 비즈니스 규칙

- BIZ-001: FSM 화이트리스트 — 허용 전이 외 전면 차단
- BIZ-002: 문서 버전 불변성 — 변경 시 새 버전 생성
- BIZ-003: LOCKED 문서 편집 불가
- BIZ-004: SAM.gov 원본 JSON 보존
- BIZ-005: 콘텐츠 해시 중복 검출
- BIZ-006: 동일 공고 중복 신청 불가
- BIZ-007: BLOCKER 미충족 시 제출 차단
- BIZ-008: 이메일 유일성
- BIZ-012: 알림 중복 발송 방지 (멱등성)
- BIZ-013: MCP 응답 무상태

## 테스트 전략

> 테스트 코드 작성 시 반드시 이 전략을 따른다.
> 개별 테스트 케이스 목록은 docs/T3-5_단위테스트_명세.md 참조.

### 테스트 범위

| 레이어 | 필수 여부 | 테스트 대상 | 파일 패턴 |
|--------|----------|------------|----------|
| Service | 필수 | 비즈니스 로직, FSM 전이, 이벤트 발행 | {Name}ServiceTest.java |
| Repository | 선택 | 복잡한 쿼리만 | {Name}RepositoryTest.java |
| Controller | 선택 | 통합테스트로 대체 가능 | {Name}ControllerTest.java |
| MCP Tool | 필수 | Tool 호출 응답 검증 | {Name}McpToolTest.java |
| FE Component | 선택 | 주요 인터랙션만 | {name}.test.tsx |

### 모킹 전략

- SAM.gov API: **전부 Mock** — 실제 호출 금지
- Aimbase / MCP: Mock 또는 테스트 서버
- DB: H2 In-memory (테스트), MariaDB (통합)
- MinIO: Mock 또는 테스트 버킷
- 이메일(SMTP): Mock — 실제 발송 금지
- 시간 의존 로직: Clock Mock 사용

### 커버리지 기준

- Service 레이어: 80% 이상 (라인 기준)
- 전체: Service 필수, 나머지 권장

### 테스트 작성 원칙

- 하나의 테스트는 하나의 행동만 검증 (단일 assert 원칙)
- 테스트 이름: `상황_행동_기대결과` (예: `미결제_24시간경과_자동취소됨`)
- FSM 전이: 허용 전이 + 금지 전이 모두 검증
- 이벤트: 이벤트 발행 여부 + 페이로드 검증
- 정책: 정책값 변경 시 동작 변경 검증

## Git 브랜치 전략

### 브랜치 구조

| 브랜치 | 역할 | 보호 |
|--------|------|------|
| `main` | 운영 배포 가능 상태 (항상 안정) | PR 머지만 허용 |
| `develop` | 개발 통합 브랜치 | PR 머지 권장 |
| `feat/SPR-{N}-{기능명}` | Sprint + 기능 단위 작업 | 작업 완료 후 develop에 PR |

### 흐름

```
feat/SPR-01-auth → develop (PR) → main (릴리스 PR) + tag v1.0
```

### AI-SDLC 단계별 git 행위

| 시점 | 브랜치 | 행위 |
|------|--------|------|
| T0~T3 설계 | `develop` | `docs/` 설계 문서 커밋 |
| Sprint 구현 시작 | `feat/SPR-01-xxx` | develop에서 분기 |
| Sprint 구현 완료 | `develop` | feat 브랜치 PR 머지 |
| T4 검증 통과 | `main` | develop → main 릴리스 PR + 태그 |

### 커밋 메시지 형식

`<type>: <설명>` — type: feat / fix / refactor / docs / chore / ui

## 현재 진행 상태

- Sprint 1~7: 코드 구현 완료 (초기 커밋에 포함)
- Sprint 8 (통합 테스트): 진행중
- CR-001: FlowGuard 연동 완료
- CR-002: Aimbase 연동 코드 완료. E2E 검증 대기 (네트워크: 로컬↔외부서버)
- CR-003: 공고 사전 분석 파이프라인 + LLM 입력 3파이프라인 — **설계 완료**, 구현 대기

## 참조 문서

- 실행스펙: ./docs/execution-spec.md
- 단위테스트 명세: ./docs/T3-5_단위테스트_명세.md
- 기능요구사항: ./docs/T1-1_기능요구사항_명세서.md
- FSM 상태: ./docs/T1-5_FSM_상태_정의.md
- 이벤트 계약: ./docs/T1-6_이벤트_계약.md
- 기술스택: ./docs/T2-1_기술스택_결정서.md

## 변경 규모 판단 및 설계 우선 원칙

> **Claude Code는 코드 수정 전에 반드시 변경 규모를 판단하고, 중규모 이상이면 설계 문서를 먼저 갱신해야 한다.**

### 규모 판단 기준

| 규모 | 기준 | 예시 |
|------|------|------|
| **소규모** | 단일 파일·함수 수준, 기존 설계 범위 이내 | 버그 수정, 필드 추가, 메시지 변경, 단순 UI 수정 |
| **중규모** | 모듈 단위, 새 API·화면·테이블 추가 | 신규 기능 1~2개, 기존 모듈 확장, 화면 구조 변경 |
| **대규모** | 아키텍처·데이터 모델·이벤트 계약에 영향 | 모듈 신설, 외부 시스템 연동 추가, 구조 변경 |

### 규모별 필수 절차

**소규모:**
1. Plan Mode → 구현 → 테스트
2. CR_변경_이력에 CR 기록
3. git commit

**중규모 이상 (화면·API·데이터 모델 변경 포함):**
1. **구현 중단** — 코드 수정을 시작하지 않는다
2. **사용자에게 규모 판단 결과를 고지**하고, 설계 우선 절차를 안내한다
3. **설계 캐스케이드 수행** (T1 → T2 → T3 → T4 → execution-spec.md 순서)
4. **CR_변경_이력에 CR 기록** + 수정된 설계서의 B-lite 메타데이터 갱신
5. 설계 문서 변경분을 **먼저 commit** 한다
6. 그 다음 코드 구현을 진행한다

### 금지 사항

- **설계 문서 갱신 없이 중규모 이상 코드 변경 금지**
- T3(API·화면)만 단독 수정 금지 — 반드시 T1부터 캐스케이드
- 사용자 승인 없이 중규모 이상 변경 착수 금지

## 원본 요구사항 관리 (docs/origins/)

원본 요구사항은 `docs/origins/` 폴더에 **정제하지 않고 있는 그대로** 보관한다.

- **대화창에 텍스트로 입력된 기획 내용은 전문(全文)을 그대로 복사하여 파일로 생성한다.**
- 기존 원본 파일은 **수정하지 않는다** — 변경분은 항상 별도 파일로 남긴다.
- 추가 요구사항 처리 시 `CR_변경_이력`에서 해당 origins 파일을 참조한다.

## Claude Code Hooks

> `.claude/hooks/enforce-workflow.sh` — 워크플로우 강제화 hook
> `.claude/settings.json` — hook 설정

## 주의사항

(구현 중 발견된 주의점 누적)
- SAM.gov API는 rate limit 있음 — 수집 간격 조절 필요
- JWT HS256 키는 application.yml의 jwt.secret — 운영 시 환경변수로 교체
- MinIO 접속 정보는 Docker Compose 환경 기준
- Aimbase MCP 연동 시 JSON-RPC 2.0 스펙 엄수 — requestId 멱등 키로 활용
- 포트 8080이 사용 중이면 `SERVER_PORT=8088`로 기동
- Flyway 마이그레이션: V7에서 `SET FOREIGN_KEY_CHECKS = 0` 사용 (FK 제약 우회)
- Aimbase Connection 생성 시 필드명: `adapter` (not `provider`), `type: "llm"` (소문자)
- Aimbase → 로컬 MCP SSE 접근: 사설 IP 불가. 배포 환경에서 테스트 필요
