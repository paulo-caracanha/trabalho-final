package com.exemplo.produtos.pedido.desconto;

import java.math.BigDecimal;

public interface DescontoStrategy {

    String getTipo();

    BigDecimal calcular(BigDecimal subtotal, int quantidade);
}
