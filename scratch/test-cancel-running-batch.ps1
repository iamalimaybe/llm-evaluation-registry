$ErrorActionPreference = "Stop"

$BaseUrl = "http://localhost:8080"
$WorkflowId = 5
$PromptVersionId = 5

function Invoke-JsonPost {
    param (
        [Parameter(Mandatory = $true)]
        [string] $Url,

        [Parameter(Mandatory = $true)]
        [object] $Body
    )

    $Json = $Body | ConvertTo-Json -Depth 20

    Invoke-RestMethod `
        -Uri $Url `
        -Method Post `
        -ContentType "application/json" `
        -Body $Json
}

function Invoke-JsonGet {
    param (
        [Parameter(Mandatory = $true)]
        [string] $Url
    )

    Invoke-RestMethod `
        -Uri $Url `
        -Method Get
}

Write-Host ""
Write-Host "Creating running batch..."

$Batch = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches" `
    -Body @{
    promptVersionId = $PromptVersionId
    modelName = "qwen3:4b"
    provider = "OLLAMA"
    temperature = 0.0
    runConfig = @{
        numPredict = 512
        contextWindow = 4096
    }
}

$BatchId = $Batch.id
Write-Host "Batch ID: $BatchId"
Write-Host "Initial status: $($Batch.status)"

Write-Host ""
Write-Host "Waiting for batch to start..."

do {
    Start-Sleep -Seconds 1

    $BatchStatus = Invoke-JsonGet `
        -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$BatchId"

    Write-Host ("Status={0}, Completed={1}/{2}" -f `
        $BatchStatus.status,
    $BatchStatus.completedRuns,
    $BatchStatus.totalRuns
    )

} while ($BatchStatus.status -eq "QUEUED")

Write-Host ""
Write-Host "Requesting cancellation..."

$CancelResponse = Invoke-RestMethod `
    -Uri "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$BatchId/cancel" `
    -Method Post

Write-Host "Cancel response status: $($CancelResponse.status)"

Write-Host ""
Write-Host "Polling until terminal status..."

$TerminalStatuses = @(
    "COMPLETED",
    "COMPLETED_WITH_FAILURES",
    "CANCELLED",
    "ERROR"
)

do {
    Start-Sleep -Seconds 2

    $BatchStatus = Invoke-JsonGet `
        -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$BatchId"

    Write-Host ("Status={0}, Completed={1}/{2}, Passed={3}, Failed={4}, Errored={5}, AvgScore={6}" -f `
        $BatchStatus.status,
    $BatchStatus.completedRuns,
    $BatchStatus.totalRuns,
    $BatchStatus.passedRuns,
    $BatchStatus.failedRuns,
    $BatchStatus.erroredRuns,
    $BatchStatus.averageScore
    )

} while ($TerminalStatuses -notcontains $BatchStatus.status)

Write-Host ""
Write-Host "Final batch:"
$BatchStatus | Format-List

Write-Host ""
Write-Host "Batch runs:"

$BatchRuns = Invoke-JsonGet `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$BatchId/runs"

$BatchRuns | Select-Object `
    id,
evaluationCaseId,
status,
passed,
score,
startedAt,
completedAt,
failureReasons |
        Format-Table -AutoSize