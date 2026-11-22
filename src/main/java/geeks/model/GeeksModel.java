package geeks.model;

import geeks.dados.AssinaturaDAO;
import geeks.dados.ClienteDAO;
import geeks.dados.CreditoDAO;
import geeks.dados.EquipamentoDAO;
import geeks.dados.ErroBanco;
import geeks.dados.PagamentoDAO;
import geeks.dados.ReservaDAO;
import geeks.dados.SessaoDAO;
import geeks.dados.Transacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regras de negócio do sistema Geeks, agora com os dados no MySQL (via DAOs/JDBC).
 * As assinaturas públicas são as mesmas usadas pelo GeeksController e pelas telas.
 * Erros de regra: IllegalArgumentException/IllegalStateException (mensagem para a tela).
 * Erros de banco: ErroBanco (mensagem clara, mostrada pelo tratador global do MainGeeks).
 */
public class GeeksModel {
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EquipamentoDAO equipamentoDAO = new EquipamentoDAO();
    private final SessaoDAO sessaoDAO = new SessaoDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final AssinaturaDAO assinaturaDAO = new AssinaturaDAO();
    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();
    private final CreditoDAO creditoDAO = new CreditoDAO();

    /** Equipamentos carregados uma vez (mesmas instâncias em toda a tela). */
    private final List<Dispositivo> dispositivos;
    private final Map<Integer, Dispositivo> dispositivoPorId = new HashMap<>();

    public GeeksModel() {
        this.dispositivos = Transacao.consultar(c -> {
            equipamentoDAO.carregarTiposEPlano(c);
            return equipamentoDAO.listarUtilizaveis(c);
        });
        for (Dispositivo d : dispositivos) {
            dispositivoPorId.put(d.getIdEquipamento(), d);
        }
    }

    // ---------- Clientes ----------

