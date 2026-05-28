# SAM.gov Bidding Agency Platform FSM 상태 정의

> 설계 버전: 1.1 | 최종 수정: 2026-03-28 | 관련 CR: CR-003

> 단계: 1. Requirements | 설계
>
> 모든 엔티티의 상태 흐름을 여기서 정의한다. 구현 시 상태 전이 검증의 기준이 된다.

## 전체 요약

| 엔티티 | 상태 코드 목록 |
|--------|--------------|
| Opportunity (공고 노출) | HIDDEN → VISIBLE (CR-003) |
| OpportunityAnalysis (공고 분석) | PENDING → ANALYZING → COMPLETED / FAILED (CR-003) |
| BidRequest (입찰 요청) | CREATED → DOCS_PENDING → DOCS_RECEIVED → ANALYZING → GENERATING → REVIEW → CONFIRMED → SUBMITTED / CLOSED |
| BidDocument (입찰 문서) | DRAFT → LOCKED → ARCHIVED |

---

## Opportunity 노출 상태 (CR-003)

```mermaid
stateDiagram-v2
    [*] --> HIDDEN : 공고 수집
    HIDDEN --> VISIBLE : 관리자 승인 (사전 분석 COMPLETED 필수)
    VISIBLE --> HIDDEN : 관리자 비노출 처리
```

### HIDDEN | 비노출
- **설명**: 수집된 공고가 아직 사용자에게 노출되지 않은 상태. 사전 분석 미완료 또는 관리자 미승인
- **진입 조건**: 공고 수집 시 기본값
- **허용 다음 상태**: VISIBLE
- **관련 기능 ID**: BID-OPP-008

### VISIBLE | 노출
- **설명**: 관리자가 사전 분석 결과를 검수 후 승인하여 사용자에게 노출되는 상태
- **진입 조건**: OpportunityAnalysis가 COMPLETED 상태 + 관리자 승인
- **허용 다음 상태**: HIDDEN (비노출 복귀 가능)
- **관련 기능 ID**: BID-OPP-008, BID-BROWSE-001

---

## OpportunityAnalysis 분석 상태 (CR-003)

```mermaid
stateDiagram-v2
    [*] --> PENDING : 첨부파일 다운로드 완료 또는 관리자 수동 업로드
    PENDING --> ANALYZING : LLM 분석 시작
    ANALYZING --> COMPLETED : 분석 성공
    ANALYZING --> FAILED : 분석 실패
    FAILED --> PENDING : 재시도
    COMPLETED --> PENDING : 재분석 요청
```

### PENDING | 분석 대기
- **설명**: 첨부파일이 준비되었으나 아직 LLM 분석이 시작되지 않은 상태
- **진입 조건**: AttachmentDownloaded 이벤트 수신 또는 관리자 수동 업로드 완료
- **허용 다음 상태**: ANALYZING
- **관련 기능 ID**: BID-OPP-006

### ANALYZING | 분석 중
- **설명**: Aimbase 워크플로우가 첨부파일을 분석하고 있는 상태
- **진입 조건**: 분석 워크플로우 트리거
- **허용 다음 상태**: COMPLETED, FAILED
- **관련 기능 ID**: BID-OPP-006
- **비고**: Aimbase Workflow 비동기 실행. 결과는 MCP 콜백으로 저장

### COMPLETED | 분석 완료
- **설명**: LLM 분석이 성공적으로 완료되어 요약/양식/필요서류/프롬프트 프리셋이 저장된 상태
- **진입 조건**: Aimbase 워크플로우 성공 완료
- **허용 다음 상태**: PENDING (재분석 필요시)
- **관련 기능 ID**: BID-OPP-006, BID-OPP-008
- **비고**: 관리자가 분석 결과를 검수하고 승인하면 Opportunity가 VISIBLE로 전이

### FAILED | 분석 실패
- **설명**: LLM 분석이 실패한 상태
- **진입 조건**: Aimbase 워크플로우 실패
- **허용 다음 상태**: PENDING (재시도)
- **관련 기능 ID**: BID-OPP-006
- **비고**: 관리자에게 실패 알림. 수동 재시도 가능

---

## BidRequest (입찰 요청)

```mermaid
stateDiagram-v2
    [*] --> CREATED : 입찰 참여 신청
    CREATED --> DOCS_PENDING : 필요 문서 안내
    DOCS_PENDING --> DOCS_RECEIVED : 고객 문서 제출 완료
    DOCS_RECEIVED --> ANALYZING : AI 요구사항 분석 시작
    ANALYZING --> GENERATING : 분석 완료 → 문서 생성 시작
    GENERATING --> REVIEW : AI 문서 생성 완료
    REVIEW --> CONFIRMED : 관리자 검토/편집 완료
    CONFIRMED --> SUBMITTED : 제출 완료

    CREATED --> CLOSED : 취소
    DOCS_PENDING --> CLOSED : 취소
    DOCS_RECEIVED --> CLOSED : 취소
    REVIEW --> GENERATING : 재생성 요청
    CONFIRMED --> REVIEW : 수정 필요
```

