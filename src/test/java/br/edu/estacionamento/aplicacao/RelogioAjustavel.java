package br.edu.estacionamento.aplicacao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio de teste que só avança quando mandado. */
public class RelogioAjustavel extends Clock {

    private Instant instante;

    public RelogioAjustavel(Instant inicio) {
        this.instante = inicio;
    }

    public void avancar(Duration duracao) {
        instante = instante.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
