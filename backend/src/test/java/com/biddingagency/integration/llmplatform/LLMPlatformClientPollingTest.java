package com.biddingagency.integration.llmplatform;

import com.biddingagency.config.LLMPlatformConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 결과 묻기 간격(IT-14-028). 간격은 배포되는 설정(src/main/resources/application.yml)에서 읽는다.
 * 시험용 설정은 운영 값이 아니라서 쓰지 않는다
 */
class LLMPlatformClientPollingTest {

    private static final String BASE = "http://aimbase.test";
    private static final String RUN_URL = BASE + "/api/v1/workflows/proposal-design/run";
    private static final String POLL_URL = BASE + "/api/v1/workflows/proposal-design/runs/run-1";

    @Test
    @DisplayName("IT-14-028 4 설계워크플로우_결과를5초간격으로물음")
    void 결과묻기_5초간격() {
        LLMPlatformConfig config = new LLMPlatformConfig();
        ReflectionTestUtils.setField(config, "connectTimeout", 1000);
        ReflectionTestUtils.setField(config, "readTimeout", 1000);
        RestTemplate rest = config.llmPlatformRestTemplate();
        MockRestServiceServer aimbase = MockRestServiceServer.bindTo(rest).build();
        LLMPlatformClient client = new LLMPlatformClient(rest);
        ReflectionTestUtils.setField(client, "baseUrl", BASE);
        ReflectionTestUtils.setField(client, "proposalDesignWorkflowId", "proposal-design");
        ReflectionTestUtils.setField(client, "pollingIntervalMs", deployedInterval());
        ReflectionTestUtils.setField(client, "maxPollingAttempts", 3);

        List<Long> askedAt = new ArrayList<>();
        for (String[] step : new String[][] {{RUN_URL, "running"}, {POLL_URL, "running"}, {POLL_URL, "completed"}}) {
            aimbase.expect(requestTo(step[0])).andRespond(request -> {
                askedAt.add(System.currentTimeMillis());
                return withSuccess(LLMPlatformClientTest.run(step[1]), MediaType.APPLICATION_JSON).createResponse(request);
            });
        }

        client.runProposalDesign(Map.of("documentId", "d-1"));

        List<Long> gaps = List.of(askedAt.get(1) - askedAt.get(0), askedAt.get(2) - askedAt.get(1));
        assertThat(gaps).allSatisfy(gap -> assertThat(gap).isBetween(5000L, 5500L));
    }

    private static long deployedInterval() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));
        String raw = yaml.getObject().getProperty("app.aimbase.polling.interval-ms");
        return Long.parseLong(new StandardEnvironment().resolvePlaceholders(raw));
    }
}
