package geeks.view;

import geeks.model.Cliente;
import geeks.model.Dispositivo;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Predicate;

/** Formulário da janela "Nova sessão": escolha do cliente e do dispositivo livre. */
public class FormNovaSessao extends FormularioGeeks {
    private final SeletorCliente cliente;
    private final SeletorDispositivo dispositivo;

    public FormNovaSessao(List<Cliente> clientes, List<Dispositivo> dispositivos, Predicate<Dispositivo> ocupado) {
        super("Nova sessão", "Iniciar sessão", Icone.Tipo.RAIO);
        cliente = new SeletorCliente(clientes, 3);
        dispositivo = new SeletorDispositivo(dispositivos, ocupado);

        JPanel corpo = getCorpo();
        JPanel dispositivos2 = Componentes.transparente(new BorderLayout());
        dispositivos2.add(bloco("Dispositivo", Icone.Tipo.CONTROLE, dispositivo), BorderLayout.NORTH);
        JPanel empilhado = Componentes.transparente(new BorderLayout(0, 16));
        empilhado.add(bloco("Cliente", Icone.Tipo.USUARIO_MAIS, cliente), BorderLayout.NORTH);
        empilhado.add(dispositivos2, BorderLayout.CENTER);
        corpo.add(empilhado, BorderLayout.CENTER);

        cliente.addListenerSelecao(this::atualizar);
        dispositivo.addListenerSelecao(this::atualizar);
        atualizar();
    }

    private void atualizar() {
        Cliente c = cliente.getSelecionado();
        Dispositivo d = dispositivo.getSelecionado();
        setConfirmarHabilitado(c != null && d != null);
        setResumo(c == null || d == null
                ? "Escolha o cliente e o dispositivo para iniciar."
                : c.getNome() + " em " + d.getNome() + " - " + Formatador.moeda(d.getTipo().getValorHora()) + " por hora");
    }

    public Cliente getCliente() {
        return cliente.getSelecionado();
    }

    public Dispositivo getDispositivo() {
        return dispositivo.getSelecionado();
    }
}
