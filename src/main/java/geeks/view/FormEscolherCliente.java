package geeks.view;

import geeks.model.Cliente;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

/** Quando a busca por nome encontra mais de um cliente (ex.: duas "Ana"), o atendente escolhe qual. */
public class FormEscolherCliente extends FormularioGeeks {
    private final SeletorCliente seletor;

    public FormEscolherCliente(String termo, List<Cliente> clientes) {
        this("Escolher cliente", clientes.size() + " clientes com “" + termo + "”", "Escolher", clientes);
    }

    /** Versão genérica (ex.: "Adicionar jogador"). */
    public FormEscolherCliente(String titulo, String tituloLista, String textoConfirmar, List<Cliente> clientes) {
        super(titulo, textoConfirmar, Icone.Tipo.CHECK);
        seletor = new SeletorCliente(clientes, 4);
        JPanel envolve = Componentes.transparente(new BorderLayout());
        envolve.add(bloco(tituloLista, Icone.Tipo.USUARIO_MAIS, seletor), BorderLayout.NORTH);
        getCorpo().add(envolve, BorderLayout.CENTER);
        seletor.addListenerSelecao(this::atualizar);
        atualizar();
    }

    private void atualizar() {
        Cliente c = seletor.getSelecionado();
        setConfirmarHabilitado(c != null);
        setResumo(c == null ? "Confira pelo CPF e escolha o cliente." : c.getNome() + " - CPF " + Formatador.cpf(c.getCpf()));
    }

    public Cliente getSelecionado() {
        return seletor.getSelecionado();
    }
}
