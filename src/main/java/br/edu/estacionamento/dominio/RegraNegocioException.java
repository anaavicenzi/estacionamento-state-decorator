package br.edu.estacionamento.dominio;

/**
 * Operação que conflita com o estado atual do sistema (ex.: serviço duplicado,
 * saída de uma permanência já finalizada). A API responde com HTTP 409.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
