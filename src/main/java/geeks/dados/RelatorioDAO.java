package geeks.dados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Relatórios do gerente: sessões ENCERRADAS no período [ini, fim) (períodos semiabertos). */
public class RelatorioDAO {

    /** Equipamentos mais usados: {identificador, sessões, segundos de uso}. */
    public List<Object[]> equipamentosMaisUsados(Connection c, LocalDateTime ini, LocalDateTime fim, int limite)
            throws SQLException {
        return listar(c, "SELECT e.identificador, COUNT(*) AS n, "
                + "SUM(TIMESTAMPDIFF(SECOND, s.horario_inicio, s.horario_fim)) AS seg "
                + "FROM sessao AS s INNER JOIN equipamento AS e ON e.id_equipamento = s.id_equipamento "
                + "WHERE s.horario_fim >= ? AND s.horario_fim < ? "
                + "GROUP BY e.id_equipamento, e.identificador ORDER BY seg DESC, n DESC LIMIT ?", ini, fim, limite);
    }

    /** Horários de pico: {hora em que a sessão começou, sessões}. */
    public List<Object[]> horariosDePico(Connection c, LocalDateTime ini, LocalDateTime fim, int limite) throws SQLException {
        return listar(c, "SELECT HOUR(s.horario_inicio) AS h, COUNT(*) AS n FROM sessao AS s "
                + "WHERE s.horario_fim >= ? AND s.horario_fim < ? "
                + "GROUP BY HOUR(s.horario_inicio) ORDER BY n DESC, h LIMIT ?", ini, fim, limite);
    }

    /** Clientes mais frequentes: {nome, sessões, valor total das sessões}. */
    public List<Object[]> clientesFrequentes(Connection c, LocalDateTime ini, LocalDateTime fim, int limite)
            throws SQLException {
        return listar(c, "SELECT cl.nome, COUNT(*) AS n, COALESCE(SUM(s.valor_total_devido), 0) AS v "
                + "FROM participante_sessao AS ps "
                + "INNER JOIN sessao AS s ON s.id_sessao = ps.id_sessao "
                + "INNER JOIN cliente AS cl ON cl.id_cliente = ps.id_cliente "
                + "WHERE s.horario_fim >= ? AND s.horario_fim < ? "
                + "GROUP BY cl.id_cliente, cl.nome ORDER BY n DESC, v DESC LIMIT ?", ini, fim, limite);
    }

    private static List<Object[]> listar(Connection c, String sql, Object... params) throws SQLException {
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql, params); ResultSet rs = ps.executeQuery()) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] linha = new Object[n];
                for (int i = 0; i < n; i++) {
                    linha[i] = rs.getObject(i + 1);
                }
                l.add(linha);
            }
        }
        return l;
    }
}
