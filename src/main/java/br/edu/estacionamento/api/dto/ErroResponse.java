package br.edu.estacionamento.api.dto;

/** Corpo de todas as respostas de erro: {"erro": "mensagem"}. */
public record ErroResponse(String erro) {
}
