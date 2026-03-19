package com.icecandylovers.repositories;

import com.icecandylovers.entities.Venda;
import com.icecandylovers.entities.Vendido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venda v " +
            "WHERE CAST(v.dataVenda AS date) = CURRENT_DATE")
    BigDecimal findTotalVendasHoje();

    List<Venda> findByDataVendaAfter(LocalDateTime dataInicio);

    List<Venda> findByDataVendaBetween(LocalDateTime start, LocalDateTime end);

    List<Venda> findTop10ByOrderByDataVendaDesc();

    List<Venda> findTop5ByOrderByDataVendaDesc();

    @Query("SELECT SUM(v.total) FROM Venda v WHERE v.dataVenda BETWEEN :start AND :end")
    BigDecimal sumTotalVendasByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venda v " +
            "WHERE v.dataVenda BETWEEN :start AND :end " +
            "AND v.vendido = :canal")
    BigDecimal sumTotalVendasByPeriodAndChannel(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("canal") Vendido canal);

    @Query("SELECT COUNT(DISTINCT v.id) FROM Venda v " +
            "WHERE v.dataVenda BETWEEN :start AND :end " +
            "AND v.vendido = :canal")
    Long countVendasByPeriodAndChannel(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("canal") Vendido canal);

    @Query("""
    SELECT 
        FUNCTION('TO_CHAR', v.dataVenda, 'YYYY-MM-DD'), 
        SUM(v.total) 
    FROM Venda v 
    WHERE v.dataVenda BETWEEN :start AND :end 
    AND (:canal = 'ALL' OR v.vendido = :canal) 
    GROUP BY FUNCTION('TO_CHAR', v.dataVenda, 'YYYY-MM-DD')
    """)
    List<Object[]> findVendasAgrupadasPorPeriodo(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("canal") Vendido canal
    );

    @Query("SELECT vi.produto.sabor, SUM(vi.quantidade) as total " +
            "FROM VendaItem vi " +
            "WHERE vi.venda.dataVenda BETWEEN :start AND :end " +
            "AND (:canal = 'ALL' OR vi.venda.vendido = :canal) " +
            "GROUP BY vi.produto.sabor " +
            "ORDER BY total DESC LIMIT :limit")
    List<Object[]> findTopProdutos(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("canal") Vendido canal,
            @Param("limit") int limit
    );

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.estoqueAtual > 10")
    int countProdutosComEstoque();

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.estoqueAtual BETWEEN 1 AND 10")
    int countProdutosComEstoqueBaixo();

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.estoqueAtual = 0")
    int countProdutosSemEstoque();

    @Query("SELECT COALESCE(SUM(p.precoCusto * vi.quantidade), 0) " +
            "FROM Venda v JOIN v.itens vi JOIN vi.produto p " +
            "WHERE v.dataVenda BETWEEN :start AND :end " +
            "AND v.vendido = :canal")
    BigDecimal sumCustosByPeriodAndChannel(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("canal") Vendido canal);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.dataVenda BETWEEN :start AND :end")
    Long countVendasByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.precoCusto * vi.quantidade), 0) " +
            "FROM Venda v JOIN v.itens vi JOIN vi.produto p " +
            "WHERE v.dataVenda BETWEEN :start AND :end")
    BigDecimal sumCustosByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT v.formaPagamento, SUM(v.total) " +
            "FROM Venda v " +
            "WHERE v.dataVenda BETWEEN :start AND :end " +
            "GROUP BY v.formaPagamento")
    List<Object[]> findVendasAgrupadasPorFormaPagamento(LocalDateTime start, LocalDateTime end);

    @Query("SELECT v.formaPagamento, SUM(v.total) " +
            "FROM Venda v " +
            "WHERE v.dataVenda BETWEEN :start AND :end " +
            "AND v.vendido = :canal " +
            "GROUP BY v.formaPagamento")
    List<Object[]> findVendasAgrupadasPorFormaPagamentoAndChannel(LocalDateTime start, LocalDateTime end, Vendido canal);

    @Query("SELECT v FROM Venda v LEFT JOIN FETCH v.itens WHERE v.id = :id")
    Optional<Venda> findByIdWithItens(@Param("id") Long id);

    @Query("SELECT v FROM Venda v LEFT JOIN FETCH v.itens")
    List<Venda> findAllWithItens();
}