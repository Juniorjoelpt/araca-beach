package com.aracabeach.service;

import com.aracabeach.domain.usuario.Usuario;
import com.aracabeach.dto.TrocarSenhaRequest;
import com.aracabeach.dto.UsuarioRequest;
import com.aracabeach.dto.UsuarioResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (usuarioRepository.findByLogin(request.login()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com este login.");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .login(request.login())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .perfil(request.perfil())
                .ativo(true)
                .build();

        return paraResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional
    public UsuarioResponse alterarStatus(Long id, boolean ativo) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(ativo);
        return paraResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse trocarSenha(Long id, TrocarSenhaRequest request) {
        Usuario usuario = buscarPorId(id);
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        return paraResponse(usuarioRepository.save(usuario));
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }

    private UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getPerfil(), usuario.isAtivo());
    }
}
