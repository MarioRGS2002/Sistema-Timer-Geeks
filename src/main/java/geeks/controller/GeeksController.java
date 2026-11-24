package geeks.controller;

import geeks.model.Cliente;
import geeks.model.Dispositivo;
import geeks.model.FaturamentoModel;
import geeks.model.Funcionario;
import geeks.model.FuncionariosModel;
import geeks.model.Perfil;
import geeks.view.FormFuncionario;
import geeks.view.FormEncerrarSessao;
import geeks.model.PreviaEncerramento;
import geeks.model.AvisoReserva;
import geeks.model.Contexto;
import geeks.model.Regras;
import javax.swing.SwingUtilities;
import geeks.model.GeeksModel;
import geeks.model.PeriodoFaturamento;
import geeks.model.SerieFaturamento;
import geeks.model.Usuario;
import geeks.model.Plano;
import geeks.model.Reserva;
import geeks.model.Sessao;
import geeks.view.Dialogos;
import geeks.view.Formatador;
import geeks.view.JanelaPrincipal;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/** Conecta a View (telas) às regras de negócio do Model. */
public class GeeksController {
    private final GeeksModel model;
    private final JanelaPrincipal view;

    private Cliente clienteDoClube;
    private YearMonth mesAtual = YearMonth.now();
    private LocalDate diaSelecionado = LocalDate.now();

    private final boolean gerente;
    private final FaturamentoModel faturamento = new FaturamentoModel();
    private final FuncionariosModel funcionarios = new FuncionariosModel();
    private final int idLogado;
    private Timer timer;
    /** Reservas já avisadas nesta execução (o aviso de sessão acabando fica marcado no banco). */
    private final java.util.Set<Integer> reservasAvisadas = new java.util.HashSet<>();
    private boolean mostrandoAviso;
    private PeriodoFaturamento periodoFaturamento = PeriodoFaturamento.SEMANA;
    private LocalDate referenciaFaturamento = LocalDate.now();

    public GeeksController(Usuario usuario) {
        this.gerente = usuario != null && usuario.isGerente();
        this.idLogado = usuario == null ? 0 : usuario.getIdFuncionario();
        this.model = new GeeksModel();
        this.view = new JanelaPrincipal(gerente);

        ligarNavegacao();
        ligarSessoes();
        ligarReservas();
        ligarHistorico();
        ligarClube();
        ligarCadastro();
        if (gerente) {
            ligarFaturamento();
            ligarFuncionarios();
        }
        if (usuario != null) {
            view.setUsuario(usuario.getNome(), usuario.getPerfil().getRotulo());
        }
        view.addListenerSair(e -> sair());

        // A cada 30 segundos: tempo e valor das sessões ativas e os avisos (tempo acabando, reserva chegando)
        timer = new Timer(30_000, e -> {
            atualizarSessoes();
            verificarAvisos();
        });
        timer.start();

        atualizarTudo();
        SwingUtilities.invokeLater(this::verificarAvisos);
        view.mostrarTela(JanelaPrincipal.SESSOES);
        view.setVisible(true);
    }

    // ---------- Ligações View -> Model ----------

    private void ligarNavegacao() {
        view.addListenerNavegacao(e -> {
            String tela = e.getActionCommand();
            if ((JanelaPrincipal.FATURAMENTO.equals(tela) || JanelaPrincipal.FUNCIONARIOS.equals(tela)) && !gerente) {
                return; // faturamento e funcionários são exclusivos do gerente
            }
            if (JanelaPrincipal.FUNCIONARIOS.equals(tela)) {
                atualizarFuncionarios();
            }
            view.mostrarTela(tela);
        });
    }

    private void ligarSessoes() {
        view.getPainelSessoes().addListenerNovaSessao(e -> novaSessao());
        view.getPainelSessoes().addListenerEncerrarSessao(e -> encerrarSessao(e.getActionCommand()));
    }

    private void ligarReservas() {
        view.getPainelReservas().addListenerMesAnterior(e -> {
            mesAtual = mesAtual.minusMonths(1);
            atualizarReservas();
        });
        view.getPainelReservas().addListenerProximoMes(e -> {
            mesAtual = mesAtual.plusMonths(1);
            atualizarReservas();
        });
        view.getPainelReservas().addListenerSelecionarDia(e -> {
            diaSelecionado = LocalDate.parse(e.getActionCommand());
            mesAtual = YearMonth.from(diaSelecionado);
            atualizarReservas();
        });
        view.getPainelReservas().addListenerRegistrar(e -> registrarReserva());
        view.getPainelReservas().addListenerGerenciar(e -> cancelarReserva());
        view.getPainelReservas().addListenerAlterar(e -> alterarReserva());
    }

