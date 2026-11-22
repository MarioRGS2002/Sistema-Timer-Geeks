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

    /**
     * Cadastra o cliente com a data de nascimento e, se houver, o responsável (nome, CPF, telefone,
     * parentesco). Menor de 18 anos exige responsável com nome e CPF válido.
     */
    public Cliente cadastrarCliente(String nome, String nascimentoTxt, String telefone, String cpf,
                                    String respNome, String respCpf, String respTelefone, String respParentesco) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do cliente.");
        }
        if (nome.trim().length() > 100) {
            throw new IllegalArgumentException("O nome pode ter no máximo 100 caracteres.");
        }
        LocalDate hoje = LocalDate.now();
        LocalDate nascimento = lerData(nascimentoTxt);
        if (nascimento == null || nascimento.isAfter(hoje) || nascimento.isBefore(hoje.minusYears(120))) {
            throw new IllegalArgumentException("Informe a data de nascimento no formato dd/mm/aaaa (ex.: 15/03/2010).");
        }
        String cpfNumeros = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (!cpfValido(cpfNumeros)) {
            throw new IllegalArgumentException("CPF inválido.");
        }
        String tel = telefone == null ? "" : telefone.trim();
        if (tel.length() > 20) {
            throw new IllegalArgumentException("Telefone inválido.");
        }
        boolean menor = nascimento.plusYears(18).isAfter(hoje);
        String rNome = respNome == null ? "" : respNome.trim();
        String rCpf = respCpf == null ? "" : respCpf.replaceAll("\\D", "");
        String rTel = respTelefone == null ? "" : respTelefone.trim();
        String rPar = respParentesco == null ? "" : respParentesco.trim();
        boolean temResponsavel = !rNome.isEmpty() || !rCpf.isEmpty();
        if (menor && !temResponsavel) {
            throw new IllegalArgumentException("Clientes menores de 18 anos precisam de um responsável (nome e CPF).");
        }
        if (temResponsavel) {
            if (rNome.isEmpty() || rNome.length() > 100) {
                throw new IllegalArgumentException("Informe o nome do responsável (até 100 caracteres).");
            }
            if (!cpfValido(rCpf)) {
                throw new IllegalArgumentException("CPF do responsável inválido.");
            }
            if (rCpf.equals(cpfNumeros)) {
                throw new IllegalArgumentException("O CPF do responsável não pode ser o mesmo do cliente.");
            }
            if (rTel.length() > 20 || rPar.length() > 50) {
                throw new IllegalArgumentException("Telefone ou parentesco do responsável grande demais.");
            }
        }
        return Transacao.executar(c -> {
            if (clienteDAO.existeCpf(c, cpfNumeros)) {
                throw new IllegalArgumentException("Já existe um cliente cadastrado com esse CPF.");
            }
            int id = clienteDAO.inserir(c, nome.trim(), nascimento, tel, cpfNumeros);
            if (temResponsavel) {
                clienteDAO.inserirResponsavel(c, id, rNome, rCpf, rTel, rPar);
            }
            return clienteDAO.porId(c, id, hoje).orElseThrow();
        });
    }

    /** dd/mm/aaaa, d/m/aaaa ou só os 8 números (ddmmaaaa). null se inválida. */
    static LocalDate lerData(String txt) {
        if (txt == null) {
            return null;
        }
        String t = txt.trim();
        try {
            if (t.matches("\\d{8}")) {
                return LocalDate.of(Integer.parseInt(t.substring(4)), Integer.parseInt(t.substring(2, 4)),
                        Integer.parseInt(t.substring(0, 2)));
            }
            String[] p = t.split("[/.-]");
            if (p.length == 3 && p[2].length() == 4) {
                return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
            }
        } catch (RuntimeException e) {
            return null; // dia/mês inexistente (ex.: 31/02)
        }
        return null;
    }

    /** Formas de pagamento cadastradas no banco (Dinheiro, PIX, Débito, Crédito...). */
    public List<String> getFormasPagamento() {
        return Transacao.consultar(pagamentoDAO::formas);
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
        assinarPlano(cliente, plano, Regras.FORMA_PAGAMENTO_PADRAO);
    }

    /** @param formaPagamento forma da mensalidade (Dinheiro, PIX, Débito, Crédito) */
    public void assinarPlano(Cliente cliente, Plano plano, String formaPagamento) {
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
                    idForma(c, formaPagamento), idFuncionario, null, null, idAssinatura);
            return null;
        });
        cliente.setPlano(Plano.CLUBE);
    }

    /** Forma de pagamento escolhida na tela (vazia = padrão). */
    private int idForma(Connection c, String forma) throws SQLException {
        return pagamentoDAO.idForma(c, forma == null || forma.isBlank() ? Regras.FORMA_PAGAMENTO_PADRAO : forma);
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

    /** Sessão em tempo ABERTO (livre). */
    public Sessao iniciarSessao(Cliente cliente, Dispositivo dispositivo) {
        return iniciarSessao(cliente, dispositivo, null);
    }

    /**
     * Abre sessão.
     * <ul>
     *   <li>{@code minutos == null}: tempo ABERTO (livre, sem hora para acabar; mínimo de 1 hora).</li>
     *   <li>{@code minutos > 0}: tempo FECHADO até agora + minutos.</li>
     *   <li>Se o cliente tem reserva neste equipamento "para agora", a sessão vira essa reserva:
     *       FECHADO até o fim da reserva (ou mais, se escolheu mais tempo) e a entrada paga
     *       é descontada no encerramento.</li>
     *   <li>Reserva de OUTRO cliente antes do fim (+ folga J): recusa e sugere livres do mesmo tipo.</li>
     * </ul>
     */
    public Sessao iniciarSessao(Cliente cliente, Dispositivo dispositivo, Integer minutos) {
        if (minutos != null && (minutos < 15 || minutos > 12 * 60)) {
            throw new IllegalArgumentException("Tempo fechado deve ser entre 15 minutos e 12 horas.");
        }
        int idFuncionario = Contexto.idFuncionario();
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        int idSessao = Transacao.executar(c -> {
            equipamentoDAO.travar(c, dispositivo.getIdEquipamento());
            if (sessaoDAO.clienteEmSessaoAtiva(c, cliente.getIdCliente())) {
                throw new IllegalStateException(cliente.getNome() + " já está em uma sessão ativa.");
            }
            Optional<ReservaDAO.Linha> daReserva = reservaDAO.doClienteParaAgora(c, dispositivo.getIdEquipamento(),
                    cliente.getIdCliente(), agora, Regras.JANELA_MINUTOS);
            LocalDateTime previsto = minutos == null ? null : agora.plusMinutes(minutos);
            if (daReserva.isPresent() && (previsto == null || previsto.isBefore(daReserva.get().fim))) {
                previsto = daReserva.get().fim; // reserva é sempre tempo FECHADO
            }
            LocalDateTime limite = previsto == null ? agora : previsto;
            Optional<ReservaDAO.Linha> prox = reservaDAO.proximaAtiva(c, dispositivo.getIdEquipamento(), agora);
            if (prox.isPresent() && prox.get().idCliente != cliente.getIdCliente()
                    && prox.get().inicio.isBefore(limite.plusMinutes(Regras.JANELA_MINUTOS))) {
                List<String> livres = equipamentoDAO.livresMesmoTipo(c, dispositivo.getIdEquipamento(), agora,
                        prox.get().fim, agora, Regras.JANELA_MINUTOS);
                throw new IllegalStateException("O dispositivo " + dispositivo.getNome() + " tem reserva às "
                        + prox.get().inicio.toLocalTime() + "." + sugestao(livres));
            }
            try {
                TipoTempo tipo = previsto == null ? TipoTempo.ABERTO : TipoTempo.FECHADO;
                int id = sessaoDAO.abrir(c, tipo.name(), agora, previsto, dispositivo.getTipo().getValorHora(),
                        dispositivo.getIdEquipamento(), idFuncionario, cliente.getIdCliente());
                if (daReserva.isPresent()) {
                    reservaDAO.vincularSessao(c, daReserva.get().idReserva, id);
                }
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
        encerrarSessao(id, Regras.FORMA_PAGAMENTO_PADRAO, false);
        return null;
    }

    /**
     * Prévia do encerramento AGORA (para a janela de confirmação): tempo usado, hora grátis,
     * crédito usado/gerado, entrada já paga na reserva e o valor a pagar. Não grava nada.
     */
    public PreviaEncerramento previaEncerramento(String id, boolean usarCredito) {
        int idSessao = idNumerico(id, "S");
        LocalDateTime fim = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return Transacao.consultar(c -> calcularEncerramento(c, sessaoDAO.travarAtiva(c, idSessao), fim, usarCredito));
    }

    /**
     * Encerra a sessão numa única transação: trava a sessão, grava fim e valor, a hora grátis
     * do Clube usada, o crédito usado (abatido do saldo), os pagamentos (grátis = valor 0 sem
     * forma; o resto na forma escolhida, já descontada a entrada da reserva) e o crédito do
     * tempo pago e não usado. Em qualquer falha, nada fica gravado (rollback).
     */
    public PreviaEncerramento encerrarSessao(String id, String formaPagamento, boolean usarCredito) {
        int idSessao = idNumerico(id, "S");
        int idFuncionario = Contexto.idFuncionario();
        LocalDateTime fim = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return Transacao.executar(c -> encerrar(c, idSessao, fim, idFuncionario, formaPagamento, usarCredito));
    }

    /** Encerramento em uma conexão já em transação (usado também pelos testes). */
    public void encerrar(Connection c, int idSessao, LocalDateTime fim, int idFuncionario) throws SQLException {
        encerrar(c, idSessao, fim, idFuncionario, Regras.FORMA_PAGAMENTO_PADRAO, false);
    }

    public PreviaEncerramento encerrar(Connection c, int idSessao, LocalDateTime fim, int idFuncionario,
                                       String formaPagamento, boolean usarCredito) throws SQLException {
        SessaoDAO.Linha s = sessaoDAO.travarAtiva(c, idSessao);
        PreviaEncerramento r = calcularEncerramento(c, s, fim, usarCredito);
        Cobranca cob = r.getCobranca();
        sessaoDAO.encerrar(c, idSessao, fim, cob.getValor());
        sessaoDAO.definirGratis(c, idSessao, s.idCliente, cob.getSegundosGratis());
        if (cob.getSegundosCredito() > 0) {
            sessaoDAO.definirCredito(c, idSessao, s.idCliente, cob.getSegundosCredito());
            creditoDAO.consumir(c, s.idCliente, s.idTipo, cob.getMinutosCreditoUsados(), fim.toLocalDate());
        }
        if (cob.getSegundosGratis() > 0) {
            pagamentoDAO.registrar(c, fim, BigDecimal.ZERO.setScale(2), "SAIDA", PagamentoDAO.SESSAO, null,
                    idFuncionario, idSessao, null, null);
        }
        if (r.getAPagar().signum() > 0) {
            pagamentoDAO.registrar(c, fim, r.getAPagar(), "SAIDA", PagamentoDAO.SESSAO,
                    idForma(c, formaPagamento), idFuncionario, idSessao, null, null);
        }
        if (cob.getMinutosCredito() > 0) {
            creditoDAO.gerar(c, s.idCliente, s.idTipo, idSessao, cob.getMinutosCredito(), fim);
        }
        equipamentoDAO.definirStatus(c, s.idEquipamento, "DISPONIVEL");
        return r;
    }

    private PreviaEncerramento calcularEncerramento(Connection c, SessaoDAO.Linha s, LocalDateTime fim,
                                                    boolean usarCredito) throws SQLException {
        LocalDate dia = s.inicio.toLocalDate();
        int gratisDisponivel = 0;
        if (assinaturaDAO.emDia(c, s.idCliente, dia)) {
            gratisDisponivel = Math.max(0, Cobranca.SEGUNDOS_HORA - sessaoDAO.gratisUsadoNoDia(c, s.idCliente, dia));
        }
        int saldo = creditoDAO.saldoDoTipo(c, s.idCliente, s.idTipo, fim.toLocalDate());
        Cobranca cob = Cobranca.calcular(TipoTempo.valueOf(s.tipoTempo), s.inicio, s.previsto, fim, s.valorHora,
                gratisDisponivel, usarCredito ? saldo * 60 : 0);
        BigDecimal entrada = reservaDAO.entradaPagaDaSessao(c, s.idSessao);
        return new PreviaEncerramento(cob, saldo, entrada);
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
        return registrarReserva(cliente, dispositivo, dataHora, Regras.DURACAO_RESERVA_MINUTOS, null, false);
    }

    /** Valor previsto de uma reserva (valor/hora do tipo x duração). */
    public static BigDecimal valorReserva(Dispositivo d, int minutos) {
        return d.getTipo().getValorHora().multiply(BigDecimal.valueOf(minutos))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** Entrada mínima de uma reserva (percentual configurado, padrão 50%). */
    public static BigDecimal entradaReserva(Dispositivo d, int minutos) {
        return valorReserva(d, minutos).multiply(Regras.PERCENTUAL_ENTRADA).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Registra a reserva (sempre tempo FECHADO).
     * @param minutos            duração (30 min a 8 h)
     * @param formaEntrada       forma em que o cliente pagou a entrada agora; null = ainda não pagou
     * @param ignorarAviso       true quando o atendente já confirmou o aviso de sessão sem hora para acabar
     * @throws AvisoReserva      se o equipamento está em uso sem hora para acabar (pode confirmar e repetir)
     */
    public Reserva registrarReserva(Cliente cliente, Dispositivo dispositivo, LocalDateTime dataHora, int minutos,
                                    String formaEntrada, boolean ignorarAviso) {
        LocalDateTime agora = LocalDateTime.now();
        if (dataHora.isBefore(agora)) {
            throw new IllegalArgumentException("A reserva precisa ser para um horário futuro.");
        }
        if (minutos < 30 || minutos > 8 * 60) {
            throw new IllegalArgumentException("A reserva deve durar de 30 minutos a 8 horas.");
        }
        LocalDateTime fim = dataHora.plusMinutes(minutos);
        BigDecimal valor = valorReserva(dispositivo, minutos);
        BigDecimal entrada = entradaReserva(dispositivo, minutos);
        Integer idFuncionario = formaEntrada == null ? null : Contexto.idFuncionario();
        int id = Transacao.executar(c -> {
            equipamentoDAO.travar(c, dispositivo.getIdEquipamento());
            verificarConflito(c, dispositivo, 0, dataHora, fim, agora, ignorarAviso);
            int r = reservaDAO.inserir(c, dataHora, fim, valor, Regras.PERCENTUAL_ENTRADA, entrada,
                    cliente.getIdCliente(), dispositivo.getIdEquipamento());
            if (formaEntrada != null && entrada.signum() > 0) {
                pagamentoDAO.registrar(c, LocalDateTime.now(), entrada, "ENTRADA", PagamentoDAO.RESERVA,
                        idForma(c, formaEntrada), idFuncionario, null, r, null);
                reservaDAO.confirmarEntrada(c, r);
            }
            return r;
        });
        return new Reserva(id, cliente, dispositivo, dataHora, fim);
    }

    public void alterarReserva(Reserva reserva, LocalDateTime novaDataHora) {
        alterarReserva(reserva, novaDataHora, false);
    }

    /** Muda o horário mantendo a duração. @throws AvisoReserva como em registrarReserva */
    public void alterarReserva(Reserva reserva, LocalDateTime novaDataHora, boolean ignorarAviso) {
        LocalDateTime agora = LocalDateTime.now();
        if (novaDataHora.isBefore(agora)) {
            throw new IllegalArgumentException("A reserva precisa ser para um horário futuro.");
        }
        Transacao.executar(c -> {
            equipamentoDAO.travar(c, reserva.getDispositivo().getIdEquipamento());
            ReservaDAO.Linha atual = reservaDAO.travar(c, reserva.getIdReserva());
            long duracao = ChronoUnit.MINUTES.between(atual.inicio, atual.fim);
            LocalDateTime novoFim = novaDataHora.plusMinutes(duracao);
            verificarConflito(c, reserva.getDispositivo(), reserva.getIdReserva(), novaDataHora, novoFim, agora, ignorarAviso);
            reservaDAO.alterarHorario(c, reserva.getIdReserva(), novaDataHora, novoFim);
            return null;
        });
    }

    /** Cancelar = status 'CANCELADA' (a reserva não é apagada, fica no histórico). */
    public void cancelarReserva(Reserva reserva) {
        Transacao.executar(c -> reservaDAO.cancelar(c, reserva.getIdReserva()));
    }

    private void verificarConflito(Connection c, Dispositivo d, int idIgnorar, LocalDateTime ini, LocalDateTime fim,
                                   LocalDateTime agora, boolean ignorarAviso) throws SQLException {
        if (reservaDAO.conflitos(c, d.getIdEquipamento(), idIgnorar, ini, fim, Regras.JANELA_MINUTOS) > 0) {
            throw new IllegalStateException("Esse dispositivo já tem reserva em um horário próximo (mínimo de "
                    + Regras.JANELA_MINUTOS + " minutos de intervalo).");
        }
        Optional<LocalDateTime> fechadaAte = reservaDAO.sessaoFechadaAte(c, d.getIdEquipamento());
        if (fechadaAte.isPresent() && ini.isBefore(fechadaAte.get().plusMinutes(Regras.JANELA_MINUTOS))) {
            List<String> livres = equipamentoDAO.livresMesmoTipo(c, d.getIdEquipamento(), ini, fim, agora, Regras.JANELA_MINUTOS);
            throw new IllegalStateException("O dispositivo " + d.getNome() + " está em uso até "
                    + fechadaAte.get().toLocalTime() + "." + sugestao(livres));
        }
        if (!ignorarAviso && ini.isBefore(agora.plusMinutes(Regras.JANELA_MINUTOS))
                && reservaDAO.sessaoAbertaEmAndamento(c, d.getIdEquipamento())) {
            List<String> livres = equipamentoDAO.livresMesmoTipo(c, d.getIdEquipamento(), ini, fim, agora, Regras.JANELA_MINUTOS);
            throw new AvisoReserva("O dispositivo " + d.getNome() + " está em uso sem hora para acabar." + sugestao(livres));
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
