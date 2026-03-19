package com.icecandylovers.repositories;

import com.icecandylovers.entities.LoteIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoteIngredienteRepository extends JpaRepository<LoteIngrediente, Long> {
    Optional<LoteIngrediente> findTopByIngredienteIdOrderByDataCompraDesc(Long ingredienteId);

    @Modifying
    @Query("DELETE FROM LoteIngrediente l WHERE l.ingrediente.id = :ingredienteId")
    void deleteByIngredienteId(Long ingredienteId);

    @Query("SELECT l FROM LoteIngrediente l WHERE l.ingrediente.id = :ingredienteId ORDER BY l.dataCompra ASC")
    List<LoteIngrediente> findAllByIngredienteIdOrderByDataCompraAsc(Long ingredienteId);
}