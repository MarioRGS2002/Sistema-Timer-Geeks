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

    /** Retorna {Cliente, Dispositivo, Integer minutos (null = livre)} ou null se o usuário cancelar. */
    public static Object[] pedirNovaSessao(Component pai, List<Cliente> clientes, List<Dispositivo> dispositivos,
                                           Predicate<Dispositivo> ocupado) {
        FormNovaSessao form = new FormNovaSessao(clientes, dispositivos, ocupado);
        return exibir(pai, "Nova sessão", form) ? new Object[]{form.getCliente(), form.getDispositivo(), form.getMinutos()} : null;
    }

    /**
     * Retorna {Cliente, Dispositivo, LocalTime, Integer minutos, String formaEntrada (null = não pagou)}
     * ou null se o usuário cancelar.
     */
    public static Object[] pedirReserva(Component pai, List<Cliente> clientes, List<Dispositivo> dispositivos, LocalDate dia,
                                        List<String> formas) {
        FormReserva form = new FormReserva(clientes, dispositivos, dia, formas);
        return exibir(pai, "Registrar reserva", form)
                ? new Object[]{form.getCliente(), form.getDispositivo(), form.getHora(), form.getMinutos(), form.getFormaEntrada()}
                : null;
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

    /** Formulário "Novo funcionário" preenchido, ou null se cancelar. Apague as senhas depois de usar. */
    public static FormFuncionario pedirNovoFuncionario(Component pai) {
        FormFuncionario form = new FormFuncionario();
        return exibir(pai, "Novo funcionário", form) ? form : null;
    }

    /** Formulário "Trocar senha" preenchido, ou null se cancelar. Apague as senhas depois de usar. */
    public static FormFuncionario pedirNovaSenha(Component pai, String nomeFuncionario) {
        FormFuncionario form = FormFuncionario.trocarSenha(nomeFuncionario);
        return exibir(pai, "Trocar senha", form) ? form : null;
    }

    // ---------- Confirmações e avisos ----------

    /** Formulário confirmado (forma de pagamento e uso do crédito), ou null se cancelar. */
    public static FormEncerrarSessao confirmarEncerramento(Component pai, Sessao sessao, List<String> formas,
                                                           java.util.function.BiFunction<Boolean, java.time.LocalDateTime,
                                                                   geeks.model.PreviaEncerramento> previa,
                                                           boolean podeCorrigirSaida) {
        FormEncerrarSessao form = new FormEncerrarSessao(sessao, formas, previa, podeCorrigirSaida);
        return exibir(pai, "Encerrar sessão", form) ? form : null;
    }

    /** {valor digitado, motivo} do estorno, ou null se cancelar. */
    public static String[] pedirEstorno(Component pai, geeks.dados.CaixaDAO.Pagamento p) {
        FormEstorno form = new FormEstorno(p);
        return exibir(pai, "Estornar pagamento", form) ? new String[]{form.getValor(), form.getMotivo()} : null;
    }

    /** Minutos a acrescentar numa sessão de tempo fechado, ou null se cancelar. */
    public static Integer pedirMaisTempo(Component pai, Sessao sessao) {
        FormAcrescentarTempo form = new FormAcrescentarTempo(sessao);
        return exibir(pai, "Mais tempo", form) ? form.getMinutos() : null;
    }

    /** Escolha da forma de pagamento de um valor (ex.: mensalidade do Clube). Retorna a forma ou null. */
    public static String pedirPagamento(Component pai, String titulo, String descricao, java.math.BigDecimal valor,
                                        List<String> formas, String textoConfirmar) {
        FormPagamento form = new FormPagamento(titulo, descricao, valor, formas, textoConfirmar);
        return exibir(pai, titulo, form) ? form.getForma() : null;
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

    /** Mostra a ficha; devolve true se o usuário clicou em "Editar dados". */
    public static boolean mostrarCliente(Component pai, geeks.model.FichaCliente ficha) {
        return exibir(pai, "Dados do usuário", new FormDadosCliente(ficha));
    }

    /** Formulário de edição preenchido, ou null se cancelar. */
    public static FormEditarCliente editarCliente(Component pai, geeks.model.FichaCliente ficha, String[] valores) {
        FormEditarCliente form = new FormEditarCliente(ficha, valores);
        return exibir(pai, "Editar cliente", form) ? form : null;
    }

    /** Escolher entre vários clientes com nome parecido (busca do Clube). Retorna o escolhido ou null. */
    public static Cliente escolherCliente(Component pai, String termo, List<Cliente> clientes) {
        FormEscolherCliente form = new FormEscolherCliente(termo, clientes);
        return exibir(pai, "Escolher cliente", form) ? form.getSelecionado() : null;
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
        // Rolagem: em telas baixas (ex.: notebook 1366x768) a janela não passa da tela e o conteúdo rola.
        javax.swing.JScrollPane rolagem = new javax.swing.JScrollPane(new AcompanhaLargura(form),
                javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setBorder(null);
        rolagem.getViewport().setBackground(Tema.FUNDO);
        rolagem.getVerticalScrollBar().setUnitIncrement(18);
        ScrollGeeks.aplicar(rolagem.getVerticalScrollBar());
        dialogo.setContentPane(rolagem);
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        List<Image> icones = Tema.iconesDaJanela();
        if (!icones.isEmpty()) {
            dialogo.setIconImages(icones);
        }
        dialogo.setResizable(true);
        dialogo.pack();
        java.awt.Rectangle tela = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        if (dialogo.getHeight() > tela.height - 24) {
            dialogo.setSize(dialogo.getWidth() + 14, tela.height - 24);
        }
        dialogo.setMinimumSize(new java.awt.Dimension(Math.min(dialogo.getWidth(), 420), 260));
        dialogo.setLocationRelativeTo(pai);
        form.animarEntrada();
        dialogo.setVisible(true);
        return confirmado[0];
    }

    /** Dentro da rolagem, o formulário acompanha a largura da janela e cresce para preencher a altura. */
    private static final class AcompanhaLargura extends javax.swing.JPanel implements javax.swing.Scrollable {
        AcompanhaLargura(FormularioGeeks form) {
            super(new java.awt.BorderLayout());
            add(form, java.awt.BorderLayout.CENTER);
        }

        @Override
        public java.awt.Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientacao, int direcao) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientacao, int direcao) {
            return Math.max(18, r.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }
}
