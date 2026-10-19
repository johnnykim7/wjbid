---
name: sam-proposal
description: "AX 수주 제안 스킬의 미 연방 조달(SAM.gov) 판. 공고문/RFP 분석 → Bid/No-Bid 결정 → 제안전략·수행전략 수립 → 목차/스토리 기획 → 섹션 기획서(PDW) → 본문 작성(DOCX+HTML 도식) → 제안서 리뷰까지 7단계 RFP 제안 트랙과, 콜드메일·미팅 제안서·ES(경영 요약)를 다루는 아웃바운드 세일즈 트랙을 실행한다. 사용자가 '제안', '제안서', '수주', '입찰', 'RFP', '제안요청서', '공고문', '과업지시서', 'Go/No-Go', 'Pwin', '제안 전략', 'Winning Theme', '핫버튼', '경쟁사 분석', '목차 기획', '스토리보드', '섹션 기획', 'PDW', '본문 작성', '도식화', '제안서 리뷰', '모의 채점', '콜드메일', '콜드 메일', '아웃바운드', '미팅 제안서', '비즈니스 미팅', 'ES', 'Executive Summary', '경영 요약', '요약문' 등을 언급하거나, '캡처플랜', '캡쳐플랜', 'Capture Plan', '영업기회 조사', '기회 분석'을 언급하거나, '미팅 플래너', '미팅 플랜', '미팅 준비', '고객 미팅', '미팅 기획', '미팅 아젠다', 'SPIN', '질문 세트', '사전 미팅', '미팅 브리프', '미팅 후 정리', '팔로업'을 언급하면 반드시 이 스킬을 사용한다. SAM.gov 공고, Solicitation, RFQ, Section L, Section M, PWS, SOW, Sources Sought, Capability Statement 가 언급되거나 HWP/HWPX 회사 자료가 올라와도 트리거한다. 부분 단계만 요청해도 해당 단계를 실행한다."
---

# AX 수주 제안 스킬 — RFP 제안 트랙 + 아웃바운드 세일즈 트랙

한국 수주 환경판을 미 연방 조달(SAM.gov 공고, 미군 발주 포함)에 맞게 고친 제안 시스템이다.
한국 조달 용어는 아래 「SAM.gov 판 읽기」 표대로 읽는다. 사전 인텔리전스 + 두 트랙으로 구성된다.

- **사전 영업 인텔리전스 (캡처플랜, 11)**: 정식 RFP 이전부터 영업기회(입찰건)를 SAM.gov·FPDS·USAspending·
  웹 리서치로 조사해 사업·발주처·핵심이슈·경쟁사·자사를 구조화한다. 1~3단계의 원재료가 된다.
- **RFP 제안 트랙 (1~7단계)**: 공고/RFP가 있는 공식 입찰을 처음부터 끝까지 처리한다.
- **아웃바운드 세일즈 트랙 (8~10, 12)**: 공고 이전 단계에서 가망 고객을 발굴·설득한다.
  콜드메일(8)로 미팅 제안서(9)를 열게 하고, 미팅이 성사되면 미팅 플래너(12)로
  미팅 실행을 준비·기록한다. 공식 제안 시에는 ES(10)로 요약한다.
  (ES는 6단계 본문의 첫 청크로도 쓰인다.) 미팅 결과는 캡처플랜(11)의 원재료가 된다.

## 전체 프로세스

```
[1. RFP 분석] → [2. Bid/No-Bid] → [3. 전략 수립] → [4. 목차/스토리] → [5. 섹션 기획] → [6. 본문 작성] → [7. 리뷰]
 컴플라이언스      Go/No-Go 보고     수주 전략서       목차 전략표        섹션 기획서       DOCX+도식      모의채점 리포트
 매트릭스 생성                     (제안+수행 2트랙)                    (PDW+목업)
```

**척추 = 컴플라이언스 매트릭스.** 1단계에서 모든 요구사항에 ID(REQ-001…)를 부여하고,
이 ID가 목차 매핑(4) → 섹션 기획서(5) → 본문(6) → 커버리지 검증(7)까지 관통한다.
어느 단계에서든 요구사항을 다룰 때는 반드시 REQ ID로 참조한다.

## 라우팅 규칙

사용자 요청을 분석해 해당 단계의 reference 파일을 **반드시 먼저 읽고** 실행한다.

