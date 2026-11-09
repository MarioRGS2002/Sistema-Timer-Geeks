package geeks.controller;

import geeks.model.Cliente;
import geeks.model.Dispositivo;
import geeks.model.GeeksModel;
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
import java.util.ArrayList;
import java.util.List;

/** Conecta a View (telas) às regras de negócio do Model. */
public class GeeksController {
    private final GeeksModel model;
    private final JanelaPrincipal view;

    private Cliente clienteDoClube;
    private YearMonth mesAtual = YearMonth.now();
    private LocalDate diaSelecionado = LocalDate.now();

    public GeeksController() {
        this.model = new GeeksModel();
        this.view = new JanelaPrincipal();

        ligarNavegacao();
        ligarSessoes();
        ligarReservas();
        ligarHistorico();
        ligarClube();
        ligarCadastro();

        // Atualiza tempo e valor das sessões ativas a cada 30 segundos
        new Timer(30_000, e -> atualizarSessoes()).start();

        atualizarTudo();
        view.mostrarTela(JanelaPrincipal.SESSOES);
        view.setVisible(true);
    }

    // ---------- Ligações View -> Model ----------

    private void ligarNavegacao() {
        view.addListenerNavegacao(e -> view.mostrarTela(e.getActionCommand()));
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
            Cliente c = model.buscarCliente(e.getActionCommand());
            if (c != null) {
                Dialogos.mostrarCliente(view, c);
            }
        });
    }

    private void ligarClube() {
        view.getPainelClube().addListenerBuscar(e -> buscarClienteDoClube());
        view.getPainelClube().addListenerAssinar(e -> assinarPlano(Plano.valueOf(e.getActionCommand())));
        view.getPainelClube().addListenerGerenciar(e -> gerenciarAssinatura());
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
        Object[] r = Dialogos.pedirNovaSessao(view, model.getClientes(), livres);
        if (r == null) {
            return;
        }
        try {
            model.iniciarSessao((Cliente) r[0], (Dispositivo) r[1]);
            atualizarSessoes();
        } catch (IllegalStateException ex) {
            Dialogos.erro(view, ex.getMessage());
        }
    }

    private void encerrarSessao(String id) {
        Sessao sessao = model.getSessoesAtivas().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        if (sessao == null) {
            return;
        }
        String resumo = "Encerrar a sessão de " + sessao.getCliente().getNome() + " em " + sessao.getDispositivo().getNome()
                + "?\nTempo: " + Formatador.duracao(sessao.getDuracao()) + "\nValor: " + Formatador.moeda(sessao.getValor());
        if (Dialogos.confirmar(view, resumo)) {
            model.encerrarSessao(id);
            atualizarSessoes();
            atualizarHistorico();
        }
    }

    // ---------- Reservas ----------

    private void registrarReserva() {
        if (model.getClientes().isEmpty()) {
            Dialogos.info(view, "Cadastre um cliente antes de registrar uma reserva.");
            return;
        }
        Object[] r = Dialogos.pedirReserva(view, model.getClientes(), model.getDispositivos(), diaSelecionado);
        if (r == null) {
            return;
        }
        try {
            model.registrarReserva((Cliente) r[0], (Dispositivo) r[1], LocalDateTime.of(diaSelecionado, (LocalTime) r[2]));
            atualizarReservas();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Dialogos.erro(view, ex.getMessage());
        }
    }

    private void cancelarReserva() {
        Reserva r = escolherReservaDoDia("Gerenciar reservas", "Selecione a reserva que deseja cancelar:");
        if (r != null && Dialogos.confirmar(view, "Cancelar a reserva \"" + r + "\"?")) {
            model.cancelarReserva(r);
            atualizarReservas();
        }
    }

    private void alterarReserva() {
        Reserva r = escolherReservaDoDia("Alterar reserva", "Selecione a reserva que deseja alterar:");
        if (r == null) {
            return;
        }
        LocalTime novo = Dialogos.pedirHorario(view, r.getDataHora().toLocalTime());
        if (novo == null) {
            return;
        }
        try {
            model.alterarReserva(r, LocalDateTime.of(diaSelecionado, novo));
            atualizarReservas();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Dialogos.erro(view, ex.getMessage());
        }
    }

    private Reserva escolherReservaDoDia(String titulo, String mensagem) {
        List<Reserva> doDia = model.getReservasDoDia(diaSelecionado);
        if (doDia.isEmpty()) {
            Dialogos.info(view, "Não há reservas em " + Formatador.dataExtenso(diaSelecionado) + ".");
            return null;
        }
        return Dialogos.escolher(view, titulo, mensagem, doDia);
    }

    // ---------- Clube Geeks ----------

    private void buscarClienteDoClube() {
        String termo = view.getPainelClube().getTermoBusca();
        if (termo.isBlank()) {
            return;
        }
        clienteDoClube = model.buscarCliente(termo);
        if (clienteDoClube == null) {
            view.getPainelClube().mostrarNaoEncontrado(termo.trim());
        } else {
            view.getPainelClube().mostrarCliente(clienteDoClube);
        }
    }

    private void assinarPlano(Plano plano) {
        if (clienteDoClube == null) {
            return;
        }
        String msg = "Assinar o plano " + plano.getNome() + " (" + Formatador.moeda(plano.getMensalidade())
                + "/mês) para " + clienteDoClube.getNome() + "?";
        if (Dialogos.confirmar(view, msg)) {
            model.assinarPlano(clienteDoClube, plano);
            view.getPainelClube().mostrarCliente(clienteDoClube);
        }
    }

    private void gerenciarAssinatura() {
        if (clienteDoClube == null || clienteDoClube.getPlano() == Plano.NENHUM) {
            return;
        }
        List<String> opcoes = List.of("Alterar plano", "Cancelar assinatura");
        String escolha = Dialogos.escolher(view, "Gerenciar assinatura", "O que deseja fazer?", opcoes);
        if (escolha == null) {
            return;
        }
        if (escolha.equals("Cancelar assinatura")) {
            if (Dialogos.confirmar(view, "Cancelar a assinatura de " + clienteDoClube.getNome() + "?")) {
                model.assinarPlano(clienteDoClube, Plano.NENHUM);
            }
        } else {
            List<Plano> outros = new ArrayList<>(List.of(Plano.BASICO, Plano.PREMIUM));
            outros.remove(clienteDoClube.getPlano());
            Plano novo = Dialogos.escolher(view, "Alterar plano", "Escolha o novo plano:", outros);
            if (novo != null) {
                model.assinarPlano(clienteDoClube, novo);
            }
        }
        view.getPainelClube().mostrarCliente(clienteDoClube);
    }

    // ---------- Cadastro ----------

    private void salvarCadastro() {
        try {
            Cliente novo = model.cadastrarCliente(view.getPainelCadastro().getNome(), view.getPainelCadastro().getIdade(),
                    view.getPainelCadastro().getTelefone(), view.getPainelCadastro().getCpf(),
                    view.getPainelCadastro().getResponsavel());
            boolean escolherPlano = view.getPainelCadastro().isEscolherPlano();
            view.getPainelCadastro().limparCampos();
            Dialogos.info(view, "Cliente " + novo.getNome() + " cadastrado com sucesso!");
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
