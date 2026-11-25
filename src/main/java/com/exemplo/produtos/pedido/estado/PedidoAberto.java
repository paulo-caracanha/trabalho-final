package com.exemplo.produtos.pedido.estado;

import com.exemplo.produtos.model.Pedido;

public class PedidoAberto implements EstadoPedido {

    @Override
    public void confirmar(Pedido pedido) {
        pedido.setStatus(StatusPedido.CONFIRMADO);
    }

    @Override
    public void cancelar(Pedido pedido) {
        pedido.setStatus(StatusPedido.CANCELADO);
    }
}
