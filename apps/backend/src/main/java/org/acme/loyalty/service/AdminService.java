package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.acme.loyalty.dto.*;
import org.acme.loyalty.entity.MovimentoPontos;
import org.acme.loyalty.entity.SaldoPontos;
import org.acme.loyalty.entity.Usuario;
import org.acme.loyalty.entity.Transacao.StatusTransacao;
import org.acme.loyalty.repository.*;

import org.jboss.logging.Logger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.*;

@ApplicationScoped
public class AdminService {

    @Inject UsuarioRepository usuarioRepository;
    @Inject CartaoRepository cartaoRepository;
    @Inject SaldoPontosRepository saldoPontosRepository;
    @Inject MovimentoPontosRepository movimentoPontosRepository;
    @Inject TransacaoRepository transacaoRepository;
    @Inject ResgateRepository resgateRepository;
    @Inject EntityManager em;

    // ============ DASHBOARD ============

    public DashboardDTO consultarDashboard() {
        DashboardDTO dto = new DashboardDTO();

        dto.totalUsuarios = Math.toIntExact(usuarioRepository.count());
        dto.totalCartoes = Math.toIntExact(cartaoRepository.count());

        Long saldoTotal = saldoPontosRepository.sumSaldoTotal();
        dto.saldoTotal = (saldoTotal != null ? saldoTotal : 0L);

        LocalDateTime iniHoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje = LocalDate.now().atTime(23, 59, 59);
        
        // Transações do dia
        dto.totalTransacoes = Math.toIntExact(transacaoRepository.countByPeriodo(iniHoje, fimHoje));

        // Pontos movimentados (do dia)
        Long pontosAcumulados = movimentoPontosRepository
                .sumPontosByTipoBetween(MovimentoPontos.TipoMovimento.ACUMULO, iniHoje, fimHoje);
        dto.pontosAcumulados = (pontosAcumulados != null ? pontosAcumulados : 0L);
        
        Long pontosResgatados = movimentoPontosRepository
                .sumPontosByTipoBetween(MovimentoPontos.TipoMovimento.RESGATE, iniHoje, fimHoje);
        dto.pontosResgatados = (pontosResgatados != null ? pontosResgatados : 0L);
        
        Long pontosExpirados = movimentoPontosRepository
                .sumPontosByTipoBetween(MovimentoPontos.TipoMovimento.EXPIRACAO, iniHoje, fimHoje);
        dto.pontosExpirados = (pontosExpirados != null ? pontosExpirados : 0L);

        // Resgates por status (todos os tempos)
        dto.resgatesPendentes = Math.toIntExact(resgateRepository.countByStatus("PENDENTE"));
        dto.resgatesAprovados = Math.toIntExact(resgateRepository.countByStatus("APROVADO"));
        dto.resgatesConcluidos = Math.toIntExact(resgateRepository.countByStatus("CONCLUIDO"));
        dto.resgatesNegados = Math.toIntExact(resgateRepository.countByStatus("NEGADO"));
        dto.resgatesCancelados = Math.toIntExact(resgateRepository.countByStatus("CANCELADO"));

        // Período do dashboard
        dto.periodoIni = iniHoje;
        dto.periodoFim = fimHoje;

        return dto;
    }

    // ============ ESTATÍSTICAS ============

