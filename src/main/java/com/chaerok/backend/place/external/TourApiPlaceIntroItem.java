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
        String infoCenterFood
) {

    public String openingHours() {
        return firstNonBlank(useTime, openTimeFood);
    }

    public String phone() {
        return firstNonBlank(infoCenter, infoCenterFood);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }
}