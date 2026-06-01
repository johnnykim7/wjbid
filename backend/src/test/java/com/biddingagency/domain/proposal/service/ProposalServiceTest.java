package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.*;
import com.biddingagency.domain.proposal.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-027: ProposalService 테스트.
 * 영속화 골격의 핵심 불변식 — block 교체 시 기존 전량 삭제 + DRAFTED 전이, LOCKED 안전장치, section FSM.
 */
@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock private ProposalChapterRepository chapterRepository;
    @Mock private ProposalSectionRepository sectionRepository;
    @Mock private ProposalBlockRepository blockRepository;
    @Mock private VerificationLogService verificationLogService;

    @InjectMocks private ProposalService proposalService;

    private ProposalSection pendingSection(UUID id) {
        ProposalSection s = ProposalSection.builder()
                .chapterId(UUID.randomUUID())
                .status(SectionStatus.PENDING)
                .orderNo(0)
                .build();
        // BaseEntity.id 는 @UuidGenerator 라 테스트에서 직접 못 박음 → stub 으로 조회만 맞춤
        return s;
    }

    // ── block 교체 ───────────────────────────────────────

    @Test
    @DisplayName("block교체_기존삭제후신규저장_DRAFTED전이")
    void replaceBlocks_기존삭제후저장_DRAFTED() {
        // given
        UUID sectionId = UUID.randomUUID();
        ProposalSection section = pendingSection(sectionId);
        given(sectionRepository.findById(sectionId)).willReturn(java.util.Optional.of(section));
        given(blockRepository.findBySectionIdOrderByOrderNoAsc(sectionId)).willReturn(List.of());
        // CR-031: 정형 룰 통과(findings 비어있음) → DRAFTED 유지
        given(verificationLogService.evaluateSectionRules(anyInt(), anyInt(), any())).willReturn(List.of());

        List<ProposalService.BlockInput> inputs = List.of(
                new ProposalService.BlockInput(BlockType.PARAGRAPH, Map.of("type", "doc"), null),
                new ProposalService.BlockInput(BlockType.TABLE, Map.of("type", "table"), null)
        );

        // when
        proposalService.replaceBlocks(sectionId, inputs);

        // then
        then(blockRepository).should().deleteBySectionId(sectionId);
        then(blockRepository).should(times(2)).save(any(ProposalBlock.class));
        assertThat(section.getStatus()).isEqualTo(SectionStatus.DRAFTED);
    }

    @Test
    @DisplayName("block교체_LOCKED섹션_차단됨")
    void replaceBlocks_LOCKED섹션_예외() {
        // given
        UUID sectionId = UUID.randomUUID();
        ProposalSection locked = ProposalSection.builder()
                .chapterId(UUID.randomUUID()).status(SectionStatus.LOCKED).orderNo(0).build();
        given(sectionRepository.findById(sectionId)).willReturn(java.util.Optional.of(locked));

        // when / then
        assertThatThrownBy(() -> proposalService.replaceBlocks(sectionId, List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOCKED");
        then(blockRepository).should(never()).deleteBySectionId(any());
    }

    // ── section FSM ──────────────────────────────────────

    @Test
    @DisplayName("LOCKED섹션_markVerified시도_차단됨")
    void markVerified_LOCKED_예외() {
        UUID sectionId = UUID.randomUUID();
        ProposalSection locked = ProposalSection.builder()
                .chapterId(UUID.randomUUID()).status(SectionStatus.LOCKED).orderNo(0).build();
        given(sectionRepository.findById(sectionId)).willReturn(java.util.Optional.of(locked));

        assertThatThrownBy(() -> proposalService.markVerified(sectionId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("잠금해제_LOCKED에서DRAFTED복귀")
    void unlock_LOCKED에서DRAFTED() {
        UUID sectionId = UUID.randomUUID();
        ProposalSection locked = ProposalSection.builder()
                .chapterId(UUID.randomUUID()).status(SectionStatus.LOCKED).orderNo(0).build();
        given(sectionRepository.findById(sectionId)).willReturn(java.util.Optional.of(locked));

        proposalService.unlockSection(sectionId);

        assertThat(locked.getStatus()).isEqualTo(SectionStatus.DRAFTED);
    }

    @Test
    @DisplayName("잠금해제_LOCKED아님_예외")
    void unlock_NOT_LOCKED_예외() {
        UUID sectionId = UUID.randomUUID();
        ProposalSection drafted = ProposalSection.builder()
                .chapterId(UUID.randomUUID()).status(SectionStatus.DRAFTED).orderNo(0).build();
        given(sectionRepository.findById(sectionId)).willReturn(java.util.Optional.of(drafted));

        assertThatThrownBy(() -> proposalService.unlockSection(sectionId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("섹션없음_조회시_예외")
    void getSection_없음_예외() {
        UUID sectionId = UUID.randomUUID();
        given(sectionRepository.findById(sectionId)).willReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> proposalService.getSection(sectionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ── 생성 ─────────────────────────────────────────────

    @Test
    @DisplayName("section생성_PENDING상태로저장")
    void createSection_PENDING으로저장() {
        UUID chapterId = UUID.randomUUID();
        given(sectionRepository.save(any(ProposalSection.class))).willAnswer(inv -> inv.getArgument(0));

        ProposalSection created = proposalService.createSection(
                chapterId, "A", "Prior Experience", "작성 지침", List.of("REQ-001"), 500, 0);

        assertThat(created.getStatus()).isEqualTo(SectionStatus.PENDING);
        assertThat(created.getChapterId()).isEqualTo(chapterId);
    }
}
