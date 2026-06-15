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
Write-Host "Creating slow running batch..."

$RunningBatch = Invoke-JsonPost `
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

$RunningBatchId = $RunningBatch.id
Write-Host "Running batch candidate ID: $RunningBatchId"

Start-Sleep -Seconds 1

Write-Host ""
Write-Host "Creating second queued batch..."

$QueuedBatch = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches" `
    -Body @{
    promptVersionId = $PromptVersionId
    modelName = "qwen3:4b"
    provider = "OLLAMA"
    temperature = 0.0
    runConfig = @{
        numPredict = 128
        contextWindow = 4096
    }
}

$QueuedBatchId = $QueuedBatch.id
Write-Host "Queued batch ID: $QueuedBatchId"
Write-Host "Queued batch initial status: $($QueuedBatch.status)"

Write-Host ""
Write-Host "Cancelling queued batch..."

$CancelledBatch = Invoke-RestMethod `
    -Uri "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$QueuedBatchId/cancel" `
    -Method Post

Write-Host "Cancelled batch status: $($CancelledBatch.status)"

Write-Host ""
Write-Host "Current running batch:"
Invoke-JsonGet `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$RunningBatchId" |
        Format-List

Write-Host ""
Write-Host "Cancelled queued batch:"
Invoke-JsonGet `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$QueuedBatchId" |
        Format-List