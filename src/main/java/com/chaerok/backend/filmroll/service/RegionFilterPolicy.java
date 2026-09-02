package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.region.entity.Region;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RegionFilterPolicy {

    private static final Map<RegionKey, String> FILTER_BY_REGION = Map.of(
            new RegionKey("충청남도", "공주시"), "gongju",
            new RegionKey("충청남도", "부여군"), "buyeo",
            new RegionKey("충청남도", "서산시"), "seosan",
            new RegionKey("충청남도", "예산군"), "yesan"
    );

    public void validate(
            Region region,
            String filterId
    ) {
        if (region == null) {
            throw new IllegalArgumentException("지역은 필수입니다.");
        }

        if (filterId == null || filterId.isBlank()) {
            throw new IllegalArgumentException("필터 ID는 필수입니다.");
        }

        RegionKey regionKey = new RegionKey(
                region.getProvinceName(),
                region.getCityCountyName()
        );
        String expectedFilterId = FILTER_BY_REGION.get(regionKey);
        String normalizedFilterId = filterId.trim();

        if (!normalizedFilterId.equals(expectedFilterId)) {
            throw new BusinessException(
                    FilmRollErrorCode.INVALID_REGION_FILTER
            );
        }
    }

    private record RegionKey(
            String provinceName,
            String cityCountyName
    ) {
    }
}
