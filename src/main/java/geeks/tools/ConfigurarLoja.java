package geeks.tools;

import geeks.dados.Banco;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Configuração da loja num computador novo ("Configurar Sistema Geeks" no menu Iniciar).
 *
 * <ul>
 *   <li>Sem argumentos: abre o assistente COM TELAS ({@link AssistenteConfiguracao}).</li>
 *   <li>Com {@code --console}: o mesmo passo a passo em modo texto (para suporte técnico).</li>
 * </ul>
 * As duas formas usam o mesmo {@link ServicoConfiguracao}: as regras ficam num lugar só.
 */
public final class ConfigurarLoja {
    private final ServicoConfiguracao servico = new ServicoConfiguracao();
    private final Console console = System.console();
    private final BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private final PrintWriter out = console != null ? console.writer() : new PrintWriter(System.out, true);

    private ConfigurarLoja() {
    }

    /** Sem argumentos: assistente com telas. Com "--console": modo texto. */
    public static void main(String[] args) {
        if (!java.util.Arrays.asList(args).contains("--console")) {
            geeks.MainGeeks.abrirAssistente();
            return;
        }
        ConfigurarLoja c = new ConfigurarLoja();
        int codigo = 0;
        try {
            c.executar();
        } catch (Exception e) {
            c.out.println();
            c.out.println("ERRO: " + (e.getMessage() == null ? e : e.getMessage()));
            c.out.println("Nada foi perdido. Corrija o problema e rode de novo.");
            codigo = 1;
        } finally {
            Banco.fechar();
        }
        c.ler("\nAperte Enter para fechar.");
        System.exit(codigo);
    }

    private void executar() throws Exception {
        titulo("CONFIGURAR SISTEMA GEEKS (modo texto)");

        titulo("1/4  Banco de dados");
        if (servico.bancoPronto()) {
            out.println("O banco deste computador já está configurado.");
            servico.iniciarBanco();
        } else {
            for (int tentativa = 1; ; tentativa++) {
                String usuario = ler("Usuário administrador do MySQL [root]: ").trim();
                try {
                    servico.prepararBanco(usuario, lerSenha("Senha do MySQL (não aparece ao digitar): "));
                    break;
                } catch (IllegalArgumentException e) {
                    out.println(e.getMessage());
                    if (tentativa >= 3) {
                        throw e;
                    }
                }
            }
            out.println("Banco pronto. Configuração em " + servico.getArquivoConfig());
        }

        titulo("2/4  Gerente");
        String gerente = servico.gerenteExistente();
        out.println(gerente == null ? "Cadastre o gerente." : "Já existe o gerente " + gerente + ".");
        while (gerente == null || sim("Cadastrar outro gerente? (s/N): ", false)) {
            try {
                servico.cadastrarGerente(ler("Login (ex.: maria): "), ler("Nome completo: "),
                        lerSenha("Senha (mínimo 8 caracteres): "), lerSenha("Repita a senha: "));
                gerente = servico.gerenteExistente();
                out.println("Gerente salvo.");
                if (!sim("Cadastrar mais um? (s/N): ", false)) {
                    break;
                }
            } catch (IllegalArgumentException e) {
                out.println(e.getMessage() + " Vamos tentar de novo.");
            }
        }

        titulo("3/4  Equipamentos");
        List<String> tipos = servico.tiposDeEquipamento();
        out.println("Já cadastrados: " + String.join(", ", servico.equipamentos()));
        List<String[]> novos = new ArrayList<>();
        while (true) {
            String nome = ler("Nome do equipamento (Enter vazio para terminar): ").trim();
            if (nome.isEmpty()) {
                break;
            }
            for (int i = 0; i < tipos.size(); i++) {
                out.println("  " + (i + 1) + ") " + tipos.get(i));
            }
            try {
                ServicoConfiguracao.validarEquipamento(nome);
                int n = Integer.parseInt(ler("Tipo (número): ").trim());
                novos.add(new String[]{nome, tipos.get(n - 1)});
            } catch (RuntimeException e) {
                out.println("Não entendi o tipo ou o nome. Tente de novo.");
            }
        }
        if (!novos.isEmpty()) {
            out.println(servico.cadastrarEquipamentos(novos) + " equipamento(s) cadastrado(s).");
        }

        titulo("4/4  Backup automático");
        if (!ServicoConfiguracao.seteZipInstalado()) {
            out.println("O 7-Zip não está instalado: instale (7-zip.org) e rode de novo para ligar o backup.");
        } else if (sim("Fazer backup automático todo dia? (S/n): ", true)) {
            String destino = ler("Pasta do backup [C:\\GeeksBackup]: ").trim();
            String hora = ler("Horário [23:30]: ").trim();
            String senha = servico.configurarBackup(destino.isEmpty() ? "C:\\GeeksBackup" : destino,
                    hora.isEmpty() ? "23:30" : hora);
            out.println("Senha que abre os backups (GUARDE num lugar seguro): " + senha);
        }

        titulo("PRONTO");
        out.println("Abra o sistema pelo atalho \"Sistema Geeks\".");
    }

    private void titulo(String t) {
        out.println();
        out.println("==== " + t + " ====");
    }

    private boolean sim(String pergunta, boolean padrao) {
        String r = ler(pergunta).trim().toLowerCase(Locale.ROOT);
        return r.isEmpty() ? padrao : r.startsWith("s");
    }

    private String ler(String rotulo) {
        try {
            if (console != null) {
                String s = console.readLine("%s", rotulo);
                return s == null ? "" : s;
            }
            out.print(rotulo);
            out.flush();
            String s = entrada.readLine();
            return s == null ? "" : s;
        } catch (IOException e) {
            return "";
        }
    }

    private char[] lerSenha(String rotulo) {
        if (console != null) {
            char[] s = console.readPassword("%s", rotulo);
            return s == null ? new char[0] : s;
        }
        return ler(rotulo).toCharArray();
    }
}
