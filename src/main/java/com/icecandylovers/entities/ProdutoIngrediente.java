package com.icecandylovers.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "produto_ingrediente")
public class ProdutoIngrediente {

    @EmbeddedId
    private ProdutoIngredienteId id = new ProdutoIngredienteId();

    @MapsId("produtoId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @MapsId("loteIngredienteId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_ingrediente_id", nullable = false)
    private LoteIngrediente loteIngrediente;

    @Column(name = "quantidade")
    private BigDecimal quantidade;

    public ProdutoIngrediente() {}

    public ProdutoIngredienteId getId() {
        return id;
    }

    public void setId(ProdutoIngredienteId id) {
        this.id = id;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public LoteIngrediente getLoteIngrediente() {
        return loteIngrediente;
    }

    public void setLoteIngrediente(LoteIngrediente loteIngrediente) {
        this.loteIngrediente = loteIngrediente;
    }

    public Ingrediente getIngrediente() {
        return loteIngrediente != null ? loteIngrediente.getIngrediente() : null;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProdutoIngrediente that = (ProdutoIngrediente) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
