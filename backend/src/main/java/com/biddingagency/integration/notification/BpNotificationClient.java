package com.biddingagency.integration.notification;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * bp-notification 발송 클라이언트 (CR-005/006)
 *
 * 외부 bp-notification 서비스(POST /messages/email)를 호출하여 템플릿 기반 이메일을 발송한다.
 * BIDDING 솔루션의 BIDDING_* / BID_* 템플릿을 templateCode + variables 방식으로 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BpNotificationClient {

    private final RestTemplate bpNotificationRestTemplate;

    @Value("${app.bp-notification.base-url:http://59.8.160.12:8185/api/v1}")
    private String baseUrl;

    /**
     * 템플릿 기반 이메일 발송.
     *
     * @param email        수신자 이메일
     * @param templateCode bp-notification 등록 템플릿 코드 (예: BID_OPPORTUNITY_APPROVED)
     * @param variables    템플릿 치환 변수
     * @return 발송 성공 여부 (bp-notification data.success 기준)
     */
    public SendResult sendEmail(String email, String templateCode, Map<String, Object> variables) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("templateCode", templateCode);
        payload.put("variables", variables != null ? variables : Map.of());

        try {
            ResponseEntity<JsonNode> response = bpNotificationRestTemplate.postForEntity(
                    baseUrl + "/messages/email",
                    new HttpEntity<>(payload),
                    JsonNode.class);

            JsonNode body = response.getBody();
            boolean success = body != null
                    && body.path("success").asBoolean(false)
                    && body.path("data").path("success").asBoolean(false);
            String requestId = body != null ? body.path("data").path("requestId").asText(null) : null;

            if (success) {
                log.info("bp-notification email sent: to={}, template={}, requestId={}",
                        email, templateCode, requestId);
                return new SendResult(true, requestId, null);
            }

            String message = body != null ? body.path("message").asText("unknown") : "empty response";
            log.error("bp-notification email failed: to={}, template={}, message={}",
                    email, templateCode, message);
            return new SendResult(false, requestId, message);

        } catch (Exception e) {
            log.error("bp-notification email call error: to={}, template={}, error={}",
                    email, templateCode, e.getMessage());
            return new SendResult(false, null, e.getMessage());
        }
    }

    /**
     * 발송 결과.
     *
     * @param success      bp-notification 발송 성공 여부
     * @param requestId    bp-notification 발송 추적 ID (실패 시 null 가능)
     * @param errorMessage 실패 사유 (성공 시 null)
     */
    public record SendResult(boolean success, String requestId, String errorMessage) {
    }
}
