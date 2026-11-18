package geeks.view;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Base dos formulários em janela (nova sessão, reserva...): fundo preto, título com faixa amarela,
 * logo "g" sobre a mancha amarela do canto e rodapé com resumo e botões.
 */
public abstract class FormularioGeeks extends JPanel {
    private final JPanel corpo = Componentes.transparente(new BorderLayout());
    private final JLabel resumo = Componentes.rotulo(" ", Font.PLAIN, 14, Tema.CINZA);
    private final BotaoGeeks btConfirmar;
    private Runnable aoConfirmar = () -> { };
    private Runnable aoCancelar = () -> { };

    protected FormularioGeeks(String titulo, String textoConfirmar, Icone.Tipo icone) {
        super(new BorderLayout(0, 18));
        setBorder(new EmptyBorder(24, 28, 22, 28));

        JPanel cabecalho = Componentes.transparente(new BorderLayout());
        cabecalho.add(new TituloGeeks(titulo), BorderLayout.WEST);
        ImageIcon logo = Tema.imagem("/img/g_geeks_logo.png", 40);
        if (logo != null) {
            cabecalho.add(new JLabel(logo), BorderLayout.EAST);
        }

        btConfirmar = new BotaoGeeks(textoConfirmar, icone, BotaoGeeks.Estilo.PREENCHIDO);
        BotaoGeeks btCancelar = new BotaoGeeks("Cancelar", null, BotaoGeeks.Estilo.CONTORNO);
        btConfirmar.addActionListener(e -> aoConfirmar.run());
        btCancelar.addActionListener(e -> aoCancelar.run());
        JPanel botoes = Componentes.transparente(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botoes.add(btCancelar);
        botoes.add(btConfirmar);
        JPanel rodape = Componentes.transparente(new BorderLayout(0, 12));
        rodape.add(resumo, BorderLayout.NORTH);
        rodape.add(botoes, BorderLayout.SOUTH);

        add(cabecalho, BorderLayout.NORTH);
        add(corpo, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    protected JPanel getCorpo() {
        return corpo;
    }

    protected void setResumo(String texto) {
        resumo.setText(texto);
    }

    protected void setConfirmarHabilitado(boolean habilitado) {
        btConfirmar.setEnabled(habilitado);
    }

    protected JComponent secao(String texto, Icone.Tipo icone) {
        return Componentes.rotuloIcone(texto, icone, Tema.BRANCO, 16);
    }

    /** Agrupa um título de seção e o seu conteúdo. */
    protected JPanel bloco(String titulo, Icone.Tipo icone, JComponent conteudo) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 8));
        p.add(secao(titulo, icone), BorderLayout.NORTH);
        p.add(conteudo, BorderLayout.CENTER);
        return p;
    }

    public void setAoConfirmar(Runnable r) {
        this.aoConfirmar = r;
    }

    public void setAoCancelar(Runnable r) {
        this.aoCancelar = r;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Tema.FUNDO);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Tema.AMARELO);
        g.fillOval(getWidth() - 106, -22, 124, 124);
        g.dispose();
    }
}
