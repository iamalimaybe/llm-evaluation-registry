$ErrorActionPreference = "Stop"

$BaseUrl = "http://localhost:8080"

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
Write-Host "Creating workflow..."

$RunSuffix = Get-Date -Format "yyyyMMdd-HHmmss"

$Workflow = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows" `
    -Body @{
    name = "Shipping Support Evaluation $RunSuffix"
    description = "Evaluates whether models avoid unsupported shipping status claims."
}

$WorkflowId = $Workflow.id
Write-Host "Workflow ID: $WorkflowId"

Write-Host ""
Write-Host "Creating prompt version..."

$PromptVersion = Invoke-JsonPost `
    -Url "$BaseUrl/api/workflows/$WorkflowId/prompt-versions" `
    -Body @{
        version = "v1"
        promptText = "Extract the shipping status from the customer message. Return JSON only with fields: status and reason. If the message does not specify a shipping status, return status as INSUFFICIENT_INFORMATION."
        changeNotes = "Initial shipping status extraction prompt."
    }

$PromptVersionId = $PromptVersion.id
Write-Host "Prompt Version ID: $PromptVersionId"

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
            criticalForbiddenClaims = @("shipped", "delivered")
        }
    }

Write-Host "Created cases:"
Write-Host "- $($Case1.id): $($Case1.name)"
Write-Host "- $($Case2.id): $($Case2.name)"
Write-Host "- $($Case3.id): $($Case3.name)"
Write-Host "- $($Case4.id): $($Case4.name)"

Write-Host ""
Write-Host "Creating evaluation batch..."

$Batch = Invoke-JsonPost `
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

$BatchId = $Batch.id
Write-Host "Batch ID: $BatchId"
Write-Host "Initial status: $($Batch.status)"

Write-Host ""
Write-Host "Polling batch status..."

$TerminalStatuses = @(
    "COMPLETED",
    "COMPLETED_WITH_FAILURES",
    "CANCELLED",
    "ERROR"
)

do {
    Start-Sleep -Seconds 3

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
    modelName,
    status,
    passed,
    score,
    failureReasons |
    Format-Table -AutoSize

Write-Host ""
Write-Host "Done."