    public EstatisticasDTO consultarEstatisticas(String periodo, LocalDate dataInicio, LocalDate dataFim) {
        EstatisticasDTO dto = new EstatisticasDTO();
    
        LocalDateTime ini = (dataInicio != null) ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = (dataFim != null) ? dataFim.atTime(23, 59, 59) : null;
    
        if (ini == null || fim == null) {
            LocalDate hoje = LocalDate.now();
            String periodoUpper = periodo != null ? periodo.toUpperCase() : "";
            if ("SEMANA".equals(periodoUpper)) {
                ini = hoje.minusDays(6).atStartOfDay(); fim = hoje.atTime(23,59,59);
            } else if ("MES".equals(periodoUpper)) {
                ini = hoje.withDayOfMonth(1).atStartOfDay(); fim = hoje.atTime(23,59,59);
            } else {
                ini = hoje.atStartOfDay(); fim = hoje.atTime(23,59,59);
            }
        }
    
        dto.periodoIni = ini;
        dto.periodoFim = fim;
    
        Long totalTx = transacaoRepository.countByPeriodo(ini, fim);
        dto.totalTransacoes = (totalTx != null) ? Math.toIntExact(totalTx) : 0;
    
        Long acumulados = movimentoPontosRepository
                .sumPontosByTipoBetween(MovimentoPontos.TipoMovimento.ACUMULO, ini, fim);
        Long resgatados = movimentoPontosRepository
                .sumPontosByTipoBetween(MovimentoPontos.TipoMovimento.RESGATE, ini, fim);
        Long expirados = movimentoPontosRepository
                .sumPontosByTipoBetween(MovimentoPontos.TipoMovimento.EXPIRACAO, ini, fim);
    
        dto.pontosAcumulados = (acumulados != null) ? acumulados : 0L;
        // reporta resgatados como valor absoluto positivo
        dto.pontosResgatados = Math.abs((resgatados != null) ? resgatados : 0L);
        dto.pontosExpirados  = (expirados != null) ? Math.abs(expirados) : 0L;
    
        // opcionalmente, preencha outros campos do DTO se desejar (saldoTotal, etc.)
        return dto;
    }
    

    // ============ AJUSTE / ESTORNO ============

    @Transactional
    public void ajustarPontos(AjustePontosDTO ajuste) {
        validarAjustePontos(ajuste);

        if (ajuste.jobId != null && movimentoPontosRepository.existsByJobId(ajuste.jobId)) {
            return;
        }

        Usuario usuario = usuarioRepository.findByIdOptional(ajuste.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + ajuste.usuarioId));

        SaldoPontos saldo = saldoPontosRepository.findByUsuarioAndCartao(ajuste.usuarioId, ajuste.cartaoId)
                .orElseThrow(() -> new NotFoundException("Saldo não encontrado para o cartão: " + ajuste.cartaoId));

        saldo.saldo = (saldo.saldo != null ? saldo.saldo : 0L) + ajuste.pontos;
        saldo.atualizadoEm = LocalDateTime.now();
        saldoPontosRepository.persist(saldo);

