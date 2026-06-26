# CR-122 — opportunity-analysis `extract_facts` 분리 설계

> 작성 2026-06-26. 새 논의 없이, 분리 토론에서 **합의된 결론만** 모은 설계서.
> 본 문서는 결정 확정용이며, 코드(WF 정의) 변경 전 사용자 승인 게이트.

---

## 1. 배경 — 왜 분리하나 (실측 근거)

현재 `opportunity-analysis` WF의 STEP3 `extract_facts`는 **단일 AGENT_CALL** (connection=`cli-runner-bidding-001`, Claude CLI 자율주행)이 다음을 한 호출 안에서 수행:

1. 첨부 N개 + `description.txt` **전부 순차 Read** (프롬프트가 "1~20p씩 끝까지, 재독 금지" 지시)
2. 10개 항목 분석 (overview/scope/contractPeriod/siteVisit/eligibility/evaluationCriteria/keyDates/budgetInfo/specialNotes/contactInfo)
3. 제출서류 FACTOR>Subfactor 계층 + 충족주체 분류
4. 모든 fact에 근거(sourceFile/sourceQuote/page/confidence) 부착

### 실측으로 확인된 문제 (정본)

- **대용량 공고는 단일 컨텍스트 한계(1M 토큰)를 초과한다.**
  - 표본: opportunity `9E97ECE1...`(11개 첨부, 73.3MB). 핵심 첨부 텍스트 추출 토큰 실측:
    | 첨부 | 페이지 | 추출 텍스트 | 추정 토큰 |
    |---|---|---|---|
    | SOW | 18p | 6.5만 자 | ~16K |
    | SUBMITTAL | 54p | 32만 자 | ~82K |
    | Elec_B(전기사양) | 645p | 177만 자 | **~443K** |
    | As_built 도면1(35MB) | 81p | 242만 자 | **~605K** |
  - 이 4개만 합쳐 ~1,147K → **이미 1M 초과**. 첨부는 11개라 전부 Read 시 한참 초과.
  - 결과: 컨텍스트 압축 발동 → 앞서 읽은 디테일 손실 → 정확도 붕괴 / 또는 타임아웃.
- **작은 공고(예: P-6120, Solicitation 텍스트 8K 토큰)는 토큰 여유** — 단일 컨텍스트로 충분. 환각 0건(41 fact sourceQuote 전수 대조 결과 전부 원문 실재).
  - 즉 **분리는 작은 공고 정확도 개선용이 아니라, 대용량 공고를 처리 가능하게 만드는 필수 구조.**

### 폐기된 오판 (재발 방지)

- ❌ "공고는 토큰 여유롭다" — 작은 공고만 보고 일반화한 오류. 대용량은 1M 초과(실측).
- ❌ "extract_facts는 네이티브 도구 봉인(AIMBASE 모드)" — 틀림. **Claude CLI 자율주행**이라 Read/Grep/Glob 등 네이티브 도구 열려 있음. 프롬프트가 "전체 순차 Read"로 몰아넣을 뿐.

---

## 2. 합의된 방향

### 2.1 책임 경계
- **분할 제어 메커니즘(Claude CLI subagent 옵션 동작 여부)은 Aimbase 플랫폼이 푼다.** → Aimbase 쪽에서 정리 완료(사용자 보고): **CLI 어댑터 사용 시 subagent 옵션에 따라 동작하도록 처리됨.**
- **bidding-agency(소비앱)가 할 일** = 그 위에서 `opportunity-analysis` WF 정의를 분리 구조로 재작성 + 백업.

### 2.2 핵심 원칙 (토론 수렴점)
> **분할(=보장이 필요한 부분)은 WF 구조가 강제하고, 분할된 조각 내 추출(=똑똑함이 필요한 부분)은 자율주행이 담당.**
- 자율주행의 "내부 subagent를 알아서 띄울지"는 컨트롤 불가 → **그 보장을 자율주행에 맡기지 않는다.**
- 대신 **각 agent_call에 넘기는 입력을 좁혀서** 자율주행이 못 터지게 한다. (좁은 조각이면 내부 subagent를 띄워도 안전 → 봉인 불필요.)

---

## 3. 작업 항목

### 3.1 백업 (완료)
- 운영 현행 WF 백업: `backend/docs/aimbase-workflows/backup/opportunity-analysis.backup.json`
  - updatedAt 2026-06-20, 6 STEP 전체 보존. 문제 시 이 파일로 즉시 롤백 PUT.

### 3.2 분리 축 — **A(자료/파일축) 확정** ✅

| 안 | 분할 축 | 대용량 해소 | 항목 정확도 | 구현 | 결정 |
|---|---|---|---|---|---|
| **A. 자료(파일)축** | 첨부+description을 FOREACH로 분할, 파일별 agent_call → 파일별 fact 추출 후 머지 | ◎ 확실 | △ 항목이 파일 가로지르면 머지 필요 | 쉬움(download_attachments가 이미 FOREACH) | **★ 채택** |
| B. 항목축 | 10항목을 2~3그룹으로 나눔 | △ 각 그룹도 전체 Read하면 또 터짐 | ◎ | 중 | 기각(대용량 미해소) |
| C. 읽기/분석 단계 | 위치맵 생성 + 맵 기반 타겟 추출 2단계 | ◎ | ◎ | 무거움 | 보류(미래 보강 후보) |

