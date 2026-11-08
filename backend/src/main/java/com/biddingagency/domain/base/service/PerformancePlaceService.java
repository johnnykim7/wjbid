package com.biddingagency.domain.base.service;

import com.biddingagency.domain.base.entity.MilitaryBase;
import com.biddingagency.domain.base.repository.MilitaryBaseRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 화면에 보일 수행 장소 (BIZ-103, CR-191).
 * SAM 이 준 짧은 수행 장소, 없으면 영문 제목에서 찾은 기지(BIZ-031)의 한글 이름, 그것도 없으면 null.
 */
@Service
@RequiredArgsConstructor
public class PerformancePlaceService {

    private final MilitaryBaseRepository baseRepository;

    /** 목록 하나를 채우는 동안 기지 목록을 한 번만 읽도록 계산기를 돌려준다 */
    @Transactional(readOnly = true)
    public Resolver resolver() {
        List<MilitaryBase> bases = baseRepository.findAllByOrderBySortOrderAscNameKoAsc();
        Map<UUID, String> names = bases.stream()
                .collect(Collectors.toMap(MilitaryBase::getId, MilitaryBase::getNameKo, (a, b) -> a));
        return new Resolver(BaseMatcher.of(bases), names::get);
    }

    @Transactional(readOnly = true)
    public String placeOf(Opportunity opp) {
        return resolver().placeOf(opp.getPlaceOfPerformanceShort(), opp.getTitle());
    }

    public static final class Resolver {

        private final BaseMatcher matcher;
        private final Function<UUID, String> nameOf;

        Resolver(BaseMatcher matcher, Function<UUID, String> nameOf) {
            this.matcher = matcher;
            this.nameOf = nameOf;
        }

        /** 기지가 여럿이면 기지 목록 순서로 잇는다 */
        public String placeOf(String samPlace, String title) {
            if (samPlace != null && !samPlace.isBlank()) return samPlace.trim();
            String bases = matcher.match(title).stream().map(nameOf).collect(Collectors.joining(", "));
            return bases.isEmpty() ? null : bases;
        }
    }
}
