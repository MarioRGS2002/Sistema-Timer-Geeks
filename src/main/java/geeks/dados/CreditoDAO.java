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

    /** Saldo em minutos: soma dos créditos com valido_ate >= hoje. */
    public int saldo(Connection c, int idCliente, LocalDate hoje) throws SQLException {
        return (int) Sql.contar(c, "SELECT COALESCE(SUM(minutos_restantes), 0) FROM credito_tempo "
                + "WHERE id_cliente = ? AND valido_ate >= ?", idCliente, hoje);
    }
}
