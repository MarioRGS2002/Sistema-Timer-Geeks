package geeks.dados;

import geeks.model.Cliente;
import geeks.model.Plano;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Clientes (com o nome do responsável e se a assinatura do Clube está em dia). */
public class ClienteDAO {
    private static final String BASE = "SELECT c.id_cliente, c.nome, c.data_nascimento, c.telefone, c.cpf, "
            + "(SELECT r.nome FROM responsavel AS r WHERE r.id_cliente = c.id_cliente "
            + "  ORDER BY r.id_responsavel LIMIT 1) AS responsavel, "
            + "EXISTS (SELECT 1 FROM assinatura_clube AS a WHERE a.id_cliente = c.id_cliente "
            + "  AND a.situacao = 'ATIVA' AND a.data_fim >= ?) AS em_dia "
            + "FROM cliente AS c ";

    public List<Cliente> listarAtivos(Connection c, LocalDate hoje) throws SQLException {
        return listar(c, BASE + "WHERE c.ativo = 1 ORDER BY c.nome", hoje);
    }

    public Optional<Cliente> porId(Connection c, int idCliente, LocalDate hoje) throws SQLException {
        return listar(c, BASE + "WHERE c.id_cliente = ?", hoje, idCliente).stream().findFirst();
    }

    public Optional<Cliente> porCpf(Connection c, String cpf, LocalDate hoje) throws SQLException {
        return listar(c, BASE + "WHERE c.ativo = 1 AND c.cpf = ?", hoje, cpf).stream().findFirst();
    }

    /** Primeiro cliente ativo cujo nome contém o termo (sem diferenciar maiúsculas e acentos). */
    public Optional<Cliente> porParteDoNome(Connection c, String termo, LocalDate hoje) throws SQLException {
        return listar(c, BASE + "WHERE c.ativo = 1 AND c.nome LIKE ? ORDER BY c.nome LIMIT 1",
                hoje, "%" + escaparLike(termo) + "%").stream().findFirst();
    }

    /** Todos os clientes ativos cujo nome contém o termo (até 30), para escolher entre homônimos. */
    public List<Cliente> listarPorParteDoNome(Connection c, String termo, LocalDate hoje) throws SQLException {
        return listar(c, BASE + "WHERE c.ativo = 1 AND c.nome LIKE ? ORDER BY c.nome LIMIT 30",
                hoje, "%" + escaparLike(termo) + "%");
    }

    /**
     * Dados extras da ficha do cliente: responsável (nome, parentesco, CPF, telefone), até quando vai
     * o Clube, data do cadastro, quantas sessões já fez e a última visita.
     */
    public java.util.Map<String, Object> ficha(Connection c, int idCliente) throws SQLException {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        try (PreparedStatement ps = Sql.preparar(c, "SELECT r.nome, r.parentesco, r.cpf, r.telefone FROM responsavel AS r "
                + "WHERE r.id_cliente = ? ORDER BY r.id_responsavel LIMIT 1", idCliente); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                m.put("respNome", rs.getString(1));
                m.put("respParentesco", rs.getString(2));
                m.put("respCpf", rs.getString(3));
                m.put("respTelefone", rs.getString(4));
            }
        }
        try (PreparedStatement ps = Sql.preparar(c, "SELECT MAX(a.data_fim) FROM assinatura_clube AS a "
                + "WHERE a.id_cliente = ? AND a.situacao = 'ATIVA'", idCliente); ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getDate(1) != null) {
                m.put("clubeAte", rs.getDate(1).toLocalDate());
            }
        }
        try (PreparedStatement ps = Sql.preparar(c, "SELECT c.data_cadastro, COUNT(s.id_sessao), MAX(s.horario_fim) "
                + "FROM cliente AS c LEFT JOIN participante_sessao AS ps ON ps.id_cliente = c.id_cliente "
                + "LEFT JOIN sessao AS s ON s.id_sessao = ps.id_sessao AND s.horario_fim IS NOT NULL "
                + "WHERE c.id_cliente = ? GROUP BY c.data_cadastro", idCliente); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                m.put("cadastro", Sql.dataHora(rs, "data_cadastro"));
                m.put("sessoes", rs.getLong(2));
                java.sql.Timestamp ult = rs.getTimestamp(3);
                if (ult != null) {
                    m.put("ultimaVisita", ult.toLocalDateTime());
                }
            }
        }
        return m;
    }

    public boolean existeCpf(Connection c, String cpf) throws SQLException {
        return Sql.contar(c, "SELECT COUNT(*) FROM cliente WHERE cpf = ?", cpf) > 0;
    }

    public int inserir(Connection c, String nome, LocalDate nascimento, String telefone, String cpf) throws SQLException {
        return Sql.inserir(c, "INSERT INTO cliente (nome, cpf, data_nascimento, telefone) VALUES (?, ?, ?, ?)",
                nome, cpf == null || cpf.isEmpty() ? null : cpf, nascimento,
                telefone == null || telefone.isBlank() ? null : telefone);
    }

    /** Corrige nome, nascimento e telefone (o CPF não muda). */
    public void atualizar(Connection c, int idCliente, String nome, LocalDate nascimento, String telefone) throws SQLException {
        Sql.atualizar(c, "UPDATE cliente SET nome = ?, data_nascimento = ?, telefone = ? WHERE id_cliente = ?",
                nome, nascimento, telefone == null || telefone.isBlank() ? null : telefone, idCliente);
    }

    /** Atualiza o primeiro responsável do cliente ou cadastra um, se não houver. */
    public void salvarResponsavel(Connection c, int idCliente, String nome, String cpf, String telefone, String parentesco)
            throws SQLException {
        long id = Sql.contar(c, "SELECT COALESCE(MIN(id_responsavel), 0) FROM responsavel WHERE id_cliente = ?", idCliente);
        if (id == 0) {
            inserirResponsavel(c, idCliente, nome, cpf, telefone, parentesco);
            return;
        }
        Sql.atualizar(c, "UPDATE responsavel SET nome = ?, cpf = ?, telefone = ?, parentesco = ? WHERE id_responsavel = ?",
                nome, cpf, telefone == null || telefone.isBlank() ? null : telefone,
                parentesco == null || parentesco.isBlank() ? null : parentesco, id);
    }

    public int inserirResponsavel(Connection c, int idCliente, String nome, String cpf, String telefone, String parentesco)
            throws SQLException {
        return Sql.inserir(c, "INSERT INTO responsavel (id_cliente, nome, cpf, telefone, parentesco) VALUES (?, ?, ?, ?, ?)",
                idCliente, nome, cpf, telefone == null || telefone.isBlank() ? null : telefone,
                parentesco == null || parentesco.isBlank() ? null : parentesco);
    }

    private List<Cliente> listar(Connection c, String sql, Object... params) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql, params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cliente cl = new Cliente(rs.getInt("id_cliente"), rs.getString("nome"), Sql.data(rs, "data_nascimento"),
                        rs.getString("telefone"), rs.getString("cpf"), rs.getString("responsavel"));
                cl.setPlano(rs.getBoolean("em_dia") ? Plano.CLUBE : Plano.NENHUM);
                lista.add(cl);
            }
        }
        return lista;
    }

    static String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
