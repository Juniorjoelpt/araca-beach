package com.aracabeach.controller;

import com.aracabeach.domain.produto.Produto;
import com.aracabeach.dto.ProdutoRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ProdutoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoRepository produtoRepository;

    @GetMapping
    public List<Produto> listar() {
        return produtoRepository.findAll();
    }

    @PostMapping
    public Produto criar(@Valid @RequestBody ProdutoRequest request) {
        Produto produto = Produto.builder()
                .nome(request.nome())
                .categoria(request.categoria())
                .preco(request.preco())
                .estoque(request.estoque() != null ? request.estoque() : 0)
                .custo(request.custo())
                .ehAluguel(request.ehAluguel())
                .build();
        return produtoRepository.save(produto);
    }

    @PutMapping("/{id}")
    public Produto atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
        produto.setNome(request.nome());
        produto.setCategoria(request.categoria());
        produto.setPreco(request.preco());
        produto.setEstoque(request.estoque() != null ? request.estoque() : 0);
        produto.setCusto(request.custo());
        produto.setEhAluguel(request.ehAluguel());
        return produtoRepository.save(produto);
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id) {
        produtoRepository.deleteById(id);
    }
}
