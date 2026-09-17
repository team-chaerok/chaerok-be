package com.chaerok.backend.place.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TourApiPlaceIntroItem(
        @JsonProperty("usetime")
        String useTime,

        @JsonProperty("infocenter")
        String infoCenter,

        @JsonProperty("opentimefood")
        String openTimeFood,

        @JsonProperty("infocenterfood")
        String infoCenterFood,

        @JsonProperty("usetimeculture")
                String useTimeCulture,

        @JsonProperty("infocenterculture")
        String infoCenterCulture,

        @JsonProperty("usetimeleports")
        String useTimeLeports,

        @JsonProperty("infocenterleports")
        String infoCenterLeports,

        @JsonProperty("opentime")
        String openTime,

        @JsonProperty("playtime")
        String playTime,

        @JsonProperty("sponsor1tel")
        String sponsor1Tel,

        @JsonProperty("sponsor2tel")
        String sponsor2Tel
) {

    public String openingHours() {
        return firstNonBlank(
                useTime,
                openTimeFood,
                useTimeCulture,
                useTimeLeports,
                openTime,
                playTime
        );
    }

    public String phone() {
        return firstNonBlank(
                infoCenter,
                infoCenterFood,
                infoCenterCulture,
                infoCenterLeports,
                sponsor1Tel,
                sponsor2Tel
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}