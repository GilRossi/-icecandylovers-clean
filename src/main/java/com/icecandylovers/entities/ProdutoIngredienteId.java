package com.icecandylovers.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProdutoIngredienteId implements Serializable {

    @Column(name = "produto_id")
    private Long produtoId;

    @Column(name = "lote_ingrediente_id")
    private Long loteIngredienteId;

    public ProdutoIngredienteId() {}

    public ProdutoIngredienteId(Long produtoId, Long loteIngredienteId) {
        this.produtoId = produtoId;
        this.loteIngredienteId = loteIngredienteId;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public Long getLoteIngredienteId() {
        return loteIngredienteId;
    }

    public void setLoteIngredienteId(Long loteIngredienteId) {
        this.loteIngredienteId = loteIngredienteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProdutoIngredienteId that = (ProdutoIngredienteId) o;
        return Objects.equals(produtoId, that.produtoId) && Objects.equals(loteIngredienteId, that.loteIngredienteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produtoId, loteIngredienteId);
    }
}