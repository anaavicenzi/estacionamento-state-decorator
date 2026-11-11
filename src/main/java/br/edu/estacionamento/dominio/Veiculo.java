package br.edu.estacionamento.dominio;

/**
 * Veículo estacionado. A placa é normalizada (sem espaços nas pontas e em
 * maiúsculas) para que "abc1d23" e "ABC1D23" sejam o mesmo veículo.
 */
public record Veiculo(String placa, String modelo) {

    public Veiculo {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("Placa é obrigatória");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new IllegalArgumentException("Modelo é obrigatório");
        }
        placa = placa.trim().toUpperCase();
        modelo = modelo.trim();
    }
}
