package com.chaerok.backend.region.controller;

import com.chaerok.backend.region.dto.RegionResponse;
import com.chaerok.backend.region.service.RegionService;
import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegionService regionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("서비스 대상 지역 검증 요청에 성공한다")
    void resolveSupportedRegion() throws Exception {
        // given
        RegionResponse response = new RegionResponse(
                true,
                1L,
                "충청남도",
                "공주시"
        );

        when(regionService.resolve(any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/regions/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provinceName": "충청남도",
                                  "cityCountyName": "공주시"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceArea").value(true))
                .andExpect(jsonPath("$.regionId").value(1))
                .andExpect(jsonPath("$.provinceName").value("충청남도"))
                .andExpect(jsonPath("$.cityCountyName").value("공주시"));
    }

    @Test
    @DisplayName("서비스 대상이 아닌 지역도 정상 응답으로 반환한다")
    void resolveUnsupportedRegion() throws Exception {
        // given
        RegionResponse response = new RegionResponse(
                false,
                null,
                "대전광역시",
                "유성구"
        );

        when(regionService.resolve(any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/regions/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provinceName": "대전광역시",
                                  "cityCountyName": "유성구"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceArea").value(false))
                .andExpect(jsonPath("$.regionId").isEmpty())
                .andExpect(jsonPath("$.provinceName").value("대전광역시"))
                .andExpect(jsonPath("$.cityCountyName").value("유성구"));
    }

    @Test
    @DisplayName("시도명이 누락되면 400 응답을 반환한다")
    void resolveRegionWithoutProvinceName() throws Exception {
        mockMvc.perform(post("/api/regions/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityCountyName": "공주시"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(regionService);
    }

    @Test
    @DisplayName("시군명이 공백이면 400 응답을 반환한다")
    void resolveRegionWithBlankCityCountyName() throws Exception {
        mockMvc.perform(post("/api/regions/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provinceName": "충청남도",
                                  "cityCountyName": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(regionService);
    }
}