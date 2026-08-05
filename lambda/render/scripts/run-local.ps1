param(
    [string]$ResultQueueUrl =
        "https://sqs.ap-northeast-2.amazonaws.com/921972553505/chaerok-render-result-dev",
    [int]$RequestMaxReceiveCount = 3
)

$ErrorActionPreference = "Stop"

$existing = docker ps -aq `
    --filter "name=chaerok-render-lambda-local"

if ($existing) {
    docker rm -f chaerok-render-lambda-local |
        Out-Null
}

$awsDirectory = Join-Path $env:USERPROFILE ".aws"

if (-not (Test-Path $awsDirectory)) {
    throw "AWS CLI 자격 증명 폴더를 찾을 수 없습니다: $awsDirectory"
}

if ([string]::IsNullOrWhiteSpace($ResultQueueUrl)) {
    throw "ResultQueueUrl은 필수입니다."
}

if ($RequestMaxReceiveCount -lt 1) {
    throw "RequestMaxReceiveCount는 1 이상이어야 합니다."
}

docker run `
    --rm `
    --name chaerok-render-lambda-local `
    --platform linux/amd64 `
    --publish 9000:8080 `
    --env AWS_PROFILE=chaerok-dev `
    --env AWS_REGION=ap-northeast-2 `
    --env AWS_DEFAULT_REGION=ap-northeast-2 `
    --env "RENDER_RESULT_QUEUE_URL=$ResultQueueUrl" `
    --env "RENDER_REQUEST_MAX_RECEIVE_COUNT=$RequestMaxReceiveCount" `
    --volume "${awsDirectory}:/root/.aws:ro" `
    chaerok-render-lambda:render-v2-result-v1
