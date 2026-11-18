package geeks.model;

import java.math.BigDecimal;

/** O Clube Geeks tem um único plano. */
public enum Plano {
    NENHUM("Sem plano", "0.00"),
    CLUBE("Clube Geeks", "180.00");

    private final String nome;
    private final BigDecimal valor;

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

    // Benefícios de EXEMPLO: definir com o cliente
    public String[] getBeneficios() {
        return new String[]{"[Benefício 1]", "[Benefício 2]", "[Benefício 3]"};
    }
}
