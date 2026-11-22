package geeks.controller;

import geeks.model.LoginModel;
import geeks.model.Usuario;
import geeks.view.TelaLogin;

import java.util.Arrays;

/** Conecta a tela de login ao modelo de autenticação e, se der certo, abre o sistema. */
public class LoginController {
    private final LoginModel model;
    private final TelaLogin view;

    public LoginController() {
        this.model = new LoginModel();
        this.view = new TelaLogin();
        this.view.addListenerEntrar(e -> entrar());
        this.view.setVisible(true);
    }

    private void entrar() {
        char[] senha = view.getSenha();
        try {
            Usuario usuario = model.autenticar(view.getUsuario(), senha);
            if (usuario == null) {
                view.mostrarErro("Usuário ou senha incorretos.");
                view.limparSenha();
                return;
            }
            view.entrando(() -> {
                view.dispose();
                new GeeksController(usuario);
            });
        } catch (IllegalArgumentException ex) {
            view.mostrarErro(ex.getMessage());
        } finally {
            Arrays.fill(senha, '\0'); // não deixa a senha na memória
        }
    }
}
