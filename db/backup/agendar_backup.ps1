# ============================================================
# GEEKS - agenda o backup diário no Agendador de Tarefas do Windows (schtasks)
# ============================================================
# Cria (ou substitui) a tarefa "Sistema Geeks - Backup", que roda backup.ps1
# todo dia no horário escolhido, com o seu usuário do Windows (o computador
# precisa estar ligado e com você logado nesse horário).
#
# Uso:    powershell -ExecutionPolicy Bypass -File db\backup\agendar_backup.ps1 [-Hora 23:30] [-Destino C:\GeeksBackup] [-DiasManter 30]
# Ver:    schtasks /Query /TN "Sistema Geeks - Backup" /V /FO LIST
# Testar: schtasks /Run /TN "Sistema Geeks - Backup"
# Tirar:  schtasks /Delete /TN "Sistema Geeks - Backup" /F
# ============================================================
param(
    [ValidatePattern('^([01]\d|2[0-3]):[0-5]\d$')][string]$Hora = '23:30',
    [string]$Destino = 'C:\GeeksBackup',
    [ValidateRange(14, 30)][int]$DiasManter = 30
)
$ErrorActionPreference = 'Stop'
$nome = 'Sistema Geeks - Backup'
$script = Join-Path $PSScriptRoot 'backup.ps1'
$comando = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$script`" -Destino `"$Destino`" -DiasManter $DiasManter"
if ($comando.Length -gt 261) { throw "Comando longo demais para o schtasks ($($comando.Length) caracteres). Mova o projeto para uma pasta mais curta." }

& schtasks /Create /F /SC DAILY /ST $Hora /TN $nome /TR $comando
if ($LASTEXITCODE -ne 0) { throw "schtasks terminou com código $LASTEXITCODE" }
Write-Host "Agendado: '$nome' todo dia às $Hora -> $Destino" -ForegroundColor Green
