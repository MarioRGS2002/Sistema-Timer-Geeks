package geeks.dados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pagamentos (nunca apagados) e as somas do faturamento. Períodos semiabertos [ini, fim). */
public class PagamentoDAO {
    public static final String SESSAO = "SESSAO";
    public static final String RESERVA = "RESERVA";
    public static final String ASSINATURA = "ASSINATURA";

    /** Nomes das formas de pagamento ativas (Dinheiro, PIX, Débito, Crédito...), na ordem do cadastro. */
    public java.util.List<String> formas(Connection c) throws SQLException {
        java.util.List<String> l = new java.util.ArrayList<>();
        try (java.sql.PreparedStatement ps = Sql.preparar(c, "SELECT f.nome FROM forma_pagamento AS f "
                + "WHERE f.ativo = 1 ORDER BY f.id_forma_pagamento"); java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                l.add(rs.getString(1));
            }
        }
        return l;
    }

    public int idForma(Connection c, String nome) throws SQLException {
        long id = Sql.contar(c, "SELECT id_forma_pagamento FROM forma_pagamento WHERE nome = ? AND ativo = 1", nome);
        if (id == 0) {
            throw new ErroBanco("Forma de pagamento \"" + nome + "\" não cadastrada.");
        }
        return (int) id;
    }

    /**
     * Registra um pagamento com exatamente uma origem. Valor 0 (hora grátis) vai sem forma
     * de pagamento; valor maior que 0 exige forma (CHECK do banco).
     */
    public int registrar(Connection c, LocalDateTime quando, BigDecimal valor, String momento, String tipo,
                         Integer idForma, int idFuncionario, Integer idSessao, Integer idReserva, Integer idAssinatura)
            throws SQLException {
        return Sql.inserir(c, "INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, "
                        + "id_forma_pagamento, id_funcionario, id_sessao, id_reserva, id_assinatura) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                quando, valor, momento, tipo, idForma, idFuncionario, idSessao, idReserva, idAssinatura);
    }

    /**
     * Movimento de caixa no período [ini, fim): pagamentos (positivos) e estornos (negativos, na
     * data do estorno, com a forma do pagamento estornado). Cada lado usa o próprio índice de data.
     * Parâmetros: ini, fim, ini, fim.
     */
    private static final String MOVIMENTO = "(SELECT p.data_hora_pagamento AS quando, p.valor_pago AS valor, "
            + "p.id_forma_pagamento AS id_forma FROM pagamento AS p "
            + "WHERE p.data_hora_pagamento >= ? AND p.data_hora_pagamento < ? "
            + "UNION ALL SELECT e.data_hora, -e.valor, pe.id_forma_pagamento FROM estorno AS e "
            + "INNER JOIN pagamento AS pe ON pe.id_pagamento = e.id_pagamento "
            + "WHERE e.data_hora >= ? AND e.data_hora < ?) AS m ";

    public BigDecimal total(Connection c, LocalDateTime ini, LocalDateTime fim) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT COALESCE(SUM(m.valor), 0) FROM " + MOVIMENTO, ini, fim, ini, fim);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    /** Soma por hora do dia (só horas com movimento aparecem). */
    public Map<Integer, BigDecimal> porHora(Connection c, LocalDateTime ini, LocalDateTime fim) throws SQLException {
        Map<Integer, BigDecimal> m = new LinkedHashMap<>();
        try (PreparedStatement ps = Sql.preparar(c, "SELECT HOUR(m.quando) AS hora, SUM(m.valor) AS total FROM "
                + MOVIMENTO + "GROUP BY HOUR(m.quando) ORDER BY hora", ini, fim, ini, fim);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getInt("hora"), rs.getBigDecimal("total"));
            }
        }
        return m;
    }

    /** Soma por dia (só dias com pagamento aparecem). */
    public Map<LocalDate, BigDecimal> porDia(Connection c, LocalDateTime ini, LocalDateTime fim) throws SQLException {
        Map<LocalDate, BigDecimal> m = new LinkedHashMap<>();
        try (PreparedStatement ps = Sql.preparar(c, "SELECT DATE(m.quando) AS dia, SUM(m.valor) AS total FROM "
                + MOVIMENTO + "GROUP BY DATE(m.quando) ORDER BY dia", ini, fim, ini, fim);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(Sql.data(rs, "dia"), rs.getBigDecimal("total"));
            }
        }
        return m;
    }

    /** Soma por forma de pagamento (hora grátis aparece com forma nula). */
    public Map<String, BigDecimal> porForma(Connection c, LocalDateTime ini, LocalDateTime fim) throws SQLException {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        try (PreparedStatement ps = Sql.preparar(c, "SELECT fp.nome AS forma, SUM(m.valor) AS total FROM " + MOVIMENTO
                + "LEFT JOIN forma_pagamento AS fp ON fp.id_forma_pagamento = m.id_forma "
                + "GROUP BY fp.nome ORDER BY fp.nome", ini, fim, ini, fim);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString("forma"), rs.getBigDecimal("total"));
            }
        }
        return m;
    }
}
