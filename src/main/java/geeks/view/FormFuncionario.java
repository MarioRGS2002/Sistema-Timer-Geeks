package geeks.view;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Formulário "Novo funcionário" (gerente): nome, login, senha e se é gerente.
 * Também usado só para senha ("Trocar senha"), com os outros campos escondidos.
 */
public class FormFuncionario extends FormularioGeeks {
    private static final int SENHA_MINIMA = 8;
    private final CampoGeeks campoNome = new CampoGeeks("Nome completo");
    private final CampoGeeks campoLogin = new CampoGeeks("ex.: maria");
    private final SenhaGeeks campoSenha = new SenhaGeeks("Mínimo " + SENHA_MINIMA + " caracteres");
    private final SenhaGeeks campoConfirmacao = new SenhaGeeks("Repita a senha");
    private final CaixaMarcacao chkGerente = new CaixaMarcacao("É gerente (vê o faturamento e gerencia funcionários)");
    private final boolean soSenha;

    /** Cadastro completo. */
    public FormFuncionario() {
        this("Novo funcionário", "Cadastrar", false, null);
    }

    /** Só a senha (trocar a senha de alguém). */
    public static FormFuncionario trocarSenha(String nomeFuncionario) {
        return new FormFuncionario("Trocar senha", "Salvar senha", true, nomeFuncionario);
    }

    private FormFuncionario(String titulo, String textoConfirmar, boolean soSenha, String nomeFuncionario) {
        super(titulo, textoConfirmar, Icone.Tipo.CHECK);
        this.soSenha = soSenha;
        campoSenha.setIconeEsquerda(Icone.Tipo.CADEADO);
        campoConfirmacao.setIconeEsquerda(Icone.Tipo.CADEADO);

        JPanel campos = Componentes.transparente(new GridLayout(0, 1, 0, 12));
        if (soSenha) {
            campos.add(Componentes.rotulo("Nova senha de " + nomeFuncionario, Font.BOLD, 16, Tema.BRANCO));
        } else {
            campos.add(comRotulo("Nome", campoNome));
            campos.add(comRotulo("Login (usado para entrar no sistema)", campoLogin));
        }
        JPanel senhas = Componentes.transparente(new GridLayout(1, 2, 16, 0));
        senhas.add(comRotulo("Senha", campoSenha));
        senhas.add(comRotulo("Confirmar senha", campoConfirmacao));
        campos.add(senhas);
        if (!soSenha) {
            campos.add(chkGerente);
        }
        JPanel envolve = Componentes.transparente(new BorderLayout());
        envolve.add(campos, BorderLayout.NORTH);
        envolve.setPreferredSize(new java.awt.Dimension(560, envolve.getPreferredSize().height));
        getCorpo().add(envolve, BorderLayout.CENTER);

        DocumentListener aoDigitar = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                atualizar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                atualizar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                atualizar();
            }
        };
        for (javax.swing.text.JTextComponent c : new javax.swing.text.JTextComponent[]{campoNome, campoLogin, campoSenha, campoConfirmacao}) {
            c.getDocument().addDocumentListener(aoDigitar);
        }
        atualizar();
    }

    private JComponent comRotulo(String rotulo, JComponent campo) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 6));
        p.add(Componentes.rotulo(rotulo, Font.BOLD, 14, Tema.BRANCO), BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    /** Dica no rodapé; a validação definitiva é feita no back-end (FuncionariosModel). */
    private void atualizar() {
        String msg = null;
        if (!soSenha && campoNome.getText().isBlank()) {
            msg = "Informe o nome.";
        } else if (!soSenha && !campoLogin.getText().trim().toLowerCase().matches("[a-z0-9._-]{3,50}")) {
            msg = "Login: 3 a 50 letras minúsculas, números, ponto, _ ou - (sem espaço).";
        } else if (campoSenha.getPassword().length < SENHA_MINIMA) {
            msg = "A senha precisa ter pelo menos " + SENHA_MINIMA + " caracteres.";
        } else if (!java.util.Arrays.equals(campoSenha.getPassword(), campoConfirmacao.getPassword())) {
            msg = "As senhas não conferem.";
        }
        setConfirmarHabilitado(msg == null);
        setResumo(msg == null ? "Tudo certo." : msg);
    }

    public String getNome() {
        return campoNome.getText();
    }

    public String getLogin() {
        return campoLogin.getText();
    }

    public boolean isGerente() {
        return chkGerente.isSelected();
    }

    /** Quem chama deve apagar o vetor depois de usar. */
    public char[] getSenha() {
        return campoSenha.getPassword();
    }

    public char[] getConfirmacao() {
        return campoConfirmacao.getPassword();
    }

    /** Apaga as senhas dos campos. */
    public void limparSenhas() {
        campoSenha.setText("");
        campoConfirmacao.setText("");
    }
}
