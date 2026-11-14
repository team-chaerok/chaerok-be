package com.chaerok.backend.place.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourApiPlaceResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesItemsObject() throws Exception {
        String json = """
                {
                  "response": {
                    "header": {"resultCode": "0000", "resultMsg": "OK"},
                    "body": {
                      "items": {"item": [{"contentid": "123", "title": "공산성"}]},
                      "numOfRows": 20,
                      "pageNo": 1,
                      "totalCount": 1
                    }
                  }
                }
                """;

        TourApiPlaceResponse response = objectMapper.readValue(json, TourApiPlaceResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).contentId()).isEqualTo("123");
        assertThat(response.getItems().get(0).title()).isEqualTo("공산성");
    }

    @Test
    void deserializesEmptyItemsStringAsNoPlaces() throws Exception {
        String json = """
                {
                  "response": {
                    "header": {"resultCode": "0000", "resultMsg": "OK"},
                    "body": {
                      "items": "",
                      "numOfRows": 20,
                      "pageNo": 1,
                      "totalCount": 0
                    }
                  }
                }
                """;

        TourApiPlaceResponse response = objectMapper.readValue(json, TourApiPlaceResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void rejectsUnexpectedItemsTypes() {
        String json = """
                {"response":{"header":{"resultCode":"0000"},"body":{"items":%s,"totalCount":0}}}
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json.formatted("\"unexpected\""), TourApiPlaceResponse.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> objectMapper.readValue(json.formatted("[]"), TourApiPlaceResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }
}
