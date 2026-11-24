package geeks.view;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Máscara de digitação: o campo só aceita números e a pontuação aparece sozinha.
 * <ul>
 *   <li>TELEFONE: (11) 91234-5678 ou (11) 1234-5678 — o DDD entre parênteses é posto automaticamente;</li>
 *   <li>CPF: 000.000.000-00, no máximo 11 números;</li>
 *   <li>DATA: dd/mm/aaaa.</li>
 * </ul>
 */
public enum Mascara {
    TELEFONE(11), CPF(11), DATA(8);

    private final int maxDigitos;

    Mascara(int maxDigitos) {
        this.maxDigitos = maxDigitos;
    }

    public void aplicar(JTextComponent campo) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new Filtro(campo));
    }

    /** Formata uma sequência de números (já limitada). */
    String formatar(String d) {
        switch (this) {
            case CPF:
                return grupo(d, 0, 3) + sep(d, 3, ".") + grupo(d, 3, 6) + sep(d, 6, ".") + grupo(d, 6, 9)
                        + sep(d, 9, "-") + grupo(d, 9, 11);
            case DATA:
                return grupo(d, 0, 2) + sep(d, 2, "/") + grupo(d, 2, 4) + sep(d, 4, "/") + grupo(d, 4, 8);
            default:
                if (d.isEmpty()) {
                    return "";
                }
                String ddd = "(" + grupo(d, 0, 2) + (d.length() >= 2 ? ") " : "");
                String resto = d.length() > 2 ? d.substring(2) : "";
                int corte = resto.length() > 8 ? 5 : 4; // celular (9 dígitos) ou fixo (8)
                if (resto.length() <= 4) {
                    return ddd + resto;
                }
                return ddd + resto.substring(0, Math.min(corte, resto.length())) + "-" + resto.substring(Math.min(corte, resto.length()));
        }
    }

    private static String grupo(String d, int ini, int fim) {
        return d.length() <= ini ? "" : d.substring(ini, Math.min(fim, d.length()));
    }

    private static String sep(String d, int depoisDe, String s) {
        return d.length() > depoisDe ? s : "";
    }

    private final class Filtro extends DocumentFilter {
        private final JTextComponent campo;

        Filtro(JTextComponent campo) {
            this.campo = campo;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, texto, attr);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            String atual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String novo = atual.substring(0, offset) + atual.substring(offset + length);
            int cursor = offset;
            // Backspace em cima da pontuação apaga o número anterior (senão a máscara "devolve" o sinal e trava)
            if (length == 1 && !Character.isDigit(atual.charAt(offset))) {
                int i = offset - 1;
                while (i >= 0 && !Character.isDigit(atual.charAt(i))) {
                    i--;
                }
                if (i >= 0) {
                    novo = atual.substring(0, i) + atual.substring(i + 1);
                    cursor = i;
                }
            }
            aplicar(fb, novo, cursor, null);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String texto, AttributeSet attr) throws BadLocationException {
            String atual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String inserido = texto == null ? "" : texto;
            String novo = atual.substring(0, offset) + inserido + atual.substring(offset + length);
            aplicar(fb, novo, offset + inserido.length(), attr);
        }

        /** Reescreve o campo formatado e põe o cursor logo depois do mesmo número em que estava. */
        private void aplicar(FilterBypass fb, String texto, int cursorNoTexto, AttributeSet attr) throws BadLocationException {
            int digitosAntes = Math.min(maxDigitos, texto.substring(0, Math.min(cursorNoTexto, texto.length()))
                    .replaceAll("\\D", "").length());
            String d = texto.replaceAll("\\D", "");
            if (d.length() > maxDigitos) {
                d = d.substring(0, maxDigitos);
            }
            String formatado = formatar(d);
            fb.replace(0, fb.getDocument().getLength(), formatado, attr);
            int pos = 0;
            for (int vistos = 0; pos < formatado.length() && vistos < digitosAntes; pos++) {
                if (Character.isDigit(formatado.charAt(pos))) {
                    vistos++;
                }
            }
            if (digitosAntes == d.length()) {
                pos = formatado.length(); // digitando no fim: cursor no fim (depois de ")" ou "-" automáticos)
            }
            campo.setCaretPosition(Math.min(pos, formatado.length()));
        }
    }
}
