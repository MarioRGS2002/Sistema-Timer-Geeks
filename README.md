# Sistema Geeks (Java Swing, MVC)

Estrutura (marque a pasta `src` como *Sources Root* no IntelliJ):

    src/
      img/                    logos oficiais (geeks_logo.png, g_geeks_logo.png)
      geeks/
        MainGeeks.java        ponto de entrada
        model/                regras de negócio (GeeksModel, Sessao, Reserva, Cliente, Plano...)
        view/                 telas (JanelaPrincipal, Painel*, componentes do tema Geeks)
        controller/           GeeksController (liga View e Model)

Java 11+ (testado no JDK 21). Os dados atuais são de exemplo e ficam em memória (GeeksModel.carregarDadosDeExemplo).
Clube Geeks: plano único de R$ 180,00 (enum Plano).
Animações (classe Animacao): clique (EfeitoClique), entrada de telas e janelas, menu com pílula deslizante, cartões com hover/entrada/saída, barras e valores que "contam".
Janelas de confirmação, aviso, sucesso e formulários seguem o tema (FormularioGeeks e Form*, abertas por Dialogos).
Fonte opcional: instale "Barlow" para ficar idêntico ao design; sem ela o sistema usa Segoe UI/SansSerif.

Login: MainGeeks abre o LoginController. Usuários de EXEMPLO (LoginModel, senha guardada como hash SHA-256): atendente / geeks123 e gerente / gerente123.
Faturamento: tela exclusiva do gerente (PainelFaturamento). Os valores são de EXEMPLO (FaturamentoModel), só para a interface; ainda sem backend.
