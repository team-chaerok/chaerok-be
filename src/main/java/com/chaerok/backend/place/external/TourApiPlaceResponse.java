package com.chaerok.backend.place.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TourApiPlaceResponse(
        @JsonProperty("response")
        TourApiResponse response
) {

    public List<TourApiPlaceItem> getItems() {
        if (response == null || response.body == null || response.body.items == null || response.body.items.item == null) {
            return List.of();
        }

        return response.body.items.item;
    }

    public record TourApiResponse(
            @JsonProperty("header")
            TourApiHeader header,

            @JsonProperty("body")
            TourApiBody body
    ) {
    }

    public record TourApiHeader(
            @JsonProperty("resultCode")
            String resultCode,

            @JsonProperty("resultMsg")
            String resultMsg
    ) {
    }

    public record TourApiBody(
            @JsonProperty("items")
            TourApiItems items,

            @JsonProperty("numOfRows")
            Integer numOfRows,

            @JsonProperty("pageNo")
            Integer pageNo,

            @JsonProperty("totalCount")
            Integer totalCount
    ) {
    }

    public record TourApiItems(
            @JsonProperty("item")
            List<TourApiPlaceItem> item
    ) {
    }
}