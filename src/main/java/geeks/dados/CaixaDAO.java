package geeks.dados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Movimento de caixa: pagamentos recebidos (com cliente e quem recebeu) e estornos. */
public class CaixaDAO {

    /** Um pagamento recebido no dia. */
    public static final class Pagamento {
        public int idPagamento;
        public LocalDateTime quando;
        public BigDecimal valor;
        public String tipo;
        public String forma;
        public String funcionario;
        public String cliente;
        public BigDecimal estornado;
    }

    /** Um estorno feito no dia. */
    public static final class Estorno {
        public int idEstorno;
        public int idPagamento;
        public LocalDateTime quando;
        public BigDecimal valor;
        public String motivo;
        public String funcionario;
        public String forma;
    }

    /** Pagamentos com valor (sem as horas grátis de valor 0) em [ini, fim), mais recentes primeiro. */
    public List<Pagamento> pagamentos(Connection c, LocalDateTime ini, LocalDateTime fim) throws SQLException {
        String sql = "SELECT p.id_pagamento, p.data_hora_pagamento, p.valor_pago, p.tipo_pagamento, fp.nome AS forma, "
                + "f.nome AS funcionario, COALESCE(cs.nome, cr.nome, ca.nome) AS cliente, "
                + "(SELECT COALESCE(SUM(e.valor), 0) FROM estorno AS e WHERE e.id_pagamento = p.id_pagamento) AS estornado "
                + "FROM pagamento AS p "
                + "INNER JOIN funcionario AS f ON f.id_funcionario = p.id_funcionario "
                + "LEFT JOIN forma_pagamento AS fp ON fp.id_forma_pagamento = p.id_forma_pagamento "
                + "LEFT JOIN participante_sessao AS ps ON ps.id_sessao = p.id_sessao "
                + "LEFT JOIN cliente AS cs ON cs.id_cliente = ps.id_cliente "
                + "LEFT JOIN reserva AS r ON r.id_reserva = p.id_reserva "
                + "LEFT JOIN cliente AS cr ON cr.id_cliente = r.id_cliente "
                + "LEFT JOIN assinatura_clube AS a ON a.id_assinatura = p.id_assinatura "
                + "LEFT JOIN cliente AS ca ON ca.id_cliente = a.id_cliente "
                + "WHERE p.data_hora_pagamento >= ? AND p.data_hora_pagamento < ? AND p.valor_pago > 0 "
                + "ORDER BY p.data_hora_pagamento DESC, p.id_pagamento DESC";
        List<Pagamento> l = new ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql, ini, fim); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pagamento p = new Pagamento();
                p.idPagamento = rs.getInt("id_pagamento");
                p.quando = Sql.dataHora(rs, "data_hora_pagamento");
                p.valor = rs.getBigDecimal("valor_pago");
                p.tipo = rs.getString("tipo_pagamento");
                p.forma = rs.getString("forma");
                p.funcionario = rs.getString("funcionario");
                p.cliente = rs.getString("cliente");
                p.estornado = rs.getBigDecimal("estornado");
                l.add(p);
            }
        }
        return l;
    }

    public List<Estorno> estornos(Connection c, LocalDateTime ini, LocalDateTime fim) throws SQLException {
        String sql = "SELECT e.id_estorno, e.id_pagamento, e.data_hora, e.valor, e.motivo, f.nome AS funcionario, "
                + "fp.nome AS forma FROM estorno AS e "
                + "INNER JOIN funcionario AS f ON f.id_funcionario = e.id_funcionario "
                + "INNER JOIN pagamento AS p ON p.id_pagamento = e.id_pagamento "
                + "LEFT JOIN forma_pagamento AS fp ON fp.id_forma_pagamento = p.id_forma_pagamento "
                + "WHERE e.data_hora >= ? AND e.data_hora < ? ORDER BY e.data_hora DESC";
        List<Estorno> l = new ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql, ini, fim); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Estorno e = new Estorno();
                e.idEstorno = rs.getInt("id_estorno");
                e.idPagamento = rs.getInt("id_pagamento");
                e.quando = Sql.dataHora(rs, "data_hora");
                e.valor = rs.getBigDecimal("valor");
                e.motivo = rs.getString("motivo");
                e.funcionario = rs.getString("funcionario");
                e.forma = rs.getString("forma");
                l.add(e);
            }
        }
        return l;
    }

    /**
     * Trava o pagamento (FOR UPDATE) e devolve {valor pago, já estornado}; null se não existir.
     * Dois estornos ao mesmo tempo no mesmo pagamento ficam em fila e não passam do valor pago.
     */
    public BigDecimal[] travarPagamento(Connection c, int idPagamento) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT p.valor_pago FROM pagamento AS p WHERE p.id_pagamento = ? FOR UPDATE",
                idPagamento); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            BigDecimal valor = rs.getBigDecimal(1);
            BigDecimal estornado;
            try (PreparedStatement ps2 = Sql.preparar(c, "SELECT COALESCE(SUM(e.valor), 0) FROM estorno AS e WHERE e.id_pagamento = ?",
                    idPagamento); ResultSet rs2 = ps2.executeQuery()) {
                rs2.next();
                estornado = rs2.getBigDecimal(1);
            }
            return new BigDecimal[]{valor, estornado};
        }
    }

    public int inserirEstorno(Connection c, int idPagamento, BigDecimal valor, String motivo, LocalDateTime quando,
                              int idFuncionario) throws SQLException {
        return Sql.inserir(c, "INSERT INTO estorno (id_pagamento, valor, motivo, data_hora, id_funcionario) VALUES (?, ?, ?, ?, ?)",
                idPagamento, valor, motivo, quando, idFuncionario);
    }
}
