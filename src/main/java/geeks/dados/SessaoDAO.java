package geeks.dados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Sessões e participantes. */
public class SessaoDAO {

    /** Linha de sessão com um participante (uma linha por participante). */
    public static final class Linha {
        public int idSessao;
        public int idCliente;
        public int idEquipamento;
        public int idTipo;
        public LocalDateTime inicio;
        public LocalDateTime previsto;
        public LocalDateTime fim;
        public String tipoTempo;
        public BigDecimal valorHora;
        public BigDecimal valorTotal;
        public int segundosGratis;
        /** Entrada paga na reserva que virou esta sessão (0 se não veio de reserva). */
        public BigDecimal entradaPaga;
    }

    private static final String BASE = "SELECT s.id_sessao, s.horario_inicio, s.horario_previsto, s.horario_fim, "
            + "s.tipo_tempo, s.valor_hora_contratado, s.valor_total_devido, s.id_equipamento, e.id_tipo, "
            + "ps.id_cliente, ps.segundos_gratis, "
            + "(SELECT COALESCE(SUM(p.valor_pago), 0) FROM reserva AS r "
            + "  INNER JOIN pagamento AS p ON p.id_reserva = r.id_reserva WHERE r.id_sessao = s.id_sessao) AS entrada_paga "
            + "FROM sessao AS s "
            + "INNER JOIN equipamento AS e ON e.id_equipamento = s.id_equipamento "
            + "INNER JOIN tipo_equipamento AS t ON t.id_tipo = e.id_tipo "
            + "INNER JOIN participante_sessao AS ps ON ps.id_sessao = s.id_sessao "
            + "INNER JOIN cliente AS c ON c.id_cliente = ps.id_cliente ";

    public int abrir(Connection c, String tipoTempo, LocalDateTime inicio, LocalDateTime previsto,
                     BigDecimal valorHora, int idEquipamento, int idFuncionario, int idCliente) throws SQLException {
        int id = Sql.inserir(c, "INSERT INTO sessao (horario_inicio, horario_previsto, tipo_tempo, status_sessao, "
                        + "valor_hora_contratado, id_equipamento, id_funcionario) VALUES (?, ?, ?, 'EM_ANDAMENTO', ?, ?, ?)",
                inicio, previsto, tipoTempo, valorHora, idEquipamento, idFuncionario);
        Sql.atualizar(c, "INSERT INTO participante_sessao (id_sessao, id_cliente, segundos_gratis) VALUES (?, ?, 0)",
                id, idCliente);
        return id;
    }

    /** Regra da aplicação: um cliente só pode estar em uma sessão ativa por vez. */
    public boolean clienteEmSessaoAtiva(Connection c, int idCliente) throws SQLException {
        return Sql.contar(c, "SELECT COUNT(*) FROM participante_sessao AS ps "
                + "INNER JOIN sessao AS s ON s.id_sessao = ps.id_sessao "
                + "WHERE ps.id_cliente = ? AND s.horario_fim IS NULL", idCliente) > 0;
    }

    public List<Linha> ativas(Connection c) throws SQLException {
        return listar(c, BASE + "WHERE s.horario_fim IS NULL ORDER BY s.horario_inicio");
    }

