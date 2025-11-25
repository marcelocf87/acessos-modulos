package com.supera.acessos.solicitacao.controller;

import com.supera.acessos.solicitacao.dto.CriarSolicitacaoDTO;
import com.supera.acessos.solicitacao.dto.ReprovarSolicitacaoDTO;
import com.supera.acessos.solicitacao.entity.SolicitacaoModulo;
import com.supera.acessos.solicitacao.service.SolicitacaoModuloService;
import com.supera.acessos.usuario.entity.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {

    private final SolicitacaoModuloService solicitacaoService;

    @PostMapping
    public ResponseEntity<SolicitacaoModulo> criar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CriarSolicitacaoDTO dto
    ) {
        return ResponseEntity.ok(solicitacaoService.criarSolicitacao(usuario, dto));
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<SolicitacaoModulo> aprovar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario aprovador
    ) {
        return ResponseEntity.ok(solicitacaoService.aprovarSolicitacao(id, aprovador));
    }

    @PostMapping("/{id}/reprovar")
    public ResponseEntity<SolicitacaoModulo> reprovar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario aprovador,
            @Valid @RequestBody ReprovarSolicitacaoDTO dto
    ) {
        return ResponseEntity.ok(
                solicitacaoService.reprovarSolicitacao(id, aprovador, dto.motivo())
        );
    }
}