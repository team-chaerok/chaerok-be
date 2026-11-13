package com.chaerok.backend.odii.external;

import com.chaerok.backend.odii.config.OdiiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.CodecException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class OdiiApiClient {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_NUM_OF_ROWS = 20;
    private static final String RESPONSE_TYPE = "json";
    private static final String SUCCESS_CODE = "0000";

    private static final Duration API_TIMEOUT = Duration.ofSeconds(15);

    private final WebClient webClient;
    private final OdiiProperties properties;

    public OdiiApiClient(OdiiProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * 관광지 키워드 검색
     *
     * @param keyword Odii 관광지 검색 키워드
     * @return 검색된 관광지 목록
     */
    public List<OdiiThemeItem> searchThemes(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/themeSearchList")
                            .queryParam("numOfRows", DEFAULT_NUM_OF_ROWS)
                            .queryParam("pageNo", DEFAULT_PAGE_NO)
                            .queryParam("MobileOS", properties.getMobileOs())
                            .queryParam("MobileApp", properties.getMobileApp())
                            .queryParam("serviceKey", properties.getServiceKey())
                            .queryParam("_type", RESPONSE_TYPE)
                            .queryParam("keyword", keyword)
                            .queryParam("langCode", properties.getLanguageCode())
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(API_TIMEOUT)
                    .onErrorMap(this::isExternalFailure,
                            exception -> new OdiiApiException("themeSearchList 호출 실패", exception))
                    .block();

            validateResponse(root, "themeSearchList", "keyword=" + keyword);

            JsonNode itemNode = getItemNode(root);

            if (itemNode == null
                    || itemNode.isMissingNode()
                    || itemNode.isNull()) {

                return Collections.emptyList();
            }

            List<OdiiThemeItem> items = new ArrayList<>();

            for (JsonNode node : normalizeItems(itemNode)) {
                items.add(new OdiiThemeItem(
                        requiredText(node, "tid"),
                        requiredText(node, "tlid"),
                        requiredText(node, "title"),
                        text(node, "mapX"),
                        text(node, "mapY")
                ));
            }

            return items;
        } catch (OdiiApiException e) {
            log.warn(
                    "Odii themeSearchList 호출 중 예외 발생. keyword={}, exception={}, message={}",
                    keyword,
                    e.getCause() == null ? e.getClass().getSimpleName() : e.getCause().getClass().getSimpleName(),
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage()
            );

            throw e;
        }
    }

    /**
     * 관광지에 연결된 이야기 목록 조회
     *
     * @param tid  관광지 ID
     * @param tlid 관광지 언어 ID
     * @return 관광지 이야기 목록
     */
    public List<OdiiStoryItem> getStories(String tid, String tlid) {
        if (tid == null
                || tid.isBlank()
                || tlid == null
                || tlid.isBlank()) {

            return Collections.emptyList();
        }

        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/storyBasedList")
                            .queryParam("numOfRows", DEFAULT_NUM_OF_ROWS)
                            .queryParam("pageNo", DEFAULT_PAGE_NO)
                            .queryParam("MobileOS", properties.getMobileOs())
                            .queryParam("MobileApp", properties.getMobileApp())
                            .queryParam("serviceKey", properties.getServiceKey())
                            .queryParam("_type", RESPONSE_TYPE)
                            .queryParam("langCode", properties.getLanguageCode())
                            .queryParam("tid", tid)
                            .queryParam("tlid", tlid)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(API_TIMEOUT)
                    .onErrorMap(this::isExternalFailure,
                            exception -> new OdiiApiException("storyBasedList 호출 실패", exception))
                    .block();

            validateResponse(root, "storyBasedList", "tid=" + tid + ", tlid=" + tlid);

            JsonNode itemNode = getItemNode(root);

            if (itemNode == null
                    || itemNode.isMissingNode()
                    || itemNode.isNull()) {

                return Collections.emptyList();
            }

            List<OdiiStoryItem> stories = new ArrayList<>();

            for (JsonNode node : normalizeItems(itemNode)) {
                stories.add(new OdiiStoryItem(
                        text(node, "tid"),
                        text(node, "tlid"),
                        text(node, "stid"),
                        text(node, "stlid"),
                        requiredText(node, "title"),
                        text(node, "mapX"),
                        text(node, "mapY"),
                        text(node, "audioTitle"),
                        text(node, "script"),
                        integer(node, "playTime"),
                        text(node, "audioUrl"),
                        text(node, "langCode"),
                        text(node, "imageUrl")
                ));
            }

            return stories;
        } catch (OdiiApiException e) {
            log.warn(
                    "Odii storyBasedList 호출 중 예외 발생. tid={}, tlid={}, exception={}, message={}",
                    tid,
                    tlid,
                    e.getCause() == null ? e.getClass().getSimpleName() : e.getCause().getClass().getSimpleName(),
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage()
            );

            throw e;
        }
    }

    private void validateResponse(JsonNode root, String operation, String requestContext) {
        if (root == null
                || !root.path("response").isObject()
                || !root.path("response").path("header").isObject()
                || !root.path("response").path("header").path("resultCode").isTextual()) {
            throw new OdiiApiException("Odii 응답 형식이 올바르지 않습니다.");
        }

        if (!isSuccess(root)) {
            logApiFailure(operation, root, requestContext);
            throw new OdiiApiException("Odii API 오류 응답");
        }

        if (!root.path("response").path("body").isObject()) {
            throw new OdiiApiException("Odii 응답 본문 형식이 올바르지 않습니다.");
        }
    }

    private boolean isExternalFailure(Throwable exception) {
        return exception instanceof WebClientException
                || exception instanceof CodecException
                || exception instanceof TimeoutException;
    }

    private boolean isSuccess(JsonNode root) {
        String resultCode = root.path("response")
                .path("header")
                .path("resultCode")
                .asText();

        return SUCCESS_CODE.equals(resultCode);
    }

    private JsonNode getItemNode(JsonNode root) {
        JsonNode items = root.path("response")
                .path("body")
                .path("items");

        if (items.isMissingNode() || items.isNull()
                || (items.isArray() && items.isEmpty())) {
            return null;
        }
        if (!items.isObject()) {
            throw new OdiiApiException("Odii items 형식이 올바르지 않습니다.");
        }
        return items.path("item");
    }

    /**
     * Odii 응답은 결과 개수에 따라 item이 배열 또는 단일 객체로 내려올 수 있어
     * 항상 순회 가능한 목록으로 정규화한다.
     */
    private List<JsonNode> normalizeItems(JsonNode itemNode) {
        if (itemNode.isArray()) {
            List<JsonNode> items = new ArrayList<>();
            itemNode.forEach(node -> {
                if (!node.isObject()) {
                    throw new OdiiApiException("Odii item 형식이 올바르지 않습니다.");
                }
                items.add(node);
            });
            return items;
        }

        if (itemNode.isObject()) {
            return List.of(itemNode);
        }

        throw new OdiiApiException("Odii item 형식이 올바르지 않습니다.");
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null) {
            throw new OdiiApiException("Odii 필수 필드 누락: " + fieldName);
        }
        return value;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();

        return text.isBlank() ? null : text.trim();
    }

    private Integer integer(JsonNode node, String fieldName) {
        String value = text(node, fieldName);

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            log.debug(
                    "Odii 숫자 필드 변환 실패. fieldName={}, value={}",
                    fieldName,
                    value
            );

            return null;
        }
    }

    private void logApiFailure(
            String operation,
            JsonNode root,
            String requestContext
    ) {
        if (root == null) {
            log.warn(
                    "Odii API 응답이 없습니다. operation={}, request={}",
                    operation,
                    requestContext
            );

            return;
        }

        String resultCode = root.path("response")
                .path("header")
                .path("resultCode")
                .asText();

        String resultMessage = root.path("response")
                .path("header")
                .path("resultMsg")
                .asText();

        log.warn(
                "Odii API 호출 실패. operation={}, request={}, resultCode={}, resultMessage={}",
                operation,
                requestContext,
                resultCode,
                resultMessage
        );
    }
}
