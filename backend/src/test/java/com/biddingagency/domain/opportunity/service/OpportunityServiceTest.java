package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-BROWSE-001 ~ TC-BROWSE-005, TC-OPP-002 ~ TC-OPP-003: OpportunityService 테스트
 */
@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @InjectMocks
    private OpportunityService opportunityService;

    // TC-BROWSE-001: 공고 목록 페이징
    @Test
    @DisplayName("페이징조회_page0_size20_20건반환")
    void 페이징_findAllActive_20건반환() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        List<Opportunity> opportunities = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            opportunities.add(Opportunity.builder()
                    .noticeId("NOTICE-" + i)
                    .title("Test Opportunity " + i)
                    .active(true)
                    .firstSeenAt(LocalDateTime.now())
                    .lastModifiedAt(LocalDateTime.now())
                    .build());
        }
        Page<Opportunity> page = new PageImpl<>(opportunities, pageable, 50);
        given(opportunityRepository.findByActiveTrue(pageable)).willReturn(page);

        // when
        Page<Opportunity> result = opportunityService.findAllActive(pageable);

        // then
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getTotalElements()).isEqualTo(50);
    }

    // TC-BROWSE-002: 키워드 검색
    @Test
    @DisplayName("키워드검색_maintenance_포함건만반환")
    void 키워드_searchByKeyword_해당건만반환() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Opportunity opp = Opportunity.builder()
                .noticeId("N-001")
                .title("Building maintenance contract")
                .active(true)
                .firstSeenAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();
        Page<Opportunity> page = new PageImpl<>(List.of(opp), pageable, 1);
        given(opportunityRepository.searchByTitle("maintenance", pageable)).willReturn(page);

        // when
        Page<Opportunity> result = opportunityService.searchByKeyword("maintenance", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("maintenance");
    }

    // TC-BROWSE-003: 마감 임박 조회
    @Test
    @DisplayName("마감임박_7일이내_해당공고만반환")
    void 마감7일이내_findNearDeadline_해당건반환() {
        // given
        Opportunity opp = Opportunity.builder()
                .noticeId("N-002")
                .title("Near deadline opportunity")
                .responseDeadline(LocalDateTime.now().plusDays(3))
                .active(true)
                .firstSeenAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();
        given(opportunityRepository.findNearDeadline(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of(opp));

        // when
        List<Opportunity> result = opportunityService.findNearDeadline(7);

        // then
        assertThat(result).hasSize(1);
    }

    // TC-BROWSE-004: 공고 상세 조회
    @Test
    @DisplayName("유효ID_상세조회_공고반환")
    void 유효한ID_findById_공고반환() {
        // given
        UUID id = UUID.randomUUID();
        Opportunity opp = Opportunity.builder()
                .noticeId("N-003")
                .title("Test Opportunity")
                .active(true)
                .firstSeenAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();
        given(opportunityRepository.findById(id)).willReturn(Optional.of(opp));

        // when
        Opportunity result = opportunityService.findById(id);

        // then
        assertThat(result.getNoticeId()).isEqualTo("N-003");
    }

    // TC-BROWSE-005: 존재하지 않는 공고
    @Test
    @DisplayName("무효ID_상세조회_NotFound예외")
    void 무효ID_findById_예외() {
        // given
        UUID id = UUID.randomUUID();
        given(opportunityRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> opportunityService.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Opportunity not found");
    }

    // TC-OPP-002: 중복 공고 검출 (동일 contentHash → 갱신)
    @Test
    @DisplayName("동일contentHash_createOrUpdate_갱신안함_BIZ005")
    void 동일해시_createOrUpdate_갱신하지않음() {
        // given
        Opportunity existing = Opportunity.builder()
                .noticeId("N-DUP")
                .title("Original Title")
                .contentHash("hash123")
                .active(true)
                .firstSeenAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();
        given(opportunityRepository.findByNoticeId("N-DUP")).willReturn(Optional.of(existing));

        // when
        Opportunity result = opportunityService.createOrUpdate(
                "N-DUP", "SOL-001", "Updated Title", "type", "org",
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                "http://link", "http://desc", Map.of("key", "value"), "hash123", // 동일 해시
                null
        );

        // then - 제목이 변경되지 않음 (동일 해시)
        assertThat(result.getTitle()).isEqualTo("Original Title");
    }

    // TC-OPP-002 변형: contentHash 다르면 갱신
    @Test
    @DisplayName("다른contentHash_createOrUpdate_갱신됨")
    void 다른해시_createOrUpdate_갱신됨() {
        // given
        Opportunity existing = Opportunity.builder()
                .noticeId("N-DUP2")
                .title("Original Title")
                .contentHash("old_hash")
                .active(true)
                .firstSeenAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();
        given(opportunityRepository.findByNoticeId("N-DUP2")).willReturn(Optional.of(existing));

        // when
        Opportunity result = opportunityService.createOrUpdate(
                "N-DUP2", "SOL-002", "Updated Title", "type", "org",
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                "http://link", "http://desc", Map.of("key", "value"), "new_hash",
                null
        );

        // then - 제목이 갱신됨
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getContentHash()).isEqualTo("new_hash");
    }

    // TC-OPP-003: 원본 JSON 보존
    @Test
    @DisplayName("수집응답_rawJson_원본보존_BIZ004")
    void 신규공고_createOrUpdate_rawJson보존() {
        // given
        Map<String, Object> rawJson = Map.of("notice_id", "N-RAW", "full", "data");
        given(opportunityRepository.findByNoticeId("N-RAW")).willReturn(Optional.empty());
        given(opportunityRepository.save(any(Opportunity.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Opportunity result = opportunityService.createOrUpdate(
                "N-RAW", "SOL-003", "Title", "type", "org",
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                "http://link", "http://desc", rawJson, "hash_raw",
                null
        );

        // then
        assertThat(result.getRawJson()).isEqualTo(rawJson);
        assertThat(result.getRawJson().get("notice_id")).isEqualTo("N-RAW");
    }
}
