package com.chaerok.backend.place.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiPlaceIntroResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("item이 단일 객체여도 정상적으로 역직렬화한다")
    void deserializeSingleItem() throws Exception {
        // given
        String json = """
                {
                  "response": {
                    "header": {
                      "resultCode": "0000",
                      "resultMsg": "OK"
                    },
                    "body": {
                      "items": {
                        "item": {
                          "usetime": "09:00~18:00",
                          "infocenter": "041-000-0000"
                        }
                      }
                    }
                  }
                }
                """;

        // when
        TourApiPlaceIntroResponse response =
                objectMapper.readValue(
                        json,
                        TourApiPlaceIntroResponse.class
                );

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getFirstItem()).isNotNull();
        assertThat(response.getFirstItem().openingHours())
                .isEqualTo("09:00~18:00");
        assertThat(response.getFirstItem().phone())
                .isEqualTo("041-000-0000");
    }

    @Test
    @DisplayName("item이 배열이어도 정상적으로 역직렬화한다")
    void deserializeItemArray() throws Exception {
        // given
        String json = """
                {
                  "response": {
                    "header": {
                      "resultCode": "0000",
                      "resultMsg": "OK"
                    },
                    "body": {
                      "items": {
                        "item": [
                          {
                            "usetime": "09:00~18:00",
                            "infocenter": "041-000-0000"
                          },
                          {
                            "usetime": "10:00~19:00",
                            "infocenter": "041-111-1111"
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        // when
        TourApiPlaceIntroResponse response =
                objectMapper.readValue(
                        json,
                        TourApiPlaceIntroResponse.class
                );

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.response().body().items().item())
                .hasSize(2);

        assertThat(response.getFirstItem().openingHours())
                .isEqualTo("09:00~18:00");
    }

    @Test
    @DisplayName("추가 정보가 없는 경우 null을 반환한다")
    void returnsNullWhenItemsAreMissing() throws Exception {
        // given
        String json = """
                {
                  "response": {
                    "header": {
                      "resultCode": "0000",
                      "resultMsg": "OK"
                    },
                    "body": {
                      "items": {}
                    }
                  }
                }
                """;

        // when
        TourApiPlaceIntroResponse response =
                objectMapper.readValue(
                        json,
                        TourApiPlaceIntroResponse.class
                );

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getFirstItem()).isNull();
    }
}