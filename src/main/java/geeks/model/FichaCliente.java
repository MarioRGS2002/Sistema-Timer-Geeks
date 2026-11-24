package geeks.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tudo o que a janela "Dados do usuário" mostra sobre um cliente. */
public class FichaCliente {
    private final Cliente cliente;
    private final String respNome;
    private final String respParentesco;
    private final String respCpf;
    private final String respTelefone;
    private final LocalDate clubeAte;
    private final int saldoCreditoMinutos;
    private final LocalDateTime cadastro;
    private final long sessoes;
    private final LocalDateTime ultimaVisita;

    public FichaCliente(Cliente cliente, String respNome, String respParentesco, String respCpf, String respTelefone,
                        LocalDate clubeAte, int saldoCreditoMinutos, LocalDateTime cadastro, long sessoes,
                        LocalDateTime ultimaVisita) {
        this.cliente = cliente;
        this.respNome = respNome;
        this.respParentesco = respParentesco;
        this.respCpf = respCpf;
        this.respTelefone = respTelefone;
        this.clubeAte = clubeAte;
        this.saldoCreditoMinutos = saldoCreditoMinutos;
        this.cadastro = cadastro;
        this.sessoes = sessoes;
        this.ultimaVisita = ultimaVisita;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public boolean temResponsavel() {
        return respNome != null && !respNome.isBlank();
    }

    public String getRespNome() {
        return respNome;
    }

    public String getRespParentesco() {
        return respParentesco;
    }

    public String getRespCpf() {
        return respCpf;
    }

    public String getRespTelefone() {
        return respTelefone;
    }

    /** Último dia do Clube em dia (null = não assina). */
    public LocalDate getClubeAte() {
        return clubeAte;
    }

    public int getSaldoCreditoMinutos() {
        return saldoCreditoMinutos;
    }

    public LocalDateTime getCadastro() {
        return cadastro;
    }

    public long getSessoes() {
        return sessoes;
    }

    public LocalDateTime getUltimaVisita() {
        return ultimaVisita;
    }
}
