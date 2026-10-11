package com.biddingagency.domain.base.service;

import com.biddingagency.domain.base.entity.MilitaryBase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 공고 제목으로 기지 찾기 (BIZ-031).
 * 찾을 낱말이 낱말 단위로(앞뒤가 영문자·숫자가 아님) 들어 있으면 걸린다. 대소문자는 가리지 않는다.
 */
public final class BaseMatcher {

    private record Entry(UUID baseId, List<Pattern> patterns) {
    }

    private final List<Entry> entries;

    private BaseMatcher(List<Entry> entries) {
        this.entries = entries;
    }

    public static BaseMatcher of(List<MilitaryBase> bases) {
        List<Entry> entries = new ArrayList<>();
        for (MilitaryBase b : bases) {
            entries.add(new Entry(b.getId(), b.terms().stream().map(BaseMatcher::wordPattern).toList()));
        }
        return new BaseMatcher(entries);
    }

    static Pattern wordPattern(String term) {
        return Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(term) + "(?![A-Za-z0-9])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** 제목이 걸리는 기지 id. 기지 목록 순서대로 */
    public Set<UUID> match(String title) {
        Set<UUID> result = new LinkedHashSet<>();
        if (title == null || title.isBlank()) return result;
        for (Entry e : entries) {
            for (Pattern p : e.patterns()) {
                if (p.matcher(title).find()) {
                    result.add(e.baseId());
                    break;
                }
            }
        }
        return result;
    }
}
