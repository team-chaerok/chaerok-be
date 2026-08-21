package com.chaerok.backend.place.service;

import com.chaerok.backend.region.entity.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionCenterProviderTest {

    private RegionCenterProvider regionCenterProvider;
    private Region region;

    @BeforeEach
    void setUp() {
        regionCenterProvider = new RegionCenterProvider();
        region = mock(Region.class);
    }

    @Test
    @DisplayName("공주시 시군 코드에 해당하는 중심 좌표를 반환한다")
    void getGongjuCenter() {
        // given
        when(region.getLdongSignguCd()).thenReturn("150");

        // when
        RegionCenterProvider.RegionCenter center =
                regionCenterProvider.getCenter(region);

        // then
        assertThat(center).isNotNull();
        assertThat(center.longitude())
                .isEqualByComparingTo(new BigDecimal("127.1190"));
        assertThat(center.latitude())
                .isEqualByComparingTo(new BigDecimal("36.4465"));
    }

    @Test
    @DisplayName("부여군 시군 코드에 해당하는 중심 좌표를 반환한다")
    void getBuyeoCenter() {
        // given
        when(region.getLdongSignguCd()).thenReturn("760");

        // when
        RegionCenterProvider.RegionCenter center =
                regionCenterProvider.getCenter(region);

        // then
        assertThat(center).isNotNull();
        assertThat(center.longitude())
                .isEqualByComparingTo(new BigDecimal("126.9119"));
        assertThat(center.latitude())
                .isEqualByComparingTo(new BigDecimal("36.2757"));
    }

    @Test
    @DisplayName("서산시 시군 코드에 해당하는 중심 좌표를 반환한다")
    void getSeosanCenter() {
        // given
        when(region.getLdongSignguCd()).thenReturn("210");

        // when
        RegionCenterProvider.RegionCenter center =
                regionCenterProvider.getCenter(region);

        // then
        assertThat(center).isNotNull();
        assertThat(center.longitude())
                .isEqualByComparingTo(new BigDecimal("126.4503"));
        assertThat(center.latitude())
                .isEqualByComparingTo(new BigDecimal("36.7845"));
    }

    @Test
    @DisplayName("예산군 시군 코드에 해당하는 중심 좌표를 반환한다")
    void getYesanCenter() {
        // given
        when(region.getLdongSignguCd()).thenReturn("810");

        // when
        RegionCenterProvider.RegionCenter center =
                regionCenterProvider.getCenter(region);

        // then
        assertThat(center).isNotNull();
        assertThat(center.longitude())
                .isEqualByComparingTo(new BigDecimal("126.8447"));
        assertThat(center.latitude())
                .isEqualByComparingTo(new BigDecimal("36.6829"));
    }

    @Test
    @DisplayName("지원하지 않는 시군 코드는 중심 좌표를 반환하지 않는다")
    void getUnsupportedRegionCenter() {
        // given
        when(region.getLdongSignguCd()).thenReturn("999");

        // when
        RegionCenterProvider.RegionCenter center =
                regionCenterProvider.getCenter(region);

        // then
        assertThat(center).isNull();
    }
}