# Sistema Geeks (Java Swing, MVC + MySQL)

Sistema de controle de tempo, reservas, Clube Geeks e faturamento da loja.
Interface em Java Swing; dados em MySQL (JDBC + HikariCP), com estrutura criada por migrations (Flyway).

## Requisitos

| Item | Versão | Para quê |
|---|---|---|
| JDK | 17 ou mais novo | compilar e rodar |
| Maven | 3.9+ | build (`mvn package`) |
| MySQL Server | 8.0.16+ (testado no 26.7) | banco de dados |
| 7-Zip | qualquer recente | só para o backup criptografado |

## Instalação na loja (instalador)

No computador da loja não precisa de Java, Maven, Git nem PowerShell:

1. Instale o **MySQL Server** (instalador oficial do MySQL; anote a senha do `root` que ele pedir).
2. Instale o **7-Zip** (7-zip.org) — só para o backup automático.
3. Rode o **`Sistema Geeks-1.0.0.exe`** (avançar, avançar, concluir).
4. Abra **"Configurar Sistema Geeks"** no menu Iniciar (uma vez só). Ele pergunta, em ordem:
   a senha do root do MySQL, o login/nome/senha do gerente (e outros funcionários),
   os equipamentos da loja e se quer backup automático diário (pasta e horário).
5. Pronto: o sistema abre pelo atalho **"Sistema Geeks"** na área de trabalho.

Funcionários: depois que o gerente existe, ele mesmo cuida da equipe pela tela **Funcionários** do sistema
(só aparece para gerente): cadastra atendentes ou outros gerentes, troca senha (funcionário que esqueceu)
e desativa/reativa. Ninguém consegue desativar o próprio acesso e sempre sobra pelo menos um gerente ativo.
Desativar não apaga: o funcionário só deixa de entrar, e o histórico continua com o nome dele.
Equipamentos novos: abra o configurador de novo (ele mantém o que já existe).
A configuração fica em `C:\ProgramData\SistemaGeeks` (legível só pelo usuário do Windows que rodou o configurador:
use o mesmo usuário do Windows para o configurador e para o sistema).

### Gerar o instalador (desenvolvedor)

```powershell
powershell -ExecutionPolicy Bypass -File instalador\gerar_instalador.ps1 -Jdk "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
```

Precisa de um JDK 17+ (com `jpackage`) e do WiX Toolset 3.14 (zip `wix314-binaries.zip` do GitHub oficial
`wixtoolset/wix3`, extraído em `%USERPROFILE%\tools\wix314` ou informado com `-Wix`).
Gera `target\instalador\Sistema Geeks-<versão>.exe` (~55 MB, já com o Java dentro).
O script se recusa a empacotar arquivos com senha (`*.properties`, `backup.cnf`, `senha-backup.txt`).

## Instalação para desenvolvimento

Todos os comandos são no PowerShell, na pasta do projeto.

1. **Arquivo de configuração** — copie os modelos e troque as senhas (invente senhas fortes, diferentes entre si):

   ```powershell
   Copy-Item db.properties.example db.properties
   Copy-Item db.test.properties.example db.test.properties
   notepad db.properties
   ```

   `db.properties` e `db.test.properties` **não vão para o Git** (estão no `.gitignore`).
   As senhas do `db.test.properties` devem ser **as mesmas** do `db.properties` (os usuários do MySQL são os mesmos).

2. **Bancos e usuários do MySQL** (pede a senha do `root` do MySQL; ela não fica salva):

   ```powershell
   powershell -ExecutionPolicy Bypass -File db\scripts\criar_bancos_e_usuarios.ps1
   ```

   Cria os bancos `geeks` e `geeks_test` e dois usuários:
   - `geeks_migrator`: dono da estrutura (cria/altera tabelas). Usado só na inicialização, pelas migrations.
   - `geeks_app`: usado pelo sistema no dia a dia. Só `SELECT`, `INSERT`, `UPDATE` (e `DELETE` apenas em `reserva`). Não consegue apagar histórico nem alterar tabelas.

3. **Build**:

   ```powershell
   mvn package
   ```

   Gera `target\sistema-geeks.jar` (com todas as bibliotecas dentro).

4. **Primeiro gerente** (em produção não existe usuário/senha padrão):

   ```powershell
   java -cp target\sistema-geeks.jar geeks.tools.CriarUsuario
   ```

   Pede login, nome, cargo (`ATENDENTE` ou `GERENTE`) e a senha (mínimo 8 caracteres, digitada sem aparecer).
   A senha é guardada como hash BCrypt. Rodar de novo com o mesmo login troca a senha.

