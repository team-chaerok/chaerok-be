$ErrorActionPreference = "Stop"

$registry = "921972553505.dkr.ecr.ap-northeast-2.amazonaws.com"
$repository = "$registry/chaerok-render-lambda-dev"
$tag = "smoke-v1"

docker tag `
    chaerok-render-lambda:smoke-v1 `
    "${repository}:${tag}"

docker push "${repository}:${tag}"

Write-Host ""
Write-Host "Pushed image:"
Write-Host "${repository}:${tag}"
