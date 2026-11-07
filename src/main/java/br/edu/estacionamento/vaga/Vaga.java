package br.edu.estacionamento.vaga;

public class Vaga {

    private final Long id;
    private final int numero;
    private EstadoVaga estado;

    public Vaga(Long id, int numero) {
        this.id = id;
        this.numero = numero;
        this.estado = new VagaLivre();
    }

    public void ocupar() {
        estado = estado.ocupar();
    }

    public void liberar() {
        estado = estado.liberar();
    }

    public void reservar() {
        estado = estado.reservar();
    }

    public Long getId() {
        return id;
    }

    public int getNumero() {
        return numero;
    }

    public EstadoVaga getEstado() {
        return estado;
    }

    public String getNomeEstado() {
        return estado.nome();
    }
}
