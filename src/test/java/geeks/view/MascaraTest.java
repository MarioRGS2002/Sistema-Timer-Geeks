package geeks.view;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Máscaras de digitação (sem banco, sem tela visível). */
class MascaraTest {

    private static String digitar(Mascara m, String texto) {
        JTextField f = new JTextField();
        m.aplicar(f);
        for (char ch : texto.toCharArray()) {
            f.replaceSelection(String.valueOf(ch)); // como se digitasse uma tecla por vez
        }
        return f.getText();
    }

    @Test
    void telefoneCelularEFixoComDddAutomatico() {
        assertEquals("(11) 91234-5678", digitar(Mascara.TELEFONE, "11912345678"));
        assertEquals("(11) 3456-7890", digitar(Mascara.TELEFONE, "1134567890"));
        assertEquals("(11) 91234-5678", digitar(Mascara.TELEFONE, "(11) 91234-56789999"), "no máximo 11 números");
        assertEquals("(1", digitar(Mascara.TELEFONE, "1"));
        assertEquals("(11) 9", digitar(Mascara.TELEFONE, "119"));
    }

    @Test
    void cpfLimitadoA11NumerosEIgnoraLetras() {
        assertEquals("123.456.789-09", digitar(Mascara.CPF, "12345678909"));
        assertEquals("123.456.789-09", digitar(Mascara.CPF, "1234567890912345"), "não passa de 11 números");
        assertEquals("123.4", digitar(Mascara.CPF, "12a3b4"), "letras são ignoradas");
    }

    @Test
    void dataDdMmAaaa() {
        assertEquals("15/03/2010", digitar(Mascara.DATA, "15032010"));
        assertEquals("15/03", digitar(Mascara.DATA, "1503"));
        assertEquals("15/03/2010", digitar(Mascara.DATA, "150320109999"));
    }

    @Test
    void backspaceEmCimaDaPontuacaoApagaONumeroAnterior() throws Exception {
        JTextField f = new JTextField();
        Mascara.CPF.aplicar(f);
        f.setText("1234");                         // 123.4
        assertEquals("123.4", f.getText());
        f.getDocument().remove(4, 1);              // apaga o 4
        assertEquals("123", f.getText());
        f.setText("1234");
        f.getDocument().remove(3, 1);              // apaga o ponto -> some o 3 junto
        assertEquals("124", f.getText().replaceAll("\\D", ""));
    }
}
