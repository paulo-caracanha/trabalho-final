package com.exemplo.produtos.pedido.desconto;

import com.exemplo.produtos.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class DescontoCompraMaior implements DescontoStrategy {

    @Override
    public String getTipo() {
        return "COMPRA_MAIOR";
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, int quantidade) {
        if (subtotal.compareTo(new BigDecimal("500.00")) < 0) {
            throw new RegraNegocioException("O desconto por valor exige uma compra de pelo menos R$ 500,00");
        }
        return subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
    }
}
