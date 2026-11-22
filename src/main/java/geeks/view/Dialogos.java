package geeks.view;

import geeks.model.Cliente;
import geeks.model.Dispositivo;
import geeks.model.Reserva;
import geeks.model.Sessao;

import javax.swing.JComponent;
import javax.swing.JDialog;
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

/** Janelas de diálogo do sistema, todas no visual da Geeks (nenhum JOptionPane padrão). */
public final class Dialogos {
    private Dialogos() {
    }

    // ---------- Formulários ----------

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

    public static Reserva escolherReserva(Component pai, String titulo, String mensagem, String textoConfirmar,
                                          List<Reserva> reservas) {
        FormEscolherReserva form = new FormEscolherReserva(titulo, mensagem, textoConfirmar, reservas);
        return exibir(pai, titulo, form) ? form.getSelecionada() : null;
    }

    // ---------- Confirmações e avisos ----------

    public static boolean confirmarEncerramento(Component pai, Sessao sessao) {
        return exibir(pai, "Encerrar sessão", new FormEncerrarSessao(sessao));
    }

    /** destrutivo = true usa o tom de alerta (vermelho), por exemplo para cancelamentos. */
    public static boolean confirmar(Component pai, String titulo, String mensagem, String textoConfirmar, boolean destrutivo) {
        FormMensagem form = new FormMensagem(titulo, mensagem, destrutivo ? FormMensagem.Tom.ALERTA : FormMensagem.Tom.INFO,
                destrutivo ? Icone.Tipo.ALERTA : Icone.Tipo.INFO, textoConfirmar, true);
        return exibir(pai, titulo, form);
    }

    public static void info(Component pai, String mensagem) {
        exibir(pai, "Aviso", new FormMensagem("Aviso", mensagem, FormMensagem.Tom.INFO, Icone.Tipo.INFO, "Entendi", false));
    }

    public static void sucesso(Component pai, String mensagem) {
        exibir(pai, "Tudo certo", new FormMensagem("Tudo certo", mensagem, FormMensagem.Tom.SUCESSO, Icone.Tipo.CHECK, "Ótimo", false));
    }

    public static void erro(Component pai, String mensagem) {
        exibir(pai, "Atenção", new FormMensagem("Atenção", mensagem, FormMensagem.Tom.ALERTA, Icone.Tipo.ALERTA, "Entendi", false));
    }

    public static void mostrarCliente(Component pai, Cliente c) {
        exibir(pai, "Dados do usuário", new FormDadosCliente(c));
    }

    // ---------- Janela modal ----------

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
        List<Image> icones = Tema.iconesDaJanela();
        if (!icones.isEmpty()) {
            dialogo.setIconImages(icones);
        }
        dialogo.setResizable(false);
        dialogo.pack();
        dialogo.setLocationRelativeTo(pai);
        form.animarEntrada();
        dialogo.setVisible(true);
        return confirmado[0];
    }
}
