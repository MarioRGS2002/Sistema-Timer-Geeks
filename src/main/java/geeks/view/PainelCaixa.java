package geeks.view;

import geeks.dados.CaixaDAO;
import geeks.model.CaixaModel;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Caixa (só gerente): o que entrou no dia por forma de pagamento (para conferir a gaveta e as
 * maquininhas no fim do expediente), cada pagamento com o botão Estornar e os estornos do dia.
 */
public class PainelCaixa extends PainelPagina {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private final JLabel lbDia = Componentes.rotulo(" ", Font.BOLD, 22, Tema.BRANCO);
    private final BotaoGeeks btAnterior = new BotaoGeeks("<", null, BotaoGeeks.Estilo.CONTORNO);
    private final BotaoGeeks btProximo = new BotaoGeeks(">", null, BotaoGeeks.Estilo.CONTORNO);
    private final BotaoGeeks btHoje = new BotaoGeeks("Hoje", null, BotaoGeeks.Estilo.LINK);
    private final JPanel resumo = Componentes.transparente(new GridLayout(1, 0, 14, 0));
    private final JPanel lista = Componentes.transparente(new GridLayout(0, 1, 0, 10));
    private ActionListener listenerEstornar;

    public PainelCaixa() {
        super("Caixa");
        lbDia.setIcon(new Icone(Icone.Tipo.CALENDARIO, 22, Tema.AMARELO));
        lbDia.setIconTextGap(10);
        JPanel botoes = Componentes.transparente(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.add(btHoje);
        botoes.add(btAnterior);
        botoes.add(btProximo);
        JPanel navegacao = Componentes.transparente(new BorderLayout());
        navegacao.add(lbDia, BorderLayout.WEST);
        navegacao.add(botoes, BorderLayout.EAST);
        navegacao.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel topo = Componentes.transparente(new BorderLayout(0, 16));
        topo.add(navegacao, BorderLayout.NORTH);
        topo.add(resumo, BorderLayout.CENTER);
        topo.setBorder(new EmptyBorder(0, 0, 18, 0));

        JPanel envoltorio = Componentes.transparente(new BorderLayout());
        envoltorio.add(lista, BorderLayout.NORTH);
        getCorpo().add(topo, BorderLayout.NORTH);
        getCorpo().add(rolagem(envoltorio), BorderLayout.CENTER);
    }

    public void atualizar(CaixaModel.Dia dia, boolean podeAvancar) {
        String d = dia.dia.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("pt", "BR")));
        lbDia.setText(Character.toUpperCase(d.charAt(0)) + d.substring(1) + (dia.dia.equals(LocalDate.now()) ? "  (hoje)" : ""));
        btProximo.setEnabled(podeAvancar);

        resumo.removeAll();
        resumo.add(cartao("Total do dia", dia.total, Icone.Tipo.MOEDA, true));
        for (Map.Entry<String, BigDecimal> e : dia.porForma.entrySet()) {
            resumo.add(cartao(e.getKey(), e.getValue(), Icone.Tipo.LISTA, false));
        }
        if (dia.totalEstornos().signum() > 0) {
            resumo.add(cartao("Estornos", dia.totalEstornos().negate(), Icone.Tipo.ALERTA, false));
        }

