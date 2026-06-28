package com.chaerok.backend.region.service;

import com.chaerok.backend.region.dto.RegionResponse;
import com.chaerok.backend.region.dto.ResolveRegionRequest;
import com.chaerok.backend.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private static final Map<String, String> PROVINCE_ALIASES = Map.of(
            "충청남도", "충청남도",
            "충남", "충청남도"
    );

    private static final Map<String, String> CITY_COUNTY_ALIASES = Map.of(
            "공주시", "공주시",
            "공주", "공주시",
            "부여군", "부여군",
            "부여", "부여군",
            "서산시", "서산시",
            "서산", "서산시",
            "예산군", "예산군",
            "예산", "예산군"
    );

    private final RegionRepository regionRepository;

    public RegionResponse resolve(ResolveRegionRequest request) {
        String provinceName = normalizeProvinceName(request.provinceName());
        String cityCountyName = normalizeCityCountyName(request.cityCountyName());

        return regionRepository
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        provinceName,
                        cityCountyName
                )
                .map(RegionResponse::from)
                .orElseGet(() ->
                        RegionResponse.unsupported(
                                provinceName,
                                cityCountyName
                        )
                );
    }

    private String normalizeProvinceName(String provinceName) {
        String trimmedName = provinceName.trim();
        return PROVINCE_ALIASES.getOrDefault(trimmedName, trimmedName);
    }

    private String normalizeCityCountyName(String cityCountyName) {
        String trimmedName = cityCountyName.trim();
        return CITY_COUNTY_ALIASES.getOrDefault(trimmedName, trimmedName);
    }
}