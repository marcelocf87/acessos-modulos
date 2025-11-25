package com.supera.acessos.solicitacao.service;

import com.supera.acessos.modulo.entity.Modulo;
import com.supera.acessos.solicitacao.entity.SolicitacaoModulo;
import com.supera.acessos.solicitacao.entity.StatusSolicitacao;
import com.supera.acessos.solicitacao.repository.SolicitacaoModuloRepository;
import com.supera.acessos.usuario.entity.Usuario;
import com.supera.acessos.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SolicitacaoModuloService {

    private final SolicitacaoModuloRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;

    //aprovar
    public SolicitacaoModulo aprovarSolicitacao(Long solicitacaoId, Usuario aprovador) {

        SolicitacaoModulo solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

        Modulo modulo = solicitacao.getModulo();
        Usuario usuario = solicitacao.getSolicitante();

        if (!modulo.isAtivo()) {
            throw new RuntimeException("Módulo inativo");
        }

        if (usuario.getModulosAtivos().contains(modulo)) {
            throw new RuntimeException("Usuário já possui este módulo");
        }

        switch (solicitacao.getStatus()) {

            case AGUARDANDO_GESTOR -> {
                if (modulo.isExigeAprovacaoSeguranca()) {
                    solicitacao.setStatus(StatusSolicitacao.AGUARDANDO_SEGURANCA);
                } else {
                    solicitacao.setStatus(StatusSolicitacao.APROVADA);
                    concederAcesso(usuario, modulo);
                    registrarExpiracao(solicitacao, modulo);
                }
            }

            case AGUARDANDO_SEGURANCA -> {
                solicitacao.setStatus(StatusSolicitacao.APROVADA);
                concederAcesso(usuario, modulo);
                registrarExpiracao(solicitacao, modulo);
            }

            case ABERTA -> {
                if (modulo.isExigeAprovacaoGestor()) {
                    solicitacao.setStatus(StatusSolicitacao.AGUARDANDO_GESTOR);
                } else if (modulo.isExigeAprovacaoSeguranca()) {
                    solicitacao.setStatus(StatusSolicitacao.AGUARDANDO_SEGURANCA);
                } else {
                    solicitacao.setStatus(StatusSolicitacao.APROVADA);
                    concederAcesso(usuario, modulo);
                    registrarExpiracao(solicitacao, modulo);
                }
            }

            default -> throw new RuntimeException("Solicitação não pode ser aprovada.");
        }

        solicitacao.setDataAprovacao(LocalDateTime.now());

        usuarioRepository.save(usuario);
        return solicitacaoRepository.save(solicitacao);
    }

    private void concederAcesso(Usuario usuario, Modulo modulo) {
        usuario.getModulosAtivos().add(modulo);
    }

    private void registrarExpiracao(SolicitacaoModulo solicitacao, Modulo modulo) {
        if (modulo.getPrazoExpiracaoDias() > 0) {
            solicitacao.setDataExpiracao(
                    LocalDateTime.now().plusDays(modulo.getPrazoExpiracaoDias())
            );
        }
    }

    //reprovar
    public SolicitacaoModulo reprovarSolicitacao(Long id, Usuario aprovador, String motivo) {

        SolicitacaoModulo solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

        switch (solicitacao.getStatus()) {
            case ABERTA, AGUARDANDO_GESTOR, AGUARDANDO_SEGURANCA -> {
                solicitacao.setStatus(StatusSolicitacao.REPROVADA);
                solicitacao.setDataReprovacao(LocalDateTime.now());
                solicitacao.setMotivoRecusa(motivo);
            }

            default -> throw new RuntimeException("Solicitação não pode ser reprovada");
        }

        return solicitacaoRepository.save(solicitacao);
    }
}