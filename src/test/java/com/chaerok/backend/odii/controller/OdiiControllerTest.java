package com.chaerok.backend.odii.controller;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.heritage.exception.HeritageErrorCode;
import com.chaerok.backend.odii.dto.OdiiGuideResponse;
import com.chaerok.backend.odii.dto.OdiiGuideResponse.OdiiStatus;
import com.chaerok.backend.odii.service.OdiiService;
import com.chaerok.backend.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OdiiController.class)
@AutoConfigureMockMvc(addFilters = false)
class OdiiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OdiiService odiiService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 모든_Odii_상태를_기존_필드와_함께_200으로_반환한다() throws Exception {
        for (OdiiStatus odiiStatus : OdiiStatus.values()) {
            boolean heritage = odiiStatus != OdiiStatus.NOT_APPLICABLE;
            OdiiGuideResponse response = new OdiiGuideResponse(
                    1L, "공산성", heritage, false, null, null, "TourAPI 소개", odiiStatus
            );
            when(odiiService.getAudioGuide(1L)).thenReturn(response);

            mockMvc.perform(get("/api/odii/places/{placeId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.placeId").value(1))
                    .andExpect(jsonPath("$.placeTitle").value("공산성"))
                    .andExpect(jsonPath("$.heritage").value(heritage))
                    .andExpect(jsonPath("$.audioAvailable").value(false))
                    .andExpect(jsonPath("$.audioGuide").doesNotExist())
                    .andExpect(jsonPath("$.guideScript").doesNotExist())
                    .andExpect(jsonPath("$.fallbackOverview").value("TourAPI 소개"))
                    .andExpect(jsonPath("$.odiiStatus").value(odiiStatus.name()));
        }
    }

    @Test
    void 존재하지_않는_장소는_기존_404를_유지한다() throws Exception {
        when(odiiService.getAudioGuide(2L))
                .thenThrow(new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND));

        mockMvc.perform(get("/api/odii/places/{placeId}", 2L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_001"));
    }

    @Test
    void TourAPI_콘텐츠_ID가_없는_장소는_기존_400을_유지한다() throws Exception {
        when(odiiService.getAudioGuide(3L))
                .thenThrow(new BusinessException(HeritageErrorCode.NOT_TOUR_API_PLACE));

        mockMvc.perform(get("/api/odii/places/{placeId}", 3L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HERITAGE_001"));
    }
}
