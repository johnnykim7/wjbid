package com.biddingagency.domain.compliance.dto;

import java.util.List;
import java.util.UUID;

/**
 * 제안서 문서 하나의 커버리지 검증 (CR-159, BIZ-078). 볼 때마다 계산하고 저장하지 않는다.
 *
 * @param matrixReady    공고에 매트릭스 줄이 있으면 true. false 면 나머지는 비어 있다
 * @param targetCount    대상(Shall 이면서 플랫폼 생성) 줄 수
 * @param fulfilledCount 대상 가운데 이 문서의 절로 충족된 줄 수
 * @param missing        누락 REQ. 매핑 안 된 REQ 는 unmapped 가 true
 * @param unknownIds     절이 들고 있으나 매트릭스에 없는 ID
 */
public record CoverageResult(boolean matrixReady, int targetCount, int fulfilledCount,
                             List<Missing> missing, List<UnknownId> unknownIds, List<SectionRefs> sections) {

    public static CoverageResult notReady() {
        return new CoverageResult(false, 0, 0, List.of(), List.of(), List.of());
    }

    public record Missing(String reqId, String text, boolean unmapped) {
    }

    public record UnknownId(String reqId, UUID sectionId, String sectionTitle) {
    }

    public record SectionRefs(UUID sectionId, List<Ref> refs) {
    }

    public record Ref(String reqId, boolean fulfilled) {
    }
}
