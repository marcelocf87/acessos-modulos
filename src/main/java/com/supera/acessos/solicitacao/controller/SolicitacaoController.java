package com.supera.acessos.solicitacao.controller;

import com.supera.acessos.solicitacao.dto.CriarSolicitacaoDTO;
import com.supera.acessos.solicitacao.dto.ReprovarSolicitacaoDTO;
import com.supera.acessos.solicitacao.dto.SolicitacaoResponseDTO;
import com.supera.acessos.solicitacao.service.SolicitacaoModuloService;
import com.supera.acessos.usuario.entity.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {

    private final SolicitacaoModuloService solicitacaoService;

    @PostMapping
    public ResponseEntity<SolicitacaoResponseDTO> criar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CriarSolicitacaoDTO dto
    ) {
        return ResponseEntity.ok(
                solicitacaoService.toDTO(
                        solicitacaoService.criarSolicitacao(usuario, dto)
                )
        );
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<SolicitacaoResponseDTO> aprovar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario aprovador
    ) {
        return ResponseEntity.ok(
                solicitacaoService.toDTO(
                        solicitacaoService.aprovarSolicitacao(id, aprovador)
                )
        );
    }

    @PostMapping("/{id}/reprovar")
    public ResponseEntity<SolicitacaoResponseDTO> reprovar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario aprovador,
            @Valid @RequestBody ReprovarSolicitacaoDTO dto
    ) {
        return ResponseEntity.ok(
                solicitacaoService.toDTO(
                        solicitacaoService.reprovarSolicitacao(id, aprovador, dto.motivo())
                )
        );
    }

    @PostMapping("/{id}/renovar")
    public ResponseEntity<SolicitacaoResponseDTO> renovar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(
                solicitacaoService.toDTO(
                        solicitacaoService.renovarSolicitacao(id, usuario)
                )
        );
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<SolicitacaoResponseDTO> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(
                solicitacaoService.toDTO(
                        solicitacaoService.cancelarSolicitacao(id, usuario)
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoResponseDTO>> listarMinhasSolicitacoes(
            @AuthenticationPrincipal Usuario usuario
    ) {
        List<SolicitacaoResponseDTO> lista =
                solicitacaoService.listarSolicitacoesDoUsuario(usuario).stream()
                        .map(solicitacaoService::toDTO)
                        .toList();

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoResponseDTO> detalhar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(
                solicitacaoService.toDTO(
                        solicitacaoService.detalharSolicitacao(id, usuario)
                )
        );
    }

}
