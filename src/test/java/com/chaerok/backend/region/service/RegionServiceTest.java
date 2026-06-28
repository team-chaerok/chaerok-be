package com.chaerok.backend.region.service;

import com.chaerok.backend.region.dto.RegionResponse;
import com.chaerok.backend.region.dto.ResolveRegionRequest;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private Region region;

    private RegionService regionService;

    @BeforeEach
    void setUp() {
        regionService = new RegionService(regionRepository);
    }

    @Test
    @DisplayName("서비스 대상 지역이면 표준 지역 정보와 regionId를 반환한다")
    void resolveSupportedRegion() {
        // given
        ResolveRegionRequest request =
                new ResolveRegionRequest("충청남도", "공주시");

        when(regionRepository
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "충청남도",
                        "공주시"
                ))
                .thenReturn(Optional.of(region));

        when(region.getId()).thenReturn(1L);
        when(region.getProvinceName()).thenReturn("충청남도");
        when(region.getCityCountyName()).thenReturn("공주시");

        // when
        RegionResponse response = regionService.resolve(request);

        // then
        assertThat(response.serviceArea()).isTrue();
        assertThat(response.regionId()).isEqualTo(1L);
        assertThat(response.provinceName()).isEqualTo("충청남도");
        assertThat(response.cityCountyName()).isEqualTo("공주시");

        verify(regionRepository)
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "충청남도",
                        "공주시"
                );
    }

    @Test
    @DisplayName("지역 축약형과 앞뒤 공백을 표준 지역명으로 변환한다")
    void resolveRegionWithAliases() {
        // given
        ResolveRegionRequest request =
                new ResolveRegionRequest(" 충남 ", " 공주 ");

        when(regionRepository
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "충청남도",
                        "공주시"
                ))
                .thenReturn(Optional.of(region));

        when(region.getId()).thenReturn(1L);
        when(region.getProvinceName()).thenReturn("충청남도");
        when(region.getCityCountyName()).thenReturn("공주시");

        // when
        RegionResponse response = regionService.resolve(request);

        // then
        assertThat(response.serviceArea()).isTrue();
        assertThat(response.regionId()).isEqualTo(1L);
        assertThat(response.provinceName()).isEqualTo("충청남도");
        assertThat(response.cityCountyName()).isEqualTo("공주시");

        verify(regionRepository)
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "충청남도",
                        "공주시"
                );
    }

    @Test
    @DisplayName("서비스 대상이 아닌 지역이면 serviceArea false를 반환한다")
    void resolveUnsupportedRegion() {
        // given
        ResolveRegionRequest request =
                new ResolveRegionRequest("대전광역시", "유성구");

        when(regionRepository
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "대전광역시",
                        "유성구"
                ))
                .thenReturn(Optional.empty());

        // when
        RegionResponse response = regionService.resolve(request);

        // then
        assertThat(response.serviceArea()).isFalse();
        assertThat(response.regionId()).isNull();
        assertThat(response.provinceName()).isEqualTo("대전광역시");
        assertThat(response.cityCountyName()).isEqualTo("유성구");

        verify(regionRepository)
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "대전광역시",
                        "유성구"
                );
    }

    @Test
    @DisplayName("지원 지역의 정식 명칭에 포함된 앞뒤 공백을 제거한다")
    void resolveRegionWithWhitespace() {
        // given
        ResolveRegionRequest request =
                new ResolveRegionRequest(" 충청남도 ", " 예산군 ");

        when(regionRepository
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "충청남도",
                        "예산군"
                ))
                .thenReturn(Optional.of(region));

        when(region.getId()).thenReturn(4L);
        when(region.getProvinceName()).thenReturn("충청남도");
        when(region.getCityCountyName()).thenReturn("예산군");

        // when
        RegionResponse response = regionService.resolve(request);

        // then
        assertThat(response.serviceArea()).isTrue();
        assertThat(response.regionId()).isEqualTo(4L);
        assertThat(response.provinceName()).isEqualTo("충청남도");
        assertThat(response.cityCountyName()).isEqualTo("예산군");

        verify(regionRepository)
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "충청남도",
                        "예산군"
                );
    }
}