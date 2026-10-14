package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.PieeFetchStatus;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * TC-PIEE-011 ~ TC-PIEE-015 (CR-160): 수집이 PIEE 입찰서류 받기를 거는 조건(BIZ-079, POL-040)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpportunityServicePieeTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 13, 0);

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private OpportunityAttachmentRepository attachmentRepository;
    @Mock
    private AttachmentAutoDownloadService attachmentAutoDownloadService;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private PieeSettings pieeSettings;
    @InjectMocks
    private OpportunityService opportunityService;

    @BeforeEach
    void setUp() {
        given(pieeSettings.enabled()).willReturn(true);
        given(pieeSettings.now()).willReturn(NOW);
        given(pieeSettings.noticeUrl(anyString())).willAnswer(inv -> {
            String solNo = inv.getArgument(0);
            return solNo == null || solNo.isBlank() ? Optional.empty() : Optional.of("https://piee/" + solNo);
        });
        given(opportunityRepository.findByNoticeId(anyString())).willReturn(Optional.empty());
        given(opportunityRepository.save(any(Opportunity.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private Opportunity collect(String noticeId, String solNo, String type, String contentHash) {
        return opportunityService.createOrUpdate(noticeId, solNo, "title", type, "org",
                NOW, NOW.plusDays(30), "http://ui", "http://desc", Map.of(), contentHash, null, null).opportunity();
    }

    // TC-PIEE-011
    @Test
    @DisplayName("새공고_받기걸음")
    void 새공고_받기걸음() {
        Opportunity opp = collect("N-1", "W91QVN26QA030", "Solicitation", "h1");

        assertThat(opp.getPieeFetchStatus()).isEqualTo(PieeFetchStatus.WAITING);
        assertThat(opp.getPieeFetchStatusAt()).isEqualTo(NOW);
    }

    // TC-PIEE-012
    @Test
    @DisplayName("낙찰공지_받기안걸음")
    void 낙찰공지_받기안걸음() {
        Opportunity opp = collect("N-2", "W91QVN26QA030", "Award Notice", "h1");

        assertThat(opp.getPieeFetchStatus()).isNull();
    }

    // TC-PIEE-013
    @Test
    @DisplayName("공고번호빈공고_받기안걸음")
    void 공고번호빈공고_받기안걸음() {
        Opportunity opp = collect("N-3", " ", "Solicitation", "h1");

        assertThat(opp.getPieeFetchStatus()).isNull();
    }

    // TC-PIEE-014
    @Test
    @DisplayName("내용바뀐공고_다시걸음")
    void 내용바뀐공고_다시걸음() {
        Opportunity existing = Opportunity.builder().noticeId("N-4").solicitationNumber("W91QVN26QA030")
                .title("old").type("Solicitation").contentHash("old").active(true)
                .firstSeenAt(NOW).lastModifiedAt(NOW).build();
        ReflectionTestUtils.setField(existing, "pieeFetchStatus", PieeFetchStatus.DONE);
        given(opportunityRepository.findByNoticeId("N-4")).willReturn(Optional.of(existing));

        Opportunity opp = collect("N-4", "W91QVN26QA030", "Solicitation", "new");

        assertThat(opp.getPieeFetchStatus()).isEqualTo(PieeFetchStatus.WAITING);
    }

    // TC-PIEE-015
    @Test
    @DisplayName("설정꺼짐_받기안걸음")
    void 설정꺼짐_받기안걸음() {
        given(pieeSettings.enabled()).willReturn(false);

        Opportunity opp = collect("N-5", "W91QVN26QA030", "Solicitation", "h1");

        assertThat(opp.getPieeFetchStatus()).isNull();
    }
}
