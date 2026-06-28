package com.chaerok.backend.region.repository;

import com.chaerok.backend.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
            String provinceName,
            String cityCountyName
    );
}