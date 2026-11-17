package geeks.view;

import geeks.model.Sessao;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.List;

public class PainelSessoes extends PainelPagina {
    private static final double REFERENCIA_BARRA_MIN = 180; // só efeito visual: barra cheia = 3h

    private final JPanel grade = Componentes.transparente(new GridLayout(0, 3, 20, 20));
    private final Componentes.Pilula pilulaAtivas = new Componentes.Pilula(Icone.Tipo.CONTROLE, "0 ativas");
    private final Componentes.Pilula pilulaFaturamento = new Componentes.Pilula(Icone.Tipo.MOEDA, "R$ 0,00 hoje");
    private ActionListener listenerEncerrar;
    private ActionListener listenerNova;

    public PainelSessoes() {
        super("Sessões ativas");
        JPanel pilulas = Componentes.transparente(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pilulas.add(pilulaAtivas);
        pilulas.add(pilulaFaturamento);
        setLadoDireito(pilulas);
        JPanel envoltorio = Componentes.transparente(new BorderLayout());
        envoltorio.add(grade, BorderLayout.NORTH);
        getCorpo().add(rolagem(envoltorio), BorderLayout.CENTER);
    }

    public void atualizarSessoes(List<Sessao> sessoes) {
        grade.removeAll();
        for (Sessao s : sessoes) {
            grade.add(criarCartao(s));
        }
        grade.add(criarCartaoNova());
        grade.revalidate();
        grade.repaint();
    }

    public void atualizarResumo(int ativas, BigDecimal faturamentoHoje) {
        pilulaAtivas.setText(ativas + (ativas == 1 ? " ativa" : " ativas"));
        pilulaFaturamento.setText(Formatador.moeda(faturamentoHoje) + " hoje");
    }

    private JComponent criarCartao(Sessao s) {
        CartaoGeeks cartao = new CartaoGeeks();
        cartao.setLayout(new BorderLayout(0, 10));
        cartao.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel topo = Componentes.transparente(new BorderLayout(12, 0));
        topo.add(new Componentes.BlocoIcone(s.getDispositivo().getTipo(), 52), BorderLayout.WEST);
        JPanel nomes = Componentes.transparente(new GridLayout(2, 1, 0, 4));
        nomes.add(Componentes.rotulo(s.getCliente().getNome(), Font.BOLD, 18, Tema.BRANCO));
        JPanel linhaChip = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaChip.add(new Componentes.Chip(s.getDispositivo().getNome(), Tema.corDe(s.getDispositivo().getTipo()),
                Tema.iconeDe(s.getDispositivo().getTipo())));
        nomes.add(linhaChip);
        topo.add(nomes, BorderLayout.CENTER);

        JPanel meio = Componentes.transparente(new GridLayout(2, 1, 0, 8));
        meio.add(new Componentes.Barra(s.getDuracao().toMinutes() / REFERENCIA_BARRA_MIN));
        JPanel valores = Componentes.transparente(new BorderLayout());
        valores.add(Componentes.rotuloIcone(Formatador.duracao(s.getDuracao()), Icone.Tipo.RELOGIO, Tema.CINZA, 15), BorderLayout.WEST);
        valores.add(Componentes.rotuloIcone(Formatador.moeda(s.getValor()), Icone.Tipo.MOEDA, Tema.AMARELO, 15), BorderLayout.EAST);
        meio.add(valores);

        BotaoGeeks encerrar = new BotaoGeeks("Encerrar sessão", Icone.Tipo.RAIO, BotaoGeeks.Estilo.CONTORNO);
        encerrar.setActionCommand(s.getId());
        if (listenerEncerrar != null) {
            encerrar.addActionListener(listenerEncerrar);
        }

        cartao.add(topo, BorderLayout.NORTH);
        cartao.add(meio, BorderLayout.CENTER);
        cartao.add(encerrar, BorderLayout.SOUTH);
        return cartao;
    }

    private JComponent criarCartaoNova() {
        CartaoGeeks cartao = new CartaoGeeks(Tema.CARTAO, Tema.AMARELO, true);
        cartao.setLayout(new java.awt.GridBagLayout());
        BotaoGeeks nova = new BotaoGeeks("Iniciar nova sessão", Icone.Tipo.MAIS, BotaoGeeks.Estilo.PREENCHIDO);
        if (listenerNova != null) {
            nova.addActionListener(listenerNova);
        }
        cartao.add(nova);
        return cartao;
    }

    public void addListenerNovaSessao(ActionListener listener) {
        this.listenerNova = listener;
    }

    public void addListenerEncerrarSessao(ActionListener listener) {
        this.listenerEncerrar = listener;
    }
}
