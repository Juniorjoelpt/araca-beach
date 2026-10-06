package com.aracabeach.controller;

import com.aracabeach.domain.auditoria.RegistroAuditoria;
import com.aracabeach.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public Map<String, Object> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String recurso,
            @RequestParam(required = false) String tipoUsuario,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        Page<RegistroAuditoria> p = auditoriaService.buscar(inicio, fim, usuario, recurso, tipoUsuario, texto, pagina, tamanho);
        return Map.of(
                "itens", p.getContent(),
                "pagina", p.getNumber(),
                "totalPaginas", p.getTotalPages(),
                "total", p.getTotalElements());
    }

    @GetMapping("/recursos")
    public List<String> recursos() {
        return auditoriaService.recursos();
    }
}
