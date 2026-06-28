package com.chaerok.backend.region.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "regions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_regions_province_city_county",
                        columnNames = {"province_name", "city_county_name"}
                ),
                @UniqueConstraint(
                        name = "uk_regions_ldong_codes",
                        columnNames = {"ldong_regn_cd", "ldong_signgu_cd"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "province_name", nullable = false, length = 20)
    private String provinceName;

    @Column(name = "city_county_name", nullable = false, length = 20)
    private String cityCountyName;

    @Column(name = "ldong_regn_cd", nullable = false, length = 10)
    private String ldongRegnCd;

    @Column(name = "ldong_signgu_cd", nullable = false, length = 10)
    private String ldongSignguCd;

    @Column(name = "service_enabled", nullable = false)
    private boolean serviceEnabled;

    private Region(
            String provinceName,
            String cityCountyName,
            String ldongRegnCd,
            String ldongSignguCd,
            boolean serviceEnabled
    ) {
        this.provinceName = provinceName;
        this.cityCountyName = cityCountyName;
        this.ldongRegnCd = ldongRegnCd;
        this.ldongSignguCd = ldongSignguCd;
        this.serviceEnabled = serviceEnabled;
    }

    public static Region create(
            String provinceName,
            String cityCountyName,
            String ldongRegnCd,
            String ldongSignguCd,
            boolean serviceEnabled
    ) {
        return new Region(
                provinceName,
                cityCountyName,
                ldongRegnCd,
                ldongSignguCd,
                serviceEnabled
        );
    }
}