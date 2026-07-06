package com.chaerok.backend.place.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TourApiPlaceItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("title")
        String title,

        @JsonProperty("addr1")
        String address,

        @JsonProperty("mapy")
        String latitude,

        @JsonProperty("mapx")
        String longitude,

        @JsonProperty("firstimage")
        String firstImageUrl,

        @JsonProperty("lDongRegnCd")
        String lDongRegnCd,

        @JsonProperty("lDongSignguCd")
        String lDongSignguCd,

        @JsonProperty("lclsSystm1")
        String lclsSystm1,

        @JsonProperty("lclsSystm2")
        String lclsSystm2,

        @JsonProperty("lclsSystm3")
        String lclsSystm3,

        @JsonProperty("overview")
        String overview
) {
}