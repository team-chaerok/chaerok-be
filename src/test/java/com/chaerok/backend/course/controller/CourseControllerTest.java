package com.chaerok.backend.course.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.course.dto.CourseAddPlacesRequest;
import com.chaerok.backend.course.dto.CourseCreateRequest;
import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.course.dto.CourseRecommendResponse;
import com.chaerok.backend.course.dto.SelectedCourseResponse;
import com.chaerok.backend.course.service.CourseCommandService;
import com.chaerok.backend.course.service.CourseRecommendService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseRecommendService courseRecommendService;

    @Mock
    private CourseCommandService courseCommandService;

    private CourseController controller;
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        controller = new CourseController(
                courseRecommendService,
                courseCommandService
        );

        authenticatedUser = new AuthenticatedUser(
                1L,
                UserRole.USER
        );
    }

    @Test
    @DisplayName("지역을 기준으로 추천 코스 후보를 조회하고 200 응답을 반환한다")
    void recommendCoursesByRegion() {
        // given
        Long regionId = 10L;

        CourseRecommendResponse serviceResponse =
                new CourseRecommendResponse(
                        regionId,
                        "REGION",
                        null,
                        List.of()
                );

        when(courseRecommendService.recommendCourses(
                regionId,
                null
        )).thenReturn(serviceResponse);

        // when
        ResponseEntity<CourseRecommendResponse> response =
                controller.recommendCourses(
                        regionId,
                        null
                );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().regionId())
                .isEqualTo(regionId);
        assertThat(response.getBody().recommendationType())
                .isEqualTo("REGION");
        assertThat(response.getBody().anchorPlaceId()).isNull();
        assertThat(response.getBody().courses()).isEmpty();

        verify(courseRecommendService).recommendCourses(
                regionId,
                null
        );
        verifyNoMoreInteractions(courseRecommendService);
        verifyNoMoreInteractions(courseCommandService);
    }

    @Test
    @DisplayName("Anchor 장소를 기준으로 추천 코스 후보를 조회한다")
    void recommendCoursesByAnchorPlace() {
        // given
        Long regionId = 10L;
        Long anchorPlaceId = 100L;

        CourseRecommendResponse serviceResponse =
                new CourseRecommendResponse(
                        regionId,
                        "ANCHOR_PLACE",
                        anchorPlaceId,
                        List.of()
                );

        when(courseRecommendService.recommendCourses(
                regionId,
                anchorPlaceId
        )).thenReturn(serviceResponse);

        // when
        ResponseEntity<CourseRecommendResponse> response =
                controller.recommendCourses(
                        regionId,
                        anchorPlaceId
                );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().anchorPlaceId())
                .isEqualTo(anchorPlaceId);

        verify(courseRecommendService).recommendCourses(
                regionId,
                anchorPlaceId
        );
        verifyNoMoreInteractions(courseRecommendService);
        verifyNoMoreInteractions(courseCommandService);
    }

    @Test
    @DisplayName("인증된 사용자의 선택 장소로 코스를 생성하고 200 응답을 반환한다")
    void createCourse() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createPlaceRequest();

        CourseCreateRequest request =
                new CourseCreateRequest(
                        10L,
                        "서울 여행 코스",
                        List.of(placeRequest)
                );

        SelectedCourseResponse serviceResponse =
                createSelectedCourseResponse(
                        20L,
                        10L,
                        "서울 여행 코스",
                        1
                );

        when(courseCommandService.createCourse(
                1L,
                request
        )).thenReturn(serviceResponse);

        // when
        ResponseEntity<SelectedCourseResponse> response =
                controller.createCourse(
                        authenticatedUser,
                        request
                );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().courseId())
                .isEqualTo(20L);
        assertThat(response.getBody().regionId())
                .isEqualTo(10L);
        assertThat(response.getBody().title())
                .isEqualTo("서울 여행 코스");
        assertThat(response.getBody().status())
                .isEqualTo("ACTIVE");
        assertThat(response.getBody().placeCount())
                .isEqualTo(1);
        assertThat(response.getBody().completed()).isFalse();

        verify(courseCommandService).createCourse(
                1L,
                request
        );
        verifyNoMoreInteractions(courseCommandService);
        verifyNoMoreInteractions(courseRecommendService);
    }

    @Test
    @DisplayName("인증된 사용자의 ACTIVE 코스에 장소를 추가한다")
    void addPlacesToActiveCourse() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createPlaceRequest();

        CourseAddPlacesRequest request =
                new CourseAddPlacesRequest(
                        List.of(placeRequest)
                );

        SelectedCourseResponse serviceResponse =
                createSelectedCourseResponse(
                        20L,
                        10L,
                        "서울 여행 코스",
                        2
                );

        when(courseCommandService.addPlacesToActiveCourse(
                1L,
                request
        )).thenReturn(serviceResponse);

        // when
        ResponseEntity<SelectedCourseResponse> response =
                controller.addPlacesToActiveCourse(
                        authenticatedUser,
                        request
                );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().courseId())
                .isEqualTo(20L);
        assertThat(response.getBody().placeCount())
                .isEqualTo(2);
        assertThat(response.getBody().completed()).isFalse();

        verify(courseCommandService)
                .addPlacesToActiveCourse(
                        1L,
                        request
                );
        verifyNoMoreInteractions(courseCommandService);
        verifyNoMoreInteractions(courseRecommendService);
    }

    @Test
    @DisplayName("인증된 사용자의 ACTIVE 코스를 조회하고 200 응답을 반환한다")
    void getActiveCourse() {
        // given
        SelectedCourseResponse serviceResponse =
                createSelectedCourseResponse(
                        20L,
                        10L,
                        "서울 여행 코스",
                        3
                );

        when(courseCommandService.getActiveCourse(1L))
                .thenReturn(serviceResponse);

        // when
        ResponseEntity<SelectedCourseResponse> response =
                controller.getActiveCourse(
                        authenticatedUser
                );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().courseId())
                .isEqualTo(20L);
        assertThat(response.getBody().placeCount())
                .isEqualTo(3);
        assertThat(response.getBody().completed()).isTrue();

        verify(courseCommandService).getActiveCourse(1L);
        verifyNoMoreInteractions(courseCommandService);
        verifyNoMoreInteractions(courseRecommendService);
    }

    private CoursePlaceSaveRequest createPlaceRequest() {
        return new CoursePlaceSaveRequest(
                100L,
                null,
                "TOUR_API",
                "테스트 장소",
                "TOURISM",
                "HERITAGE",
                "서울특별시 테스트구",
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                "https://place.example"
        );
    }

    private SelectedCourseResponse createSelectedCourseResponse(
            Long courseId,
            Long regionId,
            String title,
            int placeCount
    ) {
        return new SelectedCourseResponse(
                courseId,
                regionId,
                title,
                "ACTIVE",
                placeCount,
                placeCount == 3,
                List.of()
        );
    }
}