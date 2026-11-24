package geeks.view;

import geeks.model.PeriodoFaturamento;
import geeks.model.SerieFaturamento;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

/**
 * Tela de Faturamento (visível só para o gerente): resumo do período e gráfico de renda
 * com filtro por dia, semana ou mês e navegação entre períodos.
 */
public class PainelFaturamento extends PainelPagina {
    private final SegmentoPeriodo segmento = new SegmentoPeriodo();
    private final CartaoResumo cartaoTotal = new CartaoResumo("Total do período", Icone.Tipo.MOEDA);
    private final CartaoResumo cartaoMedia = new CartaoResumo("Média por dia", Icone.Tipo.CALENDARIO);
    private final CartaoResumo cartaoMelhor = new CartaoResumo("Melhor dia", Icone.Tipo.TROFEU);
    private final GraficoBarras grafico = new GraficoBarras();
    private final JLabel lbPeriodo = Componentes.rotulo(" ", Font.BOLD, 22, Tema.BRANCO);
    private final BotaoGeeks btAnterior = new BotaoGeeks("<", null, BotaoGeeks.Estilo.CONTORNO);
    private final BotaoGeeks btProximo = new BotaoGeeks(">", null, BotaoGeeks.Estilo.CONTORNO);
    private final BotaoGeeks btHoje = new BotaoGeeks("Hoje", null, BotaoGeeks.Estilo.LINK);
    private SerieFaturamento ultima;
    private final JPanel relatorios = Componentes.transparente(new GridLayout(1, 3, 16, 0));

    public PainelFaturamento() {
        super("Faturamento");
        setLadoDireito(segmento);

        JPanel resumo = Componentes.transparente(new GridLayout(1, 3, 16, 0));
        resumo.setBorder(new EmptyBorder(0, 0, 16, 0));
        resumo.add(cartaoTotal);
        resumo.add(cartaoMedia);
        resumo.add(cartaoMelhor);

        lbPeriodo.setIcon(new Icone(Icone.Tipo.CALENDARIO, 22, Tema.AMARELO));
        lbPeriodo.setIconTextGap(10);
        JPanel botoes = Componentes.transparente(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.add(btHoje);
        botoes.add(btAnterior);
        botoes.add(btProximo);
        JPanel navegacao = Componentes.transparente(new BorderLayout());
        navegacao.add(lbPeriodo, BorderLayout.WEST);
        navegacao.add(botoes, BorderLayout.EAST);

        CartaoGeeks cartaoGrafico = new CartaoGeeks();
        cartaoGrafico.setLayout(new BorderLayout(0, 8));
        cartaoGrafico.setBorder(new EmptyBorder(16, 22, 12, 18));
        cartaoGrafico.add(navegacao, BorderLayout.NORTH);
        cartaoGrafico.add(grafico, BorderLayout.CENTER);

        getCorpo().add(resumo, BorderLayout.NORTH);
        getCorpo().add(cartaoGrafico, BorderLayout.CENTER);
        relatorios.setBorder(new EmptyBorder(16, 0, 0, 0));
        getCorpo().add(relatorios, BorderLayout.SOUTH);
    }

    /** Relatórios do mesmo período do gráfico: equipamentos mais usados, horários de pico e clientes frequentes. */
    public void atualizarRelatorios(geeks.model.RelatoriosModel.Relatorio r) {
        relatorios.removeAll();
        relatorios.add(listaRelatorio("Equipamentos mais usados", Icone.Tipo.CONTROLE, r.equipamentos()));
        relatorios.add(listaRelatorio("Horários de pico", Icone.Tipo.RELOGIO, r.pico()));
        relatorios.add(listaRelatorio("Clientes mais frequentes", Icone.Tipo.USUARIO_MAIS, r.clientes()));
        relatorios.revalidate();
        relatorios.repaint();
    }

    private javax.swing.JComponent listaRelatorio(String titulo, Icone.Tipo icone,
                                                  java.util.List<geeks.model.RelatoriosModel.Linha> linhas) {
        CartaoGeeks c = new CartaoGeeks();
        c.setLayout(new BorderLayout(0, 8));
        c.setBorder(new EmptyBorder(12, 16, 12, 16));
        c.add(Componentes.rotuloIcone(titulo, icone, Tema.AMARELO, 15), BorderLayout.NORTH);
        JPanel l = Componentes.transparente(new java.awt.GridLayout(0, 1, 0, 4));
        if (linhas.isEmpty()) {
            l.add(Componentes.rotulo("Sem sessões neste período.", Font.PLAIN, 13, Tema.CINZA));
        }
        int pos = 1;
        for (geeks.model.RelatoriosModel.Linha linha : linhas) {
            JPanel item = Componentes.transparente(new BorderLayout(8, 0));
            item.add(Componentes.rotulo(pos++ + ". " + linha.rotulo(), Font.BOLD, 14, Tema.BRANCO), BorderLayout.WEST);
            item.add(Componentes.rotulo(linha.detalhe(), Font.PLAIN, 13, Tema.CINZA), BorderLayout.EAST);
            l.add(item);
        }
        c.add(l, BorderLayout.CENTER);
        return c;
    }

    /**
     * @param podeAvancar   false quando o próximo período está no futuro
     * @param periodoAtual  true quando o período mostrado já contém a data de hoje
     */
    public void atualizar(SerieFaturamento serie, boolean podeAvancar, boolean periodoAtual) {
        this.ultima = serie;
        PeriodoFaturamento p = serie.getPeriodo();
        boolean dia = p == PeriodoFaturamento.DIA;
        lbPeriodo.setText(serie.getTitulo());
        btProximo.setEnabled(podeAvancar);
        btHoje.setEnabled(!periodoAtual);

        cartaoTotal.setTitulo(dia ? "Total do dia" : p == PeriodoFaturamento.SEMANA ? "Total da semana" : "Total do mês");
        cartaoTotal.animarValor(serie.getTotal());
        cartaoMedia.setTitulo(dia ? "Média por hora" : "Média por dia");
        cartaoMedia.animarValor(serie.getMedia());
        cartaoMelhor.setTitulo(dia ? "Melhor horário" : "Melhor dia");
        int melhor = serie.getIndiceMelhor();
        if (melhor < 0) {
            cartaoMelhor.definirTexto("-", "");
        } else {
            cartaoMelhor.definirTexto(serie.getRotulosCompletos().get(melhor), Formatador.moeda(serie.getValores().get(melhor)));
        }
        grafico.definir(serie.getRotulos(), serie.getRotulosCompletos(), serie.getValores());
    }

    /** Ao abrir a tela, os números recomeçam a contar e as barras crescem de novo. */
    @Override
    public void animarEntrada() {
        super.animarEntrada();
        if (ultima != null) {
            cartaoTotal.zerar();
            cartaoMedia.zerar();
            atualizar(ultima, btProximo.isEnabled(), !btHoje.isEnabled());
        }
    }

    public PeriodoFaturamento getPeriodo() {
        return segmento.getPeriodo();
    }

    public void addListenerPeriodo(ActionListener l) {
        segmento.addListenerPeriodo(l);
    }

    public void addListenerAnterior(ActionListener l) {
        btAnterior.addActionListener(l);
    }

    public void addListenerProximo(ActionListener l) {
        btProximo.addActionListener(l);
    }

    public void addListenerHoje(ActionListener l) {
        btHoje.addActionListener(l);
    }
}
