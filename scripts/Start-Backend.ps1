param([ValidateSet('demo','mysql')][string]$Profile='mysql')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'dev-env.ps1')
Use-ProjectJava
$maven = Get-ProjectMaven
$repo = (Join-Path $ProjectRoot '.local/m2').Replace('\','/')
$env:SPRING_PROFILES_ACTIVE=$Profile
Push-Location (Join-Path $ProjectRoot 'portal-backend')
try {
    & $maven -B -ntp "-Dmaven.repo.local=$repo" '-DskipTests' spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw '后端启动失败；检查日志。wrapper下载失败可先执行scripts/Bootstrap-Maven.ps1。' }
} finally { Pop-Location }
