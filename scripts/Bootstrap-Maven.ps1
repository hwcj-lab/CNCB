$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$tools=Join-Path $root '.local/tools'
$target=Join-Path $tools 'apache-maven-3.9.11/bin/mvn.cmd'
if(Test-Path -LiteralPath $target){Write-Host '项目内Maven已存在';return}
Push-Location $root
try {
    node scripts/bootstrap-maven.mjs
    if($LASTEXITCODE -ne 0){throw 'Maven引导下载/校验失败'}
    Expand-Archive -LiteralPath (Join-Path $tools 'maven.zip') -DestinationPath $tools -Force
    if(!(Test-Path -LiteralPath $target)){throw 'Maven解压失败'}
    Write-Host "Maven已就绪：$target"
} finally { Pop-Location }
