# SAM.gov Bidding Agency Platform API 설계

> 설계 버전: 1.16 | 최종 수정: 2026-10-12 | 관련 CR: CR-001, CR-002, CR-003, CR-003a, CR-129, CR-131, CR-132, CR-135, CR-136, CR-137, CR-140, CR-141, CR-142, CR-143, CR-144, CR-146, CR-147, CR-152, CR-153, CR-155

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
| POST | /auth/register | 이메일 회원가입 (CUSTOMER 역할). 이미 가입된 이메일은 409, `error: EMAIL_ALREADY_EXISTS` (CR-131, BIZ-008). 확인을 마친 인증번호가 없으면 400 `EMAIL_NOT_VERIFIED`, 가입하면 그 번호를 다 쓴 것으로 하고 이메일 확인 시각을 채운다 (CR-146, BIZ-062) | | BID-AUTH-001 |
| POST | /auth/login | 로그인 (JWT Access+Refresh 발급) | | BID-AUTH-002 |
| POST | /auth/refresh | Refresh 토큰으로 Access 토큰 갱신 | | BID-AUTH-003 |
| POST | /auth/email-codes | 인증번호 보내기 `{email}`. 200 `{expiresInSeconds, resendAfterSeconds}`. 이미 가입된 메일 409 `EMAIL_ALREADY_EXISTS`, 간격 전 429 `EMAIL_CODE_TOO_SOON`(`retryAfterSeconds`), 하루 한도 429 `EMAIL_CODE_DAILY_LIMIT`, 메일 실패 502 `EMAIL_SEND_FAILED` (CR-146, POL-037) | | BID-AUTH-005 |
| POST | /auth/email-codes/verify | 인증번호 확인 `{email, code}`. 200. 틀림 400 `EMAIL_CODE_MISMATCH`(`remainingAttempts`), 시간 지남 410 `EMAIL_CODE_EXPIRED`, 횟수 넘음 429 `EMAIL_CODE_LOCKED` (CR-146) | | BID-AUTH-005 |
| GET | /auth/sns/providers | 키가 설정된 SNS 목록 `[NAVER, KAKAO]` 중 일부(GOOGLE 은 보류) (CR-146) | | BID-AUTH-006 |
| GET | /oauth2/authorization/{provider} | SNS 로그인 시작. 스프링 기본 주소, SNS 로그인 화면으로 보낸다 (CR-146) | | BID-AUTH-006, BID-AUTH-007 |
| GET | /login/oauth2/code/{provider} | SNS 가 돌려보내는 주소(SNS 앱에 등록). 이어진 회원이면 LOGIN 표를 만들어 화면 `/auth/sns/done?ticket=`, 처음이면 SIGNUP 표로 `/register/sns?ticket=`, SNS 이메일로 가입된 회원이 있으면 `/login?sns=EMAIL_IN_USE`, 취소 `/login?sns=CANCELLED`, 실패 `/login?sns=FAILED` 로 보낸다 (CR-146, BIZ-063) | | BID-AUTH-006, BID-AUTH-007 |
| POST | /auth/sns/login | LOGIN 표 `{ticket}` 를 Access, Refresh 토큰으로 바꾼다. 한 번만. 쓴 표, 시간 지난 표 410 `SNS_TICKET_EXPIRED` (CR-146, POL-038) | | BID-AUTH-007 |
| GET | /auth/sns/signup/{ticket} | SNS 가입 화면 채울 값 `{provider, email, emailVerifiedByProvider}`. 쓴 표, 시간 지난 표 410 `SNS_TICKET_EXPIRED` (CR-146) | | BID-AUTH-006 |
| POST | /auth/sns/register | SNS 가입 `{ticket, email, companyName, businessRegistrationNumber, contactPerson, phone, agreeTerms, agreePrivacy, agreeMarketing}`. 이메일이 SNS 확인 메일과 다르거나 SNS 확인이 없으면 확인을 마친 인증번호가 있어야 한다(없으면 400 `EMAIL_NOT_VERIFIED`). 이미 가입된 메일 409 `EMAIL_ALREADY_EXISTS`, 같은 SNS 계정이 먼저 가입을 마쳤으면 409 `SNS_ACCOUNT_IN_USE`, 표 410 `SNS_TICKET_EXPIRED`. 성공하면 회원, SNS 연결, 동의 이력을 남기고 토큰을 준다 (CR-146, BIZ-062, BIZ-063) | | BID-AUTH-006 |
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

## B-5. 미 연방 계약 받기 (Admin, CR-142)

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| POST | /admin/federal-contracts/sync | 받기 바로 실행. 본문 `mode`(FULL, DAILY). 회차를 만들고 뒤에서 돌린 뒤 202 와 회차 id 를 준다 | 🔒 ADMIN | BID-MKT-027 |
| GET | /admin/federal-contracts/sync-runs | 최근 받기 회차 `size`건(기본 20, 최대 100), 최근 시작순. 줄은 T3-1 federal_contract_sync_runs 칸 그대로 | 🔒 ADMIN | BID-MKT-027 |

- 돌고 있는 회차가 있으면 409. mode 가 없거나 다른 값이면 400.
- 매일 받기는 스케줄러(POL-032)가 같은 처리를 부른다. 관리자 화면은 두지 않는다.

---

