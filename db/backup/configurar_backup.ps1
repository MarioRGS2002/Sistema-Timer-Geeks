# ============================================================
# GEEKS - prepara o backup (rodar uma vez)
# ============================================================
# Cria, nesta pasta (db\backup), os dois arquivos que o backup usa e que NÃO vão
# para o Git (.gitignore):
#   backup.cnf        usuário/senha do MySQL (geeks_migrator, lidos do db.properties)
#   senha-backup.txt  senha de criptografia dos arquivos .7z (gerada aleatória)
# Os dois ficam legíveis só pelo seu usuário do Windows.
#
# GUARDE A SENHA DE BACKUP EM OUTRO LUGAR (gerenciador de senhas): sem ela os
# arquivos .7z não abrem. Se perder o computador, perde a senha junto.
#
# Uso: powershell -ExecutionPolicy Bypass -File db\backup\configurar_backup.ps1
#      (-Recriar para sobrescrever os arquivos existentes)
# ============================================================
param([switch]$Recriar)
. (Join-Path $PSScriptRoot 'comum.ps1')

$utf8 = New-Object Text.UTF8Encoding($false)

if ((Test-Path $Script:Cnf) -and -not $Recriar) {
    Write-Host "backup.cnf já existe (use -Recriar para refazer)."
} else {
    $p = Ler-Propriedades (Join-Path $Script:Raiz 'db.properties')
    $usuario = $p['db.migrator.usuario']
    $senha = $p['db.migrator.senha']
    if (-not $usuario -or -not $senha -or $senha -like 'TROQUE*') { throw 'Preencha db.migrator.usuario e db.migrator.senha no db.properties.' }
    $senhaCnf = $senha.Replace('\', '\\').Replace('"', '\"')
    $texto = "[client]`r`nuser=$usuario`r`npassword=`"$senhaCnf`"`r`nhost=localhost`r`nport=3306`r`n"
    [IO.File]::WriteAllText($Script:Cnf, $texto, $utf8)
    Restringir-Acesso $Script:Cnf
    Write-Host "Criado: $($Script:Cnf)"
}

if ((Test-Path $Script:ArquivoSenha) -and -not $Recriar) {
    Write-Host "senha-backup.txt já existe (use -Recriar para refazer; backups antigos continuam com a senha antiga)."
} else {
    $letras = [char[]]'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    $bytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $senha = -join ($bytes | ForEach-Object { $letras[$_ % $letras.Length] })
    [IO.File]::WriteAllText($Script:ArquivoSenha, $senha, $utf8)
    Restringir-Acesso $Script:ArquivoSenha
    Write-Host "Criado: $($Script:ArquivoSenha)"
    Write-Host 'IMPORTANTE: copie a senha desse arquivo para um gerenciador de senhas.' -ForegroundColor Yellow
}
