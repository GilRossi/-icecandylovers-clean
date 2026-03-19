package com.icecandylovers.repositories;

import com.icecandylovers.entities.VendaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VendaItemRepository extends JpaRepository<VendaItem, Long> {

    @Query("SELECT v.vendido, SUM(v.total) FROM Venda v WHERE v.dataVenda BETWEEN :start AND :end GROUP BY v.vendido")
    List<Object[]> findVendasAgrupadasPorCanal(LocalDateTime start, LocalDateTime end);

    @Query("SELECT vi.produto.sabor, AVG(vi.valorUnitario) " +
            "FROM VendaItem vi " +
            "JOIN vi.venda v " +
            "WHERE v.dataVenda BETWEEN :start AND :end " +
            "GROUP BY vi.produto.sabor")
    List<Object[]> findPrecoCustoUnitarioPorProduto(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i.nome, i.custoPorUnidade, i.estoqueAtual FROM Ingrediente i WHERE i.id IN " +
            "(SELECT pi.loteIngrediente.ingrediente.id FROM ProdutoIngrediente pi WHERE pi.produto.id IN " +
            "(SELECT vi.produto.id FROM VendaItem vi WHERE vi.venda.dataVenda BETWEEN :start AND :end))")
    List<Object[]> findIngredientesComPrecoEEstoque(LocalDateTime start, LocalDateTime end);

    boolean existsByProdutoId(Long produtoId);
}