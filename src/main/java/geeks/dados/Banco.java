package geeks.dados;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inicialização do banco: Flyway (com o usuário migrator) e depois o pool HikariCP
 * (com o usuário da aplicação, que não tem DDL). A aplicação nunca usa root.
 */
public final class Banco {
    private static final Logger LOG = Logger.getLogger(Banco.class.getName());
    private static volatile HikariDataSource dataSource;

    private Banco() {
    }

    /** Roda as migrations pendentes com o usuário migrator. */
    public static MigrateResult migrar(ConfigBanco config) {
        Flyway flyway = Flyway.configure()
                .dataSource(config.getUrl(), config.getUsuarioMigrator(), config.getSenhaMigrator())
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .connectRetries(0)
                .load();
        MigrateResult r = flyway.migrate();
        LOG.info(() -> "Flyway: " + r.migrationsExecuted + " migration(s) aplicada(s); versão do banco: "
                + (r.targetSchemaVersion == null ? flyway.info().current().getVersion() : r.targetSchemaVersion));
        return r;
    }

    /** Cria o pool de conexões do usuário da aplicação. */
    public static HikariDataSource criarPool(ConfigBanco config) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.getUrl());
        hc.setUsername(config.getUsuarioApp());
        hc.setPassword(config.getSenhaApp());
        hc.setMinimumIdle(config.getPoolMinimo());
        hc.setMaximumPoolSize(config.getPoolMaximo());
        hc.setPoolName("geeks-app");
        hc.setConnectionTimeout(8_000);
        hc.setAutoCommit(true);
        return new HikariDataSource(hc);
    }

    /** Inicialização completa usada pelo MainGeeks. */
    public static void iniciar(ConfigBanco config) {
        migrar(config);
        HikariDataSource ds = criarPool(config);
        try (Connection c = ds.getConnection()) {
            c.isValid(3);
        } catch (SQLException e) {
            ds.close();
            throw ErroBanco.de(e);
        }
        usar(ds);
    }

    /** Define o pool usado pelos DAOs (os testes usam isto com o banco geeks_test). */
    public static synchronized void usar(HikariDataSource ds) {
        if (dataSource != null && dataSource != ds) {
            dataSource.close();
        }
        dataSource = ds;
    }

    public static DataSource dataSource() {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            throw new ErroBanco("O banco de dados não foi iniciado.");
        }
        return ds;
    }

    public static Connection conexao() {
        try {
            return dataSource().getConnection();
        } catch (SQLException e) {
            throw ErroBanco.de(e);
        }
    }

    public static synchronized void fechar() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (RuntimeException e) {
                LOG.log(Level.FINE, "erro ao fechar o pool", e);
            }
            dataSource = null;
        }
    }
}
