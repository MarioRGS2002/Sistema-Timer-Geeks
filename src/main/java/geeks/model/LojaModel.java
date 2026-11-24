package geeks.model;

import geeks.dados.EquipamentoDAO;
import geeks.dados.Transacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela Loja (só gerente): equipamentos (cadastrar, manutenção) e preços (valor da hora por tipo e
 * mensalidade do Clube). Mudar o preço não muda sessões já abertas (o valor da hora fica gravado
 * em cada sessão quando ela começa).
 */
public class LojaModel {
    public static final BigDecimal PRECO_MAXIMO = new BigDecimal("1000.00");
    private final EquipamentoDAO dao = new EquipamentoDAO();

    /** Um equipamento como a tela Loja mostra. */
    public record Equipamento(int id, String nome, String tipo, String status, int reservasFuturas) {
        public boolean emManutencao() {
            return "INDISPONIVEL".equals(status);
        }

        public boolean emUso() {
            return "EM_USO".equals(status);
        }
    }

    public List<Equipamento> equipamentos() {
        Contexto.exigirGerente();
        LocalDateTime agora = LocalDateTime.now();
        return Transacao.consultar(c -> dao.listarTodos(c, agora).stream()
                .map(r -> new Equipamento((int) r[0], (String) r[1], (String) r[2], (String) r[3], (int) r[4])).toList());
    }

    public List<String> tipos() {
        return Transacao.consultar(c -> List.copyOf(dao.precosPorTipo(c).keySet()));
    }

    public void cadastrar(String nome, String tipo) {
        Contexto.exigirGerente();
        String n = nome == null ? "" : nome.trim();
        if (n.isEmpty() || n.length() > 50) {
            throw new IllegalArgumentException("Dê um nome de 1 a 50 caracteres ao equipamento (ex.: PC05).");
        }
        int novos = Transacao.executar(c -> dao.inserir(c, n, tipo));
        if (novos == 0) {
            throw new IllegalArgumentException("Já existe um equipamento chamado " + n + ".");
        }
    }

    /**
     * Coloca em manutenção (some das telas de sessão e reserva) ou volta a usar.
     * @return quantas reservas futuras ativas o equipamento tem (para avisar o gerente)
     */
    public int manutencao(int idEquipamento, boolean emManutencao) {
        Contexto.exigirGerente();
        LocalDateTime agora = LocalDateTime.now();
        return Transacao.executar(c -> {
            String status = dao.statusTravando(c, idEquipamento);
            if (emManutencao && "EM_USO".equals(status)) {
                throw new IllegalStateException("Esse equipamento está em uso agora. Encerre a sessão antes de colocar em manutenção.");
            }
            dao.definirManutencao(c, idEquipamento, emManutencao);
            return dao.listarTodos(c, agora).stream().filter(r -> (int) r[0] == idEquipamento)
                    .mapToInt(r -> (int) r[4]).findFirst().orElse(0);
        });
    }

    /** Preços atuais: valor da hora de cada tipo (na ordem) e, por último, "Clube Geeks" -> mensalidade. */
    public Map<String, BigDecimal> precos() {
        Map<String, BigDecimal> m = new LinkedHashMap<>(Transacao.consultar(dao::precosPorTipo));
        m.put(Plano.CLUBE.getNome(), Plano.CLUBE.getValor());
        return m;
    }

    /** Grava os preços. Valores de 0 a 1000, com centavos. As telas passam a usar os novos na hora. */
    public void alterarPrecos(Map<String, BigDecimal> valorHoraPorTipo, BigDecimal mensalidadeClube) {
        Contexto.exigirGerente();
        for (BigDecimal v : valorHoraPorTipo.values()) {
            validarPreco(v);
        }
        validarPreco(mensalidadeClube);
        Transacao.executar(c -> {
            for (Map.Entry<String, BigDecimal> e : valorHoraPorTipo.entrySet()) {
                dao.alterarValorHora(c, e.getKey(), e.getValue().setScale(2, RoundingMode.HALF_UP));
            }
            dao.alterarMensalidadeClube(c, mensalidadeClube.setScale(2, RoundingMode.HALF_UP));
            dao.carregarTiposEPlano(c); // atualiza os valores que as telas mostram
            return null;
        });
    }

    private static void validarPreco(BigDecimal v) {
        if (v == null || v.signum() < 0 || v.compareTo(PRECO_MAXIMO) > 0) {
            throw new IllegalArgumentException("Preço inválido: use um valor de 0,00 a 1.000,00.");
        }
    }
}
