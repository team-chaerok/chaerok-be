package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.region.entity.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThatThrownBy(() -> policy.validate(
                region("공주시"),
                "buyeo"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선택한 지역에서 사용할 수 없는 필터입니다.");
    }

    @Test
    @DisplayName("정책에 등록되지 않은 지역은 필터를 허용하지 않는다")
    void rejectsUnmappedRegion() {
        assertThatThrownBy(() -> policy.validate(
                region("천안시"),
                "gongju"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선택한 지역에서 사용할 수 없는 필터입니다.");
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
