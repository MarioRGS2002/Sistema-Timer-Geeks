package geeks.view;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Menu lateral: a pílula amarela desliza (com leve exagero) até o item escolhido. */
public class MenuLateral extends JPanel {
    private Component alvo;
    private double yAtual;
    private boolean animando;

    public MenuLateral() {
        setBackground(Tema.LATERAL);
    }

    public void mover(Component novo) {
        if (alvo == null || !isShowing()) {
            alvo = novo;
            animando = false;
            repaint();
            return;
        }
        final double de = animando ? yAtual : alvo.getY();
        alvo = novo;
        animando = true;
        Animacao.executar(320, 0, p -> {
            yAtual = de + (novo.getY() - de) * Animacao.elastico(p);
            repaint();
        }, () -> {
            animando = false;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (alvo == null) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Tema.AMARELO);
        double y = animando ? yAtual : alvo.getY();
        g.fillRoundRect(alvo.getX(), (int) y, alvo.getWidth(), alvo.getHeight(), 16, 16);
        g.dispose();
    }
}
