package geeks.view;

import geeks.model.Dispositivo;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Grade de cartões para escolher o dispositivo (os que estão em uso aparecem bloqueados). */
public class SeletorDispositivo extends JPanel {
    private final ButtonGroup grupo = new ButtonGroup();
    private final List<Cartao> cartoes = new ArrayList<>();

    public SeletorDispositivo(List<Dispositivo> dispositivos, Predicate<Dispositivo> ocupado) {
        super(new GridLayout(0, 3, 10, 10));
        setOpaque(false);
        for (Dispositivo d : dispositivos) {
            Cartao c = new Cartao(d, ocupado.test(d));
            cartoes.add(c);
            grupo.add(c);
            add(c);
        }
    }

    public Dispositivo getSelecionado() {
        for (Cartao c : cartoes) {
            if (c.isSelected()) {
                return c.dispositivo;
            }
        }
        return null;
    }

    public void addListenerSelecao(Runnable r) {
        cartoes.forEach(c -> c.addActionListener(e -> r.run()));
    }

    private static class Cartao extends JToggleButton {
        private final Dispositivo dispositivo;
        private final boolean ocupado;
        private final EfeitoClique efeito = new EfeitoClique(this);
        private boolean passando;

        Cartao(Dispositivo dispositivo, boolean ocupado) {
            this.dispositivo = dispositivo;
            this.ocupado = ocupado;
            setEnabled(!ocupado);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setPreferredSize(new Dimension(190, 64));
            setCursor(Cursor.getPredefinedCursor(ocupado ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        efeito.iniciar(e.getPoint(), Tema.AMARELO, Tema.AMARELO);
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

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean sel = isSelected();
            g.setColor(ocupado ? new Color(0x131313) : sel ? new Color(255, 214, 10, 40) : Tema.CARTAO);
            g.fillRoundRect(0, 0, w - 1, h - 1, 22, 22);
            efeito.pintar(g, new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 22, 22));
            g.setColor(sel ? Tema.AMARELO : ocupado ? new Color(0x262626) : passando ? new Color(0x5A5A5A) : Tema.BORDA);
            g.setStroke(new java.awt.BasicStroke(sel ? 2.5f : 1.5f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 22, 22);

            Color corBloco = Tema.corDe(dispositivo.getTipo());
            g.setColor(ocupado ? new Color(0x3A3A3A) : corBloco);
            g.fillRoundRect(12, (h - 40) / 2, 40, 40, 14, 14);
            new Icone(Tema.iconeDe(dispositivo.getTipo()), 22, ocupado ? new Color(0x777777) : Tema.FUNDO)
                    .paintIcon(this, g, 21, (h - 22) / 2);

            g.setFont(Tema.fonte(Font.BOLD, 15));
            g.setColor(ocupado ? new Color(0x777777) : Tema.BRANCO);
            g.drawString(dispositivo.getNome(), 64, h / 2 - 3);
            g.setFont(Tema.fonte(Font.PLAIN, 13));
            g.setColor(ocupado ? Tema.VERMELHO : Tema.CINZA);
            g.drawString(ocupado ? "Em uso agora" : Formatador.moeda(dispositivo.getTipo().getValorHora()) + "/hora", 64, h / 2 + 15);
            if (sel) {
                new Icone(Icone.Tipo.CHECK, 16, Tema.AMARELO).paintIcon(this, g, w - 28, h - 26);
            }
            g.dispose();
        }
    }
}