| 사용자 키워드/상황 | 단계 | 참조 파일 |
|---|---|---|
| 공고문/RFP 업로드, "RFP 분석", "공고 분석", "요구사항 추출", "발주처 분석", "발주 이력", "발주 히스토리", "기존 수행사", "반복 발주", "경쟁사 예상", HWP/HWPX 파일 | 1 | `references/01-rfp-analysis.md` |
| "입찰 참여", "Go/No-Go", "Pwin", "수주 가능성", "들어갈까 말까", "수익성", "리스크" | 2 | `references/02-bid-decision.md` |
| "전략", "제안전략", "수행전략", "Winning Theme", "핫버튼", "포지셔닝", "4방향", "Ghost", "Why Us" | 3 | `references/03-win-strategy.md` |
| "목차", "스토리", "스토리라인", "페이지 배분", "목차 기획" | 4 | `references/04-outline-story.md` |
| "섹션 기획", "PDW", "섹션기획서", "목업", "스토리보드" | 5 | `references/05-section-plan.md` |
| "본문 작성", "제안서 작성", "초안", "도식", "도식화", "ES", "Executive Summary" | 6 | `references/06-writing.md` |
| "캡처플랜", "캡쳐플랜", "Capture Plan", "영업기회 조사", "기회 분석", "입찰건 리서치" | 11 | `references/11-capture-plan.md` |
| "리뷰", "검토", "모의 채점", "Red Team", "개선점", "평가" | 7 | `references/07-review.md` |
| "콜드메일", "콜드 메일", "아웃바운드 메일", "영업 메일", "첫 컨택 메일" | 8 | `references/08-cold-email.md` |
| "미팅 제안서", "비즈니스 미팅", "미팅 잡기", "세일즈 제안서", "면담 제안" | 9 | `references/09-meeting-proposal.md` |
| "미팅 플래너", "미팅 플랜", "미팅 준비", "고객 미팅 기획", "미팅 아젠다", "SPIN", "질문 세트", "미팅 브리프", "미팅 후 정리", "미팅 결과 갱신", "팔로업" | 12 | `references/12-meeting-planner.md` |
| "ES", "Executive Summary", "경영 요약", "요약문", "제안 요약" | 10 | `references/10-executive-summary.md` |
| "전체", "처음부터", "제안서 만들어줘" (RFP만 주고 포괄 요청) | 1부터 순차 | 각 단계 순서대로 |
| "가망고객 발굴부터", "콜드메일+미팅 제안서" (아웃바운드 포괄 요청) | 8→9(→12) 순차 | 08 → 09 순서대로, 미팅 성사 시 12 |

**4~7단계 및 9·10·12 실행 시에는 해당 파일과 함께 `references/00-writing-style.md`를 반드시 읽는다.**
(작성 어투, 4-Box, 주제문, F-B-P, 정보 표제, Action Caption, 카피라이팅 규칙이 여기에 있다.)
콜드메일(8)은 대화체 세일즈 톤이라 00번 대신 08번 파일 자체의 톤 규칙을 따른다.

## SAM.gov 판 읽기

이 묶음의 참고서는 한국 조달 용어로 쓰여 있다. 미 연방 공고에서는 아래처럼 읽는다.

| 참고서의 말 | 미 연방 공고에서 |
|---|---|
| 공고문, 제안요청서(RFP) | Solicitation(RFP, RFQ, IFB)과 수정 공고(Amendment) |
| 과업지시서 | Section C 또는 PWS, SOW |
| 제출 지시사항 | Section L. 상업 품목은 FAR 52.212-1 과 공고의 addendum |
| 평가표, 평가 항목 | Section M. 상업 품목은 FAR 52.212-2 와 공고의 평가 문구 |
| 배점, 고배점 | 평가 요소의 상대 중요도(relative importance). 점수가 적혀 있으면 그 점수 |
| 정량/정성, 기술:가격 비율 | 평가 방식(LPTA, Best Value Tradeoff)과 가격 대비 비가격 요소의 중요도 |
| 발주처 | 계약 부서(Contracting Office, 계약관 CO)와 수요 부대 |
| 나라장터 | SAM.gov Contract Opportunities |
| 발주·낙찰 이력 | SAM.gov 낙찰 공지(Award Notice), FPDS, USAspending |
| 사전규격공개 | Sources Sought, RFI, Draft RFP, Pre-solicitation 공지 |
| 법정 최소 공고 기간 | FAR 5.203. 공고 전 사전 공지 15일, 단순 구매 한도를 넘으면 응답 기간 30일 이상. 상업 품목은 줄일 수 있다 |
| 면허, 등록, 참가 제한 | SAM 등록(UEI, CAGE), NAICS 규모 기준, Set-aside, 공고가 요구하는 면허와 인증 |
| 예산(부가세 포함 여부), 계약 방식 | 정부 추정가(공개된 경우), 계약 형태(FFP, T&M, IDIQ 등) |
| 협상 | Discussions(공고가 협상 단계를 둘 때) |
| PT(발표) | Oral presentation |
| A4 쪽수 | 공고가 정한 용지와 쪽수 한도 |

