package br.edu.estacionamento.aplicacao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import br.edu.estacionamento.vaga.Vaga;

/** Repositório em memória de vagas (sem banco de dados). */
@Repository
public class VagaRepository {

    private final Map<Long, Vaga> vagas = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    public Vaga criar(int numero) {
        Vaga vaga = new Vaga(sequencia.incrementAndGet(), numero);
        vagas.put(vaga.getId(), vaga);
        return vaga;
    }

    public Optional<Vaga> buscarPorId(Long id) {
        return Optional.ofNullable(vagas.get(id));
    }

    public boolean existeNumero(int numero) {
        return vagas.values().stream().anyMatch(v -> v.getNumero() == numero);
    }

    /** Lista as vagas ordenadas pelo número. */
    public List<Vaga> listar() {
        List<Vaga> lista = new ArrayList<>(vagas.values());
        lista.sort(Comparator.comparingInt(Vaga::getNumero));
        return lista;
    }
}
