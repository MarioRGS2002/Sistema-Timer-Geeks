package geeks.tools;

import geeks.dados.ConfigBanco;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Executa um arquivo .sql inteiro (vários comandos) com o usuário migrator. */
public final class ExecutorSql {
    private ExecutorSql() {
    }

    public static void executarArquivo(ConfigBanco config, Path arquivo) throws IOException, SQLException {
        String sql = Files.readString(arquivo, StandardCharsets.UTF_8).replace("﻿", "");
        String url = config.getUrl() + (config.getUrl().contains("?") ? "&" : "?") + "allowMultiQueries=true";
        try (Connection c = DriverManager.getConnection(url, config.getUsuarioMigrator(), config.getSenhaMigrator());
             Statement st = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                boolean temResultado = st.execute(sql);
                while (temResultado || st.getUpdateCount() != -1) {
                    temResultado = st.getMoreResults();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