    public Cliente cadastrarCliente(String nome, String idadeTxt, String telefone, String cpf, String responsavel) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do cliente.");
        }
        if (nome.trim().length() > 100) {
            throw new IllegalArgumentException("O nome pode ter no máximo 100 caracteres.");
        }
        int idade;
        try {
            idade = Integer.parseInt(idadeTxt == null ? "" : idadeTxt.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe uma idade válida.");
        }
        if (idade < 0 || idade > 120) {
            throw new IllegalArgumentException("Informe uma idade válida.");
        }
        String cpfNumeros = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (!cpfValido(cpfNumeros)) {
            throw new IllegalArgumentException("CPF inválido.");
        }
        if (idade < 18 && (responsavel == null || responsavel.isBlank())) {
            throw new IllegalArgumentException("Clientes menores de 18 anos precisam de um responsável.");
        }
        String tel = telefone == null ? "" : telefone.trim();
        if (tel.length() > 20) {
            throw new IllegalArgumentException("Telefone inválido.");
        }
        // PENDÊNCIA DO FRONT: a tela pede a idade (não a data de nascimento) e só o NOME do
        // responsável. data_nascimento = hoje - idade; o responsável não é gravado porque o
        // banco exige o CPF dele (ver README, "Pendências que dependem do front").
        LocalDate nascimento = LocalDate.now().minusYears(idade);
        LocalDate hoje = LocalDate.now();
        return Transacao.executar(c -> {
            if (clienteDAO.existeCpf(c, cpfNumeros)) {
                throw new IllegalArgumentException("Já existe um cliente cadastrado com esse CPF.");
            }
            int id = clienteDAO.inserir(c, nome.trim(), nascimento, tel, cpfNumeros);
            return clienteDAO.porId(c, id, hoje).orElseThrow();
        });
    }

    /** Busca por CPF (somente números) ou por parte do nome. */
    public Cliente buscarCliente(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String numeros = termo.replaceAll("\\D", "");
        LocalDate hoje = LocalDate.now();
        return Transacao.consultar(c -> {
            Optional<Cliente> r = numeros.length() == 11
                    ? clienteDAO.porCpf(c, numeros, hoje)
                    : clienteDAO.porParteDoNome(c, termo.trim(), hoje);
            return r.orElse(null);
        });
    }

    /**
     * Assinar (Plano.CLUBE) ou cancelar (Plano.NENHUM) o Clube Geeks.
     * Assinar: cria assinatura de 1 mês ou renova a ativa, e registra o pagamento da mensalidade.
     */
    public void assinarPlano(Cliente cliente, Plano plano) {
        int idFuncionario = Contexto.idFuncionario();
        LocalDate hoje = LocalDate.now();
        if (plano == Plano.NENHUM) {
            Transacao.executar(c -> assinaturaDAO.cancelar(c, cliente.getIdCliente()));
            cliente.setPlano(Plano.NENHUM);
            return;
        }
        Transacao.executar(c -> {
            Optional<Integer> ativa = assinaturaDAO.ativaParaAtualizar(c, cliente.getIdCliente());
            int idAssinatura;
            if (ativa.isPresent()) {
                idAssinatura = ativa.get();
                assinaturaDAO.renovar(c, idAssinatura, hoje);
            } else {
                idAssinatura = assinaturaDAO.criar(c, Plano.CLUBE.getIdPlano(), cliente.getIdCliente(), hoje);
            }
            pagamentoDAO.registrar(c, LocalDateTime.now(), Plano.CLUBE.getValor(), null, PagamentoDAO.ASSINATURA,
                    pagamentoDAO.idForma(c, Regras.FORMA_PAGAMENTO_PADRAO), idFuncionario, null, null, idAssinatura);
            return null;
        });
        cliente.setPlano(Plano.CLUBE);
    }

    private boolean cpfValido(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digito(cpf, 9) == cpf.charAt(9) - '0' && digito(cpf, 10) == cpf.charAt(10) - '0';
    }

    private int digito(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    // ---------- Sessões ----------

    /**
     * Abre sessão. A tela não pergunta o tipo de tempo: sessões abertas no balcão são
     * tempo ABERTO (presencial, sem hora para acabar). Se houver reserva do equipamento a
     * menos de J minutos, avisa e sugere equipamentos livres do mesmo tipo.
     */
    public Sessao iniciarSessao(Cliente cliente, Dispositivo dispositivo) {
        int idFuncionario = Contexto.idFuncionario();
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        int idSessao = Transacao.executar(c -> {
            equipamentoDAO.travar(c, dispositivo.getIdEquipamento());
            if (sessaoDAO.clienteEmSessaoAtiva(c, cliente.getIdCliente())) {
                throw new IllegalStateException(cliente.getNome() + " já está em uma sessão ativa.");
            }
            Optional<ReservaDAO.Linha> prox = reservaDAO.proximaAtiva(c, dispositivo.getIdEquipamento(), agora);
            if (prox.isPresent() && prox.get().inicio.isBefore(agora.plusMinutes(Regras.JANELA_MINUTOS))
                    && prox.get().idCliente != cliente.getIdCliente()) {
                List<String> livres = equipamentoDAO.livresMesmoTipo(c, dispositivo.getIdEquipamento(), agora,
                        prox.get().fim, agora, Regras.JANELA_MINUTOS);
                throw new IllegalStateException("O dispositivo " + dispositivo.getNome() + " tem reserva às "
                        + prox.get().inicio.toLocalTime() + "." + sugestao(livres));
            }
            try {
                int id = sessaoDAO.abrir(c, TipoTempo.ABERTO.name(), agora, null, dispositivo.getTipo().getValorHora(),
                        dispositivo.getIdEquipamento(), idFuncionario, cliente.getIdCliente());
                equipamentoDAO.definirStatus(c, dispositivo.getIdEquipamento(), "EM_USO");
                return id;
            } catch (SQLException e) {
                if (String.valueOf(e.getMessage()).contains("uq_sessao_equipamento_em_uso")) {
                    throw new IllegalStateException("O dispositivo " + dispositivo.getNome() + " já está em uso.");
                }
                throw e;
            }
        });
        return getSessoesAtivas().stream().filter(s -> s.getIdSessao() == idSessao).findFirst().orElse(null);
    }

    private static String sugestao(List<String> livres) {
        return livres.isEmpty() ? " Não há outro equipamento do mesmo tipo livre."
                : " Livres do mesmo tipo: " + String.join(", ", livres) + ".";
    }

    /**
     * Encerra a sessão numa única transação: trava a sessão, grava fim e valor, a hora grátis
     * do Clube usada, os pagamentos (grátis = valor 0 sem forma) e o crédito do tempo pago
     * e não usado. Em qualquer falha, nada fica gravado (rollback). O equipamento volta a
     * DISPONIVEL na mesma transação.
     */
    public Sessao encerrarSessao(String id) {
        int idSessao = idNumerico(id, "S");
        int idFuncionario = Contexto.idFuncionario();
        LocalDateTime fim = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Transacao.executar(c -> {
            encerrar(c, idSessao, fim, idFuncionario);
            return null;
        });
        return null;
    }

    /** Encerramento em uma conexão já em transação (usado também pelos testes). */
    public void encerrar(Connection c, int idSessao, LocalDateTime fim, int idFuncionario) throws SQLException {
        SessaoDAO.Linha s = sessaoDAO.travarAtiva(c, idSessao);
        LocalDate dia = s.inicio.toLocalDate();
        int gratisDisponivel = 0;
        if (assinaturaDAO.emDia(c, s.idCliente, dia)) {
            gratisDisponivel = Math.max(0, Cobranca.SEGUNDOS_HORA - sessaoDAO.gratisUsadoNoDia(c, s.idCliente, dia));
        }
        Cobranca cob = Cobranca.calcular(TipoTempo.valueOf(s.tipoTempo), s.inicio, s.previsto, fim, s.valorHora,
                gratisDisponivel);
        sessaoDAO.encerrar(c, idSessao, fim, cob.getValor());
        sessaoDAO.definirGratis(c, idSessao, s.idCliente, cob.getSegundosGratis());
        if (cob.getSegundosGratis() > 0) {
            pagamentoDAO.registrar(c, fim, BigDecimal.ZERO.setScale(2), "SAIDA", PagamentoDAO.SESSAO, null,
                    idFuncionario, idSessao, null, null);
        }
        if (cob.getValor().signum() > 0) {
            pagamentoDAO.registrar(c, fim, cob.getValor(), "SAIDA", PagamentoDAO.SESSAO,
                    pagamentoDAO.idForma(c, Regras.FORMA_PAGAMENTO_PADRAO), idFuncionario, idSessao, null, null);
        }
        if (cob.getMinutosCredito() > 0) {
            creditoDAO.gerar(c, s.idCliente, s.idTipo, idSessao, cob.getMinutosCredito(), fim);
        }
        equipamentoDAO.definirStatus(c, s.idEquipamento, "DISPONIVEL");
    }

    public boolean dispositivoOcupado(Dispositivo dispositivo) {
        return getSessoesAtivas().stream().anyMatch(s -> s.getDispositivo().equals(dispositivo));
    }

    public List<Dispositivo> getDispositivosLivres() {
        Set<Dispositivo> ocupados = getSessoesAtivas().stream().map(Sessao::getDispositivo).collect(Collectors.toSet());
        return dispositivos.stream().filter(d -> !ocupados.contains(d)).collect(Collectors.toList());
    }

    public List<Sessao> getSessoesAtivas() {
        LocalDate hoje = LocalDate.now();
        return Transacao.consultar(c -> {
            List<SessaoDAO.Linha> linhas = sessaoDAO.ativas(c);
            Map<Integer, Cliente> clientes = clientesPorId(c, hoje);
            List<Sessao> lista = new ArrayList<>();
            for (SessaoDAO.Linha l : linhas) {
                Cliente cl = clientes.get(l.idCliente);
                int gratis = 0;
                if (cl != null && cl.getPlano() == Plano.CLUBE && assinaturaDAO.emDia(c, l.idCliente, l.inicio.toLocalDate())) {
                    gratis = Math.max(0, Cobranca.SEGUNDOS_HORA - sessaoDAO.gratisUsadoNoDia(c, l.idCliente, l.inicio.toLocalDate()));
                }
                lista.add(montar(l, cl, gratis));
            }
            return lista;
        });
    }

    /** Total recebido hoje (todos os pagamentos com data de hoje). */
    public BigDecimal getFaturamentoHoje() {
        LocalDate hoje = LocalDate.now();
        return Transacao.consultar(c -> pagamentoDAO.total(c, hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay()));
    }

    // ---------- Histórico ----------

    public List<Sessao> filtrarHistorico(PeriodoHistorico periodo, String texto) {
        LocalDate inicio = periodo.inicio(LocalDate.now());
        LocalDate hoje = LocalDate.now();
        return Transacao.consultar(c -> {
            Map<Integer, Cliente> clientes = clientesPorId(c, hoje);
            List<Sessao> lista = new ArrayList<>();
            for (SessaoDAO.Linha l : sessaoDAO.historico(c, inicio.atStartOfDay(), texto)) {
                lista.add(montar(l, clientes.get(l.idCliente), 0));
            }
            return lista;
        });
    }

    // ---------- Reservas ----------

    public Reserva registrarReserva(Cliente cliente, Dispositivo dispositivo, LocalDateTime dataHora) {
        LocalDateTime agora = LocalDateTime.now();
        if (dataHora.isBefore(agora)) {
            throw new IllegalArgumentException("A reserva precisa ser para um horário futuro.");
        }
        LocalDateTime fim = dataHora.plusMinutes(Regras.DURACAO_RESERVA_MINUTOS);
        BigDecimal valor = dispositivo.getTipo().getValorHora()
                .multiply(BigDecimal.valueOf(Regras.DURACAO_RESERVA_MINUTOS))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal entrada = valor.multiply(Regras.PERCENTUAL_ENTRADA).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        int id = Transacao.executar(c -> {
            equipamentoDAO.travar(c, dispositivo.getIdEquipamento());
            verificarConflito(c, dispositivo, 0, dataHora, fim, agora);
            return reservaDAO.inserir(c, dataHora, fim, valor, Regras.PERCENTUAL_ENTRADA, entrada,
                    cliente.getIdCliente(), dispositivo.getIdEquipamento());
        });
        return new Reserva(id, cliente, dispositivo, dataHora, fim);
    }

    public void alterarReserva(Reserva reserva, LocalDateTime novaDataHora) {
        LocalDateTime agora = LocalDateTime.now();
        if (novaDataHora.isBefore(agora)) {
            throw new IllegalArgumentException("A reserva precisa ser para um horário futuro.");
        }
        Transacao.executar(c -> {
            equipamentoDAO.travar(c, reserva.getDispositivo().getIdEquipamento());
            ReservaDAO.Linha atual = reservaDAO.travar(c, reserva.getIdReserva());
            long duracao = ChronoUnit.MINUTES.between(atual.inicio, atual.fim);
            LocalDateTime novoFim = novaDataHora.plusMinutes(duracao);
            verificarConflito(c, reserva.getDispositivo(), reserva.getIdReserva(), novaDataHora, novoFim, agora);
            reservaDAO.alterarHorario(c, reserva.getIdReserva(), novaDataHora, novoFim);
            return null;
        });
    }

    /** Cancelar = status 'CANCELADA' (a reserva não é apagada, fica no histórico). */
    public void cancelarReserva(Reserva reserva) {
        Transacao.executar(c -> reservaDAO.cancelar(c, reserva.getIdReserva()));
    }

    private void verificarConflito(Connection c, Dispositivo d, int idIgnorar, LocalDateTime ini, LocalDateTime fim,
                                   LocalDateTime agora) throws SQLException {
        if (reservaDAO.conflitos(c, d.getIdEquipamento(), idIgnorar, ini, fim, Regras.JANELA_MINUTOS) > 0) {
            throw new IllegalStateException("Esse dispositivo já tem reserva em um horário próximo (mínimo de 1 hora de intervalo).");
        }
        if (ini.isBefore(agora.plusMinutes(Regras.JANELA_MINUTOS)) && reservaDAO.sessaoAbertaEmAndamento(c, d.getIdEquipamento())) {
            List<String> livres = equipamentoDAO.livresMesmoTipo(c, d.getIdEquipamento(), ini, fim, agora, Regras.JANELA_MINUTOS);
            throw new IllegalStateException("O dispositivo " + d.getNome() + " está em uso sem hora para acabar." + sugestao(livres));
        }
    }

    public List<Reserva> getReservasDoDia(LocalDate dia) {
        LocalDate hoje = LocalDate.now();
        return Transacao.consultar(c -> {
            Map<Integer, Cliente> clientes = clientesPorId(c, hoje);
            List<Reserva> lista = new ArrayList<>();
            for (ReservaDAO.Linha l : reservaDAO.ativasNoPeriodo(c, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay())) {
                Dispositivo d = dispositivoPorId.get(l.idEquipamento);
                Cliente cl = clientes.get(l.idCliente);
                if (d != null && cl != null) {
                    lista.add(new Reserva(l.idReserva, cl, d, l.inicio, l.fim));
                }
            }
            return lista;
        });
    }

    public Set<LocalDate> getDiasComReserva(YearMonth mes) {
        // inclui a semana anterior e a seguinte, que também aparecem no calendário
        LocalDate ini = mes.atDay(1).minusDays(7);
        LocalDate fim = mes.atEndOfMonth().plusDays(15);
        return Transacao.consultar(c -> reservaDAO.diasComReserva(c, ini, fim));
    }

    public List<Dispositivo> getDispositivos() {
        return dispositivos;
    }

    public List<Cliente> getClientes() {
        LocalDate hoje = LocalDate.now();
        return Transacao.consultar(c -> clienteDAO.listarAtivos(c, hoje));
    }

    /** Saldo de crédito de tempo do cliente, em minutos (créditos dentro da validade). */
    public int getSaldoCredito(Cliente cliente) {
        LocalDate hoje = LocalDate.now();
        return Transacao.consultar(c -> creditoDAO.saldo(c, cliente.getIdCliente(), hoje));
    }

    // ---------- auxiliares ----------

    private Map<Integer, Cliente> clientesPorId(Connection c, LocalDate hoje) throws SQLException {
        Map<Integer, Cliente> m = new HashMap<>();
        for (Cliente cl : clienteDAO.listarAtivos(c, hoje)) {
            m.put(cl.getIdCliente(), cl);
        }
        return m;
    }

    private Sessao montar(SessaoDAO.Linha l, Cliente cl, int gratisDisponivel) {
        Cliente cliente = cl != null ? cl
                : new Cliente(l.idCliente, "Cliente #" + l.idCliente, LocalDate.now().minusYears(18), "", "", "");
        Dispositivo d = dispositivoPorId.get(l.idEquipamento);
        if (d == null) {
            d = new Dispositivo(l.idEquipamento, "Equipamento #" + l.idEquipamento, TipoDispositivo.PC);
        }
        return new Sessao(l.idSessao, cliente, d, l.inicio, l.previsto, l.fim, TipoTempo.valueOf(l.tipoTempo),
                l.valorHora, gratisDisponivel, l.valorTotal);
    }

    private static int idNumerico(String id, String prefixo) {
        String s = id == null ? "" : id.trim().toUpperCase(Locale.ROOT);
        if (s.startsWith(prefixo)) {
            s = s.substring(prefixo.length());
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new ErroBanco("Identificador inválido: " + id);
        }
    }
}
