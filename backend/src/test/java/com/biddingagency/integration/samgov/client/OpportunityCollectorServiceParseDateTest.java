package com.biddingagency.integration.samgov.client;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OpportunityCollectorServiceParseDateTest {

    @Test
    void 서울시각_마감_서울벽시계그대로() {
        assertThat(OpportunityCollectorService.parseDate("2026-11-03T13:00:00+09:00"))
                .isEqualTo(LocalDateTime.of(2026, 11, 3, 13, 0));
    }

    @Test
    void 미국시각_마감_서울시각으로옮겨짐() {
        assertThat(OpportunityCollectorService.parseDate("2026-03-05T16:30:00-04:00"))
                .isEqualTo(LocalDateTime.of(2026, 3, 6, 5, 30));
    }

    @Test
    void 날짜만_게시일_자정() {
        assertThat(OpportunityCollectorService.parseDate("2026-10-11"))
                .isEqualTo(LocalDateTime.of(2026, 10, 11, 0, 0));
    }
}
