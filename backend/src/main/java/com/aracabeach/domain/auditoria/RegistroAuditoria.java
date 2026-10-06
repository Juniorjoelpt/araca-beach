package com.aracabeach.domain.auditoria;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trilha de auditoria: quem (equipe, cliente do portal ou sistema) fez qual
 * alteracao e quando. Registros nunca sao editados, apenas expirados pela
 * politica de retencao.
 */
@Entity
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_criado_em", columnList = "criado_em"),
        @Index(name = "idx_auditoria_recurso", columnList = "recurso")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    /** EQUIPE, CLIENTE ou SISTEMA. */
    @Column(name = "tipo_usuario", nullable = false, length = 10)
    private String tipoUsuario;

    /** Login (equipe) ou e-mail (cliente). */
    @Column(name = "usuario", length = 150)
    private String usuario;

    /** ADMIN, RECEPCAO ou CLIENTE. */
    @Column(length = 20)
    private String perfil;

    /** Verbo de negocio legivel: "Cancelou reserva", "Excluiu pagamento"... */
    @Column(nullable = false, length = 120)
    private String acao;

    /** Modulo afetado (reservas, pagamentos, usuarios...). */
    @Column(length = 60)
    private String recurso;

    @Column(name = "recurso_id", length = 40)
    private String recursoId;

    @Column(length = 500)
    private String detalhe;

    @Column(length = 10)
    private String metodo;

    @Column(length = 300)
    private String caminho;

    private Integer status;

    @Column(length = 45)
    private String ip;
}
