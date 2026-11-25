package com.exemplo.produtos.controller;

import com.exemplo.produtos.dto.PedidoRequest;
import com.exemplo.produtos.model.Pedido;
import com.exemplo.produtos.service.PedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Pedido> criar(@RequestBody PedidoRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dados));
    }

    @GetMapping
    public List<Pedido> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Pedido buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/confirmar")
    public Pedido confirmar(@PathVariable Long id) {
        return service.confirmar(id);
    }

    @PostMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }

    @PostMapping("/{id}/entregar")
    public Pedido entregar(@PathVariable Long id) {
        return service.entregar(id);
    }
}
