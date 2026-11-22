# ============================================================
# GEEKS - teste de restauração do backup
# ============================================================
# Abre o .7z mais recente (ou -Arquivo), restaura no banco geeks_restore_test
# (NUNCA no geeks), compara a quantidade de linhas de cada tabela com o banco
# geeks e apaga o geeks_restore_test no final.
#
# Diferenças podem ser normais se o sistema foi usado depois do backup; logo
# depois de um backup as contagens devem ser iguais.
#
# Uso: powershell -ExecutionPolicy Bypass -File db\backup\testar_restauracao.ps1 [-Destino C:\GeeksBackup] [-Arquivo x.7z] [-Manter]
# ============================================================
param(
    [string]$Destino = 'C:\GeeksBackup',
    [string]$Arquivo = '',
    [switch]$Manter
)
. (Join-Path $PSScriptRoot 'comum.ps1')

$Restaurado = 'geeks_restore_test'
$Original = 'geeks'
Exigir-Configuracao
$7z = Achar-7z
$mysql = Achar-Mysql 'mysql.exe'
$senha = Senha-Backup

if (-not $Arquivo) {
    $ultimo = Get-ChildItem $Destino -Filter "$Original-*.7z" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $ultimo) { throw "Nenhum backup $Original-*.7z em $Destino" }
    $Arquivo = $ultimo.FullName
}
Write-Host "Backup: $Arquivo"

$tmp = Join-Path $env:TEMP ('geeks-restauracao-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tmp | Out-Null
$falhas = 0
try {
    & $7z e "-p$senha" "-o$tmp" -y $Arquivo | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "7z (extrair) terminou com código $LASTEXITCODE (senha errada?)" }
    $sql = Get-ChildItem $tmp -Filter '*.sql' | Select-Object -First 1
    if (-not $sql) { throw 'O .7z não tem arquivo .sql' }

    Mysql-Consulta "DROP DATABASE IF EXISTS $Restaurado; CREATE DATABASE $Restaurado CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | Out-Null
    # pela entrada padrão via cmd: o PowerShell 5.1 não converte a codificação do arquivo
    cmd /c "`"$mysql`" `"--defaults-extra-file=$($Script:Cnf)`" --default-character-set=utf8mb4 $Restaurado < `"$($sql.FullName)`""
    if ($LASTEXITCODE -ne 0) { throw "mysql (restaurar) terminou com código $LASTEXITCODE" }

    $tabelas = Mysql-Consulta "SELECT table_name FROM information_schema.tables WHERE table_schema = '$Original' AND table_type = 'BASE TABLE' ORDER BY table_name;"
    Write-Host ''
    Write-Host ('{0,-24} {1,10} {2,12}' -f 'tabela', $Original, 'restaurado')
    foreach ($t in $tabelas) {
        $linha = Mysql-Consulta "SELECT (SELECT COUNT(*) FROM ``$Original``.``$t``), (SELECT COUNT(*) FROM ``$Restaurado``.``$t``);"
        $a, $b = $linha -split "`t"
        $marca = if ($a -eq $b) { '' } else { $falhas++; '  <-- diferente' }
        Write-Host ('{0,-24} {1,10} {2,12}{3}' -f $t, $a, $b, $marca)
    }
    $views = Mysql-Consulta "SELECT COUNT(*) FROM information_schema.views WHERE table_schema = '$Restaurado';"
    Write-Host "views restauradas: $views"
} finally {
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    if (-not $Manter) { Mysql-Consulta "DROP DATABASE IF EXISTS $Restaurado;" | Out-Null }
}

Write-Host ''
if ($falhas -eq 0) {
    Write-Host 'Restauração OK: todas as tabelas com a mesma quantidade de linhas.' -ForegroundColor Green
    exit 0
}
Write-Host "$falhas tabela(s) com contagem diferente (normal só se o sistema foi usado depois do backup)." -ForegroundColor Yellow
exit 1
