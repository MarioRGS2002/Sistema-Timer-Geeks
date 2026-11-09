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
Fonte opcional: instale "Barlow" para ficar idêntico ao design; sem ela o sistema usa Segoe UI/SansSerif.
