# ============================================================
# GEEKS - gera o instalador do Windows (SistemaGeeks-<versão>.exe)
# ============================================================
# Resultado: target\instalador\Sistema Geeks-<versão>.exe
# O instalador já leva o Java dentro (o computador da loja não precisa ter Java)
# e cria dois atalhos:
#   "Sistema Geeks"            abre o sistema (área de trabalho e menu Iniciar)
#   "Configurar Sistema Geeks" assistente de primeira configuração (menu Iniciar)
#
# Precisa (só no computador de quem GERA o instalador):
#   - JDK 17+ com jpackage (ex.: Eclipse Temurin)
#   - WiX Toolset 3.14 (candle.exe/light.exe) -> -Wix "C:\caminho\wix314"
#   - Maven (para gerar o sistema-geeks.jar)
#
# Uso: powershell -ExecutionPolicy Bypass -File instalador\gerar_instalador.ps1 [-Jdk C:\...\jdk] [-Wix C:\...\wix314] [-SemBuild]
# ============================================================
param(
    [string]$Jdk = $env:JAVA_HOME,
    [string]$Wix = (Join-Path $env:USERPROFILE 'tools\wix314'),
    [string]$Mvn = '',
    [string]$Versao = '1.0.4',
    [switch]$SemBuild
)
$ErrorActionPreference = 'Stop'
$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $raiz

if (-not $Jdk -or -not (Test-Path (Join-Path $Jdk 'bin\jpackage.exe'))) { throw "jpackage não encontrado. Informe -Jdk com a pasta de um JDK 17+." }
if (-not (Test-Path (Join-Path $Wix 'candle.exe'))) { throw "WiX 3.14 não encontrado em $Wix. Informe -Wix." }
$env:JAVA_HOME = $Jdk
$env:PATH = "$Wix;$Jdk\bin;$env:PATH"

# 1. build
if (-not $SemBuild) {
    if (-not $Mvn) { $c = Get-Command mvn.cmd -ErrorAction SilentlyContinue; $Mvn = if ($c) { $c.Source } else { Join-Path $env:USERPROFILE 'tools\apache-maven-3.9.9\bin\mvn.cmd' } }
    & $Mvn -B -q package
    if ($LASTEXITCODE -ne 0) { throw 'mvn package falhou' }
}
$jar = Join-Path $raiz 'target\sistema-geeks.jar'
if (-not (Test-Path $jar)) { throw "Falta $jar (rode sem -SemBuild)" }

# 2. pasta de entrada: só o que o programa precisa. NUNCA arquivos com senha.
$trabalho = Join-Path $raiz 'target\instalador-trabalho'
$entrada = Join-Path $trabalho 'entrada'
$saida = Join-Path $raiz 'target\instalador'
Remove-Item $trabalho, $saida -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$entrada\db\scripts", "$entrada\db\backup", $saida | Out-Null
Copy-Item $jar $entrada
Copy-Item 'db\scripts\01_criar_bancos_e_usuarios.sql' "$entrada\db\scripts\"
Copy-Item 'db\backup\*.ps1' "$entrada\db\backup\"
$proibidos = Get-ChildItem $entrada -Recurse -File | Where-Object { $_.Name -match '\.properties$|backup\.cnf|senha-backup' }
if ($proibidos) { throw "Arquivo com senha na pasta do instalador: $($proibidos.FullName -join ', ')" }

# 3. ícone (.ico com PNG 256x256 dentro, aceito pelo Windows Vista em diante)
Add-Type -AssemblyName System.Drawing
$png = Join-Path $raiz 'src\main\resources\img\g_geeks_icone.png'
$ico = Join-Path $trabalho 'geeks.ico'
$orig = [Drawing.Image]::FromFile($png)
$bmp = New-Object Drawing.Bitmap 256, 256
$g = [Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = 'HighQualityBicubic'
$lado = [Math]::Min(256 / $orig.Width, 256 / $orig.Height)
$w = [int]($orig.Width * $lado); $h = [int]($orig.Height * $lado)
$g.DrawImage($orig, [int]((256 - $w) / 2), [int]((256 - $h) / 2), $w, $h)
$g.Dispose(); $orig.Dispose()
$ms = New-Object IO.MemoryStream
$bmp.Save($ms, [Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
$dados = $ms.ToArray()
$fs = [IO.File]::Create($ico); $bw = New-Object IO.BinaryWriter $fs
$bw.Write([UInt16]0); $bw.Write([UInt16]1); $bw.Write([UInt16]1)          # cabeçalho: 1 imagem
$bw.Write([byte]0); $bw.Write([byte]0); $bw.Write([byte]0); $bw.Write([byte]0) # 256x256, sem paleta
$bw.Write([UInt16]1); $bw.Write([UInt16]32); $bw.Write([UInt32]$dados.Length); $bw.Write([UInt32]22)
$bw.Write($dados); $bw.Close()

# 4. segundo atalho: assistente de configuração (console)
$props = Join-Path $trabalho 'configurar.properties'
@(
    'main-class=geeks.tools.ConfigurarLoja',
    'win-console=true',
    'win-shortcut=false',
    'win-menu=true',
    ('icon=' + $ico.Replace('\', '/'))
) | Set-Content -Path $props -Encoding ascii

# 5. jpackage
# O UUID de atualização é fixo: uma versão nova instala por cima da anterior.
& "$Jdk\bin\jpackage.exe" --type exe `
    --name 'Sistema Geeks' --app-version $Versao --vendor 'Geeks' `
    --description 'Controle de tempo, reservas e Clube da Geeks' `
    --input $entrada --main-jar 'sistema-geeks.jar' --main-class 'geeks.MainGeeks' `
    --icon $ico --java-options '-Dfile.encoding=UTF-8' `
    --add-launcher "Configurar Sistema Geeks=$props" `
    --win-menu --win-menu-group 'Sistema Geeks' --win-shortcut --win-dir-chooser `
    --win-upgrade-uuid '6f1c2a5e-3b7d-4c1e-9a52-7d0e8b4f2c19' `
    --dest $saida
if ($LASTEXITCODE -ne 0) { throw "jpackage terminou com código $LASTEXITCODE" }

$exe = Get-ChildItem $saida -Filter '*.exe' | Select-Object -First 1
Write-Host ''
Write-Host "Instalador gerado: $($exe.FullName) ($([math]::Round($exe.Length / 1MB, 1)) MB)" -ForegroundColor Green
