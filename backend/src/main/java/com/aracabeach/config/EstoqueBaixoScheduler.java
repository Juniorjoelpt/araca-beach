package com.aracabeach.config;

import com.aracabeach.domain.produto.Produto;
import com.aracabeach.repository.ProdutoRepository;
import com.aracabeach.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Uma vez por dia, verifica produtos com estoque igual ou abaixo do limite
 * configurado e envia um e-mail de resumo para o endereco interno da arena
 * (se configurado). Nao envia nada se nenhum produto estiver baixo.
 */
@Component
@RequiredArgsConstructor
public class EstoqueBaixoScheduler {

    private final ProdutoRepository produtoRepository;
    private final NotificacaoService notificacaoService;

    @Value("${araca-beach.notificacoes.email.limite-estoque-baixo:5}")
    private int limiteEstoqueBaixo;

    // Roda todo dia as 08:00
    @Scheduled(cron = "0 0 8 * * *")
    public void verificarEstoque() {
        List<Produto> produtosComEstoqueBaixo = produtoRepository.findByEstoqueLessThanEqual(limiteEstoqueBaixo);
        notificacaoService.enviarAlertaEstoqueBaixo(produtosComEstoqueBaixo);
    }
}
