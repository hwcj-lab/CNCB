$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'dev-env.ps1')
Use-ProjectJava
$maven=Get-ProjectMaven
$npm=Get-ProjectNpm
$repo=(Join-Path $ProjectRoot '.local/m2').Replace('\','/')
$env:npm_config_cache=Join-Path $ProjectRoot '.local/npm-cache'
Push-Location $ProjectRoot
try {
    & $maven -B -ntp -f portal-backend/pom.xml "-Dmaven.repo.local=$repo" clean verify
    if($LASTEXITCODE -ne 0){throw '后端验证失败'}
    Push-Location portal-frontend
    try {
        & $npm test
        if($LASTEXITCODE -ne 0){throw '前端测试失败'}
        & $npm run build
        if($LASTEXITCODE -ne 0){throw '前端构建失败'}
    } finally { Pop-Location }
} finally { Pop-Location }
