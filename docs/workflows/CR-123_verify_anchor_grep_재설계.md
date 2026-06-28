# CR-123: 공고분석 verify anchor 기반 grep 재설계

> 작성: 2026-06-28 · 규모: 중(WF 정의 + extract/verify 프롬프트 재설계, BE 무변경 예상) · 선행: CR-122(extract FOREACH 분리), 11c2ce7(verify 입력 structured_data 교체)

## 1. 배경 (실측)

CR-122 로 extract_facts 를 파일별 FOREACH 로 분리하고, 11c2ce7 로 verify 입력을 `{{extract_facts.structured_data}}` 로 교체해 context 238% 초과를 해소했다(E2E SMYU 26-1050 run 6102b7eb 통과). 그러나 verify_and_gapcheck(AGENT_CALL) **검증 방식 자체가 비효율·불완전**하다.

### 실측 데이터 (run 6102b7eb, SMYU 26-1050, 12파일)

- extract 원본 fact = **1,840개**
- 각 fact 의 `sourceFile` 채움: **1,840 / 1,840** (전수)
- 각 fact 의 `page` 채움: **1,840 / 1,840** (전수)
- 각 fact 의 `sourceQuote` 채움: **0 / 1,840** (전부 빈값)
- verify 출력 fact = **12개** (1,840 → 12 로 축소)
- verify 소요 391s, 자율탐색 중 12파일 일부만 열람(샘플링)

### 진단

각 fact 는 **이미 좌표(sourceFile + page)와 value 를 들고 있다.** 그런데 verify(AGENT_CALL)는 이 메타를 쓰지 않고:

1. `ls`/`find` 로 작업장을 맨땅부터 탐색 (fact 에 파일명이 박혀 있는데 불필요)
2. 큰 파일을 통째/앞부분 Read (grep 아님 → 컨텍스트 낭비 + 뒷부분 누락)
3. 12파일 중 일부만 열람 → 나머지 검증 안 함 (샘플링)
4. 1,840 → 12 로 과도 축소

원인: verify 프롬프트가 "작업장 원문을 직접 열어 대조하라"고 **자율 지시**할 뿐, fact 의 좌표(sourceFile/page)를 쓰라거나 grep 하라고 명시하지 않음. CLI 는 시키는 대로 파일을 열어 읽음.

또 하나: extract 프롬프트는 [출력 최소화=시간] 원칙으로 sourceQuote(원문 인용)를 **의도적으로 비운다**(원문 통째 복사 금지). 그래서 "sourceQuote 를 grep" 하는 기계 검증은 **현 구조에서 성립하지 않는다**(grep 대상이 없음).

## 2. 핵심 통찰

환각검증의 본질 = "이 value 가 원문에 실제 근거가 있나". extract 는 **그 fragment 원문을 보면서** fact 를 뽑으므로, 그 순간 근거가 눈앞에 있다. 그때 fact 에 **검증 가능한 anchor(원문 영어 단어/짧은 구절)** 를 같이 남기면, verify 는 "그 anchor 가 그 파일에 진짜 있나" 라는 단순 **기계 grep** 으로 줄어든다.

- anchor 는 **영어 원문 5~10단어** (통째 인용 아님 → 출력 증가 제한적)
- anchor 가 영어라 영어 원문 파일에 그대로 grep 됨 → value 가 한국어여도 한↔영 매칭 문제 없음
- extract 가 이미 본 걸 받아쓰는 것 → extra LLM 비용 거의 없음

이로써 사용자가 처음 제시한 "코드 grep 정석"이 비로소 성립한다(이전엔 anchor 가 없어 불가능했음).

## 3. 결정

### 3.1 extract 재설계

각 fact 에 `anchor` 필드 추가:
- 정의: 이 fact 의 근거가 되는 **원문(영어) 그대로의 5~10단어 구절**. 고유명사·숫자·핵심 영어 문구 우선.
- 통째 인용 금지(출력 단축 원칙 유지) — anchor 는 검증용 닻이지 인용이 아님.
- 도면·이미지·스캔(텍스트 레이어 없음) fact 는 anchor 를 비울 수 있음(grep 불가 → confidence 로 구분).
- sourceFile/page 는 이미 전수 채워지므로 유지.

