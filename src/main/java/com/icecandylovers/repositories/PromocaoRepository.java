package com.icecandylovers.repositories;

import com.icecandylovers.entities.Promocao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromocaoRepository extends JpaRepository<Promocao, Long> {

    @Query("""
            SELECT p FROM Promocao p LEFT JOIN p.produtos prod
            WHERE p.ativo = true AND p.dataInicio <= :hoje AND p.dataFim >= :hoje
            AND (prod.id = :produtoId OR SIZE(p.produtos) = 0)
            """)
    List<Promocao> findActiveByProdutoId(@Param("produtoId") Long produtoId, @Param("hoje") LocalDate hoje);

    @Query("SELECT p FROM Promocao p WHERE p.ativo = true AND p.dataInicio <= :hoje AND p.dataFim >= :hoje")
    List<Promocao> findAllActive(@Param("hoje") LocalDate hoje);
    @Query("SELECT p FROM Promocao p JOIN p.produtos prod WHERE prod.id = :produtoId")
    List<Promocao> findAllByProdutoId(@Param("produtoId") Long produtoId);
}