        MovimentoPontos mov = new MovimentoPontos();
        mov.usuario = usuario;
        mov.cartao = saldo.cartao;
        mov.tipo = MovimentoPontos.TipoMovimento.AJUSTE;
        mov.pontos = ajuste.pontos;
        mov.observacao = ajuste.observacao;
        mov.criadoEm = LocalDateTime.now();
        mov.jobId = ajuste.jobId;
        movimentoPontosRepository.persist(mov);
    }

    @Transactional
    public void estornarPontos(Long movimentoId, String motivo) {
        MovimentoPontos original = movimentoPontosRepository.findByIdOptional(movimentoId)
                .orElseThrow(() -> new NotFoundException("Movimento não encontrado: " + movimentoId));

        if (!MovimentoPontos.TipoMovimento.ACUMULO.equals(original.tipo)) {
            throw new IllegalArgumentException("Apenas movimentos de acúmulo podem ser estornados");
        }

        SaldoPontos saldo = saldoPontosRepository.findByUsuarioAndCartao(original.usuario.id, original.cartao.id)
                .orElseThrow(() -> new NotFoundException("Saldo não encontrado"));

        saldo.saldo = (saldo.saldo != null ? saldo.saldo : 0L) - original.pontos;
        saldo.atualizadoEm = LocalDateTime.now();
        saldoPontosRepository.persist(saldo);

        MovimentoPontos estorno = new MovimentoPontos();
        estorno.usuario = original.usuario;
        estorno.cartao = original.cartao;
        estorno.tipo = MovimentoPontos.TipoMovimento.ESTORNO;
        estorno.pontos = -original.pontos;
        estorno.refTransacaoId = original.refTransacaoId;
        estorno.observacao = "Estorno: " + motivo;
        estorno.criadoEm = LocalDateTime.now();
        movimentoPontosRepository.persist(estorno);
    }

    // ============ AUDITORIA ============

    /**
     * Gera um snapshot consolidado do usuário para o período e retorna como lista com 1 item.
     * (Evita depender de métodos inexistentes em AuditoriaUsuarioDTO.)
     */
    public List<AuditoriaUsuarioDTO> consultarAuditoriaUsuario(Long usuarioId, LocalDate de, LocalDate ate) {
        if (usuarioId == null) throw new IllegalArgumentException("usuarioId é obrigatório");

        LocalDateTime ini = (de != null) ? de.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime fim = (ate != null) ? ate.atTime(23,59,59) : LocalDateTime.now();

        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        var saldos = saldoPontosRepository.listByUsuarioId(usuarioId);
        var txs    = transacaoRepository.listByUsuarioAndPeriodo(usuarioId, ini, fim);
        var movs   = movimentoPontosRepository.listByUsuarioBetween(usuarioId, ini, fim);
        var resg   = resgateRepository.listByUsuarioAndPeriodo(usuarioId, ini, fim);

        AuditoriaUsuarioDTO snapshot = AuditoriaUsuarioDTO.fromEntity(usuario, saldos, txs, movs, resg);
        return List.of(snapshot);
    }

    // ============ MANUTENÇÃO ============
// dentro da classe AdminService
private static final Logger LOG = Logger.getLogger(AdminService.class);

