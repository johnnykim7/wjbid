# SAM.gov Bidding Agency Platform API 설계

> 설계 버전: 1.9 | 최종 수정: 2026-10-11 | 관련 CR: CR-001, CR-002, CR-003, CR-003a, CR-129, CR-131, CR-132, CR-135, CR-136, CR-137, CR-140

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
| POST | /auth/register | 이메일 회원가입 (CUSTOMER 역할). 이미 가입된 이메일은 409, `error: EMAIL_ALREADY_EXISTS` (CR-131, BIZ-008) | | BID-AUTH-001 |
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

## B-3. 공고문 관리 — Admin (Notice Admin) — CR-016

> 공고문 = 원본(Opportunity)에서 선별·한글화된 산출물. 베이스 경로 `/admin/notices` (context-path `/api` 포함 시 `/api/admin/notices`). 구현체: [NoticeAdminController](../backend/src/main/java/com/biddingagency/controller/admin/NoticeAdminController.java).

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /admin/notices | 공고문 목록 (한글화 상태, 노출 상태) | 🔒 ADMIN | BID-NOTICE |
| GET | /admin/notices/{id} | 공고문 상세 (한글화 결과) | 🔒 ADMIN | BID-NOTICE |
| POST | /admin/notices/{id}/publish | 노출 (검수 완료 → 고객 노출, 게이트②) | 🔒 ADMIN | BID-NOTICE |
| POST | /admin/notices/{id}/hide | 비노출 (VISIBLE → HIDDEN) | 🔒 ADMIN | BID-NOTICE |
| POST | /admin/notices/{id}/regenerate | 한글화 재생성 (markAnalyzing → 비동기 재실행). ANALYZING 중엔 FE에서 버튼 disabled | 🔒 ADMIN | BID-NOTICE |
| **POST** | **/admin/notices/{id}/cancel** | **강제 중단 (CR-039)** — ANALYZING만 허용. Aimbase 워크플로우 취소 best-effort 후 결과 무관 FAILED 전환. 비-ANALYZING은 무시(idempotent) | 🔒 ADMIN | BID-NOTICE |
| PATCH | /admin/notices/{id} | 한글화 결과 보정 (관리자 수동) | 🔒 ADMIN | BID-NOTICE |
| **DELETE** | **/admin/notices/{id}** | **공고문 삭제 (CR-040)** — hard delete. VISIBLE/ANALYZING이면 409 거부. 고아 VerificationLog 함께 제거, 원본 Opportunity 보존 | 🔒 ADMIN | BID-NOTICE |

**POST /admin/notices/{id}/cancel 응답 (CR-039)**:
```json
{ "status": "CANCELLED", "generationStatus": "FAILED", "noticeId": "..." }
```
> 강제 중단 후 generationStatus가 FAILED가 되므로 FE 재생성 버튼 disabled(=ANALYZING 조건)가 풀려 재시도 가능. 우리 쪽 상태는 Aimbase 응답과 무관하게 항상 즉시 FAILED (BIZ-021).
> **Aimbase 취소 연동 (CR-105)**: `POST /api/v1/workflows/runs/{runId}/cancel` 협조적 중지. `LLMPlatformClient.cancelWorkflowRun`은 @Async — cancel 후 응답이 running이면 `GET /workflows/runs/{runId}`를 2.5초 간격 폴링해 terminal 확정까지 백그라운드 대기(가이드 §4-7). 우리 화면 잠금 해제(즉시 FAILED)는 비동기라 폴링에 막히지 않음.
> **stuck 자동 정리**: `NoticeStuckCleanupScheduler`(@Scheduled)가 analysis_started_at 임계분(POL-012, 기본 60분) 초과 ANALYZING 건을 동일 cancel 경로로 자동 FAILED. API 호출 아님(백그라운드).

---

