package geeks.view;

import geeks.model.Reserva;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/** Lista de reservas para escolher uma (cancelar ou alterar). */
public class FormEscolherReserva extends FormularioGeeks {
    private static final int ALTURA_LINHA = 58;

    private final JList<Reserva> lista;

    public FormEscolherReserva(String titulo, String mensagem, String textoConfirmar, List<Reserva> reservas) {
        super(titulo, textoConfirmar, Icone.Tipo.CHECK);
        DefaultListModel<Reserva> modelo = new DefaultListModel<>();
        reservas.forEach(modelo::addElement);
        lista = new JList<>(modelo);
        lista.setOpaque(false);
        lista.setFixedCellHeight(ALTURA_LINHA);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new Linha());
        lista.setSelectedIndex(0);
        lista.addListSelectionListener(e -> setConfirmarHabilitado(lista.getSelectedValue() != null));

        JScrollPane rolagem = new JScrollPane(lista);
        rolagem.setBorder(null);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        ScrollGeeks.aplicar(rolagem.getVerticalScrollBar());

        CartaoGeeks moldura = new CartaoGeeks(Tema.CAMPO, new Color(0x333333), false);
        moldura.setLayout(new BorderLayout());
        moldura.setBorder(new EmptyBorder(6, 6, 6, 6));
        moldura.add(rolagem, BorderLayout.CENTER);
        moldura.setPreferredSize(new Dimension(520, Math.min(3, reservas.size()) * ALTURA_LINHA + 14));

        JPanel envolve = Componentes.transparente(new BorderLayout(0, 12));
        envolve.add(Componentes.rotulo(mensagem, Font.PLAIN, 16, Tema.CINZA), BorderLayout.NORTH);
        envolve.add(moldura, BorderLayout.CENTER);
        getCorpo().add(envolve, BorderLayout.CENTER);
        setResumo(" ");
    }

    public Reserva getSelecionada() {
        return lista.getSelectedValue();
    }

    private static class Linha extends JPanel implements ListCellRenderer<Reserva> {
        private Reserva reserva;
        private boolean selecionada;

        Linha() {
            setOpaque(false);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Reserva> list, Reserva value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            this.reserva = value;
            this.selecionada = isSelected;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, ALTURA_LINHA);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (selecionada) {
                g.setColor(Tema.AMARELO);
                g.fillRoundRect(2, 3, w - 4, h - 6, 18, 18);
            }
            g.setColor(selecionada ? Tema.FUNDO : Tema.corDe(reserva.getDispositivo().getTipo()));
            g.fillRoundRect(12, (h - 40) / 2, 40, 40, 14, 14);
            new Icone(Tema.iconeDe(reserva.getDispositivo().getTipo()), 22,
                    selecionada ? Tema.AMARELO : Tema.FUNDO).paintIcon(this, g, 21, (h - 22) / 2);
            g.setFont(Tema.fonte(Font.BOLD, 16));
            g.setColor(selecionada ? Tema.FUNDO : Tema.BRANCO);
            g.drawString(reserva.getCliente().getNome(), 64, h / 2 - 3);
            g.setFont(Tema.fonte(Font.PLAIN, 13));
            g.setColor(selecionada ? new Color(0x3A3A3A) : Tema.CINZA);
            g.drawString(reserva.getDispositivo().getNome(), 64, h / 2 + 15);
            g.setFont(Tema.fonte(Font.BOLD | Font.ITALIC, 24));
            g.setColor(selecionada ? Tema.FUNDO : Tema.AMARELO);
            FontMetrics fm = g.getFontMetrics();
            String hora = reserva.getDataHora().toLocalTime().toString();
            g.drawString(hora, w - 16 - fm.stringWidth(hora), (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }
}
