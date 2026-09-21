#requires -Version 5.1
param([ValidateSet('infra', 'backend', 'frontend', 'stack', 'stop', 'status')][string]$Target = 'infra')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
$composeFile = Join-Path $projectRoot 'infra/compose.yaml'
$composeArgs = @('compose', '--project-name', 'exile-hephaistos', '--project-directory', $projectRoot,
    '--env-file', $envFile, '-f', $composeFile)
$exitCode = 0

function Invoke-Compose {
    param([string[]]$Arguments)
    & docker @composeArgs @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose failed (exit $LASTEXITCODE)." }
}

Push-Location $projectRoot
try {
    if (!(Test-Path -LiteralPath $envFile)) {
        if ($Target -in @('stop', 'status')) { throw 'No .env found. Run dev.ps1 infra or stack first.' }
        # Windows PowerShell 5.1 uses .NET Framework; use its instance RNG API.
        $bytes = New-Object byte[] 24
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $password = [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
        $template = [IO.File]::ReadAllText((Join-Path $projectRoot '.env.example'))
        [IO.File]::WriteAllText($envFile, $template.Replace('replace-with-local-password', $password),
            (New-Object System.Text.UTF8Encoding $false))
        Write-Host 'Created local .env with a random development password.'
    }

    if ($Target -in @('infra', 'stack')) {
        # Resolve dotenv interpolation through Compose without printing credentials.
        $json = & docker @composeArgs config --format json
        if ($LASTEXITCODE -ne 0) { throw 'Invalid Docker Compose configuration.' }
        $config = ($json -join "`n") | ConvertFrom-Json
        $volumeName = $config.volumes.'postgres-data'.name
        if ([string]::IsNullOrWhiteSpace($volumeName)) { throw 'PostgreSQL volume name is missing.' }
        # External volumes survive compose down --volumes. Only create on a fresh installation.
        $volumes = & docker volume ls --format '{{.Name}}'
        if ($LASTEXITCODE -ne 0) { throw 'Cannot list Docker volumes. Check Docker Desktop.' }
        if ($volumes -notcontains $volumeName) {
            & docker volume create $volumeName | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'Cannot create PostgreSQL volume.' }
        }
    }

    switch ($Target) {
        'infra' { Invoke-Compose -Arguments @('up', '-d', '--wait', 'postgres', 'redis') }
        'stack' { Invoke-Compose -Arguments @('--profile', 'stack', 'up', '-d', '--build', '--wait', '--wait-timeout', '120') }
        'stop' { Invoke-Compose -Arguments @('--profile', 'stack', 'stop') }
        'status' { Invoke-Compose -Arguments @('--profile', 'stack', 'ps', '-a') }
        'backend' {
            foreach ($line in [IO.File]::ReadAllLines($envFile)) {
                if ($line -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
                    [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
                }
            }
            Set-Location (Join-Path $projectRoot 'backend')
            & ./gradlew.bat bootRun
            $exitCode = $LASTEXITCODE
        }
        'frontend' {
            Set-Location (Join-Path $projectRoot 'frontend')
            & npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
            & npm.cmd run dev
            $exitCode = $LASTEXITCODE
        }
    }
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    $exitCode = 1
} finally {
    Pop-Location
}
exit $exitCode
