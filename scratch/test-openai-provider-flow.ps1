param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ModelName = "gpt-4.1-mini"
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [object]$Body
    )

    $JsonBody = $Body | ConvertTo-Json -Depth 20

    Invoke-RestMethod `
        -Uri $Uri `
        -Method Post `
        -ContentType "application/json" `
        -Body $JsonBody
}

$RunStamp = Get-Date -Format "yyyyMMdd-HHmmss"

Write-Host "Creating workflow..."
$Workflow = Invoke-JsonPost `
    -Uri "$BaseUrl/api/workflows" `
    -Body @{
    name = "OpenAI Provider Smoke Test $RunStamp"
    description = "Smoke test for OPENAI model execution provider"
}

$WorkflowId = $Workflow.id
Write-Host "Workflow ID: $WorkflowId"

Write-Host ""
Write-Host "Creating prompt version..."
$PromptVersion = Invoke-JsonPost `
    -Uri "$BaseUrl/api/workflows/$WorkflowId/prompt-versions" `
    -Body @{
    version = "openai-json-extraction-v1"
    promptText = @"
Extract the shipping status from the evaluation input.

Return valid JSON only with this exact shape:
{
  "status": "INSUFFICIENT_INFORMATION",
  "reason": "short reason"
}

Allowed status values:
- DELIVERED
- SHIPPED
- INSUFFICIENT_INFORMATION

If the input does not explicitly mention that the order was delivered or shipped, return INSUFFICIENT_INFORMATION.
"@
}

$PromptVersionId = $PromptVersion.id
Write-Host "Prompt Version ID: $PromptVersionId"

Write-Host ""
Write-Host "Creating evaluation case..."
$EvaluationCase = Invoke-JsonPost `
    -Uri "$BaseUrl/api/workflows/$WorkflowId/evaluation-cases" `
    -Body @{
    name = "Missing shipping status"
    taskType = "shipping_status_extraction"
    enabled = $true
    inputPayload = @{
        customerMessage = "Can you tell me where my order is?"
    }
    expectedOutput = @{
        status = "INSUFFICIENT_INFORMATION"
    }
    requiredFacts = @(
        "INSUFFICIENT_INFORMATION"
    )
    forbiddenClaims = @(
        "DELIVERED",
        "SHIPPED"
    )
    scoringRules = @{
        criticalExpectedFields = @("status")
        criticalRequiredFacts = @("INSUFFICIENT_INFORMATION")
        criticalForbiddenClaims = @("DELIVERED", "SHIPPED")
    }
}

$EvaluationCaseId = $EvaluationCase.id
Write-Host "Evaluation Case ID: $EvaluationCaseId"

Write-Host ""
Write-Host "Creating OpenAI evaluation run..."
$Run = Invoke-JsonPost `
    -Uri "$BaseUrl/api/workflows/$WorkflowId/evaluation-runs" `
    -Body @{
    promptVersionId = $PromptVersionId
    evaluationCaseId = $EvaluationCaseId
    modelName = $ModelName
    provider = "OPENAI"
    temperature = 0
    runConfig = @{
        numPredict = 128
    }
}

$RunId = $Run.id
Write-Host "Run ID: $RunId"
Write-Host "Initial Status: $($Run.status)"

Write-Host ""
Write-Host "Executing OpenAI evaluation run..."
$ExecutedRun = Invoke-RestMethod `
    -Uri "$BaseUrl/api/workflows/$WorkflowId/evaluation-runs/$RunId/execute" `
    -Method Post

Write-Host ""
Write-Host "Executed run summary:"
$ExecutedRun | Format-List

Write-Host ""
Write-Host "Raw output:"
$ExecutedRun.rawOutput

Write-Host ""
Write-Host "Parsed output:"
$ExecutedRun.parsedOutput | ConvertTo-Json -Depth 20

Write-Host ""
Write-Host "Failure reasons:"
$ExecutedRun.failureReasons

Write-Host ""
Write-Host "Done."