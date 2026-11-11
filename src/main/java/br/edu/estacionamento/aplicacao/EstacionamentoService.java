package br.edu.estacionamento.aplicacao;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.estacionamento.dominio.Permanencia;
import br.edu.estacionamento.dominio.RegraNegocioException;
import br.edu.estacionamento.dominio.TipoServico;
import br.edu.estacionamento.dominio.Veiculo;
import br.edu.estacionamento.vaga.Vaga;

/**
 * Casos de uso do estacionamento. As regras de transição da vaga ficam nos
 * estados (pattern State) e a composição da cobrança nos decorators (pattern
 * Decorator); aqui só se orquestram as operações.
 *
 * <p>Os métodos que alteram dados são {@code synchronized} porque o
 * armazenamento é em memória e compartilhado entre as requisições.
 */
@Service
public class EstacionamentoService {

    private final VagaRepository vagas;
    private final PermanenciaRepository permanencias;
    private final Clock relogio;

    public EstacionamentoService(VagaRepository vagas, PermanenciaRepository permanencias, Clock relogio) {
        this.vagas = vagas;
        this.permanencias = permanencias;
        this.relogio = relogio;
    }

    // ---- Vagas ----

    public List<Vaga> listarVagas() {
        return vagas.listar();
    }

    public synchronized Vaga criarVaga(Integer numero) {
        if (numero == null || numero <= 0) {
            throw new IllegalArgumentException("Número da vaga deve ser um inteiro positivo");
        }
        if (vagas.existeNumero(numero)) {
            throw new RegraNegocioException("Já existe uma vaga com o número " + numero);
        }
        return vagas.criar(numero);
    }

    public synchronized Vaga reservarVaga(Long id) {
        Vaga vaga = buscarVaga(id);
        vaga.reservar();
        return vaga;
    }

    /**
     * Libera manualmente uma vaga (uso previsto: cancelar uma reserva). Uma vaga
     * ocupada só é liberada pela saída da permanência, para não deixar uma
     * permanência ativa apontando para uma vaga livre.
     */
    public synchronized Vaga liberarVaga(Long id) {
        Vaga vaga = buscarVaga(id);
        permanencias.buscarAtivaPorVaga(id).ifPresent(p -> {
            throw new RegraNegocioException("Vaga " + vaga.getNumero()
                    + " está ocupada pela permanência " + p.getId() + "; registre a saída para liberá-la");
        });
        vaga.liberar();
        return vaga;
    }

    // ---- Permanências ----

    public List<Permanencia> listarPermanencias() {
        return permanencias.listar();
    }

    public synchronized Permanencia registrarEntrada(String placa, String modelo, Long vagaId) {
        Veiculo veiculo = new Veiculo(placa, modelo);
        Vaga vaga = buscarVaga(vagaId);
        permanencias.buscarAtivaPorPlaca(veiculo.placa()).ifPresent(p -> {
            throw new RegraNegocioException("Veículo " + veiculo.placa()
                    + " já está estacionado na vaga " + p.getVaga().getNumero());
        });
        vaga.ocupar();
        return permanencias.criar(vaga, veiculo, agora());
    }

    public synchronized Permanencia adicionarServico(Long permanenciaId, String tipo) {
        TipoServico tipoServico = TipoServico.de(tipo);
        Permanencia permanencia = buscarPermanencia(permanenciaId);
        permanencia.adicionarServico(tipoServico);
        return permanencia;
    }

    public synchronized Permanencia registrarSaida(Long permanenciaId) {
        Permanencia permanencia = buscarPermanencia(permanenciaId);
        permanencia.finalizar(agora());
        return permanencia;
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private Vaga buscarVaga(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id da vaga é obrigatório");
        }
        return vagas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga " + id + " não encontrada"));
    }

    private Permanencia buscarPermanencia(Long id) {
        return permanencias.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Permanência " + id + " não encontrada"));
    }
}
