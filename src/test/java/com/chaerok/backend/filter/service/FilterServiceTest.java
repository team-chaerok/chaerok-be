package com.chaerok.backend.filter.service;

import com.chaerok.backend.filter.dto.FilterResponse;
import com.chaerok.backend.filter.preset.FilmFilterPreset;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilterServiceTest {

    @Mock
    private FilmFilterPresetProvider presetProvider;

    private FilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new FilterService(presetProvider);
    }

    @Test
    @DisplayName("등록된 필터 프리셋을 응답 목록으로 변환한다")
    void getFilters() {
        // given
        FilmFilterPreset gongjuPreset = new FilmFilterPreset(
                "gongju",
                "공주",
                "공주의 따뜻한 필름톤",
                4,
                0.96,
                8,
                9,
                7,
                12,
                "filter/overlays/gongju_overlay.png",
                0.38,
                "overlay"
        );

        FilmFilterPreset buyeoPreset = new FilmFilterPreset(
                "buyeo",
                "부여",
                "부여의 파스텔 필름톤",
                3,
                0.89,
                2,
                17,
                9,
                30,
                "filter/overlays/buyeo_overlay.png",
                0.36,
                "overlay"
        );

        when(presetProvider.getAll())
                .thenReturn(List.of(
                        gongjuPreset,
                        buyeoPreset
                ));

        // when
        List<FilterResponse> result =
                filterService.getFilters();

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(
                        FilterResponse::filterId,
                        FilterResponse::name,
                        FilterResponse::description
                )
                .containsExactly(
                        tuple(
                                "gongju",
                                "공주",
                                "공주의 따뜻한 필름톤"
                        ),
                        tuple(
                                "buyeo",
                                "부여",
                                "부여의 파스텔 필름톤"
                        )
                );

        verify(presetProvider).getAll();
        verifyNoMoreInteractions(presetProvider);
    }

    @Test
    @DisplayName("등록된 필터 프리셋이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoPresetExists() {
        // given
        when(presetProvider.getAll())
                .thenReturn(List.of());

        // when
        List<FilterResponse> result =
                filterService.getFilters();

        // then
        assertThat(result).isEmpty();

        verify(presetProvider).getAll();
        verifyNoMoreInteractions(presetProvider);
    }

    @Test
    @DisplayName("필터 프리셋의 내부 처리 설정은 응답에 포함하지 않는다")
    void responseContainsOnlyPublicFilterInformation() {
        // given
        FilmFilterPreset preset = new FilmFilterPreset(
                "seosan",
                "서산",
                "서산의 따뜻한 필름톤",
                5,
                0.95,
                16,
                10,
                8,
                14,
                "filter/overlays/seosan_overlay.jpg",
                0.24,
                "soft_light"
        );

        when(presetProvider.getAll())
                .thenReturn(List.of(preset));

        // when
        List<FilterResponse> result =
                filterService.getFilters();

        // then
        assertThat(result).containsExactly(
                new FilterResponse(
                        "seosan",
                        "서산",
                        "서산의 따뜻한 필름톤"
                )
        );

        verify(presetProvider).getAll();
        verifyNoMoreInteractions(presetProvider);
    }
}