    private void ligarHistorico() {
        view.getPainelHistorico().addListenerFiltro(e -> atualizarHistorico());
        view.getPainelHistorico().addListenerBusca(new AoDigitar(this::atualizarHistorico));
        view.getPainelHistorico().addListenerDadosUsuario(e -> {
            geeks.model.FichaCliente ficha = model.getFicha(Integer.parseInt(e.getActionCommand()));
            if (ficha == null) {
                Dialogos.info(view, "Não encontrei o cadastro desse cliente.");
            } else {
                Dialogos.mostrarCliente(view, ficha);
            }
        });
    }

    private void ligarClube() {
        view.getPainelClube().addListenerBuscar(e -> buscarClienteDoClube());
        view.getPainelClube().addListenerAssinar(e -> assinarPlano(Plano.valueOf(e.getActionCommand())));
        view.getPainelClube().addListenerCancelar(e -> cancelarAssinatura());
        view.getPainelClube().addListenerNovoCliente(e -> view.mostrarTela(JanelaPrincipal.CADASTROS));
    }

    private void ligarCadastro() {
        view.getPainelCadastro().addListenerSalvar(e -> salvarCadastro());
        view.getPainelCadastro().addListenerLimpar(e -> view.getPainelCadastro().limparCampos());
    }

    // ---------- Atualização das telas ----------

    private void atualizarTudo() {
        atualizarSessoes();
        atualizarReservas();
        atualizarHistorico();
        if (gerente) {
            atualizarFaturamento();
        }
    }

    // ---------- Faturamento (somente gerente) ----------

    private void ligarFaturamento() {
        view.getPainelFaturamento().addListenerPeriodo(e -> {
            periodoFaturamento = PeriodoFaturamento.valueOf(e.getActionCommand());
            atualizarFaturamento();
        });
        view.getPainelFaturamento().addListenerAnterior(e -> {
            referenciaFaturamento = periodoFaturamento.deslocar(referenciaFaturamento, -1);
            atualizarFaturamento();
        });
        view.getPainelFaturamento().addListenerProximo(e -> {
            referenciaFaturamento = periodoFaturamento.deslocar(referenciaFaturamento, 1);
            atualizarFaturamento();
        });
        view.getPainelFaturamento().addListenerHoje(e -> {
            referenciaFaturamento = LocalDate.now();
            atualizarFaturamento();
        });
    }

    private void atualizarFaturamento() {
        LocalDate hoje = LocalDate.now();
        SerieFaturamento serie = faturamento.serie(periodoFaturamento, referenciaFaturamento);
        boolean podeAvancar = !periodoFaturamento.inicio(periodoFaturamento.deslocar(referenciaFaturamento, 1)).isAfter(hoje);
        boolean periodoAtual = periodoFaturamento.inicio(referenciaFaturamento).equals(periodoFaturamento.inicio(hoje));
        view.getPainelFaturamento().atualizar(serie, podeAvancar, periodoAtual);
    }

    // ---------- Sair e avisos ----------

    /** Volta para a tela de login (ex.: o atendente sai e o gerente entra no mesmo computador). */
    private void sair() {
        if (!Dialogos.confirmar(view, "Sair", "Sair do sistema e voltar para a tela de login? As sessões ativas continuam "
                + "contando normalmente.", "Sair", false)) {
            return;
        }
        timer.stop();
        Contexto.sair();
        view.dispose();
        new LoginController();
    }

