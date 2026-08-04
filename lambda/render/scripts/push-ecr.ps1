$ErrorActionPreference = "Stop"

$registry = "921972553505.dkr.ecr.ap-northeast-2.amazonaws.com"
$repository = "$registry/chaerok-render-lambda-dev"
$tag = "render-v2-stage1"

aws ecr get-login-password `
    --region ap-northeast-2 `
    --profile chaerok-dev |
docker login `
    --username AWS `
    --password-stdin $registry

docker tag `
    chaerok-render-lambda:render-v2-stage1 `
    "${repository}:${tag}"

docker push "${repository}:${tag}"

Write-Host ""
Write-Host "Pushed image:"
Write-Host "${repository}:${tag}"
