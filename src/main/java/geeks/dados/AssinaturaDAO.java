package geeks.dados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/** Assinaturas do Clube Geeks. Nada é apagado: cancelar = situacao 'CANCELADA'. */
public class AssinaturaDAO {

    /** Cliente em dia: assinatura ATIVA com data_fim >= hoje. */
    public boolean emDia(Connection c, int idCliente, LocalDate hoje) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT 1 FROM assinatura_clube "
                + "WHERE id_cliente = ? AND situacao = 'ATIVA' AND data_fim >= ?", idCliente, hoje);
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }

    /** Assinatura ATIVA do cliente (em dia ou não), travada para atualização. */
    public Optional<Integer> ativaParaAtualizar(Connection c, int idCliente) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT id_assinatura FROM assinatura_clube "
                + "WHERE id_cliente = ? AND situacao = 'ATIVA' FOR UPDATE", idCliente);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
        }
    }

    /** Nova assinatura de 1 mês: de hoje até hoje + 1 mês - 1 dia. */
    public int criar(Connection c, int idPlano, int idCliente, LocalDate hoje) throws SQLException {
        return Sql.inserir(c, "INSERT INTO assinatura_clube (data_inicio, data_fim, situacao, id_plano, id_cliente) "
                + "VALUES (?, ? + INTERVAL 1 MONTH - INTERVAL 1 DAY, 'ATIVA', ?, ?)", hoje, hoje, idPlano, idCliente);
    }

    /** Renova 1 mês: a partir de data_fim + 1 dia se em dia, ou de hoje se vencida. */
    public int renovar(Connection c, int idAssinatura, LocalDate hoje) throws SQLException {
        return Sql.atualizar(c, "UPDATE assinatura_clube "
                + "SET data_fim = DATE_ADD(GREATEST(data_fim + INTERVAL 1 DAY, ?), INTERVAL 1 MONTH) - INTERVAL 1 DAY "
                + "WHERE id_assinatura = ? AND situacao = 'ATIVA'", hoje, idAssinatura);
    }

    public int cancelar(Connection c, int idCliente) throws SQLException {
        return Sql.atualizar(c, "UPDATE assinatura_clube SET situacao = 'CANCELADA' "
                + "WHERE id_cliente = ? AND situacao = 'ATIVA'", idCliente);
    }

    public LocalDate dataFim(Connection c, int idAssinatura) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT data_fim FROM assinatura_clube WHERE id_assinatura = ?",
                idAssinatura); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Sql.data(rs, "data_fim") : null;
        }
    }
}
