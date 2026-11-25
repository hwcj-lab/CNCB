$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'dev-env.ps1')
$npm=Get-ProjectNpm
$env:npm_config_cache=Join-Path $ProjectRoot '.local/npm-cache'
Push-Location (Join-Path $ProjectRoot 'portal-frontend')
try {
    if (!(Test-Path -LiteralPath 'node_modules/vite/package.json')) { & $npm ci; if($LASTEXITCODE -ne 0){throw 'npm ci失败'} }
    & $npm run dev
    if($LASTEXITCODE -ne 0){throw '前端启动失败'}
} finally { Pop-Location }
