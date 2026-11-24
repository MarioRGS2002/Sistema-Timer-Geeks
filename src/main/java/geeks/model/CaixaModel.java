package geeks.model;

import geeks.dados.CaixaDAO;
import geeks.dados.PagamentoDAO;
import geeks.dados.Transacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Fechamento de caixa do dia e estorno de pagamento (somente GERENTE, checado aqui).
 * O total de cada forma já desconta os estornos feitos no dia.
 */
public class CaixaModel {
    private final CaixaDAO caixaDAO = new CaixaDAO();
    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();

    /** Resumo de um dia de caixa. */
    public static final class Dia {
        public final LocalDate dia;
        public final BigDecimal total;
        public final Map<String, BigDecimal> porForma;
        public final List<LancamentoCaixa.Pagamento> pagamentos;
        public final List<LancamentoCaixa.Estorno> estornos;

        Dia(LocalDate dia, BigDecimal total, Map<String, BigDecimal> porForma, List<LancamentoCaixa.Pagamento> pagamentos,
            List<LancamentoCaixa.Estorno> estornos) {
            this.dia = dia;
            this.total = total;
            this.porForma = porForma;
            this.pagamentos = pagamentos;
            this.estornos = estornos;
        }

        public BigDecimal totalEstornos() {
            return estornos.stream().map(LancamentoCaixa.Estorno::valor).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        }
    }

    public Dia dia(LocalDate dia) {
        Contexto.exigirGerente();
        LocalDateTime ini = dia.atStartOfDay();
        LocalDateTime fim = dia.plusDays(1).atStartOfDay();
        return Transacao.consultar(c -> {
            Map<String, BigDecimal> formas = pagamentoDAO.porForma(c, ini, fim);
            formas.remove(null); // hora grátis (valor 0) não é dinheiro no caixa
            List<LancamentoCaixa.Pagamento> pags = caixaDAO.pagamentos(c, ini, fim).stream()
                    .map(p -> new LancamentoCaixa.Pagamento(p.idPagamento, p.quando, p.valor, p.tipo, p.forma,
                            p.funcionario, p.cliente, p.estornado)).toList();
            List<LancamentoCaixa.Estorno> ests = caixaDAO.estornos(c, ini, fim).stream()
                    .map(e -> new LancamentoCaixa.Estorno(e.idEstorno, e.idPagamento, e.quando, e.valor, e.motivo,
                            e.funcionario, e.forma)).toList();
            return new Dia(dia, pagamentoDAO.total(c, ini, fim), formas, pags, ests);
        });
    }

    /**
     * Estorna (devolve) parte ou todo um pagamento lançado errado. Não apaga nem altera o pagamento:
     * grava um estorno com motivo e quem fez, e o faturamento passa a descontá-lo.
     */
    public void estornar(int idPagamento, BigDecimal valor, String motivo) {
        Contexto.exigirGerente();
        String m = motivo == null ? "" : motivo.trim();
        if (m.length() < 3 || m.length() > 200) {
            throw new IllegalArgumentException("Informe o motivo do estorno (de 3 a 200 caracteres).");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Informe um valor maior que zero.");
        }
        BigDecimal v = valor.setScale(2, RoundingMode.HALF_UP);
        int idFuncionario = Contexto.idFuncionario();
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Transacao.executar(c -> {
            BigDecimal[] pago = caixaDAO.travarPagamento(c, idPagamento);
            if (pago == null) {
                throw new IllegalArgumentException("Pagamento não encontrado.");
            }
            BigDecimal restante = pago[0].subtract(pago[1]);
            if (v.compareTo(restante) > 0) {
                throw new IllegalArgumentException("O estorno passa do valor que ainda pode ser estornado ("
                        + restante.toPlainString().replace('.', ',') + ").");
            }
            return caixaDAO.inserirEstorno(c, idPagamento, v, m, agora, idFuncionario);
        });
    }

    /** "12,50" / "12.50" / "R$ 12,50" para BigDecimal (null se inválido). */
    public static BigDecimal lerValor(String txt) {
        if (txt == null) {
            return null;
        }
        String t = txt.replace("R$", "").replace(" ", "").trim();
        if (t.contains(",")) {
            t = t.replace(".", "").replace(',', '.');
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
