package com.icecandylovers.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lote_ingrediente", indexes = {
        @Index(name = "idx_lote_ing_id_data", columnList = "ingrediente_id, data_compra")
})
public class LoteIngrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id")
    @JsonBackReference // Evita recursão infinita
    private Ingrediente ingrediente;

    @Column(precision = 10, scale = 3)
    private BigDecimal quantidade; // Quantidade inicial do lote

    @Column(precision = 10, scale = 3)
    private BigDecimal quantidadeAtual; // Quantidade restante

    @Column(precision = 10, scale = 2)
    private BigDecimal custoPorUnidade;

    private LocalDate dataCompra;

    @Column(name = "quantidade_comprada", nullable = false)
    private BigDecimal quantidadeComprada;

    @PrePersist
    protected void prePersist() {
        if (this.dataCompra == null) {
            this.dataCompra = LocalDate.now();
        }

        if (this.quantidadeComprada == null) {
            this.quantidadeComprada = this.quantidade;
        }
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getQuantidadeAtual() { return quantidadeAtual; }
    public void setQuantidadeAtual(BigDecimal quantidadeAtual) { this.quantidadeAtual = quantidadeAtual; }
    public BigDecimal getCustoPorUnidade() { return custoPorUnidade; }
    public void setCustoPorUnidade(BigDecimal custoPorUnidade) { this.custoPorUnidade = custoPorUnidade; }
    public LocalDate getDataCompra() { return dataCompra; }
    public void setDataCompra(LocalDate dataCompra) { this.dataCompra = dataCompra; }
    public BigDecimal getQuantidadeComprada() { return quantidadeComprada; }
    public void setQuantidadeComprada(BigDecimal quantidadeComprada) { this.quantidadeComprada = quantidadeComprada; }
}