## 실행 원칙

1. **순차 의존성**: 앞 단계 산출물이 뒷 단계 입력이다. 이전 단계 산출물이 없으면
   사용자에게 알리고, (a) 이전 단계부터 실행하거나 (b) 사용자가 가진 자료로 대체할지 확인한다.
2. **부분 실행 허용**: 특정 단계만 요청하면 그 단계만 실행한다.
3. **상태 파일 유지**: 매 단계 시작 시 `bid-project/_status.md`를 읽고, 완료 시 갱신한다.
   (템플릿: `assets/templates/project-status.md`) 새 프로젝트면 이 파일부터 생성한다.
4. **HITL 게이트**: 각 단계 산출물은 사용자 확인 후 다음 단계로 진행한다.
   특히 Go/No-Go(2), Winning Theme(3), 목차(4)는 이후 모든 작업의 전제이므로 반드시 확인받는다.
5. **언어**: 정부에 내는 제안서 본문은 영어로 쓴다. 분석, 전략, 기획 같은 내부 산출물은 한국어로 쓰고,
   공고 원문은 영어 그대로 인용한다. 도메인 용어(Pwin, Hot Button 등)는 원어 병기 가능.
6. **균등 깊이 원칙**: 여러 항목(섹션, 카드, 목차)을 생산할 때 앞 항목만 상세하고
   뒤로 갈수록 부실해지는 것을 금지한다. 청크를 나눠서라도 모든 항목을 같은 깊이로 만든다.

## 산출물 저장 규칙

모든 산출물은 작업장의 `bid-project/` 아래에 저장한다. 워크플로우가 저장 도구를 정해 주면 그 도구가 먼저다:

```
bid-project/
├── _status.md                     # 프로젝트 상태 (매 단계 갱신)
├── 0-capture/                     # 캡처플랜 (사전 영업 인텔리전스, 11)
│   └── capture-plan-{사업명}.md
├── source/                        # 원본 RFP + 변환본(md)
├── 1-analysis/
│   ├── compliance-matrix.md       # ★요구사항 ID 매트릭스 (척추)
│   └── rfp-analysis-report.md     # 종합 분석 리포트
├── 2-decision/bid-decision.md
├── 3-strategy/win-strategy.md
├── 4-outline/outline-strategy.md  # 목차 전략표
├── 5-section-plans/               # 섹션별 기획서 (S-1.1.md 형식)
├── 6-proposal/
│   ├── sections/                  # 청크별 본문 md
│   ├── diagrams/                  # HTML 도식 원본 + PNG 캡처
│   └── proposal-final.docx        # 최종 산출물
├── 7-review/review-report.md
└── 8-outbound/                     # 아웃바운드 세일즈 트랙 (RFP 없이도 단독 사용)
    ├── cold-email-{타겟사}.md       # 콜드메일 (8)
    ├── meeting-proposal-{타겟사}.docx  # 미팅 제안서 (9)
    └── meeting-planner-{고객사}.md    # 미팅 플래너 (12, 미팅 후 같은 파일 갱신)
```

최종 전달물은 워크플로우가 정한 저장 도구로 넘긴다.

## 도구 (scripts/)

| 스크립트 | 용도 | 사용 시점 |
|---|---|---|
| `scripts/convert_hwp.py` | .hwp/.hwpx → 텍스트/md 변환 | 1단계, HWP 계열 파일 입력 시 |
| `scripts/capture_diagram.py` | HTML 도식 → PNG 캡처 | 6단계, docx 삽입용 |
| `scripts/check_coverage.py` | REQ ID ↔ 본문 커버리지 검증 | 7단계 (및 6단계 청크 완료 시) |

HTML 도식 템플릿 6종은 `assets/diagram-library/`에 있다 (사용법은 06-writing.md 참조).

## DOCX 생성

6단계에서 DOCX를 만들 때는 docx 스킬이 있으면 그 안내서를 먼저 읽고,
**AX 표준 서식 v2**(`assets/templates/docx-style.js` + 06-writing.md §DOCX 조립)를 따른다.
전체 폰트 Pretendard, 줄간격 130%, 개조식 □○− 계층, 무테 음영 주제문 박스,
상하선만 있는 표, 장마다 풀블리드 간지(`assets/diagram-library/ganji-template.html`).
대외 제출은 PDF 병행 산출이 필수다.
공고의 작성 지시(Section L, 상업 품목은 FAR 52.212-1 에 따른 지시)가 폰트, 글자 크기, 여백, 쪽수,
파일 형식을 정하면 그 지시가 이 서식보다 먼저다.
