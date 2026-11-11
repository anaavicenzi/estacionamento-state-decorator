package br.edu.estacionamento.dominio;

import java.util.Arrays;
import java.util.function.UnaryOperator;

import br.edu.estacionamento.servico.Lavagem;
import br.edu.estacionamento.servico.Manobrista;
import br.edu.estacionamento.servico.Servico;

/**
 * Serviços extras que podem ser adicionados a uma permanência. Cada tipo sabe
 * qual decorator do pacote {@code servico} o representa, então um novo
 * serviço (ex.: RecargaEletrica) exige só a classe nova e uma constante aqui.
 */
public enum TipoServico {

    LAVAGEM(Lavagem::new),
    MANOBRISTA(Manobrista::new);

    private final UnaryOperator<Servico> decorador;

    TipoServico(UnaryOperator<Servico> decorador) {
        this.decorador = decorador;
    }

    /** Envolve o serviço recebido com o decorator deste tipo. */
    public Servico decorar(Servico servico) {
        return decorador.apply(servico);
    }

    /** Converte o texto recebido pela API, sem diferenciar maiúsculas. */
    public static TipoServico de(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Tipo de serviço é obrigatório");
        }
        try {
            return valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de serviço inválido: " + tipo
                    + ". Valores aceitos: " + Arrays.toString(values()));
        }
    }
}
