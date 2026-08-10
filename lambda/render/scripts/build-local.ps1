$ErrorActionPreference = "Stop"

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = Resolve-Path (
    Join-Path $scriptDirectory "..\..\.."
)

Push-Location $repositoryRoot

try {
    docker buildx build `
        --platform linux/amd64 `
        --provenance=false `
        --load `
        --file lambda/render/Dockerfile `
        --tag chaerok-render-lambda:render-v2-result-v1 `
        .
} finally {
    Pop-Location
}
