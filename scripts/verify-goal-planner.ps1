#requires -Version 5.1
param([string]$BaseUrl = 'http://localhost:8081', [ValidateRange(1, 100)][int]$MaximumSlices = 100)
$ErrorActionPreference = 'Stop'
# Own synthetic jobs only. Never resumes/cancels a user's existing search.
$provenance = Invoke-RestMethod "$BaseUrl/api/v1/crafting/basic-paths/provenance"
$headers = @{ 'X-Crafting-Ruleset' = $provenance.rulesetIdentity }
$initial = Invoke-RestMethod "$BaseUrl/api/v1/crafting/workbench/initial?base=solar&itemLevel=82"
if (!$initial.modifiers.'amulet:prefix:athlete-s') { throw 'Required real life modifier is absent.' }
$item = [ordered]@{
    snapshotId = $initial.state.snapshotId; baseItemId = $initial.state.baseItemId
    itemLevel = 82; rarity = 'MAGIC'; implicits = @($initial.state.implicits)
    explicits = @(@{ modifierId = 'amulet:prefix:athlete-s'; values = @{ base_maximum_life = 120 }; fractured = $false })
    conditions = @(); augmentSockets = $null; catalystQuality = $null
}
$query = '?snapshotId=' + [uri]::EscapeDataString($item.snapshotId) + '&baseItemId=' + [uri]::EscapeDataString($item.baseItemId) + '&itemLevel=82'
$catalog = Invoke-RestMethod "$BaseUrl/api/v1/crafting/support/goal-filters/catalog$query"
foreach ($count in @(2, 3)) {
    $stats = @('base_maximum_life', 'base_chaos_damage_resistance_%')
    if ($count -eq 3) { $stats += 'base_cold_damage_resistance_%' }
    $entries = @($stats | ForEach-Object {
        $sourceStat = $_
        $stat = @($catalog.stats | Where-Object { $_.kind -eq 'EXPLICIT' -and $_.sourceStatIds.Count -eq 1 -and $_.sourceStatIds[0] -eq $sourceStat })[0]
        if (!$stat) { throw "Real goal stat absent: $sourceStat" }
        @{ id = $sourceStat; statId = $stat.statId; unit = $stat.unit; range = @{ min = 1; max = $null }; weight = $null; disabled = $false }
    })
    $goal = @{
        version = 1; catalogVersion = $catalog.catalogVersion
        general = @{ baseItemId = $item.baseItemId; itemLevel = @{ min = $null; max = $null }; rarities = @('RARE') }
        groups = @(@{ id = 'targets'; type = 'AND'; disabled = $false; range = $null; entries = $entries })
    }
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $body = @{ abstractQuality40 = $false; request = @{ version = 2; clientRequestId = [guid]::NewGuid().ToString(); start = @{ item = $item; provenance = $provenance }; goal = $goal; activeOmens = @() } }
    $uri = "$BaseUrl/api/v2/crafting/method-searches/automatic"
    $view = Invoke-RestMethod $uri -Method Post -WebSession $session -Headers $headers -ContentType 'application/json' -Body ($body | ConvertTo-Json -Depth 30 -Compress)
    $slices = 0
    while ($view.status -eq 'READY' -and $slices -lt $MaximumSlices) {
        $command = @{ commandId = [guid]::NewGuid().ToString(); revision = $view.revision; generation = $view.generation; operation = 'ADVANCE' }
        $view = Invoke-RestMethod "$uri/$($view.id)/control" -Method Post -WebSession $session -Headers $headers -ContentType 'application/json' -Body ($command | ConvertTo-Json -Compress)
        $slices++
    }
    if ($view.status -eq 'READY') {
        $command = @{ commandId = [guid]::NewGuid().ToString(); revision = $view.revision; generation = $view.generation; operation = 'CANCEL' }
        $view = Invoke-RestMethod "$uri/$($view.id)/control" -Method Post -WebSession $session -Headers $headers -ContentType 'application/json' -Body ($command | ConvertTo-Json -Compress)
    }
    $hits = @($view.nodes | Where-Object { $_.progress.goal -eq 'MATCH' })
    [ordered]@{ goalCount = $count; id = $view.id; status = $view.status; reason = $view.reason; complete = $view.complete; slices = $slices; operations = $view.operations; nodes = $view.nodes.Count; methods = $view.methods.Count; goalNodes = $hits.Count; maximumDepth = ($view.nodes | Measure-Object depth -Maximum).Maximum; retainedStates = $view.retainedStates } | ConvertTo-Json -Compress
    if ($view.status -notin @('COMPLETE', 'PARTIAL') -or !$hits.Count) { throw "Case $count did not terminate with a goal. No existing user job was modified." }
    if ($count -eq 3 -and @($hits | Where-Object { $_.item.explicits.Count -lt 3 }).Count) { throw 'Three-goal search incorrectly completed before three explicits.' }
}
