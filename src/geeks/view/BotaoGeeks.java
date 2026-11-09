package geeks.view;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class BotaoGeeks extends JButton {
    public enum Estilo { PREENCHIDO, CONTORNO, ESCURO, LINK }

    private final Estilo estilo;

    public BotaoGeeks(String texto, Icone.Tipo icone, Estilo estilo) {
        super(texto);
        this.estilo = estilo;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Tema.fonte(Font.BOLD, 15));
        setBorder(new EmptyBorder(estilo == Estilo.LINK ? 4 : 10, estilo == Estilo.LINK ? 4 : 22,
                estilo == Estilo.LINK ? 4 : 10, estilo == Estilo.LINK ? 4 : 22));
        setIconTextGap(8);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (icone != null) {
            setIcon(new Icone(icone, 18));
            setDisabledIcon(new Icone(icone, 18));
        }
    }

    @Override
    public Color getForeground() {
        if (!isEnabled()) {
            return new Color(0x8A8A8A);
        }
        return estilo == Estilo.PREENCHIDO ? Tema.FUNDO : Tema.AMARELO;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        switch (estilo) {
            case PREENCHIDO:
                g.setColor(isEnabled() ? Tema.AMARELO : new Color(0x3A3A3A));
                g.fillRoundRect(0, 0, w, h, h, h);
                break;
            case ESCURO:
                g.setColor(Tema.FUNDO);
                g.fillRoundRect(0, 0, w, h, h, h);
                break;
            case CONTORNO:
                g.setColor(isEnabled() ? Tema.AMARELO : new Color(0x555555));
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, w - 3, h - 3, h, h);
                break;
            default:
                break;
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
