# 앱 심사용 Review Mode

## 목적

Review Mode는 앱 심사자가 실제 충남 서비스 지역에 있지 않아도 채록의 운영 흐름을 검증할 수 있도록 위치 입력만 테스트 장소 좌표로 대체하는 기능이다.

방문 인증, 사진 촬영, 사진 업로드, Visit 생성, 현상 요청, RenderJob, SQS, Lambda, ZIP/Reel 생성, FCM은 운영 흐름을 그대로 사용한다.

## 백엔드 책임

- `users.review_mode`로 심사용 계정을 서버에서 식별한다.
- `GET /api/users/me/review-mode`에서 심사용 계정에게 공주 지역과 테스트 장소 3개를 반환한다.
- 테스트 장소 응답에는 `placeId`, `title`, `categoryGroup`, `address`, `latitude`, `longitude`를 포함한다.
- 테스트 장소는 `TOURISM`, `FOOD`, `CAFE_DESSERT`를 정확히 하나씩 구성한다.
- `/exit`은 기존 FilmRoll 종료 검증을 그대로 수행한다.
- `/exit`은 일반 사용자와 심사용 계정 모두 `developAvailableAt = exitedAt + 1시간`으로 저장해 기존 FilmRoll/DB 시간 규칙을 유지한다.
- `/develop`에서 심사용 계정만 1시간 대기 검사만 면제하며, 지역 이탈·사진·Visit 등 다른 검증은 그대로 적용한다.
- `/develop` 이후 파이프라인에는 Review Mode 분기를 추가하지 않는다.

## 프론트엔드 계약

프론트엔드는 `ReviewLocationProvider`를 기존 `LocationProvider` 구조에 연결한다. 심사용 계정에서 사용자가 선택한 테스트 장소의 좌표를 현재 위치로 공급하되 기존 `accuracy <= 50m`, `distance <= 100m` 검증을 그대로 실행한다.

Review Mode는 방문 성공을 강제로 만드는 기능이 아니다. 위치만 테스트 좌표로 대체하며 실제 카메라 촬영, Photo 업로드 완료, `placeId + photoId` 기반 Visit 생성은 운영 흐름과 동일하게 수행한다.

## 기본 테스트 장소

서버 기본 설정은 DB PK가 아니라 TourAPI `contentId`를 사용한다. 실제 응답의 `placeId`는 현재 DB에서 조회한 값을 반환한다.

| 분류 | 장소 | TourAPI contentId |
| --- | --- | --- |
| TOURISM | 공주 공산성 [유네스코 세계유산] | `125949` |
| FOOD | 진흥각 | `2738735` |
| CAFE_DESSERT | 공다방 | `2876760` |

필요하면 Spring Boot relaxed binding 환경변수로 기본값을 교체할 수 있다.

```text
CHAEROK_REVIEW_MODE_PROVINCE_NAME=충청남도
CHAEROK_REVIEW_MODE_CITY_COUNTY_NAME=공주시
CHAEROK_REVIEW_MODE_TEST_PLACE_TOUR_CONTENT_IDS=125949,2738735,2876760
```

## 심사용 계정 지정

심사용 계정 여부를 클라이언트가 변경하는 API는 제공하지 않는다. 운영자가 대상 사용자를 확인한 뒤 DB에서만 설정한다.

```sql
UPDATE users
SET review_mode = TRUE
WHERE id = :review_user_id;
```

심사 종료 후에는 필요에 따라 다시 `FALSE`로 변경한다.
