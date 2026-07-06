package com.chaerok.backend.place.entity;

import com.chaerok.backend.region.entity.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "tour_content_id", nullable = false, unique = true, length = 50)
    private String tourContentId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "first_image_url", length = 500)
    private String firstImageUrl;

    @Column(name = "ldong_regn_cd", length = 10)
    private String lDongRegnCd;

    @Column(name = "ldong_signgu_cd", length = 10)
    private String lDongSignguCd;

    @Column(name = "lcls_systm1", length = 20)
    private String lclsSystm1;

    @Column(name = "lcls_systm2", length = 20)
    private String lclsSystm2;

    @Column(name = "lcls_systm3", length = 20)
    private String lclsSystm3;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_group", nullable = false, length = 30)
    private PlaceCategoryGroup categoryGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_detail", length = 30)
    private PlaceCategoryDetail categoryDetail;

    @Column(name = "is_representative", nullable = false)
    private boolean representative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlaceSource source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Place create(
            Region region,
            String tourContentId,
            String title,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String firstImageUrl,
            String lDongRegnCd,
            String lDongSignguCd,
            String lclsSystm1,
            String lclsSystm2,
            String lclsSystm3,
            PlaceCategoryGroup categoryGroup,
            PlaceCategoryDetail categoryDetail,
            boolean representative,
            PlaceSource source
    ) {
        Place place = new Place();
        place.region = region;
        place.tourContentId = tourContentId;
        place.title = title;
        place.address = address;
        place.latitude = latitude;
        place.longitude = longitude;
        place.firstImageUrl = firstImageUrl;
        place.lDongRegnCd = lDongRegnCd;
        place.lDongSignguCd = lDongSignguCd;
        place.lclsSystm1 = lclsSystm1;
        place.lclsSystm2 = lclsSystm2;
        place.lclsSystm3 = lclsSystm3;
        place.categoryGroup = categoryGroup;
        place.categoryDetail = categoryDetail;
        place.representative = representative;
        place.source = source;
        return place;
    }

    public void updateFromTourApi(
            String title,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String firstImageUrl,
            String lDongRegnCd,
            String lDongSignguCd,
            String lclsSystm1,
            String lclsSystm2,
            String lclsSystm3,
            PlaceCategoryGroup categoryGroup,
            PlaceCategoryDetail categoryDetail
    ) {
        this.title = title;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.firstImageUrl = firstImageUrl;
        this.lDongRegnCd = lDongRegnCd;
        this.lDongSignguCd = lDongSignguCd;
        this.lclsSystm1 = lclsSystm1;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
        this.categoryGroup = categoryGroup;
        this.categoryDetail = categoryDetail;
    }
}