**A 채택 근거 (실측):**
- 단일 컨텍스트 1M 한계 → 파일 수만큼 1M씩 분산. 73.3MB·11파일 케이스(645p 전기사양 ~443K + 35MB 도면 ~605K + …) 처리 가능.
- **단일 파일 1M 초과 위험 = 운영에 없음**: 최대 단일 파일 33.8MB(도면, 텍스트 적음)도 ~600K 수준. 미래 1M 넘는 단일 파일 들어오면 C(페이지 범위 재분할) 보강 — 지금 불필요.
- download_attachments가 이미 FOREACH라 구조 재사용 → verify/save 정합성 위험 최소.

### 3.3 분리 설계 (확정)

**각 파일 에이전트는 10항목 전원 공유, 자기 파일 1개만 Read** (= 항목 정의는 공유, 자료만 분할):
- FOREACH `items={{input.analysisTargets}}`, `mode=parallel`, `max_concurrency=4`, `collect=append`, `on_item_error=continue`.
- 각 에이전트: `{{item.targetPath}}` 한 파일만 읽고 10항목 관점으로 fact 추출(없는 항목은 비움), sourceFile=담당 파일.
- 에이전트 간 통신 없음 — append가 파일별 fact를 기계적으로 모음(항목 라벨 key는 각 에이전트가 부착).

**머지 = verify가 흡수** (새 STEP 불필요):
- append로 모인 배열엔 같은 key가 여러 파일에서 중복 가능(제출서류=SOW+SUBMITTAL).
- STEP4 verify 프롬프트에 **항목(key)별 통합 지시** 추가 — ①중복 합치되 근거 보존 ②보완 정보 묶음 ③충돌 시 권위 원문(본문>도면) 채택. 최종 summary/requiredDocuments는 단일 항목 구조.

**save = 무변경**: save는 `{{verify_and_gapcheck.structured_data.*}}`만 참조 → verify가 최종 정형화하므로 분리 영향이 extract→verify 경계에만 국한.

### 3.4 BE 변경 (1건, 소규모)
- `NoticeService` analyze input 맵에 **`analysisTargets` 추가**: `[{targetPath:"description.txt"}, {targetPath:"attachments/<fileName>"} × N]`.
  - 기존 `buildAttachmentFiles`(SUCCESS 첨부)에 description.txt 1건을 앞에 붙여 구성.
  - 기존 `attachmentFiles`/`opportunityText` 키는 유지(STEP1·2가 그대로 사용).

### 3.5 산출물
- WF 정의 v3: `docs/workflows/opportunity-analysis.steps.v3.json` (생성 완료, PUT용 8필드만).
- WF PUT 시 **WorkflowController 8필드만** (CR-112: GET 응답 그대로 PUT 시 _meta/projectId/graphMode/active/updatedAt 등 unknown property 400).

### 3.6 FOREACH 출력 형태 — 실측 확정 (ForeachStepExecutor.java)
- `collect=append` 시 `{{extract_facts.output}}` = **각 파일 body 결과의 List(배열)**. 각 원소 = `{...body출력..., status:"ok"|"failed", index:N}`. (`ForeachStepExecutor.java:60,167-174`)
- ⚠️ 단일→배열로 바뀌므로 **verify 프롬프트가 그대로면 입력 형태를 못 알아봄** → **보정 완료**: verify 프롬프트에 "이전 STEP은 파일별 분석 결과 배열, 모든 원소를 훑어 취합 후 머지" 안내 추가.

### 3.7 미결정 (PUT 전 확인)
- [ ] **Aimbase subagent 옵션** — 분리된 각 agent_call에서 subagent ON/OFF (좁은 조각이라 ON이어도 안전, 비용·세션 판단). Aimbase 측 정리됨(어댑터 옵션 따라 동작) → 운영 설정값 확인 필요.
- [ ] **E2E 실측** — 대용량 공고(예: opp `9E97ECE1...` 73.3MB·11파일) 1건으로 v3 실행 → ①컨텍스트 안 터지는지 ②verify가 배열 취합·머지 정상인지 ③save 결과 항목별 단일 구조인지 확인.

---

## 4. 규모·절차
- **규모**: 중규모 (WF 구조 변경, 새 화면·API·테이블 없음. BE/FE/데이터모델/마이그레이션 무변경).
- **절차**: 본 설계서로 분리 축 확정 → WF 정의 v3 작성(`docs/workflows/opportunity-analysis.steps.v3.json`) → 사용자 승인 → Aimbase WF PUT → E2E(대용량 공고 1건으로 1M 초과 안 터지는지 실측) → CR-122 이력 등록.
- **백업 보존**: 문제 시 `backup/opportunity-analysis.backup.json`으로 즉시 롤백 PUT.
