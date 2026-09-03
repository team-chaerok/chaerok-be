package com.chaerok.backend.user.service;

import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import com.chaerok.backend.user.config.ReviewModeProperties;
import com.chaerok.backend.user.dto.ReviewModeResponse;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.exception.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewModeServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private User user;

    private ReviewModeProperties properties;
    private ReviewModeService service;

    @BeforeEach
    void setUp() {
        properties = new ReviewModeProperties();
        service = new ReviewModeService(
                userService,
                regionRepository,
                placeRepository,
                properties
        );

        when(userService.findById(1L)).thenReturn(user);
    }

    @Test
    @DisplayName("일반 사용자는 테스트 장소를 조회하지 않고 심사용 모드 비활성 응답을 받는다")
    void returnsDisabledForNormalUser() {
        when(user.isReviewMode()).thenReturn(false);

        ReviewModeResponse response = service.getReviewMode(1L);

        assertThat(response.enabled()).isFalse();
        assertThat(response.region()).isNull();
        assertThat(response.testPlaces()).isEmpty();
        verifyNoInteractions(regionRepository, placeRepository);
    }

    @Test
    @DisplayName("심사용 사용자는 공주와 TOURISM FOOD CAFE_DESSERT 테스트 장소를 순서대로 받는다")
    void returnsConfiguredReviewMode() {
        when(user.isReviewMode()).thenReturn(true);
        Region gongju = region(1L, "충청남도", "공주시");
        Place tourism = place(
                11L,
                gongju,
                "125949",
                "공주 공산성 [유네스코 세계유산]",
                PlaceCategoryGroup.TOURISM,
                new BigDecimal("36.4629499677"),
                new BigDecimal("127.1267906104")
        );
        Place food = place(
                15L,
                gongju,
                "2738735",
                "진흥각",
                PlaceCategoryGroup.FOOD,
                new BigDecimal("36.4527437295"),
                new BigDecimal("127.1234970212")
        );
        Place cafe = place(
                18L,
                gongju,
                "2876760",
                "공다방",
                PlaceCategoryGroup.CAFE_DESSERT,
                new BigDecimal("36.4651450786"),
                new BigDecimal("127.1230828345")
        );

        stubRegion(gongju);
        stubDefaultPlaces(tourism, food, cafe);

        ReviewModeResponse response = service.getReviewMode(1L);

        assertThat(response.enabled()).isTrue();
        assertThat(response.region().regionId()).isEqualTo(1L);
        assertThat(response.region().provinceName()).isEqualTo("충청남도");
        assertThat(response.region().cityCountyName()).isEqualTo("공주시");
        assertThat(response.testPlaces())
                .extracting(ReviewModeResponse.ReviewTestPlaceResponse::categoryGroup)
                .containsExactly(
                        "TOURISM",
                        "FOOD",
                        "CAFE_DESSERT"
                );
        assertThat(response.testPlaces())
                .extracting(ReviewModeResponse.ReviewTestPlaceResponse::placeId)
                .containsExactly(11L, 15L, 18L);
        assertThat(response.testPlaces().get(0).latitude())
                .isEqualByComparingTo("36.4629499677");
    }

    @Test
    @DisplayName("설정된 심사용 장소가 DB에 없으면 설정 오류로 실패한다")
    void rejectsMissingReviewPlace() {
        when(user.isReviewMode()).thenReturn(true);
        Region gongju = region(1L, "충청남도", "공주시");
        stubRegion(gongju);
        when(placeRepository.findByTourContentId("125949"))
                .thenReturn(Optional.empty());

        assertInvalidConfiguration(
                () -> service.getReviewMode(1L)
        );
    }

    @Test
    @DisplayName("심사용 장소 카테고리가 중복되면 설정 오류로 실패한다")
    void rejectsDuplicatedCategory() {
        when(user.isReviewMode()).thenReturn(true);
        Region gongju = region(1L, "충청남도", "공주시");
        Place tourism = place(
                11L, gongju, "125949", "공산성",
                PlaceCategoryGroup.TOURISM,
                BigDecimal.ONE, BigDecimal.ONE
        );
        Place duplicatedTourism = place(
                15L, gongju, "2738735", "진흥각",
                PlaceCategoryGroup.TOURISM,
                BigDecimal.ONE, BigDecimal.ONE
        );
        stubRegion(gongju);
        when(placeRepository.findByTourContentId("125949"))
                .thenReturn(Optional.of(tourism));
        when(placeRepository.findByTourContentId("2738735"))
                .thenReturn(Optional.of(duplicatedTourism));

        assertInvalidConfiguration(
                () -> service.getReviewMode(1L)
        );
    }

    @Test
    @DisplayName("심사용 장소가 공주가 아닌 다른 지역에 속하면 설정 오류로 실패한다")
    void rejectsPlaceFromAnotherRegion() {
        when(user.isReviewMode()).thenReturn(true);
        Region gongju = region(1L, "충청남도", "공주시");
        Region buyeo = region(2L, "충청남도", "부여군");
        Place tourism = place(
                11L, buyeo, "125949", "공산성",
                PlaceCategoryGroup.TOURISM,
                BigDecimal.ONE, BigDecimal.ONE
        );

        stubRegion(gongju);
        when(placeRepository.findByTourContentId("125949"))
                .thenReturn(Optional.of(tourism));

        assertInvalidConfiguration(
                () -> service.getReviewMode(1L)
        );
    }

    @Test
    @DisplayName("심사용 장소 좌표가 없으면 위치 공급에 사용할 수 없으므로 설정 오류로 실패한다")
    void rejectsPlaceWithoutCoordinates() {
        when(user.isReviewMode()).thenReturn(true);
        Region gongju = region(1L, "충청남도", "공주시");
        Place tourism = place(
                11L, gongju, "125949", "공산성",
                PlaceCategoryGroup.TOURISM,
                null, BigDecimal.ONE
        );

        stubRegion(gongju);
        when(placeRepository.findByTourContentId("125949"))
                .thenReturn(Optional.of(tourism));

        assertInvalidConfiguration(
                () -> service.getReviewMode(1L)
        );
    }

    private void stubRegion(Region region) {
        when(regionRepository
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        "충청남도",
                        "공주시"
                ))
                .thenReturn(Optional.of(region));
    }

    private void stubDefaultPlaces(
            Place tourism,
            Place food,
            Place cafe
    ) {
        when(placeRepository.findByTourContentId("125949"))
                .thenReturn(Optional.of(tourism));
        when(placeRepository.findByTourContentId("2738735"))
                .thenReturn(Optional.of(food));
        when(placeRepository.findByTourContentId("2876760"))
                .thenReturn(Optional.of(cafe));
    }

    private Region region(
            Long id,
            String provinceName,
            String cityCountyName
    ) {
        Region region = Region.create(
                provinceName,
                cityCountyName,
                "44",
                id.equals(1L) ? "150" : "760",
                true
        );
        ReflectionTestUtils.setField(region, "id", id);
        return region;
    }

    private Place place(
            Long id,
            Region region,
            String tourContentId,
            String title,
            PlaceCategoryGroup categoryGroup,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        Place place = Place.create(
                region,
                tourContentId,
                null,
                title,
                "충청남도 공주시 테스트 주소",
                latitude,
                longitude,
                null,
                "44",
                "150",
                null,
                null,
                null,
                categoryGroup,
                null,
                true,
                PlaceSource.TOUR_API
        );
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    private void assertInvalidConfiguration(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        UserErrorCode
                                                .REVIEW_MODE_CONFIGURATION_INVALID
                                )
                );
    }
}
