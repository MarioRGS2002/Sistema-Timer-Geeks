# Funções usadas pelos scripts de backup (carregado com ". comum.ps1").
$ErrorActionPreference = 'Stop'
$Script:PastaBackup = $PSScriptRoot
$Script:Raiz = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
# Onde ficam db.properties, backup.cnf e senha-backup.txt:
#  - sistema instalado (Configurar Sistema Geeks): %ProgramData%\SistemaGeeks
#  - desenvolvimento: db.properties na raiz do projeto e os outros dois nesta pasta
$Script:PastaLoja = Join-Path $env:ProgramData 'SistemaGeeks'
if ($env:GEEKS_CONFIG_DIR) { $Script:PastaLoja = $env:GEEKS_CONFIG_DIR }
if (Test-Path (Join-Path $Script:Raiz 'db.properties')) {
    $Script:DbProperties = Join-Path $Script:Raiz 'db.properties'
    $Script:PastaSegredos = $PSScriptRoot
} else {
    $Script:DbProperties = Join-Path $Script:PastaLoja 'db.properties'
    $Script:PastaSegredos = $Script:PastaLoja
}
$Script:Cnf = Join-Path $Script:PastaSegredos 'backup.cnf'
$Script:ArquivoSenha = Join-Path $Script:PastaSegredos 'senha-backup.txt'

function Ler-Propriedades([string]$arquivo) {
    if (-not (Test-Path $arquivo)) { throw "Arquivo não encontrado: $arquivo" }
    $p = @{}
    foreach ($linha in Get-Content $arquivo -Encoding UTF8) {
        $l = $linha.Trim().TrimStart([char]0xFEFF)
        if ($l -eq '' -or $l.StartsWith('#')) { continue }
        $i = $l.IndexOf('=')
        if ($i -gt 0) { $p[$l.Substring(0, $i).Trim()] = $l.Substring($i + 1).Trim() }
    }
    return $p
}

function Achar-Exe([string]$nome, [string[]]$pastas) {
    $c = Get-Command $nome -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    foreach ($pasta in $pastas) {
        if (-not (Test-Path $pasta)) { continue }
        $achado = Get-ChildItem $pasta -Recurse -Filter $nome -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($achado) { return $achado.FullName }
    }
    throw "$nome não encontrado. Instale ou coloque a pasta dele no PATH."
}

function Achar-Mysql([string]$nome) { return Achar-Exe $nome @('C:\Program Files\MySQL') }
function Achar-7z { return Achar-Exe '7z.exe' @('C:\Program Files\7-Zip', 'C:\Program Files (x86)\7-Zip') }

function Exigir-Configuracao {
    foreach ($f in $Script:Cnf, $Script:ArquivoSenha) {
        if (-not (Test-Path $f)) {
            throw "Falta $f. Rode antes: powershell -ExecutionPolicy Bypass -File db\backup\configurar_backup.ps1"
        }
    }
}

function Senha-Backup {
    $s = (Get-Content $Script:ArquivoSenha -Raw -Encoding UTF8).Trim().TrimStart([char]0xFEFF)
    if ($s.Length -lt 16) { throw "A senha em $($Script:ArquivoSenha) precisa ter pelo menos 16 caracteres." }
    return $s
}

# Executa o mysql com o usuário do backup.cnf; devolve as linhas (-N -B: sem cabeçalho, separado por TAB)
function Mysql-Consulta([string]$sql) {
    $mysql = Achar-Mysql 'mysql.exe'
    $saida = & $mysql "--defaults-extra-file=$($Script:Cnf)" -N -B --default-character-set=utf8mb4 -e $sql
    if ($LASTEXITCODE -ne 0) { throw "mysql terminou com código $LASTEXITCODE" }
    return $saida
}

function Escrever-Log([string]$destino, [string]$texto) {
    $linha = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $texto"
    Write-Host $linha
    Add-Content -Path (Join-Path $destino 'backup.log') -Value $linha -Encoding UTF8
}

# Deixa o arquivo legível só pelo usuário atual do Windows (e pelo SYSTEM/Administradores)
function Restringir-Acesso([string]$arquivo) {
    # SIDs: S-1-5-18 = SYSTEM, S-1-5-32-544 = Administradores (funciona em Windows de qualquer idioma)
    & icacls $arquivo /inheritance:r /grant:r "$($env:USERNAME):(R,W)" '*S-1-5-18:(F)' '*S-1-5-32-544:(F)' *> $null
    if ($LASTEXITCODE -ne 0) { Write-Warning "Não foi possível restringir o acesso a $arquivo" }
}