@Transactional
public void executarManutencao(String tipo, Map<String, Object> parametros) {
    if ("LIMPEZA_LOGS".equals(tipo)) {
        int anos = ((Number) parametros.getOrDefault("anos", 5)).intValue();
        LocalDateTime limite = LocalDate.now().minusYears(anos).atStartOfDay();
        long deletados = movimentoPontosRepository.deleteOlderThan(limite);
        LOG.infof("LIMPEZA_LOGS: %d movimentos removidos (anteriores a %s).", deletados, limite);
    } else if ("VALIDACAO_INTEGRIDADE".equals(tipo)) {
        var inconsistencias = saldoPontosRepository.validarSaldos();
        if (!inconsistencias.isEmpty()) {
            throw new IllegalStateException("Inconsistências de saldo: " + inconsistencias);
        }
        LOG.info("VALIDACAO_INTEGRIDADE: sem inconsistências.");
    } else if ("REINDEXACAO".equals(tipo)) {
        em.createNativeQuery("REINDEX SCHEMA loyalty").executeUpdate();
        LOG.info("REINDEXACAO: schema 'loyalty' reindexado com sucesso.");
    } else {
        throw new IllegalArgumentException("Tipo de manutenção não suportado: " + tipo);
    }
}

    // ============ SAÚDE ============

    public SaudeSistemaDTO consultarSaudeSistema() {
        SaudeSistemaDTO s = new SaudeSistemaDTO();
        s.timestamp = LocalDateTime.now();
        s.statusGeral = SaudeSistemaDTO.Status.UP;

        try {
            Query q = em.createNativeQuery("SELECT 1");
            q.getSingleResult();
            s.addComponente("database", SaudeSistemaDTO.Status.UP, 0L, "OK", "jdbc:postgresql://postgres:5432/quarkus-social");

            long usuarios = usuarioRepository.count();
            s.addComponente("usuarios", SaudeSistemaDTO.Status.UP, usuarios, "count", null);

            long pendResg = resgateRepository.countByStatus("PENDENTE");
            s.addComponente("resgates_pend", SaudeSistemaDTO.Status.UP, pendResg, "pendentes", null);

        } catch (Exception e) {
            s.addComponente("database", SaudeSistemaDTO.Status.DOWN, 0L, "ERRO: " + e.getMessage(), null);
            s.statusGeral = SaudeSistemaDTO.Status.DOWN;
        }

        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        s.addComponente("jvm_mem", SaudeSistemaDTO.Status.UP, total - free, "usedBytes", null);

        s.recompute();
        return s;
    }

    // ============ MÉTRICAS ============

    public Map<String, Object> consultarMetricas() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime h24 = agora.minusHours(24);

        Long tx24 = transacaoRepository.countByPeriodo(h24, agora);
        Long ac24 = movimentoPontosRepository.sumPontosByTipoBetween(MovimentoPontos.TipoMovimento.ACUMULO, h24, agora);
        Long rg24 = movimentoPontosRepository.sumPontosByTipoBetween(MovimentoPontos.TipoMovimento.RESGATE, h24, agora);
        Long saldoTotal = saldoPontosRepository.sumSaldoTotal();

        return Map.of(
            "timestamp", agora,
            "transacoes_24h", tx24 != null ? tx24 : 0L,
            "pontos_acumulados_24h", ac24 != null ? ac24 : 0L,
            "pontos_resgatados_24h", Math.abs(rg24 != null ? rg24 : 0L),
            "saldo_total", saldoTotal != null ? saldoTotal : 0L
        );
    }

    // -------- validação --------
    private void validarAjustePontos(AjustePontosDTO a) {
        if (a.usuarioId == null) throw new IllegalArgumentException("ID do usuário é obrigatório");
        if (a.cartaoId == null) throw new IllegalArgumentException("ID do cartão é obrigatório");
        if (a.pontos == null) throw new IllegalArgumentException("Quantidade de pontos é obrigatória");
        if (a.observacao == null || a.observacao.trim().isEmpty()) throw new IllegalArgumentException("Observação do ajuste é obrigatória");
        if (a.jobId == null || a.jobId.trim().isEmpty()) throw new IllegalArgumentException("ID do job é obrigatório");
    }

    // ============================================================
    // TRANSACOES (registro + publicação de evento)
    // ============================================================

    /**
     * Registra uma transação (status=PENDENTE) e "publica" o evento TransactionCreated (via log).
     * Idempotência básica pode ser feita fora via unique key natural (ex.: hash dos campos).
     */
    @Transactional
    public Long registrarTransacao(Long usuarioId,
                                   Long cartaoId,
                                   java.math.BigDecimal valor,
                                   String moeda,
                                   String mcc,
                                   String categoria,
                                   Long parceiroId,
                                   LocalDateTime dataEvento) {

        if (usuarioId == null || cartaoId == null || valor == null) {
            throw new IllegalArgumentException("usuarioId, cartaoId e valor são obrigatórios");
        }

        var usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // usamos o EntityManager para referenciar entidades sem carregar tudo
        var cartao = em.getReference(org.acme.loyalty.entity.Cartao.class, cartaoId);

        var t = new org.acme.loyalty.entity.Transacao();
        t.usuario = usuario;
        t.cartao = cartao;
        t.valor = valor;
        t.moeda = (moeda != null ? moeda : "BRL");
        t.mcc = mcc;
        t.categoria = categoria;
        t.parceiroId = parceiroId;
        t.dataEvento = (dataEvento != null ? dataEvento : LocalDateTime.now());
        t.status = StatusTransacao.APROVADA;

        transacaoRepository.persist(t);

        // "Publica" o evento (stub): quando tiver Kafka/Outbox, trocar por publisher real
        publicarTransactionCreated(t);

        return t.id;
    }

    private void publicarTransactionCreated(org.acme.loyalty.entity.Transacao t) {
        LOG.infof("EVENT TransactionCreated{id=%d, usuarioId=%d, cartaoId=%d, valor=%s, mcc=%s, categoria=%s, data=%s}",
                t.id, t.usuario.id, t.cartao.id, String.valueOf(t.valor), t.mcc, t.categoria, String.valueOf(t.dataEvento));
        // Enviar para Kafka/Rabbit ou gravar Outbox quando necessário
    }

    // ============================================================
    // PONTUAÇÃO (motor simples) - ACÚMULO a partir da transação
    // ============================================================
