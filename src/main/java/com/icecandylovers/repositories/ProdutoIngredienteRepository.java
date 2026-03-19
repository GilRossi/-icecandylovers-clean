package com.icecandylovers.repositories;

import com.icecandylovers.entities.ProdutoIngrediente;
import com.icecandylovers.entities.ProdutoIngredienteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoIngredienteRepository extends JpaRepository<ProdutoIngrediente, ProdutoIngredienteId> {
}
