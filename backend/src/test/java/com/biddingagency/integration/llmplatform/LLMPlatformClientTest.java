package com.biddingagency.integration.llmplatform;

import com.biddingagency.config.LLMPlatformConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 문서 작성 중 전환이 띄우는 설계 워크플로우의 실행 요청과 결과 묻기 요청(IT-14-028).
 * 열쇠 머리글은 운영과 같은 설정 클래스가 만든 RestTemplate 으로 본다
 */
class LLMPlatformClientTest {

    private static final String BASE = "http://aimbase.test";
    private static final String KEY = "plat-test-key";
    private static final String RUN_URL = BASE + "/api/v1/workflows/proposal-design/run";
    private static final String POLL_URL = BASE + "/api/v1/workflows/proposal-design/runs/run-1";

    private MockRestServiceServer aimbase;
    private LLMPlatformClient client;

    @BeforeEach
    void setUp() {
        LLMPlatformConfig config = new LLMPlatformConfig();
        ReflectionTestUtils.setField(config, "apiKey", KEY);
        ReflectionTestUtils.setField(config, "tenantId", "bidding_system");
        ReflectionTestUtils.setField(config, "connectTimeout", 1000);
        ReflectionTestUtils.setField(config, "readTimeout", 1000);
        RestTemplate rest = config.llmPlatformRestTemplate();
        aimbase = MockRestServiceServer.bindTo(rest).build();

        client = new LLMPlatformClient(rest);
        ReflectionTestUtils.setField(client, "baseUrl", BASE);
        ReflectionTestUtils.setField(client, "proposalDesignWorkflowId", "proposal-design");
        ReflectionTestUtils.setField(client, "pollingIntervalMs", 10L);
        ReflectionTestUtils.setField(client, "maxPollingAttempts", 3);
    }

    @Test
    @DisplayName("IT-14-028 5 설계워크플로우_실행요청과결과묻기_XApiKey실림")
    void 실행요청과결과묻기_열쇠머리글() {
        aimbase.expect(requestTo(RUN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", KEY))
                .andRespond(withSuccess(run("running"), MediaType.APPLICATION_JSON));
        aimbase.expect(requestTo(POLL_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", KEY))
                .andRespond(withSuccess(run("completed"), MediaType.APPLICATION_JSON));

        client.runProposalDesign(Map.of("documentId", "d-1"));

        aimbase.verify();
    }

    static String run(String status) {
        return "{\"success\":true,\"data\":{\"id\":\"run-1\",\"status\":\"" + status + "\"}}";
    }
}
