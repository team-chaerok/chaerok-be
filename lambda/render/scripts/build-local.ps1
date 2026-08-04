$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent (
    Split-Path -Parent $MyInvocation.MyCommand.Path
)

Push-Location $projectRoot

try {
    docker buildx build `
        --platform linux/amd64 `
        --provenance=false `
        --load `
        --tag chaerok-render-lambda:smoke-v1 `
        .
} finally {
    Pop-Location
}
