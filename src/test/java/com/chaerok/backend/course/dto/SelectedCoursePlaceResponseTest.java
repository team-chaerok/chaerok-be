package com.chaerok.backend.course.dto;

import com.chaerok.backend.course.entity.CoursePlace;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelectedCoursePlaceResponseTest {

    @Test
    void TourAPI_HS_장소는_Heritage_대상이다() {
        assertThat(response(PlaceSource.TOUR_API, "100", "HS", PlaceCategoryDetail.HERITAGE)
                .heritage()).isTrue();
    }

    @Test
    void 궁남지는_일반_분류여도_Heritage_대상이다() {
        assertThat(response(PlaceSource.TOUR_API, "125984", "NA", PlaceCategoryDetail.NATURE)
                .heritage()).isTrue();
    }

    @Test
    void 일반_TourAPI_장소는_Heritage_비대상이다() {
        assertThat(response(PlaceSource.TOUR_API, "100", "NA", PlaceCategoryDetail.NATURE)
                .heritage()).isFalse();
    }

    @Test
    void Kakao_단독_장소는_과거_상세_분류가_Heritage여도_비대상이다() {
        assertThat(response(PlaceSource.KAKAO_LOCAL, null, null, PlaceCategoryDetail.HERITAGE)
                .heritage()).isFalse();
    }

    @Test
    void 코스_응답의_기존_필드와_Heritage_필드를_직렬화한다() {
        SelectedCoursePlaceResponse placeResponse = response(
                PlaceSource.TOUR_API, "100", "HS", PlaceCategoryDetail.HERITAGE
        );
        SelectedCourseResponse courseResponse = new SelectedCourseResponse(
                20L, 10L, "공주 여행", "ACTIVE", 1, false, List.of(placeResponse)
        );

        JsonNode json = new ObjectMapper().valueToTree(courseResponse);

        assertThat(json.path("courseId").asLong()).isEqualTo(20L);
        assertThat(json.path("regionId").asLong()).isEqualTo(10L);
        assertThat(json.path("title").asText()).isEqualTo("공주 여행");
        assertThat(json.path("status").asText()).isEqualTo("ACTIVE");
        assertThat(json.path("placeCount").asInt()).isEqualTo(1);
        assertThat(json.path("completed").asBoolean()).isFalse();
        JsonNode placeJson = json.path("places").get(0);
        assertThat(placeJson.path("placeId").asLong()).isEqualTo(100L);
        assertThat(placeJson.path("source").asText()).isEqualTo("TOUR_API");
        assertThat(placeJson.path("title").asText()).isEqualTo("공산성");
        assertThat(placeJson.path("categoryGroup").asText()).isEqualTo("TOURISM");
        assertThat(placeJson.path("categoryDetail").asText()).isEqualTo("HERITAGE");
        assertThat(placeJson.path("address").asText()).isEqualTo("충남 공주시");
        assertThat(placeJson.path("sequence").asInt()).isEqualTo(1);
        assertThat(placeJson.path("heritage").asBoolean()).isTrue();
    }

    private SelectedCoursePlaceResponse response(
            PlaceSource source,
            String contentId,
            String lclsSystm1,
            PlaceCategoryDetail categoryDetail
    ) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(100L);
        when(place.getSource()).thenReturn(source);
        when(place.getTitle()).thenReturn("공산성");
        when(place.getCategoryGroup()).thenReturn(PlaceCategoryGroup.TOURISM);
        when(place.getCategoryDetail()).thenReturn(categoryDetail);
        when(place.getAddress()).thenReturn("충남 공주시");
        if (contentId != null) {
            when(place.getTourContentId()).thenReturn(contentId);
        }
        if (lclsSystm1 != null) {
            when(place.getLclsSystm1()).thenReturn(lclsSystm1);
        }

        CoursePlace coursePlace = mock(CoursePlace.class);
        when(coursePlace.getPlace()).thenReturn(place);
        when(coursePlace.getSequence()).thenReturn(1);
        return SelectedCoursePlaceResponse.from(coursePlace);
    }
}
