param(
    [string]$QueueUrl = "https://sqs.ap-northeast-2.amazonaws.com/921972553505/chaerok-render-result-dev",
    [string]$RenderJobId = "133d6ee3-a120-4df3-8ba3-f60adbdd64d6",
    [switch]$KeepAfterRead
)

$ErrorActionPreference = "Stop"

$receiveArguments = @(
    "sqs", "receive-message",
    "--queue-url", $QueueUrl,
    "--max-number-of-messages", "10",
    "--wait-time-seconds", "20",
    "--visibility-timeout", "30",
    "--attribute-names", "All",
    "--message-attribute-names", "All",
    "--region", "ap-northeast-2",
    "--profile", "chaerok-dev",
    "--output", "json"
)

$rawOutput = & aws @receiveArguments

if ($LASTEXITCODE -ne 0) {
    throw "Failed to receive messages from the render result queue."
}

$rawJson = $rawOutput -join [Environment]::NewLine
$response = $rawJson | ConvertFrom-Json

$messages = @()

if ($null -ne $response.Messages) {
    $messages = @($response.Messages)
}

if ($messages.Count -eq 0) {
    throw "No messages were found in the render result queue."
}

$matchedCount = 0

foreach ($message in $messages) {
    $body = $null

    try {
        $body = $message.Body | ConvertFrom-Json
    }
    catch {
        Write-Warning "Skipping a non-JSON message: $($message.MessageId)"
    }

    $isMatched = (
        $null -ne $body -and
        $body.renderJobId -eq $RenderJobId
    )

    if (-not $isMatched) {
        $visibilityArguments = @(
            "sqs", "change-message-visibility",
            "--queue-url", $QueueUrl,
            "--receipt-handle", $message.ReceiptHandle,
            "--visibility-timeout", "0",
            "--region", "ap-northeast-2",
            "--profile", "chaerok-dev"
        )

        & aws @visibilityArguments | Out-Null
        continue
    }

    $matchedCount++

    Write-Host ""
    Write-Host "Render result message found"
    Write-Host "messageId=$($message.MessageId)"
    Write-Host "eventType=$($body.eventType)"
    Write-Host "renderJobId=$($body.renderJobId)"
    Write-Host "filmRollId=$($body.filmRollId)"
    Write-Host "status=$($body.status)"
    Write-Host "attempt=$($body.attempt)"
    Write-Host "retryable=$($body.retryable)"
    Write-Host ""

    $body | ConvertTo-Json -Depth 20

    if ($KeepAfterRead) {
        Write-Warning "The message was kept in the queue."

        $visibilityArguments = @(
            "sqs", "change-message-visibility",
            "--queue-url", $QueueUrl,
            "--receipt-handle", $message.ReceiptHandle,
            "--visibility-timeout", "0",
            "--region", "ap-northeast-2",
            "--profile", "chaerok-dev"
        )

        & aws @visibilityArguments | Out-Null
        continue
    }

    $deleteArguments = @(
        "sqs", "delete-message",
        "--queue-url", $QueueUrl,
        "--receipt-handle", $message.ReceiptHandle,
        "--region", "ap-northeast-2",
        "--profile", "chaerok-dev"
    )

    & aws @deleteArguments | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to delete the verified result message."
    }

    Write-Host ""
    Write-Host "The verified result message was deleted from the queue."
}

if ($matchedCount -eq 0) {
    throw "No result message matched renderJobId=$RenderJobId."
}