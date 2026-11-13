# 运行 OpenAPI 快照导出测试，并把 admin 分组同步到 Admin 前端契约目录。
# 依赖：JDK 25（JAVA_HOME）、Maven 3.9+、Docker（Testcontainers）。

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$backendDir = Join-Path $repoRoot 'backend'
$snapshotSource = Join-Path $backendDir 'keel-bootstrap\target\openapi\admin.json'
$adminOpenApiDir = Join-Path $repoRoot 'frontend\admin\openapi'

Push-Location $backendDir
try {
    mvn -q -pl keel-bootstrap -am "-Dtest=OpenApiSnapshotTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
    if ($LASTEXITCODE -ne 0) {
        throw "OpenAPI 快照导出失败：mvn 退出码 $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

if (-not (Test-Path $snapshotSource)) {
    throw "未找到 OpenAPI 快照：$snapshotSource"
}

New-Item -ItemType Directory -Force -Path $adminOpenApiDir | Out-Null
Copy-Item -Path $snapshotSource -Destination (Join-Path $adminOpenApiDir 'openapi.json') -Force

Write-Host 'OpenAPI 快照已导出到 frontend/admin/openapi/openapi.json'
