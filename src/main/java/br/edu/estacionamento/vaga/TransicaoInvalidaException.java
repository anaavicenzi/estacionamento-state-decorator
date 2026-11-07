package br.edu.estacionamento.vaga;

public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(String operacao, String estado) {
        super("Não é possível " + operacao + " uma vaga no estado " + estado);
    }
}