    /**
     * Avisos: sessão de tempo fechado acabando (ou acabada) e reserva começando em poucos minutos.
     * Um aviso por sessão (marcado no banco, vale para todos os computadores) e por reserva.
     */
    private void verificarAvisos() {
        if (mostrandoAviso || !view.isDisplayable()) {
            return;
        }
        List<String> avisos = new java.util.ArrayList<>();
        for (Sessao s : model.sessoesParaAvisar(Regras.ALERTA_RESERVA_MINUTOS)) {
            long falta = java.time.Duration.between(LocalDateTime.now(), s.getPrevisto()).toMinutes();
            avisos.add(s.getCliente().getNome() + " em " + s.getDispositivo().getNome()
                    + (falta > 0 ? ": o tempo acaba em " + falta + " min (" + s.getPrevisto().toLocalTime().withNano(0) + ")."
                    : ": o tempo combinado acabou às " + s.getPrevisto().toLocalTime().withNano(0) + "."));
        }
        for (Reserva r : model.reservasChegando(Regras.ALERTA_RESERVA_MINUTOS)) {
            if (reservasAvisadas.add(r.getIdReserva())) {
                avisos.add("Reserva de " + r.getCliente().getNome() + " em " + r.getDispositivo().getNome()
                        + " às " + r.getDataHora().toLocalTime() + ".");
            }
        }
        if (avisos.isEmpty()) {
            return;
        }
        mostrandoAviso = true;
        try {
            Dialogos.info(view, String.join("\n", avisos));
        } finally {
            mostrandoAviso = false;
        }
    }

    // ---------- Funcionários (somente gerente) ----------

    private void ligarFuncionarios() {
        var painel = view.getPainelFuncionarios();
        painel.addListenerNovo(e -> novoFuncionario());
        painel.addListenerTrocarSenha(e -> trocarSenha(Integer.parseInt(e.getActionCommand())));
        painel.addListenerDesativar(e -> desativarFuncionario(Integer.parseInt(e.getActionCommand())));
        painel.addListenerReativar(e -> reativarFuncionario(Integer.parseInt(e.getActionCommand())));
    }

    private void atualizarFuncionarios() {
        view.getPainelFuncionarios().atualizar(funcionarios.listar(), idLogado);
    }

    private Funcionario funcionarioPorId(int id) {
        return funcionarios.listar().stream().filter(f -> f.getId() == id).findFirst().orElse(null);
    }

