package geeks.view;

import javax.swing.JPasswordField;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * Campo de senha com a mesma aparência do CampoGeeks e um "olho" à direita para mostrar/ocultar
 * a senha (vale para o login, o cadastro de funcionário e a troca de senha).
 */
public class SenhaGeeks extends JPasswordField {
    private static final int OLHO = 22;
    private static final int MARGEM_OLHO = 12;
    private final EstiloCampo estilo;
    private final char ecoPadrao;
    private boolean visivel;
    private boolean sobreOlho;

    public SenhaGeeks(String dica) {
        this.estilo = new EstiloCampo(this, dica);
        this.ecoPadrao = getEchoChar();
        reservarEspacoDoOlho();
        setToolTipText(null);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (noOlho(e.getX())) {
                    alternar();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sobreOlho = false;
                setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean agora = noOlho(e.getX());
                if (agora != sobreOlho) {
                    sobreOlho = agora;
                    setCursor(Cursor.getPredefinedCursor(agora ? Cursor.HAND_CURSOR : Cursor.TEXT_CURSOR));
                    setToolTipText(agora ? (visivel ? "Ocultar senha" : "Mostrar senha") : null);
                    repaint();
                }
            }
        });
    }

    public void setIconeEsquerda(Icone.Tipo icone) {
        estilo.setIcone(icone);
        reservarEspacoDoOlho();
    }

    /** Mostra ou oculta a senha digitada. */
    public void alternar() {
        visivel = !visivel;
        setEchoChar(visivel ? (char) 0 : ecoPadrao);
        setToolTipText(visivel ? "Ocultar senha" : "Mostrar senha");
        repaint();
    }

    private void reservarEspacoDoOlho() {
        Insets in = getBorder() == null ? new Insets(11, 14, 11, 14) : getBorder().getBorderInsets(this);
        setBorder(new EmptyBorder(in.top, in.left, in.bottom, OLHO + MARGEM_OLHO * 2));
    }

    private boolean noOlho(int x) {
        return x >= getWidth() - OLHO - MARGEM_OLHO * 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        estilo.pintarFundo(g);
        super.paintComponent(g);
        estilo.pintarDica(g);
        pintarOlho(g);
    }

    private void pintarOlho(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double x = getWidth() - MARGEM_OLHO - OLHO;
        double y = (getHeight() - OLHO * 0.62) / 2;
        double w = OLHO;
        double h = OLHO * 0.62;
        g.setColor(sobreOlho || visivel ? Tema.AMARELO : new Color(0x8A8A8A));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double olho = new Path2D.Double();
        olho.moveTo(x, y + h / 2);
        olho.quadTo(x + w / 2, y - h / 2, x + w, y + h / 2);
        olho.quadTo(x + w / 2, y + h * 1.5, x, y + h / 2);
        g.draw(olho);
        double r = h * 0.62;
        g.fill(new Ellipse2D.Double(x + w / 2 - r / 2, y + h / 2 - r / 2, r, r));
        if (!visivel) { // risco = senha escondida
            g.draw(new java.awt.geom.Line2D.Double(x + 2, y + h + 3, x + w - 2, y - 3));
        }
        g.dispose();
    }
}
