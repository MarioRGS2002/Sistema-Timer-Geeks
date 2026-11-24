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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Reservas (sempre em tempo FECHADO). Cancelar = status 'CANCELADA'. */
public class ReservaDAO {

    public static final class Linha {
        public int idReserva;
        public int idCliente;
        public int idEquipamento;
        public LocalDateTime inicio;
        public LocalDateTime fim;
    }

    /**
     * Quantas reservas ativas do equipamento conflitam com [inicio, fim), considerando a janela
     * de folga J. Deve ser precedido de EquipamentoDAO.travar (SELECT ... FOR UPDATE).
     * {@code idIgnorar} = a própria reserva numa alteração (0 para nenhuma).
     */
    public long conflitos(Connection c, int idEquipamento, int idIgnorar, LocalDateTime inicio, LocalDateTime fim,
                          int janelaMinutos) throws SQLException {
        return Sql.contar(c, "SELECT COUNT(*) FROM reserva "
                        + "WHERE id_equipamento = ? AND id_reserva <> ? "
                        + "AND status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA') "
                        + "AND inicio_previsto < ? AND fim_previsto > ?",
                idEquipamento, idIgnorar, fim.plusMinutes(janelaMinutos), inicio.minusMinutes(janelaMinutos));
    }

    public int inserir(Connection c, LocalDateTime inicio, LocalDateTime fim, BigDecimal valorPrevisto,
                       BigDecimal percentualEntrada, BigDecimal valorEntrada, int idCliente, int idEquipamento) throws SQLException {
        return Sql.inserir(c, "INSERT INTO reserva (inicio_previsto, fim_previsto, status_reserva, valor_previsto, "
                        + "percentual_entrada_minima, valor_entrada_minima, id_cliente, id_equipamento) "
                        + "VALUES (?, ?, 'PENDENTE_ENTRADA', ?, ?, ?, ?, ?)",
                inicio, fim, valorPrevisto, percentualEntrada, valorEntrada, idCliente, idEquipamento);
    }

    public Linha travar(Connection c, int idReserva) throws SQLException {
        List<Linha> l = listar(c, "SELECT r.id_reserva, r.id_cliente, r.id_equipamento, r.inicio_previsto, r.fim_previsto "
                + "FROM reserva AS r WHERE r.id_reserva = ? AND r.status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA') "
                + "FOR UPDATE", idReserva);
        if (l.isEmpty()) {
            throw new ErroBanco("Essa reserva não está mais ativa.");
        }
        return l.get(0);
    }

    public void alterarHorario(Connection c, int idReserva, LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        Sql.atualizar(c, "UPDATE reserva SET inicio_previsto = ?, fim_previsto = ? WHERE id_reserva = ?",
                inicio, fim, idReserva);
    }

    public int cancelar(Connection c, int idReserva) throws SQLException {
        return Sql.atualizar(c, "UPDATE reserva SET status_reserva = 'CANCELADA' "
                + "WHERE id_reserva = ? AND status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA')", idReserva);
    }

    /** Reservas ativas com início em [inicio, fim). */
    public List<Linha> ativasNoPeriodo(Connection c, LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        return listar(c, "SELECT r.id_reserva, r.id_cliente, r.id_equipamento, r.inicio_previsto, r.fim_previsto "
                + "FROM reserva AS r WHERE r.status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA') "
                + "AND r.inicio_previsto >= ? AND r.inicio_previsto < ? ORDER BY r.inicio_previsto", inicio, fim);
    }

    public Set<LocalDate> diasComReserva(Connection c, LocalDate primeiroDia, LocalDate diaSeguinteAoUltimo) throws SQLException {
        Set<LocalDate> dias = new TreeSet<>();
        for (Linha l : ativasNoPeriodo(c, primeiroDia.atStartOfDay(), diaSeguinteAoUltimo.atStartOfDay())) {
            dias.add(l.inicio.toLocalDate());
        }
        return dias;
    }

