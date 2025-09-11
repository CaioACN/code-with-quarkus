package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.loyalty.dto.event.PointsAccruedEvent;
import org.acme.loyalty.entity.*;
import org.acme.loyalty.repository.*;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de Pontuação conforme regras de negócio 17.4, 17.5 e 17.6.
 * Responsável por:
 * - Aplicar regras de conversão
 * - Aplicar campanhas de bônus
 * - Gerar movimentos de pontos
 * - Atualizar saldos
 */
@ApplicationScoped
public class PontuacaoService {

    @Inject
    RegraConversaoRepository regraConversaoRepository;
    
    @Inject
    CampanhaBonusRepository campanhaBonusRepository;
    
    @Inject
    MovimentoPontosRepository movimentoPontosRepository;
    
    @Inject
    SaldoPontosRepository saldoPontosRepository;
    
    @Inject
    TransacaoRepository transacaoRepository;
    
    @Inject
    EventPublisherService eventPublisherService;

    /**
     * Processa transação para gerar pontos conforme regras 17.4 e 17.5.
     * Este método deve ser chamado quando uma transação é criada.
     */
    @Transactional
    public void processarTransacaoParaPontos(Long transacaoId) {
        Transacao transacao = transacaoRepository.findByIdOptional(transacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada: " + transacaoId));
        
        // Verificar se pode gerar pontos conforme regra 17.3
        if (!transacao.podeGerarPontos()) {
            return; // NEGADA não gera pontos
        }
        
        // Verificar se já foi processada (idempotência)
        if (movimentoPontosRepository.existeMovimentoParaTransacao(transacaoId, MovimentoPontos.TipoMovimento.ACUMULO)) {
            return; // Já processada
        }
        
        // Aplicar regra de conversão conforme regra 17.4
        Long pontosBase = calcularPontosBase(transacao);
        if (pontosBase <= 0) {
            return; // Nenhum ponto a ser gerado
        }
        
        // Aplicar campanha de bônus conforme regra 17.5
        Long pontosTotais = aplicarCampanhaBonus(transacao, pontosBase);
        
        // Gerar movimento de pontos conforme regra 17.6
        MovimentoPontos movimento = criarMovimentoAcumulo(transacao, pontosTotais);
        movimentoPontosRepository.persist(movimento);
        
        // Atualizar saldo conforme regra 17.7
        saldoPontosRepository.creditarSaldoAtomicamente(
            transacao.usuario.id, 
            transacao.cartao.id, 
            pontosTotais
        );
        
        // Marcar transação como processada
        transacao.marcarComoProcessada(pontosTotais.intValue());
        transacaoRepository.persist(transacao);
        
        // Publicar evento
        PointsAccruedEvent event = new PointsAccruedEvent(
            transacao.usuario.id,
            transacao.cartao.id,
            pontosTotais.intValue(),
            transacaoId,
            LocalDateTime.now()
        );
        eventPublisherService.publishEvent(event);
    }
    
    /**
     * Calcula pontos base conforme regra 17.4:
     * - Seleção por vigência e prioridade (maior primeiro)
     * - Escopo por mcc_regex / categoria / parceiro_id
     * - pontos_base = floor(valor * multiplicador)
     */
    private Long calcularPontosBase(Transacao transacao) {
        LocalDate dataTransacao = transacao.dataEvento.toLocalDate();
        
        // Buscar regra mais prioritária
        Optional<RegraConversao> regraOpt = regraConversaoRepository.selecionarRegraMaisPrioritaria(
            transacao.mcc,
            transacao.categoria,
            transacao.parceiroId,
            dataTransacao
        );
        
        if (regraOpt.isEmpty()) {
            return 0L; // Nenhuma regra aplicável
        }
        
        RegraConversao regra = regraOpt.get();
        
        // Verificar se aplica para esta transação
        if (!regra.aplicaParaMcc(transacao.mcc) ||
            !regra.aplicaParaCategoria(transacao.categoria) ||
            !regra.aplicaParaParceiro(transacao.parceiroId)) {
            return 0L;
        }
        
        // Calcular pontos base
        Long pontosBase = regra.calcularPontos(transacao.valor);
        
        // Aplicar teto mensal se definido
        if (regra.temTetoMensal()) {
            Long pontosMes = calcularPontosMes(transacao.usuario.id, transacao.cartao.id, dataTransacao);
            if (pontosMes + pontosBase > regra.tetoMensal) {
                pontosBase = Math.max(0, regra.tetoMensal - pontosMes);
            }
        }
        
        return pontosBase;
    }
    
    /**
     * Aplica campanha de bônus conforme regra 17.5:
     * pontos_totais = floor(pontos_base * (1 + multiplicador_extra))
     */
    private Long aplicarCampanhaBonus(Transacao transacao, Long pontosBase) {
        LocalDate dataTransacao = transacao.dataEvento.toLocalDate();
        
        // Buscar campanhas vigentes
        List<CampanhaBonus> campanhas = campanhaBonusRepository.listarVigentes(dataTransacao);
        
        for (CampanhaBonus campanha : campanhas) {
            if (campanha.aplicaParaSegmento(null)) { // Implementar segmento do usuário quando necessário
                return campanha.calcularPontosComBonus(pontosBase);
            }
        }
        
        return pontosBase; // Nenhuma campanha aplicável
    }
    
    /**
     * Cria movimento de acúmulo conforme regra 17.6
     */
    private MovimentoPontos criarMovimentoAcumulo(Transacao transacao, Long pontos) {
        MovimentoPontos movimento = new MovimentoPontos(
            transacao.usuario,
            transacao.cartao,
            MovimentoPontos.TipoMovimento.ACUMULO,
            pontos.intValue(),
            transacao,
            "Acúmulo automático por transação"
        );
        
        return movimento;
    }
    
    /**
     * Processa estorno de transação conforme regra 17.3:
     * ESTORNADA deve produzir movimento_pontos(ESTORNO)
     */
    @Transactional
    public void processarEstornoTransacao(Long transacaoId) {
        Transacao transacao = transacaoRepository.findByIdOptional(transacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada: " + transacaoId));
        
        if (!transacao.foiEstornada()) {
            throw new IllegalArgumentException("Transação não foi estornada: " + transacaoId);
        }
        
        // Verificar se já foi processado o estorno
        if (movimentoPontosRepository.existeMovimentoParaTransacao(transacaoId, MovimentoPontos.TipoMovimento.ESTORNO)) {
            return; // Já processado
        }
        
        // Buscar movimento de acúmulo original
        List<MovimentoPontos> movimentosOriginais = movimentoPontosRepository
            .listarVinculadosATransacao(transacaoId);
        
        for (MovimentoPontos movimentoOriginal : movimentosOriginais) {
            if (movimentoOriginal.isAcumulo()) {
                // Criar movimento de estorno (valor negativo)
                MovimentoPontos movimentoEstorno = new MovimentoPontos(
                    transacao.usuario,
                    transacao.cartao,
                    MovimentoPontos.TipoMovimento.ESTORNO,
                    -movimentoOriginal.pontos,
                    transacao,
                    "Estorno de transação"
                );
                
                movimentoPontosRepository.persist(movimentoEstorno);
                
                // Atualizar saldo
                saldoPontosRepository.debitarSaldoAtomicamente(
                    transacao.usuario.id,
                    transacao.cartao.id,
                    (long) Math.abs(movimentoOriginal.pontos)
                );
            }
        }
    }
    
    /**
     * Calcula pontos acumulados no mês para aplicação de teto
     */
    private Long calcularPontosMes(Long usuarioId, Long cartaoId, LocalDate dataTransacao) {
        LocalDateTime inicioMes = dataTransacao.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = dataTransacao.withDayOfMonth(dataTransacao.lengthOfMonth()).atTime(23, 59, 59);
        
        return movimentoPontosRepository.sumPontosByUsuarioAndTipoInPeriodo(
            usuarioId,
            MovimentoPontos.TipoMovimento.ACUMULO,
            inicioMes,
            fimMes
        );
    }
}
