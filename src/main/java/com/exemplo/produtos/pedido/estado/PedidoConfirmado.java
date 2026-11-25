package com.exemplo.produtos.pedido.estado;

import com.exemplo.produtos.model.Pedido;

public class PedidoConfirmado implements EstadoPedido {

    @Override
    public void cancelar(Pedido pedido) {
        pedido.setStatus(StatusPedido.CANCELADO);
    }

    @Override
    public void entregar(Pedido pedido) {
        pedido.setStatus(StatusPedido.ENTREGUE);
    }
}
