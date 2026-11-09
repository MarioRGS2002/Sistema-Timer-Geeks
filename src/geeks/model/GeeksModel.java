package geeks.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Regras de negócio do sistema Geeks (sem nenhuma dependência de interface gráfica). */
public class GeeksModel {
    private final List<Dispositivo> dispositivos = new ArrayList<>();
    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Sessao> sessoes = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();

    public GeeksModel() {
        carregarDadosDeExemplo();
    }

    // ---------- Clientes ----------

    public Cliente cadastrarCliente(String nome, String idadeTxt, String telefone, String cpf, String responsavel) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do cliente.");
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
        if (buscarPorCpf(cpfNumeros) != null) {
            throw new IllegalArgumentException("Já existe um cliente cadastrado com esse CPF.");
        }
        if (idade < 18 && (responsavel == null || responsavel.isBlank())) {
            throw new IllegalArgumentException("Clientes menores de 18 anos precisam de um responsável.");
        }
        Cliente cliente = new Cliente(nome.trim(), idade, telefone == null ? "" : telefone.trim(), cpfNumeros,
                responsavel == null ? "" : responsavel.trim());
        clientes.add(cliente);
        return cliente;
    }

    /** Busca por CPF (somente números) ou por parte do nome. */
    public Cliente buscarCliente(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String numeros = termo.replaceAll("\\D", "");
        if (numeros.length() == 11) {
            return buscarPorCpf(numeros);
        }
        String t = termo.trim().toLowerCase(Locale.ROOT);
        return clientes.stream().filter(c -> c.getNome().toLowerCase(Locale.ROOT).contains(t)).findFirst().orElse(null);
    }

    private Cliente buscarPorCpf(String cpf) {
        return clientes.stream().filter(c -> c.getCpf().equals(cpf)).findFirst().orElse(null);
    }

    public void assinarPlano(Cliente cliente, Plano plano) {
        cliente.setPlano(plano);
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

    public Sessao iniciarSessao(Cliente cliente, Dispositivo dispositivo) {
        if (dispositivoOcupado(dispositivo)) {
            throw new IllegalStateException("O dispositivo " + dispositivo.getNome() + " já está em uso.");
        }
        Sessao sessao = new Sessao(cliente, dispositivo, LocalDateTime.now());
        sessoes.add(sessao);
        return sessao;
    }

    public Sessao encerrarSessao(String id) {
        Sessao sessao = sessoes.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        if (sessao != null) {
            sessao.encerrar();
        }
        return sessao;
    }

    public boolean dispositivoOcupado(Dispositivo dispositivo) {
        return sessoes.stream().anyMatch(s -> s.isAtiva() && s.getDispositivo() == dispositivo);
    }

    public List<Dispositivo> getDispositivosLivres() {
        return dispositivos.stream().filter(d -> !dispositivoOcupado(d)).collect(Collectors.toList());
    }

    public List<Sessao> getSessoesAtivas() {
        return sessoes.stream().filter(Sessao::isAtiva).collect(Collectors.toList());
    }

    public BigDecimal getFaturamentoHoje() {
        LocalDate hoje = LocalDate.now();
        return sessoes.stream().filter(s -> s.getInicio().toLocalDate().equals(hoje))
                .map(Sessao::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------- Histórico ----------

    public List<Sessao> filtrarHistorico(PeriodoHistorico periodo, String texto) {
        LocalDate inicio = periodo.inicio(LocalDate.now());
        String t = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return sessoes.stream()
                .filter(s -> !s.isAtiva() && !s.getFim().toLocalDate().isBefore(inicio))
                .filter(s -> t.isEmpty()
                        || s.getDispositivo().getNome().toLowerCase(Locale.ROOT).contains(t)
                        || s.getDispositivo().getTipo().getRotulo().toLowerCase(Locale.ROOT).contains(t)
                        || s.getCliente().getNome().toLowerCase(Locale.ROOT).contains(t))
                .sorted(Comparator.comparing(Sessao::getFim).reversed())
                .collect(Collectors.toList());
    }

    // ---------- Reservas ----------

    public Reserva registrarReserva(Cliente cliente, Dispositivo dispositivo, LocalDateTime dataHora) {
        if (dataHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A reserva precisa ser para um horário futuro.");
        }
        verificarConflito(dispositivo, dataHora, null);
        Reserva reserva = new Reserva(cliente, dispositivo, dataHora);
        reservas.add(reserva);
        return reserva;
    }

    public void alterarReserva(Reserva reserva, LocalDateTime novaDataHora) {
        if (novaDataHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A reserva precisa ser para um horário futuro.");
        }
        verificarConflito(reserva.getDispositivo(), novaDataHora, reserva);
        reserva.setDataHora(novaDataHora);
    }

    public void cancelarReserva(Reserva reserva) {
        reservas.remove(reserva);
    }

    private void verificarConflito(Dispositivo dispositivo, LocalDateTime dataHora, Reserva ignorada) {
        boolean conflito = reservas.stream().anyMatch(r -> r != ignorada && r.getDispositivo() == dispositivo
                && Math.abs(Duration.between(r.getDataHora(), dataHora).toMinutes()) < 60);
        if (conflito) {
            throw new IllegalStateException("Esse dispositivo já tem reserva em um horário próximo (mínimo de 1 hora de intervalo).");
        }
    }

    public List<Reserva> getReservasDoDia(LocalDate dia) {
        return reservas.stream().filter(r -> r.getDataHora().toLocalDate().equals(dia))
                .sorted(Comparator.comparing(Reserva::getDataHora)).collect(Collectors.toList());
    }

    public Set<LocalDate> getDiasComReserva(YearMonth mes) {
        Set<LocalDate> dias = new TreeSet<>();
        for (Reserva r : reservas) {
            dias.add(r.getDataHora().toLocalDate());
        }
        return dias;
    }

    public List<Dispositivo> getDispositivos() {
        return dispositivos;
    }

    public List<Cliente> getClientes() {
        return clientes;
    }

    // ---------- Dados de exemplo (remover quando houver persistência) ----------

    private void carregarDadosDeExemplo() {
        Dispositivo pc1 = new Dispositivo("PC01", TipoDispositivo.PC);
        Dispositivo pc2 = new Dispositivo("PC02", TipoDispositivo.PC);
        Dispositivo ps5 = new Dispositivo("Ps5", TipoDispositivo.PLAYSTATION);
        Dispositivo nsw = new Dispositivo("Nintendo Switch", TipoDispositivo.SWITCH);
        Dispositivo xad = new Dispositivo("Xadrez", TipoDispositivo.XADREZ);
        dispositivos.addAll(List.of(pc1, pc2, ps5, nsw, xad));

        Cliente gabriel = new Cliente("Gabriel Dantas", 22, "(75) 99999-0001", "11144477735", "");
        Cliente mario = new Cliente("Mario Romero", 25, "(75) 99999-0002", "52998224725", "");
        Cliente wby = new Cliente("Wbyrathan", 19, "(75) 99999-0003", "39053344705", "");
        Cliente matheus = new Cliente("Matheus Barbosa", 20, "(75) 99999-0004", "16899535009", "");
        Cliente nicolas = new Cliente("Nicolas Aragão", 16, "(75) 99999-0005", "11122233396", "Fernanda Aragão");
        gabriel.setPlano(Plano.PREMIUM);
        clientes.addAll(List.of(gabriel, mario, wby, matheus, nicolas));

        LocalDateTime agora = LocalDateTime.now();
        sessoes.add(new Sessao(gabriel, pc1, agora.minusMinutes(92)));
        sessoes.add(new Sessao(mario, pc2, agora.minusMinutes(48)));
        sessoes.add(new Sessao(wby, xad, agora.minusMinutes(130)));
        sessoes.add(new Sessao(matheus, ps5, agora.minusMinutes(65)));
        sessoes.add(new Sessao(nicolas, nsw, agora.minusMinutes(25)));

        historicoExemplo(gabriel, pc1, agora.minusDays(1), 90);
        historicoExemplo(gabriel, ps5, agora.minusDays(3), 60);
        historicoExemplo(mario, pc2, agora.minusDays(12), 120);
        historicoExemplo(nicolas, nsw, agora.minusDays(40), 45);
        historicoExemplo(matheus, ps5, agora.minusDays(100), 80);

        reservas.add(new Reserva(gabriel, pc1, agora.withHour(21).withMinute(0).withSecond(0).withNano(0)));
        reservas.add(new Reserva(matheus, ps5, agora.withHour(22).withMinute(30).withSecond(0).withNano(0)));
        reservas.add(new Reserva(mario, pc2, agora.plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)));
    }

    private void historicoExemplo(Cliente c, Dispositivo d, LocalDateTime fim, int minutos) {
        Sessao s = new Sessao(c, d, fim.minusMinutes(minutos));
        s.encerrarEm(fim);
        sessoes.add(s);
    }
}
