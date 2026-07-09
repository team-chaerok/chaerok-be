package com.chaerok.backend.place.external;

import java.util.List;

public record KakaoPlaceResponse(
        List<KakaoPlaceItem> documents
) {

    public List<KakaoPlaceItem> getItems() {
        if (documents == null) {
            return List.of();
        }

        return documents;
    }
}