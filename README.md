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
4. Abra o **"Sistema Geeks"** pela área de trabalho. Na primeira vez abre sozinho o **assistente de configuração**
   (janela com telas, sem PowerShell), que pede em ordem: a senha do MySQL (só uma vez; não fica salva),
   o nome/login/senha do gerente, os equipamentos da loja (nome + tipo) e o backup automático diário
   (botão "Escolher pasta" e horário). No fim mostra a senha do backup com o botão "Copiar senha".
5. Pronto: o assistente abre o login do sistema.

O mesmo assistente fica no menu Iniciar como **"Configurar Sistema Geeks"** (para acrescentar equipamentos ou
mudar o backup); num computador já configurado ele não pede a senha do MySQL de novo.
Modo texto antigo, para suporte técnico: `java -cp sistema-geeks.jar geeks.tools.ConfigurarLoja --console`.

Funcionários: depois que o gerente existe, ele mesmo cuida da equipe pela tela **Funcionários** do sistema
(só aparece para gerente): cadastra atendentes ou outros gerentes, troca senha (funcionário que esqueceu)
e desativa/reativa. Ninguém consegue desativar o próprio acesso e sempre sobra pelo menos um gerente ativo.
Desativar não apaga: o funcionário só deixa de entrar, e o histórico continua com o nome dele.
Equipamentos novos: abra "Configurar Sistema Geeks" de novo (ele mantém o que já existe).
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

## Pendências que dependiam do front (todas resolvidas)

| # | Tela | O que foi feito |
|---|---|---|
| 1 | Encerrar sessão, Clube e reserva | a tela pergunta a **forma de pagamento** (Dinheiro, PIX, Débito, Crédito, lidas da tabela `forma_pagamento`) |
| 2 | Cadastro de cliente | **data de nascimento** (dd/mm/aaaa ou só os 8 números), no lugar da idade |
| 3 | Cadastro de cliente | **responsável** com nome, parentesco, CPF e telefone, gravado na tabela `responsavel`; obrigatório para menores de 18 |
| 4 | Nova sessão | escolha de **tempo livre** (mínimo 1 h) ou **fechado** (30 min a 4 h, com horário previsto) |
| 5 | Encerrar sessão | opção **usar crédito do cliente** (do mesmo tipo de equipamento); abate do crédito que vence primeiro e fica registrado em `participante_sessao.segundos_credito` (migration V5) |
| 6 | Nova reserva | escolha da **duração** (30 min a 4 h) |
| 7 | Nova reserva | **entrada paga na hora** com forma de pagamento: lança o pagamento e a reserva fica CONFIRMADA. Quando o cliente chega e a sessão é aberta no equipamento reservado, a reserva **vira a sessão** (tempo fechado até o fim da reserva) e a entrada é **descontada** no encerramento |
| 8 | Nova reserva | o aviso "equipamento em uso sem hora para acabar" virou pergunta **"Reservar mesmo assim?"** (sugere outro equipamento livre do mesmo tipo). Sessão de tempo fechado bloqueia reservas antes do fim + folga |
| 9 | Encerrar sessão | a confirmação mostra **hora grátis do Clube, crédito usado, entrada já paga e crédito que sobra**; ao encerrar com sobra de crédito aparece um aviso |
| 10 | Funcionários | tela **Funcionários** (só gerente) |

### Ajustes depois do teste com o cliente

| Onde | O que foi feito |
|---|---|
| Campos de senha | ícone de **olho** para mostrar/ocultar a senha (login, funcionário, troca de senha) |
| Menu lateral | mostra quem está logado e o botão **Sair** (volta ao login sem fechar o sistema) |
| Janelas (reserva, encerrar...) | podem ser redimensionadas e **rolam** quando não cabem na tela |
| Cadastro | **máscaras**: telefone com DDD automático, CPF limitado a 11 números, data dd/mm/aaaa; telefone do responsável não pode ser o do cliente |
| Histórico | ficha **Dados do usuário** completa (nascimento, responsável, Clube até quando, sessões, última visita, crédito), sem cortes |
| Clube | busca por nome mostra a **lista** quando há nomes parecidos |
| Sessões | cartão do tempo fechado mostra **até que horas e quanto falta** (laranja nos últimos 10 min, vermelho ao esgotar); valor já descontada a entrada da reserva |
| Avisos | a cada 30 s: **tempo fechado acabando** (uma vez por sessão, marcado no banco) e **reserva chegando** (`geeks.alertaReservaMinutos`, padrão 10) |
| Encerrar sessão | grava **o mesmo horário e valor mostrados** na janela |
| Nova sessão | reserva de outro cliente **já em andamento** também bloqueia o equipamento |
| Funcionários | dois gerentes desativando um ao outro ao mesmo tempo não travam o banco |

