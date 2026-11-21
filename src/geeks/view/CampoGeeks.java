package geeks.view;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Campo de texto escuro, arredondado, com borda amarela quando em foco e texto de dica. */
public class CampoGeeks extends JTextField {
    private final String dica;
    private Icone.Tipo icone;
    private float foco;
    private javax.swing.Timer timerFoco;

    public CampoGeeks(String dica) {
        this.dica = dica;
        setOpaque(false);
        setForeground(Tema.BRANCO);
        setCaretColor(Tema.AMARELO);
        setFont(Tema.fonte(Font.PLAIN, 16));
        setBorder(new EmptyBorder(11, 14, 11, 14));
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                animarFoco(1f);
            }

            @Override
            public void focusLost(FocusEvent e) {
                animarFoco(0f);
            }
        });
    }

    private void animarFoco(float alvo) {
        if (timerFoco != null) {
            timerFoco.stop();
        }
        final float de = foco;
        timerFoco = Animacao.executar(170, 0, t -> {
            foco = (float) (de + (alvo - de) * t);
            repaint();
        }, null);
    }

    public void setIconeEsquerda(Icone.Tipo icone) {
        this.icone = icone;
        setBorder(new EmptyBorder(11, 42, 11, 14));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Tema.CAMPO);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        if (foco > 0) {
            g.setColor(new Color(255, 214, 10, (int) (50 * foco)));
            g.setStroke(new java.awt.BasicStroke(6f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
        }
        g.setColor(Tema.misturar(new Color(0x333333), Tema.AMARELO, foco));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
        if (icone != null) {
            new Icone(icone, 20, Tema.AMARELO).paintIcon(this, g, 13, (getHeight() - 20) / 2);
        }
        g.dispose();
        super.paintComponent(g0);
        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D h = (Graphics2D) g0.create();
            h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            h.setColor(new Color(0x777777));
            h.setFont(getFont());
            h.drawString(dica, getInsets().left, (getHeight() + h.getFontMetrics().getAscent() - h.getFontMetrics().getDescent()) / 2);
            h.dispose();
        }
    }
}
