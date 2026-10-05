# Runs the JUnit tests of every module that has them (mirror of test.sh). Run compile.bat first.
Set-Location $PSScriptRoot
$status = 0
function Run-Tests($name, $deps) {
    if (-not (Test-Path "out\test\$name")) { return }
    if (-not (Get-ChildItem "out\test\$name" -Recurse -Filter *.class | Select-Object -First 1)) { return }
    $cp = "out\test\$name;out\main\$name"
    foreach ($d in $deps) { $cp += ";out\main\$d" }
    Write-Host "=== tests: $name"
    & java -jar lib\junit-platform-console-standalone-1.10.2.jar --class-path "$cp;lib\*" --scan-class-path "out\test\$name"
    if ($LASTEXITCODE -ne 0) { $script:status = 1 }
}
Run-Tests "core"     @()
Run-Tests "protocol" @()
Run-Tests "server"   @("core", "protocol")
Run-Tests "client"   @("protocol")
Run-Tests "bot"      @("protocol")
Run-Tests "integration" @("core", "protocol", "server", "bot")
exit $status
