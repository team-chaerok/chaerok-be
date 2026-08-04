# 채록 Render Lambda Smoke v1

이 단계는 실제 사진 처리나 FFmpeg 실행 전, 아래 기반만 검증합니다.

- Java 17 Lambda 컨테이너 이미지 빌드
- SQS 이벤트 JSON 파싱
- 메시지 스키마 1 검증
- 부분 배치 실패 응답
- 로컬 Lambda Runtime 호출
- Amazon ECR 이미지 푸시

SQS 트리거는 아직 연결하지 않습니다.

## 1. 프로젝트에 압축 해제

`D:\chaerok-be`에서 실행합니다.

```powershell
Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\chaerok-render-lambda-smoke-v1.zip" `
  -DestinationPath "D:\chaerok-be" `
  -Force
```

## 2. 이미지 빌드

```powershell
cd D:\chaerok-be\lambda\render
.\scripts\build-local.ps1
```

## 3. 로컬 실행

터미널 1:

```powershell
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

## 4. ECR 푸시

로컬 컨테이너를 `Ctrl+C`로 종료한 후:

```powershell
.\scripts\push-ecr.ps1
```

최종 이미지:

```text
921972553505.dkr.ecr.ap-northeast-2.amazonaws.com/chaerok-render-lambda-dev:smoke-v1
```
