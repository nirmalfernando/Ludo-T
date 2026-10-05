# Builds every module in dependency order (mirror of compile.sh).
# Each module sees only its declared dependencies, so the compiler enforces the architecture.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$release = "17"
$jfxLib  = "lib\javafx-sdk\lib"
New-Item -ItemType Directory -Force -Path out\main, out\test, dist | Out-Null

function Get-Sources($dir) {
    if (-not (Test-Path $dir)) { return @() }
    return @(Get-ChildItem $dir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
}

function Build-Module($name, $main, $deps) {
    $sources = Get-Sources "src\$name"
    if ($sources.Count -eq 0) { Write-Host "  [$name] no sources yet, skipped"; return }

    $cp = ($deps | ForEach-Object { "out\main\$_" }) -join ";"
    $fx = @()
    if ($name -eq "client") { $fx = @("--module-path", $jfxLib, "--add-modules", "javafx.controls,javafx.graphics") }

    Remove-Item -Recurse -Force "out\main\$name", "out\test\$name" -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path "out\main\$name", "out\test\$name" | Out-Null

    $args1 = @("--release", $release, "-encoding", "UTF-8", "-d", "out\main\$name") + $fx
    if ($cp) { $args1 += @("-cp", $cp) }
    & javac @args1 @sources
    if ($LASTEXITCODE -ne 0) { throw "[$name] compile failed" }

    if ($main -eq "-") { & jar --create --file "dist\$name.jar" -C "out\main\$name" . }
    else { & jar --create --file "dist\$name.jar" --main-class $main -C "out\main\$name" . }

    $tests = Get-Sources "test\$name"
    if ($tests.Count -gt 0) {
        $tcp = "out\main\$name"
        if ($cp) { $tcp = "$tcp;$cp" }
        & javac --release $release -encoding UTF-8 -d "out\test\$name" @fx -cp "$tcp;lib\*" @tests
        if ($LASTEXITCODE -ne 0) { throw "[$name] test compile failed" }
    }
    Write-Host "  [$name] built -> dist\$name.jar"
}

function Build-TestModule($name, $deps) {
    $tests = Get-Sources "test\$name"
    if ($tests.Count -eq 0) { return }
    $cp = ($deps | ForEach-Object { "out\main\$_" }) -join ";"
    Remove-Item -Recurse -Force "out\test\$name" -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path "out\test\$name" | Out-Null
    & javac --release $release -encoding UTF-8 -d "out\test\$name" -cp "$cp;lib\*" @tests
    if ($LASTEXITCODE -ne 0) { throw "[$name] test compile failed" }
    Write-Host "  [$name] cross-module tests built"
}

Write-Host "Building modules..."
Build-Module "core"     "com.ludot.Main"              @()
Build-Module "protocol" "-"                           @()
Build-Module "server"   "com.ludot.server.ServerMain" @("core", "protocol")
Build-Module "client"   "com.ludot.client.ClientApp"  @("protocol")
Build-Module "bot"      "com.ludot.bot.BotMain"       @("protocol")
Build-TestModule "integration" @("core", "protocol", "server", "bot")
Write-Host "Build complete."