5. **Equipamentos** — cadastre os PCs/consoles da loja editando e rodando `db\scripts\cadastrar_dispositivo.sql` (veja `db\README.md`).

## Como rodar

```powershell
java -jar target\sistema-geeks.jar
```

Ao abrir, o sistema aplica as migrations pendentes (Flyway) **antes** do login. Se o banco estiver fora do ar,
a senha estiver errada ou uma migration falhar, aparece uma janela de erro e o sistema fecha (código de saída 2).

O `db.properties` é procurado, nesta ordem: `-Dgeeks.db.config=C:\caminho\db.properties`, a pasta atual, a pasta do `.jar`
e `C:\ProgramData\SistemaGeeks` (sistema instalado).

### Dados de exemplo (só desenvolvimento)

```powershell
java -cp target\sistema-geeks.jar geeks.tools.CarregarSeedDev
```

Recusa rodar se o banco já tiver clientes. Cria equipamentos, clientes, sessões, reservas e dois usuários:
`atendente` / `geeks123` e `gerente` / `gerente123`. **Não use em produção.**

## Configuração (`db.properties`)

| Chave | O que é |
|---|---|
| `db.url` | URL JDBC (banco `geeks`, fuso `-03:00`) |
| `db.migrator.usuario` / `db.migrator.senha` | usuário das migrations |
| `db.app.usuario` / `db.app.senha` | usuário do dia a dia |
| `db.pool.minimo` / `db.pool.maximo` | conexões do pool (2 a 5) |

Regras ajustáveis sem recompilar (opcionais, com `-D` antes do `-jar`):

| Propriedade | Padrão | O que é |
|---|---|---|
| `geeks.janelaMinutos` | 60 | folga mínima entre reservas / reserva e sessão aberta |
| `geeks.duracaoReservaMinutos` | 60 | duração de uma reserva (a tela não pede o fim) |
| `geeks.percentualEntrada` | 50.00 | % de entrada calculado na reserva |
| `geeks.formaPagamentoPadrao` | Dinheiro | forma de pagamento usada (a tela não pergunta) |
| `geeks.alertaReservaMinutos` | 10 | antecedência do aviso de reserva |

Exemplo: `java -Dgeeks.janelaMinutos=30 -jar target\sistema-geeks.jar`

## Testes

```powershell
mvn test           # só os testes sem banco (cálculo de cobrança)
mvn verify -Pdb    # todos, incluindo os que usam o MySQL (banco geeks_test)
```

Os testes de banco **apagam e recriam o `geeks_test`** a cada teste e se recusam a rodar em um banco com outro nome.

## Backup

Veja `db\README.md`, seção *Backup*. Resumo:

```powershell
powershell -ExecutionPolicy Bypass -File db\backup\configurar_backup.ps1   # uma vez
powershell -ExecutionPolicy Bypass -File db\backup\backup.ps1              # backup agora
powershell -ExecutionPolicy Bypass -File db\backup\testar_restauracao.ps1  # confere o último backup
powershell -ExecutionPolicy Bypass -File db\backup\agendar_backup.ps1      # todo dia às 23:30
```

## Estrutura

    pom.xml                         dependências e build (Maven)
    db.properties.example           modelo da configuração (o real fica fora do Git)
    db/
      scripts/                      criação de bancos/usuários (root), cadastro de equipamento
      seed-dev/                     dados de exemplo (só desenvolvimento)
      backup/                       backup, restauração e agendamento (PowerShell)
      README.md                     detalhes do banco
    src/main/java/geeks/
      MainGeeks.java                ponto de entrada: migrations -> pool -> login
      view/                         telas (Swing); a integração com o banco não mudou as telas existentes; nova: PainelFuncionarios/FormFuncionario
      controller/                   liga telas e model (acrescentada só a tela Funcionários)
      model/                        regras de negócio (GeeksModel, Cobranca, LoginModel, FaturamentoModel...)
                                    Contexto guarda o funcionário logado e barra o que exige GERENTE
      dados/                        acesso ao MySQL: Banco (Flyway + pool), Transacao, Sql, *DAO, ErroBanco
      tools/                        ConfigurarLoja (assistente da loja), CriarUsuario, CarregarSeedDev
    instalador/                     gerar_instalador.ps1 (jpackage + WiX -> .exe)
    src/main/resources/
      db/migration/                 V1..V4 (Flyway) — migration aplicada NUNCA é editada; mudança = nova V5...
      img/                          logos
    src/test/java/                  JUnit 5