    /** Trava a sessão para encerrar (SELECT ... FOR UPDATE). */
    public Linha travarAtiva(Connection c, int idSessao) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT id_sessao FROM sessao WHERE id_sessao = ? FOR UPDATE", idSessao);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new ErroBanco("Sessão não encontrada.");
            }
        }
        List<Linha> l = listar(c, BASE + "WHERE s.id_sessao = ? AND s.horario_fim IS NULL", idSessao);
        if (l.isEmpty()) {
            throw new ErroBanco("Essa sessão já foi encerrada.");
        }
        return l.get(0);
    }

    /** Hora grátis já usada pelo cliente no dia [diaInicio, diaInicio + 1), pelo início da sessão. */
    public int gratisUsadoNoDia(Connection c, int idCliente, LocalDate dia) throws SQLException {
        return (int) Sql.contar(c, "SELECT COALESCE(SUM(ps.segundos_gratis), 0) FROM participante_sessao AS ps "
                        + "INNER JOIN sessao AS s ON s.id_sessao = ps.id_sessao "
                        + "WHERE ps.id_cliente = ? AND s.horario_inicio >= ? AND s.horario_inicio < ?",
                idCliente, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }

    public void encerrar(Connection c, int idSessao, LocalDateTime fim, BigDecimal valorTotal) throws SQLException {
        int n = Sql.atualizar(c, "UPDATE sessao SET horario_fim = ?, status_sessao = 'ENCERRADA', valor_total_devido = ? "
                + "WHERE id_sessao = ? AND horario_fim IS NULL", fim, valorTotal, idSessao);
        if (n != 1) {
            throw new ErroBanco("Essa sessão já foi encerrada.");
        }
    }

    /**
     * Marca que o aviso de "tempo acabando" foi dado. Devolve true só para quem marcou primeiro
     * (com dois computadores abertos, só um deles mostra o aviso).
     */
    public boolean marcarAvisoTempo(Connection c, int idSessao) throws SQLException {
        return Sql.atualizar(c, "UPDATE sessao SET notificacao_enviada = 1 WHERE id_sessao = ? "
                + "AND notificacao_enviada = 0 AND horario_fim IS NULL", idSessao) == 1;
    }

    /** Crédito de tempo usado pelo cliente nesta sessão (coluna da V5). */
    public void definirCredito(Connection c, int idSessao, int idCliente, int segundos) throws SQLException {
        Sql.atualizar(c, "UPDATE participante_sessao SET segundos_credito = ? WHERE id_sessao = ? AND id_cliente = ?",
                segundos, idSessao, idCliente);
    }

    public void definirGratis(Connection c, int idSessao, int idCliente, int segundos) throws SQLException {
        Sql.atualizar(c, "UPDATE participante_sessao SET segundos_gratis = ? WHERE id_sessao = ? AND id_cliente = ?",
                segundos, idSessao, idCliente);
    }

    /**
     * Histórico: sessões encerradas desde {@code desde}, mais recentes primeiro. Uma linha por
     * participante. Filtro LIKE no nome do cliente, no identificador e no tipo do equipamento
     * (sem diferenciar maiúsculas e acentos: collation utf8mb4_0900_ai_ci).
     */
    public List<Linha> historico(Connection c, LocalDateTime desde, String texto) throws SQLException {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            return listar(c, BASE + "WHERE s.horario_fim >= ? ORDER BY s.horario_fim DESC", desde);
        }
        String like = "%" + ClienteDAO.escaparLike(t) + "%";
        return listar(c, BASE + "WHERE s.horario_fim >= ? "
                + "AND (c.nome LIKE ? OR e.identificador LIKE ? OR t.nome LIKE ?) "
                + "ORDER BY s.horario_fim DESC", desde, like, like, like);
    }

    private List<Linha> listar(Connection c, String sql, Object... params) throws SQLException {
        List<Linha> lista = new ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql, params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Linha l = new Linha();
                l.idSessao = rs.getInt("id_sessao");
                l.idCliente = rs.getInt("id_cliente");
                l.idEquipamento = rs.getInt("id_equipamento");
                l.idTipo = rs.getInt("id_tipo");
                l.inicio = Sql.dataHora(rs, "horario_inicio");
                l.previsto = Sql.dataHora(rs, "horario_previsto");
                l.fim = Sql.dataHora(rs, "horario_fim");
                l.tipoTempo = rs.getString("tipo_tempo");
                l.valorHora = rs.getBigDecimal("valor_hora_contratado");
                l.valorTotal = rs.getBigDecimal("valor_total_devido");
                l.segundosGratis = rs.getInt("segundos_gratis");
                l.entradaPaga = rs.getBigDecimal("entrada_paga");
                lista.add(l);
            }
        }
        return lista;
    }
}
