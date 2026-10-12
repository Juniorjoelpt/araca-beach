package com.aracabeach.dto;

import java.time.LocalDateTime;

/** Uma ocorrencia de turma (aula em grupo) no calendario. */
public record TurmaAgendaResponse(Long turmaId, String nome, String professor, LocalDateTime inicio, LocalDateTime fim,
                                  int alunos) {}
