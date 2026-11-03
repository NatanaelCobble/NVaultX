param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$DependencyDirectory = "$PSScriptRoot/deps"
)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { throw 'Indiquer -JavaHome avec un JDK 25 (requis pour lire les classes du ShopUI publie).' }
$javac = Join-Path $JavaHome 'bin/javac.exe'
$java = Join-Path $JavaHome 'bin/java.exe'
$jar = Join-Path $JavaHome 'bin/jar.exe'
$dependencies = @('powernukkitx-3.0.6.jar', 'luckperms-api-5.4.jar', 'FrenchEconomy-1.0.jar', 'ShopUI-2.0.1.jar', 'StatBoard-1.0.0.jar')
foreach ($dependency in $dependencies) {
    if (-not (Test-Path (Join-Path $DependencyDirectory $dependency))) { throw "Dependance manquante : $dependency (voir README.txt)" }
}
$classpath = ($dependencies | ForEach-Object { Join-Path $DependencyDirectory $_ }) -join ';'
$build = Join-Path $PSScriptRoot 'build'
$classes = Join-Path $build 'classes'
$tests = Join-Path $build 'tests'
New-Item -ItemType Directory -Force $classes, $tests | Out-Null
$sources = @(Get-ChildItem "$PSScriptRoot/src/main/java" -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& $javac --release 21 -encoding UTF-8 -proc:none -classpath $classpath -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation echouee' }
$testSources = @(Get-ChildItem "$PSScriptRoot/src/test/java" -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& $javac --release 21 -encoding UTF-8 -proc:none -classpath "$classes;$classpath" -d $tests @testSources
if ($LASTEXITCODE -ne 0) { throw 'Compilation des tests echouee' }
& $java -classpath "$tests;$classes;$classpath" org.nvaultx.TestSuite
if ($LASTEXITCODE -ne 0) { throw 'Tests echoues' }
& $java -classpath "$tests;$classpath" org.nvaultx.ManifestTest "$PSScriptRoot/src/main/resources/plugin.yml"
if ($LASTEXITCODE -ne 0) { throw 'Verification manifeste echouee' }
Copy-Item "$PSScriptRoot/src/main/resources/plugin.yml" $classes
New-Item -ItemType Directory -Force "$classes/META-INF" | Out-Null
Copy-Item "$PSScriptRoot/LICENSE" "$classes/META-INF/LICENSE"
& $jar "-J-Djava.io.tmpdir=$build" --create --file "$PSScriptRoot/NVaultX-1.0.0.jar" -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Creation du plugin echouee' }
& $jar "-J-Djava.io.tmpdir=$build" --create --file "$PSScriptRoot/NVaultX-API-1.0.0.jar" -C $classes org/nvaultx/api -C $classes META-INF/LICENSE
if ($LASTEXITCODE -ne 0) { throw 'Creation API echouee' }
Write-Output 'Compilation et tests reussis. Plugin et API generes.'
