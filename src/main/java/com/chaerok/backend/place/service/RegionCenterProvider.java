package com.chaerok.backend.place.service;

import com.chaerok.backend.region.entity.Region;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RegionCenterProvider {

    private static final String GONGJU_SIGNGU_CODE = "150";
    private static final String BUYEO_SIGNGU_CODE = "760";
    private static final String SEOSAN_SIGNGU_CODE = "210";
    private static final String YESAN_SIGNGU_CODE = "810";

    public RegionCenter getCenter(Region region) {
        return switch (region.getLdongSignguCd()) {
            case GONGJU_SIGNGU_CODE -> new RegionCenter(
                    new BigDecimal("127.1190"),
                    new BigDecimal("36.4465")
            );
            case BUYEO_SIGNGU_CODE -> new RegionCenter(
                    new BigDecimal("126.9119"),
                    new BigDecimal("36.2757")
            );
            case SEOSAN_SIGNGU_CODE -> new RegionCenter(
                    new BigDecimal("126.4503"),
                    new BigDecimal("36.7845")
            );
            case YESAN_SIGNGU_CODE -> new RegionCenter(
                    new BigDecimal("126.8447"),
                    new BigDecimal("36.6829")
            );
            default -> new RegionCenter(
                    new BigDecimal("126.8000"),
                    new BigDecimal("36.5000")
            );
        };
    }

    public record RegionCenter(
            BigDecimal longitude,
            BigDecimal latitude
    ) {
    }
}