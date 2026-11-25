package com.exemplo.produtos.pedido.estado;

import com.exemplo.produtos.exception.RegraNegocioException;
import com.exemplo.produtos.model.Pedido;

public interface EstadoPedido {

    default void confirmar(Pedido pedido) {
        throw new RegraNegocioException("Nao e possivel confirmar um pedido " + pedido.getStatus());
    }

    default void cancelar(Pedido pedido) {
        throw new RegraNegocioException("Nao e possivel cancelar um pedido " + pedido.getStatus());
    }

    default void entregar(Pedido pedido) {
        throw new RegraNegocioException("Nao e possivel entregar um pedido " + pedido.getStatus());
    }
}
