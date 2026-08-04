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

docker run `
    --rm `
    --name chaerok-render-lambda-local `
    --platform linux/amd64 `
    --publish 9000:8080 `
    --env AWS_PROFILE=chaerok-dev `
    --env AWS_REGION=ap-northeast-2 `
    --env AWS_DEFAULT_REGION=ap-northeast-2 `
    --volume "${awsDirectory}:/root/.aws:ro" `
    chaerok-render-lambda:render-v2-stage1
