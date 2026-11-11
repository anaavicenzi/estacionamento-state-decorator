package br.edu.estacionamento.aplicacao;

/** Vaga ou permanência inexistente. A API responde com HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
