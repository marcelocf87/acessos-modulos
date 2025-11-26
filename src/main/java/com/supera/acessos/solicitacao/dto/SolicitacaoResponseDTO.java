package com.supera.acessos.solicitacao.dto;

import com.supera.acessos.modulo.dto.ModuloResumoDTO;
import com.supera.acessos.usuario.dto.UsuarioResumoDTO;
import com.supera.acessos.solicitacao.entity.StatusSolicitacao;

import java.time.LocalDateTime;

public record SolicitacaoResponseDTO(
        Long id,
        UsuarioResumoDTO solicitante,
        ModuloResumoDTO modulo,
        StatusSolicitacao status,
        LocalDateTime dataAbertura,
        LocalDateTime dataAprovacao,
        LocalDateTime dataReprovacao,
        LocalDateTime dataExpiracao,
        LocalDateTime dataCancelamento,
        String motivoRecusa
) {}