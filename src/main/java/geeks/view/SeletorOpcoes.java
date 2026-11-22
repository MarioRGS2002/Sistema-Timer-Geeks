package geeks.view;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Escolha de UMA opção em "pílulas" (mesmo visual dos filtros do Histórico).
 * Usado para forma de pagamento, tempo da sessão e duração da reserva.
 */
public class SeletorOpcoes extends JPanel {
    private final List<Pilula> pilulas = new ArrayList<>();
    private final List<Runnable> ouvintes = new ArrayList<>();

    public SeletorOpcoes(List<String> opcoes, int selecionada) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 0));
        setOpaque(false);
        ButtonGroup grupo = new ButtonGroup();
        for (int i = 0; i < opcoes.size(); i++) {
            Pilula p = new Pilula(opcoes.get(i));
            p.setSelected(i == selecionada);
            p.addActionListener(e -> ouvintes.forEach(Runnable::run));
            grupo.add(p);
            pilulas.add(p);
            add(p);
        }
    }

    /** Índice da opção escolhida (-1 se nenhuma). */
    public int getIndice() {
        for (int i = 0; i < pilulas.size(); i++) {
            if (pilulas.get(i).isSelected()) {
                return i;
            }
        }
        return -1;
    }

    public String getSelecionada() {
        int i = getIndice();
        return i < 0 ? null : pilulas.get(i).getText();
    }

    public void addListenerSelecao(Runnable r) {
        ouvintes.add(r);
    }

    @Override
    public void setEnabled(boolean habilitado) {
        super.setEnabled(habilitado);
        pilulas.forEach(p -> p.setEnabled(habilitado));
    }

    private static class Pilula extends JToggleButton {
        private final EfeitoClique efeito = new EfeitoClique(this);

        Pilula(String texto) {
            super(texto);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Tema.fonte(Font.BOLD, 14));
            setBorder(new EmptyBorder(7, 16, 7, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        efeito.iniciar(e.getPoint(), isSelected() ? Tema.BRANCO : Tema.AMARELO, isSelected() ? Tema.FUNDO : Tema.AMARELO);
                    }
                }
            });
        }

        @Override
        public Color getForeground() {
            if (!isEnabled()) {
                return new Color(0x666666);
            }
            return isSelected() ? Tema.FUNDO : Tema.BRANCO;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected() && isEnabled()) {
                g.setColor(Tema.AMARELO);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            } else {
                g.setColor(isSelected() ? new Color(0x777777) : new Color(0x444444));
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight(), getHeight());
            }
            efeito.pintar(g, new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
