package com.exemplo.produtos.pedido.estado;

public enum StatusPedido {

    ABERTO(new PedidoAberto()),
    CONFIRMADO(new PedidoConfirmado()),
    CANCELADO(new PedidoCancelado()),
    ENTREGUE(new PedidoEntregue());

    private final EstadoPedido estado;

    StatusPedido(EstadoPedido estado) {
        this.estado = estado;
    }

    public EstadoPedido getEstado() {
        return estado;
    }
}
