package br.edu.estacionamento.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.edu.estacionamento.aplicacao.RecursoNaoEncontradoException;
import br.edu.estacionamento.api.dto.ErroResponse;
import br.edu.estacionamento.dominio.RegraNegocioException;
import br.edu.estacionamento.vaga.TransicaoInvalidaException;

/**
 * Converte exceções em respostas {"erro": "mensagem"}:
 * transição de estado inválida ou conflito de regra → 409,
 * recurso inexistente → 404, dados inválidos → 400.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({TransicaoInvalidaException.class, RegraNegocioException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflito(RuntimeException e) {
        return new ErroResponse(e.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse naoEncontrado(RecursoNaoEncontradoException e) {
        return new ErroResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse requisicaoInvalida(IllegalArgumentException e) {
        return new ErroResponse(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse corpoInvalido(HttpMessageNotReadableException e) {
        return new ErroResponse("Corpo da requisição ausente ou em formato inválido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse parametroInvalido(MethodArgumentTypeMismatchException e) {
        return new ErroResponse("Valor inválido para o parâmetro '" + e.getName() + "': " + e.getValue());
    }
}
