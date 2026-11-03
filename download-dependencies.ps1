param([string]$Destination = "$PSScriptRoot/deps")
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force $Destination | Out-Null
$artifacts = @(
    @('powernukkitx-3.0.6.jar', 'https://github.com/PowerNukkitX/PowerNukkitX/releases/download/3.0.6/powernukkitx.jar', '10822D818C1F2C7D423092A235F3F04BD0EB915B2298B204ED1697D7A0DD9539'),
    @('luckperms-api-5.4.jar', 'https://repo.maven.apache.org/maven2/net/luckperms/api/5.4/api-5.4.jar', '086E3971EA63C0B5AD567881B2DFD955ACBBFFA24FE85CEAC8ABF54B114CE986'),
    @('FrenchEconomy-1.0.jar', 'https://github.com/Psycofeu/FrenchEconomy/releases/download/Release/FrenchEconomy-1.0.jar', '60781A05FDA4A48FC1F746D948FBC4FE8977404D8B8A235D7A537EFAE68B7217'),
    @('ShopUI-2.0.1.jar', 'https://github.com/PowerNukkitX-Bundle/ShopUI/releases/download/v2.0.1/ShopUI.jar', '5030AC559CAB15CEE012B86A3DC049F046400CD4BA54D797B48D178154D059FF'),
    @('StatBoard-1.0.0.jar', 'https://github.com/DeepSlateDevStudio/statboard/releases/download/v1.0.0/StatBoard.jar', '903985CF043256499702245BB36754559FE3876244B0ADEF2A87C927E3F8D2D4')
)
foreach ($artifact in $artifacts) {
    $path = Join-Path $Destination $artifact[0]
    if (-not (Test-Path $path)) { Invoke-WebRequest -Uri $artifact[1] -OutFile $path }
    if ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $artifact[2]) {
        throw "Empreinte inattendue pour $($artifact[0]). Telechargement refuse pour compilation."
    }
    Write-Output "Verifie : $($artifact[0])"
}
