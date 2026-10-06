package com.aracabeach.config;

import com.aracabeach.domain.restaurante.CategoriaCardapio;
import com.aracabeach.domain.restaurante.ItemCardapio;
import com.aracabeach.domain.restaurante.Praca;
import com.aracabeach.repository.CategoriaCardapioRepository;
import com.aracabeach.repository.ItemCardapioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Carga inicial do cardapio do restaurante (a partir do cardapio impresso da Araca). Roda
 * apenas quando ainda nao existe nenhuma categoria; depois disso o cardapio e gerido pela tela.
 * Nao e @Transactional de proposito: uma falha aqui so e registrada em log e nao derruba o sistema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CardapioSeeder implements ApplicationRunner {

    private final CategoriaCardapioRepository categoriaRepository;
    private final ItemCardapioRepository itemRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (categoriaRepository.count() > 0) return;

            CategoriaCardapio c = cat("Lanches leves", 1);
            item(c, "Misto", "Pão forma ou francês, manteiga, queijo e presunto na chapa", null, 14);
            item(c, "Francês com carne de sol e queijo", "Pão francês, manteiga, carne de sol desfiada, queijo mussarela, alface, tomate e maionese da casa", null, 22);
            item(c, "Carne de sol com queijo", "Massa de tapioca ou cuscuz, manteiga, carne de sol desfiada e queijo mussarela", null, 19);
            item(c, "Frango", "Massa de tapioca ou cuscuz, manteiga e filé de frango desfiado", null, 16);
            item(c, "Omelete simples", null, null, 14);
            item(c, "Omelete recheado", "Frango com queijo, queijo com presunto ou carne com queijo", null, 19);

            c = cat("Burgers", 2);
            item(c, "Burger Clássico", "Pão brioche, blend especial 120g, cheddar, alface americana, tomate, cebola roxa e maionese de alho da casa", null, 32);
            item(c, "Burger Bacon", "Pão brioche, blend especial 120g, queijo cheddar, bacon crispy, cebola caramelizada e maionese de alho da casa", null, 35);

            c = cat("Petiscos", 3);
            item(c, "Fritas Araçá", "400g de fritas com creme de alho, carne de sol desfiada, bacon, parmesão, cebolinha e orégano", null, 48);
            item(c, "Fritas Cheddar Bacon", "400g de fritas com creme de cheddar, bacon, parmesão, cebolinha e orégano", null, 48);
            item(c, "Fritas Simples", "400g de fritas com creme de alho", null, 30);
            item(c, "Coxinhas do Chef", "Cinco coxinhas de frango 100% recheio, empanadas na farinha panko, com maionese de alho", null, 36);
            item(c, "Grill com Queijo Coalho", "300g de alcatra trinchada, queijo coalho e paçoca da casa", null, 98);
            item(c, "Chapa Picanha do Chef", "400g de picanha premium trinchada, grelhada no fogo, 400g de fritas com bacon e queijo ralado e macaxeira cozida", null, 119);
            item(c, "Pasteizinhos Carne de Sol com Queijo", "Seis unidades de pastéis recheados de carne de sol com cream cheese e geleia", null, 35);
            item(c, "Pasteizinhos Camarão", "Seis unidades de pastéis de camarão", null, 39);

            c = cat("Jantinha churrasco", 4);
            String jant = "150g de carne. Acompanha arroz branco, tropeiro, fritas, vinagrete e creme de alho";
            item(c, "Jantinha de Picanha", jant, "Serve individualmente", 50);
            item(c, "Jantinha de Alcatra", jant, "Serve individualmente", 38);
            item(c, "Jantinha de Filé com Bacon e Catupiry", jant, "Serve individualmente", 42);
            item(c, "Jantinha de Filé de Frango", jant, "Serve individualmente", 36);
            item(c, "Jantinha de Alcatra + Linguiça", jant, "Serve individualmente", 38);

            c = cat("Churrasco para compartilhar", 5);
            String comp = "Acompanha arroz branco, tropeiro, fritas, vinagrete, farofa e creme de alho";
            item(c, "Picanha 400g", comp, "Serve 2 pessoas", 120);
            item(c, "Picanha 800g", comp, "Serve 3 a 4 pessoas", 220);
            item(c, "Alcatra 400g", comp, "Serve 2 pessoas", 105);
            item(c, "Alcatra 800g", comp, "Serve 3 a 4 pessoas", 200);
            item(c, "Filé Mignon 400g", comp, "Serve 2 pessoas", 115);
            item(c, "Filé Mignon 800g", comp, "Serve 3 a 4 pessoas", 210);
            item(c, "Alcatra + Linguiça + Filé de Frango 400g", comp, "Serve 2 pessoas", 105);
            item(c, "Alcatra + Linguiça + Filé de Frango 800g", comp, "Serve 3 a 4 pessoas", 200);

            c = cat("Porções extras (250g)", 6);
            item(c, "Arroz branco (250g)", null, null, 15);
            item(c, "Tropeiro (250g)", null, null, 20);
            item(c, "Vinagrete (250g)", null, null, 15);
            item(c, "Farofa (250g)", null, null, 15);

            c = cat("Pratos especiais", 7);
            item(c, "Parmegiana Clássico", "Filé mignon empanado, gratinado ao pomodoro da casa, com queijo e orégano, acompanhado de arroz branco e batata palha caseira", "Serve individual", 45);
            item(c, "Tilápia com Legumes", "Filé de tilápia, acompanha arroz branco e legumes refogados", "Serve individual", 48);
            item(c, "Caldeirada Maranhense", "Camarão descascado ao molho de tomate com leite de coco, refogado com tomate, cebola e pimentões, acompanha arroz branco e farofa", "Serve 2 pessoas", 150);
            item(c, "Peixe Frito", "Postas de pescada amarela frita, acompanha arroz branco, farofa e vinagrete", "Serve 2 pessoas", 120);

            log.info("Cardápio do restaurante carregado: {} itens.", itemRepository.count());
        } catch (RuntimeException e) {
            log.warn("Não foi possível carregar o cardápio inicial: {}", e.getMessage());
        }
    }

    private CategoriaCardapio cat(String nome, int ordem) {
        return categoriaRepository.save(CategoriaCardapio.builder().nome(nome).ordem(ordem).build());
    }

    private void item(CategoriaCardapio categoria, String nome, String descricao, String porcao, int preco) {
        itemRepository.save(ItemCardapio.builder()
                .categoria(categoria).nome(nome).descricao(descricao).porcao(porcao)
                .preco(BigDecimal.valueOf(preco)).praca(Praca.COZINHA).build());
    }
}
