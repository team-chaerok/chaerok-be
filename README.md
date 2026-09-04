# 채록 비동기 필름 현상 파이프라인

채록 백엔드의 필름 롤 촬영, 현상 요청, Lambda 렌더링, 결과 반영 흐름을 구현합니다.

## 구현 범위

- 사용자당 미완료 필름 롤 최대 1개 정책
- 필름 롤 생성, 현재 롤 조회, 상세 조회
- FilmRoll별 방문 인증과 관광지·식당·카페 방문 진행도 조회
- 프론트 판정 기반 지역 이탈 확정과 계정 정책 기반 현상 대기
- 현상 가능 시각이 지난 FilmRoll 자동 현상 요청
- 사진 업로드용 Presigned URL 발급
- 사진 업로드 완료 처리 및 사진 목록 조회
- 통합 현상 요청 API
- 요청 SQS를 통한 Lambda 비동기 실행
- 지역별 필름 필터 적용
- 이미지 밝기 기반 `LANDSCAPE`·`NIGHT` 자동 판정
- 필터 JPEG, ZIP, 1080×1920 H.264 MP4 생성
- `manifest.json` 기반 Lambda 멱등 처리
- 결과 SQS 발행 및 Spring 결과 소비
- `FilmRoll`, `Photo`, `RenderJob` 상태 반영
- 완료 결과 Presigned 다운로드 URL 제공
- 실패 재시도, 중복 결과, 이전 작업 지연 결과 처리
- 한국 시간 기준 완료·만료 시각 처리
- 만료된 결과 다운로드 차단

## 공개 API

```text
GET  /api/users/me/review-mode

POST /api/film-rolls
GET  /api/film-rolls/current
GET  /api/film-rolls/{filmRollId}
POST /api/film-rolls/{filmRollId}/exit

POST /api/film-rolls/{filmRollId}/visits
GET  /api/film-rolls/{filmRollId}/visits

POST /api/film-rolls/{filmRollId}/photos/upload-url
POST /api/film-rolls/{filmRollId}/photos/{photoId}/complete
GET  /api/film-rolls/{filmRollId}/photos

POST /api/film-rolls/{filmRollId}/develop
GET  /api/film-rolls/{filmRollId}/results
```

기존 `/ready`, `/render-jobs` API는 하위 호환을 위해 유지하지만 Swagger에서는 숨깁니다.

방문 인증은 프론트가 GPS와 거리 검증을 완료한 뒤 `placeId`만 전달합니다. 백엔드는 GPS 좌표·정확도·거리·이동 경로를 받거나 저장하지 않습니다. 현상에는 `TOURISM`, `FOOD`, `CAFE_DESSERT` 세 유형을 각각 1곳 이상 방문한 기록이 필요합니다.

지역 이탈 역시 프론트가 GPS·행정구역·연속 외부 판정과 사용자 확인을 완료한 뒤 `/exit`만 호출합니다. 이탈 시점에 Visit 3유형 조건과 사진 1장 이상을 모두 충족한 필름 롤은 `exitedAt`과 `developAvailableAt`을 저장합니다. 일반 사용자와 서버에서 지정한 심사용 계정 모두 `developAvailableAt = exitedAt + 1시간`을 저장하며, 심사용 계정은 `/develop` 요청 시 서버에서 1시간 대기 검사만 면제합니다. Visit 조건 또는 사진 조건이 하나라도 부족한 필름 롤은 심사용 계정도 동일하게 이탈 사실을 기록한 뒤 `EXPIRED`로 종료합니다. 현상 가능 시각이 지난 FilmRoll은 스케줄러가 기존 SQS 현상 파이프라인을 자동으로 재사용합니다.

심사용 계정은 `GET /api/users/me/review-mode`에서 공주 지역과 `TOURISM`, `FOOD`, `CAFE_DESSERT` 테스트 장소 3곳의 실제 DB `placeId`, 제목, 카테고리, 주소, 좌표를 받습니다. 심사용 모드는 방문 성공을 우회하지 않으며, 프론트는 해당 좌표를 `ReviewLocationProvider`에 공급해 기존 정확도·거리 검증을 그대로 수행해야 합니다. 사진 촬영·업로드, Photo 상태 검증, Visit 생성과 이후 현상 파이프라인은 운영 흐름을 그대로 사용합니다.

