package com.biddingagency.domain.base.service;

import com.biddingagency.domain.base.entity.MilitaryBase;
import com.biddingagency.domain.base.repository.MilitaryBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PerformancePlaceServiceTest {

    @Mock
    private MilitaryBaseRepository baseRepository;

    private PerformancePlaceService.Resolver resolver;

    @BeforeEach
    void setUp() {
        given(baseRepository.findAllByOrderBySortOrderAscNameKoAsc())
                .willReturn(List.of(base("오산 공군기지", "OSAN"), base("캠프 험프리스", "HUMPHREYS")));
        resolver = new PerformancePlaceService(baseRepository).resolver();
    }

    private static MilitaryBase base(String nameKo, String terms) {
        MilitaryBase b = MilitaryBase.builder().nameKo(nameKo).nameEn(terms).matchTerms(terms).sortOrder(0).build();
        ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
        return b;
    }

    // TC-MKT-054
    @Test
    @DisplayName("짧은장소있음_제목기지있어도_짧은장소")
    void 짧은장소있음_제목기지있어도_짧은장소() {
        assertThat(resolver.placeOf("Pyeongtaek, Gyeonggi", "REPAIR ROOF, CAMP HUMPHREYS"))
                .isEqualTo("Pyeongtaek, Gyeonggi");
    }

    // TC-MKT-055
    @Test
    @DisplayName("짧은장소없음_제목기지둘_목록순서로이음")
    void 짧은장소없음_제목기지둘_목록순서로이음() {
        assertThat(resolver.placeOf(null, "PAVING AT HUMPHREYS AND OSAN")).isEqualTo("오산 공군기지, 캠프 험프리스");
    }

    // TC-MKT-056
    @Test
    @DisplayName("짧은장소도제목기지도없음_null")
    void 짧은장소도제목기지도없음_null() {
        assertThat(resolver.placeOf(" ", "3D Printer and 3D Scanner")).isNull();
    }
}
