package com.biddingagency.domain.rfp.dto;

import java.util.Map;

/** 패턴 가이드 수동 편집 요청 (CR-013) → source=HUMAN_EDITED */
public record PatternGuideEditRequest(
        Map<String, Object> guideJson,
        String guideMarkdown
) {}
