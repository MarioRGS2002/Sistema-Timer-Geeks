package geeks.dados;

import geeks.model.Dispositivo;
import geeks.model.Plano;
import geeks.model.TipoDispositivo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Tipos de equipamento, equipamentos e plano do clube. */
public class EquipamentoDAO {

    /** Carrega valor por hora e id de cada tipo para o enum usado pela interface. */
    public void carregarTiposEPlano(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id_tipo, nome, valor_hora FROM tipo_equipamento");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TipoDispositivo.doNome(rs.getString("nome")).definirDoBanco(rs.getInt("id_tipo"), rs.getBigDecimal("valor_hora"));
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id_plano, valor_mensalidade FROM plano_clube WHERE nome_plano = 'Clube Geeks'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Plano.CLUBE.definirDoBanco(rs.getInt("id_plano"), rs.getBigDecimal("valor_mensalidade"));
            }
        }
    }

    /** Equipamentos que podem ser usados (status diferente de INDISPONIVEL). */
    public List<Dispositivo> listarUtilizaveis(Connection c) throws SQLException {
        String sql = "SELECT e.id_equipamento, e.identificador, t.nome AS tipo "
                + "FROM equipamento AS e INNER JOIN tipo_equipamento AS t ON t.id_tipo = e.id_tipo "
                + "WHERE e.status <> 'INDISPONIVEL' ORDER BY e.id_equipamento";
        List<Dispositivo> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Dispositivo(rs.getInt("id_equipamento"), rs.getString("identificador"),
                        TipoDispositivo.doNome(rs.getString("tipo"))));
            }
        }
        return lista;
    }

    /** Todos os equipamentos (inclusive em manutenção): {id, identificador, tipo, status, reservas futuras ativas}. */
    public List<Object[]> listarTodos(Connection c, LocalDateTime agora) throws SQLException {
        String sql = "SELECT e.id_equipamento, e.identificador, t.nome AS tipo, e.status, "
                + "(SELECT COUNT(*) FROM reserva AS r WHERE r.id_equipamento = e.id_equipamento "
                + "  AND r.status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA') AND r.fim_previsto > ?) AS reservas "
                + "FROM equipamento AS e INNER JOIN tipo_equipamento AS t ON t.id_tipo = e.id_tipo "
                + "ORDER BY t.id_tipo, e.identificador";
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql, agora); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                l.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5)});
            }
        }
        return l;
    }

    /** Status atual travando a linha (FOR UPDATE). */
    public String statusTravando(Connection c, int idEquipamento) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT status FROM equipamento WHERE id_equipamento = ? FOR UPDATE",
                idEquipamento); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new ErroBanco("Equipamento não encontrado.");
            }
            return rs.getString(1);
        }
    }

    /** Manutenção: INDISPONIVEL (some das telas de sessão e reserva) ou volta a DISPONIVEL. */
    public void definirManutencao(Connection c, int idEquipamento, boolean emManutencao) throws SQLException {
        Sql.atualizar(c, "UPDATE equipamento SET status = ? WHERE id_equipamento = ?",
                emManutencao ? "INDISPONIVEL" : "DISPONIVEL", idEquipamento);
    }

    public int inserir(Connection c, String identificador, String tipo) throws SQLException {
        return Sql.atualizar(c, "INSERT INTO equipamento (identificador, id_tipo, data_aquisicao, status) "
                + "SELECT ?, t.id_tipo, CURDATE(), 'DISPONIVEL' FROM tipo_equipamento AS t "
                + "WHERE t.nome = ? AND NOT EXISTS (SELECT 1 FROM equipamento AS q WHERE q.identificador = ?)",
                identificador, tipo, identificador);
    }

    /** Preços: {nome do tipo -> valor/hora} na ordem do cadastro. */
    public java.util.Map<String, java.math.BigDecimal> precosPorTipo(Connection c) throws SQLException {
        java.util.Map<String, java.math.BigDecimal> m = new java.util.LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT nome, valor_hora FROM tipo_equipamento ORDER BY id_tipo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString(1), rs.getBigDecimal(2));
            }
        }
        return m;
    }

    public void alterarValorHora(Connection c, String tipo, java.math.BigDecimal valor) throws SQLException {
        Sql.atualizar(c, "UPDATE tipo_equipamento SET valor_hora = ? WHERE nome = ?", valor, tipo);
    }

    public void alterarMensalidadeClube(Connection c, java.math.BigDecimal valor) throws SQLException {
        Sql.atualizar(c, "UPDATE plano_clube SET valor_mensalidade = ? WHERE nome_plano = 'Clube Geeks'", valor);
    }

    public void definirStatus(Connection c, int idEquipamento, String status) throws SQLException {
        Sql.atualizar(c, "UPDATE equipamento SET status = ? WHERE id_equipamento = ? AND status <> 'INDISPONIVEL'",
                status, idEquipamento);
    }

    /** Trava a linha do equipamento (serializa reservas/sessões concorrentes no mesmo equipamento). */
    public void travar(Connection c, int idEquipamento) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c,
                "SELECT id_equipamento FROM equipamento WHERE id_equipamento = ? FOR UPDATE", idEquipamento);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new ErroBanco("Equipamento não encontrado.");
            }
        }
    }

    /**
     * Equipamentos livres do mesmo tipo para o período [ini, fim): sem sessão aberta que avance
     * sobre o período e sem reserva ativa sobreposta. Contrato da seção 5.
     */
    public List<String> livresMesmoTipo(Connection c, int idEquipamento, LocalDateTime ini, LocalDateTime fim,
                                        LocalDateTime agora, int janelaMinutos) throws SQLException {
        String sql = "SELECT e2.id_equipamento, e2.identificador "
                + "FROM equipamento AS e1 "
                + "INNER JOIN equipamento AS e2 ON e2.id_tipo = e1.id_tipo AND e2.id_equipamento <> e1.id_equipamento "
                + "WHERE e1.id_equipamento = ? AND e2.status <> 'INDISPONIVEL' "
                + "  AND NOT EXISTS (SELECT 1 FROM sessao AS s "
                + "        WHERE s.id_equipamento = e2.id_equipamento AND s.horario_fim IS NULL "
                + "          AND ((s.horario_previsto IS NOT NULL AND s.horario_previsto > ?) "
                + "            OR (s.horario_previsto IS NULL AND ? < ? + INTERVAL ? MINUTE))) "
                + "  AND NOT EXISTS (SELECT 1 FROM reserva AS r "
                + "        WHERE r.id_equipamento = e2.id_equipamento "
                + "          AND r.status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA') "
                + "          AND r.inicio_previsto < ? AND r.fim_previsto > ?) "
                + "ORDER BY e2.identificador";
        List<String> nomes = new ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql, idEquipamento, ini, ini, agora, janelaMinutos, fim, ini);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                nomes.add(rs.getString("identificador"));
            }
        }
        return nomes;
    }
}
