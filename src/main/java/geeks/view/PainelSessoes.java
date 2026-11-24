package geeks.view;

import geeks.model.Sessao;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PainelSessoes extends PainelPagina {
    private static final double REFERENCIA_BARRA_MIN = 180; // só efeito visual: barra cheia = 3h
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final JPanel grade = Componentes.transparente(new GridLayout(0, 3, 20, 20));
    private final Componentes.Pilula pilulaAtivas = new Componentes.Pilula(Icone.Tipo.CONTROLE, "0 ativas");
    private final Componentes.Pilula pilulaFaturamento = new Componentes.Pilula(Icone.Tipo.MOEDA, "R$ 0,00 hoje");
    private final Set<String> idsAnimados = new HashSet<>();
    private final Map<String, CartaoGeeks> cartoesPorId = new HashMap<>();
    private List<Sessao> ultimas = new ArrayList<>();
    private BigDecimal valorExibido;
    private int ativasAnterior = -1;
    private Timer timerValor;
    private ActionListener listenerEncerrar;
    private ActionListener listenerNova;
    private ActionListener listenerMaisTempo;
    private ActionListener listenerJogador;

    /** O actionCommand do evento é o id da sessão. Só aparece quando o equipamento aceita mais jogadores. */
    public void addListenerAdicionarJogador(ActionListener listener) {
        this.listenerJogador = listener;
    }

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
        this.ultimas = new ArrayList<>(sessoes);
        reconstruir();
    }

    /** Ao entrar na tela, os cartões surgem em sequência e as barras crescem de novo. */
    @Override
    public void animarEntrada() {
        super.animarEntrada();
        idsAnimados.clear();
        reconstruir();
    }

    private void reconstruir() {
        grade.removeAll();
        cartoesPorId.clear();
        int novos = 0;
        for (Sessao s : ultimas) {
            boolean novo = idsAnimados.add(s.getId());
            CartaoGeeks c = criarCartao(s, novo ? novos * 60 : -1);
            if (novo) {
                c.entrar(novos * 60);
                novos++;
            }
            cartoesPorId.put(s.getId(), c);
            grade.add(c);
        }
        grade.add(criarCartaoNova());
        grade.revalidate();
        grade.repaint();
    }

    /** A pílula "R$ X hoje" só aparece para o gerente. */
    public void setMostrarFaturamento(boolean mostrar) {
        pilulaFaturamento.setVisible(mostrar);
    }

    public void atualizarResumo(int ativas, BigDecimal faturamentoHoje) {
        pilulaAtivas.setText(ativas + (ativas == 1 ? " ativa" : " ativas"));
        if (ativasAnterior >= 0 && ativasAnterior != ativas) {
            pilulaAtivas.piscar();
        }
        ativasAnterior = ativas;

        final BigDecimal de = valorExibido == null ? faturamentoHoje : valorExibido;
        valorExibido = faturamentoHoje;
        if (timerValor != null) {
            timerValor.stop();
        }
        if (de.compareTo(faturamentoHoje) == 0) {
            pilulaFaturamento.setText(Formatador.moeda(faturamentoHoje) + " hoje");
            return;
        }
        if (faturamentoHoje.subtract(de).abs().compareTo(BigDecimal.ONE) >= 0) {
            pilulaFaturamento.piscar();
        }
        // o valor "conta" até o novo total
        timerValor = Animacao.executar(650, 0, p -> {
            BigDecimal v = de.add(faturamentoHoje.subtract(de).multiply(BigDecimal.valueOf(Animacao.suave(p))))
                    .setScale(2, RoundingMode.HALF_UP);
            pilulaFaturamento.setText(Formatador.moeda(v) + " hoje");
        }, () -> pilulaFaturamento.setText(Formatador.moeda(faturamentoHoje) + " hoje"));
    }

    /** O cartão da sessão encerrada some com animação e só depois a ação é executada. */
    public void animarSaida(String idSessao, Runnable depois) {
        CartaoGeeks c = cartoesPorId.get(idSessao);
        if (c == null) {
            depois.run();
        } else {
            c.sair(depois);
        }
    }

    private CartaoGeeks criarCartao(Sessao s, int atrasoAnimacao) {
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
        if (!s.getJogadores().isEmpty()) {
            javax.swing.JLabel mais = Componentes.rotulo("  +" + s.getJogadores().size(), Font.BOLD, 13, Tema.AMARELO);
            mais.setToolTipText("Jogando junto: " + String.join(", ", s.getJogadores()));
            linhaChip.add(mais);
        }
        if (s.cabeMaisJogador()) {
            BotaoGeeks jogador = new BotaoGeeks("+ Jogador", null, BotaoGeeks.Estilo.LINK);
            jogador.setActionCommand(s.getId());
            jogador.setToolTipText("Acrescentar um amigo jogando junto neste equipamento");
            if (listenerJogador != null) {
                jogador.addActionListener(listenerJogador);
            }
            linhaChip.add(jogador);
        }
        nomes.add(linhaChip);
        topo.add(nomes, BorderLayout.CENTER);

        long minutos = s.getDuracao().toMinutes();
        int atrasoBarra = atrasoAnimacao < 0 ? -1 : atrasoAnimacao + 150;
        Componentes.Barra barra;
        String linhaTempo;
        Color corLinha;
        if (s.getPrevisto() != null) {
            // Tempo fechado: a barra enche até o horário combinado; laranja nos últimos 10 min, vermelha ao esgotar
            long contratado = Math.max(1, java.time.Duration.between(s.getInicio(), s.getPrevisto()).toMinutes());
            long falta = java.time.Duration.between(java.time.LocalDateTime.now(), s.getPrevisto()).toMinutes();
            double fracao = Math.min(1.0, (double) minutos / contratado);
            if (falta < 0) {
                barra = new Componentes.Barra(1, atrasoBarra, Tema.VERMELHO, new Color(0xFF2D55));
                linhaTempo = "Tempo esgotado às " + s.getPrevisto().format(HORA) + "  ·  há " + (-falta) + " min";
                corLinha = Tema.VERMELHO;
            } else if (falta <= 10) {
                barra = new Componentes.Barra(fracao, atrasoBarra, new Color(0xFFB800), new Color(0xFF8A00));
                linhaTempo = "Até " + s.getPrevisto().format(HORA) + "  ·  faltam " + falta + " min";
                corLinha = new Color(0xFFB800);
            } else {
                barra = new Componentes.Barra(fracao, atrasoBarra);
                linhaTempo = "Até " + s.getPrevisto().format(HORA) + "  ·  faltam " + Formatador.duracao(java.time.Duration.ofMinutes(falta));
                corLinha = new Color(0x8A8A8A);
            }
        } else {
            // Tempo livre: a barra muda de cor conforme a sessão fica longa (2h laranja, 3h vermelho)
            if (minutos >= 180) {
                barra = new Componentes.Barra(1, atrasoBarra, Tema.VERMELHO, new Color(0xFF2D55));
            } else if (minutos >= 120) {
                barra = new Componentes.Barra(minutos / REFERENCIA_BARRA_MIN, atrasoBarra, new Color(0xFFB800), new Color(0xFF8A00));
            } else {
                barra = new Componentes.Barra(minutos / REFERENCIA_BARRA_MIN, atrasoBarra);
            }
            linhaTempo = "Tempo livre desde " + s.getInicio().format(HORA) + (minutos >= 120 ? "  ·  sessão longa" : "");
            corLinha = minutos >= 120 ? new Color(0xFFB800) : new Color(0x8A8A8A);
        }
        JPanel meio = Componentes.transparente(new GridLayout(3, 1, 0, 6));
        meio.add(Componentes.rotulo(linhaTempo, Font.PLAIN, 13, corLinha));
        meio.add(barra);
        JPanel valores = Componentes.transparente(new BorderLayout());
        valores.add(Componentes.rotuloIcone(Formatador.duracao(s.getDuracao()), Icone.Tipo.RELOGIO, Tema.CINZA, 15), BorderLayout.WEST);
        // valor que falta pagar agora (já descontada a entrada da reserva, se veio de uma)
        javax.swing.JLabel valor = Componentes.rotuloIcone(Formatador.moeda(s.getAPagar()), Icone.Tipo.MOEDA, Tema.AMARELO, 15);
        if (s.getEntradaPaga().signum() > 0) {
            valor.setToolTipText("Entrada de " + Formatador.moeda(s.getEntradaPaga()) + " já paga na reserva");
        }
        valores.add(valor, BorderLayout.EAST);
        meio.add(valores);

        BotaoGeeks encerrar = new BotaoGeeks("Encerrar sessão", Icone.Tipo.RAIO, BotaoGeeks.Estilo.CONTORNO);
        encerrar.setActionCommand(s.getId());
        if (listenerEncerrar != null) {
            encerrar.addActionListener(listenerEncerrar);
        }
        JComponent rodape = encerrar;
        if (s.getPrevisto() != null) {
            // tempo fechado: o cliente pode pedir mais tempo sem encerrar e abrir outra sessão
            BotaoGeeks mais = new BotaoGeeks("Mais tempo", Icone.Tipo.MAIS, BotaoGeeks.Estilo.ESCURO);
            mais.setActionCommand(s.getId());
            if (listenerMaisTempo != null) {
                mais.addActionListener(listenerMaisTempo);
            }
            encerrar.setText("Encerrar");
            JPanel dois = Componentes.transparente(new GridLayout(1, 2, 8, 0));
            dois.add(mais);
            dois.add(encerrar);
            rodape = dois;
        }

        cartao.add(topo, BorderLayout.NORTH);
        cartao.add(meio, BorderLayout.CENTER);
        cartao.add(rodape, BorderLayout.SOUTH);
        cartao.ativarHover();
        return cartao;
    }

    private JComponent criarCartaoNova() {
        CartaoGeeks cartao = new CartaoGeeks(Tema.CARTAO, Tema.AMARELO, true) {
            @Override
            public java.awt.Dimension getPreferredSize() {
                // sem sessões ativas, o cartão não fica achatado: mesma altura de um cartão de sessão
                java.awt.Dimension d = super.getPreferredSize();
                return new java.awt.Dimension(d.width, Math.max(d.height, 200));
            }
        };
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

    /** O actionCommand do evento é o id da sessão ("S" + número). Só aparece em sessões de tempo fechado. */
    public void addListenerMaisTempo(ActionListener listener) {
        this.listenerMaisTempo = listener;
    }
}
