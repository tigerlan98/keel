# Full verification: backend build/tests, OpenAPI snapshot, frontend checks and E2E.
# Requirements: JDK 25 (JAVA_HOME), Maven 3.9+, Node 24, pnpm 10, Docker.

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $env:JAVA_HOME) {
    throw 'JAVA_HOME must point to JDK 25'
}

function Invoke-Maven {
    Push-Location (Join-Path $repoRoot 'backend')
    try {
        mvn verify
        if ($LASTEXITCODE -ne 0) {
            throw "mvn verify failed (exit $LASTEXITCODE)"
        }
    } finally {
        Pop-Location
    }
}

function Invoke-Admin {
    Push-Location (Join-Path $repoRoot 'frontend\admin')
    try {
        pnpm install --frozen-lockfile
        if ($LASTEXITCODE -ne 0) { throw "pnpm install failed (exit $LASTEXITCODE)" }
        pnpm lint
        if ($LASTEXITCODE -ne 0) { throw "pnpm lint failed (exit $LASTEXITCODE)" }
        pnpm typecheck
        if ($LASTEXITCODE -ne 0) { throw "pnpm typecheck failed (exit $LASTEXITCODE)" }
        pnpm build
        if ($LASTEXITCODE -ne 0) { throw "pnpm build failed (exit $LASTEXITCODE)" }
        pnpm test:e2e
        if ($LASTEXITCODE -ne 0) { throw "pnpm test:e2e failed (exit $LASTEXITCODE)" }
        pnpm openapi:check
        if ($LASTEXITCODE -ne 0) { throw "pnpm openapi:check failed (exit $LASTEXITCODE)" }
    } finally {
        Pop-Location
    }
}

function Invoke-Portal {
    Push-Location (Join-Path $repoRoot 'frontend\portal')
    try {
        pnpm install --frozen-lockfile
        if ($LASTEXITCODE -ne 0) { throw "pnpm install failed (exit $LASTEXITCODE)" }
        pnpm lint
        if ($LASTEXITCODE -ne 0) { throw "pnpm lint failed (exit $LASTEXITCODE)" }
        pnpm typecheck
        if ($LASTEXITCODE -ne 0) { throw "pnpm typecheck failed (exit $LASTEXITCODE)" }
        pnpm build
        if ($LASTEXITCODE -ne 0) { throw "pnpm build failed (exit $LASTEXITCODE)" }
    } finally {
        Pop-Location
    }
}

Write-Host '==> backend verification'
Invoke-Maven

Write-Host '==> export OpenAPI snapshot'
& (Join-Path $repoRoot 'scripts\openapi\export-openapi.ps1')

Write-Host '==> admin verification'
Invoke-Admin

Write-Host '==> portal verification'
Invoke-Portal

Write-Host 'All checks passed'