        lista.removeAll();
        lista.add(Componentes.rotuloIcone("Pagamentos do dia (" + dia.pagamentos.size() + ")", Icone.Tipo.MOEDA, Tema.AMARELO, 17));
        if (dia.pagamentos.isEmpty()) {
            lista.add(Componentes.rotulo("Nenhum pagamento neste dia.", Font.PLAIN, 16, Tema.CINZA));
        }
        for (CaixaDAO.Pagamento p : dia.pagamentos) {
            lista.add(linhaPagamento(p));
        }
        if (!dia.estornos.isEmpty()) {
            lista.add(Componentes.rotuloIcone("Estornos do dia (" + dia.estornos.size() + ")", Icone.Tipo.ALERTA, Tema.VERMELHO, 17));
            for (CaixaDAO.Estorno e : dia.estornos) {
                lista.add(linhaEstorno(e));
            }
        }
        resumo.revalidate();
        lista.revalidate();
        repaint();
    }

    private JComponent cartao(String titulo, BigDecimal valor, Icone.Tipo icone, boolean destaque) {
        CartaoGeeks c = destaque ? new CartaoGeeks(Tema.AMARELO, Tema.AMARELO, false) : new CartaoGeeks();
        c.setLayout(new GridLayout(2, 1, 0, 4));
        c.setBorder(new EmptyBorder(12, 16, 12, 16));
        c.add(Componentes.rotuloIcone(titulo, icone, destaque ? Tema.FUNDO : Tema.CINZA, 14));
        c.add(Componentes.rotulo(Formatador.moeda(valor), Font.BOLD | Font.ITALIC, 24,
                destaque ? Tema.FUNDO : valor.signum() < 0 ? Tema.VERMELHO : Tema.BRANCO));
        return c;
    }

    private JComponent linhaPagamento(CaixaDAO.Pagamento p) {
        CartaoGeeks linha = new CartaoGeeks();
        linha.setLayout(new BorderLayout(16, 0));
        linha.setBorder(new EmptyBorder(10, 16, 10, 16));
        linha.add(Componentes.rotulo(p.quando.format(HORA), Font.BOLD, 18, Tema.BRANCO), BorderLayout.WEST);
        JPanel centro = Componentes.transparente(new GridLayout(1, 3, 12, 0));
        JPanel quem = Componentes.transparente(new GridLayout(2, 1, 0, 2));
        quem.add(Componentes.rotulo(p.cliente == null ? "-" : p.cliente, Font.BOLD, 16, Tema.BRANCO));
        quem.add(Componentes.rotulo(tipo(p.tipo) + "  ·  recebido por " + p.funcionario, Font.PLAIN, 13, Tema.CINZA));
        JPanel forma = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 8));
        forma.add(new Componentes.Chip(p.forma == null ? "-" : p.forma, Tema.AZUL, Icone.Tipo.MOEDA));
        JPanel valores = Componentes.transparente(new GridLayout(2, 1, 0, 2));
        valores.add(Componentes.rotulo(Formatador.moeda(p.valor), Font.BOLD, 17, Tema.AMARELO));
        valores.add(Componentes.rotulo(p.estornado.signum() > 0 ? "estornado " + Formatador.moeda(p.estornado) : " ",
                Font.PLAIN, 13, Tema.VERMELHO));
        centro.add(quem);
        centro.add(forma);
        centro.add(valores);
        linha.add(centro, BorderLayout.CENTER);
        BotaoGeeks estornar = new BotaoGeeks("Estornar", Icone.Tipo.SETA_ESQUERDA, BotaoGeeks.Estilo.LINK);
        estornar.setActionCommand(String.valueOf(p.idPagamento));
        estornar.setEnabled(p.estornado.compareTo(p.valor) < 0);
        if (listenerEstornar != null) {
            estornar.addActionListener(listenerEstornar);
        }
        linha.add(estornar, BorderLayout.EAST);
        linha.ativarHover();
        return linha;
    }

    private JComponent linhaEstorno(CaixaDAO.Estorno e) {
        CartaoGeeks linha = new CartaoGeeks();
        linha.setLayout(new BorderLayout(16, 0));
        linha.setBorder(new EmptyBorder(10, 16, 10, 16));
        linha.add(Componentes.rotulo(e.quando.format(HORA), Font.BOLD, 18, Tema.VERMELHO), BorderLayout.WEST);
        linha.add(Componentes.rotulo("<html>" + e.motivo.replace("<", "&lt;") + "<br><span style='color:#B8B8B8'>por "
                + e.funcionario + (e.forma == null ? "" : "  ·  " + e.forma) + "</span></html>", Font.PLAIN, 15, Tema.BRANCO),
                BorderLayout.CENTER);
        linha.add(Componentes.rotulo("- " + Formatador.moeda(e.valor), Font.BOLD, 17, Tema.VERMELHO), BorderLayout.EAST);
        return linha;
    }

    private static String tipo(String t) {
        return "SESSAO".equals(t) ? "Sessão" : "RESERVA".equals(t) ? "Entrada de reserva" : "ASSINATURA".equals(t) ? "Clube Geeks" : t;
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

    /** O actionCommand do evento é o id do pagamento. */
    public void addListenerEstornar(ActionListener l) {
        this.listenerEstornar = l;
    }
}
