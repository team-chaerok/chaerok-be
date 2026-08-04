# 채록 Render Lambda v2 — Stage 1

현재 단계는 실제 렌더링 파이프라인을 로컬 Docker와 Lambda 콘솔에서 수동 검증하기 위한 구현입니다.

## 구현된 범위

- Spring의 `RenderQueueMessage` 스키마 1 파싱 및 검증
- S3 입력 JPEG를 Lambda `/tmp`로 다운로드
- 기존 Java 필터 엔진과 지역별 오버레이 재사용
- 사진 순서대로 필터 JPEG 생성
- 필터 사진 개별 S3 업로드
- 필터 사진 ZIP 생성 및 S3 업로드
- FFmpeg 기반 1080x1920, H.264 MP4 생성 및 S3 업로드
- `manifest.json` 생성 및 S3 업로드
- `renderJobId` 기반 고정 결과 경로
- manifest를 성공 마커로 사용한 재시도 멱등 처리
- SQS 부분 배치 실패 응답 유지

## 아직 구현하지 않은 범위

- 결과 전용 SQS 큐 발행
- Spring의 `Photo`, `RenderJob`, `FilmRoll` 상태 반영
- 통합 `/develop` API
- FCM 완료 알림
- 실제 SQS 트리거 연결

**SQS 트리거는 아직 연결하지 않습니다.**

## 결과 S3 경로

```text
users/{userId}/rolls/{filmRollId}/render-jobs/{renderJobId}/
├─ filtered/001.jpg
├─ filtered/002.jpg
├─ export/chaerok_{regionId}_{filmRollId}_{job8}.zip
├─ export/chaerok_{regionId}_{filmRollId}_{job8}.mp4
└─ manifest.json
```

같은 `renderJobId`가 다시 전달되면 `manifest.json`을 확인해 이미 완료된 결과를 재사용합니다. manifest가 없는 부분 실패 상태라면 같은 경로에 결과를 다시 생성하고 덮어씁니다.

## 1. Gradle 테스트

프로젝트 루트 `D:\chaerok-be`에서 실행합니다.

```powershell
.\gradlew.bat -p lambda\render clean test
```

## 2. 테스트 JPEG를 S3에 업로드

```powershell
cd D:\chaerok-be\lambda\render
.\scripts\prepare-local-test.ps1 `
  -ImagePath "C:\Users\사용자명\Pictures\test.jpg"
```

테스트 파일은 아래 개발용 경로에 업로드됩니다.

```text
s3://chaerok-media-dev-7f3k2m/local-tests/render-v2-stage1/input.jpg
```

## 3. Docker 이미지 빌드

```powershell
.\scripts\build-local.ps1
```

생성 이미지:

```text
chaerok-render-lambda:render-v2-stage1
```

## 4. 로컬 Lambda Runtime 실행

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

정상 생성 경로:

```text
s3://chaerok-media-dev-7f3k2m/users/999001/rolls/999001/render-jobs/133d6ee3-a120-4df3-8ba3-f60adbdd64d6/
```

## 5. 결과 확인

```powershell
aws s3 ls `
  s3://chaerok-media-dev-7f3k2m/users/999001/rolls/999001/render-jobs/133d6ee3-a120-4df3-8ba3-f60adbdd64d6/ `
  --recursive `
  --profile chaerok-dev `
  --region ap-northeast-2
```

로컬 다운로드:

```powershell
aws s3 cp `
  s3://chaerok-media-dev-7f3k2m/users/999001/rolls/999001/render-jobs/133d6ee3-a120-4df3-8ba3-f60adbdd64d6/ `
  .\local-render-result\ `
  --recursive `
  --profile chaerok-dev `
  --region ap-northeast-2
```

## 6. ECR 푸시

로컬 결과물을 직접 확인한 뒤에만 실행합니다.

```powershell
.\scripts\push-ecr.ps1
```

푸시 이미지:

```text
921972553505.dkr.ecr.ap-northeast-2.amazonaws.com/chaerok-render-lambda-dev:render-v2-stage1
```

`smoke-v1` 태그는 덮어쓰지 않습니다.
