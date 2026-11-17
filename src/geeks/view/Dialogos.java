package geeks.view;

import geeks.model.Cliente;
import geeks.model.Dispositivo;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Predicate;

/** Janelas de diálogo usadas pelo Controller. */
public final class Dialogos {
    private Dialogos() {
    }

    /** Retorna {Cliente, Dispositivo} ou null se o usuário cancelar. */
    public static Object[] pedirNovaSessao(Component pai, List<Cliente> clientes, List<Dispositivo> dispositivos,
                                           Predicate<Dispositivo> ocupado) {
        FormNovaSessao form = new FormNovaSessao(clientes, dispositivos, ocupado);
        return exibir(pai, "Nova sessão", form) ? new Object[]{form.getCliente(), form.getDispositivo()} : null;
    }

    /** Retorna {Cliente, Dispositivo, LocalTime} ou null se o usuário cancelar. */
    public static Object[] pedirReserva(Component pai, List<Cliente> clientes, List<Dispositivo> dispositivos, LocalDate dia) {
        FormReserva form = new FormReserva(clientes, dispositivos, dia);
        return exibir(pai, "Registrar reserva", form) ? new Object[]{form.getCliente(), form.getDispositivo(), form.getHora()} : null;
    }

    public static LocalTime pedirHorario(Component pai, LocalTime atual) {
        FormHorario form = new FormHorario(atual);
        return exibir(pai, "Alterar reserva", form) ? form.getHora() : null;
    }

    private static boolean exibir(Component pai, String titulo, FormularioGeeks form) {
        Window dono = pai instanceof Window ? (Window) pai : SwingUtilities.getWindowAncestor(pai);
        JDialog dialogo = new JDialog(dono, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        boolean[] confirmado = {false};
        form.setAoConfirmar(() -> {
            confirmado[0] = true;
            dialogo.dispose();
        });
        form.setAoCancelar(dialogo::dispose);
        dialogo.setContentPane(form);
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        Image icone = Tema.imagemOriginal("/img/g_geeks_logo.png");
        if (icone != null) {
            dialogo.setIconImage(icone);
        }
        dialogo.setResizable(false);
        dialogo.pack();
        dialogo.setLocationRelativeTo(pai);
        dialogo.setVisible(true);
        return confirmado[0];
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
}
