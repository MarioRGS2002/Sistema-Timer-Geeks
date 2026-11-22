# ============================================================
# GEEKS - agenda o backup diário no Agendador de Tarefas do Windows
# ============================================================
# Cria (ou substitui) a tarefa "Sistema Geeks - Backup", que roda backup.ps1
# todo dia no horário escolhido, com o seu usuário do Windows (você precisa
# estar logado). Se o computador estiver desligado no horário, o backup roda
# assim que ele for ligado (StartWhenAvailable).
#
# Usa Register-ScheduledTask (passa o caminho do script sem depender de aspas:
# funciona mesmo instalado em "C:\Program Files\Sistema Geeks"). Se o cmdlet não
# existir, cai no schtasks.exe.
#
# Uso:    powershell -ExecutionPolicy Bypass -File db\backup\agendar_backup.ps1 [-Hora 23:30] [-Destino C:\GeeksBackup] [-DiasManter 30]
# Ver:    schtasks /Query /TN "Sistema Geeks - Backup" /V /FO LIST
# Testar: schtasks /Run /TN "Sistema Geeks - Backup"
# Tirar:  schtasks /Delete /TN "Sistema Geeks - Backup" /F
# ============================================================
param(
    [ValidatePattern('^([01]\d|2[0-3]):[0-5]\d$')][string]$Hora = '23:30',
    [string]$Destino = 'C:\GeeksBackup',
    [ValidateRange(14, 30)][int]$DiasManter = 30,
    [string]$Nome = 'Sistema Geeks - Backup'
)
$ErrorActionPreference = 'Stop'
$script = Join-Path $PSScriptRoot 'backup.ps1'
$argumentos = "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$script`" -Destino `"$Destino`" -DiasManter $DiasManter"

if (Get-Command Register-ScheduledTask -ErrorAction SilentlyContinue) {
    $acao = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument $argumentos -WorkingDirectory $PSScriptRoot
    $quando = New-ScheduledTaskTrigger -Daily -At ([datetime]::ParseExact($Hora, 'HH:mm', $null))
    $opcoes = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
        -ExecutionTimeLimit (New-TimeSpan -Hours 2)
    $quem = New-ScheduledTaskPrincipal -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive
    Register-ScheduledTask -TaskName $Nome -Action $acao -Trigger $quando -Settings $opcoes -Principal $quem `
        -Description 'Backup diário do banco do Sistema Geeks (db\backup\backup.ps1)' -Force | Out-Null
} else {
    # schtasks: no PowerShell 5.1 as aspas internas precisam ir como \" para o caminho com espaços chegar inteiro
    $comando = "powershell.exe $($argumentos.Replace('"', '\"'))"
    if ($comando.Length -gt 261) { throw "Comando longo demais para o schtasks ($($comando.Length) caracteres)." }
    & schtasks /Create /F /SC DAILY /ST $Hora /TN $Nome /TR $comando
    if ($LASTEXITCODE -ne 0) { throw "schtasks terminou com código $LASTEXITCODE" }
}
Write-Host "Agendado: '$Nome' todo dia às $Hora -> $Destino" -ForegroundColor Green
