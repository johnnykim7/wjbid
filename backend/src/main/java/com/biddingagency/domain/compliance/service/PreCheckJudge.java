package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.compliance.entity.FileFormat;
import com.biddingagency.domain.compliance.entity.PreCheckVerdict;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 제출 전 검사의 형식·쪽수 판정 (CR-141, BIZ-047, BIZ-048, POL-028).
 *
 * 공고 조건 원문이 형식 이름이나 "숫자 하나 + 쪽 단위" 로만 되어 있을 때만 기계가 판정한다.
 * 다른 말이 섞이면 원문에 없는 기준을 지어내지 않으려고 사람 확인으로 둔다.
 */
@Component
@RequiredArgsConstructor
public class PreCheckJudge {

    private static final Pattern WORD_SPLIT = Pattern.compile("[^0-9A-Za-z가-힣]+");
    private static final Pattern DIGIT_LETTER_BOUNDARY = Pattern.compile("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private final SystemSettingService systemSettingService;

    public record FormatResult(PreCheckVerdict verdict, String basis, List<FileFormat> allowed) {
    }

    public record PageResult(PreCheckVerdict verdict, String basis, Integer limit) {
    }

    public FormatResult judgeFormat(String formatText, boolean uploaded, String extension) {
        List<String> words = words(formatText);
        if (words.isEmpty()) {
            return new FormatResult(PreCheckVerdict.NO_CONDITION, blankToNull(formatText), List.of());
        }
        List<FileFormat> allowed = new ArrayList<>();
        for (String word : words) {
            Optional<FileFormat> format = FileFormat.byName(word);
            if (format.isEmpty()) {
                return new FormatResult(PreCheckVerdict.NEEDS_HUMAN, formatText, List.of());
            }
            if (!allowed.contains(format.get())) {
                allowed.add(format.get());
            }
        }
        if (!uploaded) {
            return new FormatResult(PreCheckVerdict.NOT_UPLOADED, formatText, allowed);
        }
        boolean match = FileFormat.byExtension(extension).map(allowed::contains).orElse(false);
        return new FormatResult(match ? PreCheckVerdict.MATCH : PreCheckVerdict.MISMATCH, formatText, allowed);
    }

    public PageResult judgePages(String pageLimitText, boolean uploaded, String extension, Integer pageCount,
                                 Set<String> pageUnits) {
        if (pageLimitText == null || !DIGITS.matcher(pageLimitText).find()) {
            return new PageResult(PreCheckVerdict.NO_CONDITION, blankToNull(pageLimitText), null);
        }
        Integer limit = readSingleLimit(pageLimitText, pageUnits);
        if (!uploaded) {
            return new PageResult(PreCheckVerdict.NOT_UPLOADED, pageLimitText, limit);
        }
        if (limit == null || pageCount == null || FileFormat.byExtension(extension).orElse(null) != FileFormat.PDF) {
            return new PageResult(PreCheckVerdict.NEEDS_HUMAN, pageLimitText, limit);
        }
        return new PageResult(pageCount <= limit ? PreCheckVerdict.MATCH : PreCheckVerdict.OVER, pageLimitText, limit);
    }

    /** "1 page", "10페이지" 처럼 숫자 하나 뒤에 쪽 단위 낱말 하나만 있을 때 그 숫자. 그 밖은 null */
    private Integer readSingleLimit(String text, Set<String> units) {
        if (units.isEmpty()) {
            return null;
        }
        List<String> tokens = words(text).stream()
                .flatMap(w -> Arrays.stream(DIGIT_LETTER_BOUNDARY.split(w)))
                .filter(t -> !t.isEmpty())
                .toList();
        if (tokens.size() != 2 || !DIGITS.matcher(tokens.get(0)).matches()
                || !units.contains(tokens.get(1).toLowerCase(Locale.ROOT))) {
            return null;
        }
        try {
            return Integer.parseInt(tokens.get(0));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 쪽 단위 낱말(소문자). 설정이 비었으면 빈 집합이고 쪽수 조건은 모두 사람 확인이 된다. 한 번 읽어 여러 항목에 쓴다 */
    public Set<String> pageUnits() {
        return systemSettingService.findValue(SettingKey.PRE_CHECK_PAGE_UNITS)
                .map(v -> Arrays.stream(v.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private static List<String> words(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(WORD_SPLIT.split(text.trim()))
                .filter(w -> !w.isEmpty())
                .toList();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