/**
 * Processa a transação gerando ACUMULO de pontos e atualizando saldo.
 * Idempotência: se já existir movimento com refTransacaoId = transacaoId, não reaplica.
 * Multiplicador default = 1.0 (pode evoluir para ler regra/campanha).
 * @return pontos acumulados
 */
@Transactional
public long processarTransacaoPontuacao(Long transacaoId, Double multiplicadorDefault) {
    var t = transacaoRepository.findByIdOptional(transacaoId)
            .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + transacaoId));

    // já processada
    if (t.pontosGerados != null && t.pontosGerados > 0) {
        return 0L;
    }

    // idempotência por existência de movimento atrelado
    boolean jaTemMov = !movimentoPontosRepository.listByRefTransacaoId(transacaoId).isEmpty();
    if (jaTemMov) return 0L;

    // calcula pontos (garantindo não-negativo)
    double mult = (multiplicadorDefault != null ? multiplicadorDefault : 1.0d);
    long pontos = Math.max(0L, calcularPontosAcumulo(t, mult));

    // garante o saldo (cria se não existir)
    var saldoOpt = saldoPontosRepository.findByUsuarioAndCartao(t.usuario.id, t.cartao.id);
    org.acme.loyalty.entity.SaldoPontos saldo;
    if (saldoOpt.isPresent()) {
        saldo = saldoOpt.get();
        saldo.saldo = (saldo.saldo == null ? 0L : saldo.saldo) + pontos;
    } else {
        saldo = new org.acme.loyalty.entity.SaldoPontos();
        saldo.usuario = t.usuario;
        saldo.cartao  = t.cartao;
        saldo.saldo   = pontos;
    }
    saldo.atualizadoEm = LocalDateTime.now();
    saldoPontosRepository.persist(saldo); // se já estiver gerenciada, é no-op

    // lança movimento ACUMULO (pontos é long -> converter para Integer com clamp)
    var mov = new org.acme.loyalty.entity.MovimentoPontos();
    mov.usuario = t.usuario;
    mov.cartao  = t.cartao;
    mov.tipo    = org.acme.loyalty.entity.MovimentoPontos.TipoMovimento.ACUMULO;

    int pontosInt = (pontos > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) pontos;
    mov.pontos        = pontosInt;
    mov.refTransacaoId= t.id;
    mov.criadoEm      = LocalDateTime.now();
    movimentoPontosRepository.persist(mov);

    // marca transação como processada
    t.marcarComoProcessada((int) pontos);
    transacaoRepository.persist(t);

    // notificação (stub)
    publicarPointsAccrued(mov);

    return pontos;
}


    private long calcularPontosAcumulo(org.acme.loyalty.entity.Transacao t, Double multiplicadorDefault) {
        double mult = (multiplicadorDefault == null ? 1.0 : Math.max(0.0, multiplicadorDefault));
        // regra simplificada: arredonda para baixo
        var valor = (t.valor == null ? java.math.BigDecimal.ZERO : t.valor);
        return java.math.BigDecimal.valueOf(mult)
                .multiply(valor)
                .setScale(0, java.math.RoundingMode.FLOOR)
                .longValue();
    }

    private void publicarPointsAccrued(org.acme.loyalty.entity.MovimentoPontos m) {
        LOG.infof("EVENT PointsAccrued{usuarioId=%d, cartaoId=%d, pontos=%d, refTransacaoId=%s, criadoEm=%s}",
                m.usuario.id, m.cartao.id, m.pontos, String.valueOf(m.refTransacaoId), String.valueOf(m.criadoEm));
        // Enviar para Kafka/Rabbit ou gravar Outbox quando necessário
    }

    // ============================================================
    // EXTRATO & SALDO (consultas MVP)
    // ============================================================

    /** Lista saldos do usuário (com cartão) para o MVP. */
    public List<org.acme.loyalty.entity.SaldoPontos> obterSaldosUsuario(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return saldoPontosRepository.listByUsuarioWithCartao(usuarioId);
    }

    /** Extrato paginado (opcionalmente filtrado por cartao e tipo). */
    public List<org.acme.loyalty.entity.MovimentoPontos> obterExtrato(Long usuarioId,
                                                                      Long cartaoId,
                                                                      LocalDate de,
                                                                      LocalDate ate,
                                                                      org.acme.loyalty.entity.MovimentoPontos.TipoMovimento tipo,
                                                                      int page, int size) {
        LocalDateTime ini = (de != null) ? de.atStartOfDay() : null;
        LocalDateTime fim = (ate != null) ? ate.atTime(23, 59, 59) : null;
        return movimentoPontosRepository
                .queryExtrato(usuarioId, cartaoId, ini, fim, tipo, page, size)
                .list();
    }

    // ============================================================
    // RESGATES (workflow PENDENTE → APROVADO → CONCLUIDO|NEGADO)
    // ============================================================

    /** Cria um pedido de resgate (status=PENDENTE). */
    @Transactional
    public Long solicitarResgate(Long usuarioId, Long cartaoId, Long recompensaId, Long pontos) {
        if (usuarioId == null || cartaoId == null || recompensaId == null || pontos == null || pontos <= 0) {
            throw new IllegalArgumentException("usuarioId, cartaoId, recompensaId e pontos (>0) são obrigatórios");
        }

        var usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));
        var cartao = em.getReference(org.acme.loyalty.entity.Cartao.class, cartaoId);
        var recompensa = em.getReference(org.acme.loyalty.entity.Recompensa.class, recompensaId);

        var r = new org.acme.loyalty.entity.Resgate();
        r.usuario = usuario;
        r.cartao = cartao;
        r.recompensa = recompensa;
        r.pontosUtilizados = pontos;
        r.status = org.acme.loyalty.entity.Resgate.StatusResgate.PENDENTE;
        r.criadoEm = LocalDateTime.now();

        resgateRepository.persist(r);

        publicarRedeemRequested(r);
        return r.id;
    }
