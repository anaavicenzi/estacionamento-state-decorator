package br.edu.estacionamento.api;

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
import br.edu.estacionamento.api.dto.CriarVagaRequest;
import br.edu.estacionamento.api.dto.VagaResponse;

@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    private final EstacionamentoService service;

    public VagaController(EstacionamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<VagaResponse> listar() {
        return service.listarVagas().stream().map(VagaResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VagaResponse criar(@RequestBody CriarVagaRequest request) {
        return VagaResponse.de(service.criarVaga(request.numero()));
    }

    @PostMapping("/{id}/reservar")
    public VagaResponse reservar(@PathVariable Long id) {
        return VagaResponse.de(service.reservarVaga(id));
    }

    @PostMapping("/{id}/liberar")
    public VagaResponse liberar(@PathVariable Long id) {
        return VagaResponse.de(service.liberarVaga(id));
    }
}
