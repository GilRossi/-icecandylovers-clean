package com.icecandylovers.repositories;

import com.icecandylovers.entities.CategoriaProduto;
import com.icecandylovers.entities.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @Query("""
            SELECT DISTINCT p
            FROM Produto p
            LEFT JOIN FETCH p.ingredientes pi
            LEFT JOIN FETCH pi.loteIngrediente li
            LEFT JOIN FETCH li.ingrediente
            WHERE p.id = :id
            """)
    Optional<Produto> findByIdWithIngredientes(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p
            FROM Produto p
            LEFT JOIN FETCH p.ingredientes pi
            LEFT JOIN FETCH pi.loteIngrediente li
            LEFT JOIN FETCH li.ingrediente
            """)
    List<Produto> findAllWithIngredientes();

    @Query("SELECT COALESCE(SUM(p.estoqueAtual), 0) FROM Produto p")
    int sumEstoqueAtual();

    Optional<Produto> findBySabor(String sabor);

    @Query("SELECT COUNT(pi) > 0 FROM ProdutoIngrediente pi WHERE pi.loteIngrediente.ingrediente.id = :ingredienteId")
    boolean existsByIngredienteId(@Param("ingredienteId") Long ingredienteId);

    @Query("SELECT p.sabor, p.estoqueAtual FROM Produto p")
    List<Object[]> findQuantidadeEstoquePorProduto();

    @Query("""
            SELECT DISTINCT p
            FROM Produto p
            LEFT JOIN FETCH p.ingredientes pi
            LEFT JOIN FETCH pi.loteIngrediente li
            LEFT JOIN FETCH li.ingrediente
            WHERE p.categoria = :categoria
            """)
    List<Produto> findByCategoria(@Param("categoria") CategoriaProduto categoria);


}
