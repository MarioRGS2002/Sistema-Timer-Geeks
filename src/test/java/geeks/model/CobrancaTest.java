package geeks.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regras de cálculo (sem banco): rodam sempre, até com o MySQL desligado. */
class CobrancaTest {
    private static final BigDecimal DEZ = new BigDecimal("10.00");
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 14, 0);

    @Test
    void abertoComMenosDeUmaHoraPagaUmaHoraEGeraCredito() {
        Cobranca c = Cobranca.calcular(TipoTempo.ABERTO, T0, null, T0.plusMinutes(30), DEZ, 0);
        assertEquals(new BigDecimal("10.00"), c.getValor());
        assertEquals(30, c.getMinutosCredito());
    }

    @Test
    void abertaPorEnganoEncerradaEmAteDoisMinutosNaoCobra() {
        Cobranca engano = Cobranca.calcular(TipoTempo.ABERTO, T0, null, T0.plusSeconds(90), DEZ, 3600, 600);
        assertEquals(new BigDecimal("0.00"), engano.getValor());
        assertEquals(0, engano.getSegundosGratis(), "não gasta a hora grátis");
        assertEquals(0, engano.getSegundosCredito(), "não gasta crédito");
        assertEquals(0, engano.getMinutosCredito());
        Cobranca jogou = Cobranca.calcular(TipoTempo.ABERTO, T0, null, T0.plusMinutes(3), DEZ, 0);
        assertEquals(DEZ, jogou.getValor(), "passou da tolerância: mínimo de 1 hora");
    }

    @Test
    void abertoDepoisDaPrimeiraHoraEProporcional() {
        Cobranca c = Cobranca.calcular(TipoTempo.ABERTO, T0, null, T0.plusMinutes(90), DEZ, 0);
        assertEquals(new BigDecimal("15.00"), c.getValor());
        assertEquals(0, c.getMinutosCredito());
    }

    @Test
    void semClubeFechadoDuasHoras() {
        Cobranca c = Cobranca.calcular(TipoTempo.FECHADO, T0, T0.plusMinutes(120), T0.plusMinutes(120), DEZ, 0);
        assertEquals(new BigDecimal("20.00"), c.getValor());
    }

    @Test
    void clubeHoraGratisInteira() {
        Cobranca c = Cobranca.calcular(TipoTempo.FECHADO, T0, T0.plusMinutes(90), T0.plusMinutes(90), DEZ, 3600);
        assertEquals(3600, c.getSegundosGratis());
        assertEquals(new BigDecimal("5.00"), c.getValor());
    }

    @Test
    void sessaoTotalmenteGratisNaoTemMinimo() {
        Cobranca c = Cobranca.calcular(TipoTempo.ABERTO, T0, null, T0.plusMinutes(40), DEZ, 3600);
        assertEquals(2400, c.getSegundosGratis());
        assertEquals(new BigDecimal("0.00"), c.getValor());
        assertEquals(0, c.getMinutosCredito());
    }

    @Test
    void horaGratisParcial() {
        Cobranca c = Cobranca.calcular(TipoTempo.FECHADO, T0, T0.plusMinutes(60), T0.plusMinutes(60), DEZ, 1200);
        assertEquals(1200, c.getSegundosGratis());
        assertEquals(new BigDecimal("6.67"), c.getValor());
    }

    @Test
    void fechadoUsadoMenosQueOContratadoViraCredito() {
        Cobranca c = Cobranca.calcular(TipoTempo.FECHADO, T0, T0.plusMinutes(120), T0.plusMinutes(75), DEZ, 0);
        assertEquals(new BigDecimal("20.00"), c.getValor());
        assertEquals(45, c.getMinutosCredito());
    }
}
