package geeks.view;

import geeks.model.Cliente;
import geeks.model.Dispositivo;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.Component;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/** Caixas de diálogo simples usadas pelo Controller. */
public final class Dialogos {
    private Dialogos() {
    }

    /** Retorna {Cliente, Dispositivo} ou null se o usuário cancelar. */
    public static Object[] pedirNovaSessao(Component pai, List<Cliente> clientes, List<Dispositivo> livres) {
        JComboBox<Cliente> cbCliente = new JComboBox<>(clientes.toArray(new Cliente[0]));
        JComboBox<Dispositivo> cbDispositivo = new JComboBox<>(livres.toArray(new Dispositivo[0]));
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("Cliente"));
        p.add(cbCliente);
        p.add(new JLabel("Dispositivo livre"));
        p.add(cbDispositivo);
        int r = JOptionPane.showConfirmDialog(pai, p, "Nova sessão", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION ? new Object[]{cbCliente.getSelectedItem(), cbDispositivo.getSelectedItem()} : null;
    }

    /** Retorna {Cliente, Dispositivo, LocalTime} ou null se o usuário cancelar. */
    public static Object[] pedirReserva(Component pai, List<Cliente> clientes, List<Dispositivo> dispositivos, LocalDate dia) {
        JComboBox<Cliente> cbCliente = new JComboBox<>(clientes.toArray(new Cliente[0]));
        JComboBox<Dispositivo> cbDispositivo = new JComboBox<>(dispositivos.toArray(new Dispositivo[0]));
        JSpinner hora = spinnerHora(LocalTime.of(14, 0));
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("Cliente"));
        p.add(cbCliente);
        p.add(new JLabel("Dispositivo"));
        p.add(cbDispositivo);
        p.add(new JLabel("Horário em " + Formatador.dataExtenso(dia)));
        p.add(hora);
        int r = JOptionPane.showConfirmDialog(pai, p, "Registrar reserva", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION
                ? new Object[]{cbCliente.getSelectedItem(), cbDispositivo.getSelectedItem(), lerHora(hora)} : null;
    }

    public static LocalTime pedirHorario(Component pai, LocalTime atual) {
        JSpinner hora = spinnerHora(atual);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("Novo horário"));
        p.add(hora);
        int r = JOptionPane.showConfirmDialog(pai, p, "Alterar reserva", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION ? lerHora(hora) : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T escolher(Component pai, String titulo, String mensagem, List<T> itens) {
        Object r = JOptionPane.showInputDialog(pai, mensagem, titulo, JOptionPane.QUESTION_MESSAGE, null,
                itens.toArray(), itens.get(0));
        return (T) r;
    }

    public static boolean confirmar(Component pai, String mensagem) {
        return JOptionPane.showConfirmDialog(pai, mensagem, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static void info(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Geeks", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void erro(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    public static void mostrarCliente(Component pai, Cliente c) {
        String texto = "Nome: " + c.getNome() + "\nIdade: " + c.getIdade() + "\nTelefone: " + c.getTelefone()
                + "\nCPF: " + Formatador.cpf(c.getCpf())
                + (c.getResponsavel().isEmpty() ? "" : "\nResponsável: " + c.getResponsavel())
                + "\nPlano: " + c.getPlano().getNome();
        JOptionPane.showMessageDialog(pai, texto, "Dados do usuário", JOptionPane.PLAIN_MESSAGE);
    }

    private static JSpinner spinnerHora(LocalTime hora) {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm"));
        s.setValue(Date.from(LocalDateTime.of(LocalDate.now(), hora).atZone(ZoneId.systemDefault()).toInstant()));
        return s;
    }

    private static LocalTime lerHora(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
    }
}
