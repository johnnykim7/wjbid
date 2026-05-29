package com.biddingagency.domain.rfp.service;

import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.entity.RfpSample;
import com.biddingagency.domain.rfp.entity.RfpSampleFile;
import com.biddingagency.domain.rfp.repository.RfpSampleFileRepository;
import com.biddingagency.domain.rfp.repository.RfpSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 공고유형에 매칭되는 성공 제안서 원본의 메타 + 다운로드 URL 수집 (CR-014).
 * MCP get_reference_samples(Aimbase 파싱용)와 작성 워크플로우 P3(build3PipelineContext) 양쪽이 공유.
 * PWS(공고문)는 제안서 참조 대상이 아니므로 제외.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceSampleService {

    private final RfpSampleRepository rfpSampleRepository;
    private final RfpSampleFileRepository fileRepository;

    @Value("${app.self-base-url:http://59.8.160.12:8183/api}")
    private String selfBaseUrl;

    /**
     * 해당 유형의 성공 제안서 목록(각 원본 파일 메타 + 다운로드 URL) 수집.
     * @return rfpSampleId/opportunityNo/outcome/company/files[fileId,fileName,contentType,downloadUrl]
     */
    public List<Map<String, Object>> collect(IndustryType industryType) {
        if (industryType == null) return List.of();

        List<Map<String, Object>> samples = new ArrayList<>();
        for (RfpSample sample : rfpSampleRepository.findByIndustryTypeAndUseForPatternTrue(industryType)) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("rfpSampleId", sample.getId().toString());
            s.put("opportunityNo", sample.getOpportunityNo());
            s.put("outcome", sample.getOutcome().name());
            if (sample.getCompany() != null) s.put("company", sample.getCompany());

            List<Map<String, Object>> files = new ArrayList<>();
            for (RfpSampleFile f : fileRepository.findByRfpSampleId(sample.getId())) {
                if (f.isPws()) continue; // 공고문(PWS)은 제안서 참조 대상 아님
                Map<String, Object> fm = new LinkedHashMap<>();
                fm.put("fileId", f.getId().toString());
                fm.put("fileName", f.getFileName());
                fm.put("contentType", f.getContentType());
                fm.put("downloadUrl", selfBaseUrl + "/mcp/rfp-files/" + f.getId() + "/download");
                files.add(fm);
            }
            s.put("files", files);
            samples.add(s);
        }
        return samples;
    }
}
