package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.ComandaRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Regras de negocio de Cliente. Extraido do ClienteController (que antes
 * salvava a entidade direto via repository, sem nenhuma validacao) para
 * impedir cadastro de e-mail/telefone duplicados e para permitir exclusao
 * de forma segura (sem violar integridade referencial com Reserva/Comanda).
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoria;
    private final ReservaRepository reservaRepository;
    private final ComandaRepository comandaRepository;

    @Transactional
    public Cliente criar(Cliente cliente) {
        cliente.setId(null);
        cliente.setOrigemCadastro("RECEPCAO");
        validarDuplicidade(cliente.getEmail(), cliente.getTelefone(), null);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente atualizar(Long id, Cliente cliente) {
        Cliente existente = buscarPorId(id);
        validarDuplicidade(cliente.getEmail(), cliente.getTelefone(), id);
        cliente.setId(existente.getId());
        // Campos que nao vem do formulario da recepcao (senha do portal e @JsonIgnore):
        // preservar para a edicao nao apagar o acesso do jogador ao portal.
        cliente.setSenhaHash(existente.getSenhaHash());
        cliente.setOrigemCadastro(existente.getOrigemCadastro());
        cliente.setResetTokenHash(existente.getResetTokenHash());
        cliente.setResetExpiraEm(existente.getResetExpiraEm());
        cliente.setEmailConfirmado(existente.getEmailConfirmado());
        cliente.setConfirmacaoTokenHash(existente.getConfirmacaoTokenHash());
        cliente.setConfirmacaoExpiraEm(existente.getConfirmacaoExpiraEm());
        // Trocar o e-mail de uma conta do portal exige confirmar o novo endereco.
        boolean emailMudou = existente.getEmail() != null
                && !existente.getEmail().equalsIgnoreCase(cliente.getEmail() == null ? "" : cliente.getEmail());
        if (existente.isPossuiAcessoPortal() && emailMudou) {
            cliente.setEmailConfirmado(false);
            cliente.setConfirmacaoTokenHash(null);
            cliente.setConfirmacaoExpiraEm(null);
        }
        cliente.setCriadoEm(existente.getCriadoEm());
        return clienteRepository.save(cliente);
    }

    /** Fallback da recepcao quando o e-mail de confirmacao nao chega (ex.: SMTP fora do ar). */
    @Transactional
    public Cliente confirmarEmailManualmente(Long id) {
        Cliente cliente = buscarPorId(id);
        cliente.setEmailConfirmado(true);
        cliente.setConfirmacaoTokenHash(null);
        cliente.setConfirmacaoExpiraEm(null);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void deletar(Long id) {
        Cliente alvo = buscarPorId(id);

        boolean possuiReservas = !reservaRepository.findByClienteIdOrderByInicioDesc(id).isEmpty();
        boolean possuiComandas = !comandaRepository.findByClienteIdOrderByCriadoEmDesc(id).isEmpty();

        if (possuiReservas || possuiComandas) {
            throw new IllegalArgumentException(
                    "Nao e possivel excluir este cliente: ele possui reservas ou comandas registradas no historico.");
        }

        auditoria.detalhe("Cliente '" + alvo.getNome() + "' (#" + id + ")");
        clienteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + id));
    }

    private void validarDuplicidade(String email, String telefone, Long idParaIgnorar) {
        if (email != null && !email.isBlank()) {
            Optional<Cliente> porEmail = clienteRepository.findByEmail(email);
            if (porEmail.isPresent() && !porEmail.get().getId().equals(idParaIgnorar)) {
                throw new IllegalArgumentException("Ja existe um cliente cadastrado com este e-mail.");
            }
        }
        if (telefone != null && !telefone.isBlank()) {
            Optional<Cliente> porTelefone = clienteRepository.findByTelefone(telefone);
            if (porTelefone.isPresent() && !porTelefone.get().getId().equals(idParaIgnorar)) {
                throw new IllegalArgumentException("Ja existe um cliente cadastrado com este telefone.");
            }
        }
    }
}
