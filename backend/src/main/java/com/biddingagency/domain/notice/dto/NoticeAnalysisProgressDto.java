package com.biddingagency.domain.notice.dto;

import com.biddingagency.integration.llmplatform.dto.WorkflowRunResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 공고분석 진행 STEP 표시용 응답 DTO.
 *
 * 화면(공고문 상세, ANALYZING 동안)이 "지금 몇 번째 단계인지"를 사람이 읽는 라벨로 보여주기 위함.
 * Aimbase run 조회 응답(currentStep/currentStepName/steps[])을 그대로 화면 친화 형태로 전달.
 *
 * Aimbase 미반영(steps[] null) 기간에도 화면이 동작하도록, BE 가 fallback 으로 step 목록을 합성한다:
 *   - 전체 step 순서·라벨은 STEP_LABELS(공고분석 WF 정의와 동일 순서) 로 합성
 *   - 각 step status 는 run.stepResults(완료) + currentStep(진행 중) 로 도출
 * Aimbase 가 steps[] 를 내려주면 그걸 우선 사용(진실원천 = WF 정의).
 */
public record NoticeAnalysisProgressDto(
        String generationStatus,   // 우리 쪽 상태 (PENDING/ANALYZING/COMPLETED/FAILED)
        String currentStepId,      // Aimbase run.currentStep (없으면 null)
        String currentStepName,    // 사람이 읽는 현재 단계명 (없으면 null)
        List<StepProgress> steps   // 전체 단계 순서 + 완료여부
) {

    public record StepProgress(String id, String name, String status) {}

    /** 단계 상태 값 */
    public static final String COMPLETED = "completed";
    public static final String RUNNING = "running";
    public static final String PENDING = "pending";
    public static final String FAILED = "failed";

    /**
     * 공고분석 WF(opportunity-analysis) step id → 사람 라벨.
     * 진실원천은 Aimbase WF 정의 step.name. 이 맵은 Aimbase 가 steps[]/currentStepName 을
     * 아직 안 내려줄 때만 쓰는 fallback (WF 정의와 라벨·순서 동일하게 유지).
     */
    private static final Map<String, String> STEP_LABELS = new LinkedHashMap<>();
    static {
        STEP_LABELS.put("fetch_opportunity", "공고 정보 불러오는 중");
        STEP_LABELS.put("write_description", "공고 본문 준비 중");
        STEP_LABELS.put("download_attachments", "첨부파일 받는 중");
        STEP_LABELS.put("extract_facts", "공고 내용 분석 중");
        STEP_LABELS.put("verify_and_gapcheck", "분석 결과 검증 중");
        STEP_LABELS.put("save", "저장 중");
    }

    /**
     * run 응답 + 우리 generationStatus 로 진행 DTO 조립.
     *
     * @param generationStatus 우리 Notice.generationStatus 이름 (PENDING/ANALYZING/...)
     * @param run              Aimbase run 조회 결과 (null 가능 — 미조회/실패 시)
     */
    public static NoticeAnalysisProgressDto of(String generationStatus, WorkflowRunResponse run) {
        if (run == null) {
            // run 미조회: 단계 라벨만(전부 pending) 내려 화면이 체크리스트 골격은 그릴 수 있게
            return new NoticeAnalysisProgressDto(generationStatus, null, null, skeletonSteps());
        }

        String currentStepId = run.getCurrentStep();
        String currentStepName = run.getCurrentStepName();

        // 1) Aimbase 가 steps[] 를 내려줬으면 그대로 사용 (진실원천 = WF 정의)
        List<StepProgress> steps = fromAimbaseSteps(run.getSteps());

        // 2) 없으면 BE 가 합성 (fallback)
        if (steps == null) {
            steps = synthesize(run, currentStepId);
        }

        // currentStepName 폴백: Aimbase 가 안 주면 id 로 라벨 매핑
        if (currentStepName == null && currentStepId != null) {
            currentStepName = STEP_LABELS.get(currentStepId);
        }

        return new NoticeAnalysisProgressDto(generationStatus, currentStepId, currentStepName, steps);
    }

    /** Aimbase steps[] (List&lt;Map&gt;) → StepProgress 목록. 비거나 null 이면 null 반환(합성으로 폴백). */
    private static List<StepProgress> fromAimbaseSteps(List<Map<String, Object>> aimbaseSteps) {
        if (aimbaseSteps == null || aimbaseSteps.isEmpty()) {
            return null;
        }
        List<StepProgress> out = new ArrayList<>();
        for (Map<String, Object> s : aimbaseSteps) {
            out.add(new StepProgress(
                    str(s.get("id")),
                    str(s.get("name")),
                    str(s.get("status"))
            ));
        }
        return out;
    }

    /**
     * stepResults(완료) + currentStep(진행 중) + terminal 여부로 각 단계 status 합성.
     * - stepResults 에 key 존재 → completed
     * - currentStep 과 일치 → terminal 이면 그 종료상태, 아니면 running
     * - 그 외 → pending
     */
    private static List<StepProgress> synthesize(WorkflowRunResponse run, String currentStepId) {
        Map<String, Object> stepResults = run.getStepResults();
        boolean failed = run.isFailed();
        List<StepProgress> out = new ArrayList<>();
        for (Map.Entry<String, String> e : STEP_LABELS.entrySet()) {
            String id = e.getKey();
            String status;
            if (stepResults != null && stepResults.containsKey(id)) {
                status = COMPLETED;
            } else if (id.equals(currentStepId)) {
                status = failed ? FAILED : RUNNING;
            } else {
                status = PENDING;
            }
            out.add(new StepProgress(id, e.getValue(), status));
        }
        return out;
    }

    /** run 정보 없을 때 단계 골격(전부 pending). */
    private static List<StepProgress> skeletonSteps() {
        List<StepProgress> out = new ArrayList<>();
        for (Map.Entry<String, String> e : STEP_LABELS.entrySet()) {
            out.add(new StepProgress(e.getKey(), e.getValue(), PENDING));
        }
        return out;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
