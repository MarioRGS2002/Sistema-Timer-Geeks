package geeks.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Linhas da tela Caixa (o que a tela precisa saber, sem depender da camada de dados). */
public final class LancamentoCaixa {
    private LancamentoCaixa() {
    }

    /** Um pagamento recebido no dia. */
    public record Pagamento(int idPagamento, LocalDateTime quando, BigDecimal valor, String tipo, String forma,
                            String funcionario, String cliente, BigDecimal estornado) {
        /** Quanto ainda pode ser estornado. */
        public BigDecimal restante() {
            return valor.subtract(estornado);
        }
    }

    /** Um estorno feito no dia. */
    public record Estorno(int idEstorno, int idPagamento, LocalDateTime quando, BigDecimal valor, String motivo,
                          String funcionario, String forma) {
    }
}
