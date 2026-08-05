# 채록 Render Lambda v2 — Result Publish v1

현재 단계는 검증이 끝난 렌더링 파이프라인에 **결과 전용 SQS 발행**을 추가한 구현입니다.

## 구현된 범위

- Spring의 `RenderQueueMessage` 스키마 1 파싱 및 검증
- S3 입력 JPEG 다운로드
- 기존 Java 필터 엔진과 지역별 오버레이 재사용
- 필터 JPEG 개별 업로드
- ZIP 및 1080x1920 H.264 MP4 생성·업로드
- `manifest.json` 생성·업로드
- `renderJobId` 기반 고정 경로와 manifest 멱등 처리
- SQS 부분 배치 실패 응답
- 렌더링 성공 시 결과 큐에 `CHAEROK_RENDER_COMPLETED` 발행
- 재시도 불가능한 실패 또는 마지막 재시도 실패 시 결과 큐에 `CHAEROK_RENDER_FAILED` 발행
- 중간 재시도 실패는 terminal FAILED 결과를 발행하지 않아 이후 성공 결과와 충돌하지 않음
- 결과 메시지에 요청 메시지 ID와 시도 횟수 포함
- `RENDER_RESULT_QUEUE_URL` 환경 변수 시작 시 검증
- 요청 큐 최대 수신 횟수는 `RENDER_REQUEST_MAX_RECEIVE_COUNT`로 설정하며 기본값은 현재 큐와 같은 3

## 아직 구현하지 않은 범위

- Spring 결과 큐 소비자
- `Photo`, `RenderJob`, `FilmRoll` 상태 반영
- 통합 `/develop` API
- FCM 완료 알림
- 요청 SQS → Lambda 트리거 연결

**요청 SQS 트리거는 아직 연결하지 않습니다.**

## 결과 S3 경로

```text
users/{userId}/rolls/{filmRollId}/render-jobs/{renderJobId}/
├─ filtered/001.jpg
├─ filtered/002.jpg
├─ export/chaerok_{regionId}_{filmRollId}_{job8}.zip
├─ export/chaerok_{regionId}_{filmRollId}_{job8}.mp4
└─ manifest.json
```

같은 `renderJobId`가 다시 전달되면 `manifest.json`을 읽어 기존 완료 결과를 재사용한 뒤, `COMPLETED` 결과 메시지를 다시 발행합니다. 따라서 결과 큐 소비자는 `renderJobId` 기준으로 멱등 처리해야 합니다.

## 결과 메시지 주요 필드

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
  "occurredAt": "2026-08-05T00:00:00Z",
  "errorCode": null,
  "errorMessage": null
}
```

실패 메시지는 `status=FAILED`이며 `errorCode`, `errorMessage`, `retryable=false`를 포함합니다. 검증 오류처럼 재시도 불가능한 실패는 결과 발행 성공 후 요청 메시지를 소비 완료 처리합니다. S3·FFmpeg 같은 실행 실패는 설정된 최대 수신 횟수 전까지 결과 큐에 terminal FAILED를 발행하지 않고 요청 큐 재시도만 수행합니다. 마지막 시도에도 실패하면 terminal FAILED를 발행하면서 요청 메시지는 실패로 반환해 요청 DLQ에도 원본을 남깁니다.

## 1. Gradle 테스트

프로젝트 루트 `D:\chaerok-be`에서 실행합니다.

```powershell
.\gradlew.bat -p lambda\render clean test --no-build-cache --rerun-tasks
```

## 2. 테스트 JPEG 업로드

```powershell
cd D:\chaerok-be\lambda\render
.\scripts\prepare-local-test.ps1 `
  -ImagePath "C:\Users\사용자명\Pictures\test.jpg"
```

테스트 입력 경로:

```text
s3://chaerok-media-dev-7f3k2m/local-tests/render-v2-stage1/input.jpg
```

## 3. Docker 이미지 빌드

```powershell
.\scripts\build-local.ps1
```

생성 이미지:

```text
chaerok-render-lambda:render-v2-result-v1
```

## 4. 로컬 Lambda Runtime 실행

`run-local.ps1`은 개발 결과 큐 URL과 현재 요청 큐의 `maxReceiveCount=3`을 기본값으로 전달합니다.

터미널 1:

```powershell
cd D:\chaerok-be\lambda\render
.\scripts\run-local.ps1
```

터미널 2:

```powershell
cd D:\chaerok-be\lambda\render
.\scripts\invoke-local.ps1
```

정상 응답:

```json
{
  "batchItemFailures": []
}
```

## 5. 결과 큐 메시지 확인

로컬 AWS 프로필에는 개발 결과 큐에 대한 아래 권한이 필요합니다.

```text
sqs:ReceiveMessage
sqs:DeleteMessage
sqs:ChangeMessageVisibility
sqs:GetQueueAttributes
```

```powershell
.\scripts\verify-result-queue.ps1
```

검증 스크립트는 확인한 테스트 메시지를 기본으로 삭제합니다. 메시지를 남겨야 하는 특별한 경우에만 아래처럼 실행합니다. 반복 조회는 수신 횟수를 증가시켜 결과 DLQ 이동을 유발할 수 있습니다.

```powershell
.\scripts\verify-result-queue.ps1 -KeepAfterRead
```

기본 확인 대상:

```text
Queue: chaerok-render-result-dev
renderJobId: 133d6ee3-a120-4df3-8ba3-f60adbdd64d6
```

## 6. ECR 푸시

로컬 테스트와 결과 큐 발행을 확인한 뒤 실행합니다.

```powershell
.\scripts\push-ecr.ps1
```

푸시 이미지:

```text
921972553505.dkr.ecr.ap-northeast-2.amazonaws.com/chaerok-render-lambda-dev:render-v2-result-v1
```

기존 `smoke-v1`, `render-v2-stage1` 태그는 덮어쓰지 않습니다.
