package geeks.view;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionListener;

/** Conteúdo da tela de login: painel animado à esquerda e formulário à direita. */
public class PainelLogin extends JPanel {
    private final PainelPlayStation esquerda = new PainelPlayStation();
    private final Formulario direita = new Formulario();
    private final CampoGeeks campoUsuario = new CampoGeeks("Seu usuário");
    private final SenhaGeeks campoSenha = new SenhaGeeks("Sua senha");
    private final AvisoErro aviso = new AvisoErro();
    private final BotaoGeeks btEntrar = new BotaoGeeks("Entrar", Icone.Tipo.RAIO, BotaoGeeks.Estilo.PREENCHIDO);

    public PainelLogin() {
        super(new BorderLayout());
        campoUsuario.setIconeEsquerda(Icone.Tipo.USUARIO_MAIS);
        campoSenha.setIconeEsquerda(Icone.Tipo.CADEADO);
        campoUsuario.setPreferredSize(new Dimension(408, 46));
        campoSenha.setPreferredSize(new Dimension(408, 46));
        btEntrar.setPreferredSize(new Dimension(408, 50));

        DocumentListener limpaAviso = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aviso.ocultar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aviso.ocultar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aviso.ocultar();
            }
        };
        campoUsuario.getDocument().addDocumentListener(limpaAviso);
        campoSenha.getDocument().addDocumentListener(limpaAviso);

        JPanel bloco = Componentes.transparente(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0;
        g.fill = GridBagConstraints.NONE;
        g.insets = new Insets(0, 0, 10, 0);
        bloco.add(new TituloGeeks("Entrar"), g);

        g.gridy = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 28, 0);
        bloco.add(Componentes.rotulo("Use seu usuário e senha para acessar o sistema.", Font.PLAIN, 15, Tema.CINZA), g);

        g.gridy = 2;
        g.insets = new Insets(0, 0, 6, 0);
        bloco.add(Componentes.rotulo("Usuário", Font.BOLD, 14, Tema.BRANCO), g);
        g.gridy = 3;
        g.insets = new Insets(0, 0, 16, 0);
        bloco.add(campoUsuario, g);
        g.gridy = 4;
        g.insets = new Insets(0, 0, 6, 0);
        bloco.add(Componentes.rotulo("Senha", Font.BOLD, 14, Tema.BRANCO), g);
        g.gridy = 5;
        g.insets = new Insets(0, 0, 6, 0);
        bloco.add(campoSenha, g);
        g.gridy = 6;
        g.insets = new Insets(0, 0, 12, 0);
        bloco.add(aviso, g);
        g.gridy = 7;
        g.insets = new Insets(0, 0, 0, 0);
        bloco.add(btEntrar, g);

        direita.setLayout(new GridBagLayout());
        direita.add(bloco);
        direita.setPreferredSize(new Dimension(520, 560));

        add(esquerda, BorderLayout.WEST);
        add(direita, BorderLayout.CENTER);
    }

    public String getUsuario() {
        return campoUsuario.getText();
    }

    public char[] getSenha() {
        return campoSenha.getPassword();
    }

    public void mostrarErro(String mensagem) {
        aviso.mostrar(mensagem);
        direita.agitar();
    }

    public void limparSenha() {
        campoSenha.setText("");
        campoSenha.requestFocusInWindow();
    }

    public void focarUsuario() {
        campoUsuario.requestFocusInWindow();
    }

    /** Estado de "entrando": bloqueia o botão e muda o texto. */
    public void setEntrando(boolean entrando) {
        btEntrar.setEnabled(!entrando);
        btEntrar.setText(entrando ? "Entrando..." : "Entrar");
    }

    public void addListenerEntrar(ActionListener l) {
        btEntrar.addActionListener(l);
    }

    public JButton getBotaoEntrar() {
        return btEntrar;
    }

    /** Lado direito: entra com fade e sobe levemente; balança de leve quando o login falha. */
    private static class Formulario extends JPanel {
        private float entrada = 1f;
        private double deslocamento;

        Formulario() {
            setBackground(Tema.FUNDO);
            setBorder(new EmptyBorder(0, 56, 0, 56));
        }

        @Override
        public void addNotify() {
            super.addNotify();
            entrada = 0f;
            Animacao.executar(420, 80, p -> {
                entrada = (float) Animacao.suave(p);
                repaint();
            }, () -> {
                entrada = 1f;
                repaint();
            });
        }

        void agitar() {
            Animacao.executar(420, 0, p -> {
                deslocamento = Math.sin(p * Math.PI * 6) * (1 - p) * 12;
                repaint();
            }, () -> {
                deslocamento = 0;
                repaint();
            });
        }

        @Override
        protected void paintChildren(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setComposite(AlphaComposite.SrcOver.derive(entrada));
            g.translate(deslocamento, (1 - entrada) * 16);
            super.paintChildren(g);
            g.dispose();
        }
    }
}
