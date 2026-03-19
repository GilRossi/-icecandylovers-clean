package com.icecandylovers.services;

import com.icecandylovers.dtos.PromocaoDTO;
import com.icecandylovers.entities.Produto;
import com.icecandylovers.entities.Promocao;
import com.icecandylovers.entities.TipoDesconto;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.repositories.ProdutoRepository;
import com.icecandylovers.repositories.PromocaoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromocaoService {

    private static final Logger logger = LoggerFactory.getLogger(PromocaoService.class);

    private final PromocaoRepository promocaoRepository;
    private final ProdutoRepository produtoRepository;

    public PromocaoService(PromocaoRepository promocaoRepository, ProdutoRepository produtoRepository) {
        this.promocaoRepository = promocaoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public PromocaoDTO criarPromocao(PromocaoDTO dto) {
        logger.info("Criando promoção: {}", dto.nome());
        Promocao promocao = toEntity(dto);
        Promocao salva = promocaoRepository.save(promocao);
        logger.info("Promoção criada com sucesso, ID: {}", salva.getId());
        return toDTO(salva);
    }

    @Transactional
    public PromocaoDTO atualizarPromocao(Long id, PromocaoDTO dto) {
        logger.info("Atualizando promoção ID: {}", id);
        Promocao existente = buscarEntidade(id);
        existente.setNome(dto.nome());
        existente.setDescricao(dto.descricao());
        existente.setTipo(dto.tipo());
        existente.setValor(dto.valor());
        existente.setDataInicio(dto.dataInicio());
        existente.setDataFim(dto.dataFim());
        existente.setAtivo(dto.ativo());
        existente.setProdutos(resolverProdutos(dto.produtoIds()));
        Promocao atualizada = promocaoRepository.save(existente);
        logger.info("Promoção atualizada com sucesso, ID: {}", atualizada.getId());
        return toDTO(atualizada);
    }

    @Transactional
    public void deletarPromocao(Long id) {
        logger.info("Deletando promoção ID: {}", id);
        Promocao existente = buscarEntidade(id);
        promocaoRepository.delete(existente);
        logger.info("Promoção deletada com sucesso, ID: {}", id);
    }

    @Transactional
    public PromocaoDTO ativarDesativar(Long id, boolean ativo) {
        logger.info("Alterando status da promoção ID: {} para ativo={}", id, ativo);
        Promocao existente = buscarEntidade(id);
        existente.setAtivo(ativo);
        Promocao atualizada = promocaoRepository.save(existente);
        return toDTO(atualizada);
    }

    public List<PromocaoDTO> listarTodas() {
        logger.info("Listando todas as promoções");
        return promocaoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<PromocaoDTO> listarAtivas() {
        logger.info("Listando promoções ativas para hoje");
        return promocaoRepository.findAllActive(LocalDate.now()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<PromocaoDTO> buscarPromocoesParaProduto(Long produtoId) {
        logger.info("Buscando promoções ativas para produto ID: {}", produtoId);
        return promocaoRepository.findActiveByProdutoId(produtoId, LocalDate.now()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public BigDecimal calcularPrecoComDesconto(BigDecimal precoOriginal, Promocao promocao) {
        if (promocao.getTipo() == TipoDesconto.PERCENTUAL) {
            BigDecimal fator = BigDecimal.ONE.subtract(
                    promocao.getValor().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            return precoOriginal.multiply(fator).setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal resultado = precoOriginal.subtract(promocao.getValor());
            return resultado.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : resultado.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public BigDecimal calcularMelhorDesconto(Long produtoId, BigDecimal precoOriginal) {
        logger.info("Calculando melhor desconto para produto ID: {}", produtoId);
        List<Promocao> promocoes = promocaoRepository.findActiveByProdutoId(produtoId, LocalDate.now());
        if (promocoes.isEmpty()) {
            return precoOriginal;
        }
        return promocoes.stream()
                .map(p -> calcularPrecoComDesconto(precoOriginal, p))
                .min(BigDecimal::compareTo)
                .orElse(precoOriginal);
    }

    private Promocao buscarEntidade(Long id) {
        return promocaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoção não encontrada com ID: " + id));
    }

    private Promocao toEntity(PromocaoDTO dto) {
        Promocao p = new Promocao();
        p.setNome(dto.nome());
        p.setDescricao(dto.descricao());
        p.setTipo(dto.tipo());
        p.setValor(dto.valor());
        p.setDataInicio(dto.dataInicio());
        p.setDataFim(dto.dataFim());
        p.setAtivo(dto.ativo());
        p.setProdutos(resolverProdutos(dto.produtoIds()));
        return p;
    }

    private List<Produto> resolverProdutos(List<Long> produtoIds) {
        if (produtoIds == null || produtoIds.isEmpty()) {
            return Collections.emptyList();
        }
        return produtoIds.stream()
                .map(pid -> produtoRepository.findById(pid)
                        .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + pid)))
                .collect(Collectors.toList());
    }

    public PromocaoDTO toDTO(Promocao promocao) {
        List<Long> produtoIds = promocao.getProdutos().stream()
                .map(Produto::getId)
                .collect(Collectors.toList());
        return new PromocaoDTO(
                promocao.getId(),
                promocao.getNome(),
                promocao.getDescricao(),
                promocao.getTipo(),
                promocao.getValor(),
                promocao.getDataInicio(),
                promocao.getDataFim(),
                promocao.isAtivo(),
                produtoIds
        );
    }
}
