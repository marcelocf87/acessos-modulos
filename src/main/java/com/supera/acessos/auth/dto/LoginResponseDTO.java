package com.supera.acessos.auth.dto;

public record LoginResponseDTO(
        String token,
        String expiraEm
) {}
