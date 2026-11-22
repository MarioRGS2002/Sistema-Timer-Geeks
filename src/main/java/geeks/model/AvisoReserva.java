package geeks.model;

/**
 * Aviso (não erro) ao reservar: o equipamento está em uso em tempo livre, sem hora para acabar.
 * A tela pergunta "reservar mesmo assim?" e, se o atendente confirmar, repete com ignorarAviso = true.
 */
public class AvisoReserva extends IllegalStateException {
    public AvisoReserva(String mensagem) {
        super(mensagem);
    }
}
