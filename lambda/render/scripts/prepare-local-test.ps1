param(
    [Parameter(Mandatory = $true)]
    [string]$ImagePath
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ImagePath -PathType Leaf)) {
    throw "테스트 이미지를 찾을 수 없습니다: $ImagePath"
}

$bucket = "chaerok-media-dev-7f3k2m"
$objectKey = "local-tests/render-v2-stage1/input.jpg"

aws s3 cp `
    $ImagePath `
    "s3://$bucket/$objectKey" `
    --content-type "image/jpeg" `
    --profile chaerok-dev `
    --region ap-northeast-2

aws s3api head-object `
    --bucket $bucket `
    --key $objectKey `
    --profile chaerok-dev `
    --region ap-northeast-2

Write-Host ""
Write-Host "Lambda v2 로컬 테스트 입력 업로드 완료:"
Write-Host "s3://$bucket/$objectKey"
