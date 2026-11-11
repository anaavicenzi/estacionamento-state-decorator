package br.edu.estacionamento.api;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.edu.estacionamento.aplicacao.EstacionamentoService;
import br.edu.estacionamento.api.dto.EntradaRequest;
import br.edu.estacionamento.api.dto.PermanenciaResponse;
import br.edu.estacionamento.api.dto.ServicoRequest;
import br.edu.estacionamento.dominio.Permanencia;

@RestController
@RequestMapping("/api/permanencias")
public class PermanenciaController {

    private final EstacionamentoService service;

    public PermanenciaController(EstacionamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PermanenciaResponse> listar() {
        OffsetDateTime agora = service.agora();
        return service.listarPermanencias().stream()
                .map(p -> PermanenciaResponse.de(p, agora))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermanenciaResponse registrarEntrada(@RequestBody EntradaRequest request) {
        return resposta(service.registrarEntrada(request.placa(), request.modelo(), request.vagaId()));
    }

    @PostMapping("/{id}/servicos")
    public PermanenciaResponse adicionarServico(@PathVariable Long id, @RequestBody ServicoRequest request) {
        return resposta(service.adicionarServico(id, request.tipo()));
    }

    @PostMapping("/{id}/saida")
    public PermanenciaResponse registrarSaida(@PathVariable Long id) {
        return resposta(service.registrarSaida(id));
    }

    private PermanenciaResponse resposta(Permanencia permanencia) {
        return PermanenciaResponse.de(permanencia, service.agora());
    }
}
