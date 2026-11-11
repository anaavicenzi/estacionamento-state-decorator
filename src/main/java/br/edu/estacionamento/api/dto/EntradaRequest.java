package br.edu.estacionamento.api.dto;

/** Corpo de POST /api/permanencias. */
public record EntradaRequest(String placa, String modelo, Long vagaId) {
}
