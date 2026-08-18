package com.chaerok.backend.place.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TourApiPlaceResponse(
        @JsonProperty("response")
        Response response
) {

    public List<TourApiPlaceItem> getItems() {
        if (!isSuccess()) {
            return List.of();
        }

        if (response.body() == null
                || response.body().items() == null
                || response.body().items().item() == null) {
            return List.of();
        }

        return response.body().items().item();
    }

    public boolean isSuccess() {
        return response != null
                && response.header() != null
                && "0000".equals(response.header().resultCode());
    }

    public String getResultCode() {
        if (response == null || response.header() == null) {
            return null;
        }

        return response.header().resultCode();
    }

    public String getResultMsg() {
        if (response == null || response.header() == null) {
            return null;
        }

        return response.header().resultMsg();
    }

    public int getTotalCount() {
        if (response == null
                || response.body() == null
                || response.body().totalCount() == null) {
            return 0;
        }

        return response.body().totalCount();
    }

    public record Response(
            @JsonProperty("header")
            Header header,

            @JsonProperty("body")
            Body body
    ) {
    }

    public record Header(
            @JsonProperty("resultCode")
            String resultCode,

            @JsonProperty("resultMsg")
            String resultMsg
    ) {
    }

    public record Body(
            @JsonProperty("items")
            Items items,

            @JsonProperty("numOfRows")
            Integer numOfRows,

            @JsonProperty("pageNo")
            Integer pageNo,

            @JsonProperty("totalCount")
            Integer totalCount
    ) {
    }

    public record Items(
            @JsonProperty("item")
            List<TourApiPlaceItem> item
    ) {
    }
}