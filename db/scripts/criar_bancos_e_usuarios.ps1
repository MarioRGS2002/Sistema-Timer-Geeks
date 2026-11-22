# ============================================================
# GEEKS - cria os bancos e usuários do MySQL (rodar uma vez, como root)
# ============================================================
# Lê as senhas do geeks_migrator e do geeks_app do db.properties (na raiz do
# projeto) e executa 01_criar_bancos_e_usuarios.sql com o usuário root.
# A senha do root é pedida pelo próprio mysql (não aparece na tela nem fica salva).
#
# Uso (na raiz do projeto):
#   powershell -ExecutionPolicy Bypass -File db\scripts\criar_bancos_e_usuarios.ps1
# Opcional: -Mysql "C:\caminho\mysql.exe"  -Root root
# ============================================================
param(
    [string]$Mysql = '',
    [string]$Root = 'root',
    [string]$Propriedades = ''
)
$ErrorActionPreference = 'Stop'
$raiz = Resolve-Path (Join-Path $PSScriptRoot '..\..')
if (-not $Propriedades) { $Propriedades = Join-Path $raiz 'db.properties' }

function Ler-Propriedades([string]$arquivo) {
    if (-not (Test-Path $arquivo)) { throw "Arquivo não encontrado: $arquivo (copie db.properties.example e preencha as senhas)" }
    $p = @{}
    foreach ($linha in Get-Content $arquivo -Encoding UTF8) {
        $l = $linha.Trim()
        if ($l -eq '' -or $l.StartsWith('#')) { continue }
        $i = $l.IndexOf('=')
        if ($i -gt 0) { $p[$l.Substring(0, $i).Trim()] = $l.Substring($i + 1).Trim() }
    }
    return $p
}

function Achar-Mysql {
    $c = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    $achado = Get-ChildItem 'C:\Program Files\MySQL' -Recurse -Filter mysql.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($achado) { return $achado.FullName }
    throw 'mysql.exe não encontrado. Informe com -Mysql "C:\...\bin\mysql.exe".'
}

$p = Ler-Propriedades $Propriedades
foreach ($k in 'db.migrator.senha', 'db.app.senha') {
    if (-not $p[$k] -or $p[$k] -like 'TROQUE*') { throw "Preencha $k em $Propriedades" }
}
if (-not $Mysql) { $Mysql = Achar-Mysql }

function Sql-Texto([string]$s) { return "'" + $s.Replace('\', '\\').Replace("'", "''") + "'" }

# Arquivo temporário = variáveis + conteúdo do .sql (UTF-8 sem BOM), entregue ao
# mysql pela entrada padrão via cmd (sem conversão de codificação do PowerShell).
# Fica em %TEMP% (só o usuário do Windows lê) e é apagado no finally.
$tmp = Join-Path $env:TEMP ("geeks-usuarios-" + [guid]::NewGuid().ToString('N') + '.sql')
$script = Join-Path $PSScriptRoot '01_criar_bancos_e_usuarios.sql'
try {
    $conteudo = "SET @senha_migrator = $(Sql-Texto $p['db.migrator.senha']);`n" +
                "SET @senha_app = $(Sql-Texto $p['db.app.senha']);`n" +
                [IO.File]::ReadAllText($script, [Text.Encoding]::UTF8)
    [IO.File]::WriteAllText($tmp, $conteudo, (New-Object Text.UTF8Encoding($false)))
    Write-Host "Conectando como '$Root' (o mysql vai pedir a senha do $Root)..."
    cmd /c "`"$Mysql`" -u $Root -p --default-character-set=utf8mb4 < `"$tmp`""
    if ($LASTEXITCODE -ne 0) { throw "mysql terminou com codigo $LASTEXITCODE" }
    Write-Host ''
    Write-Host 'Pronto: bancos geeks e geeks_test, usuarios geeks_migrator e geeks_app criados.' -ForegroundColor Green
} finally {
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
}