/** Aprova resgate (pontos já foram debitados na criação). */
@Transactional
public boolean aprovarResgate(Long resgateId) {
    var opt = resgateRepository.findByIdOptional(resgateId);
    if (opt.isEmpty()) return false;

    var r = opt.get();
    if (r.status != org.acme.loyalty.entity.Resgate.StatusResgate.PENDENTE) return false;

    // Pontos já foram debitados na criação do resgate, apenas atualiza status
    r.status     = org.acme.loyalty.entity.Resgate.StatusResgate.APROVADO;
    r.aprovadoEm = LocalDateTime.now();
    resgateRepository.persist(r);

    // Publicar evento RedeemApproved/Completed se houver mensageria quando necessário
    return true;
}


    /** Conclui um resgate aprovado. */
    @Transactional
    public boolean concluirResgate(Long resgateId) {
        boolean ok = resgateRepository.concluirResgate(resgateId);
        if (ok) {
            var r = resgateRepository.findById(resgateId);
            publicarRedeemCompleted(r);
        }
        return ok;
    }

    /** Nega um resgate pendente (sem débito de pontos). */
    @Transactional
    public boolean negarResgate(Long resgateId, String motivo) {
        return resgateRepository.negarResgate(resgateId, motivo != null ? motivo : "Sem motivo informado");
    }

    private void publicarRedeemRequested(org.acme.loyalty.entity.Resgate r) {
        LOG.infof("EVENT RedeemRequested{resgateId=%d, usuarioId=%d, cartaoId=%d, pontos=%d}",
                r.id, r.usuario.id, r.cartao.id, r.pontosUtilizados);
        // Enviar para Kafka/Rabbit ou gravar Outbox quando necessário
    }

    private void publicarRedeemCompleted(org.acme.loyalty.entity.Resgate r) {
        if (r != null) {
            LOG.infof("EVENT RedeemCompleted{resgateId=%d, usuarioId=%d, cartaoId=%d, pontos=%d, status=%s}",
                    r.id, r.usuario.id, r.cartao.id, r.pontosUtilizados, r.status);
        }
        // Enviar para Kafka/Rabbit ou gravar Outbox quando necessário
    }

    // ============================================================
    // EXPIRAÇÃO (job diário simples)
    // ============================================================

    /**
     * Expira pontos de todas as carteiras com saldo > 0.
     * Estratégia simples: expira até 'limitePorCarteira' pontos (ou todo o saldo se limite <= 0).
     * Use um agendador (Quartz ou @Scheduled) para chamar este método diariamente.
     */
    @Transactional
    public int executarExpiracaoDiaria(LocalDate dataBase, String jobId, long limitePorCarteira) {
        LocalDateTime ts = (dataBase != null ? dataBase.atTime(23, 59, 59) : LocalDateTime.now());
        int carteirasAfetadas = 0;

        var positivos = saldoPontosRepository.listBySaldoMinimo(1L);
        for (var sp : positivos) {
            long exp = (limitePorCarteira > 0 ? Math.min(limitePorCarteira, sp.saldo) : sp.saldo);
            if (exp <= 0) continue;

            expirarPontosCarteira(sp.usuario.id, sp.cartao.id, exp, jobId, ts);
            carteirasAfetadas++;
        }
        return carteirasAfetadas;
    }