### Dia a dia (versão 1.0.4)

| Onde | O que foi feito |
|---|---|
| **Caixa** (só gerente) | fechamento do dia: total e total por forma de pagamento, cada pagamento com cliente, tipo e quem recebeu; navega por data |
| **Estorno** (só gerente) | no Caixa, botão Estornar: devolve parte ou todo um pagamento, com motivo obrigatório. O pagamento original não muda; o estorno fica na tabela `estorno` (migration V6) e o faturamento o desconta **na data do estorno** |
| Sessões | botão **Mais tempo** nas sessões de tempo fechado (conta do fim combinado; recusa se bater na reserva de outro cliente) |
| Sessão esquecida | aviso quando uma sessão de tempo livre passa de `geeks.sessaoLongaHoras` (padrão 6 h); o **gerente** encerra informando a **saída real** (a primeira vez que deu aquela hora depois do início). Fica gravado quem corrigiu (`sessao.fim_corrigido_por`) e o pagamento entra no caixa de **hoje** |
| Reservas | em "Gerenciar reservas", reserva cujo horário já passou é marcada como **não compareceu** (`NAO_COMPARECEU`); a entrada paga fica com a loja |
| Histórico | ficha "Dados do usuário" com botão **Editar dados** (nome, nascimento, telefone e responsável; o CPF não muda) |
| Sessões | a pílula "R$ X hoje" só aparece para o gerente (o model também recusa para o atendente) |
| Janela | o sistema abre **maximizado** |

### Gestão do gerente e acabamentos (versão 1.0.5)

| Onde | O que foi feito |
|---|---|
| Primeira configuração | **assistente com telas** (sem PowerShell) que abre sozinho num computador novo; o atalho "Configurar Sistema Geeks" abre o mesmo assistente |
| **Loja** (só gerente) | **preços** (valor da hora de cada tipo e mensalidade do Clube; sessões já abertas mantêm o preço de quando começaram) e **equipamentos** (cadastrar, colocar em manutenção, voltar a usar; em manutenção some das telas de sessão e reserva, e avisa se havia reservas) |
| Clube | lista de quem **vence nos próximos 7 dias ou venceu há até 7 dias** (clicar abre o cliente), chip "Clube até dd/mm/aaaa" e botão **Renovar (+1 mês)** antes de vencer |
| Faturamento | **relatórios** do período: equipamentos mais usados, horários de pico e clientes mais frequentes |
| Sessões | **+ Jogador**: amigos jogando junto no mesmo equipamento (até o máximo do tipo: PC 1, PlayStation 4, Switch 4, Xadrez 2). Quem paga é o titular; os acompanhantes ficam registrados e não podem estar em outra sessão (migration V7) |
| Cobrança | sessão encerrada em até 2 minutos foi aberta **por engano**: não cobra (`geeks.toleranciaEnganoSegundos`) |
| Banco | Flyway atualizado para a 13.12 |
## Decisões a confirmar com a loja

- Depois da 1ª hora, o tempo ABERTO é cobrado proporcional ao uso (por segundo, arredondado ao centavo).
- Sessão encerrada em até 2 minutos (aberta por engano) não cobra nada.
- Hora grátis do Clube conta pelo dia em que a sessão **começou** (sessão que cruza a meia-noite usa a do dia do início).
- Cancelar uma reserva com entrada paga não devolve a entrada pelo sistema (devolução, se houver, é feita por fora).
- Se a entrada paga for maior que o valor final da sessão, a diferença não vira troco nem crédito.
- Destino do backup: hoje `C:\GeeksBackup`, no mesmo disco. O recomendado é um HD externo ou outro computador.

## Interface (notas do front)

Animações (classe Animacao): clique (EfeitoClique), entrada de telas e janelas, menu com pílula deslizante, cartões com hover/entrada/saída, barras e valores que "contam".
Janelas de confirmação, aviso, sucesso e formulários seguem o tema (FormularioGeeks e Form*, abertas por Dialogos).
Fonte opcional: instale "Barlow" para ficar idêntico ao design; sem ela o sistema usa Segoe UI/SansSerif.
Clube Geeks: plano único (valor vem do banco, tabela `plano_clube`).
