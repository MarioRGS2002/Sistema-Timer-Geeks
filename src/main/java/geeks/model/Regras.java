package geeks.model;

import java.math.BigDecimal;

/**
 * Constantes de regra de negócio que NÃO ficam no banco. Podem ser trocadas ao iniciar o
 * sistema com -Dgeeks.janelaMinutos=... etc. Valores padrão a confirmar com a loja.
 */
public final class Regras {
    /** Janela de folga J entre reservas e sessões, em minutos. */
    public static final int JANELA_MINUTOS = Integer.getInteger("geeks.janelaMinutos", 60);
    /** Aviso de reserva próxima, em minutos antes do início. */
    public static final int ALERTA_RESERVA_MINUTOS = Integer.getInteger("geeks.alertaReservaMinutos", 10);
    /** Sessão de tempo livre com mais horas que isto gera aviso ("o cliente ainda está aí?"). */
    public static final int SESSAO_LONGA_HORAS = Integer.getInteger("geeks.sessaoLongaHoras", 6);
    /** Encerrar com horário de saída mais antigo que isto (minutos) é correção: só o gerente. */
    public static final int TOLERANCIA_CORRECAO_MINUTOS = 15;
    /** Duração da reserva quando a tela informa só o horário de início (a tela não pede o fim). */
    public static final int DURACAO_RESERVA_MINUTOS = Integer.getInteger("geeks.duracaoReservaMinutos", 60);
    /** Entrada mínima da reserva (% do valor previsto). */
    public static final BigDecimal PERCENTUAL_ENTRADA = new BigDecimal(System.getProperty("geeks.percentualEntrada", "50.00"));
    /**
     * Forma de pagamento usada enquanto a tela não pergunta a forma (decisão provisória
     * registrada nas pendências do front).
     */
    public static final String FORMA_PAGAMENTO_PADRAO = System.getProperty("geeks.formaPagamentoPadrao", "Dinheiro");

    private Regras() {
    }
}
