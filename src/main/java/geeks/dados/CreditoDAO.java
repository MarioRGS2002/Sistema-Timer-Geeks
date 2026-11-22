package geeks.dados;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Créditos de tempo (tempo pago e não usado). minutos_restantes é o único saldo;
 * validade de 3 meses; crédito vencido fica guardado e não conta no saldo.
 */
public class CreditoDAO {
    public static final int MESES_VALIDADE = 3;

    public int gerar(Connection c, int idCliente, int idTipo, int idSessaoOrigem, int minutos, LocalDateTime geracao)
            throws SQLException {
        return Sql.inserir(c, "INSERT INTO credito_tempo (id_cliente, id_tipo, id_sessao_origem, minutos_restantes, "
                        + "data_geracao, valido_ate) VALUES (?, ?, ?, ?, ?, ?)",
                idCliente, idTipo, idSessaoOrigem, minutos, geracao, geracao.toLocalDate().plusMonths(MESES_VALIDADE));
    }

    /** Saldo em minutos só do tipo de equipamento (crédito de PlayStation não vale no PC). */
    public int saldoDoTipo(Connection c, int idCliente, int idTipo, LocalDate hoje) throws SQLException {
        return (int) Sql.contar(c, "SELECT COALESCE(SUM(minutos_restantes), 0) FROM credito_tempo "
                + "WHERE id_cliente = ? AND id_tipo = ? AND valido_ate >= ?", idCliente, idTipo, hoje);
    }

    /**
     * Abate {@code minutos} do saldo do cliente nesse tipo, começando pelo crédito que vence
     * primeiro (FOR UPDATE: dois encerramentos ao mesmo tempo não usam o mesmo crédito).
     */
    public void consumir(Connection c, int idCliente, int idTipo, int minutos, LocalDate hoje) throws SQLException {
        int falta = minutos;
        java.util.List<int[]> linhas = new java.util.ArrayList<>();
        try (java.sql.PreparedStatement ps = Sql.preparar(c, "SELECT id_credito, minutos_restantes FROM credito_tempo "
                + "WHERE id_cliente = ? AND id_tipo = ? AND valido_ate >= ? AND minutos_restantes > 0 "
                + "ORDER BY valido_ate, id_credito FOR UPDATE", idCliente, idTipo, hoje);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                linhas.add(new int[]{rs.getInt(1), rs.getInt(2)});
            }
        }
        for (int[] l : linhas) {
            if (falta <= 0) {
                break;
            }
            int usa = Math.min(falta, l[1]);
            Sql.atualizar(c, "UPDATE credito_tempo SET minutos_restantes = minutos_restantes - ? WHERE id_credito = ?", usa, l[0]);
            falta -= usa;
        }
        if (falta > 0) {
            throw new IllegalStateException("O cliente não tem mais crédito suficiente (foi usado em outro computador?).");
        }
    }

    /** Saldo em minutos: soma dos créditos com valido_ate >= hoje. */
    public int saldo(Connection c, int idCliente, LocalDate hoje) throws SQLException {
        return (int) Sql.contar(c, "SELECT COALESCE(SUM(minutos_restantes), 0) FROM credito_tempo "
                + "WHERE id_cliente = ? AND valido_ate >= ?", idCliente, hoje);
    }
}
