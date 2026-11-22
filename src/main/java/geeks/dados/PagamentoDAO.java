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

    public BigDecimal total(Connection c, LocalDateTime ini, LocalDateTime fim) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT COALESCE(SUM(valor_pago), 0) FROM pagamento "
                + "WHERE data_hora_pagamento >= ? AND data_hora_pagamento < ?", ini, fim);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    /** Soma por hora do dia (só horas com pagamento aparecem). */
    public Map<Integer, BigDecimal> porHora(Connection c, LocalDateTime ini, LocalDateTime fim) throws SQLException {
        Map<Integer, BigDecimal> m = new LinkedHashMap<>();
        try (PreparedStatement ps = Sql.preparar(c, "SELECT HOUR(data_hora_pagamento) AS hora, SUM(valor_pago) AS total "
                + "FROM pagamento WHERE data_hora_pagamento >= ? AND data_hora_pagamento < ? "
                + "GROUP BY HOUR(data_hora_pagamento) ORDER BY hora", ini, fim);
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
        try (PreparedStatement ps = Sql.preparar(c, "SELECT DATE(data_hora_pagamento) AS dia, SUM(valor_pago) AS total "
                + "FROM pagamento WHERE data_hora_pagamento >= ? AND data_hora_pagamento < ? "
                + "GROUP BY DATE(data_hora_pagamento) ORDER BY dia", ini, fim);
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
        try (PreparedStatement ps = Sql.preparar(c, "SELECT fp.nome AS forma, SUM(p.valor_pago) AS total "
                + "FROM pagamento AS p LEFT JOIN forma_pagamento AS fp ON fp.id_forma_pagamento = p.id_forma_pagamento "
                + "WHERE p.data_hora_pagamento >= ? AND p.data_hora_pagamento < ? "
                + "GROUP BY fp.nome ORDER BY fp.nome", ini, fim);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString("forma"), rs.getBigDecimal("total"));
            }
        }
        return m;
    }
}