/** Expira uma quantidade na carteira informada (gera movimento EXPIRACAO e baixa saldo). */
@Transactional
public void expirarPontosCarteira(Long usuarioId, Long cartaoId, long pontos, String jobId, LocalDateTime quando) {
    if (usuarioId == null || cartaoId == null || pontos <= 0) return;

    var saldoOpt = saldoPontosRepository.findByUsuarioAndCartao(usuarioId, cartaoId);
    if (saldoOpt.isEmpty()) return;

    var saldo = saldoOpt.get();
    long expirado = Math.min(pontos, Math.max(0L, saldo.saldo == null ? 0L : saldo.saldo));
    if (expirado <= 0) return;

    saldo.saldo = (saldo.saldo == null ? 0L : saldo.saldo) - expirado;
    saldo.atualizadoEm = LocalDateTime.now();
    saldoPontosRepository.persist(saldo);

    var m = new org.acme.loyalty.entity.MovimentoPontos();
    m.usuario = saldo.usuario;
    m.cartao  = saldo.cartao;
    m.tipo    = org.acme.loyalty.entity.MovimentoPontos.TipoMovimento.EXPIRACAO;

    // conversão segura: limita ao intervalo de Integer
    int expInt = (expirado > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) expirado;
    m.pontos   = -expInt;

    m.criadoEm = (quando != null ? quando : LocalDateTime.now());
    m.jobId    = jobId;
    movimentoPontosRepository.persist(m);

    publicarPointsExpired(m);
}

    private void publicarPointsExpired(org.acme.loyalty.entity.MovimentoPontos m) {
        LOG.infof("EVENT PointsExpired{usuarioId=%d, cartaoId=%d, pontos=%d, jobId=%s, criadoEm=%s}",
                m.usuario.id, m.cartao.id, m.pontos, String.valueOf(m.jobId), String.valueOf(m.criadoEm));
        // Enviar para Kafka/Rabbit ou gravar Outbox quando necessário
    }
}
