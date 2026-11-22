# Banco de dados do Sistema Geeks

MySQL 8.0.16+ (testado no 26.7), `utf8mb4` / `utf8mb4_0900_ai_ci`, InnoDB.
A estrutura é criada pelas migrations em `src/main/resources/db/migration`, aplicadas pelo Flyway quando o sistema abre.

## Pastas

| Pasta | Conteúdo |
|---|---|
| `scripts/` | `criar_bancos_e_usuarios.ps1` + `.sql` (rodar como root, uma vez); `cadastrar_dispositivo.sql` |
| `seed-dev/` | dados de exemplo (só desenvolvimento; `geeks.tools.CarregarSeedDev`) |
| `backup/` | backup criptografado, teste de restauração e agendamento |

## Migrations

| Versão | O que faz |
|---|---|
| V1 `cadastros` | cliente, responsavel, tipo_equipamento, equipamento, funcionario, plano_clube, forma_pagamento |
| V2 `movimento` | assinatura_clube, sessao, participante_sessao, reserva, pagamento, credito_tempo, view `vw_menor_sem_responsavel` |
| V3 `dados_base` | tipos (PC, PlayStation, Nintendo Switch, Xadrez — R$ 10,00/h), Clube Geeks (R$ 180,00), formas de pagamento |
| V4 `permissao_delete_reserva` | `GRANT DELETE ON reserva TO geeks_app` |

Regras:
- **Nunca edite uma migration já aplicada** (o Flyway recusa: `validateOnMigrate`). Mudança de estrutura = novo arquivo `V5__descricao.sql`.
- `flyway clean` está desligado (`cleanDisabled`).
- Migrations rodam com o `geeks_migrator`; o sistema usa o `geeks_app`.

## Usuários do MySQL

| Usuário | Permissões | Uso |
|---|---|---|
| `geeks_migrator` | tudo em `geeks`, `geeks_test` e `geeks_restore_test` | migrations, ferramentas, backup |
| `geeks_app` | `SELECT, INSERT, UPDATE` em `geeks` e `geeks_test`; `DELETE` só em `reserva` | o sistema no dia a dia |

Para trocar uma senha: altere no `db.properties` e rode de novo `scripts\criar_bancos_e_usuarios.ps1` (faz `ALTER USER`).
Depois, se usar backup: `backup\configurar_backup.ps1 -Recriar` (atenção: isso também gera uma nova senha de criptografia).

## Restrições que o banco garante

Os nomes abaixo aparecem nas mensagens de erro; o sistema traduz para uma mensagem amigável (`ErroBanco`). **Não renomeie.**

| Restrição | Garante |
|---|---|
| `uq_cliente_cpf` | um cadastro por CPF |
| `uq_assinatura_ativa` | no máximo uma assinatura ATIVA por cliente |
| `uq_sessao_equipamento_em_uso` | no máximo uma sessão em andamento por equipamento (vale mesmo com dois computadores ao mesmo tempo) |

Além disso: CPF com 11 dígitos, valores não negativos, fim depois do início, sessão FECHADO com horário previsto e ABERTO sem,
hora grátis entre 0 e 3600 s, pagamento com exatamente uma origem (sessão, reserva ou assinatura),
pagamento de valor 0 sem forma de pagamento e valor > 0 com forma, crédito com validade depois da geração.

Nada que tenha histórico é apagado: cliente/funcionário/equipamento são desativados; reserva é cancelada (`status_reserva = 'CANCELADA'`).

## Convenções

- Dinheiro: `DECIMAL(10,2)`. Nomes: `snake_case`.
- `JOIN ... ON` explícito, com apelidos.
- Períodos de data sempre semiabertos: `data >= inicio AND data < fim` (nunca `BETWEEN` em datas).
- Fuso: a conexão usa `-03:00` fixo (Brasília, sem horário de verão desde 2019). `America/Sao_Paulo` exige carregar as
  tabelas de fuso no MySQL, que não vêm no Windows (erro *Unknown or incorrect time zone*).

## Cadastrar equipamento

