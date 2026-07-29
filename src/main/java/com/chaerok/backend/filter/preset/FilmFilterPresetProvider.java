package com.chaerok.backend.filter.preset;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilmFilterPresetProvider {

    private final List<FilmFilterPreset> presets = List.of(
            new FilmFilterPreset(
                    "gongju_baekje_love",
                    "백제의 연(戀)",
                    "공주의 녹음과 오래된 성곽의 따뜻한 필름톤",
                    4,
                    0.96,
                    8,
                    9,
                    7,
                    12,
                    "filter/overlays/gongju_overlay.png",
                    0.38,
                    "overlay"
            ),
            new FilmFilterPreset(
                    "buyeo_baekje_dream",
                    "백제의 몽(夢)",
                    "부여의 흐릿하고 몽환적인 파스텔 필름톤",
                    3,
                    0.89,
                    2,
                    17,
                    9,
                    30,
                    "filter/overlays/buyeo_overlay.png",
                    0.36,
                    "overlay"
            ),
            new FilmFilterPreset(
                    "seosan_warm_sunset",
                    "서산의 잔상",
                    "서산의 노을빛과 오래된 필름 질감이 섞인 따뜻한 필름톤",
                    5,
                    0.95,
                    16,
                    10,
                    8,
                    14,
                    "filter/overlays/Seosan_overlay.jpg",
                    0.24,
                    "soft_light"
            ),
            new FilmFilterPreset(
                    "yesan_old_memory",
                    "예산의 결",
                    "예산의 오래된 골목과 시장의 색을 닮은 빈티지 필름톤",
                    1,
                    0.93,
                    -4,
                    13,
                    8,
                    13,
                    "filter/overlays/yesan_overlay.jpg",
                    0.30,
                    "multiply"
            )
    );

    public FilmFilterPreset getByFilterId(String filterId) {
        return presets.stream()
                .filter(preset -> preset.filterId().equals(filterId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 필터입니다: " + filterId)
                );
    }

    public List<FilmFilterPreset> getAll() {
        return presets;
    }
}