    /**
     * Próxima reserva ativa (ainda sem sessão) do equipamento que ainda não terminou: inclui a que
     * já começou e cujo cliente ainda não chegou (senão outra pessoa ocuparia o horário reservado).
     */
    public Optional<Linha> proximaAtiva(Connection c, int idEquipamento, LocalDateTime agora) throws SQLException {
        return listar(c, "SELECT r.id_reserva, r.id_cliente, r.id_equipamento, r.inicio_previsto, r.fim_previsto "
                + "FROM reserva AS r WHERE r.id_equipamento = ? "
                + "AND r.status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA') "
                + "AND r.id_sessao IS NULL AND r.fim_previsto > ? "
                + "ORDER BY r.inicio_previsto LIMIT 1", idEquipamento, agora).stream().findFirst();
    }

    /**
     * Reserva ativa do cliente neste equipamento "para agora": começa em até J minutos ou já
     * começou e ainda não terminou. É ela que vira a sessão quando o cliente chega.
     */
    public Optional<Linha> doClienteParaAgora(Connection c, int idEquipamento, int idCliente, LocalDateTime agora,
                                              int janelaMinutos) throws SQLException {
        return listar(c, "SELECT r.id_reserva, r.id_cliente, r.id_equipamento, r.inicio_previsto, r.fim_previsto "
                + "FROM reserva AS r WHERE r.id_equipamento = ? AND r.id_cliente = ? "
                + "AND r.status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA') AND r.id_sessao IS NULL "
                + "AND r.inicio_previsto < ? AND r.fim_previsto > ? "
                + "ORDER BY r.inicio_previsto LIMIT 1 FOR UPDATE",
                idEquipamento, idCliente, agora.plusMinutes(janelaMinutos), agora).stream().findFirst();
    }

    /** A reserva virou sessão: liga as duas e marca a reserva como CONCLUIDA. */
    public void vincularSessao(Connection c, int idReserva, int idSessao) throws SQLException {
        Sql.atualizar(c, "UPDATE reserva SET id_sessao = ?, status_reserva = 'CONCLUIDA' WHERE id_reserva = ?",
                idSessao, idReserva);
    }

    /** Entrada paga: a reserva passa de PENDENTE_ENTRADA para CONFIRMADA. */
    public void confirmarEntrada(Connection c, int idReserva) throws SQLException {
        Sql.atualizar(c, "UPDATE reserva SET status_reserva = 'CONFIRMADA' WHERE id_reserva = ?", idReserva);
    }

    /** Total já pago de entrada na reserva que virou esta sessão (0 se não veio de reserva). */
    public BigDecimal entradaPagaDaSessao(Connection c, int idSessao) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT COALESCE(SUM(p.valor_pago), 0) FROM reserva AS r "
                + "INNER JOIN pagamento AS p ON p.id_reserva = r.id_reserva WHERE r.id_sessao = ?", idSessao);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    /** Fim previsto da sessão FECHADA em andamento no equipamento (vazio se não houver). */
    public Optional<LocalDateTime> sessaoFechadaAte(Connection c, int idEquipamento) throws SQLException {
        try (PreparedStatement ps = Sql.preparar(c, "SELECT s.horario_previsto FROM sessao AS s WHERE s.id_equipamento = ? "
                + "AND s.horario_fim IS NULL AND s.tipo_tempo = 'FECHADO'", idEquipamento);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.ofNullable(Sql.dataHora(rs, "horario_previsto")) : Optional.empty();
        }
    }

    /** Há sessão ABERTA (sem hora para acabar) em andamento no equipamento? */
    public boolean sessaoAbertaEmAndamento(Connection c, int idEquipamento) throws SQLException {
        return Sql.contar(c, "SELECT COUNT(*) FROM sessao WHERE id_equipamento = ? AND horario_fim IS NULL "
                + "AND tipo_tempo = 'ABERTO'", idEquipamento) > 0;
    }

    private List<Linha> listar(Connection c, String sql, Object... params) throws SQLException {
        List<Linha> lista = new ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql, params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Linha l = new Linha();
                l.idReserva = rs.getInt("id_reserva");
                l.idCliente = rs.getInt("id_cliente");
                l.idEquipamento = rs.getInt("id_equipamento");
                l.inicio = Sql.dataHora(rs, "inicio_previsto");
                l.fim = Sql.dataHora(rs, "fim_previsto");
                lista.add(l);
            }
        }
        return lista;
    }
}
