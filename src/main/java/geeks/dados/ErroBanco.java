package geeks.dados;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTransientConnectionException;

/**
 * Erro de acesso a dados com mensagem pronta para mostrar ao atendente.
 * Traduz as violações de constraint conhecidas (os nomes uq_cliente_cpf,
 * uq_assinatura_ativa e uq_sessao_equipamento_em_uso NÃO podem ser renomeados no banco).
 */
public class ErroBanco extends RuntimeException {
    public ErroBanco(String mensagem) {
        super(mensagem);
    }

    public ErroBanco(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    /** Converte uma SQLException em mensagem clara (rede, permissão, regra do banco). */
    public static ErroBanco de(SQLException e) {
        return new ErroBanco(mensagemPara(e), e);
    }

    static String mensagemPara(SQLException e) {
        String texto = e.getMessage() == null ? "" : e.getMessage();
        if (texto.contains("uq_cliente_cpf")) {
            return "Já existe um cliente cadastrado com esse CPF.";
        }
        if (texto.contains("uq_assinatura_ativa")) {
            return "Esse cliente já tem uma assinatura ativa do Clube Geeks.";
        }
        if (texto.contains("uq_sessao_equipamento_em_uso")) {
            return "Esse dispositivo já está em uso em outra sessão.";
        }
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        if (estado.startsWith("08") || e instanceof SQLTransientConnectionException
                || texto.contains("Communications link failure")) {
            return "Não foi possível conectar ao banco de dados. Verifique se o MySQL está ligado e tente de novo.";
        }
        if (e.getErrorCode() == 1142 || e.getErrorCode() == 1044 || e.getErrorCode() == 1045) {
            return "O banco de dados recusou a operação por falta de permissão.";
        }
        if (e.getErrorCode() == 3819 || e instanceof SQLIntegrityConstraintViolationException) {
            return "O banco de dados recusou os dados informados (" + resumo(texto) + ").";
        }
        if (e.getErrorCode() == 1205 || e.getErrorCode() == 1213) {
            return "Outra operação estava usando os mesmos dados. Tente de novo.";
        }
        return "Erro no banco de dados: " + resumo(texto);
    }

    private static String resumo(String texto) {
        return texto.length() > 160 ? texto.substring(0, 160) + "..." : texto;
    }
}
