# CR-124 — Customer Portal UI 전면 개편 (Design System 도입)

> 상태: **설계** (2026-06-29) · 규모: **대규모** (전 화면 + DS 도입 + 데이터모델 변경)
> 원칙: CLAUDE.md "설계 우선" — 본 문서 + T3-3/T3-1/CR이력 갱신 후 코드 착수

---

## 1. 배경 / 목표

- 현재 customer-portal UI를 **전면 교체**한다.
- 컨셉 정본: `docs/ui-mockup/customer-portal-military-poc.html` (낙점) — **상단 GNB형 + 나라장터식 차분한 톤 + Noto Sans KR 폰트 + 풀 밀리터리(네이비) 리브랜딩**.
- **메인(랜딩)은 현재 운영 HomePage 유지** (poc 메인은 폐기). 독수리 네이비 히어로 유지.
- 구현은 **Design System(`@therecommerce-develop/ds-ui`) 컴포넌트로 재구축**. (DS 원칙: 소비앱은 DS만 import, 메트로닉 직접 복사 금지)

### 폐기 (mock 비교용이었던 나머지 2종)
- `customer-portal-lnb.html` (GNB+LNB), `customer-portal-sidebar.html` (사이드바 단독) — 미채택. 참고용으로만 보존.

---

## 2. Design System 도입 (실측 기반)

| 항목 | 값 |
|------|-----|
| 패키지 | `@therecommerce-develop/ds-ui` (현재 v0.3.0) |
| 레지스트리 | GitHub Packages — `.npmrc` + `GITHUB_TOKEN` |
| 설치 정본 레퍼런스 | `bp-backoffice2` (`.npmrc` 복사) |
| CSS | entry에 `import '@therecommerce-develop/ds-ui/dist/style.css'` |
| 테마 | `globals.css @theme inline` CSS 변수. `themes/banpoom.css` 패턴 → **WJbid 네이비 테마(wjbid.css)** 신규 |
| 스택 호환 | React 19 / Tailwind 4.1 / Radix / TanStack / lucide — 현 customer-portal과 일치 ✅ |

### 우리 화면에 직결되는 DS 컴포넌트 (실측 확인)
- **layout**: Header(로고+로그인슬롯), **CategoryNav `variant='horizontal'`**(=상단 GNB), Footer, Breadcrumb
- **composite**: DataGrid, FilterPanel, PageHeader, StatSummary, StatusFlow(FSM), Stepper, **Timeline**, Pagination, SearchInput, ConfirmDialog
- **base**: Button, Input, Select, Checkbox, Badge, Dialog, DropdownMenu, Tabs, Accordion(=FAQ), Calendar(=입찰캘린더), Avatar, Card

### DS에 없어 자체 구현/포팅 필요 (확인)
- 건당 과금 요금 카드(랜딩 pricing 패턴 참고), 법적 정책 페이지(텍스트), 면책/환불 — 자체 콘텐츠
- 차트(대시보드) → DS chart 미포팅 시 자체 또는 후속

---

## 3. 최종 IA (메뉴 리스트) — 확정본

범례: ✅기존 · 🆕신규 · ⚖️법적필수

### A. 비로그인 GNB (공개)
| 메뉴 | 경로 |
|------|------|
| 홈(랜딩) | `/` ✅ |
| 서비스 소개 | `/#about` ✅ |
| 입찰 프로세스 | `/#process` ✅ |
| 이용 안내(가이드) | `/guide` ✅ |
| 요금 안내 | `/pricing` ✅(건당 과금 개편) |
| 공지사항 | `/notices` 🆕 |
| FAQ | `/faq` 🆕 |
| 고객센터(FAQ+문의폼) | `/support` 🆕 |
| 로그인 / 회원가입 | `/login` `/register` ✅ |

### B. 로그인 후 GNB 대분류 + 하위
| 대분류 | 하위 | 경로 |
|--------|------|------|
| 공고 검색 | 전체 공고 검색 | `/search` ✅ |
| | └ 공고 상세 | `/search/:id` ✅ |
| | 마감 임박 | `/search?deadline` 🆕 |
| | 관심 공고 | `/bookmarks` ✅ |
| | 입찰 캘린더 | `/calendar` 🆕 |
| | 저장한 검색/알림 구독 | `/saved-searches` 🆕 |
| 내 제안서 | 제안서 목록 | `/proposals` ✅ |
| | └ 상세/편집 | `/proposals/:id` ✅ |
| | 진행 현황(FSM) | `/proposals?progress` 🆕 |
| 서류함 | 제출 서류 슬롯 | `/documents` 🆕 |
| 알림 | 알림 목록 | `/notifications` ✅ |

### C. 계정 (우측 유저 드롭다운)
| 메뉴 | 경로 |
|------|------|
| 기업 프로필 | `/profile` ✅ |
| 결제 내역 | `/billing` 🆕 |
| 로그아웃 | — ✅ |

### D. ⚖️ 법적·정책 (푸터 + 가입 동의)
| 메뉴 | 경로 | 노출 |
|------|------|------|
| 이용약관 | `/terms` 🆕⚖️ | 푸터 + 가입 필수동의 |
| 개인정보처리방침 | `/privacy` 🆕⚖️ | 푸터 + 가입 필수동의 |
| 환불 정책 | `/refund` 🆕⚖️ | 푸터 + 결제화면 |
| 면책 고지 | `/disclaimer` 🆕⚖️ | 푸터 + 요금화면 |
| 사업자 정보 | 푸터 텍스트 | 상호·대표·사업자번호·주소(전상법 표기) |

