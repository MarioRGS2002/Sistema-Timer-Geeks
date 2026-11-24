package geeks.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/** Sessão de uso de um equipamento (tabela sessao + participante_sessao). */
public class Sessao {
    private final int idSessao;
    private final Cliente cliente;
    private final Dispositivo dispositivo;
    private final LocalDateTime inicio;
    private final LocalDateTime previsto;
    private final LocalDateTime fim;
    private final TipoTempo tipoTempo;
    private final BigDecimal valorHora;
    private final int gratisDisponivel;
    private final BigDecimal valorTotal;
    private final BigDecimal entradaPaga;

    /**
     * @param gratisDisponivel segundos de hora grátis do Clube ainda disponíveis no dia (sessão ativa)
     * @param valorTotal       valor_total_devido gravado (sessão encerrada), ou null
     */
    public Sessao(int idSessao, Cliente cliente, Dispositivo dispositivo, LocalDateTime inicio, LocalDateTime previsto,
                  LocalDateTime fim, TipoTempo tipoTempo, BigDecimal valorHora, int gratisDisponivel, BigDecimal valorTotal) {
        this.idSessao = idSessao;
        this.cliente = cliente;
        this.dispositivo = dispositivo;
        this.inicio = inicio;
        this.previsto = previsto;
        this.fim = fim;
        this.tipoTempo = tipoTempo;
        this.valorHora = valorHora;
        this.gratisDisponivel = gratisDisponivel;
        this.valorTotal = valorTotal;
        this.entradaPaga = BigDecimal.ZERO;
    }

    /** @param entradaPaga entrada já paga na reserva que virou esta sessão (descontada do "a pagar") */
    public Sessao(int idSessao, Cliente cliente, Dispositivo dispositivo, LocalDateTime inicio, LocalDateTime previsto,
                  LocalDateTime fim, TipoTempo tipoTempo, BigDecimal valorHora, int gratisDisponivel, BigDecimal valorTotal,
                  BigDecimal entradaPaga) {
        this.idSessao = idSessao;
        this.cliente = cliente;
        this.dispositivo = dispositivo;
        this.inicio = inicio;
        this.previsto = previsto;
        this.fim = fim;
        this.tipoTempo = tipoTempo;
        this.valorHora = valorHora;
        this.gratisDisponivel = gratisDisponivel;
        this.valorTotal = valorTotal;
        this.entradaPaga = entradaPaga == null ? BigDecimal.ZERO : entradaPaga;
    }

    /**
     * Quanto falta pagar AGORA na sessão ativa: o valor até agora menos a entrada da reserva
     * (é o que o cartão da sessão mostra e o que aparece ao encerrar sem usar crédito).
     */
    public BigDecimal getAPagar() {
        BigDecimal resto = getValor().subtract(isAtiva() ? entradaPaga : BigDecimal.ZERO);
        return resto.signum() > 0 ? resto : BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal getEntradaPaga() {
        return entradaPaga;
    }

    public boolean isAtiva() {
        return fim == null;
    }

    public Duration getDuracao() {
        return Duration.between(inicio, fim != null ? fim : LocalDateTime.now());
    }

    /** Valor a pagar: o gravado no banco (encerrada) ou o calculado até agora (ativa). */
    public BigDecimal getValor() {
        if (fim != null && valorTotal != null) {
            return valorTotal;
        }
        return calcularCobranca(fim != null ? fim : LocalDateTime.now()).getValor();
    }

    public Cobranca calcularCobranca(LocalDateTime momentoFim) {
        return Cobranca.calcular(tipoTempo, inicio, previsto, momentoFim, valorHora, gratisDisponivel);
    }

    /** Identificador usado pela interface ("S" + id_sessao). */
    public String getId() {
        return "S" + idSessao;
    }

    public int getIdSessao() {
        return idSessao;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Dispositivo getDispositivo() {
        return dispositivo;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getPrevisto() {
        return previsto;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public TipoTempo getTipoTempo() {
        return tipoTempo;
    }

    public BigDecimal getValorHora() {
        return valorHora;
    }
}
