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
    private final java.util.function.BiFunction<Boolean, java.time.LocalDateTime, PreviaEncerramento> previa;
    private final CampoGeeks campoSaida = new CampoGeeks("hh:mm");
    private final JLabel lbTempo = Componentes.rotulo(" ", Font.BOLD | Font.ITALIC, 38, Tema.BRANCO);
    private java.time.LocalDateTime saidaReal;
    private final java.time.LocalDateTime inicioSessao;
    private String erroSaida;
    private PreviaEncerramento atual;

    /**
     * @param previa           calcula o encerramento (usar crédito?, saída real ou null = agora)
     * @param podeCorrigirSaida true para o gerente: campo "Saída real" para sessão esquecida aberta
     */
    public FormEncerrarSessao(Sessao s, List<String> formas,
                              java.util.function.BiFunction<Boolean, java.time.LocalDateTime, PreviaEncerramento> previa,
                              boolean podeCorrigirSaida) {
        super("Encerrar sessão", "Encerrar sessão", Icone.Tipo.RAIO);
        this.inicioSessao = s.getInicio();
        this.previa = previa;
        this.atual = previa.apply(false, null);
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
        estatisticas.add(estatistica("Tempo de uso", lbTempo));
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
        if (podeCorrigirSaida) {
            // Sessão esquecida aberta: o gerente informa quando o cliente realmente saiu
            Mascara.HORA.aplicar(campoSaida);
            campoSaida.setPreferredSize(new Dimension(110, 44));
            JPanel linhaSaida = Componentes.transparente(new BorderLayout(12, 0));
            linhaSaida.add(campoSaida, BorderLayout.WEST);
            linhaSaida.add(Componentes.rotulo("<html>Só se o cliente já foi embora e a sessão ficou aberta: a primeira vez "
                    + "que deu essa hora depois do início. Vazio = agora.</html>", Font.PLAIN, 13, Tema.CINZA), BorderLayout.CENTER);
            opcoes.add(bloco("Saída real (gerente)", Icone.Tipo.RELOGIO, linhaSaida), BorderLayout.SOUTH);
            campoSaida.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    javax.swing.SwingUtilities.invokeLater(FormEncerrarSessao.this::lerSaida);
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    javax.swing.SwingUtilities.invokeLater(FormEncerrarSessao.this::lerSaida);
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                }
            });
        }

        // largura fixa, altura acompanha o conteúdo (o detalhamento muda conforme crédito/saída real)
        JPanel tudo = new JPanel(new BorderLayout(0, 16)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(560, super.getPreferredSize().height);
            }
        };
        tudo.setOpaque(false);
        tudo.add(cartao, BorderLayout.CENTER);
        tudo.add(opcoes, BorderLayout.SOUTH);
        getCorpo().add(tudo, BorderLayout.CENTER);
        mostrar();
    }

    /**
     * Lê "hh:mm": a PRIMEIRA vez que deu essa hora depois do início da sessão (ex.: começou 21:47 do
     * dia 22 e "23:00" = 23:00 do dia 22). A data aparece no detalhamento para conferir.
     */
    private void lerSaida() {
        String t = campoSaida.getText();
        erroSaida = null;
        if (t.isEmpty()) {
            saidaReal = null;
        } else if (t.length() < 5) {
            return; // ainda digitando
        } else {
            try {
                java.time.LocalTime hora = java.time.LocalTime.parse(t);
                java.time.LocalDateTime candidato = inicioSessao.toLocalDate().atTime(hora);
                if (candidato.isBefore(inicioSessao)) {
                    candidato = candidato.plusDays(1);
                }
                saidaReal = candidato;
            } catch (java.time.format.DateTimeParseException e) {
                erroSaida = "Horário inválido (use hh:mm, ex.: 22:30).";
            }
        }
        recalcular();
    }

    private void recalcular() {
        if (erroSaida == null) {
            try {
                atual = previa.apply(chkCredito.isSelected(), saidaReal);
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                erroSaida = e.getMessage();
            }
        }
        mostrar();
        lbTotal.setText(Formatador.moeda(atual.getAPagar()));
    }

    private void mostrar() {
        Cobranca c = atual.getCobranca();
        lbTempo.setText(Formatador.duracao(Duration.ofSeconds(c.getSegundosUsados())));
        detalhes.removeAll();
        if (saidaReal != null && erroSaida == null) {
            detalhes.add(item("Saída corrigida para " + saidaReal.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                    + " (fica registrado que foi você)", Icone.Tipo.ALERTA));
        }
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
        setConfirmarHabilitado(erroSaida == null);
        if (erroSaida != null) {
            setResumo(erroSaida);
        } else {
            setResumo(cobra ? "Ao confirmar, a sessão é finalizada e o pagamento é registrado."
                    : "Nada a pagar. Ao confirmar, a sessão é finalizada e vai para o histórico.");
        }
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
