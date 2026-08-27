package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.region.entity.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseExternalPlaceResolverTest {

    @Mock
    private TourApiPlaceClient tourApiPlaceClient;

    @Mock
    private Region region;

    private CourseExternalPlaceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CourseExternalPlaceResolver(
                tourApiPlaceClient
        );
    }

    @Test
    @DisplayName("DB 장소 요청은 TourAPI를 호출하지 않는다")
    void doesNotCallTourApiForExistingDbPlace() {
        // given
        CoursePlaceSaveRequest request =
                createDbPlaceRequest();

        // when
        Optional<TourApiPlaceItem> result =
                resolver.resolveTourApiPlace(
                        region,
                        request
                );

        // then
        assertThat(result).isEmpty();

        verify(tourApiPlaceClient, never())
                .getPlaceDetail(org.mockito.ArgumentMatchers.anyString());

        verify(tourApiPlaceClient, never())
                .searchPlacesByKeyword(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    @Test
    @DisplayName("TourAPI 요청은 contentId로 상세 조회한다")
    void resolvesTourApiPlaceByContentId() {
        // given
        CoursePlaceSaveRequest request =
                createTourApiRequest();

        TourApiPlaceItem item =
                createTourApiPlaceItem(
                        "126204",
                        "공산성",
                        "44",
                        "150"
                );

        when(region.getLdongRegnCd())
                .thenReturn("44");

        when(region.getLdongSignguCd())
                .thenReturn("150");

        when(tourApiPlaceClient.getPlaceDetail("126204"))
                .thenReturn(item);

        // when
        Optional<TourApiPlaceItem> result =
                resolver.resolveTourApiPlace(
                        region,
                        request
                );

        // then
        assertThat(result)
                .contains(item);

        verify(tourApiPlaceClient)
                .getPlaceDetail("126204");

        verify(tourApiPlaceClient, never())
                .searchPlacesByKeyword(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    @Test
    @DisplayName("TourAPI 상세 조회 결과가 없으면 예외가 발생한다")
    void throwsWhenTourApiDetailIsMissing() {
        // given
        CoursePlaceSaveRequest request =
                createTourApiRequest();

        when(tourApiPlaceClient.getPlaceDetail("126204"))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() ->
                resolver.resolveTourApiPlace(
                        region,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "TourAPI 장소 정보를 찾을 수 없습니다."
                );
    }

    @Test
    @DisplayName("TourAPI 장소가 코스 지역과 다르면 예외가 발생한다")
    void throwsWhenTourApiPlaceRegionDoesNotMatch() {
        // given
        CoursePlaceSaveRequest request =
                createTourApiRequest();

        TourApiPlaceItem item =
                createTourApiPlaceItem(
                        "126204",
                        "공산성",
                        "44",
                        "170"
                );

        when(region.getLdongRegnCd())
                .thenReturn("44");

        when(region.getLdongSignguCd())
                .thenReturn("150");

        when(tourApiPlaceClient.getPlaceDetail("126204"))
                .thenReturn(item);

        // when & then
        assertThatThrownBy(() ->
                resolver.resolveTourApiPlace(
                        region,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "TourAPI 장소가 코스 지역과 일치하지 않습니다."
                );
    }

    @Test
    @DisplayName("Kakao 외부 장소는 제목과 지역코드로 TourAPI를 검색한다")
    void searchesTourApiForKakaoExternalPlace() {
        // given
        CoursePlaceSaveRequest request =
                createKakaoRequest(
                        "공산성",
                        "충남 공주시 금성동"
                );

        TourApiPlaceItem item =
                createTourApiPlaceItem(
                        "126204",
                        "공산성",
                        "44",
                        "150"
                );

        when(region.getLdongRegnCd())
                .thenReturn("44");

        when(region.getLdongSignguCd())
                .thenReturn("150");

        when(tourApiPlaceClient.searchPlacesByKeyword(
                "공산성",
                "44",
                "150"
        )).thenReturn(List.of(item));

        // when
        Optional<TourApiPlaceItem> result =
                resolver.resolveTourApiPlace(
                        region,
                        request
                );

        // then
        assertThat(result)
                .contains(item);
    }

    @Test
    @DisplayName("TourAPI 검색 결과에서 제목이 다른 장소는 매칭하지 않는다")
    void doesNotMatchDifferentTitle() {
        // given
        CoursePlaceSaveRequest request =
                createKakaoRequest(
                        "공산성",
                        "충남 공주시 금성동"
                );

        TourApiPlaceItem item =
                createTourApiPlaceItem(
                        "999999",
                        "무령왕릉",
                        "44",
                        "150"
                );

        when(region.getLdongRegnCd())
                .thenReturn("44");

        when(region.getLdongSignguCd())
                .thenReturn("150");

        when(tourApiPlaceClient.searchPlacesByKeyword(
                "공산성",
                "44",
                "150"
        )).thenReturn(List.of(item));

        // when
        Optional<TourApiPlaceItem> result =
                resolver.resolveTourApiPlace(
                        region,
                        request
                );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("제목과 주소가 일치하는 TourAPI 검색 결과를 매칭한다")
    void matchesTourApiPlaceByTitleAndAddress() {
        // given
        CoursePlaceSaveRequest request =
                createKakaoRequest(
                        "공 산 성",
                        "충남 공주시 금성동"
                );

        TourApiPlaceItem item =
                new TourApiPlaceItem(
                        "126204",
                        "공산성",
                        "충남 공주시 금성동",
                        "36.4650",
                        "127.1270",
                        null,
                        "44",
                        "150",
                        "HS",
                        null,
                        "HS01",
                        null
                );

        when(region.getLdongRegnCd())
                .thenReturn("44");

        when(region.getLdongSignguCd())
                .thenReturn("150");

        when(tourApiPlaceClient.searchPlacesByKeyword(
                "공 산 성",
                "44",
                "150"
        )).thenReturn(List.of(item));

        // when
        Optional<TourApiPlaceItem> result =
                resolver.resolveTourApiPlace(
                        region,
                        request
                );

        // then
        assertThat(result)
                .contains(item);
    }

    private CoursePlaceSaveRequest createDbPlaceRequest() {
        return new CoursePlaceSaveRequest(
                100L,
                null,
                PlaceSource.TOUR_API.name(),
                "공산성",
                "TOURISM",
                "HERITAGE",
                "충남 공주시 금성동",
                new BigDecimal("36.4650"),
                new BigDecimal("127.1270"),
                null
        );
    }

    private CoursePlaceSaveRequest createTourApiRequest() {
        return new CoursePlaceSaveRequest(
                null,
                "126204",
                PlaceSource.TOUR_API.name(),
                "공산성",
                "TOURISM",
                "HERITAGE",
                "충남 공주시 금성동",
                new BigDecimal("36.4650"),
                new BigDecimal("127.1270"),
                null
        );
    }

    private CoursePlaceSaveRequest createKakaoRequest(
            String title,
            String address
    ) {
        return new CoursePlaceSaveRequest(
                null,
                "kakao-1",
                PlaceSource.KAKAO_LOCAL.name(),
                title,
                "TOURISM",
                "HERITAGE",
                address,
                new BigDecimal("36.4650"),
                new BigDecimal("127.1270"),
                null
        );
    }

    private TourApiPlaceItem createTourApiPlaceItem(
            String contentId,
            String title,
            String regionCode,
            String sigunguCode
    ) {
        return new TourApiPlaceItem(
                contentId,
                title,
                "충남 공주시 금성동",
                "36.4650",
                "127.1270",
                null,
                regionCode,
                sigunguCode,
                "HS",
                null,
                "HS01",
                null
        );
    }
}