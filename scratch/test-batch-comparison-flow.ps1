param (
    [string] $BaseUrl = "http://localhost:8080",
    [string] $ModelName = "qwen3:4b",
    [int] $PollSeconds = 3,
    [int] $MaxPollAttempts = 120
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param (
        [Parameter(Mandatory = $true)]
        [string] $Url,

        [Parameter(Mandatory = $true)]
        [object] $Body
    )

    $Json = $Body | ConvertTo-Json -Depth 30

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

function Wait-ForEvaluationBatch {
    param (
        [Parameter(Mandatory = $true)]
        [long] $WorkflowId,

        [Parameter(Mandatory = $true)]
        [long] $BatchId,

        [Parameter(Mandatory = $true)]
        [string] $Label
    )

    $TerminalStatuses = @(
        "COMPLETED",
        "COMPLETED_WITH_FAILURES",
        "CANCELLED",
        "ERROR"
    )

    Write-Host ""
    Write-Host "Polling $Label batch status..."

    for ($Attempt = 1; $Attempt -le $MaxPollAttempts; $Attempt++) {
        Start-Sleep -Seconds $PollSeconds

        $BatchStatus = Invoke-JsonGet `
            -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$BatchId"

        Write-Host ("{0}: Status={1}, Completed={2}/{3}, Passed={4}, Failed={5}, Errored={6}, AvgScore={7}" -f `
            $Label,
            $BatchStatus.status,
            $BatchStatus.completedRuns,
            $BatchStatus.totalRuns,
            $BatchStatus.passedRuns,
            $BatchStatus.failedRuns,
            $BatchStatus.erroredRuns,
            $BatchStatus.averageScore
        )

        if ($TerminalStatuses -contains $BatchStatus.status) {
            return $BatchStatus
        }
    }

    throw "Timed out waiting for $Label batch $BatchId after $MaxPollAttempts polling attempts."
}

function Show-BatchRuns {
    param (
        [Parameter(Mandatory = $true)]
        [long] $WorkflowId,

        [Parameter(Mandatory = $true)]
        [long] $BatchId,

        [Parameter(Mandatory = $true)]
        [string] $Label
    )

    Write-Host ""
    Write-Host "$Label batch runs:"

    $Runs = Invoke-JsonGet `
        -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/$BatchId/runs"

    $Runs | Select-Object `
        id,
        evaluationCaseId,
        modelName,
        status,
        passed,
        score,
        failureReasons |
        Format-Table -AutoSize
}

Write-Host ""
Write-Host "Creating workflow..."

$RunSuffix = Get-Date -Format "yyyyMMdd-HHmmss"

$Workflow = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows" `
    -Body @{
        name = "Batch Comparison Smoke Test $RunSuffix"
        description = "Creates baseline and candidate evaluation batches, then compares them."
    }

$WorkflowId = $Workflow.id
Write-Host "Workflow ID: $WorkflowId"

Write-Host ""
Write-Host "Creating baseline prompt version..."

$BaselinePromptVersion = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/prompt-versions" `
    -Body @{
        version = "baseline-v1"
        promptText = "Extract the shipping status from the customer message. Return JSON only with fields: status and reason. If the message does not specify a shipping status, return status as INSUFFICIENT_INFORMATION. In the reason, explicitly say Shipping status is not specified when the status is unknown."
        changeNotes = "Baseline prompt for batch comparison smoke test."
    }

$BaselinePromptVersionId = $BaselinePromptVersion.id
Write-Host "Baseline Prompt Version ID: $BaselinePromptVersionId"

Write-Host ""
Write-Host "Creating candidate prompt version..."

$CandidatePromptVersion = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/prompt-versions" `
    -Body @{
        version = "candidate-v2"
        promptText = "You are evaluating shipping support messages. Return valid JSON only with fields: status and reason. Allowed status values are DELIVERED, SHIPPED, or INSUFFICIENT_INFORMATION. Do not infer shipping status. If status is missing or vague, use INSUFFICIENT_INFORMATION and include the exact phrase Shipping status is not specified in the reason."
        changeNotes = "Candidate prompt with stricter unsupported-claim handling."
    }

$CandidatePromptVersionId = $CandidatePromptVersion.id
Write-Host "Candidate Prompt Version ID: $CandidatePromptVersionId"

Write-Host ""
Write-Host "Creating evaluation cases..."

$Case1 = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-cases" `
    -Body @{
        name = "Missing shipping status"
        taskType = "EXTRACTION"
        inputPayload = @{
            message = "The customer asked about shipping status."
        }
        expectedOutput = @{
            status = "INSUFFICIENT_INFORMATION"
        }
        requiredFacts = @(
            "Shipping status is not specified"
        )
        forbiddenClaims = @(
            "shipped",
            "delivered"
        )
        scoringRules = @{
            criticalExpectedFields = @("status")
            criticalRequiredFacts = @("Shipping status is not specified")
            criticalForbiddenClaims = @("shipped", "delivered")
        }
    }

