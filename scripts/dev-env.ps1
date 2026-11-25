$ProjectRoot = Split-Path -Parent $PSScriptRoot
function Use-ProjectJava {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $candidates += @(Get-ChildItem -LiteralPath (Join-Path $ProjectRoot '.local/tools') -Directory -ErrorAction SilentlyContinue | Where-Object Name -Match 'jdk.*17|jdk-17' | ForEach-Object FullName)
    $candidates += @(Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue | ForEach-Object FullName)
    foreach ($candidate in $candidates) {
        $release = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath $release) -and (Test-Path -LiteralPath (Join-Path $candidate 'bin/java.exe'))) {
            $version = Get-Content -LiteralPath $release | Where-Object { $_ -match '^JAVA_VERSION=' } | Select-Object -First 1
            if ($version -match '"(\d+)' -and [int]$Matches[1] -ge 17) {
                $env:JAVA_HOME = $candidate
                $env:Path = (Join-Path $candidate 'bin') + ';' + $env:Path
                $tmp = Join-Path $ProjectRoot '.local/tmp'
                New-Item -ItemType Directory -Force -Path $tmp | Out-Null
                $env:MAVEN_USER_HOME = Join-Path $ProjectRoot '.local/maven-home'
                $env:MAVEN_OPTS = "-Dfile.encoding=UTF-8 -Djava.io.tmpdir=$($tmp.Replace('\','/'))"
                Write-Host "JAVA_HOME: $candidate"
                return
            }
        }
    }
    throw 'JDK 17 or later was not found. Configure JAVA_HOME or install JDK 17 in IDEA.'
}
function Get-ProjectMaven {
    $local = Join-Path $ProjectRoot '.local/tools/apache-maven-3.9.11/bin/mvn.cmd'
    if (Test-Path -LiteralPath $local) { return $local }
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    return Join-Path $ProjectRoot 'portal-backend/mvnw.cmd'
}
function Get-ProjectNpm {
    $command = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $installed = Join-Path $env:ProgramFiles 'nodejs/npm.cmd'
    if (Test-Path -LiteralPath $installed) { return $installed }
    throw 'npm.cmd was not found. Install a supported Node.js version and add npm.cmd to PATH.'
}
