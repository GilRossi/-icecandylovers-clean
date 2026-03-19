package com.icecandylovers.services;

import com.icecandylovers.entities.Ingrediente;
import com.icecandylovers.entities.LoteIngrediente;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.repositories.IngredienteRepository;
import com.icecandylovers.repositories.LoteIngredienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class IngredienteService {

    private static final Logger logger = LoggerFactory.getLogger(IngredienteService.class);

    @Autowired
    private IngredienteRepository ingredienteRepository;

    @Autowired
    private LoteIngredienteRepository loteIngredienteRepository;

    @Transactional
    public Ingrediente salvar(Ingrediente ingrediente) {
        logger.info("Salvando novo ingrediente: {}", ingrediente.getNome());
        try {
            Ingrediente salvo = ingredienteRepository.save(ingrediente);
            logger.debug("Ingrediente salvo com sucesso: {}", salvo.getNome());
            return salvo;
        } catch (Exception e) {
            logger.error("Erro ao salvar ingrediente '{}': {}", ingrediente.getNome(), e.getMessage(), e);
            throw new RuntimeException("Erro interno ao salvar ingrediente", e);
        }
    }

    @Transactional
    public void deletar(Long id) {
        logger.info("Deletando ingrediente com ID: {}", id);
        try {
            Ingrediente ingrediente = ingredienteRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("Ingrediente não encontrado para deleção: {}", id);
                        return new ResourceNotFoundException("Ingrediente não encontrado: " + id);
                    });

            loteIngredienteRepository.deleteByIngredienteId(id);
            ingredienteRepository.delete(ingrediente);
            logger.debug("Ingrediente deletado com sucesso: {}", id);
        } catch (ResourceNotFoundException e) {
            throw e; // Já logado
        } catch (Exception e) {
            logger.error("Erro ao deletar ingrediente com ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao deletar ingrediente", e);
        }
    }

    @Transactional
    public Ingrediente editar(Long id, String nome, String unidadeMedida, BigDecimal estoqueAtual) {
        logger.info("Editando ingrediente com ID: {}", id);
        try {
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
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            throw e; // Já logado
        } catch (Exception e) {
            logger.error("Erro ao editar ingrediente com ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao editar ingrediente", e);
        }
    }

    @Transactional
    public Ingrediente editar(Long id, String nome, BigDecimal custoPorUnidade, String unidadeMedida, BigDecimal estoqueAtual) {
        logger.info("Editando ingrediente com custo por unidade, ID: {}", id);
        try {
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
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Erro ao editar ingrediente com ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao editar ingrediente", e);
        }
    }

    @Transactional
    public LoteIngrediente adicionarLote(Long ingredienteId, BigDecimal quantidade, BigDecimal custoPorUnidade) {
        logger.info("Adicionando lote ao ingrediente ID: {} com quantidade: {} e custo: {}",
                ingredienteId, quantidade, custoPorUnidade);
        try {
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

            // Calcula o custo médio ponderado
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
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            throw e; // Já logado
        } catch (Exception e) {
            logger.error("Erro ao adicionar lote ao ingrediente ID {}: {}", ingredienteId, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao adicionar lote", e);
        }
    }

    @Transactional
    public void consumirEstoque(Long ingredienteId, BigDecimal quantidade) {
        logger.info("Consumindo {} unidades do ingrediente ID: {}", quantidade, ingredienteId);
        try {
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
                loteIngredienteRepository.save(lote);
            }

            ingrediente.setEstoqueAtual(estoqueAtual.subtract(quantidade));
            ingredienteRepository.save(ingrediente);
            logger.debug("Estoque consumido com sucesso para ingrediente ID: {}", ingredienteId);
        } catch (ResourceNotFoundException | IllegalArgumentException | IllegalStateException e) {
            throw e; // Já logado
        } catch (Exception e) {
            logger.error("Erro ao consumir estoque do ingrediente ID {}: {}", ingredienteId, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao consumir estoque", e);
        }
    }

    public List<Ingrediente> listarTodos() {
        logger.info("Listando todos os ingredientes");
        try {
            List<Ingrediente> ingredientes = ingredienteRepository.findAll();
            logger.debug("Total de ingredientes listados: {}", ingredientes.size());
            return ingredientes;
        } catch (Exception e) {
            logger.error("Erro ao listar ingredientes: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao listar ingredientes", e);
        }
    }

    public Optional<Ingrediente> buscarPorId(Long id) {
        logger.info("Buscando ingrediente por ID: {}", id);
        try {
            Optional<Ingrediente> ingrediente = ingredienteRepository.findById(id);
            if (ingrediente.isPresent()) {
                logger.debug("Ingrediente encontrado: {}", ingrediente.get().getNome());
            } else {
                logger.debug("Ingrediente não encontrado para ID: {}", id);
            }
            return ingrediente;
        } catch (Exception e) {
            logger.error("Erro ao buscar ingrediente por ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao buscar ingrediente", e);
        }
    }
}
