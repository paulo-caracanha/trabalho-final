package com.exemplo.produtos.pedido.desconto;

import com.exemplo.produtos.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class DescontoAtacado implements DescontoStrategy {

    @Override
    public String getTipo() {
        return "ATACADO";
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, int quantidade) {
        if (quantidade < 10) {
            throw new RegraNegocioException("O desconto de atacado exige pelo menos 10 unidades");
        }
        return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
    }
}
