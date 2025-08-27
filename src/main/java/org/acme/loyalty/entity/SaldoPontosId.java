package org.acme.loyalty.entity;

import java.io.Serializable;
import java.util.Objects;

public class SaldoPontosId implements Serializable {
    
    private Long usuario;
    private Long cartao;
    
    public SaldoPontosId() {}
    
    public SaldoPontosId(Long usuario, Long cartao) {
        this.usuario = usuario;
        this.cartao = cartao;
    }
    
    public Long getUsuario() {
        return usuario;
    }
    
    public void setUsuario(Long usuario) {
        this.usuario = usuario;
    }
    
    public Long getCartao() {
        return cartao;
    }
    
    public void setCartao(Long cartao) {
        this.cartao = cartao;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaldoPontosId that = (SaldoPontosId) o;
        return Objects.equals(usuario, that.usuario) &&
               Objects.equals(cartao, that.cartao);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(usuario, cartao);
    }
    
    @Override
    public String toString() {
        return "SaldoPontosId{" +
                "usuario=" + usuario +
                ", cartao=" + cartao +
                '}';
    }
}

