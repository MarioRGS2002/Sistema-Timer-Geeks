package geeks.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cálculo do valor de uma sessão (regra da aplicação; o banco só guarda o resultado).
 *
 * <ul>
 *   <li>Hora grátis do Clube: abate primeiro o tempo usado (até o que sobrou no dia).</li>
 *   <li>Crédito de tempo (se o cliente escolher usar): abate o tempo usado que sobrou.</li>
 *   <li>Tempo ABERTO: o tempo pago tem mínimo de 1 hora. Quem usa menos paga 1 hora e
 *       a diferença vira crédito de tempo.</li>
 *   <li>Tempo FECHADO: paga o tempo contratado (horario_previsto - inicio) menos a parte
 *       grátis e o crédito usado; o que foi pago e não usado vira crédito.</li>
 *   <li>Se todo o tempo usado foi grátis/crédito, nada é cobrado (sem mínimo).</li>
 *   <li>PREMISSA A CONFIRMAR COM A LOJA: depois da 1ª hora, o ABERTO é cobrado
 *       proporcionalmente ao tempo usado (por segundo, arredondado ao centavo).</li>
 * </ul>
 */
public final class Cobranca {
    public static final int SEGUNDOS_HORA = 3600;

    private final long segundosUsados;
    private final int segundosGratis;
    private final int segundosCredito;
    private final long segundosCobrados;
    private final int minutosCredito;
    private final BigDecimal valor;

    private Cobranca(long usados, int gratis, int credito, long cobrados, int minutosCredito, BigDecimal valor) {
        this.segundosUsados = usados;
        this.segundosGratis = gratis;
        this.segundosCredito = credito;
        this.segundosCobrados = cobrados;
        this.minutosCredito = minutosCredito;
        this.valor = valor;
    }

    /** Sem crédito de tempo. */
    public static Cobranca calcular(TipoTempo tipo, LocalDateTime inicio, LocalDateTime previsto, LocalDateTime fim,
                                    BigDecimal valorHora, int gratisDisponivel) {
        return calcular(tipo, inicio, previsto, fim, valorHora, gratisDisponivel, 0);
    }

    /**
     * @param gratisDisponivel  segundos de hora grátis do Clube que o cliente ainda tem no dia (0 a 3600)
     * @param creditoDisponivel segundos de crédito de tempo que o cliente quer usar (0 = não usar)
     */
    public static Cobranca calcular(TipoTempo tipo, LocalDateTime inicio, LocalDateTime previsto, LocalDateTime fim,
                                    BigDecimal valorHora, int gratisDisponivel, int creditoDisponivel) {
        long usados = Math.max(0, Duration.between(inicio, fim).getSeconds());
        int gratis = (int) Math.min(Math.max(0, Math.min(gratisDisponivel, SEGUNDOS_HORA)), usados);
        int credito = (int) Math.min(Math.max(0, creditoDisponivel), usados - gratis);
        long pagoUsado = usados - gratis - credito;
        long cobrados;
        if (pagoUsado == 0) {
            cobrados = 0;
        } else if (tipo == TipoTempo.ABERTO) {
            cobrados = Math.max(pagoUsado, SEGUNDOS_HORA);
        } else {
            long contratado = previsto == null ? usados : Math.max(0, Duration.between(inicio, previsto).getSeconds());
            cobrados = Math.max(pagoUsado, contratado - gratis - credito);
        }
        int creditoGerado = (int) ((cobrados - pagoUsado) / 60);
        BigDecimal v = valorHora.multiply(BigDecimal.valueOf(cobrados))
                .divide(BigDecimal.valueOf(SEGUNDOS_HORA), 2, RoundingMode.HALF_UP);
        return new Cobranca(usados, gratis, credito, cobrados, creditoGerado, v);
    }

    public long getSegundosUsados() {
        return segundosUsados;
    }

    public int getSegundosGratis() {
        return segundosGratis;
    }

    /** Crédito de tempo usado nesta sessão, em segundos. */
    public int getSegundosCredito() {
        return segundosCredito;
    }

    /** Minutos de crédito consumidos do saldo (arredondado para cima). */
    public int getMinutosCreditoUsados() {
        return (segundosCredito + 59) / 60;
    }

    public long getSegundosCobrados() {
        return segundosCobrados;
    }

    /** Crédito novo gerado (tempo pago e não usado), em minutos. */
    public int getMinutosCredito() {
        return minutosCredito;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
