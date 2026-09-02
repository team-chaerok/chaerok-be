package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.region.entity.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RegionFilterPolicyTest {

    private final RegionFilterPolicy policy = new RegionFilterPolicy();

    @Test
    @DisplayName("지원 지역은 각 지역 전용 필터만 허용한다")
    void allowsMatchingRegionFilters() {
        assertThatCode(() -> policy.validate(
                region("공주시"),
                "gongju"
        )).doesNotThrowAnyException();

        assertThatCode(() -> policy.validate(
                region("부여군"),
                "buyeo"
        )).doesNotThrowAnyException();

        assertThatCode(() -> policy.validate(
                region("서산시"),
                "seosan"
        )).doesNotThrowAnyException();

        assertThatCode(() -> policy.validate(
                region("예산군"),
                "yesan"
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 지역의 필터를 선택하면 거부한다")
    void rejectsMismatchedRegionFilter() {
        assertThatThrownBy(() ->
                policy.validate(
                        region("공주시"),
                        "wrong-filter"
                )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(
                                                FilmRollErrorCode.INVALID_REGION_FILTER
                                        )
                );
    }

    @Test
    @DisplayName("정책에 등록되지 않은 지역은 필터를 허용하지 않는다")
    void rejectsUnmappedRegion() {
        assertThatThrownBy(() ->
                policy.validate(
                        region("천안시"),
                        "gongju"
                )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(
                                                FilmRollErrorCode.INVALID_REGION_FILTER
                                        )
                );
    }

    private Region region(String cityCountyName) {
        return Region.create(
                "충청남도",
                cityCountyName,
                "44",
                "999",
                true
        );
    }
}
