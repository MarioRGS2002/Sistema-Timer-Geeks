package geeks.view;

import javax.swing.Timer;
import java.util.function.DoubleConsumer;

/** Utilitário de animações: mesma duração base e curvas de aceleração em todo o sistema. */
public final class Animacao {
    private Animacao() {
    }

    /** Desaceleração suave (entra rápido e assenta devagar). */
    public static double suave(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    /** Passa um pouco do ponto final e volta (efeito "pop"). */
    public static double elastico(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    /**
     * Executa uma animação na thread de eventos do Swing.
     *
     * @param duracaoMs duração em milissegundos
     * @param atrasoMs  espera antes de começar
     * @param passo     recebe o progresso de 0 a 1
     * @param fim       executado ao terminar (pode ser null)
     */
    public static Timer executar(int duracaoMs, int atrasoMs, DoubleConsumer passo, Runnable fim) {
        final long[] inicio = {0};
        final Timer timer = new Timer(15, null);
        timer.setInitialDelay(Math.max(0, atrasoMs));
        timer.addActionListener(e -> {
            if (inicio[0] == 0) {
                inicio[0] = System.currentTimeMillis();
            }
            double p = Math.min(1.0, (System.currentTimeMillis() - inicio[0]) / (double) duracaoMs);
            passo.accept(p);
            if (p >= 1.0) {
                timer.stop();
                if (fim != null) {
                    fim.run();
                }
            }
        });
        timer.start();
        return timer;
    }
}
