package geeks.view;

import javax.swing.JCheckBox;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * Caixa de marcação no estilo Geeks (para uso sobre fundo amarelo): quadrado arredondado que se preenche
 * de preto, com o "check" amarelo sendo desenhado e um leve pulso ao marcar. Continua sendo um JCheckBox.
 */
public class CaixaMarcacao extends JCheckBox {
    private static final int LADO = 24;
    private static final int ESPACO = 12;

    private float marca;
    private float pulso;
    private boolean passando;
    private Timer timer;

    public CaixaMarcacao(String texto) {
        super(texto);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(Tema.fonte(Font.BOLD, 15));
        setForeground(Tema.FUNDO);
        setBorder(new EmptyBorder(8, 10, 8, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        marca = isSelected() ? 1f : 0f;
        addItemListener(e -> animar(isSelected()));
        addMouseListener(new MouseAdapter() {
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

    private void animar(boolean marcada) {
        if (timer != null) {
            timer.stop();
        }
        final float de = marca;
        timer = Animacao.executar(240, 0, p -> {
            marca = (float) (de + ((marcada ? 1 : 0) - de) * Animacao.suave(p));
            pulso = (float) Math.sin(p * Math.PI);
            repaint();
        }, () -> {
            marca = marcada ? 1f : 0f;
            pulso = 0f;
            repaint();
        });
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        Insets in = getInsets();
        return new Dimension(in.left + LADO + ESPACO + fm.stringWidth(getText()) + in.right,
                in.top + Math.max(LADO, fm.getHeight()) + in.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // realce discreto ao passar o mouse e anel de foco para o teclado
        if (passando) {
            g.setColor(new Color(13, 13, 13, 18));
            g.fillRoundRect(0, 0, w, h, 18, 18);
        }
        if (isFocusOwner()) {
            g.setColor(new Color(13, 13, 13, 110));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 18, 18);
        }

        // a caixa
        Graphics2D c = (Graphics2D) g.create();
        int x = getInsets().left;
        c.translate(x + LADO / 2.0, h / 2.0);
        double escala = 1 + 0.10 * pulso;
        c.scale(escala, escala);
        c.translate(-LADO / 2.0, -LADO / 2.0);
        c.setColor(new Color(13, 13, 13, (int) (255 * marca)));
        c.fillRoundRect(0, 0, LADO, LADO, 9, 9);
        c.setColor(Tema.FUNDO);
        c.setStroke(new BasicStroke(2.2f));
        c.drawRoundRect(1, 1, LADO - 2, LADO - 2, 9, 9);
        desenharCheck(c, marca);
        c.dispose();

        // o texto
        g.setColor(getForeground());
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        g.drawString(getText(), x + LADO + ESPACO, (h + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }

    /** Desenha o "check" aos poucos, de acordo com o progresso (0 a 1). */
    private void desenharCheck(Graphics2D g, float progresso) {
        if (progresso <= 0.02f) {
            return;
        }
        double[][] pts = {{6.5, 12.5}, {10.5, 16.5}, {17.5, 8}};
        double l1 = Math.hypot(pts[1][0] - pts[0][0], pts[1][1] - pts[0][1]);
        double l2 = Math.hypot(pts[2][0] - pts[1][0], pts[2][1] - pts[1][1]);
        double restante = progresso * (l1 + l2);
        Path2D.Double caminho = new Path2D.Double();
        caminho.moveTo(pts[0][0], pts[0][1]);
        if (restante <= l1) {
            double t = restante / l1;
            caminho.lineTo(pts[0][0] + (pts[1][0] - pts[0][0]) * t, pts[0][1] + (pts[1][1] - pts[0][1]) * t);
        } else {
            caminho.lineTo(pts[1][0], pts[1][1]);
            double t = (restante - l1) / l2;
            caminho.lineTo(pts[1][0] + (pts[2][0] - pts[1][0]) * t, pts[1][1] + (pts[2][1] - pts[1][1]) * t);
        }
        g.setColor(Tema.AMARELO);
        g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(caminho);
    }
}
