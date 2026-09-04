package com.chaerok.backend.course.controller;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.course.dto.CourseCreateRequest;
import com.chaerok.backend.course.dto.SelectedCourseResponse;
import com.chaerok.backend.course.service.CourseCommandService;
import com.chaerok.backend.course.service.CourseRecommendService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseRecommendService courseRecommendService;

    @MockitoBean
    private CourseCommandService courseCommandService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUpAuthentication() {
        AuthenticatedUser principal =
                new AuthenticatedUser(
                        1L,
                        UserRole.USER
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("추천 코스 조회에서 regionId가 누락되면 400을 반환한다")
    void recommendRejectsMissingRegionId() throws Exception {
        mockMvc.perform(get("/api/courses/recommend"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_002"));

        verifyNoInteractions(courseRecommendService);
        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("코스 생성에서 regionId가 누락되면 400을 반환한다")
    void createCourseRejectsMissingRegionId() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "공주 여행 코스",
                                  "places": [
                                    {
                                      "placeId": 100,
                                      "source": "TOUR_API",
                                      "title": "공산성",
                                      "categoryGroup": "TOURISM"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("regionId"));

        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("코스 제목이 공백이면 400을 반환한다")
    void createCourseRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionId": 10,
                                  "title": " ",
                                  "places": [
                                    {
                                      "placeId": 100,
                                      "source": "TOUR_API",
                                      "title": "공산성",
                                      "categoryGroup": "TOURISM"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("title"));

        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("코스 생성 장소가 0개이면 400을 반환한다")
    void createCourseRejectsEmptyPlaces() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionId": 10,
                                  "title": "공주 여행 코스",
                                  "places": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("places"));

        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("코스 생성 장소가 4개이면 400을 반환한다")
    void createCourseRejectsMoreThanThreePlaces() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionId": 10,
                                  "title": "공주 여행 코스",
                                  "places": [
                                    {
                                      "placeId": 101,
                                      "source": "TOUR_API",
                                      "title": "장소 1",
                                      "categoryGroup": "TOURISM"
                                    },
                                    {
                                      "placeId": 102,
                                      "source": "KAKAO_LOCAL",
                                      "title": "장소 2",
                                      "categoryGroup": "FOOD"
                                    },
                                    {
                                      "placeId": 103,
                                      "source": "KAKAO_LOCAL",
                                      "title": "장소 3",
                                      "categoryGroup": "CAFE_DESSERT"
                                    },
                                    {
                                      "placeId": 104,
                                      "source": "TOUR_API",
                                      "title": "장소 4",
                                      "categoryGroup": "TOURISM"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("places"));

        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("코스 장소명이 공백이면 400을 반환한다")
    void createCourseRejectsBlankPlaceTitle() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionId": 10,
                                  "title": "공주 여행 코스",
                                  "places": [
                                    {
                                      "placeId": 100,
                                      "source": "TOUR_API",
                                      "title": " ",
                                      "categoryGroup": "TOURISM"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("places[0].title"));

        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("코스 장소 유형이 공백이면 400을 반환한다")
    void createCourseRejectsBlankPlaceCategoryGroup() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionId": 10,
                                  "title": "공주 여행 코스",
                                  "places": [
                                    {
                                      "placeId": 100,
                                      "source": "TOUR_API",
                                      "title": "공산성",
                                      "categoryGroup": " "
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("places[0].categoryGroup"));

        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("ACTIVE 코스에 추가할 장소가 0개이면 400을 반환한다")
    void addPlacesRejectsEmptyPlaces() throws Exception {
        mockMvc.perform(post("/api/courses/active/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "places": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("places"));

        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("ACTIVE 코스에 추가할 장소가 4개이면 400을 반환한다")
    void addPlacesRejectsMoreThanThreePlaces() throws Exception {
        mockMvc.perform(post("/api/courses/active/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "places": [
                                    {
                                      "placeId": 101,
                                      "source": "TOUR_API",
                                      "title": "장소 1",
                                      "categoryGroup": "TOURISM"
                                    },
                                    {
                                      "placeId": 102,
                                      "source": "KAKAO_LOCAL",
                                      "title": "장소 2",
                                      "categoryGroup": "FOOD"
                                    },
                                    {
                                      "placeId": 103,
                                      "source": "KAKAO_LOCAL",
                                      "title": "장소 3",
                                      "categoryGroup": "CAFE_DESSERT"
                                    },
                                    {
                                      "placeId": 104,
                                      "source": "TOUR_API",
                                      "title": "장소 4",
                                      "categoryGroup": "TOURISM"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("places"));

        verifyNoInteractions(courseCommandService);
    }

    @Test
    @DisplayName("인증 사용자의 principal을 이용해 코스를 생성한다")
    void createCourseUsesAuthenticatedPrincipal() throws Exception {
        SelectedCourseResponse response =
                new SelectedCourseResponse(
                        20L,
                        10L,
                        "공주 여행 코스",
                        "ACTIVE",
                        1,
                        false,
                        List.of()
                );

        when(courseCommandService.createCourse(
                eq(1L),
                any(CourseCreateRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionId": 10,
                                  "title": "공주 여행 코스",
                                  "places": [
                                    {
                                      "placeId": 100,
                                      "source": "TOUR_API",
                                      "title": "공산성",
                                      "categoryGroup": "TOURISM"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(20))
                .andExpect(jsonPath("$.regionId").value(10))
                .andExpect(jsonPath("$.title")
                        .value("공주 여행 코스"));

        verify(courseCommandService).createCourse(
                eq(1L),
                any(CourseCreateRequest.class)
        );
    }
}