$Case2 = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-cases" `
    -Body @{
        name = "Delivered status explicitly mentioned"
        taskType = "EXTRACTION"
        inputPayload = @{
            message = "My order was delivered yesterday."
        }
        expectedOutput = @{
            status = "DELIVERED"
        }
        requiredFacts = @(
            "order was delivered"
        )
        forbiddenClaims = @(
            "insufficient information"
        )
        scoringRules = @{
            criticalExpectedFields = @("status")
            criticalRequiredFacts = @("order was delivered")
        }
    }

$Case3 = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-cases" `
    -Body @{
        name = "Shipped status explicitly mentioned"
        taskType = "EXTRACTION"
        inputPayload = @{
            message = "The support note says the package has shipped."
        }
        expectedOutput = @{
            status = "SHIPPED"
        }
        requiredFacts = @(
            "package has shipped"
        )
        forbiddenClaims = @(
            "delivered"
        )
        scoringRules = @{
            criticalExpectedFields = @("status")
            criticalRequiredFacts = @("package has shipped")
            criticalForbiddenClaims = @("delivered")
        }
    }

$Case4 = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-cases" `
    -Body @{
        name = "Vague customer question"
        taskType = "EXTRACTION"
        inputPayload = @{
            message = "Can you check my order?"
        }
        expectedOutput = @{
            status = "INSUFFICIENT_INFORMATION"
        }
        requiredFacts = @(
            "Shipping status is not specified"
        )
        forbiddenClaims = @(
            "shipped",
            "delivered"
        )
        scoringRules = @{
            criticalExpectedFields = @("status")
            criticalRequiredFacts = @("Shipping status is not specified")
            criticalForbiddenClaims = @("shipped", "delivered")
        }
    }

Write-Host "Created cases:"
Write-Host "- $($Case1.id): $($Case1.name)"
Write-Host "- $($Case2.id): $($Case2.name)"
Write-Host "- $($Case3.id): $($Case3.name)"
Write-Host "- $($Case4.id): $($Case4.name)"

Write-Host ""
Write-Host "Creating baseline evaluation batch..."

$BaselineBatch = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches" `
    -Body @{
        promptVersionId = $BaselinePromptVersionId
        modelName = $ModelName
        provider = "OLLAMA"
        temperature = 0.0
        runConfig = @{
            numPredict = 128
            contextWindow = 4096
        }
    }

$BaselineBatchId = $BaselineBatch.id
Write-Host "Baseline Batch ID: $BaselineBatchId"
Write-Host "Baseline Initial Status: $($BaselineBatch.status)"

$BaselineFinal = Wait-ForEvaluationBatch `
    -WorkflowId $WorkflowId `
    -BatchId $BaselineBatchId `
    -Label "Baseline"

Write-Host ""
Write-Host "Baseline final batch:"
$BaselineFinal | Format-List

Show-BatchRuns `
    -WorkflowId $WorkflowId `
    -BatchId $BaselineBatchId `
    -Label "Baseline"

Write-Host ""
Write-Host "Creating candidate evaluation batch..."

$CandidateBatch = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches" `
    -Body @{
        promptVersionId = $CandidatePromptVersionId
        modelName = $ModelName
        provider = "OLLAMA"
        temperature = 0.0
        runConfig = @{
            numPredict = 128
            contextWindow = 4096
        }
    }

$CandidateBatchId = $CandidateBatch.id
Write-Host "Candidate Batch ID: $CandidateBatchId"
Write-Host "Candidate Initial Status: $($CandidateBatch.status)"

$CandidateFinal = Wait-ForEvaluationBatch `
    -WorkflowId $WorkflowId `
    -BatchId $CandidateBatchId `
    -Label "Candidate"

Write-Host ""
Write-Host "Candidate final batch:"
$CandidateFinal | Format-List

Show-BatchRuns `
    -WorkflowId $WorkflowId `
    -BatchId $CandidateBatchId `
    -Label "Candidate"

Write-Host ""
Write-Host "Comparing baseline and candidate batches..."

$Comparison = Invoke-JsonGet `
    -Url "$BaseUrl/api/workflows/$WorkflowId/evaluation-batches/compare?baselineBatchId=$BaselineBatchId&candidateBatchId=$CandidateBatchId"

Write-Host ""
Write-Host "Batch comparison summary:"
$Comparison | Select-Object `
    workflowId,
    baselineBatchId,
    candidateBatchId,
    baselineStatus,
    candidateStatus,
    baselineAverageScore,
    candidateAverageScore,
    scoreDelta,
    outcome,
    comparisonReasons |
    Format-List

Write-Host ""
Write-Host "Per-case comparison:"
$Comparison.caseComparisons | Select-Object `
    evaluationCaseId,
    baselineRunId,
    candidateRunId,
    baselineStatus,
    candidateStatus,
    baselinePassed,
    candidatePassed,
    baselineScore,
    candidateScore,
    scoreDelta,
    outcome,
    regressionReasons |
    Format-Table -AutoSize

Write-Host ""
Write-Host "Raw comparison response:"
$Comparison | ConvertTo-Json -Depth 30

Write-Host ""
Write-Host "Done."
