package com.icecandylovers.services;

import com.icecandylovers.entities.Ingrediente;
import com.icecandylovers.entities.LoteIngrediente;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.repositories.IngredienteRepository;
import com.icecandylovers.repositories.LoteIngredienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IngredienteService {

    private static final Logger logger = LoggerFactory.getLogger(IngredienteService.class);

    private final IngredienteRepository ingredienteRepository;
    private final LoteIngredienteRepository loteIngredienteRepository;

    public IngredienteService(IngredienteRepository ingredienteRepository,
                              LoteIngredienteRepository loteIngredienteRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.loteIngredienteRepository = loteIngredienteRepository;
    }

    @Transactional
    public Ingrediente salvar(Ingrediente ingrediente) {
        logger.info("Salvando novo ingrediente: {}", ingrediente.getNome());
        Ingrediente salvo = ingredienteRepository.save(ingrediente);
        logger.debug("Ingrediente salvo com sucesso: {}", salvo.getNome());
        return salvo;
    }

    @Transactional
    public Ingrediente criarComLoteInicial(String nome, String unidadeMedida,
                                           BigDecimal quantidadeLote, BigDecimal custoPorUnidadeLote) {
        logger.info("Criando ingrediente '{}' com lote inicial", nome);

        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setNome(nome);
        ingrediente.setUnidadeMedida(unidadeMedida);
        ingrediente.setEstoqueInicial(quantidadeLote);
        ingrediente.setEstoqueAtual(BigDecimal.ZERO);
        ingrediente.setCustoPorUnidade(BigDecimal.ZERO);

        Ingrediente ingredienteSalvo = ingredienteRepository.save(ingrediente);
        adicionarLote(ingredienteSalvo.getId(), quantidadeLote, custoPorUnidadeLote);

        // Recarrega o ingrediente com estoque atualizado pelo lote
        Ingrediente atualizado = ingredienteRepository.findById(ingredienteSalvo.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ingrediente não encontrado: " + ingredienteSalvo.getId()));

        logger.debug("Ingrediente '{}' criado com sucesso, ID: {}", atualizado.getNome(), atualizado.getId());
        return atualizado;
    }

    @Transactional
    public void deletar(Long id) {
        logger.info("Deletando ingrediente com ID: {}", id);
        Ingrediente ingrediente = ingredienteRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Ingrediente não encontrado para deleção: {}", id);
                    return new ResourceNotFoundException("Ingrediente não encontrado: " + id);
                });

        loteIngredienteRepository.deleteByIngredienteId(id);
        ingredienteRepository.delete(ingrediente);
        logger.debug("Ingrediente deletado com sucesso: {}", id);
    }

    @Transactional
    public Ingrediente editar(Long id, String nome, String unidadeMedida, BigDecimal estoqueAtual) {
        logger.info("Editando ingrediente com ID: {}", id);
        Ingrediente ingredienteExistente = ingredienteRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Ingrediente não encontrado para edição: {}", id);
                    return new ResourceNotFoundException("Ingrediente não encontrado: " + id);
                });

        if (nome != null && !nome.trim().isEmpty()) {
            ingredienteExistente.setNome(nome);
        }
        if (unidadeMedida != null && !unidadeMedida.trim().isEmpty()) {
            ingredienteExistente.setUnidadeMedida(unidadeMedida);
        }
        if (estoqueAtual != null) {
            if (estoqueAtual.compareTo(BigDecimal.ZERO) < 0) {
                logger.warn("Tentativa de definir estoque negativo para ingrediente ID: {}", id);
                throw new IllegalArgumentException("O estoque atual não pode ser negativo");
            }
            ingredienteExistente.setEstoqueAtual(estoqueAtual);
        }

        Ingrediente atualizado = ingredienteRepository.save(ingredienteExistente);
        logger.debug("Ingrediente atualizado com sucesso: {}", atualizado.getNome());
        return atualizado;
    }

    @Transactional
    public Ingrediente editar(Long id, String nome, BigDecimal custoPorUnidade, String unidadeMedida, BigDecimal estoqueAtual) {
        logger.info("Editando ingrediente com custo por unidade, ID: {}", id);
        Ingrediente ingredienteExistente = ingredienteRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Ingrediente não encontrado para edição: {}", id);
                    return new ResourceNotFoundException("Ingrediente não encontrado: " + id);
                });

        if (nome != null && !nome.trim().isEmpty()) {
            ingredienteExistente.setNome(nome);
        }
        if (unidadeMedida != null && !unidadeMedida.trim().isEmpty()) {
            ingredienteExistente.setUnidadeMedida(unidadeMedida);
        }
        if (custoPorUnidade != null) {
            if (custoPorUnidade.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("O custo por unidade não pode ser negativo");
            }
            ingredienteExistente.setCustoPorUnidade(custoPorUnidade);
        }
        if (estoqueAtual != null) {
            if (estoqueAtual.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("O estoque atual não pode ser negativo");
            }
            ingredienteExistente.setEstoqueAtual(estoqueAtual);
        }

        Ingrediente atualizado = ingredienteRepository.save(ingredienteExistente);
        logger.debug("Ingrediente atualizado com sucesso: {}", atualizado.getNome());
        return atualizado;
    }

    @Transactional
    public LoteIngrediente adicionarLote(Long ingredienteId, BigDecimal quantidade, BigDecimal custoPorUnidade) {
        logger.info("Adicionando lote ao ingrediente ID: {} com quantidade: {} e custo: {}",
                ingredienteId, quantidade, custoPorUnidade);

        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Quantidade inválida para novo lote: {}", quantidade);
            throw new IllegalArgumentException("A quantidade do lote deve ser maior que zero");
        }
        if (custoPorUnidade == null || custoPorUnidade.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Custo por unidade inválido para novo lote: {}", custoPorUnidade);
            throw new IllegalArgumentException("O custo por unidade deve ser maior ou igual a zero");
        }

        Ingrediente ingrediente = ingredienteRepository.findById(ingredienteId)
                .orElseThrow(() -> {
                    logger.warn("Ingrediente não encontrado para adicionar lote: {}", ingredienteId);
                    return new ResourceNotFoundException("Ingrediente não encontrado: " + ingredienteId);
                });

        // Calcula o custo medio ponderado
        BigDecimal estoqueAtual = ingrediente.getEstoqueAtual() != null ? ingrediente.getEstoqueAtual() : BigDecimal.ZERO;
        BigDecimal custoAtual = ingrediente.getCustoPorUnidade() != null ? ingrediente.getCustoPorUnidade() : BigDecimal.ZERO;
        BigDecimal custoTotalAtual = estoqueAtual.multiply(custoAtual);
        BigDecimal custoTotalNovo = quantidade.multiply(custoPorUnidade);
        BigDecimal novoEstoque = estoqueAtual.add(quantidade);

        BigDecimal custoMedio = novoEstoque.compareTo(BigDecimal.ZERO) > 0
                ? custoTotalAtual.add(custoTotalNovo).divide(novoEstoque, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ingrediente.setEstoqueAtual(novoEstoque);
        ingrediente.setCustoPorUnidade(custoMedio);
        ingredienteRepository.save(ingrediente);

        LoteIngrediente lote = new LoteIngrediente();
        lote.setIngrediente(ingrediente);
        lote.setQuantidade(quantidade);
        lote.setQuantidadeAtual(quantidade);
        lote.setCustoPorUnidade(custoPorUnidade);

        LoteIngrediente salvo = loteIngredienteRepository.save(lote);
        logger.debug("Lote adicionado com sucesso ao ingrediente ID: {}", ingredienteId);
        return salvo;
    }

    @Transactional
    public void consumirEstoque(Long ingredienteId, BigDecimal quantidade) {
        logger.info("Consumindo {} unidades do ingrediente ID: {}", quantidade, ingredienteId);

        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Quantidade inválida para consumo: {}", quantidade);
            throw new IllegalArgumentException("A quantidade a consumir deve ser maior que zero");
        }

        Ingrediente ingrediente = ingredienteRepository.findById(ingredienteId)
                .orElseThrow(() -> {
                    logger.warn("Ingrediente não encontrado para consumo: {}", ingredienteId);
                    return new ResourceNotFoundException("Ingrediente não encontrado: " + ingredienteId);
                });

        BigDecimal estoqueAtual = ingrediente.getEstoqueAtual();
        if (estoqueAtual.compareTo(quantidade) < 0) {
            logger.warn("Estoque insuficiente para ingrediente '{}': atual={}, solicitado={}",
                    ingrediente.getNome(), estoqueAtual, quantidade);
            throw new IllegalStateException("Estoque insuficiente para o ingrediente: " + ingrediente.getNome());
        }

        // Consome dos lotes mais antigos primeiro (FIFO)
        List<LoteIngrediente> lotes = loteIngredienteRepository.findAllByIngredienteIdOrderByDataCompraAsc(ingredienteId);
        BigDecimal restante = quantidade;
        List<LoteIngrediente> lotesModificados = new ArrayList<>();

        for (LoteIngrediente lote : lotes) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal qtdLote = lote.getQuantidadeAtual();
            if (qtdLote.compareTo(restante) >= 0) {
                lote.setQuantidadeAtual(qtdLote.subtract(restante));
                restante = BigDecimal.ZERO;
            } else {
                lote.setQuantidadeAtual(BigDecimal.ZERO);
                restante = restante.subtract(qtdLote);
            }
            lotesModificados.add(lote);
        }
        loteIngredienteRepository.saveAll(lotesModificados);

        ingrediente.setEstoqueAtual(estoqueAtual.subtract(quantidade));
        ingredienteRepository.save(ingrediente);
        logger.debug("Estoque consumido com sucesso para ingrediente ID: {}", ingredienteId);
    }

    @Transactional
    public Ingrediente atualizarEstoque(Long id, BigDecimal quantidade) {
        logger.info("Atualizando estoque do ingrediente ID: {} em {}", id, quantidade);

        Ingrediente ingrediente = ingredienteRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Ingrediente não encontrado para atualização de estoque: {}", id);
                    return new ResourceNotFoundException("Ingrediente não encontrado: " + id);
                });

        BigDecimal novoEstoque = ingrediente.getEstoqueAtual().add(quantidade);
        if (novoEstoque.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Estoque insuficiente para ingrediente ID {}: atual={}, solicitado={}",
                    id, ingrediente.getEstoqueAtual(), quantidade);
            throw new IllegalArgumentException("Estoque insuficiente!");
        }

        ingrediente.setEstoqueAtual(novoEstoque);
        Ingrediente atualizado = ingredienteRepository.save(ingrediente);
        logger.debug("Estoque atualizado para ingrediente ID {}: novo estoque={}", id, novoEstoque);
        return atualizado;
    }

    public List<Ingrediente> listarTodos() {
        logger.info("Listando todos os ingredientes");
        List<Ingrediente> ingredientes = ingredienteRepository.findAll();
        logger.debug("Total de ingredientes listados: {}", ingredientes.size());
        return ingredientes;
    }

    public Optional<Ingrediente> buscarPorId(Long id) {
        logger.info("Buscando ingrediente por ID: {}", id);
        Optional<Ingrediente> ingrediente = ingredienteRepository.findById(id);
        if (ingrediente.isPresent()) {
            logger.debug("Ingrediente encontrado: {}", ingrediente.get().getNome());
        } else {
            logger.debug("Ingrediente não encontrado para ID: {}", id);
        }
        return ingrediente;
    }
}
