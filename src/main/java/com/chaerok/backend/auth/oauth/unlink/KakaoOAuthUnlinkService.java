package com.chaerok.backend.auth.oauth.unlink;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class KakaoOAuthUnlinkService {

    private static final String KAKAO_UNLINK_URL =
            "https://kapi.kakao.com/v1/user/unlink";

    private final RestClient restClient;
    private final String adminKey;

    public KakaoOAuthUnlinkService(
            RestClient.Builder restClientBuilder,
            @Value("${oauth.kakao.admin-key}") String adminKey
    ) {
        this.restClient = restClientBuilder.build();
        this.adminKey = adminKey;
    }

    public void unlink(String providerUserId) {
        MultiValueMap<String, String> requestBody =
                new LinkedMultiValueMap<>();

        requestBody.add("target_id_type", "user_id");
        requestBody.add("target_id", providerUserId);

        try {
            restClient.post()
                    .uri(KAKAO_UNLINK_URL)
                    .header(
                            "Authorization",
                            "KakaoAK " + adminKey
                    )
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "카카오 연결 해제 성공: providerUserId={}",
                    providerUserId
            );
        } catch (RestClientException exception) {
            // 연결 해제가 실패해도 채록 회원 탈퇴는 계속 진행
            log.warn(
                    "카카오 연결 해제 실패: providerUserId={}",
                    providerUserId,
                    exception
            );
        }
    }
}