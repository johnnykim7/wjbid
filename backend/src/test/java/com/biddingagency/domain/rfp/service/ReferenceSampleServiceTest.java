package com.biddingagency.domain.rfp.service;

import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.entity.Outcome;
import com.biddingagency.domain.rfp.entity.RfpSample;
import com.biddingagency.domain.rfp.entity.RfpSampleFile;
import com.biddingagency.domain.rfp.repository.RfpSampleFileRepository;
import com.biddingagency.domain.rfp.repository.RfpSampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * ReferenceSampleService 테스트 (CR-014).
 * PWS 제외, downloadUrl 포맷, 미분류(null) 빈 결과 검증.
 */
@ExtendWith(MockitoExtension.class)
class ReferenceSampleServiceTest {

    @Mock
    private RfpSampleRepository rfpSampleRepository;
    @Mock
    private RfpSampleFileRepository fileRepository;

    @InjectMocks
    private ReferenceSampleService referenceSampleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(referenceSampleService, "selfBaseUrl", "http://test:8183/api");
    }

    @Test
    @DisplayName("유형매칭_PWS파일제외_제안서파일만수집")
    void collect_pws제외() {
        UUID sampleId = UUID.randomUUID();
        UUID propFileId = UUID.randomUUID();
        UUID pwsFileId = UUID.randomUUID();

        RfpSample sample = RfpSample.builder()
                .opportunityNo("W90VN725RA012")
                .industryType(IndustryType.GROUND_MAINTENANCE)
                .outcome(Outcome.WON)
                .company("녹화창조")
                .build();
        ReflectionTestUtils.setField(sample, "id", sampleId);

        RfpSampleFile propFile = RfpSampleFile.builder()
                .rfpSample(sample).fileName("FACTOR 3.PAST PERFORMANCE.pdf")
                .contentType("application/pdf").isPws(false).build();
        ReflectionTestUtils.setField(propFile, "id", propFileId);

        RfpSampleFile pwsFile = RfpSampleFile.builder()
                .rfpSample(sample).fileName("PWS.pdf")
                .contentType("application/pdf").isPws(true).build();
        ReflectionTestUtils.setField(pwsFile, "id", pwsFileId);

        given(rfpSampleRepository.findByIndustryTypeAndUseForPatternTrue(IndustryType.GROUND_MAINTENANCE))
                .willReturn(List.of(sample));
        given(fileRepository.findByRfpSampleId(sampleId))
                .willReturn(List.of(propFile, pwsFile));

        List<Map<String, Object>> result = referenceSampleService.collect(IndustryType.GROUND_MAINTENANCE);

        assertThat(result).hasSize(1);
        Map<String, Object> s = result.get(0);
        assertThat(s.get("opportunityNo")).isEqualTo("W90VN725RA012");
        assertThat(s.get("outcome")).isEqualTo("WON");
        assertThat(s.get("company")).isEqualTo("녹화창조");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) s.get("files");
        // PWS 제외 → 제안서 파일 1개만
        assertThat(files).hasSize(1);
        assertThat(files.get(0).get("fileName")).isEqualTo("FACTOR 3.PAST PERFORMANCE.pdf");
        assertThat(files.get(0).get("downloadUrl"))
                .isEqualTo("http://test:8183/api/mcp/rfp-files/" + propFileId + "/download");
    }

    @Test
    @DisplayName("null유형_빈결과")
    void collect_null_빈결과() {
        List<Map<String, Object>> result = referenceSampleService.collect(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("매칭샘플없음_빈결과")
    void collect_샘플없음_빈결과() {
        given(rfpSampleRepository.findByIndustryTypeAndUseForPatternTrue(IndustryType.PIPELINE))
                .willReturn(List.of());
        List<Map<String, Object>> result = referenceSampleService.collect(IndustryType.PIPELINE);
        assertThat(result).isEmpty();
    }
}
