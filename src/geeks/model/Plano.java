package geeks.model;

import java.math.BigDecimal;

public enum Plano {
    // Mensalidades e benefícios de EXEMPLO: definir com o cliente
    NENHUM("Sem plano", "0.00"),
    BASICO("Básico", "29.90"),
    PREMIUM("Premium", "49.90");

    private final String nome;
    private final BigDecimal mensalidade;

    Plano(String nome, String mensalidade) {
        this.nome = nome;
        this.mensalidade = new BigDecimal(mensalidade);
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getMensalidade() {
        return mensalidade;
    }

    public String[] getBeneficios() {
        return new String[]{"[Benefício 1]", "[Benefício 2]", "[Benefício 3]"};
    }
}
