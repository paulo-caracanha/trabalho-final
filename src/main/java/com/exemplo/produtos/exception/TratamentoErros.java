package com.exemplo.produtos.exception;

import com.exemplo.produtos.logger.AppLoggerComponent;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratamentoErros {

    private final AppLoggerComponent logger;

    public TratamentoErros(AppLoggerComponent logger) {
        this.logger = logger;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(IllegalArgumentException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(NaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> regraNegocio(RegraNegocioException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido() {
        return resposta(HttpStatus.BAD_REQUEST, "Confira os campos e o formato do JSON enviado");
    }

    private ResponseEntity<Map<String, String>> resposta(HttpStatus status, String mensagem) {
        logger.erro("HTTP " + status.value() + ": " + mensagem);
        return ResponseEntity.status(status).body(Map.of("mensagem", mensagem));
    }
}
