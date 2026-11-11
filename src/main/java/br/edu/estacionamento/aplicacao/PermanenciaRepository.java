package br.edu.estacionamento.aplicacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import br.edu.estacionamento.dominio.Permanencia;
import br.edu.estacionamento.dominio.Veiculo;
import br.edu.estacionamento.vaga.Vaga;

/** Repositório em memória de permanências (sem banco de dados). */
@Repository
public class PermanenciaRepository {

    private final Map<Long, Permanencia> permanencias = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    public Permanencia criar(Vaga vaga, Veiculo veiculo, LocalDateTime entrada) {
        Permanencia permanencia = new Permanencia(sequencia.incrementAndGet(), vaga, veiculo, entrada);
        permanencias.put(permanencia.getId(), permanencia);
        return permanencia;
    }

    public Optional<Permanencia> buscarPorId(Long id) {
        return Optional.ofNullable(permanencias.get(id));
    }

    public Optional<Permanencia> buscarAtivaPorVaga(Long vagaId) {
        return permanencias.values().stream()
                .filter(p -> !p.isFinalizada() && p.getVaga().getId().equals(vagaId))
                .findFirst();
    }

    public Optional<Permanencia> buscarAtivaPorPlaca(String placa) {
        return permanencias.values().stream()
                .filter(p -> !p.isFinalizada() && p.getVeiculo().placa().equals(placa))
                .findFirst();
    }

    /** Lista todas as permanências (ativas e finalizadas) pela ordem de entrada. */
    public List<Permanencia> listar() {
        List<Permanencia> lista = new ArrayList<>(permanencias.values());
        lista.sort(Comparator.comparing(Permanencia::getId));
        return lista;
    }
}
