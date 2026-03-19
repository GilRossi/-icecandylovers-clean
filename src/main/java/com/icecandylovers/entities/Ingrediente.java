package com.icecandylovers.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(precision = 10, scale = 2)
    private BigDecimal custoPorUnidade;

    private String unidadeMedida;

    @Column(precision = 10, scale = 3)
    private BigDecimal estoqueInicial;

    @Column(precision = 10, scale = 3)
    private BigDecimal estoqueAtual;

    @OneToMany(mappedBy = "ingrediente", cascade = CascadeType.ALL)
    @JsonManagedReference // Evita recursão infinita
    private List<LoteIngrediente> lotes = new ArrayList<>();

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getCustoPorUnidade() { return custoPorUnidade; }
    public void setCustoPorUnidade(BigDecimal custoPorUnidade) { this.custoPorUnidade = custoPorUnidade; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    public BigDecimal getEstoqueInicial() { return estoqueInicial; }
    public void setEstoqueInicial(BigDecimal estoqueInicial) { this.estoqueInicial = estoqueInicial; }
    public BigDecimal getEstoqueAtual() { return estoqueAtual; }
    public void setEstoqueAtual(BigDecimal estoqueAtual) { this.estoqueAtual = estoqueAtual; }
    public List<LoteIngrediente> getLotes() { return lotes; }
    public void setLotes(List<LoteIngrediente> lotes) { this.lotes = lotes; }
}