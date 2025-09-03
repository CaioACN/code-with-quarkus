package org.acme.loyalty.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ExtratoPontosDTO {
    
    public Long usuarioId;
    public Long cartaoId;
    public LocalDateTime dataInicio;
    public LocalDateTime dataFim;
    public List<MovimentoPontosDTO> movimentos;
    public Integer pagina;
    public Integer tamanhoPagina;
    public Long totalRegistros;
    public Integer totalPaginas;
    public Long saldoInicial;
    public Long saldoFinal;
    
    // Construtores
    public ExtratoPontosDTO() {}
    
    public ExtratoPontosDTO(Long usuarioId, Long cartaoId, LocalDateTime dataInicio, 
                            LocalDateTime dataFim, List<MovimentoPontosDTO> movimentos, 
                            Integer pagina, Integer tamanhoPagina, Long totalRegistros) {
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.movimentos = movimentos;
        this.pagina = pagina;
        this.tamanhoPagina = tamanhoPagina;
        this.totalRegistros = totalRegistros;
        this.totalPaginas = calcularTotalPaginas();
    }
    
    // Métodos de negócio
    private Integer calcularTotalPaginas() {
        if (tamanhoPagina == null || tamanhoPagina <= 0) return 0;
        if (totalRegistros == null || totalRegistros <= 0) return 0;
        
        return (int) Math.ceil((double) totalRegistros / tamanhoPagina);
    }
    
    public void calcularSaldos(Long saldoInicial) {
        this.saldoInicial = saldoInicial;
        this.saldoFinal = saldoInicial;
        
        if (movimentos != null) {
            for (MovimentoPontosDTO movimento : movimentos) {
                if (movimento.pontos != null) {
                    this.saldoFinal += movimento.pontos;
                }
            }
        }
    }
    
    public boolean temProximaPagina() {
        return pagina != null && totalPaginas != null && pagina < totalPaginas;
    }
    
    public boolean temPaginaAnterior() {
        return pagina != null && pagina > 1;
    }
}

