package com.biddingagency.domain.notice.repository;

import com.biddingagency.config.JpaConfig;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-OPP-024 ~ 026 (CR-188): 테스트 공고의 공고문은 고객 목록에서 빠지고 번호로는 열린다 (BIZ-099).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class NoticeRepositoryTest {

    private static final LocalDateTime POSTED = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    private Notice collectedNotice;
    private Notice testNotice;

    @BeforeEach
    void setUp() {
        collectedNotice = noticeRepository.save(visibleNotice(opportunityRepository.save(opportunity("N-SAM", false))));
        testNotice = noticeRepository.saveAndFlush(visibleNotice(opportunityRepository.save(opportunity("N-TEST", true))));
    }

    private static Opportunity opportunity(String noticeId, boolean test) {
        return Opportunity.builder()
                .noticeId(noticeId)
                .title("title " + noticeId)
                .postedDate(POSTED)
                .active(true)
                .firstSeenAt(POSTED)
                .lastModifiedAt(POSTED)
                .testNotice(test)
                .build();
    }

    private static Notice visibleNotice(Opportunity opportunity) {
        return Notice.builder()
                .opportunity(opportunity)
                .generationStatus(NoticeGenerationStatus.COMPLETED)
                .visibility(OpportunityVisibility.VISIBLE)
                .build();
    }

    // TC-OPP-024
    @Test
    @DisplayName("공개공고문목록_테스트공고_빠짐")
    void 공개공고문목록_테스트공고_빠짐() {
        List<Notice> notices = noticeRepository
                .findVisible(OpportunityVisibility.VISIBLE, true, POSTED, Pageable.unpaged()).getContent();

        assertThat(notices).extracting(Notice::getId).containsExactly(collectedNotice.getId());
    }

    // TC-OPP-025
    @Test
    @DisplayName("최근공고문_테스트공고_빠짐")
    void 최근공고문_테스트공고_빠짐() {
        List<Notice> notices = noticeRepository.findVisibleRecentlyPosted(POSTED.minusDays(1), OpportunityVisibility.VISIBLE);

        assertThat(notices).extracting(Notice::getId).containsExactly(collectedNotice.getId());
    }

    // TC-OPP-026
    @Test
    @DisplayName("테스트공고문_번호로열기_그대로열림")
    void 테스트공고문_번호로열기_그대로열림() {
        assertThat(noticeRepository.findByIdAndVisibility(testNotice.getId(), OpportunityVisibility.VISIBLE)).isPresent();
    }
}
