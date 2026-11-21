package geeks.view;

import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Aparência compartilhada pelos campos da Geeks (texto e senha): fundo arredondado, brilho no foco e dica. */
final class EstiloCampo {
    private final JTextComponent campo;
    private final String dica;
    private Icone.Tipo icone;
    private float foco;
    private Timer timer;

    EstiloCampo(JTextComponent campo, String dica) {
        this.campo = campo;
        this.dica = dica;
        campo.setOpaque(false);
        campo.setForeground(Tema.BRANCO);
        campo.setCaretColor(Tema.AMARELO);
        campo.setFont(Tema.fonte(Font.PLAIN, 16));
        campo.setBorder(new EmptyBorder(11, 14, 11, 14));
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                animar(1f);
            }

            @Override
            public void focusLost(FocusEvent e) {
                animar(0f);
            }
        });
    }

    void setIcone(Icone.Tipo icone) {
        this.icone = icone;
        campo.setBorder(new EmptyBorder(11, 42, 11, 14));
    }

    private void animar(float alvo) {
        if (timer != null) {
            timer.stop();
        }
        final float de = foco;
        timer = Animacao.executar(170, 0, t -> {
            foco = (float) (de + (alvo - de) * t);
            campo.repaint();
        }, null);
    }

    /** Desenha o fundo, o brilho, a borda e o ícone (antes do texto). */
    void pintarFundo(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = campo.getWidth(), h = campo.getHeight();
        g.setColor(Tema.CAMPO);
        g.fillRoundRect(0, 0, w, h, 16, 16);
        if (foco > 0) {
            g.setColor(new Color(255, 214, 10, (int) (50 * foco)));
            g.setStroke(new BasicStroke(6f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
        }
        g.setColor(Tema.misturar(new Color(0x333333), Tema.AMARELO, foco));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
        if (icone != null) {
            new Icone(icone, 20, Tema.AMARELO).paintIcon(campo, g, 13, (h - 20) / 2);
        }
        g.dispose();
    }

    /** Desenha a dica quando o campo está vazio (depois do texto). */
    void pintarDica(Graphics g0) {
        if (campo.getDocument().getLength() == 0 && !campo.isFocusOwner()) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0x777777));
            g.setFont(campo.getFont());
            g.drawString(dica, campo.getInsets().left,
                    (campo.getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
            g.dispose();
        }
    }
}
