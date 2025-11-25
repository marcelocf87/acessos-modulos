package com.supera.acessos.solicitacao.dto;

import jakarta.validation.constraints.NotBlank;

public record ReprovarSolicitacaoDTO(

        @NotBlank(message = "O motivo da recusa é obrigatório")
        String motivo

) {}