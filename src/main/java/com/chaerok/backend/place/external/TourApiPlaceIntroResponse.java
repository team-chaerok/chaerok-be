package com.chaerok.backend.place.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiPlaceIntroResponse(
        @JsonProperty("response")
        Response response
) {

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

    public TourApiPlaceIntroItem getFirstItem() {
        if (!isSuccess()
                || response.body() == null
                || response.body().items() == null
                || response.body().items().item() == null
                || response.body().items().item().isEmpty()) {
            return null;
        }

        return response.body().items().item().get(0);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            @JsonProperty("header")
            Header header,

            @JsonProperty("body")
            Body body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            @JsonProperty("resultCode")
            String resultCode,

            @JsonProperty("resultMsg")
            String resultMsg
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            @JsonProperty("items")
            Items items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(
            @JsonProperty("item")
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            List<TourApiPlaceIntroItem> item
    ) {
    }
}