### Segurança

- Nenhuma senha no código ou no Git: só nos arquivos `db.properties`, `db\backup\backup.cnf` e `db\backup\senha-backup.txt` (todos no `.gitignore`).
- Senhas de funcionários com BCrypt (custo 11); login com usuário inexistente gasta o mesmo tempo que senha errada.
- Funcionário inativo (`ativo = 0`) não entra.
- Faturamento: a regra "só GERENTE" é checada no model (`Contexto.exigirGerente()`), não só escondendo a tela.
- Funcionários: além disso, cada ação confere no banco se quem está logado ainda é gerente ativo
  (um gerente desativado em outro computador perde o acesso na hora, mesmo com o sistema aberto).
- O banco recusa dados inválidos mesmo que o sistema tente gravar (CHECKs, chaves únicas, chaves estrangeiras).
- O usuário do sistema (`geeks_app`) não consegue apagar clientes, sessões ou pagamentos, nem alterar a estrutura.
- Limitação: o MySQL não sabe quem é o funcionário logado (todos usam o `geeks_app`), então a diferença
  entre ATENDENTE e GERENTE é garantida pela aplicação, não pelo banco.

## Pendências que dependem do front

A integração com o banco foi feita **sem alterar as telas existentes** (a única tela nova é Funcionários, pedida depois). Onde a tela não tem um campo que o banco precisa,
o sistema usa um padrão provisório:

| # | Tela | Falta na tela | O que o sistema faz hoje |
|---|---|---|---|
| 1 | Encerrar sessão / Clube | escolher a forma de pagamento | grava tudo como **Dinheiro** (`geeks.formaPagamentoPadrao`) |
| 2 | Cadastro de cliente | data de nascimento (a tela pede a **idade**) | grava nascimento = hoje − idade (dia/mês aproximados) |
| 3 | Cadastro de cliente (menor) | CPF e telefone do responsável | o responsável **não é gravado** (o banco exige CPF). A view `vw_menor_sem_responsavel` lista esses clientes |
| 4 | Nova sessão | escolher tempo ABERTO ou FECHADO (e o horário previsto) | toda sessão começa como **ABERTO** (mínimo 1 hora; o que sobrar vira crédito) |
| 5 | Encerrar / nova sessão | usar o crédito de tempo do cliente | o crédito é gerado e guardado (3 meses), mas **não há como usar** |
| 6 | Nova reserva | horário de fim | toda reserva dura **60 min** (`geeks.duracaoReservaMinutos`) |
| 7 | Nova reserva | registrar o pagamento da entrada | o valor da entrada (50%) é calculado e guardado na reserva, mas **nenhum pagamento é lançado** |
| 8 | Nova reserva | aviso "há sessão aberta nesse equipamento" com opção de continuar | aparece como **erro** e a reserva não é feita; a mensagem sugere outro equipamento livre do mesmo tipo |
| 9 | Encerrar sessão | mostrar hora grátis usada e crédito gerado | são gravados no banco; a confirmação de encerramento mostra só o valor |
| 10 | ~~Funcionários~~ | ~~cadastro/troca de senha pela tela~~ | **resolvido**: tela Funcionários (só gerente) |

## Decisões a confirmar com a loja

- Depois da 1ª hora, o tempo ABERTO é cobrado proporcional ao uso (por segundo, arredondado ao centavo).
- Sessão aberta e encerrada no mesmo instante (engano) não cobra nada.
- Hora grátis do Clube conta pelo dia em que a sessão **começou** (sessão que cruza a meia-noite usa a do dia do início).
- Destino do backup: hoje `C:\GeeksBackup`, no mesmo disco. O recomendado é um HD externo ou outro computador.

## Interface (notas do front)

Animações (classe Animacao): clique (EfeitoClique), entrada de telas e janelas, menu com pílula deslizante, cartões com hover/entrada/saída, barras e valores que "contam".
Janelas de confirmação, aviso, sucesso e formulários seguem o tema (FormularioGeeks e Form*, abertas por Dialogos).
Fonte opcional: instale "Barlow" para ficar idêntico ao design; sem ela o sistema usa Segoe UI/SansSerif.
Clube Geeks: plano único (valor vem do banco, tabela `plano_clube`).
