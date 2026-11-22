package geeks.model;

import java.math.BigDecimal;

/** O Clube Geeks tem um único plano. A mensalidade vem da tabela plano_clube. */
public enum Plano {
    NENHUM("Sem plano", "0.00"),
    CLUBE("Clube Geeks", "180.00");

    private final String nome;
    private volatile BigDecimal valor;
    private volatile int idPlano;

    Plano(String nome, String valor) {
        this.nome = nome;
        this.valor = new BigDecimal(valor);
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public int getIdPlano() {
        return idPlano;
    }

    /** Atualiza com os dados da tabela plano_clube. */
    public void definirDoBanco(int idPlano, BigDecimal valor) {
        this.idPlano = idPlano;
        this.valor = valor;
    }

    // Benefícios de EXEMPLO: definir com o cliente
    public String[] getBeneficios() {
        return new String[]{"Ganhe uma hora diária grátis em qualquer aparelho!", "Tenha acesso a descontos exclusivos!"};
    }
}
