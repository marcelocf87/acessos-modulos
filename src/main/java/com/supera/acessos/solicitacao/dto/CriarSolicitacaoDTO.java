package com.supera.acessos.solicitacao.dto;

import jakarta.validation.constraints.NotNull;

public record CriarSolicitacaoDTO(

        @NotNull(message = "O módulo é obrigatório")
        Long moduloId

) {}