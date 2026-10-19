# 06. 본문 작성 (청크 단위, HTML 도식, DOCX)

**역할**: 섹션 기획서와 목업대로 실제 본문을 작성하고 DOCX로 생산한다.
**입력**: 섹션 기획서 전체, Winning Theme, 컴플라이언스 매트릭스
**산출물**: `6-proposal/sections/*.md`, `6-proposal/diagrams/*`, `6-proposal/proposal-final.docx`
**필수 선행 독서**: `00-writing-style.md` + docx 스킬(있으면)

## 청크 실행 규칙 (균등 깊이의 핵심)

1. 목차를 **소목차 3~4개 단위 청크**로 나눈다
2. **매 청크 시작 시 반드시 다시 읽는다**: ① `00-writing-style.md`
   ② 해당 섹션 기획서들 ③ Winning Theme·Hot Button (전략서 6장)
3. 청크 작성 → `sections/chunk-N.md` 저장 → 직전 청크와 분량·깊이 비교, 편차 크면 즉시 보강
4. 청크 완료마다 `python scripts/check_coverage.py`로 해당 섹션의 REQ 커버 확인
5. 모든 청크 완료 후 DOCX 조립

## 첫 청크 = Executive Summary

ES는 본문보다 먼저, 4-Box + Hot Button 구조로 작성한다:
- Box 1: Winning Theme → 고객 비전 → 연결 문장 (Theme을 첫 문장에)
- Box 2: Hot Button 소개 (우선순위 순)
- Box 3: HB별 Benefit → Solution → Proof (+ 비주얼 1개)
- Box 4: 요약 + Next Step (Theme을 마지막 문장에 — 수미상관)
- 분량 A4 1~2p. 고객 주어, 정량 표현. EB/TB/User 각각에게 설득력 있는지 자체 검증

## 본문 작성 (섹션별)

- 목업의 페이지 배치를 그대로 따른다 (표제 → 주제문 → 그래픽 → 본문 포인트 전개)
- 4-Box 구조, 6대 문장 원칙, 어투 규칙(주제문=합쇼체 완결문, 근거=개조식) 적용
- 각 섹션 첫머리에 대응 REQ ID를 HTML 주석으로 심는다: `<!-- REQ-003, REQ-007 -->`
  (커버리지 자동 검증용 — DOCX 변환 시 주석은 제거)
- 작성 후 카피라이팅 체크리스트(00번 파일 §5)로 자체 점검 — 특히 번역투·추상어

## HTML 도식화 파이프라인

섹션 기획서의 그래픽 기획을 구현한다. **HTML 원본을 보존해 사용자가 텍스트만 고쳐
재캡처할 수 있게 하는 것이 핵심 설계다.**

1. 유형 선택 → `assets/diagram-library/`에서 해당 템플릿 복사:

| 유형 | 템플릿 | 쓰임 |
|---|---|---|
| 변화형 | as-is-to-be.html | 현황→개선, Before/After |
| 비교형 | vs-compare.html | 자사 vs 타방식, 대안 비교 |
| 구조도 | architecture.html | 시스템/조직 계층 구조 |
| 프로세스 | process-steps.html | 단계·절차·방법론 |
| 시계열 | timeline.html | 일정, 로드맵, 마일스톤 |
| 병렬형 | parallel-cards.html | 3~4개 대등 요소 (전략, 기능) |

2. `bid-project/6-proposal/diagrams/D-{목차번호}-{이름}.html`로 저장하고
   템플릿의 텍스트 슬롯을 내용으로 교체 (구조·스타일은 유지, 슬롯 수는 가감 가능)
3. 캡처: `python scripts/capture_diagram.py diagrams/D-1.1-arch.html`
   → 같은 이름의 `.png` 생성 (폭 992px 고정 = A4 본문 폭 168mm, 2x 캡처, 높이는 내용에 따라 자동)
   - 도식 폰트는 본문(12pt)과 어울리도록 설계됨(아이템 ~11pt, 제목 ~15-17pt) — 임의 축소 금지
   - DOCX 삽입: `width: 635` 고정, `height: Math.round(635 * imgH / imgW)` (PNG 실측 비율 사용)
   - 템플릿을 브라우저에서 열면 텍스트를 더블클릭해 바로 수정 가능(수정 후 재캡처)
4. 캡처 실패 시(브라우저 미설치 등): HTML을 그대로 산출물로 전달하고
   사용자에게 브라우저 캡처를 안내한다. **파이프라인 실패로 작업을 멈추지 않는다**
