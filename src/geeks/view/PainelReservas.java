package geeks.view;

import geeks.model.Reserva;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

public class PainelReservas extends PainelPagina {
    private final JLabel lbMes = Componentes.rotulo("", Font.BOLD, 24, Tema.BRANCO);
    private final BotaoDia[] dias = new BotaoDia[42];
    private final BotaoGeeks btAnterior = new BotaoGeeks("<", null, BotaoGeeks.Estilo.CONTORNO);
    private final BotaoGeeks btProximo = new BotaoGeeks(">", null, BotaoGeeks.Estilo.CONTORNO);
    private final BotaoGeeks btRegistrar = new BotaoGeeks("Registrar reserva", Icone.Tipo.MAIS, BotaoGeeks.Estilo.PREENCHIDO);
    private final BotaoGeeks btGerenciar = new BotaoGeeks("Gerenciar reservas", Icone.Tipo.LISTA, BotaoGeeks.Estilo.CONTORNO);
    private final BotaoGeeks btAlterar = new BotaoGeeks("Alterar reserva", Icone.Tipo.LAPIS, BotaoGeeks.Estilo.CONTORNO);
    private final JLabel lbDia = Componentes.rotuloIcone("", Icone.Tipo.CALENDARIO, Tema.AMARELO, 20);
    private final JPanel listaReservas = Componentes.transparente(new GridLayout(0, 1, 0, 12));

    public PainelReservas() {
        super("Reservas");
        JPanel corpo = Componentes.transparente(new GridLayout(1, 2, 26, 0));

        CartaoGeeks calendario = new CartaoGeeks();
        calendario.setLayout(new BorderLayout(0, 12));
        calendario.setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel nav = Componentes.transparente(new BorderLayout());
        lbMes.setHorizontalAlignment(SwingConstants.CENTER);
        nav.add(btAnterior, BorderLayout.WEST);
        nav.add(lbMes, BorderLayout.CENTER);
        nav.add(btProximo, BorderLayout.EAST);
        JPanel grade = Componentes.transparente(new GridLayout(7, 7, 5, 5));
        for (String s : new String[]{"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"}) {
            JLabel l = Componentes.rotulo(s, Font.BOLD, 14, Tema.CINZA);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            grade.add(l);
        }
        for (int i = 0; i < dias.length; i++) {
            dias[i] = new BotaoDia();
            grade.add(dias[i]);
        }
        calendario.add(nav, BorderLayout.NORTH);
        calendario.add(grade, BorderLayout.CENTER);

        JPanel direita = Componentes.transparente(new BorderLayout(0, 14));
        JPanel botoes = Componentes.transparente(new GridLayout(0, 1, 0, 10));
        botoes.add(btRegistrar);
        botoes.add(btGerenciar);
        botoes.add(btAlterar);
        CartaoGeeks cartaoDia = new CartaoGeeks();
        cartaoDia.setLayout(new BorderLayout(0, 14));
        cartaoDia.setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel envoltorio = Componentes.transparente(new BorderLayout());
        envoltorio.add(listaReservas, BorderLayout.NORTH);
        cartaoDia.add(lbDia, BorderLayout.NORTH);
        cartaoDia.add(envoltorio, BorderLayout.CENTER);
        direita.add(botoes, BorderLayout.NORTH);
        direita.add(cartaoDia, BorderLayout.CENTER);

        corpo.add(calendario);
        corpo.add(direita);
        getCorpo().add(corpo, BorderLayout.CENTER);
    }

    public void atualizarCalendario(YearMonth mes, LocalDate selecionado, Set<LocalDate> diasComReserva) {
        lbMes.setText(Formatador.mes(mes));
        LocalDate inicio = mes.atDay(1).minusDays(mes.atDay(1).getDayOfWeek().getValue() - 1);
        for (int i = 0; i < dias.length; i++) {
            LocalDate d = inicio.plusDays(i);
            dias[i].configurar(d, YearMonth.from(d).equals(mes), d.equals(selecionado), diasComReserva.contains(d));
        }
    }

    public void atualizarReservasDoDia(LocalDate dia, List<Reserva> reservas) {
        lbDia.setText(Formatador.dataExtenso(dia));
        listaReservas.removeAll();
        if (reservas.isEmpty()) {
            listaReservas.add(Componentes.rotulo("Nenhuma reserva neste dia.", Font.PLAIN, 16, Tema.CINZA));
        }
        for (Reserva r : reservas) {
            JPanel linha = Componentes.transparente(new BorderLayout(12, 0));
            linha.add(new Componentes.BlocoIcone(r.getDispositivo().getTipo(), 44), BorderLayout.WEST);
            JPanel textos = Componentes.transparente(new GridLayout(2, 1));
            textos.add(Componentes.rotulo(r.getCliente().getNome(), Font.BOLD, 16, Tema.BRANCO));
            textos.add(Componentes.rotulo(r.getDispositivo().getNome(), Font.PLAIN, 14, Tema.CINZA));
            linha.add(textos, BorderLayout.CENTER);
            linha.add(Componentes.rotulo(r.getDataHora().toLocalTime().toString(), Font.BOLD, 18, Tema.AMARELO), BorderLayout.EAST);
            listaReservas.add(linha);
        }
        listaReservas.revalidate();
        listaReservas.repaint();
    }

    public void addListenerMesAnterior(ActionListener l) {
        btAnterior.addActionListener(l);
    }

    public void addListenerProximoMes(ActionListener l) {
        btProximo.addActionListener(l);
    }

    /** O actionCommand do evento é a data clicada (formato yyyy-MM-dd). */
    public void addListenerSelecionarDia(ActionListener l) {
        for (BotaoDia d : dias) {
            d.addActionListener(l);
        }
    }

    public void addListenerRegistrar(ActionListener l) {
        btRegistrar.addActionListener(l);
    }

    public void addListenerGerenciar(ActionListener l) {
        btGerenciar.addActionListener(l);
    }

    public void addListenerAlterar(ActionListener l) {
        btAlterar.addActionListener(l);
    }

    private static class BotaoDia extends JButton {
        private int numero;
        private boolean doMes;
        private boolean selecionado;
        private boolean temReserva;

        BotaoDia() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Tema.fonte(Font.BOLD, 16));
            setPreferredSize(new Dimension(44, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void configurar(LocalDate data, boolean doMes, boolean selecionado, boolean temReserva) {
            this.numero = data.getDayOfMonth();
            this.doMes = doMes;
            this.selecionado = selecionado;
            this.temReserva = temReserva;
            setActionCommand(data.toString());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (selecionado) {
                g.setColor(Tema.AMARELO);
                g.fillRoundRect(0, 0, w, h, 14, 14);
            }
            g.setColor(selecionado ? Tema.FUNDO : doMes ? Tema.BRANCO : new Color(0x6F6F6F));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            String t = String.valueOf(numero);
            g.drawString(t, (w - fm.stringWidth(t)) / 2, h / 2 + 2);
            if (temReserva && doMes) {
                g.setColor(selecionado ? Tema.FUNDO : Tema.AMARELO);
                g.fillOval(w / 2 - 3, h - 12, 6, 6);
            }
            g.dispose();
        }
    }
}
