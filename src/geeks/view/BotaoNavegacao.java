package geeks.view;

import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** Item do menu lateral: vira uma pílula amarela quando selecionado. */
public class BotaoNavegacao extends JToggleButton {
    private final EfeitoClique efeito = new EfeitoClique(this);

    public BotaoNavegacao(String texto, Icone.Tipo icone) {
        super(texto);
        setIcon(new Icone(icone, 22));
        setIconTextGap(12);
        setHorizontalAlignment(LEFT);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Tema.fonte(Font.BOLD, 17));
        setBorder(new EmptyBorder(12, 16, 12, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boolean selecionado = isSelected();
                efeito.iniciar(e.getPoint(), selecionado ? Tema.BRANCO : Tema.AMARELO, selecionado ? Tema.FUNDO : Tema.AMARELO);
            }
        });
    }

    @Override
    public Color getForeground() {
        return isSelected() ? Tema.FUNDO : new Color(0xD6D6D6);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        efeito.pintar(g, new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
        g.dispose();
        super.paintComponent(g0);
    }
}