5. 모든 도식에 Action Caption을 붙인다 (도식 하단이 아닌 DOCX 본문에서)

핵심 컨셉 일러스트가 필요하면 도식 템플릿 스타일(색/폰트)을 유지한 커스텀 HTML/SVG로 제작한다.

## 간지(장 표지) 파이프라인

각 장(Ⅰ, Ⅱ…) 시작마다 풀블리드 간지를 넣는다. 도식과 같은 HTML→PNG 방식이다.

1. `assets/diagram-library/ganji-template.html`을 `diagrams/ganji-{N}-{프로파일}.html`로 복사
2. 텍스트 슬롯 교체: {Ⅰ}(장 번호), {사업의 이해}(장 제목), 장 내 목차 항목, {사업명}
   - 테마: `<body class="ent">`(기업, 네이비+틸) / `<body class="pub">`(공공, 차콜 톤다운)
3. `python scripts/capture_diagram.py`로 캡처 (A4 비율 1240×1754 → 2x 캡처 = 300dpi급)
4. DOCX에서 간지는 **머리글·바닥글 없는 별도 섹션**으로 넣고, 이미지는
   794×1123px floating(`relative: PAGE, offset 0, behindDocument: true`)으로 전면 배치

## DOCX 조립 — AX 표준 서식 v2

1. docx 스킬이 있으면 그 안내서를 먼저 읽는다. 공고의 작성 지시(Section L, 상업 품목은 FAR 52.212-1 에 따른 지시)가 서식을 정하면 아래 표보다 그 지시가 먼저다.
2. **스타일을 즉흥 정의하지 않는다.** `assets/templates/docx-style.js`를 작업 폴더로 복사해 require한다:
   - `buildStyles(docx, profile)` · `buildNumbering(docx, profile)` · `tableBorders(docx)` · `headlineBox(docx, profile, text)`
   - 프로파일: `'public'`(공공 입찰, 완전 무채) | `'enterprise'`(기업 제안, Navy 1색)
   - 조립 코드 전체 예시는 `assets/templates/docx-build-reference.js` 참조 (표지 프레임·목차·간지 섹션·개조식·표·박스 포함)
3. 문서 구조: 표지(페이지 프레임, 별도 섹션) → 목차 → [장마다: 간지 섹션 → 본문] → 부록
4. 서식 규격 (docx-style.js에 코드로 고정되어 있음 — 임의 변경 금지):

| 요소 | 규격 |
|---|---|
| 판형·여백 | A4, 상20/하18/좌22/우20mm, 본문 폭 168mm(9525 DXA) |
| 폰트 | 전체 Pretendard (도식·간지와 동일 폰트) |
| 본문 | 12pt(공공)/11pt(기업), **줄간격 130%**, 양쪽 정렬 |
| 개조식 | □→○→− 3계층 numbering, 레벨별 크기·굵기 차등(□ 굵게 12.5 > ○ 11.5 > − 10.5 진회색) |
| 절 제목 | 음영 밴드형(공공 회색/기업 네이비 흰글자), 음영색 동일 보더로 상하 패딩 7pt |
| 주제문 | `headlineBox()` — 무테 음영 박스, 본문 폭과 정확히 정렬 |
| 표 | 상하 굵은 외곽선(1.5pt)만·좌우 무선, 헤더 회색 음영+굵게, 캡션 `< 표 N >` 상단 |
| 그림 | 폭 168mm(635px) 또는 반폭, 캡션 `< 그림 N >` 하단, Action Caption 필수 |
| 강조 | 굵게(+밑줄 허용), 색 강조 금지, 페이지당 3회 이내 |
| 쪽번호 | 바닥글 가운데 `- N -`, 간지는 쪽번호 없음 |

5. 도식 PNG 삽입 + Action Caption
6. **렌더 검증 루프(필수)**: soffice→pdf→pdftoppm→이미지 확인. 체크: 표 폭 깨짐,
   도식-캡션 페이지 분리, 페이지 끝 고아 제목, 박스-본문 정렬
7. **Pretendard 배포 규칙**: 수신자 PC에 폰트가 없을 수 있으므로 대외 제출은 PDF(폰트 임베드) 병행 산출이 필수. DOCX는 내부 편집용
8. RFP 지정 서식(표지·폰트·분량)이 있으면 이 표준 전체보다 **무조건 우선**
9. 완성본(docx+pdf)은 워크플로우가 정한 저장 도구로 넘긴다

작업량이 큰 경우 청크별로 사용자와 중간 점검하며 진행한다.