## B-6. 고객 화면 사진 (Admin, CR-153)

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /admin/site-photos | 사진 자리 목록(sort_order 순)과 자리마다 모든 사진(숨긴 것 포함, 넣은 순), 올리기 규칙 | 🔒 ADMIN | BID-SITE-001 |
| POST | /admin/site-photos | 사진 올리기. multipart `slotKey`, `file`. 줄여 저장한 뒤 사진 한 장 정보(201) | 🔒 ADMIN | BID-SITE-001 |
| PATCH | /admin/site-photos/{id} | 보임 바꾸기. 본문 `{ visible }` | 🔒 ADMIN | BID-SITE-001 |
| DELETE | /admin/site-photos/{id} | 지우기(204). 올린 사진이면 저장 파일도 지운다 | 🔒 ADMIN | BID-SITE-001 |

**목록 응답**: `rules`(`allowedTypes[]`, `maxUploadMb`, `maxMegapixels`, `minWidth`, `saveWidth`, `saveMaxKb`, POL-039 의 지금 값), `slots[]`(`key`, `name`, `location`, `photos[]`).

**사진 한 장**: `id`, `slotKey`, `url`, `fileName`, `width`, `height`, `originalWidth`(함께 배포된 사진은 null), `originalHeight`, `sizeBytes`, `bundled`(함께 배포된 사진이면 참), `visible`, `createdAt`.

**올리기 오류**: 자리가 없으면 404. 400 은 사유 문구와 함께: 받는 형식이 아니거나 그림으로 읽을 수 없음, 무게 상한 넘음, 화소 수 상한 넘음, 가로 최소보다 좁음(BIZ-068, POL-039). 파일이 비었거나 없으면 400.

**사진 주소(url)**: 함께 배포된 사진은 bundled_path 그대로(`/img/...`), 올린 사진은 `<context-path>/site-photos/{id}/image`(운영 `/api/site-photos/{id}/image`).

---

## C. 공고 열람 (Opportunity Browse) — Sprint 3

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /opportunities | 공고 목록 조회 (필터/검색/페이징). **사용자: visibility=VISIBLE만** (CR-003). 고객 화면은 더 쓰지 않고(CR-155, 마감 전 공고는 C-2 의 collected), 관리자 콘솔이 공개 공고문 수를 셀 때 쓴다 | | BID-BROWSE-001 |
| GET | /opportunities/{id} | 공고 상세 조회 (사전 분석 결과 포함 — CR-003). 응답 `responseDeadline` 은 날짜만, `responseDeadlineAt` 은 서울 시각 표시가 붙은 마감 시각(CR-138). `samUrl` 은 SAM.gov 원문 공개 주소(`uiLink` 는 SAM 로그인이 필요한 작업공간 주소라 화면 단추는 `samUrl` 을 쓴다, CR-152). `opportunityId` 는 이 공고문이 만들어진 모은 공고 id 이고 관심 공고 저장에 쓴다(CR-155) | | BID-BROWSE-002, 004 |
| GET | /opportunities/origin/{opportunityId} | 원본 공고 상세 (CR-152). 모은 원본 공고 하나(지운 것 제외)를 공고 상세와 같은 꼴로 준다. `id` 는 원본 공고 id, `title` 은 한글 제목(없으면 원문 제목), `description` 은 모아 둔 원문 본문(없으면 null), `analysis` 는 없다. 더하는 값: `originalTitle`(원문 제목), `publicNoticeId`(같은 공고의 공개 한글 공고문 id, 없으면 null. 있으면 화면이 공고문 상세로 전환한다), `samUrl`(SAM.gov 원문 공개 주소), `descriptionKo`(원문 본문을 한글로 옮긴 것, 없으면 null, CR-155). 없는 id 는 404 | | BID-BROWSE-002 |
| GET | /opportunities/{id}/requirements | 공고 요구사항 목록 (카테고리별) | | BID-BROWSE-003 |
| GET | /opportunities/{id}/attachments | 공고 첨부문서 목록 | | BID-OPP-003 |
| ~~GET~~ | ~~/opportunities/search~~ | ~~키워드 검색~~ (CR-155 폐기. 마감 전 공고 검색은 C-2 의 collected `q`) | | BID-BROWSE-001 |
| GET | /opportunities/search/organization | 기관명 검색 | | BID-BROWSE-001 |
| ~~GET~~ | ~~/opportunities/near-deadline~~ | ~~마감 임박 공고~~ (CR-155 폐기. 첫 화면 마감 임박은 C-2 의 collected `sort=DEADLINE`, `dueWithinDays`) | | BID-BROWSE-001 |
| GET | /opportunities/recent | 최근 등록 공고 | | BID-BROWSE-001 |

---

## C-2. 모은 공고 목록과 집계 (CR-135)

