package geeks.view;

import geeks.model.Cliente;
import geeks.model.Plano;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Locale;

/** Busca + lista de clientes com avatar, nome e CPF. */
public class SeletorCliente extends JPanel {
    private static final int ALTURA_LINHA = 54;

    private final List<Cliente> todos;
    private final DefaultListModel<Cliente> modelo = new DefaultListModel<>();
    private final JList<Cliente> lista = new JList<>(modelo);
    private final CampoGeeks busca = new CampoGeeks("Buscar cliente por nome ou CPF");

    public SeletorCliente(List<Cliente> clientes, int linhasVisiveis) {
        super(new BorderLayout(0, 10));
        this.todos = clientes;
        setOpaque(false);
        busca.setIconeEsquerda(Icone.Tipo.LUPA);

        lista.setOpaque(false);
        lista.setFixedCellHeight(ALTURA_LINHA);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new Linha());
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
        moldura.setPreferredSize(new Dimension(590, linhasVisiveis * ALTURA_LINHA + 14));

        add(busca, BorderLayout.NORTH);
        add(moldura, BorderLayout.CENTER);
        filtrar();
        busca.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });
    }

    private void filtrar() {
        Cliente anterior = lista.getSelectedValue();
        String t = busca.getText().trim().toLowerCase(Locale.ROOT);
        String numeros = t.replaceAll("\\D", "");
        modelo.clear();
        for (Cliente c : todos) {
            boolean porNome = c.getNome().toLowerCase(Locale.ROOT).contains(t);
            boolean porCpf = !numeros.isEmpty() && c.getCpf().contains(numeros);
            if (t.isEmpty() || porNome || porCpf) {
                modelo.addElement(c);
            }
        }
        if (anterior != null && modelo.contains(anterior)) {
            lista.setSelectedValue(anterior, true);
        }
    }

    public Cliente getSelecionado() {
        return lista.getSelectedValue();
    }

    public void addListenerSelecao(Runnable r) {
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                r.run();
            }
        });
    }

    private static class Linha extends JPanel implements ListCellRenderer<Cliente> {
        private Cliente cliente;
        private boolean selecionada;

        Linha() {
            setOpaque(false);
        }

        @Override
        public java.awt.Component getListCellRendererComponent(JList<? extends Cliente> list, Cliente value, int index,
                                                               boolean isSelected, boolean cellHasFocus) {
            this.cliente = value;
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
            Color texto = selecionada ? Tema.FUNDO : Tema.BRANCO;
            if (selecionada) {
                g.setColor(Tema.AMARELO);
                g.fillRoundRect(2, 3, w - 4, h - 6, 18, 18);
            }
            g.setColor(selecionada ? Tema.FUNDO : Tema.AMARELO);
            g.fillOval(12, (h - 36) / 2, 36, 36);
            g.setColor(selecionada ? Tema.AMARELO : Tema.FUNDO);
            g.setFont(Tema.fonte(Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            String inicial = cliente.getNome().substring(0, 1).toUpperCase(Locale.ROOT);
            g.drawString(inicial, 12 + (36 - fm.stringWidth(inicial)) / 2, (h - 36) / 2 + (36 + fm.getAscent() - fm.getDescent()) / 2);

            g.setColor(texto);
            g.setFont(Tema.fonte(Font.BOLD, 16));
            g.drawString(cliente.getNome(), 60, h / 2 - 2);
            g.setColor(selecionada ? new Color(0x3A3A3A) : Tema.CINZA);
            g.setFont(Tema.fonte(Font.PLAIN, 13));
            g.drawString("CPF " + Formatador.cpf(cliente.getCpf()), 60, h / 2 + 16);
            if (cliente.getPlano() != Plano.NENHUM) {
                new Icone(Icone.Tipo.COROA, 20, selecionada ? Tema.FUNDO : Tema.AMARELO).paintIcon(this, g, w - 38, (h - 20) / 2);
            }
            g.dispose();
        }
    }
}