    private void novoFuncionario() {
        FormFuncionario form = Dialogos.pedirNovoFuncionario(view);
        if (form == null) {
            return;
        }
        try {
            Funcionario f = funcionarios.cadastrar(form.getLogin(), form.getNome(),
                    form.isGerente() ? Perfil.GERENTE : Perfil.ATENDENTE, form.getSenha(), form.getConfirmacao());
            atualizarFuncionarios();
            Dialogos.sucesso(view, f.getNome() + " já pode entrar com o login \"" + f.getLogin() + "\".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Dialogos.erro(view, ex.getMessage());
        } finally {
            form.limparSenhas();
        }
    }

    private void trocarSenha(int id) {
        Funcionario f = funcionarioPorId(id);
        if (f == null) {
            return;
        }
        FormFuncionario form = Dialogos.pedirNovaSenha(view, f.getNome());
        if (form == null) {
            return;
        }
        try {
            funcionarios.trocarSenha(id, form.getSenha(), form.getConfirmacao());
            Dialogos.sucesso(view, "Senha de " + f.getNome() + " alterada.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Dialogos.erro(view, ex.getMessage());
        } finally {
            form.limparSenhas();
        }
    }

    private void desativarFuncionario(int id) {
        Funcionario f = funcionarioPorId(id);
        if (f == null || !Dialogos.confirmar(view, "Desativar funcionário",
                f.getNome() + " não vai mais conseguir entrar no sistema. O histórico dele continua guardado "
                        + "e você pode reativar quando quiser.", "Desativar", true)) {
            return;
        }
        try {
            funcionarios.desativar(id);
            atualizarFuncionarios();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Dialogos.erro(view, ex.getMessage());
        }
    }

    private void reativarFuncionario(int id) {
        try {
            funcionarios.reativar(id);
            atualizarFuncionarios();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Dialogos.erro(view, ex.getMessage());
        }
    }

    private void atualizarSessoes() {
        List<Sessao> ativas = model.getSessoesAtivas();
        view.getPainelSessoes().atualizarSessoes(ativas);
        view.getPainelSessoes().atualizarResumo(ativas.size(), model.getFaturamentoHoje());
    }

    private void atualizarReservas() {
        view.getPainelReservas().atualizarCalendario(mesAtual, diaSelecionado, model.getDiasComReserva(mesAtual));
        view.getPainelReservas().atualizarReservasDoDia(diaSelecionado, model.getReservasDoDia(diaSelecionado));
    }

    private void atualizarHistorico() {
        view.getPainelHistorico().atualizarHistorico(model.filtrarHistorico(
                view.getPainelHistorico().getPeriodoSelecionado(), view.getPainelHistorico().getTextoBusca()));
    }

    // ---------- Sessões ----------

    private void novaSessao() {
        if (model.getClientes().isEmpty()) {
            Dialogos.info(view, "Cadastre um cliente antes de iniciar uma sessão.");
            view.mostrarTela(JanelaPrincipal.CADASTROS);
            return;
        }
        List<Dispositivo> livres = model.getDispositivosLivres();
        if (livres.isEmpty()) {
            Dialogos.info(view, "Todos os dispositivos estão em uso no momento.");
            return;
        }
        Object[] r = Dialogos.pedirNovaSessao(view, model.getClientes(), model.getDispositivos(), model::dispositivoOcupado);
        if (r == null) {
            return;
        }
        try {
            model.iniciarSessao((Cliente) r[0], (Dispositivo) r[1], (Integer) r[2]);
            atualizarSessoes();
            atualizarReservas(); // a reserva do cliente pode ter virado esta sessão
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Dialogos.erro(view, ex.getMessage());
        }
    }

    private void encerrarSessao(String id) {
        Sessao sessao = model.getSessoesAtivas().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        if (sessao == null) {
            return;
        }
        FormEncerrarSessao form = Dialogos.confirmarEncerramento(view, sessao, model.getFormasPagamento(),
                usarCredito -> model.previaEncerramento(id, usarCredito));
        if (form == null) {
            return;
        }
        String forma = form.getFormaPagamento();
        boolean usarCredito = form.isUsarCredito();
        LocalDateTime fimMostrado = form.getFimMostrado();
        // o cartão some com animação e só então a sessão é encerrada e as telas atualizadas
        view.getPainelSessoes().animarSaida(id, () -> {
            try {
                PreviaEncerramento r = model.encerrarSessao(id, forma, usarCredito, fimMostrado);
                if (r.getCobranca().getMinutosCredito() > 0) {
                    Dialogos.sucesso(view, sessao.getCliente().getNome() + " ficou com " + r.getCobranca().getMinutosCredito()
                            + " min de crédito em " + sessao.getDispositivo().getTipo().getRotulo()
                            + " (vale 3 meses). Dá para usar ao encerrar a próxima sessão.");
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                Dialogos.erro(view, ex.getMessage());
            } finally {
                atualizarSessoes();
                atualizarHistorico();
            }
        });
    }

    // ---------- Reservas ----------

    private void registrarReserva() {
        if (model.getClientes().isEmpty()) {
            Dialogos.info(view, "Cadastre um cliente antes de registrar uma reserva.");
            return;
        }
        Object[] r = Dialogos.pedirReserva(view, model.getClientes(), model.getDispositivos(), diaSelecionado,
                model.getFormasPagamento());
        if (r == null) {
            return;
        }
        LocalDateTime quando = LocalDateTime.of(diaSelecionado, (LocalTime) r[2]);
        boolean ignorarAviso = false;
        while (true) {
            try {
                model.registrarReserva((Cliente) r[0], (Dispositivo) r[1], quando, (Integer) r[3], (String) r[4], ignorarAviso);
                atualizarReservas();
                if (r[4] != null) {
                    Dialogos.sucesso(view, "Reserva registrada com a entrada paga em " + r[4] + ".");
                }
                return;
            } catch (AvisoReserva aviso) {
                if (!Dialogos.confirmar(view, "Reservar mesmo assim?", aviso.getMessage()
                        + " Se reservar, combine com o cliente atual para liberar o equipamento antes do horário.",
                        "Reservar mesmo assim", false)) {
                    return;
                }
                ignorarAviso = true;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                Dialogos.erro(view, ex.getMessage());
                return;
            }
        }
    }

    private void cancelarReserva() {
        Reserva r = escolherReservaDoDia("Cancelar reserva", "Selecione a reserva que deseja cancelar:", "Continuar");
        if (r == null) {
            return;
        }
        String msg = "Cancelar a reserva de " + r.getCliente().getNome() + " em " + r.getDispositivo().getNome()
                + " às " + r.getDataHora().toLocalTime() + "?";
        if (Dialogos.confirmar(view, "Cancelar reserva", msg, "Cancelar reserva", true)) {
            model.cancelarReserva(r);
            atualizarReservas();
        }
    }

    private void alterarReserva() {
        Reserva r = escolherReservaDoDia("Alterar reserva", "Selecione a reserva que deseja alterar:", "Continuar");
        if (r == null) {
            return;
        }
        LocalTime novo = Dialogos.pedirHorario(view, r.getDataHora().toLocalTime());
        if (novo == null) {
            return;
        }
        boolean ignorarAviso = false;
        while (true) {
            try {
                model.alterarReserva(r, LocalDateTime.of(diaSelecionado, novo), ignorarAviso);
                atualizarReservas();
                return;
            } catch (AvisoReserva aviso) {
                if (!Dialogos.confirmar(view, "Alterar mesmo assim?", aviso.getMessage(), "Alterar mesmo assim", false)) {
                    return;
                }
                ignorarAviso = true;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                Dialogos.erro(view, ex.getMessage());
                return;
            }
        }
    }

    private Reserva escolherReservaDoDia(String titulo, String mensagem, String textoConfirmar) {
        List<Reserva> doDia = model.getReservasDoDia(diaSelecionado);
        if (doDia.isEmpty()) {
            Dialogos.info(view, "Não há reservas em " + Formatador.dataExtenso(diaSelecionado) + ".");
            return null;
        }
        return Dialogos.escolherReserva(view, titulo, mensagem, textoConfirmar, doDia);
    }

    // ---------- Clube Geeks ----------

    private void buscarClienteDoClube() {
        String termo = view.getPainelClube().getTermoBusca();
        if (termo.isBlank()) {
            return;
        }
        List<Cliente> achados = model.buscarClientes(termo);
        clienteDoClube = achados.size() == 1 ? achados.get(0)
                : achados.size() > 1 ? Dialogos.escolherCliente(view, termo.trim(), achados) : null;
        if (achados.isEmpty()) {
            view.getPainelClube().mostrarNaoEncontrado(termo.trim());
        } else if (clienteDoClube != null) {
            view.getPainelClube().mostrarCliente(clienteDoClube);
        }
    }

    private void assinarPlano(Plano plano) {
        if (clienteDoClube == null) {
            return;
        }
        String msg = "Mensalidade do " + plano.getNome() + " para " + clienteDoClube.getNome() + ".";
        String forma = Dialogos.pedirPagamento(view, "Assinar clube", msg, plano.getValor(), model.getFormasPagamento(), "Assinar");
        if (forma != null) {
            model.assinarPlano(clienteDoClube, plano, forma);
            view.getPainelClube().mostrarCliente(clienteDoClube);
            Dialogos.sucesso(view, clienteDoClube.getNome() + " agora faz parte do Clube Geeks!");
        }
    }

    private void cancelarAssinatura() {
        if (clienteDoClube == null || clienteDoClube.getPlano() == Plano.NENHUM) {
            return;
        }
        if (Dialogos.confirmar(view, "Cancelar assinatura", "Cancelar a assinatura de " + clienteDoClube.getNome() + "?",
                "Cancelar assinatura", true)) {
            model.assinarPlano(clienteDoClube, Plano.NENHUM);
            view.getPainelClube().mostrarCliente(clienteDoClube);
        }
    }

    // ---------- Cadastro ----------

    private void salvarCadastro() {
        try {
            var p = view.getPainelCadastro();
            Cliente novo = model.cadastrarCliente(p.getNome(), p.getNascimento(), p.getTelefone(), p.getCpf(),
                    p.getResponsavel(), p.getCpfResponsavel(), p.getTelefoneResponsavel(), p.getParentesco());
            boolean escolherPlano = view.getPainelCadastro().isEscolherPlano();
            view.getPainelCadastro().limparCampos();
            Dialogos.sucesso(view, "Cliente " + novo.getNome() + " cadastrado com sucesso!");
            if (escolherPlano) {
                clienteDoClube = novo;
                view.getPainelClube().mostrarCliente(novo);
                view.mostrarTela(JanelaPrincipal.CLUBE);
            }
        } catch (IllegalArgumentException ex) {
            Dialogos.erro(view, ex.getMessage());
        }
    }

    /** Dispara uma ação sempre que o texto de um campo muda. */
    private static class AoDigitar implements DocumentListener {
        private final Runnable acao;

        AoDigitar(Runnable acao) {
            this.acao = acao;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            acao.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            acao.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            acao.run();
        }
    }
}