집계: 기존 12 + 신규 ~18 = **약 30 화면**

---

## 4. 회원가입 / 회원 정책 (확정)

### 가입 필수 필드 (핵심 4 + 인증)
이메일 · 비밀번호 · **회사명 · 사업자등록번호 · 담당자명 · 연락처**

> BE Member 엔티티 실측: email/password_hash/company_name(필수), business_registration_number/contact_person/phone/address(nullable) **이미 존재**.
> → 변경: business_registration_number·contact_person·phone을 가입 단계 **필수 검증**으로 (DB NOT NULL은 기존 데이터 고려해 BE 검증 레벨로).

### 가입 후 프로필에서 입력
주소 · **UEI · CAGE 코드** (SAM.gov 입찰 필수 식별자)

### 데이터모델 변경 (Flyway 신규 마이그레이션)
`member` 테이블에 컬럼 추가:
- `uei` VARCHAR(20) NULL — SAM.gov Unique Entity ID
- `cage_code` VARCHAR(10) NULL — CAGE 코드

### 가입 동의 (필수 2 + 선택 1)
- ☑ 이용약관 동의 (필수)
- ☑ 개인정보처리방침 동의 (필수)
- ☐ 마케팅 정보 수신 (선택)

---

## 5. 법적 페이지 정책 (4개 개별 페이지)

`/terms` `/privacy` `/refund` `/disclaimer` 각각 독립 페이지.

- **면책 고지(/disclaimer)** 핵심 문구: "본 서비스는 낙찰을 보장하지 않으며, 입찰서 최종 제출 및 서명은 고객의 책임입니다."
- **환불 정책(/refund)**: 건당 과금 기준 — 분석/제안서 단계별 환불 가능 시점·불가 시점 명시 (실제 문구는 운영 확정).
- **사업자 정보**: 푸터에 상호·대표자·사업자등록번호·주소·통신판매업신고번호 (전자상거래법 표기 의무).

> ⚠️ 법적 문구 원문은 **운영/법무 확정 필요**. 설계 단계에선 페이지 골격 + 플레이스홀더.

---

## 6. 고객센터 (`/support`)
- FAQ 아코디언(DS Accordion) + 1:1 문의 폼 (이름·이메일·문의유형·내용 → BE 접수 또는 메일)
- `/faq`는 FAQ 단독, `/support`는 FAQ+문의폼 (FAQ 콘텐츠 공유)

---

## 7. 작업 순서 (구현 단계 — 설계 커밋 후)

### Phase 0 — 설계 (현재)
1. 본 문서 + T3-3 화면구조 + T3-1 데이터모델(member uei/cage) + CR_변경_이력 갱신
2. 설계 문서 **먼저 커밋**

### Phase 1 — DS 기반 셋업
- `.npmrc` 추가(GitHub Packages), `@therecommerce-develop/ds-ui` 설치
- `style.css` import, **wjbid 네이비 테마** CSS 변수 정의 (Noto Sans KR)
- 공통 셸: Header + CategoryNav(GNB) + Footer(법적링크+사업자정보) + Breadcrumb

### Phase 2 — 공개 화면
- 메인(현 HomePage 유지·DS 토큰만 적용) / 요금(건당) / 가이드 / 공지 / FAQ / 고객센터
- 로그인 / **회원가입(법인 필드 + 동의)**

### Phase 3 — 로그인 후 화면
- 공고검색(DataGrid)·상세 / 마감임박 / 관심공고 / 캘린더 / 저장검색
- 제안서 목록·상세·진행현황(StatusFlow) / 서류함 / 알림 / 프로필(UEI·CAGE) / 결제내역

### Phase 4 — 법적 페이지
- /terms /privacy /refund /disclaimer (골격 + 플레이스홀더)

### Phase 5 — 검증
- 라우트/화면 레벨 교차검증, npm run dev(:3183) 렌더 확인, build 통과

---

## 8. 미확정 / 운영 확인 필요
- 건당 과금 **실제 단가** (분석/제안서/볼륨)
- 법적 문구 **원문** (약관·개인정보·환불·면책 — 법무 확정)
- 사업자 정보 실값 (상호·대표·등록번호·신고번호)
- DoD/VA/GSA 로고 사용 가부 (현재 모노톤 텍스트로 대체)
- DS 버전 0.3.0에 차트 포함 여부 (대시보드 차트 필요 시)

---

## 부록 — 핵심 실측 근거 (CLAUDE.md 소스 실측 원칙)
- BE Member 필드: `backend/.../domain/member/entity/Member.java` (email/password_hash/company_name/business_registration_number/contact_person/phone/address)
- 현 라우트: `frontend/customer-portal/src/App.tsx` (12 라우트)
- DS 컴포넌트: `design-system/packages/ui/src/components/{base,composite,layout,template}`
- DS 소비 패턴: `bp-backoffice2/.npmrc` + `package.json`
- 컨셉 mock: `docs/ui-mockup/customer-portal-military-poc.html`
