package com.chaerok.backend.place.controller;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.place.dto.PlaceDetailResponse;
import com.chaerok.backend.place.dto.PlaceListResponse;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.service.PlaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("regionId 기준 장소 목록 조회에 성공한다")
    void getPlaces() throws Exception {
        // given
        Long regionId = 1L;

        PlaceListResponse response = new PlaceListResponse(
                1L,
                "공산성",
                "충청남도 공주시 웅진로 280",
                new BigDecimal("36.4623000"),
                new BigDecimal("127.1248000"),
                "https://example.com/image.jpg",
                PlaceCategoryGroup.TOURISM,
                PlaceCategoryDetail.HERITAGE,
                false
        );

        when(placeService.getPlacesByRegion(regionId))
                .thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/places")
                        .param("regionId", String.valueOf(regionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("공산성"))
                .andExpect(jsonPath("$[0].address").value("충청남도 공주시 웅진로 280"))
                .andExpect(jsonPath("$[0].latitude").value(36.4623000))
                .andExpect(jsonPath("$[0].longitude").value(127.1248000))
                .andExpect(jsonPath("$[0].firstImageUrl").value("https://example.com/image.jpg"))
                .andExpect(jsonPath("$[0].categoryGroup").value("TOURISM"))
                .andExpect(jsonPath("$[0].categoryDetail").value("HERITAGE"))
                .andExpect(jsonPath("$[0].isRepresentative").value(false));
    }

    @Test
    @DisplayName("placeId 기준 장소 상세 조회에 성공한다")
    void getPlace() throws Exception {
        // given
        Long placeId = 1L;

        PlaceDetailResponse response = new PlaceDetailResponse(
                1L,
                1L,
                "1001",
                "공산성",
                "충청남도 공주시 웅진로 280",
                new BigDecimal("36.4623000"),
                new BigDecimal("127.1248000"),
                "https://example.com/image.jpg",
                "44",
                "150",
                "HS",
                "HS01",
                "HS010100",
                PlaceCategoryGroup.TOURISM,
                PlaceCategoryDetail.HERITAGE,
                false,
                PlaceSource.TOUR_API
        );

        when(placeService.getPlace(placeId))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/places/{placeId}", placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.regionId").value(1))
                .andExpect(jsonPath("$.tourContentId").value("1001"))
                .andExpect(jsonPath("$.title").value("공산성"))
                .andExpect(jsonPath("$.address").value("충청남도 공주시 웅진로 280"))
                .andExpect(jsonPath("$.latitude").value(36.4623000))
                .andExpect(jsonPath("$.longitude").value(127.1248000))
                .andExpect(jsonPath("$.firstImageUrl").value("https://example.com/image.jpg"))
                .andExpect(jsonPath("$.lDongRegnCd").value("44"))
                .andExpect(jsonPath("$.lDongSignguCd").value("150"))
                .andExpect(jsonPath("$.lclsSystm1").value("HS"))
                .andExpect(jsonPath("$.lclsSystm2").value("HS01"))
                .andExpect(jsonPath("$.lclsSystm3").value("HS010100"))
                .andExpect(jsonPath("$.categoryGroup").value("TOURISM"))
                .andExpect(jsonPath("$.categoryDetail").value("HERITAGE"))
                .andExpect(jsonPath("$.isRepresentative").value(false))
                .andExpect(jsonPath("$.source").value("TOUR_API"));
    }
}