$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent (
    Split-Path -Parent $MyInvocation.MyCommand.Path
)

$eventPath = Join-Path `
    $projectRoot `
    "sample-event.json"

$body = Get-Content `
    -Path $eventPath `
    -Raw `
    -Encoding UTF8

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:9000/2015-03-31/functions/function/invocations" `
    -ContentType "application/json" `
    -Body $body |
ConvertTo-Json -Depth 10