## B-4. 기지 관리와 지난 낙찰 다시 받기 (Admin, CR-136)

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /admin/bases | 기지 목록과 기지마다 지금 걸리는 공고번호 수(matchedCount) | 🔒 ADMIN | BID-MKT-010 |
| POST | /admin/bases | 기지 추가. `nameKo`, `nameEn`, `matchTerms[]`(하나 이상), `region?`, `latitude?`, `longitude?`, `sortOrder?` | 🔒 ADMIN | BID-MKT-010 |
| PUT | /admin/bases/{id} | 기지 고치기(같은 본문) | 🔒 ADMIN | BID-MKT-010 |
| DELETE | /admin/bases/{id} | 기지 지우기 | 🔒 ADMIN | BID-MKT-010 |
| GET | /admin/bases/unmatched | 어느 기지에도 걸리지 않은 공고(낙찰 공지 제외) 목록 줄과 건수, 최근 게시순 | 🔒 ADMIN | BID-MKT-010 |
| POST | /admin/opportunities/award-backfill | 지난 낙찰 공지 다시 받기. 본문 `postedFrom`, `postedTo`(YYYY-MM-DD, 한 해 안) | 🔒 ADMIN | BID-MKT-008 |

- 검증: 이름 비면 400, 영문 이름 겹치면 409, 찾을 낱말 0개 400, 위도·경도 한쪽만 400, 위도 -90~90·경도 -180~180 밖 400.
- 다시 받기 응답: `samCalls`(이번에 부른 SAM 검색 수), `fetched`(받은 낙찰 공지 수), `filled`(이미 있던 공지에 업체·금액을 채운 수), `created`(새로 넣은 수), `withoutAwardee`(업체 이름이 오지 않은 수). 기간이 한 해를 넘거나 시작이 끝보다 늦으면 400. SAM 이 한도 초과나 인증 실패를 주면 502 와 그 사유. 검색 조건은 발주처(POL-002)와 낙찰 공지 유형.

---

## C. 공고 열람 (Opportunity Browse) — Sprint 3

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /opportunities | 공고 목록 조회 (필터/검색/페이징). **사용자: visibility=VISIBLE만** (CR-003) | | BID-BROWSE-001 |
| GET | /opportunities/{id} | 공고 상세 조회 (사전 분석 결과 포함 — CR-003). 응답 `responseDeadline` 은 날짜만, `responseDeadlineAt` 은 서울 시각 표시가 붙은 마감 시각(CR-138) | | BID-BROWSE-002 |
| GET | /opportunities/{id}/requirements | 공고 요구사항 목록 (카테고리별) | | BID-BROWSE-003 |
| GET | /opportunities/{id}/attachments | 공고 첨부문서 목록 | | BID-OPP-003 |
| GET | /opportunities/search | 키워드 검색 | | BID-BROWSE-001 |
| GET | /opportunities/search/organization | 기관명 검색 | | BID-BROWSE-001 |
| GET | /opportunities/near-deadline | 마감 임박 공고 | | BID-BROWSE-001 |
| GET | /opportunities/recent | 최근 등록 공고 | | BID-BROWSE-001 |

---

## C-2. 모은 공고 목록과 집계 (CR-135)

