package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.loyalty.dto.event.PointsExpiredEvent;
import org.acme.loyalty.entity.MovimentoPontos;

import org.acme.loyalty.repository.MovimentoPontosRepository;
import org.acme.loyalty.repository.SaldoPontosRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de Expiração de Pontos conforme regra 17.11.
 * Responsável por:
 * - Executar job diário de expiração
 * - Localizar pontos elegíveis para expiração
 * - Registrar movimentos de expiração
 * - Atualizar saldos
 * - Publicar eventos de expiração
 */
@ApplicationScoped
public class ExpiracaoPontosService {

    @Inject
    MovimentoPontosRepository movimentoPontosRepository;
    
    @Inject
    SaldoPontosRepository saldoPontosRepository;
    
    @Inject
    EventPublisherService eventPublisherService;

    /**
     * Executa processo de expiração conforme regra 17.11.
     * Deve ser executado diariamente via scheduler.
     */
    @Transactional
    public void executarExpiracaoDiaria() {
        String jobId = UUID.randomUUID().toString();
        LocalDate hoje = LocalDate.now();
        
        // Política de expiração: pontos expiram após 12 meses do acúmulo
        LocalDate dataLimite = hoje.minusMonths(12);
        
        // Buscar movimentos de acúmulo elegíveis para expiração
        List<MovimentoPontos> movimentosElegiveis = buscarMovimentosElegiveisParaExpiracao(dataLimite);
        
        for (MovimentoPontos movimento : movimentosElegiveis) {
            processarExpiracaoMovimento(movimento, jobId);
        }
        
        // Log de auditoria com quantidade de pontos expirados quando necessário
    }
    
    /**
     * Busca movimentos de acúmulo elegíveis para expiração
     */
    private List<MovimentoPontos> buscarMovimentosElegiveisParaExpiracao(LocalDate dataLimite) {
        LocalDateTime inicioLimite = dataLimite.atStartOfDay();
        LocalDateTime fimLimite = dataLimite.atTime(23, 59, 59);
        
        return movimentoPontosRepository.find(
            "tipo = ?1 and pontos > 0 and criadoEm between ?2 and ?3",
            MovimentoPontos.TipoMovimento.ACUMULO,
            inicioLimite,
            fimLimite
        ).list();
    }
    
    /**
     * Processa expiração de um movimento específico
     */
    private void processarExpiracaoMovimento(MovimentoPontos movimentoOriginal, String jobId) {
        // Verificar se já foi expirado
        if (movimentoPontosRepository.existeMovimentoParaTransacao(
            movimentoOriginal.refTransacaoId, MovimentoPontos.TipoMovimento.EXPIRACAO)) {
            return; // Já foi expirado
        }
        
        // Verificar saldo disponível
        Long saldoAtual = saldoPontosRepository.obterSaldoAtual(
            movimentoOriginal.usuario.id,
            movimentoOriginal.cartao.id
        );
        
        if (saldoAtual <= 0) {
            return; // Sem saldo para expirar
        }
        
        // Calcular pontos a expirar (mínimo entre pontos do movimento e saldo atual)
        Long pontosAExpirar = Math.min(movimentoOriginal.pontos, saldoAtual);
        
        if (pontosAExpirar <= 0) {
            return;
        }
        
        // Criar movimento de expiração
        MovimentoPontos movimentoExpiracao = new MovimentoPontos(
            movimentoOriginal.usuario,
            movimentoOriginal.cartao,
            MovimentoPontos.TipoMovimento.EXPIRACAO,
            -pontosAExpirar.intValue(), // Valor negativo para débito
            "Expiração automática de pontos"
        );
        movimentoExpiracao.jobId = jobId;
        movimentoExpiracao.refTransacaoId = movimentoOriginal.refTransacaoId;
        
        movimentoPontosRepository.persist(movimentoExpiracao);
        
        // Atualizar saldo
        saldoPontosRepository.debitarSaldoAtomicamente(
            movimentoOriginal.usuario.id,
            movimentoOriginal.cartao.id,
            pontosAExpirar
        );
        
        // Publicar evento
        PointsExpiredEvent event = new PointsExpiredEvent(
            movimentoOriginal.usuario.id,
            movimentoOriginal.cartao.id,
            -pontosAExpirar.intValue(),
            jobId,
            LocalDateTime.now()
        );
        eventPublisherService.publishEvent(event);
    }
    
    /**
     * Executa expiração para um usuário específico (para testes ou correções)
     */
    @Transactional
    public void executarExpiracaoUsuario(Long usuarioId, LocalDate dataLimite) {
        String jobId = UUID.randomUUID().toString();
        
        List<MovimentoPontos> movimentos = movimentoPontosRepository.find(
            "usuario.id = ?1 and tipo = ?2 and pontos > 0 and criadoEm <= ?3",
            usuarioId,
            MovimentoPontos.TipoMovimento.ACUMULO,
            dataLimite.atTime(23, 59, 59)
        ).list();
        
        for (MovimentoPontos movimento : movimentos) {
            processarExpiracaoMovimento(movimento, jobId);
        }
    }
    
    /**
     * Executa expiração para um cartão específico (para testes ou correções)
     */
    @Transactional
    public void executarExpiracaoCartao(Long usuarioId, Long cartaoId, LocalDate dataLimite) {
        String jobId = UUID.randomUUID().toString();
        
        List<MovimentoPontos> movimentos = movimentoPontosRepository.find(
            "usuario.id = ?1 and cartao.id = ?2 and tipo = ?3 and pontos > 0 and criadoEm <= ?4",
            usuarioId,
            cartaoId,
            MovimentoPontos.TipoMovimento.ACUMULO,
            dataLimite.atTime(23, 59, 59)
        ).list();
        
        for (MovimentoPontos movimento : movimentos) {
            processarExpiracaoMovimento(movimento, jobId);
        }
    }
}