package com.aracabeach.domain.cliente;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Representa tanto os clientes cadastrados manualmente pela recepcao quanto
 * os que se auto-cadastram pelo portal (app mobile). "senhaHash" so e
 * preenchido para quem tem conta no portal - null significa que o cliente
 * foi cadastrado pela equipe e nao tem acesso de login (e o caso mais comum
 * hoje). O e-mail funciona como login quando o cliente tem conta no portal.
 *
 * IMPORTANTE: Cliente aparece embutido em Reserva/Comanda, que sao
 * retornados diretamente pela API (sem passar por DTO) - por isso os
 * campos/metodos de autenticacao tem @JsonIgnore, para a senha (mesmo
 * em hash) nunca vazar numa resposta JSON.
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(length = 14)
    private String cpf;

    @JsonIgnore
    @Column(name = "senha_hash")
    private String senhaHash;

    @Column(name = "criado_em")
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(length = 300)
    private String observacoes;

    /**
     * Confirmacao do e-mail da conta do portal. Nulo = conta antiga (anterior a esta
     * funcionalidade), tratada como confirmada; false = aguardando confirmacao.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "email_confirmado")
    private Boolean emailConfirmado;

    @JsonIgnore
    @Column(name = "confirmacao_token_hash", length = 64)
    private String confirmacaoTokenHash;

    @JsonIgnore
    @Column(name = "confirmacao_expira_em")
    private LocalDateTime confirmacaoExpiraEm;

    /** SHA-256 do token de recuperacao de senha (o token em si so vai por e-mail). */
    @JsonIgnore
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

    @JsonIgnore
    @Column(name = "reset_expira_em")
    private LocalDateTime resetExpiraEm;

    /** RECEPCAO (cadastrado pela equipe) ou PORTAL (auto-cadastro do jogador). Nulo = RECEPCAO (dados antigos). */
    @Column(name = "origem_cadastro", length = 20)
    private String origemCadastro;

    @Transient
    public boolean isEmailPendente() {
        return isPossuiAcessoPortal() && Boolean.FALSE.equals(emailConfirmado);
    }

    @Transient
    public boolean isPossuiAcessoPortal() {
        return senhaHash != null && !senhaHash.isBlank();
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return senhaHash;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return email;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }
}