메뉴 속 화면 2묶음(사전 공고, 변경 공고, 공고문 예시, 최근 낙찰, 월별 동향, 업종별 발주)이 쓴다. C 의 공개 공고문 조회와 달리 **모은 원본 공고 전부**(지운 것 제외)를 읽는다. 모두 로그인 없이 부른다(GET /opportunities/** 공개). 읽기만 한다.

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /opportunities/collected | 모은 공고 목록, 게시일 최신순, 쪽. 거르기: `group`(PRE 사전, BID 입찰, AWARD 낙찰, OTHER 그 밖), `type`(SAM 공고 유형), `month`(게시 달 YYYY-MM), `sector`(업종 대분류, 예 23, 31-33, NONE), `openOnly`(답변 기한 남은 것만), `latestOnly`(같은 공고번호는 마지막 판 하나만, 업종별 최근 공고에 씀). 같은 거르기에서 유형별 건수를 함께 준다 | | BID-MKT-001, 005, 006 |
| GET | /opportunities/amendments | 같은 공고번호로 두 번 이상 올라온 공고 묶음(낙찰 공지 제외), 마지막 게시일 최신순, 쪽. `openOnly`(마지막 판 마감 전만), `solicitationNumber`(그 공고번호 묶음만, 사전 공고의 "본 공고 나옴" 에서 씀). 묶음마다 판 목록. `openCount`(진행 중 묶음 수), `versionCount`(묶음에 든 판 수)를 함께 준다 | | BID-MKT-002 |
| GET | /opportunities/sample | 설정 `NOTICE_SAMPLE_ID` 의 공개 공고문. 응답 `notice`(공고 상세와 같은 공고문), `originalTitle`(원문 제목), `samUrl`(SAM.gov 공개 주소), `firstPostedDate`(같은 공고번호의 첫 게시일, 낙찰 공지 제외). 없으면 204 | | BID-MKT-003 |
| GET | /opportunities/awards | 낙찰 공지 목록, 쪽. `month`(공지 달), `sort`(LATEST 최신순, AMOUNT 금액순). 요약(건수, 금액 공개 건수, 합계, 최대)을 함께 준다 | | BID-MKT-004 |
| GET | /opportunities/stats/monthly | 첫 게시 달부터 이번 달까지 달마다 묶음 유형별 건수(빈 달 0 포함), 자료 기간 | | BID-MKT-005 |
| GET | /opportunities/stats/naics | 업종 대분류별 건수와 세부 코드별 건수, 업종 코드 없는 건수, 자료 기간 | | BID-MKT-006 |

**목록 줄 (collected, amendments 의 판, awards 공통)**

| 필드 | 설명 |
|------|------|
| id | 공고 id |
| noticeId | SAM 공고 ID |
| solicitationNumber | 공고번호 |
| title, titleKo | 원문 제목, 한글 제목(없으면 null) |
| type, typeKo, group | SAM 공고 유형, 한글 유형 이름, 묶음 유형 |
| postedDate, responseDeadline | 게시일, 답변 기한(없으면 null) |
| awardAmount | 낙찰 금액 표시 문자열(낙찰 공지만, 없으면 null) |
| awardeeName | 낙찰 업체 이름(낙찰 공지만). 업체 이름 칸이 생기기 전(CR-136 전)에는 늘 null |
| publicNoticeId | 같은 공고의 공개 한글 공고문 id(없으면 null). 있으면 화면이 공고 상세로 보낸다 |
| samUrl | SAM.gov 원문 공개 주소. 주소 형식은 설정 `sam-gov.public-opportunity-url` |
| hasMainNotice | (사전 공고만) 같은 공고번호로 입찰 공고가 올라왔는지 |
| originalId | (낙찰 공지만) 같은 공고번호의 원래 공고 중 마지막 판 id(없으면 null) |
| originalPublicNoticeId, originalSamUrl | (낙찰 공지만) 원래 공고의 공개 한글 공고문 id(없으면 null)와 SAM.gov 원문 공개 주소. 화면의 "원래 공고" 가 쓴다 |

**자료 기간**: 응답마다 `dataFrom`, `dataTo`(대상 공고의 첫·마지막 게시일)와 `total` 을 준다.

**순서**: 목록은 게시 시각 최신순이고, 같은 시각이면 DB 의 id 순(바이트 순)이다.

**낙찰 요약**: `total`, `amountKnownCount`, `amountSum`, `amountMax`, `awardeeKnownCount`(업체 이름 있는 공지 수, CR-136 전에는 0). 화면은 `awardeeKnownCount` 가 0 이면 업체 이름을 모으지 않는다고 밝히고, 1 이상이면 업체 칸을 보인다(CR-136 과 계약, 10/11).

**금액**: 합계·최대·금액순은 낙찰 금액 숫자 칸(`award_amount_usd`, CR-136)을 쓴다. 이미 모은 공지는 V44 가 금액 문자열(예 `$60,700.00`)을 읽어 채웠고, 이후는 수집이 SAM 금액 숫자로 채운다. 비면 금액 없음으로 센다.

**업종 대분류**: 원본 공고의 업종 코드 앞 두 자리. 31~33, 44~45, 48~49 는 한 대분류. 코드가 없으면 `NONE`. 한글 이름은 NAICS 공식 대분류 20개 전부를 둔다.

---

## C-3. 낙찰 업체와 기지별 집계 (CR-136)

메뉴 속 화면 3묶음(낙찰 업체, 기지별 지도)이 쓴다. C-2 와 같이 모은 원본 공고 전부(지운 것 제외)를 읽고, 로그인 없이 연다. 목록 줄은 C-2 「목록 줄」 과 같다. 숫자는 저장된 자료로만 센다(BIZ-028).

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /opportunities/awardees | 낙찰 업체 목록. `q`(업체 이름 일부, 대소문자 무시), `sort`(COUNT 건수순 기본, AMOUNT 금액순). 업체마다 낙찰 공지 목록을 함께 준다 | | BID-MKT-009 |
| GET | /opportunities/stats/bases | 기지별 공고 수·낙찰 수·마감 전 공고 수, 기지를 알 수 없는 공고 수, 자료 기간 | | BID-MKT-011 |
| GET | /opportunities/stats/bases/{baseId}/opportunities | 그 기지 공고 최근 게시순 `size`건(기본 10, 최대 50). 낙찰 공지 제외 | | BID-MKT-011 |

**낙찰 업체 응답**

| 필드 | 설명 |
|------|------|
| dataFrom, dataTo | 낙찰 공지의 첫·마지막 게시일 |
| totalAwards | 낙찰 공지 수 |
| awardsWithAwardee | 그중 업체 이름이 있는 수. 0 이면 화면은 빈 상태 |
| awardees[].key | 묶음 키. UEI 가 있으면 `UEI:<값>`, 없으면 `NAME:<정규화한 이름>` (BIZ-029) |
| awardees[].rank | 고른 정렬로 전체 업체 중 몇 번째인지. `q` 로 찾아도 바뀌지 않는다 |
| awardees[].name, uei | 표시 이름(가장 최근 공지의 이름), UEI(없으면 null) |
| awardees[].awardCount | 낙찰 공지 수 |
| awardees[].amountSum, amountCount | 금액 합계(달러, 금액이 있는 공지만 더함), 더한 공지 수 |
| awardees[].lastPostedDate | 가장 최근 공지일 |
| awardees[].awards[] | 목록 줄 + `awardAmountUsd`(숫자, 없으면 null). 최근 공지순 |

**기지별 집계 응답**

| 필드 | 설명 |
|------|------|
| dataFrom, dataTo | 대상 공고의 첫·마지막 게시일 |
| totalNumbers | 낙찰 공지를 뺀 공고의 공고번호 수(공고번호가 없으면 SAM 공고 ID 로 센다) |
| unmatchedNumbers | 그중 어느 기지에도 걸리지 않은 공고번호 수 |
| bases[] | id, nameKo, nameEn, region, latitude, longitude(없으면 null), opportunityCount(공고번호 수), awardCount(낙찰 공지 수), openCount(마감 전 공고번호 수, 마감 전 판정은 C 의 마감 전 공고와 같다). 기지 목록 순서대로. 기지가 없으면 빈 배열 |

기지 찾기는 BIZ-031. 한 공고가 여러 기지에 걸리면 각 기지에 세므로 기지별 합은 totalNumbers 보다 클 수 있다. 없는 baseId 는 404.

---

## D. 즐겨찾기 (Bookmark) — Sprint 3

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /bookmarks | 내 즐겨찾기 목록 (페이징) | 🔒 | BID-BROWSE-004 |
| GET | /bookmarks/{opportunityId}/status | 북마크 여부 확인 | 🔒 | BID-BROWSE-004 |
| POST | /bookmarks/{opportunityId} | 북마크 추가 | 🔒 | BID-BROWSE-004 |
| DELETE | /bookmarks/{opportunityId} | 북마크 삭제 | 🔒 | BID-BROWSE-004 |

---

## E. 자격 진단 (적격성 진단, 공고별 판정), Sprint 3, CR-137 에서 고침, CR-140 판정

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /eligibility/notices | 진단할 공고 목록. 공개이고 분석이 끝난 공고문 중 참가 자격이 1개 이상인 것. 마감 전(마감일 가까운 순) 다음 마감 지남(마감일 최근 순). CR-140: 줄마다 판정과 걸린 자격 | 🔒 | BID-QUAL-001, BID-MKT-015, BID-MKT-016, BID-MKT-019 |
| GET | /eligibility/notices/{noticeId} | 공고별 진단. 공고 머리, 자격 목록과 내 답, 개수 요약, 맞을 수 있는 서류함 서류. CR-140: 판정과 걸린 자격, 자격마다 붙인 서류 | 🔒 | BID-QUAL-001, BID-MKT-017, BID-MKT-019 |
| PUT | /eligibility/notices/{noticeId}/answers/{itemNo} | 자격 하나의 답 저장(있으면 바꿈). 본문 `{ "answer": "HAVE" \| "LACK" \| "UNSURE" }`. CR-140: 없음, 확인 필요로 바꾸거나 옛 답에 다시 답하면 붙인 서류를 푼다(BIZ-043) | 🔒 | BID-QUAL-001 |
| PUT | /eligibility/notices/{noticeId}/answers/{itemNo}/document | 있음으로 답한 자격에 증빙 서류 붙이기와 떼기(CR-140). 본문 `{ "documentId": "<내 서류 id>" \| null }` | 🔒 | BID-MKT-018 |

CR-137 전에 있던 `POST /opportunities/{id}/qualification-check`(AI 자격 사전 진단 요청)는 실제 진단 없이 접수됐다는 응답만 주던 것이라 지운다(BIZ-032).

**GET /eligibility/notices 응답 줄**:

| 필드 | 설명 |
|------|------|
| noticeId, solicitationNumber, koreanTitle | 공고문 id, 공고번호, 한글 제목 |
| responseDeadline | 마감 일시(저장된 값 그대로. 수집 때 시간대가 확인되지 않아 화면은 날짜만 보인다) |
| open | 마감 전이면 true |
| itemCount | 참가 자격 수 |
| answeredCount | 지금 자격 이름과 맞는 내 답 수(BIZ-033) |
| gateCount | 통과 조건 자격 수(CR-140) |
| verdict | 판정(CR-140, BIZ-040). ELIGIBLE 참여 가능, DOCS_MISSING 서류 부족, DIFFICULT 어려움, UNDECIDED 판정 전 |
| blockers[] | 판정을 정한 자격(CR-140, BIZ-044). itemNo, title, sourceRef, problem, documentName, expiresOn. problem 은 LACK 없음, UNSURE 확인 필요, UNANSWERED 답 안 함, STALE 다시 답해야 함, NO_DOCUMENT 서류 없음, EXPIRED 서류 만료. 어려움은 LACK 만, 판정 전은 UNSURE, UNANSWERED, STALE 만, 서류 부족은 NO_DOCUMENT, EXPIRED 만 담는다. 참여 가능과 통과 조건 0개는 빈 배열. documentName, expiresOn 은 EXPIRED 일 때만 |

**GET /eligibility/notices/{noticeId} 응답**:

| 필드 | 설명 |
|------|------|
| notice | noticeId, solicitationNumber, koreanTitle, organizationName, responseDeadline, open |
| items[] | itemNo, title, description, mandatory, gate(통과 조건), sourceRef(원문 위치), evidenceBy(증빙 서류 글), answer(HAVE, LACK, UNSURE, 답 안 함은 null), stale(옛 이름의 답이 있어 다시 답해야 하면 true), answeredAt(지금 답을 저장한 시각, 답이 없거나 stale 이면 null), vaultCategory(증빙 서류 글과 맞은 서류함 분류 코드. CR-140 부터 증빙 서류 글이 맞지 않으면 자격 이름으로 맞춤. 없으면 null), vaultCategoryName(그 분류 이름), vaultDocuments[](그 분류에 올린 내 서류의 id, 이름, 올린 날), attachedDocument(CR-140. 붙인 서류의 id, name, expiresOn, expired. 붙인 서류가 없거나, 서류함에 없거나, 지금 답이 있음이 아니면 null) |
| verdict, blockers[] | 목록 줄과 같다(CR-140) |
| summary | gateCount(통과 조건 자격 수), have, lack, unsure, unanswered(통과 조건 자격 기준). allCount(전체 자격 수) |

- 404 `ELIGIBILITY_NOTICE_NOT_FOUND`: 공고문이 없거나 숨김이거나 분석이 끝나지 않았거나 참가 자격이 0개일 때. 공고문 id 가 UUID 모양이 아니면 400. 비로그인 401.
- PUT 400 `ELIGIBILITY_ANSWER_INVALID`: answer 가 셋 중 하나가 아닐 때. 400 `ELIGIBILITY_ITEM_INVALID`: itemNo 가 자격 목록 범위 밖일 때. 오류 본문은 `{ error, message }`. 저장 응답은 그 자격 한 줄(items[] 와 같은 모양)과 summary, CR-140 부터 verdict, blockers[].
- 서류 붙이기 PUT(CR-140): 400 `ELIGIBILITY_DOCUMENT_NOT_ALLOWED` 그 자격의 지금 답이 "있음" 이 아닐 때(답 없음, 옛 답 포함). 404 `ELIGIBILITY_DOCUMENT_NOT_FOUND` 서류가 없거나 남의 서류일 때. 그 밖은 답 저장과 같다. 응답은 답 저장 응답과 같은 모양이다. 고를 서류 목록은 `GET /member-documents`(N절)를 쓴다.

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

## L. 요금 안내 (Pricing) — Sprint 7, CR-129

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /pricing | 회원 월 금액 조회 | | BID-PRICE-001 |

**응답** (CR-129)

```json
{ "memberMonthlyPrice": 50000 }
```

| 필드 | 타입 | 설명 |
|------|------|------|
| memberMonthlyPrice | number 또는 null | 회원 월 요금(원, 부가세 별도). 설정 `PRICING_MEMBER_MONTHLY_KRW` 가 비었거나 정수로 읽을 수 없으면 null |

> 단계 이름과 포함 항목은 화면 정의 파일에 두고, 바뀌는 금액만 이 API 가 내준다(POL-010). 고객 포털의 요금 안내와 홈 요금 구역이 같이 쓴다.

---

## Q. 고객센터 1:1 문의 (CR-132)

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /inquiries/types | 문의 유형 목록(사용 중인 것만, 순서대로) | | BID-SUPPORT-001 |
| POST | /inquiries | 문의 등록. 로그인 토큰이 있으면 회원과 묶는다 | 선택 | BID-SUPPORT-001 |
| GET | /admin/inquiries | 문의 목록. `status`(RECEIVED, ANSWERED, 없으면 전체), `typeCode`, `page`, `size`. 최근 접수가 위 | 🔒 ADMIN | BID-SUPPORT-003 |
| GET | /admin/inquiries/counts | 상태별 건수(목록 거르기, 메뉴 미답변 숫자) | 🔒 ADMIN | BID-SUPPORT-003 |
| GET | /admin/inquiries/{id} | 문의 상세(관리자 알림 결과, 답변 포함) | 🔒 ADMIN | BID-SUPPORT-003 |
| POST | /admin/inquiries/{id}/answer | 답변 보내기. 고객 메일 발송이 성공하면 답변완료(BIZ-024) | 🔒 ADMIN | BID-SUPPORT-003 |

**GET /inquiries/types 응답**

```json
[{ "code": "SERVICE", "label": "서비스 이용 문의" }, { "code": "CONSULTING", "label": "서비스 상담" }]
```

**POST /inquiries 요청 / 응답(201)**

```json
{ "typeCode": "CONSULTING", "title": "등록 대행 상담 신청", "content": "...", "name": "홍길동", "email": "you@example.com" }
```

```json
{ "id": "6f1c...", "status": "RECEIVED", "createdAt": "2026-10-11T09:42:10" }
```

| 필드 | 제약 |
|------|------|
| typeCode | 필수. 사용 중인 유형 코드 |
| title | 필수, 200자 이하 |
| content | 필수, 5000자 이하 |
| name | 필수, 100자 이하 |
| email | 필수, 이메일 형식, 255자 이하 |

**GET /admin/inquiries 응답**: `Page<T>`, 항목

```json
{ "id": "6f1c...", "createdAt": "2026-10-11T09:42:10", "typeCode": "SAM_REGISTRATION", "typeLabel": "SAM.gov 등록 문의",
  "title": "...", "name": "박서연", "email": "...", "member": false, "status": "RECEIVED" }
```

**GET /admin/inquiries/counts 응답**

```json
{ "all": 24, "received": 3, "answered": 21 }
```

**GET /admin/inquiries/{id} 응답**

```json
{ "id": "6f1c...", "typeCode": "PAYMENT", "typeLabel": "결제·환불 문의", "title": "...", "content": "...",
  "name": "이도윤", "email": "...", "memberId": "a1b2...", "memberCompanyName": "한빛시설관리",
  "status": "ANSWERED", "createdAt": "2026-10-11T08:15:02",
  "adminNotice": { "total": 2, "success": 2, "lastSentAt": "2026-10-11T08:15:03" },
  "answer": "...", "answeredAt": "2026-10-11T10:20:44", "answeredByEmail": "admin@..." }
```

- `adminNotice` 는 알림 이력(reference_type=Inquiry)에서 센다. 보낸 적이 없으면 total 0.
- 비로그인 문의는 `memberId`, `memberCompanyName` 이 null. 미답변이면 답변 세 칸이 null.

**POST /admin/inquiries/{id}/answer 요청 / 응답(200)**

```json
{ "answer": "..." }
```

응답은 상세와 같은 모양(답변완료로 바뀐 뒤).

**오류**

| 상황 | 상태 | `error` |
|------|------|---------|
| 칸 검사 실패(필수 빠짐, 길이 넘침, 이메일 형식) | 400 | (Spring 검사 오류) |
| 없거나 사용하지 않는 유형 코드 | 400 | INQUIRY_TYPE_INVALID |
| 없는 문의 | 404 | INQUIRY_NOT_FOUND |
| 답변완료 문의에 다시 답변 | 409 | INQUIRY_ALREADY_ANSWERED |
| 답변 본문이 비었거나 5000자 넘음 | 400 | (Spring 검사 오류) |
| 고객 답변 메일 발송 실패 | 502 | INQUIRY_ANSWER_MAIL_FAILED (상태는 미답변 그대로) |

오류 본문은 `{ "error": "<코드>", "message": "<설명>" }` (CR-131 이메일 중복과 같은 모양).

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
| C-2. 모은 공고 목록과 집계 | 6 | CR-135 |
| C-3. 낙찰 업체와 기지별 집계 | 3 | CR-136 |
| B-4. 기지 관리와 지난 낙찰 다시 받기 (Admin) | 6 | CR-136 |
| N. 회사 공통 서류함 | 7 | CR-125, CR-136 |
| D. 즐겨찾기 | 4 | 3 |
| E. 자격 진단 | 4 | 3, CR-137, CR-140 |
| F. 입찰 접수 | 7 | 4 |
| G. 고객 문서 제출 | 3 | 4 |
| H. 입찰 관리 (Admin) | 6 | 4 |
| I. 문서 템플릿 (Admin) | 5 | 6 |
| J. 문서 편집 | 7 | 7 |
| K. 회원 관리 (Admin) | 1 | — |
| L. 요금 안내 | 1 | 7 |
| Q. 고객센터 1:1 문의 | 6 | CR-132 |
| M. FlowGuard 연동 | 5 | — (CR-001) |
| B-2. 공고 관리 (Admin) | 8 | CR-003 |
| P. 성공 제안서 패턴 (Admin) | 13 | CR-013 |
| MCP Tools | 13 | 5, CR-003, CR-013 |
| MCP SSE 엔드포인트 | 2 | CR-002 |
| Aimbase 워크플로우 호출 | 2 | CR-002 |
| **합계** | **REST 108 + MCP 13 + SSE 2 + Aimbase 2** | |

---

## N. 회사 공통 서류함 (CR-125)

> member 종속 공통 서류 보관 + 제안서 슬롯 자동채움. 인증=본인 member(CUSTOMER).

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/member-documents` | 내 서류함 — 고정 카테고리 6슬롯 + 기타 목록(MemberDocumentVaultResponse) |
| POST | `/member-documents` (multipart) | 업로드 — `file`, `category`, `displayName?`, `description?`(기타) |
| PUT | `/member-documents/{id}` (multipart) | 교체(파일 재업로드, displayName/description 갱신) |
| DELETE | `/member-documents/{id}` | 삭제(StorageService 파일도 삭제) |
| POST | `/bid-requests/{bidRequestId}/required-document-slots/auto-fill` | 서류함에서 같은 카테고리 서류를 미충족 슬롯에 일괄 자동매칭(CR-125 2단계) |
| GET | `/member-documents/expiry` | 서류별 만료 상태(CR-136). 응답: `alertDays[]`(POL-016, 꺼져 있으면 빈 배열), `soonDays`(가장 큰 알림 날, 꺼져 있으면 null), `today`(Asia/Seoul 날짜), `summary`(expired, soon, noDate, total), `documents[]`(id, category, categoryLabel, displayName, fileName, expiresOn, daysLeft, status, lastAlertedAt) |
| PATCH | `/member-documents/{id}/expiry` | 만료일만 넣기·고치기·지우기(CR-136). 본문 `{"expiresOn": "YYYY-MM-DD" 또는 null}`. 응답 MemberDocumentDto |

- 응답 DTO: `MemberDocumentDto`(LAZY 폭발 방지) / `MemberDocumentVaultResponse`(fixedSlots[] + others[]).
- 소유 검증: 타 member 문서 접근 시 403/404. auto-fill은 미충족(PENDING/MISSING) 슬롯만 대상, 이미 충족 슬롯 미덮어씀.
- CR-136: POST·PUT 에 `expiresOn?`(YYYY-MM-DD) 를 더하고 MemberDocumentDto 에 `expiresOn` 을 싣는다. 날짜 형식이 틀리면 400.
- CR-136 만료 상태(ExpiryStatus): `EXPIRED`(만료일이 오늘보다 앞), `SOON`(남은 날 0 이상 soonDays 이하), `OK`(그보다 많이 남음, 알림이 꺼져 있으면 0 이상 전부), `NONE`(만료일 없음). daysLeft = 만료일 - 오늘(Asia/Seoul). lastAlertedAt 은 이 서류의 알림 이력(DOCUMENT_EXPIRY) 중 보내기에 성공한 마지막 시각.
