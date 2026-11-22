package geeks.view;

import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class BotaoGeeks extends JButton {
    public enum Estilo { PREENCHIDO, CONTORNO, ESCURO, LINK }

    private static final int ATRASO_ACAO_MS = 170; // deixa a animação aparecer antes da ação (ex.: abrir diálogo)

    private final Estilo estilo;
    private final EfeitoClique efeito = new EfeitoClique(this);
    private boolean passando;

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
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    boolean cheio = BotaoGeeks.this.estilo == Estilo.PREENCHIDO;
                    efeito.iniciar(e.getPoint(), cheio ? Tema.BRANCO : Tema.AMARELO, cheio ? Tema.FUNDO : Tema.AMARELO);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                passando = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                passando = false;
                repaint();
            }
        });
    }

    /** Ao pressionar, o botão "afunda" um pouco (feedback tátil). */
    @Override
    public void paint(Graphics g0) {
        if (getModel().isPressed() && isEnabled()) {
            Graphics2D g = (Graphics2D) g0.create();
            g.translate(getWidth() / 2.0, getHeight() / 2.0);
            g.scale(0.965, 0.92);
            g.translate(-getWidth() / 2.0, -getHeight() / 2.0);
            super.paint(g);
            g.dispose();
        } else {
            super.paint(g0);
        }
    }

    /** Se a animação de clique está rodando, a ação é disparada logo depois dela começar. */
    @Override
    protected void fireActionPerformed(ActionEvent e) {
        if (efeito.emAndamento()) {
            Timer t = new Timer(ATRASO_ACAO_MS, ev -> super.fireActionPerformed(e));
            t.setRepeats(false);
            t.start();
        } else {
            super.fireActionPerformed(e);
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
                g.setColor(!isEnabled() ? new Color(0x3A3A3A) : passando ? new Color(0xFFE55C) : Tema.AMARELO);
                g.fillRoundRect(0, 0, w, h, h, h);
                break;
            case ESCURO:
                g.setColor(Tema.FUNDO);
                g.fillRoundRect(0, 0, w, h, h, h);
                break;
            case CONTORNO:
                if (passando && isEnabled()) {
                    g.setColor(new Color(255, 214, 10, 32));
                    g.fillRoundRect(0, 0, w, h, h, h);
                }
                g.setColor(isEnabled() ? Tema.AMARELO : new Color(0x555555));
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, w - 3, h - 3, h, h);
                break;
            default:
                break;
        }
        efeito.pintar(g, new RoundRectangle2D.Double(0, 0, w, h, h, h));
        g.dispose();
        super.paintComponent(g0);
    }
}
