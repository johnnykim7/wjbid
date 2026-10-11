package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.compliance.entity.FileFormat;
import com.biddingagency.domain.compliance.entity.PreCheckVerdict;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * TC-PRE-007 ~ TC-PRE-017: 형식·쪽수 판정 (CR-141, BIZ-047, BIZ-048, POL-028)
 */
@ExtendWith(MockitoExtension.class)
class PreCheckJudgeTest {

    private static final Set<String> UNITS = Set.of("page", "pages", "p", "pp", "페이지", "쪽", "매");

    @Mock private SystemSettingService systemSettingService;

    @InjectMocks private PreCheckJudge judge;

    @Test
    @DisplayName("TC-PRE-007 형식이름있음_PDF올림_맞음")
    void 형식이름있음_PDF올림_맞음() {
        PreCheckJudge.FormatResult result = judge.judgeFormat("PDF/Word", true, "pdf");

        assertThat(result.verdict()).isEqualTo(PreCheckVerdict.MATCH);
        assertThat(result.allowed()).containsExactly(FileFormat.PDF, FileFormat.WORD);
    }

    @Test
    @DisplayName("TC-PRE-008 형식이름있음_다른형식올림_어긋남")
    void 형식이름있음_다른형식올림_어긋남() {
        assertThat(judge.judgeFormat("PDF", true, "docx").verdict()).isEqualTo(PreCheckVerdict.MISMATCH);
    }

    @Test
    @DisplayName("TC-PRE-009 형식이름없음_올림_사람확인")
    void 형식이름없음_올림_사람확인() {
        PreCheckJudge.FormatResult result = judge.judgeFormat("ROK Government issued certificate", true, "pdf");

        assertThat(result.verdict()).isEqualTo(PreCheckVerdict.NEEDS_HUMAN);
        assertThat(result.allowed()).isEmpty();
    }

    @Test
    @DisplayName("TC-PRE-010 형식원문없음_올림_조건없음")
    void 형식원문없음_올림_조건없음() {
        assertThat(judge.judgeFormat(null, true, "pdf").verdict()).isEqualTo(PreCheckVerdict.NO_CONDITION);
    }

    @Test
    @DisplayName("TC-PRE-011 형식이름과다른낱말섞임_올림_사람확인")
    void 형식이름과다른낱말섞임_올림_사람확인() {
        PreCheckJudge.FormatResult result = judge.judgeFormat("Excel sheet (PDF로 제출)", true, "xlsx");

        assertThat(result.verdict()).isEqualTo(PreCheckVerdict.NEEDS_HUMAN);
        assertThat(result.allowed()).isEmpty();
    }

    @Test
    @DisplayName("TC-PRE-012 쪽수하나_한도이하_맞음")
    void 쪽수하나_한도이하_맞음() {
        PreCheckJudge.PageResult result = judge.judgePages("1 page", true, "pdf", 1, UNITS);

        assertThat(result.verdict()).isEqualTo(PreCheckVerdict.MATCH);
        assertThat(result.limit()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-PRE-013 쪽수하나_한도넘음_넘음")
    void 쪽수하나_한도넘음_넘음() {
        PreCheckJudge.PageResult result = judge.judgePages("10페이지", true, "pdf", 12, UNITS);

        assertThat(result.verdict()).isEqualTo(PreCheckVerdict.OVER);
        assertThat(result.limit()).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-PRE-014 합계조건_올림_사람확인")
    void 합계조건_올림_사람확인() {
        PreCheckJudge.PageResult result = judge.judgePages("1 page total for Volume II", true, "pdf", 1, UNITS);

        assertThat(result.verdict()).isEqualTo(PreCheckVerdict.NEEDS_HUMAN);
        assertThat(result.limit()).isNull();
    }

    @Test
    @DisplayName("TC-PRE-015 숫자없음_올림_조건없음")
    void 숫자없음_올림_조건없음() {
        assertThat(judge.judgePages("명시되지 않음", true, "pdf", 1, UNITS).verdict())
                .isEqualTo(PreCheckVerdict.NO_CONDITION);
    }

    @Test
    @DisplayName("TC-PRE-016 쪽수못셈_올림_사람확인")
    void 쪽수못셈_올림_사람확인() {
        assertThat(judge.judgePages("1 page", true, "pdf", null, UNITS).verdict())
                .isEqualTo(PreCheckVerdict.NEEDS_HUMAN);
    }

    @Test
    @DisplayName("TC-PRE-017 단위설정비어있음_쪽수하나_사람확인")
    void 단위설정비어있음_쪽수하나_사람확인() {
        given(systemSettingService.findValue(SettingKey.PRE_CHECK_PAGE_UNITS)).willReturn(Optional.of(""));

        assertThat(judge.judgePages("1 page", true, "pdf", 1, judge.pageUnits()).verdict())
                .isEqualTo(PreCheckVerdict.NEEDS_HUMAN);
    }
}
