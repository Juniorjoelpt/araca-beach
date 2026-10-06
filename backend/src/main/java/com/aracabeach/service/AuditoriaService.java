package com.aracabeach.service;

import com.aracabeach.domain.auditoria.RegistroAuditoria;
import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.usuario.Usuario;
import com.aracabeach.repository.AuditoriaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Trilha de auditoria. O registro automatico de toda escrita (POST/PUT/PATCH/DELETE)
 * e feito por {@link com.aracabeach.config.AuditoriaInterceptor}; os servicos podem
 * enriquecer a entrada da requisicao em curso com {@link #detalhe(String)} (ex.: multa
 * isentada). Falha ao auditar nunca derruba a operacao de negocio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    public static final String ATRIBUTO_DETALHE = "auditoria.detalhe";
    private static final int MAX_DETALHE = 500;

    private final AuditoriaRepository repository;

    /** Anexa uma observacao a entrada de auditoria da requisicao em andamento (sem efeito fora de uma requisicao). */
    public void detalhe(String texto) {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs == null || texto == null) return;
            Object atual = attrs.getAttribute(ATRIBUTO_DETALHE, RequestAttributes.SCOPE_REQUEST);
            String novo = atual == null ? texto : atual + " | " + texto;
            attrs.setAttribute(ATRIBUTO_DETALHE, novo, RequestAttributes.SCOPE_REQUEST);
        } catch (RuntimeException e) {
            log.debug("Nao foi possivel anexar detalhe de auditoria: {}", e.getMessage());
        }
    }

    /** Registro direto (login, tarefas do sistema, etc.). Nunca lanca excecao. */
    public void registrar(String tipoUsuario, String usuario, String perfil, String acao,
                          String recurso, String recursoId, String detalhe, String ip) {
        try {
            repository.save(RegistroAuditoria.builder()
                    .tipoUsuario(tipoUsuario)
                    .usuario(cortar(usuario, 150))
                    .perfil(perfil)
                    .acao(cortar(acao, 120))
                    .recurso(cortar(recurso, 60))
                    .recursoId(cortar(recursoId, 40))
                    .detalhe(cortar(detalhe, MAX_DETALHE))
                    .ip(cortar(ip, 45))
                    .build());
        } catch (RuntimeException e) {
            log.warn("Falha ao gravar auditoria ({}): {}", acao, e.getMessage());
        }
    }

    /** Registro de uma requisicao HTTP autenticada. */
    public void registrarRequisicao(Authentication auth, String metodo, String caminho, int status,
                                    String acao, String recurso, String recursoId, String detalhe, String ip) {
        try {
            String tipo = "SISTEMA";
            String usuario = null;
            String perfil = null;
            Object principal = auth != null ? auth.getPrincipal() : null;
            if (principal instanceof Usuario u) {
                tipo = "EQUIPE";
                usuario = u.getLogin();
                perfil = u.getPerfil().name();
            } else if (principal instanceof Cliente c) {
                tipo = "CLIENTE";
                usuario = c.getEmail();
                perfil = "CLIENTE";
            }
            repository.save(RegistroAuditoria.builder()
                    .tipoUsuario(tipo)
                    .usuario(cortar(usuario, 150))
                    .perfil(perfil)
                    .acao(cortar(acao, 120))
                    .recurso(cortar(recurso, 60))
                    .recursoId(cortar(recursoId, 40))
                    .detalhe(cortar(detalhe, MAX_DETALHE))
                    .metodo(cortar(metodo, 10))
                    .caminho(cortar(caminho, 300))
                    .status(status)
                    .ip(cortar(ip, 45))
                    .build());
        } catch (RuntimeException e) {
            log.warn("Falha ao gravar auditoria de {} {}: {}", metodo, caminho, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<RegistroAuditoria> buscar(LocalDate inicio, LocalDate fim, String usuario, String recurso,
                                          String tipoUsuario, String texto, int pagina, int tamanho) {
        Specification<RegistroAuditoria> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (inicio != null) ps.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), LocalDateTime.of(inicio, LocalTime.MIN)));
            if (fim != null) ps.add(cb.lessThanOrEqualTo(root.get("criadoEm"), LocalDateTime.of(fim, LocalTime.MAX)));
            if (usuario != null && !usuario.isBlank())
                ps.add(cb.like(cb.lower(root.get("usuario")), "%" + usuario.trim().toLowerCase() + "%"));
            if (recurso != null && !recurso.isBlank()) ps.add(cb.equal(root.get("recurso"), recurso));
            if (tipoUsuario != null && !tipoUsuario.isBlank()) ps.add(cb.equal(root.get("tipoUsuario"), tipoUsuario));
            if (texto != null && !texto.isBlank()) {
                String like = "%" + texto.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("acao")), like),
                        cb.like(cb.lower(root.get("detalhe")), like),
                        cb.like(cb.lower(root.get("caminho")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        int tam = Math.max(1, Math.min(tamanho, 200));
        return repository.findAll(spec,
                PageRequest.of(Math.max(0, pagina), tam, Sort.by(Sort.Direction.DESC, "criadoEm", "id")));
    }

    @Transactional(readOnly = true)
    public List<String> recursos() {
        return repository.recursosDistintos();
    }

    public int expirarAntigos(int dias) {
        return repository.apagarAnterioresA(LocalDateTime.now().minusDays(dias));
    }

    private static String cortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
