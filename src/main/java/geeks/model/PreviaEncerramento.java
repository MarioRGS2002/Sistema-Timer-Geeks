package geeks.model;

import java.math.BigDecimal;

/** Resumo do encerramento de uma sessão: a cobrança, o crédito do cliente e a entrada já paga. */
public class PreviaEncerramento {
    private final Cobranca cobranca;
    private final int saldoCreditoMinutos;
    private final BigDecimal entradaPaga;
    private final BigDecimal aPagar;

    public PreviaEncerramento(Cobranca cobranca, int saldoCreditoMinutos, BigDecimal entradaPaga) {
        this.cobranca = cobranca;
        this.saldoCreditoMinutos = saldoCreditoMinutos;
        this.entradaPaga = entradaPaga;
        BigDecimal resto = cobranca.getValor().subtract(entradaPaga);
        this.aPagar = resto.signum() > 0 ? resto : BigDecimal.ZERO.setScale(2);
    }

    public Cobranca getCobranca() {
        return cobranca;
    }

    /** Crédito do cliente para este tipo de equipamento ANTES do encerramento, em minutos. */
    public int getSaldoCreditoMinutos() {
        return saldoCreditoMinutos;
    }

    /** Entrada paga na reserva que virou esta sessão (0 se não veio de reserva). */
    public BigDecimal getEntradaPaga() {
        return entradaPaga;
    }

    /** Valor total da sessão menos a entrada já paga (nunca negativo). */
    public BigDecimal getAPagar() {
        return aPagar;
    }
}
