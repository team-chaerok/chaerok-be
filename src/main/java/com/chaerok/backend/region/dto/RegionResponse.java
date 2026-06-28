package com.chaerok.backend.region.dto;

import com.chaerok.backend.region.entity.Region;

public record RegionResponse(
        boolean serviceArea,
        Long regionId,
        String provinceName,
        String cityCountyName
) {

    public static RegionResponse from(Region region) {
        return new RegionResponse(
                true,
                region.getId(),
                region.getProvinceName(),
                region.getCityCountyName()
        );
    }

    public static RegionResponse unsupported(
            String provinceName,
            String cityCountyName
    ) {
        return new RegionResponse(
                false,
                null,
                provinceName,
                cityCountyName
        );
    }
}