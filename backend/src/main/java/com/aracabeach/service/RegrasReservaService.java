package com.aracabeach.service;

import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.regra.BloqueioQuadra;
import com.aracabeach.domain.regra.RegraPreco;
import com.aracabeach.dto.BloqueioQuadraRequest;
import com.aracabeach.dto.BloqueioQuadraResponse;
import com.aracabeach.dto.RegraPrecoRequest;
import com.aracabeach.dto.RegraPrecoResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.BloqueioQuadraRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.RegraPrecoRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** CRUD de bloqueios de quadra e regras de preco por horario. */
@Service
@RequiredArgsConstructor
public class RegrasReservaService {

    private final BloqueioQuadraRepository bloqueioRepository;
    private final RegraPrecoRepository regraPrecoRepository;
    private final QuadraRepository quadraRepository;
    private final ReservaRepository reservaRepository;

    // ---- Bloqueios ----

    @Transactional(readOnly = true)
    public List<BloqueioQuadraResponse> listarBloqueios() {
        return bloqueioRepository.findByFimAfterOrderByInicioAsc(LocalDateTime.now()).stream()
                .map(this::paraResponse).toList();
    }

    @Transactional
    public BloqueioQuadraResponse criarBloqueio(BloqueioQuadraRequest request) {
        if (!request.fim().isAfter(request.inicio())) {
            throw new IllegalArgumentException("O fim do bloqueio deve ser posterior ao inicio.");
        }
        Quadra quadra = request.quadraId() == null ? null : quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + request.quadraId()));
        BloqueioQuadra b = bloqueioRepository.save(BloqueioQuadra.builder()
                .quadra(quadra)
                .inicio(request.inicio())
                .fim(request.fim())
                .motivo(request.motivo())
                .build());
        return paraResponse(b);
    }

    @Transactional
    public void removerBloqueio(Long id) {
        if (!bloqueioRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Bloqueio nao encontrado: " + id);
        }
        bloqueioRepository.deleteById(id);
    }

    /** Reservas ja confirmadas que caem dentro do bloqueio (nao sao canceladas automaticamente). */
    private int contarReservasAfetadas(BloqueioQuadra b) {
        List<Quadra> quadras = b.getQuadra() != null ? List.of(b.getQuadra()) : quadraRepository.findAll();
        int total = 0;
        for (Quadra q : quadras) {
            total += reservaRepository.findConflitantes(q.getId(), b.getInicio(), b.getFim()).size();
        }
        return total;
    }

    private BloqueioQuadraResponse paraResponse(BloqueioQuadra b) {
        return new BloqueioQuadraResponse(
                b.getId(),
                b.getQuadra() != null ? b.getQuadra().getId() : null,
                b.getQuadra() != null ? b.getQuadra().getNome() : "Todas as quadras",
                b.getInicio(),
                b.getFim(),
                b.getMotivo(),
                contarReservasAfetadas(b));
    }

    // ---- Precos ----

    @Transactional(readOnly = true)
    public List<RegraPrecoResponse> listarPrecos() {
        return regraPrecoRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional
    public RegraPrecoResponse criarPreco(RegraPrecoRequest request) {
        return paraResponse(regraPrecoRepository.save(aplicar(new RegraPreco(), request)));
    }

    @Transactional
    public RegraPrecoResponse atualizarPreco(Long id, RegraPrecoRequest request) {
        RegraPreco regra = regraPrecoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Regra de preco nao encontrada: " + id));
        return paraResponse(regraPrecoRepository.save(aplicar(regra, request)));
    }

    @Transactional
    public void removerPreco(Long id) {
        if (!regraPrecoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Regra de preco nao encontrada: " + id);
        }
        regraPrecoRepository.deleteById(id);
    }

    private RegraPreco aplicar(RegraPreco regra, RegraPrecoRequest r) {
        if (r.horaInicio().equals(r.horaFim())) {
            throw new IllegalArgumentException("Hora de inicio e fim da regra nao podem ser iguais.");
        }
        Quadra quadra = r.quadraId() == null ? null : quadraRepository.findById(r.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + r.quadraId()));
        String dias = null;
        if (r.diasSemana() != null && !r.diasSemana().isEmpty()) {
            // valida os nomes (lanca IllegalArgumentException se algum for invalido)
            dias = r.diasSemana().stream()
                    .map(d -> DayOfWeek.valueOf(d.trim().toUpperCase()).name())
                    .distinct()
                    .collect(Collectors.joining(","));
        }
        regra.setNome(r.nome());
        regra.setQuadra(quadra);
        regra.setDiasSemana(dias);
        regra.setHoraInicio(r.horaInicio());
        regra.setHoraFim(r.horaFim());
        regra.setValorHora(r.valorHora());
        regra.setAtiva(r.ativa());
        return regra;
    }

    private RegraPrecoResponse paraResponse(RegraPreco r) {
        List<String> dias = r.getDiasSemana() == null || r.getDiasSemana().isBlank()
                ? List.of()
                : Arrays.stream(r.getDiasSemana().split(",")).map(String::trim).toList();
        return new RegraPrecoResponse(
                r.getId(),
                r.getNome(),
                r.getQuadra() != null ? r.getQuadra().getId() : null,
                r.getQuadra() != null ? r.getQuadra().getNome() : "Todas as quadras",
                dias,
                r.getHoraInicio(),
                r.getHoraFim(),
                r.getValorHora(),
                r.isAtiva());
    }
}
