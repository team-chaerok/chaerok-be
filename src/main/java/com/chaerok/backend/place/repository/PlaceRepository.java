package com.chaerok.backend.place.repository;

import com.chaerok.backend.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByRegionId(Long regionId);

    List<Place> findByRegionIdAndRepresentativeTrue(Long regionId);

    Optional<Place> findByTourContentId(String tourContentId);

    boolean existsByTourContentId(String tourContentId);
}