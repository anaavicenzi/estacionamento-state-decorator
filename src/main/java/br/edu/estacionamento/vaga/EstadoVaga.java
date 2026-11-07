package br.edu.estacionamento.vaga;

public interface EstadoVaga {

    EstadoVaga ocupar();

    EstadoVaga liberar();

    EstadoVaga reservar();

    String nome();
}
