package com.exemplo.produtos.repository;

import com.exemplo.produtos.model.Pedido;
import com.exemplo.produtos.pedido.estado.StatusPedido;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarComBloqueio(@Param("id") Long id);

    boolean existsByProdutoIdAndStatus(Long produtoId, StatusPedido status);
}
