package geeks.view;

import geeks.model.Cobranca;
import geeks.model.PreviaEncerramento;
import geeks.model.Sessao;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Confirmação de encerramento: quem, onde, quanto tempo, o detalhamento (hora grátis do Clube,
 * crédito usado, entrada já paga, crédito gerado), a opção de usar o crédito do cliente, a forma
 * de pagamento e o total a pagar (que "conta" até o valor final).
 */
public class FormEncerrarSessao extends FormularioGeeks {
    private final JLabel lbTotal = Componentes.rotulo("R$ 0,00", Font.BOLD | Font.ITALIC, 38, Tema.AMARELO);
    private final JPanel detalhes = Componentes.transparente(new GridLayout(0, 1, 0, 4));
    private final CaixaMarcacao chkCredito = new CaixaMarcacao("Usar crédito do cliente", true);
    private final SeletorOpcoes forma;
    private final Function<Boolean, PreviaEncerramento> previa;
    private PreviaEncerramento atual;

    /** @param previa calcula o encerramento agora (true = usando o crédito do cliente) */
    public FormEncerrarSessao(Sessao s, List<String> formas, Function<Boolean, PreviaEncerramento> previa) {
        super("Encerrar sessão", "Encerrar sessão", Icone.Tipo.RAIO);
        this.previa = previa;
        this.atual = previa.apply(false);
        this.forma = new SeletorOpcoes(formas, 0);

        CartaoGeeks cartao = new CartaoGeeks(Tema.CAMPO, new Color(0x333333), false);
        cartao.setLayout(new BorderLayout(0, 16));
        cartao.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel topo = Componentes.transparente(new BorderLayout(14, 0));
        topo.add(new Componentes.BlocoIcone(s.getDispositivo().getTipo(), 56), BorderLayout.WEST);
        JPanel nomes = Componentes.transparente(new GridLayout(2, 1, 0, 6));
        nomes.add(Componentes.rotulo(s.getCliente().getNome(), Font.BOLD, 22, Tema.BRANCO));
        JPanel linhaChip = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaChip.add(new Componentes.Chip(s.getDispositivo().getNome() + (s.getPrevisto() != null
                ? "  |  tempo fechado" : "  |  tempo livre"), Tema.corDe(s.getDispositivo().getTipo()),
                Tema.iconeDe(s.getDispositivo().getTipo())));
        nomes.add(linhaChip);
        topo.add(nomes, BorderLayout.CENTER);

        JPanel estatisticas = Componentes.transparente(new GridLayout(1, 2, 16, 0));
        estatisticas.add(estatistica("Tempo de uso", Componentes.rotulo(Formatador.duracao(s.getDuracao()),
                Font.BOLD | Font.ITALIC, 38, Tema.BRANCO)));
        estatisticas.add(estatistica("Total a pagar", lbTotal));

        JPanel meio = Componentes.transparente(new BorderLayout(0, 12));
        meio.add(estatisticas, BorderLayout.NORTH);
        meio.add(detalhes, BorderLayout.CENTER);
        cartao.add(topo, BorderLayout.NORTH);
        cartao.add(meio, BorderLayout.CENTER);

        JPanel opcoes = Componentes.transparente(new BorderLayout(0, 12));
        if (atual.getSaldoCreditoMinutos() > 0) {
            chkCredito.setText("Usar crédito do cliente (" + atual.getSaldoCreditoMinutos() + " min disponíveis)");
            chkCredito.addActionListener(e -> recalcular());
            JPanel linha = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
            linha.add(chkCredito);
            opcoes.add(linha, BorderLayout.NORTH);
        }
        opcoes.add(bloco("Forma de pagamento", Icone.Tipo.MOEDA, forma), BorderLayout.CENTER);

        JPanel tudo = Componentes.transparente(new BorderLayout(0, 16));
        tudo.add(cartao, BorderLayout.CENTER);
        tudo.add(opcoes, BorderLayout.SOUTH);
        tudo.setPreferredSize(new Dimension(560, tudo.getPreferredSize().height));
        getCorpo().add(tudo, BorderLayout.CENTER);
        mostrar();
    }

    private void recalcular() {
        atual = previa.apply(chkCredito.isSelected());
        mostrar();
        lbTotal.setText(Formatador.moeda(atual.getAPagar()));
    }

    private void mostrar() {
        Cobranca c = atual.getCobranca();
        detalhes.removeAll();
        if (c.getSegundosGratis() > 0) {
            detalhes.add(item("Hora grátis do Clube: " + Formatador.duracao(Duration.ofSeconds(c.getSegundosGratis())), Icone.Tipo.COROA));
        }
        if (c.getSegundosCredito() > 0) {
            detalhes.add(item("Crédito usado: " + c.getMinutosCreditoUsados() + " min", Icone.Tipo.ESTRELA));
        }
        if (atual.getEntradaPaga().signum() > 0) {
            detalhes.add(item("Valor da sessão: " + Formatador.moeda(c.getValor()) + "  -  entrada já paga na reserva: "
                    + Formatador.moeda(atual.getEntradaPaga()), Icone.Tipo.CALENDARIO));
        }
        if (c.getMinutosCredito() > 0) {
            detalhes.add(item("Sobra de crédito para o cliente: " + c.getMinutosCredito() + " min (vale 3 meses)", Icone.Tipo.RELOGIO));
        }
        if (detalhes.getComponentCount() == 0) {
            detalhes.add(item("Sem hora grátis, crédito ou entrada nesta sessão.", Icone.Tipo.INFO));
        }
        boolean cobra = atual.getAPagar().signum() > 0;
        forma.setEnabled(cobra);
        setResumo(cobra ? "Ao confirmar, a sessão é finalizada e o pagamento é registrado."
                : "Nada a pagar. Ao confirmar, a sessão é finalizada e vai para o histórico.");
        detalhes.revalidate();
        detalhes.repaint();
    }

    private JLabel item(String texto, Icone.Tipo icone) {
        return Componentes.rotuloIcone(texto, icone, Tema.CINZA, 14);
    }

    private JPanel estatistica(String titulo, JLabel valor) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 2));
        p.add(Componentes.rotulo(titulo, Font.PLAIN, 14, Tema.CINZA), BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    /** Forma escolhida (null quando não há nada a pagar). */
    public String getFormaPagamento() {
        return atual.getAPagar().signum() > 0 ? forma.getSelecionada() : null;
    }

    public boolean isUsarCredito() {
        return chkCredito.isSelected();
    }

    /** Horário de fim do cálculo mostrado (o encerramento grava este mesmo horário). */
    public java.time.LocalDateTime getFimMostrado() {
        return atual.getFim();
    }

    @Override
    public void animarEntrada() {
        super.animarEntrada();
        BigDecimal total = atual.getAPagar();
        Animacao.executar(800, 180, p -> {
            BigDecimal v = total.multiply(BigDecimal.valueOf(Animacao.suave(p))).setScale(2, RoundingMode.HALF_UP);
            lbTotal.setText(Formatador.moeda(v));
        }, () -> lbTotal.setText(Formatador.moeda(atual.getAPagar())));
    }
}
