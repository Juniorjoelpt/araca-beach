package com.aracabeach.controller;

import com.aracabeach.dto.LucroResponse;
import com.aracabeach.service.LucroService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Lucro das vendas (somente ADMIN). */
@RestController
@RequestMapping("/api/lucro")
@RequiredArgsConstructor
public class LucroController {

    private final LucroService lucroService;

    @GetMapping
    public LucroResponse lucro(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return lucroService.gerar(inicio, fim);
    }
}