## 필름 롤 상태

```text
CAPTURING
READY
QUEUED
PROCESSING
COMPLETED
FAILED
EXPIRED
```

`FAILED`는 동일한 필름 롤로 현상을 재시도할 수 있는 미완료 상태입니다.

## 필터 프리셋

```text
gongju / 공주
buyeo  / 부여
seosan / 서산
yesan  / 예산
```

프론트는 `hasFace`나 `sceneType`을 전달하지 않습니다. Lambda가 이미지 밝기와 어두운 픽셀 비율을 분석해 `LANDSCAPE` 또는 `NIGHT`를 자동 결정합니다.

## 현상 요청 메시지

Spring이 요청 SQS에 발행하는 메시지 스키마는 버전 2입니다.

```json
{
  "schemaVersion": 2,
  "renderJobId": "133d6ee3-a120-4df3-8ba3-f60adbdd64d6",
  "filmRollId": 999001,
  "userId": 999001,
  "regionId": 1,
  "filterId": "gongju",
  "photos": [
    {
      "photoId": 1,
      "sequence": 1,
      "originalObjectKey": "users/999001/rolls/999001/original/001.jpg",
      "takenAt": "2026-08-07T00:10:00"
    }
  ]
}
```

## 결과 메시지

Lambda가 결과 SQS에 발행하는 결과 메시지 스키마는 버전 1입니다.

```json
{
  "schemaVersion": 1,
  "eventType": "CHAEROK_RENDER_COMPLETED",
  "requestMessageId": "request-sqs-message-id",
  "renderJobId": "133d6ee3-a120-4df3-8ba3-f60adbdd64d6",
  "filmRollId": 999001,
  "userId": 999001,
  "status": "COMPLETED",
  "attempt": 1,
  "retryable": false,
  "filteredPhotos": [],
  "zipObjectKey": "...zip",
  "reelObjectKey": "...mp4",
  "manifestObjectKey": ".../manifest.json",
  "occurredAt": "2026-08-07T00:00:00Z",
  "errorCode": null,
  "errorMessage": null
}
```

## 결과 S3 경로

```text
users/{userId}/rolls/{filmRollId}/render-jobs/{renderJobId}/
├─ filtered/001.jpg
├─ filtered/002.jpg
├─ export/chaerok_{regionId}_{filmRollId}_{job8}.zip
├─ export/chaerok_{regionId}_{filmRollId}_{job8}.mp4
└─ manifest.json
```

같은 `renderJobId`가 다시 처리되면 기존 `manifest.json`을 사용해 완료 결과를 재발행합니다. Spring 결과 소비자는 `renderJobId` 기준으로 멱등 처리합니다.

## 테스트

Spring 전체 테스트:

```powershell
.\gradlew.bat clean test --no-build-cache --rerun-tasks
```

Lambda 전체 테스트:

```powershell
.\gradlew.bat -p lambda\render clean test --no-build-cache --rerun-tasks
```

로컬 Lambda 테스트:

```powershell
cd D:\chaerok-be\lambda\render
.\scripts\build-local.ps1
.\scripts\run-local.ps1
.\scripts\invoke-local.ps1
```

## 배포 전 확인

요청 메시지 스키마와 필터 ID가 변경됐으므로 Spring과 Lambda를 같은 버전으로 배포해야 합니다.

```text
요청 schemaVersion: 2
결과 schemaVersion: 1
필터 ID: gongju, buyeo, seosan, yesan
```

배포 후 실제 AWS 흐름을 다시 검증합니다.

```text
Spring 현상 요청
→ 요청 SQS
→ Lambda
→ S3 필터 JPEG·ZIP·MP4
→ 결과 SQS
→ Spring DB 반영
→ 결과 조회 API
```

## 남은 범위

- FCM 현상 완료 알림
- 운영 배포 설정 정리
