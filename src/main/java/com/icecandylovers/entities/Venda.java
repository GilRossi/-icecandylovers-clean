package com.icecandylovers.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "venda", indexes = {
        @Index(name = "idx_venda_data", columnList = "data_venda")
})
public class Venda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_venda")
    private LocalDateTime dataVenda;
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private Vendido vendido;

    @Column(name = "valor_unitario_venda")
    private BigDecimal valorUnitarioVenda;

    @Column(name = "forma_pagamento")
    private String formaPagamento;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendaItem> itens;

    // Getters e Setters

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataVenda() {
        return dataVenda;
    }

    public void setDataVenda(LocalDateTime dataVenda) {
        this.dataVenda = dataVenda;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Vendido getVendido() {
        return vendido;
    }

    public void setVendido(Vendido vendido) {
        this.vendido = vendido;
    }

    public BigDecimal getValorUnitarioVenda() {
        return valorUnitarioVenda;
    }

    public void setValorUnitarioVenda(BigDecimal valorUnitarioVenda) {
        this.valorUnitarioVenda = valorUnitarioVenda;
    }

    public List<VendaItem> getItens() {
        return itens;
    }

    public void setItens(List<VendaItem> itens) {
        this.itens = itens;
    }
}