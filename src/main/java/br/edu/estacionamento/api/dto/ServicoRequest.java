package br.edu.estacionamento.api.dto;

/** Corpo de POST /api/permanencias/{id}/servicos: {"tipo": "LAVAGEM" | "MANOBRISTA"}. */
public record ServicoRequest(String tipo) {
}