### 3.2 verify 재설계 (2-track)

**Track 1 — 환각검증 = 코드(기계) grep**
- 각 fact 의 `anchor` 를 `sourceFile` 에 `grep -F`(고정 문자열) 매칭
- 있으면 근거 확인(통과) / 없으면 환각 표시
- 1,840개 **전수** 검증, 샘플링·축소 없음, LLM 불필요
- anchor 빈 fact(도면 등)는 grep 스킵 → confidence/별도 표식으로 처리

**Track 2 — gap-check = 경량 LLM (유지)**
- 환각검증(grep)으로 거를 수 없는 "원문엔 있는데 추출에서 빠진 항목" 점검
- USFK 필수항목 체크리스트(마감일/PIEE/계약기간/평가기준/FACTOR/POC/현장설명회)로 누락만 점검
- 최종 summary·requiredDocuments 통합 생성도 이 단계에서
- extract 가 이미 전수추출 → gap 은 적을 것 → 입력·시간 작음

### 3.3 grep 수행 위치 (실측 근거)

작업장(description.txt, attachments/)은 **Aimbase 컨테이너 내부**다. BE(LLMPlatformClient)는 WF 실행/폴링만 하고 작업장 파일에 접근하지 않는다(실측: backend 에 작업장 파일 접근 코드 없음). 따라서 **anchor grep 은 WF 안(Aimbase 도구)에서 수행**해야 한다. BE 무변경.

구현 후보(구현 단계에서 Aimbase 도구 역량으로 확정):
- (a) verify 를 파일별 FOREACH 로 분리, 각 STEP 이 자기 파일 fact 의 anchor 를 그 파일에 grep (extract 와 대칭)
- (b) verify AGENT 가 grep 도구로 fact 별 매칭하되 "맨땅 ls/find 금지, 각 fact 좌표로 직행" 명시
- (c) WF 에 bash/grep TOOL_CALL STEP 추가 (Aimbase WorkflowEngine 자체 구현, 메모리상 bash 도구 보유)

→ Track1(grep)과 Track2(gap-check LLM)를 어떻게 STEP 으로 나눌지는 구현 시 확정. 환각검증이 기계화되므로 verify LLM 부하는 gap-check 로 한정된다.

## 4. 변경 범위

| 영역 | 변경 | 비고 |
|------|------|------|
| WF 정의 v4 | extract output_schema 에 `anchor` 추가 + extract 프롬프트 anchor 추출 지시 / verify 를 grep 검증 + gap-check 로 재구성 | docs/workflows/opportunity-analysis.steps.v4.json |
| extract 프롬프트 | anchor(원문 영어 5~10단어) 추출 지시 추가, 통째인용 금지 유지 | 출력 소폭 증가 → extract 시간 소폭 증가 감수 |
| verify 프롬프트/구조 | 환각검증 grep 전환 + gap-check 한정 | 자율 ls/find/통째Read 금지 명시 |
| BE | 없음(예상) | 작업장 접근은 WF 측, save 콜백 무변경 |
| 데이터 모델 | 없음 | extracted_fact 에 anchor 저장 여부는 선택(검증용이라 미저장 가능) |
| 화면/API | 없음 | |

## 5. 미결 (구현 단계 확정)

1. grep STEP 구조 (4.3 의 a/b/c 중) — Aimbase 도구 역량 실측 후 확정
2. anchor 빈 fact(도면) 처리 정책 — confidence=MEDIUM 유지 + grep 스킵
3. extract 출력 증가량·시간 영향 측정 (E2E 로 anchor 추가 전후 비교)
4. 환각으로 판정된 fact 의 처리 — 제거 vs 표식 후 보존

## 6. 검증 (DoD)

- E2E(SMYU 26-1050 재실행): extract fact 의 anchor 채움률 측정(텍스트 fact 기준 高)
- verify 가 1,840개 **전수** grep(샘플링 없음) 확인 — run 상세로 검증 항목 수 대조
- 환각검증이 grep 으로 동작(anchor 없는 가짜 fact 를 심어 색출되는지)
- gap-check 가 누락 항목만 점검(전체 재추출 아님) 확인
- 최종 summary/requiredDocuments 정상 저장(회귀 없음)
