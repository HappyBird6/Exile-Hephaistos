param([ValidateSet('infra', 'backend', 'frontend', 'stack', 'stop')][string]$Target = 'infra')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$envFile = Join-Path $projectRoot '.env'
if (!(Test-Path -LiteralPath $envFile)) {
    $password = [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
    $template = [IO.File]::ReadAllText((Join-Path $projectRoot '.env.example'))
    [IO.File]::WriteAllText($envFile, $template.Replace('replace-with-local-password', $password))
    Write-Host 'Created local .env with a random development password.'
}
switch ($Target) {
    'infra' { docker compose --env-file .env -f infra/compose.yaml up -d --wait postgres redis }
    'stack' { docker compose --env-file .env -f infra/compose.yaml --profile stack up -d --build }
    'stop' { docker compose --env-file .env -f infra/compose.yaml --profile stack stop }
    'backend' {
        foreach ($line in [IO.File]::ReadAllLines($envFile)) {
            if ($line -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
                [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
            }
        }
        Set-Location backend
        & ./gradlew.bat bootRun
    }
    'frontend' {
        Set-Location frontend
        npm.cmd ci
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        npm.cmd run dev
    }
}
exit $LASTEXITCODE