### CREATED | 신청 접수
- **설명**: 고객이 공고에 대해 입찰 참여 의사를 밝힌 초기 상태
- **진입 조건**: 고객이 공고 상세에서 "입찰 참여" 클릭 + 서비스 레벨 선택
- **허용 다음 상태**: DOCS_PENDING, CLOSED
- **관련 기능 ID**: BID-REQ-001

### DOCS_PENDING | 문서 대기
- **설명**: 고객에게 필요 문서 안내가 전달되어 문서 제출을 기다리는 상태
- **진입 조건**: 관리자가 필요 문서 체크리스트를 확인/보완 후 고객에게 안내
- **허용 다음 상태**: DOCS_RECEIVED, CLOSED
- **DOCS_RECEIVED 전이 가드 (CR-010, BIZ-015)**: `ComplianceService.validateClientDocumentSlots(bidRequestId)` 호출 → 공고의 모든 BLOCKER 요구사항 슬롯이 `RequirementFulfillmentMap` 으로 충족(`ClientDocument` 또는 `DocumentSection`/`Attachment`)된 경우에만 전이 허용. 미충족 시 전이 차단 + 미충족 슬롯 목록 응답
- **관련 기능 ID**: BID-REQ-002, BID-BROWSE-003

### DOCS_RECEIVED | 문서 접수 완료
- **설명**: 고객이 필요 문서를 모두 제출한 상태
- **진입 조건**: 모든 BLOCKER 요구사항 슬롯 충족 (CR-010 게이트 통과)
- **허용 다음 상태**: ANALYZING, CLOSED
- **관련 기능 ID**: BID-REQ-002

### ANALYZING | 분석 중
- **설명**: AI(Aimbase)가 공고 요구사항을 분석하는 상태
- **진입 조건**: 관리자 승인 또는 자동 트리거 (문서 접수 완료 시)
- **허용 다음 상태**: GENERATING
- **관련 기능 ID**: BID-DOC-001
- **비고**: Aimbase Workflow 비동기 실행. 실패 시 관리자에게 알림 후 ANALYZING에 머뭄

### GENERATING | 문서 생성 중
- **설명**: AI(Aimbase)가 제안서를 생성하는 상태
- **진입 조건**: 요구사항 분석 완료
- **허용 다음 상태**: REVIEW
- **관련 기능 ID**: BID-DOC-001
- **비고**: 생성 완료 시 관리자에게 이메일 알림 (BID-DOC-002)

### REVIEW | 관리자 검토
- **설명**: AI가 생성한 문서를 관리자가 검토/편집하는 상태
- **진입 조건**: AI 문서 생성 완료
- **허용 다음 상태**: CONFIRMED, GENERATING
- **관련 기능 ID**: BID-EDIT-001
- **비고**: 재생성이 필요하면 GENERATING으로 복귀 가능

### CONFIRMED | 확정
- **설명**: 관리자가 최종 확인하여 문서가 확정된 상태. 문서 LOCKED 처리
- **진입 조건**: 관리자가 "확정" 처리 + 문서 잠금
- **허용 다음 상태**: SUBMITTED, REVIEW
- **관련 기능 ID**: BID-EDIT-002
- **비고**: 수정이 필요하면 REVIEW로 복귀 가능 (문서 잠금 해제 필요)

### SUBMITTED | 제출 완료
- **설명**: 입찰 서류가 SAM.gov에 제출된 최종 상태
- **진입 조건**: 관리자가 제출 실행
- **허용 다음 상태**: (최종 상태)
- **관련 기능 ID**: BID-FSM-001

### CLOSED | 종료
- **설명**: 입찰이 취소/만료/기타 사유로 종료된 상태
- **진입 조건**: 고객 취소 요청 또는 관리자 종료 처리 또는 공고 마감 초과
- **허용 다음 상태**: (최종 상태)
- **관련 기능 ID**: BID-FSM-001

---

## BidDocument (입찰 문서)

```mermaid
stateDiagram-v2
    [*] --> DRAFT : AI 생성 또는 수동 생성
    DRAFT --> LOCKED : 관리자 확정
    LOCKED --> ARCHIVED : 제출 후 보관
    LOCKED --> DRAFT : 잠금 해제 (수정 필요)
```

### DRAFT | 초안
- **설명**: 문서가 편집 가능한 상태
- **진입 조건**: AI 생성 완료 또는 관리자 수동 생성
- **허용 다음 상태**: LOCKED
- **관련 기능 ID**: BID-DOC-001, BID-EDIT-001

### LOCKED | 잠금
- **설명**: 문서가 확정되어 편집 불가 상태
- **진입 조건**: 관리자가 "확정" 처리
- **허용 다음 상태**: ARCHIVED, DRAFT
- **관련 기능 ID**: BID-EDIT-002
- **비고**: 수정 필요 시 DRAFT로 복귀 가능 (BidRequest도 REVIEW로 복귀)

### ARCHIVED | 보관
- **설명**: 제출 완료 후 증거 보존 목적으로 보관
- **진입 조건**: BidRequest가 SUBMITTED로 전이
- **허용 다음 상태**: (최종 상태)
- **관련 기능 ID**: BID-FSM-001
