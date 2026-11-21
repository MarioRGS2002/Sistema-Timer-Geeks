package geeks.model;

/** Perfil de acesso do usuário. O gerente enxerga telas extras (ex.: Faturamento). */
public enum Perfil {
    ATENDENTE("Atendente"),
    GERENTE("Gerente");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
