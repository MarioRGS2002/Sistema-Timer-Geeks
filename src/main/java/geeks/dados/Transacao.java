package geeks.dados;

import java.sql.Connection;
import java.sql.SQLException;

/** Executa um bloco numa transação JDBC (autocommit desligado; commit ou rollback completo). */
public final class Transacao {
    private Transacao() {
    }

    @FunctionalInterface
    public interface Bloco<T> {
        T executar(Connection c) throws SQLException;
    }

    public static <T> T executar(Bloco<T> bloco) {
        try (Connection c = Banco.conexao()) {
            boolean autoCommitAntes = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T r = bloco.executar(c);
                c.commit();
                return r;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommitAntes);
            }
        } catch (SQLException e) {
            throw ErroBanco.de(e);
        }
    }

    /** Executa fora de transação explícita (uma conexão do pool, autocommit). */
    public static <T> T consultar(Bloco<T> bloco) {
        try (Connection c = Banco.conexao()) {
            return bloco.executar(c);
        } catch (SQLException e) {
            throw ErroBanco.de(e);
        }
    }
}
