param()

$base = $PSScriptRoot

New-Item -ItemType Directory -Force -Path "$base\out\production" | Out-Null
New-Item -ItemType Directory -Force -Path "$base\out\test"       | Out-Null

# --- Production ---
$srcFiles = Get-ChildItem "$base\src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
if ($srcFiles.Count -eq 0) { Write-Error "No source files found in src/"; exit 1 }

[IO.File]::WriteAllLines("$base\sources.tmp", $srcFiles)
& javac -d "$base\out\production" "@$base\sources.tmp" 2>&1 | Write-Host
Remove-Item "$base\sources.tmp" -Force
if ($LASTEXITCODE -ne 0) { Write-Error "[ERROR] Production compile failed."; exit 1 }

# --- Tests (skip if no test files yet) ---
$testFiles = Get-ChildItem "$base\test" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
if ($testFiles.Count -gt 0) {
    [IO.File]::WriteAllLines("$base\test-sources.tmp", $testFiles)
    & javac -d "$base\out\test" -cp "$base\out\production;$base\lib\*" "@$base\test-sources.tmp" 2>&1 | Write-Host
    Remove-Item "$base\test-sources.tmp" -Force
    if ($LASTEXITCODE -ne 0) { Write-Error "[ERROR] Test compile failed."; exit 1 }
} else {
    Write-Host "No test files found - skipping test compile."
}

Write-Host "Build complete. $($srcFiles.Count) source files compiled."
