package geeks.dados;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuração de conexão lida de um arquivo .properties (db.properties).
 * O arquivo fica FORA do Git; o modelo commitado é db.properties.example.
 *
 * <p>Procura, nesta ordem: a propriedade de sistema {@code geeks.db.config}, a pasta atual,
 * a pasta do sistema-geeks.jar e a pasta da instalação na loja
 * ({@code C:\ProgramData\SistemaGeeks}, criada pelo "Configurar Sistema Geeks").</p>
 */
public final class ConfigBanco {
    private final String url;
    private final String usuarioMigrator;
    private final String senhaMigrator;
    private final String usuarioApp;
    private final String senhaApp;
    private final int poolMinimo;
    private final int poolMaximo;
    private final Path origem;

    private ConfigBanco(Properties p, Path origem) {
        this.url = obrigatorio(p, "db.url");
        this.usuarioMigrator = obrigatorio(p, "db.migrator.usuario");
        this.senhaMigrator = obrigatorio(p, "db.migrator.senha");
        this.usuarioApp = obrigatorio(p, "db.app.usuario");
        this.senhaApp = obrigatorio(p, "db.app.senha");
        this.poolMinimo = Integer.parseInt(p.getProperty("db.pool.minimo", "2").trim());
        this.poolMaximo = Integer.parseInt(p.getProperty("db.pool.maximo", "5").trim());
        this.origem = origem;
    }

    /** Carrega o db.properties da aplicação. */
    public static ConfigBanco carregar() {
        return carregar("db.properties");
    }

    /** Existe um db.properties em algum dos lugares onde o sistema procura? (false = primeira vez neste PC) */
    public static boolean existe() {
        return candidatos("db.properties").stream().anyMatch(Files::isRegularFile);
    }

    /** Carrega um arquivo de configuração pelo nome (ex.: "db.test.properties"). */
    public static ConfigBanco carregar(String nomeArquivo) {
        for (Path c : candidatos(nomeArquivo)) {
            if (Files.isRegularFile(c)) {
                return deArquivo(c);
            }
        }
        throw new ErroBanco("O sistema ainda não foi configurado neste computador.\n"
                + "Abra \"Configurar Sistema Geeks\" no menu Iniciar (uma vez só) e depois abra o sistema de novo.");
    }

    private static List<Path> candidatos(String nomeArquivo) {
        List<Path> candidatos = new ArrayList<>();
        String explicito = System.getProperty("geeks.db.config");
        if (explicito != null && !explicito.isBlank() && "db.properties".equals(nomeArquivo)) {
            candidatos.add(Paths.get(explicito));
        }
        candidatos.add(Paths.get(nomeArquivo).toAbsolutePath());
        Path pastaJar = pastaDoJar();
        if (pastaJar != null) {
            candidatos.add(pastaJar.resolve(nomeArquivo));
            candidatos.add(pastaJar.getParent() == null ? pastaJar.resolve(nomeArquivo) : pastaJar.getParent().resolve(nomeArquivo));
        }
        candidatos.add(pastaInstalacao().resolve(nomeArquivo));
        return candidatos;
    }

    /** Pasta da configuração na loja: %ProgramData%\SistemaGeeks (fora da pasta do programa, que é só leitura). */
    public static Path pastaInstalacao() {
        String programData = System.getenv("ProgramData");
        return Paths.get(programData == null || programData.isBlank() ? "C:\\ProgramData" : programData, "SistemaGeeks");
    }

    public static ConfigBanco deArquivo(Path arquivo) {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            throw new ErroBanco("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        }
        // remove o BOM que alguns editores do Windows colocam no início do arquivo
        Properties limpo = new Properties();
        p.forEach((k, v) -> limpo.put(k.toString().replace("﻿", "").trim(), v.toString().trim()));
        return new ConfigBanco(limpo, arquivo);
    }

    private static Path pastaDoJar() {
        try {
            Path p = Paths.get(ConfigBanco.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(p) ? p.getParent() : p;
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            return null;
        }
    }

    private static String obrigatorio(Properties p, String chave) {
        String v = p.getProperty(chave);
        if (v == null || v.isBlank() || v.startsWith("TROQUE")) {
            throw new ErroBanco("Preencha \"" + chave + "\" no arquivo de configuração do banco.");
        }
        return v.trim();
    }

    /** Nome do banco na URL (ex.: geeks ou geeks_test). */
    public String nomeDoBanco() {
        String semParametros = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        return semParametros.substring(semParametros.lastIndexOf('/') + 1);
    }

    /** Mesma URL apontando para outro banco (usado no reset do banco de teste). */
    public String urlSemBanco() {
        String parametros = url.contains("?") ? url.substring(url.indexOf('?')) : "";
        String semParametros = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        return semParametros.substring(0, semParametros.lastIndexOf('/') + 1) + parametros;
    }

    public String getUrl() {
        return url;
    }

    public String getUsuarioMigrator() {
        return usuarioMigrator;
    }

    public String getSenhaMigrator() {
        return senhaMigrator;
    }

    public String getUsuarioApp() {
        return usuarioApp;
    }

    public String getSenhaApp() {
        return senhaApp;
    }

    public int getPoolMinimo() {
        return poolMinimo;
    }

    public int getPoolMaximo() {
        return poolMaximo;
    }

    public Path getOrigem() {
        return origem;
    }
}
