package geeks;

import geeks.controller.LoginController;
import geeks.dados.Banco;
import geeks.dados.ConfigBanco;
import geeks.dados.ErroBanco;
import geeks.view.Dialogos;
import org.flywaydb.core.api.FlywayException;

import javax.swing.SwingUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainGeeks {
    private static final Logger LOG = Logger.getLogger(MainGeeks.class.getName());
    private static volatile long ultimoAviso;
    private static volatile String ultimaMensagem = "";

    public static void main(String[] args) {
        // Antes do login: migrations (usuário migrator) e pool de conexões (usuário da aplicação).
        try {
            Banco.iniciar(ConfigBanco.carregar());
        } catch (ErroBanco | FlywayException e) {
            LOG.log(Level.SEVERE, "Falha ao iniciar o banco de dados", e);
            String detalhe = e instanceof FlywayException
                    ? "Falha ao atualizar a estrutura do banco (migration):\n" + primeiraLinha(e.getMessage())
                    : e.getMessage();
            falharEsair("Não foi possível iniciar o banco de dados.\n\n" + detalhe
                    + "\n\nVerifique se o MySQL está ligado e se o db.properties está correto.");
            return;
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Falha ao iniciar o banco de dados", e);
            falharEsair("Não foi possível iniciar o banco de dados.\n\n" + primeiraLinha(String.valueOf(e.getMessage()))
                    + "\n\nVerifique se o MySQL está ligado e se o db.properties está correto.");
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(Banco::fechar));
        Thread.setDefaultUncaughtExceptionHandler(MainGeeks::tratarErro);
        SwingUtilities.invokeLater(LoginController::new);
    }

    /** Erros de banco/permissão que escaparem das telas aparecem no diálogo de erro do sistema. */
    private static void tratarErro(Thread t, Throwable e) {
        Throwable raiz = e;
        while (!(raiz instanceof ErroBanco || raiz instanceof SecurityException) && raiz.getCause() != null) {
            raiz = raiz.getCause();
        }
        LOG.log(Level.SEVERE, "Erro não tratado em " + t.getName(), e);
        String msg = raiz instanceof ErroBanco || raiz instanceof SecurityException
                ? raiz.getMessage()
                : "Ocorreu um erro inesperado. Tente de novo.";
        long agora = System.currentTimeMillis();
        if (msg.equals(ultimaMensagem) && agora - ultimoAviso < 60_000) {
            return; // não repete o mesmo aviso (ex.: atualização automática a cada 30 s)
        }
        ultimaMensagem = msg;
        ultimoAviso = agora;
        SwingUtilities.invokeLater(() -> Dialogos.erro(null, msg));
    }

    private static void falharEsair(String mensagem) {
        try {
            SwingUtilities.invokeAndWait(() -> Dialogos.erro(null, mensagem));
        } catch (Exception ex) {
            System.err.println(mensagem);
        }
        System.exit(2);
    }

    private static String primeiraLinha(String s) {
        if (s == null) {
            return "";
        }
        String l = s.strip();
        int i = l.indexOf('\n');
        return i > 0 ? l.substring(0, i) : l;
    }
}
