# ============================================================
# GEEKS - backup do banco "geeks"
# ============================================================
# 1. mysqldump --single-transaction (cópia consistente sem parar o sistema)
# 2. compacta e criptografa com 7-Zip (AES-256, nomes de arquivo também cifrados)
# 3. confere o .7z (7z t) e apaga o .sql temporário
# 4. apaga backups com mais de -DiasManter dias (14 a 30)
# Registra tudo em <Destino>\backup.log. Sai com código 1 se algo falhar.
#
# Uso: powershell -ExecutionPolicy Bypass -File db\backup\backup.ps1 [-Destino C:\GeeksBackup] [-DiasManter 30]
# Antes, uma vez: configurar_backup.ps1
#
# Observação: o 7-Zip recebe a senha pela linha de comando (-p). Ela fica visível
# para programas do mesmo computador enquanto o backup roda (alguns segundos).
# ============================================================
param(
    [string]$Destino = 'C:\GeeksBackup',
    [ValidateRange(14, 30)][int]$DiasManter = 30,
    [string]$Banco = 'geeks'
)
. (Join-Path $PSScriptRoot 'comum.ps1')

New-Item -ItemType Directory -Force -Path $Destino | Out-Null
$sql = $null
try {
    Exigir-Configuracao
    $mysqldump = Achar-Mysql 'mysqldump.exe'
    $7z = Achar-7z
    $senha = Senha-Backup

    $carimbo = Get-Date -Format 'yyyy-MM-dd_HHmmss'
    $sql = Join-Path $env:TEMP "$Banco-$carimbo.sql"
    $arquivo = Join-Path $Destino "$Banco-$carimbo.7z"

    Escrever-Log $Destino "Iniciando backup de '$Banco'..."
    # --defaults-extra-file precisa ser o primeiro argumento
    & $mysqldump "--defaults-extra-file=$($Script:Cnf)" --single-transaction --routines --triggers --events `
        --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 "--result-file=$sql" $Banco
    if ($LASTEXITCODE -ne 0) { throw "mysqldump terminou com código $LASTEXITCODE" }
    if ((Get-Item $sql).Length -lt 1000) { throw 'O arquivo do mysqldump ficou vazio ou pequeno demais.' }

    & $7z a -t7z -mx=7 -mhe=on "-p$senha" $arquivo $sql | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "7z (compactar) terminou com código $LASTEXITCODE" }
    & $7z t "-p$senha" $arquivo | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "7z (conferir) terminou com código $LASTEXITCODE" }

    $kb = [math]::Round((Get-Item $arquivo).Length / 1KB, 1)
    Escrever-Log $Destino "OK: $arquivo ($kb KB)"

    $limite = (Get-Date).AddDays(-$DiasManter)
    Get-ChildItem $Destino -Filter "$Banco-*.7z" | Where-Object { $_.LastWriteTime -lt $limite } | ForEach-Object {
        Remove-Item $_.FullName -Force
        Escrever-Log $Destino "Apagado (mais de $DiasManter dias): $($_.Name)"
    }
    exit 0
} catch {
    Escrever-Log $Destino "ERRO: $($_.Exception.Message)"
    exit 1
} finally {
    if ($sql -and (Test-Path $sql)) { Remove-Item $sql -Force }
}
