package geeks.view;

import javax.swing.JFrame;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/** Janela de login do sistema Geeks. */
public class TelaLogin extends JFrame {
    private final PainelLogin painel = new PainelLogin();

    public TelaLogin() throws HeadlessException {
        setTitle("Geeks - Autenticação");
        setContentPane(painel);
        List<Image> icones = Tema.iconesDaJanela();
        if (!icones.isEmpty()) {
            setIconImages(icones);
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // botão de maximizar (tela cheia) na barra de título; não encolhe além do tamanho normal
        setResizable(true);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(painel.getBotaoEntrar()); // Enter também entra
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                painel.focarUsuario();
            }
        });
    }

    public String getUsuario() {
        return painel.getUsuario();
    }

    public char[] getSenha() {
        return painel.getSenha();
    }

    public void mostrarErro(String mensagem) {
        painel.mostrarErro(mensagem);
    }

    public void limparSenha() {
        painel.limparSenha();
    }

    /** Mostra "Entrando..." por um instante e depois executa a ação (abrir o sistema). */
    public void entrando(Runnable depois) {
        painel.setEntrando(true);
        Animacao.executar(450, 0, p -> { }, depois);
    }

    public void addListenerEntrar(ActionListener listener) {
        painel.addListenerEntrar(listener);
    }
}