메뉴 속 화면 2묶음(사전 공고, 변경 공고, 공고문 예시, 최근 낙찰, 월별 동향, 업종별 발주)이 쓴다. CR-155 부터 고객 마감 전 공고 화면(`/search`)과 첫 화면 마감 전 공고 목록도 collected 를 쓴다. C 의 공개 공고문 조회와 달리 **모은 원본 공고 전부**(지운 것 제외)를 읽는다. 모두 로그인 없이 부른다(GET /opportunities/** 공개). 읽기만 한다.

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /opportunities/collected | 모은 공고 목록, 게시일 최신순, 쪽. 거르기: `group`(PRE 사전, BID 입찰, AWARD 낙찰, OTHER 그 밖), `type`(SAM 공고 유형), `month`(게시 달 YYYY-MM), `sector`(업종 대분류, 예 23, 31-33, NONE), `openOnly`(답변 기한 남은 것만), `latestOnly`(같은 공고번호는 마지막 판 하나만, 업종별 최근 공고에 씀). 같은 거르기에서 유형별 건수를 함께 준다. CR-155 에서 더한 것: `q`(검색어. 한글 제목, 원문 제목, 공고번호에 대소문자 없이 들어 있는 것), `sort`(LATEST 게시일 최신순이 기본, DEADLINE 마감이 가까운 순이고 마감이 같으면 게시일 최신순), `dueWithinDays`(마감이 지금부터 N일 안인 것만, 1 이상). `openOnly` 는 낙찰 공지를 늘 뺀다(BIZ-069). 마감 전 공고 화면과 첫 화면은 `openOnly=true&latestOnly=true` 로 부른다 | | BID-MKT-001, 005, 006, BID-BROWSE-001, 005 |
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
| agencyName | 발주처(모은 공고의 기관 이름, 없으면 null) (CR-155) |
| naicsCode | 업종 코드(원본 JSON 의 naicsCode, 없으면 null) (CR-155) |
| placeOfPerformance | 이행 장소. 짧은 이름이 있으면 그것, 없으면 원본 JSON 의 이행 장소, 둘 다 없으면 null (CR-155) |
| type, typeKo, group | SAM 공고 유형, 한글 유형 이름, 묶음 유형 |
| postedDate, responseDeadline | 게시일, 답변 기한(없으면 null) |
| awardAmount | 낙찰 금액 표시 문자열(낙찰 공지만, 없으면 null) |
| awardeeName | 낙찰 업체 이름(낙찰 공지만). 업체 이름 칸이 생기기 전(CR-136 전)에는 늘 null |
| publicNoticeId | 같은 공고의 공개 한글 공고문 id(없으면 null). 있으면 화면이 공고문 상세(`/search/{id}`)로, 없으면 원본 공고 상세(`/search/origin/{opportunityId}`)로 보낸다(CR-152) |
| samUrl | SAM.gov 원문 공개 주소. 주소 형식은 설정 `sam-gov.public-opportunity-url` |
| hasMainNotice | (사전 공고만) 같은 공고번호로 입찰 공고가 올라왔는지 |
| originalId | (낙찰 공지만) 같은 공고번호의 원래 공고 중 마지막 판 id(없으면 null) |
| originalPublicNoticeId, originalSamUrl | (낙찰 공지만) 원래 공고의 공개 한글 공고문 id(없으면 null)와 SAM.gov 원문 공개 주소. 화면의 "원래 공고" 가 쓴다 |

**자료 기간**: 응답마다 `dataFrom`, `dataTo`(대상 공고의 첫·마지막 게시일)와 `total` 을 준다.

**순서**: 목록은 게시 시각 최신순이고, 같은 시각이면 DB 의 id 순(바이트 순)이다. collected 의 `sort=DEADLINE` 은 마감 시각이 이른 순이고, 마감이 같으면 게시 시각 최신순, 그다음 id 순이다(CR-155).

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

## C-4. 미 연방 계약 자료 (CR-142)

메뉴 속 화면 4묶음(다음 달 공고, 재발주 달력, 낙찰가 범위)이 쓴다. federal_contracts 만 읽고 우리 공고와 섞지 않는다. 로그인 회원만 연다. 달과 오늘은 Asia/Seoul 날짜다.

- **변경 (CR-147)**: 재발주 예측을 지워 `GET /federal-contracts/recompete`, `GET /federal-contracts/recompete/calendar` 를 폐기한다(배포 뒤 404). 남는 것은 낙찰가 범위(award-range)와 업종 고르기(naics)이고, 업종 고르기의 기능 ID 는 BID-MKT-034 만 남는다. 자료 기준(`source`)은 남은 두 응답에 그대로 싣는다. 계약 줄에서 category, basisDateKind, basisDate, oneTimeConstruction 을 지운다. month 검증(이번 달부터 POL-033 달 수)은 recompete 와 함께 없어진다. 근거: 원문 "재발주 예측 삭제", 사용자 10/11 20:01 /전승(정할 것 1)

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| ~~GET~~ | ~~/federal-contracts/recompete~~ | ~~고른 달 계약. `month`(YYYY-MM, 기본 다음 달), `category`(RecompeteCategory, 없으면 전부), `naics`(6자리), `sort`(END 그 달 날짜순 기본, AMOUNT 금액 큰 순), `page`, `size`(기본 20, 최대 100)~~ | 🔒 | ~~BID-MKT-028, 029~~ **폐기 (CR-147)** |
| ~~GET~~ | ~~/federal-contracts/recompete/calendar~~ | ~~이번 달부터 POL-033 달 수만큼 달마다 구분별 수. `naics`~~ | 🔒 | ~~BID-MKT-030~~ **폐기 (CR-147)** |
| GET | /federal-contracts/award-range | 낙찰가 범위. `noticeNumber`(우리 공고번호) 또는 `naics` 하나는 필수, `q`(작업 설명 낱말) | 🔒 | BID-MKT-034 |
| GET | /federal-contracts/naics | 업종 고르기 목록. `q`(코드나 이름 일부). 업종마다 code, name, count(계약 수), 계약 수 많은 순 50건 | 🔒 | BID-MKT-029, 034 |

**자료 기준 (`source`, 네 응답 공통)**

| 필드 | 설명 |
|------|------|
| totalCount | 표에 있는 계약 수 |
| lastSyncedAt | 마지막으로 성공한 받기의 끝난 시각(+09:00 붙인 ISO). 한 번도 없으면 null, 화면은 "자료를 받고 있습니다" |
| lagDays | 자료가 늦게 올라오는 날 수(POL-036 FEDERAL_DATA_LAG_DAYS) |
| dataFrom | 받는 시작일(POL-031) |

**계약 줄**

| 필드 | 설명 |
|------|------|
| id, piid, parentPiid | parentPiid 는 상위 계약이 없으면 null |
| awardType | FederalAwardType |
| description, recipientName, amountUsd | amountUsd 는 숫자, 없으면 null |
| naicsCode, naicsName | |
| startDate, endDate, potentialEndDate | potentialEndDate 는 아직 받지 않았거나 USAspending 에 없으면 null |
| solicitationNumber | 원래 공고번호, 없으면 null |
| publicNoticeId | 원래 공고번호로 낸 공개 한글 공고문이 있으면 그 공고문 id(화면은 공고 상세로 보낸다), 없으면 null |
| ~~category~~ | ~~RecompeteCategory(BIZ-050)~~ **폐기 (CR-147)** |
| ~~basisDateKind, basisDate~~ | ~~그 달에 걸린 날짜와 종류(ContractDateKind). 화면은 이 둘과 awardType, parentPiid 로 근거 한 줄을 만든다~~ **폐기 (CR-147)** |
| ~~oneTimeConstruction~~ | ~~한 번짜리 공사 꼬리표(BIZ-051)~~ **폐기 (CR-147)** |
| usaspendingUrl | USAspending 계약 쪽 주소. 앞부분은 설정 `app.federal-contract.award-page-url` |

**응답**
- recompete: `month`, `counts`(RECOMPETE, EXTENDABLE, IN_PARENT, UNCONFIRMED 마다 수, 업종 거르기는 반영하고 category 거르기는 반영하지 않는다), `items`(계약 줄), `page`, `size`, `totalElements`, `source`.
- calendar: `months[]`(month, RECOMPETE, EXTENDABLE, IN_PARENT, UNCONFIRMED 수), `source`.
- award-range: `naicsCode`, `naicsName`, `noticeNumber`·`noticeTitle`(공고번호로 찾았을 때만, 제목은 원문 제목), `rows[]`(group STANDALONE 상위 계약 없는 계약 또는 ORDER 상위 계약 안 주문, count 쓴 계약 수, excludedCount 0 이하라 뺀 수, minCount POL-034, min, p25, p50, p75, max. count 가 minCount 미만이면 다섯 값은 null), `evidence[]`(쓴 계약 줄 20건, 최근 시작순, category 와 basis 칸은 비움), `source`.

**검증**: month 형식이 틀리거나 이번 달부터 POL-033 달 수 밖이면 400. award-range 에 noticeNumber 와 naics 가 모두 없으면 400. noticeNumber 공고가 없으면 404, 그 공고에 업종 코드가 없으면 422(화면은 업종을 고르라고 안내).

---

## ~~C-5. 회원 계약과 옵션 연장 알림 (CR-142)~~ (CR-147 폐기)

- **폐기 (2026-10-11, CR-147)**: 옵션 연장 알림 화면과 회원 계약 표(member_contracts, V51 DROP)를 지우면서 아래 다섯 API(GET /member-contracts, GET /member-contracts/lookup, POST /member-contracts, PATCH /member-contracts/{id}, DELETE /member-contracts/{id})를 모두 폐기한다. 근거: 원문 "재발주 예측 삭제", 사용자 10/11 20:01 /전승(정할 것 1)

옵션 연장 알림 화면이 쓴다. 로그인 회원 본인 계약만. 남의 계약 id 는 404.

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| ~~GET~~ | ~~/member-contracts~~ | ~~내 계약 목록과 알림 설정~~ | 🔒 | ~~BID-MKT-032~~ **폐기 (CR-147)** |
| ~~GET~~ | ~~/member-contracts/lookup~~ | ~~계약번호로 찾기. `piid`. 표에 없으면 받는 범위 안에서 USAspending 검색 1회, 옵션 끝날짜를 아직 받지 않았으면 상세 1회~~ | 🔒 | ~~BID-MKT-031~~ **폐기 (CR-147)** |
| ~~POST~~ | ~~/member-contracts~~ | ~~등록. 본문 `piid`, `endDate?`, `potentialEndDate?`, `description?`. 계약을 찾으면 PUBLIC 으로 등록하고 본문 날짜는 쓰지 않는다. 못 찾으면 MANUAL 이고 endDate 필수~~ | 🔒 | ~~BID-MKT-031~~ **폐기 (CR-147)** |
| ~~PATCH~~ | ~~/member-contracts/{id}~~ | ~~MANUAL 계약의 `endDate`, `potentialEndDate`, `description` 고치기~~ | 🔒 | ~~BID-MKT-032~~ **폐기 (CR-147)** |
| ~~DELETE~~ | ~~/member-contracts/{id}~~ | ~~지우기~~ | 🔒 | ~~BID-MKT-032~~ **폐기 (CR-147)** |

**목록 응답**: `alertDays[]`(POL-035, 꺼져 있으면 빈 배열), `today`, `contracts[]`(id, piid, description, source, endDate, potentialEndDate, endDaysLeft, potentialDaysLeft, nextAlertOn, nextAlertKind, datesChangedOn, lastAlertedAt, federalContract 찾은 계약 줄 또는 null), `source`(C-4 와 같은 자료 기준, 찾은 계약 칸의 기준일과 늦음 안내에 쓴다). 지금 끝날짜가 가까운 순.

**찾기 응답**: `found`(참·거짓), `contract`(계약 줄, 못 찾았으면 null), `alreadyRegistered`(이미 내 계약이면 참).

**검증**: 계약번호는 앞뒤 공백·하이픈·빈칸을 빼고 대문자로 맞춘 뒤 비면 400. 같은 계약번호 두 번째 등록 409. MANUAL 인데 endDate 가 없으면 400. potentialEndDate 가 endDate 보다 앞이면 400. PUBLIC 계약 날짜 고치기 409. USAspending 이 실패하면 찾기는 502, 화면은 직접 넣기로 안내.

---

## C-6. 고객 화면 사진 (CR-153)

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /site-photos | 자리별 보이는 사진 주소 목록. `{ slots: { <자리 키>: [url, ...] } }`. 모든 자리 키가 있고 보이는 사진이 없으면 빈 배열. 넣은 순 | | BID-SITE-002 |
| GET | /site-photos/{id}/image | 올린 사진 파일(image/jpeg). 없거나 함께 배포된 사진이면 404 | | BID-SITE-002 |

- 목록은 `Cache-Control: no-cache`. 고객 포털이 받은 목록을 브라우저에 남겨 두고 다음 방문에 쓴다(T3-4).
- 사진 파일은 한 번 저장되면 바뀌지 않으므로 `Cache-Control: public, max-age=31536000, immutable`.

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

> CR-143: `{id}` 가 붙은 위 경로는 그 입찰 건을 낸 회원과 관리자만 쓴다. 남의 입찰 건과 없는 입찰 건은 똑같이 404 "입찰 건을 찾을 수 없습니다"(BIZ-060). 상태 전이는 고객 허용 상태 확인(403) 다음에 주인을 본다.

---

## G. 고객 문서 제출 (Client Documents) — Sprint 4

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| POST | /bid-requests/{id}/client-documents | 문서 업로드 (multipart) | 🔒 | BID-REQ-002 |
| GET | /bid-requests/{id}/client-documents | 제출 문서 목록 | 🔒 | BID-REQ-002 |
| DELETE | /bid-requests/{id}/client-documents/{docId} | 제출 문서 삭제 | 🔒 | BID-REQ-002 |

> CR-143: 그 입찰 건을 낸 회원과 관리자만 쓴다. 남의 입찰 건이면 404(BIZ-060). 삭제는 그 서류가 경로의 입찰 건 것일 때만 하고 아니면 404 "서류를 찾을 수 없습니다". 올리기 크기 한도는 `spring.servlet.multipart.max-file-size`(BIZ-011, 지금 50MB)를 따르고 넘으면 400.

### G-1. 요구사항 슬롯 (CR-010)

공고 분석으로 도출된 BLOCKER 요구사항을 슬롯으로 펼쳐서 1:1 업로드 매핑. DOCS_PENDING → DOCS_RECEIVED 전이의 사전 조건(BIZ-015).

> CR-141: 슬롯은 공고 분석의 제출 서류에서 채운 요구 항목이다(BIZ-045). 모든 슬롯 API 는 그 입찰 건을 낸 회원만 쓴다. 남의 입찰 건이면 404(BIZ-049). 슬롯 목록 조회(GET)는 걷어내고 G-2 건별 제출 전 검사가 대신한다. 고객 올림이 아닌 항목(플랫폼 작성, 정부 양식)에 올리면 400.

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| ~~GET~~ | ~~/bid-requests/{id}/required-document-slots~~ | 걷어냄 (CR-141). G-2 `GET /bid-requests/{id}/pre-check` 가 대신한다 | 🔒 | BID-REQ-002 |
| POST | /bid-requests/{id}/required-document-slots/{requirementItemId}/upload | 슬롯에 직접 업로드 (multipart). 한 트랜잭션에서 ClientDocument 생성 + RequirementFulfillmentMap(`CLIENT_DOCUMENT`, `FULFILLED`) 생성/업데이트. CR-141: PDF 면 쪽수를 세어 page_count 에 넣고(세지 못하면 비움), 응답은 G-2 의 요구 항목 한 줄(clientItems[] 한 개) | 🔒 | BID-REQ-002, BID-MKT-023 |
| POST | /bid-requests/{id}/required-document-slots/{requirementItemId}/from-vault | 공통 서류함 서류를 입찰 건 서류로 복사해 잇는다 (CR-141). 본문 `{"memberDocumentId": "uuid"}`. 저장소 파일을 복사해 ClientDocument(분류는 요구 항목 이름, 쪽수 셈)를 만들고 연결을 만든다. 자기 서류가 아니면 404. 응답은 upload 와 같다 | 🔒 | BID-MKT-023 |
| DELETE | /bid-requests/{id}/required-document-slots/{requirementItemId} | 슬롯 매핑 해제. FulfillmentMap 삭제 (ClientDocument는 보존, 별도 DELETE 호출 시 제거) | 🔒 | BID-REQ-002, BID-MKT-023 |

**응답 스키마 (GET 슬롯 목록, CR-141 에서 걷어냄)**:
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

### G-2. 제출 전 검사 (CR-141)

입찰 건마다 고객 올림 요구 항목과 올린 서류를 맞춰 판정한다. 그 입찰 건을 낸 회원만(BIZ-049). 응답은 다른 API 와 같이 래퍼 없이 본문을 그대로 준다.

| 메서드 | 경로 | 설명 | 인증 | 기능 ID |
|--------|------|------|------|---------|
| GET | /bid-requests/my/pre-check | 내 입찰 건별 요약. 마감일 가까운 순 | 🔒 | BID-MKT-021 |
| GET | /bid-requests/{id}/pre-check | 입찰 건별 제출 전 검사. 남의 입찰 건이면 404 | 🔒 | BID-MKT-022 |

**응답 스키마 (GET /bid-requests/my/pre-check)**:
```json
[
  {
    "bidRequestId": "uuid", "state": "DOCS_PENDING",
    "solicitationNumber": "W90VN926RA081", "title": "...", "titleKo": "...",
    "responseDeadline": "2026-10-20T14:00:00+09:00",
    "analysisReady": true,
    "summary": { "required": 5, "uploaded": 3, "missing": 2, "mismatch": 1, "needsHuman": 2 }
  }
]
```
- analysisReady 는 그 공고에 요구 항목이 있는지다. false 면 summary 는 null.
- required 는 필수이고 고객 올림인 항목 수, uploaded 는 그중 이어진 수, missing 은 그 차. mismatch 는 형식 어긋남이거나 쪽수 넘음인 항목 수, needsHuman 은 형식이나 쪽수가 사람 확인인 항목 수(둘 다 이어진 항목만 센다).

**응답 스키마 (GET /bid-requests/{id}/pre-check)**:
```json
{
  "bidRequestId": "uuid", "state": "DOCS_PENDING",
  "solicitationNumber": "...", "title": "...", "titleKo": "...", "responseDeadline": "...", "noticeId": "uuid|null",
  "analysisReady": true,
  "clientItems": [
    {
      "requirementItemId": "uuid", "title": "Past Performance Questionnaire", "description": "...",
      "mandatory": true, "isBlocker": true, "sourceRef": "Solicitation p.38, Section 3.1.1", "notes": "...",
      "formatText": "PDF/Word", "pageLimitText": "1 page",
      "document": { "clientDocumentId": "uuid", "fileName": "ppq.pdf", "extension": "pdf", "pageCount": 2, "fileSize": 12345, "uploadedAt": "2026-10-11T12:00:00+09:00" },
      "presence": "UPLOADED",
      "format": { "verdict": "MATCH", "basis": "PDF/Word", "allowed": ["PDF", "WORD"] },
      "pages": { "verdict": "OVER", "basis": "1 page", "limit": 1 }
    }
  ],
  "platformItems": [ { "requirementItemId": "uuid", "title": "Technical Proposal", "fulfillmentParty": "PLATFORM_GENERATED", "sourceRef": "..." } ],
  "summary": { "required": 5, "uploaded": 3, "missing": 2, "mismatch": 1, "needsHuman": 2 },
  "canTransition": false,
  "editable": true,
  "maxUploadMb": 50
}
```
- presence: UPLOADED, MISSING. format.verdict: MATCH, MISMATCH, NEEDS_HUMAN, NO_CONDITION, NOT_UPLOADED(BIZ-047). pages.verdict: MATCH, OVER, NEEDS_HUMAN, NO_CONDITION, NOT_UPLOADED(BIZ-048). basis 는 공고 조건 원문 그대로, allowed 와 limit 은 원문에서 읽어 낸 기준이고 읽지 못하면 빈 배열과 null.
- canTransition 은 서류 대기 상태이고 필수 고객 서류가 모두 이어졌을 때 true(BIZ-046). 형식·쪽수 판정은 넣지 않는다.
- editable 은 접수 전(CREATED)이나 서류 대기(DOCS_PENDING)일 때 true. false 면 G-1 의 올리기, 서류함에서 고르기, 빼기, 자동 채우기가 409 로 거부된다. maxUploadMb 는 올릴 수 있는 파일 한도(spring.servlet.multipart.max-file-size)라 화면이 숫자를 따로 갖지 않는다.

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
| GET | /mcp/opportunity-attachments/{id}/download | 공고 첨부 원본 내려받기 | CR-019. Aimbase download_file 용 |
| GET | /mcp/rfp-files/{id}/download | 성공 제안서 원본 내려받기 | CR-013. Aimbase parse_document 용 |

> SSE 전송: Aimbase가 `GET /mcp/sse`로 연결 → `endpoint` 이벤트로 메시지 URL 수신 → `POST /mcp/message`로 JSON-RPC 메시지 전송 → SSE `message` 이벤트로 응답 수신

### 열쇠 확인 (CR-144, BIZ-061)

| 경로 | 받는 조건 | 막을 때 |
|------|----------|--------|
| POST /mcp, GET /mcp/sse, POST /mcp/message | `X-API-Key` 머리글이 서버 열쇠와 같다 | 401, 본문 없음. 도구를 부르지 않는다 |
| GET /mcp/opportunity-attachments/{id}/download, GET /mcp/rfp-files/{id}/download | 위 머리글, 또는 주소의 `exp`, `sig` 가 그 경로의 서명과 맞고 기한 안이다 | 401, 본문 없음 |

- 서버 열쇠는 환경변수 `MCP_API_KEY`(설정 `app.mcp.api-key`)다. 비어 있으면 /mcp 요청을 모두 401 로 막고(열어 두지 않는다) 기동할 때 경고를 남긴다.
- 서명 주소는 이 플랫폼이 만든다. `{SELF_BASE_URL}/mcp/opportunity-attachments/{id}/download?exp={만료 시각, 초}&sig={서명}` 꼴이다. 서명은 서버 열쇠로 만든 HMAC-SHA256(경로와 만료 시각)을 base64url 로 적은 것이다.
- 기한은 설정 `app.mcp.download-url-ttl`(환경변수 `MCP_DOWNLOAD_URL_TTL`, 처음 값 1시간)이다. 이 플랫폼이 Aimbase 실행을 기다리는 한도(30분)보다 길게 둔다.
- 서명 주소를 주는 곳은 공고 분석 워크플로우 입력의 `attachmentFiles[].downloadUrl` 과 `get_reference_samples` 의 `files[].downloadUrl` 이다.
- Aimbase 등록 bidding-agency-mcp 의 config 에 `headers: {"X-API-Key": 열쇠}` 를 싣는다. Aimbase 가 등록 머리글을 SSE 연결, 메시지 보내기, Streamable 요청에 모두 싣는다(Aimbase 쪽 CR).
- 401 은 보안 설정의 인증 실패처럼 상태만 준다. 막을 때 방법, 경로, 보낸 곳만 남기고 열쇠와 서명 값은 남기지 않는다.

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
| get_bid_request | 입찰 요청 상세 + 문서 목록 | bidRequestId | BID-MCP-004 |
| save_document_version | AI 생성 문서 저장 | bidRequestId, documentType, contentJson | BID-MCP-003 |
| get_document_template | 문서 템플릿 조회 | documentType | BID-MCP-003 |
| transition_bid_state | 입찰 상태 전이 | bidRequestId, targetState | BID-MCP-004 |
| get_opportunity_analysis | 공고 사전 분석 결과 조회 (CR-003) | opportunityId | BID-MCP-002 |
| save_opportunity_analysis | 공고 사전 분석 결과 저장 (CR-003) | opportunityId, summary, documentFormats, requiredDocuments, llmPromptPreset | BID-MCP-002 |
| get_past_submissions | 과거 제출 이력 조회 (CR-003) | memberId, limit | BID-MCP-002 |
| get_reference_samples | 공고유형 매칭 성공 제안서 원본 파일 메타+다운로드URL 조회 (CR-013-R) | industryType | BID-RFP-003 |
| save_pattern_guide | 공고유형별 패턴 가이드 저장 — Aimbase 콜백 (CR-013-R) | industryType, guide{골격/체크리스트/금기} | BID-RFP-003 |

> CR-141: save_requirements(LLM 추출 요구사항 저장)를 걷어냈다. 요구 항목은 공고 분석에서 채운다(BIZ-045).
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

> Validation API 는 설정 `app.validation-api.enabled` 가 켜진 곳에서만 열린다(CR-143). 기본값이 꺼짐이라 운영에는 경로가 없고(404), 로컬은 application-local.yml 에서 켠다. 인증은 없다.
> X-Flow-Id 헤더는 FlowIdFilter에서 전체 요청에 대해 자동 수신·전파.

---

## API 요약

| 영역 | 엔드포인트 수 | Sprint |
|------|-------------|--------|
| A. 인증 | 5 | 1 |
| B. 공고 수집 (Admin) | 3 | 2 |
| C. 공고 열람 | ~~8~~ 7 | 3, CR-152(origin 더함), CR-155(search, near-deadline 폐기) |
| C-2. 모은 공고 목록과 집계 | 6 | CR-135 |
| C-3. 낙찰 업체와 기지별 집계 | 3 | CR-136 |
| C-4. 미 연방 계약 자료 | 2 | CR-142, CR-147(recompete 둘 폐기) |
| ~~C-5. 회원 계약과 옵션 연장 알림~~ | ~~5~~ 0 | CR-142, CR-147 폐기 |
| B-5. 미 연방 계약 받기 (Admin) | 2 | CR-142 |
| B-4. 기지 관리와 지난 낙찰 다시 받기 (Admin) | 6 | CR-136 |
| B-6. 고객 화면 사진 (Admin) | 4 | CR-153 |
| C-6. 고객 화면 사진 | 2 | CR-153 |
| N. 회사 공통 서류함 | 7 | CR-125, CR-136 |
| D. 즐겨찾기 | 4 | 3 |
| E. 자격 진단 | 4 | 3, CR-137, CR-140 |
| F. 입찰 접수 | 7 | 4 |
| G. 고객 문서 제출 | 8 | 4, CR-010, CR-141 |
| H. 입찰 관리 (Admin) | 6 | 4 |
| I. 문서 템플릿 (Admin) | 5 | 6 |
| J. 문서 편집 | 7 | 7 |
| K. 회원 관리 (Admin) | 1 | — |
| L. 요금 안내 | 1 | 7 |
| Q. 고객센터 1:1 문의 | 6 | CR-132 |
| M. FlowGuard 연동 | 5 | — (CR-001) |
| B-2. 공고 관리 (Admin) | 8 | CR-003 |
| P. 성공 제안서 패턴 (Admin) | 13 | CR-013 |
| MCP Tools | 12 | 5, CR-003, CR-013, CR-141 |
| MCP SSE 엔드포인트 | 2 | CR-002 |
| Aimbase 워크플로우 호출 | 2 | CR-002 |
| **합계** | **REST 123 + MCP 12 + SSE 2 + Aimbase 2** | |

> CR-141: G 는 문서 3, 슬롯 3(올리기, 서류함에서 고르기, 빼기. 목록 조회는 걷어냄), 제출 전 검사 2 다. 그전에는 슬롯을 세지 않았다.

> CR-147: REST 124 에서 재발주 조회 둘(C-4)과 회원 계약 다섯(C-5)을 빼 117 이다.

> CR-153: 고객 화면 사진 관리자 4(B-6)와 공개 2(C-6)를 더해 123 이다.

---

## N. 회사 공통 서류함 (CR-125)

> member 종속 공통 서류 보관 + 제안서 슬롯 자동채움. 인증=본인 member(CUSTOMER).

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/member-documents` | 내 서류함 — 고정 카테고리 6슬롯 + 기타 목록(MemberDocumentVaultResponse) |
| POST | `/member-documents` (multipart) | 업로드 — `file`, `category`, `displayName?`, `description?`(기타) |
| PUT | `/member-documents/{id}` (multipart) | 교체(파일 재업로드, displayName/description 갱신) |
| DELETE | `/member-documents/{id}` | 삭제(StorageService 파일도 삭제) |
| POST | `/bid-requests/{bidRequestId}/required-document-slots/auto-fill` | 서류함에서 같은 카테고리 서류를 미충족 슬롯에 일괄 자동매칭(CR-125 2단계). CR-141: 슬롯 이름으로 서류함 분류를 찾고, 고객 올림 항목만, 그 입찰 건을 낸 회원만 |
| GET | `/member-documents/expiry` | 서류별 만료 상태(CR-136). 응답: `alertDays[]`(POL-016, 꺼져 있으면 빈 배열), `soonDays`(가장 큰 알림 날, 꺼져 있으면 null), `today`(Asia/Seoul 날짜), `summary`(expired, soon, noDate, total), `documents[]`(id, category, categoryLabel, displayName, fileName, expiresOn, daysLeft, status, lastAlertedAt) |
| PATCH | `/member-documents/{id}/expiry` | 만료일만 넣기·고치기·지우기(CR-136). 본문 `{"expiresOn": "YYYY-MM-DD" 또는 null}`. 응답 MemberDocumentDto |

- 응답 DTO: `MemberDocumentDto`(LAZY 폭발 방지) / `MemberDocumentVaultResponse`(fixedSlots[] + others[]).
- 소유 검증: 타 member 문서 접근 시 403/404. auto-fill은 미충족(PENDING/MISSING) 슬롯만 대상, 이미 충족 슬롯 미덮어씀.
- CR-136: POST·PUT 에 `expiresOn?`(YYYY-MM-DD) 를 더하고 MemberDocumentDto 에 `expiresOn` 을 싣는다. 날짜 형식이 틀리면 400.
- CR-136 만료 상태(ExpiryStatus): `EXPIRED`(만료일이 오늘보다 앞), `SOON`(남은 날 0 이상 soonDays 이하), `OK`(그보다 많이 남음, 알림이 꺼져 있으면 0 이상 전부), `NONE`(만료일 없음). daysLeft = 만료일 - 오늘(Asia/Seoul). lastAlertedAt 은 이 서류의 알림 이력(DOCUMENT_EXPIRY) 중 보내기에 성공한 마지막 시각.
- **변경 (CR-147)**: 고정 칸은 6슬롯에서 다섯 칸(BUSINESS_REGISTRATION, BUSINESS_REGISTRATION_CERTIFICATE, COMPANY_PROFILE, FINANCIAL_STATEMENT, LICENSE, 이 순서)이 된다. POST `/member-documents` 에 `language`(KO, EN)를 받는다. 국문·영문 칸(BUSINESS_REGISTRATION_CERTIFICATE, COMPANY_PROFILE)에 language 가 없으면 400(언어를 고르라는 안내), 다른 칸에 오면 무시하고 비워 둔다. 같은 칸·같은 언어로 다시 올리면 그 언어 서류만 바뀐다(BIZ-065). PUT `/member-documents/{id}` 는 그 서류의 분류와 언어를 그대로 둔다. GET `/member-documents` 응답의 `fixedSlots[]` 는 칸마다 `files[]` 를 낸다. `files[]` 한 줄은 `{ language(KO, EN 또는 null), document(서류 또는 null) }` 이고, 국문·영문 칸은 KO, EN 두 줄, 다른 칸은 language null 한 줄이다. MemberDocumentDto 에 `language` 를 더한다. auto-fill 처럼 분류로 서류 하나를 고를 때 국문·영문 칸은 EN 을 먼저, 없으면 KO 를 고른다(BIZ-065). 근거: 원문 "서류함 필요리스트", 사용자 10/11 20:01 /전승(정할 것 3)
