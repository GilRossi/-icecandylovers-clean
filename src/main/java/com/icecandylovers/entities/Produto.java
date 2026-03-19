package com.icecandylovers.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produto", indexes = {
        @Index(name = "idx_produto_sabor", columnList = "sabor")
})
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(unique = true)
    private String sabor;

    private Integer estoqueInicial;

    private Integer estoqueAtual;

    @Column(precision = 10, scale = 2)
    private BigDecimal precoCusto;

    @Column(precision = 10, scale = 2)
    private BigDecimal precoCustoUnitario;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProdutoIngrediente> ingredientes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private CategoriaProduto categoria;

    // Getters e Setters
    public CategoriaProduto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaProduto categoria) {
        this.categoria = categoria;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }
    public Integer getEstoqueInicial() { return estoqueInicial; }
    public void setEstoqueInicial(Integer estoqueInicial) { this.estoqueInicial = estoqueInicial; }
    public Integer getEstoqueAtual() { return estoqueAtual; }
    public void setEstoqueAtual(Integer estoqueAtual) { this.estoqueAtual = estoqueAtual; }
    public BigDecimal getPrecoCusto() { return precoCusto; }
    public void setPrecoCusto(BigDecimal precoCusto) { this.precoCusto = precoCusto; }
    public BigDecimal getPrecoCustoUnitario() { return precoCustoUnitario; }
    public void setPrecoCustoUnitario(BigDecimal precoCustoUnitario) { this.precoCustoUnitario = precoCustoUnitario; }
    public List<ProdutoIngrediente> getIngredientes() { return ingredientes; }
    public void setIngredientes(List<ProdutoIngrediente> ingredientes) { this.ingredientes = ingredientes; }
}