Edite a lista de `INSERT` em `scripts\cadastrar_dispositivo.sql` (um bloco por equipamento: identificador e tipo) e rode com o migrator:
`cmd /c "mysql --defaults-extra-file=db\backup\backup.cnf geeks < db\scripts\cadastrar_dispositivo.sql"`
(ou abra o arquivo no MySQL Workbench). Não duplica se o identificador já existir. Reinicie o sistema depois: a lista é lida ao abrir.
Para tirar um equipamento de uso, não apague: `UPDATE equipamento SET status = 'INDISPONIVEL' WHERE identificador = '...'`.

## Problemas comuns

| Mensagem | Causa / solução |
|---|---|
| `Public Key Retrieval is not allowed` | MySQL sem SSL com `caching_sha2_password`. Prefira ligar o SSL no servidor; em último caso acrescente `&allowPublicKeyRetrieval=true` à `db.url` |
| `Unknown or incorrect time zone` | use `connectionTimeZone=-03:00` na `db.url` (já é o padrão do `.example`) |
| `Access denied for user 'geeks_app'` | senha do `db.properties` diferente da do MySQL: rode `criar_bancos_e_usuarios.ps1` de novo |
| `Validate failed: Migrations have failed validation` | alguém editou uma migration já aplicada; desfaça a edição e crie uma nova versão |
| `mysqldump: ... masking policies` | aviso do MySQL 26.x (recurso que o Geeks não usa); o backup continua válido |

## Backup

Arquivos em `backup\` (PowerShell, sem `.sh`):

| Script | O que faz |
|---|---|
| `configurar_backup.ps1` | uma vez: cria `backup.cnf` (usuário/senha do MySQL, lido do `db.properties`) e `senha-backup.txt` (senha aleatória de criptografia). Os dois ficam fora do Git e legíveis só pelo seu usuário do Windows |
| `backup.ps1` | `mysqldump --single-transaction --routines --triggers` → `.7z` com AES-256 (nomes cifrados também) → confere o arquivo → apaga backups com mais de `-DiasManter` dias (14 a 30; padrão 30). Log em `<Destino>\backup.log` |
| `testar_restauracao.ps1` | restaura o último `.7z` no banco `geeks_restore_test`, compara a contagem de linhas de cada tabela com o `geeks` e apaga o banco de teste |
| `agendar_backup.ps1` | cria a tarefa "Sistema Geeks - Backup" no Agendador do Windows (`schtasks`), diária, padrão 23:30 |

```powershell
powershell -ExecutionPolicy Bypass -File db\backup\configurar_backup.ps1
powershell -ExecutionPolicy Bypass -File db\backup\backup.ps1 -Destino C:\GeeksBackup -DiasManter 30
powershell -ExecutionPolicy Bypass -File db\backup\testar_restauracao.ps1 -Destino C:\GeeksBackup
powershell -ExecutionPolicy Bypass -File db\backup\agendar_backup.ps1 -Hora 23:30 -Destino C:\GeeksBackup
```

Importante:
- **Guarde a senha do `senha-backup.txt` num gerenciador de senhas.** Sem ela, os backups não abrem.
- O destino padrão (`C:\GeeksBackup`) fica no mesmo disco do banco: protege contra erro humano, não contra o disco queimar.
  Aponte `-Destino` para um HD externo ou pasta de rede assim que possível.
- Rode `testar_restauracao.ps1` de vez em quando (ex.: uma vez por mês). Backup que nunca foi restaurado não é garantia.
- A tarefa agendada roda com o seu usuário do Windows: o computador precisa estar ligado e com você logado no horário.
- O 7-Zip recebe a senha pela linha de comando; durante os segundos do backup ela fica visível para outros programas do mesmo computador.

### Restaurar de verdade (perda do banco)

1. Extraia o `.7z` (o 7-Zip pede a senha do `senha-backup.txt`).
2. Feche o sistema em todos os computadores.
3. Restaure no banco `geeks` com o migrator:
   `cmd /c "mysql --defaults-extra-file=db\backup\backup.cnf geeks < C:\caminho\geeks-AAAA-MM-DD_HHMMSS.sql"`
4. Abra o sistema: o Flyway confere a versão do banco.
