package com.supera.acessos.solicitacao.service;

import com.supera.acessos.modulo.entity.Modulo;
import com.supera.acessos.solicitacao.dto.CriarSolicitacaoDTO;
import com.supera.acessos.solicitacao.entity.SolicitacaoModulo;
import com.supera.acessos.solicitacao.entity.StatusSolicitacao;
import com.supera.acessos.solicitacao.repository.SolicitacaoModuloRepository;
import com.supera.acessos.usuario.entity.Usuario;
import com.supera.acessos.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.supera.acessos.modulo.repository.ModuloRepository;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitacaoModuloService {

    private final SolicitacaoModuloRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloRepository moduloRepository;

    public SolicitacaoModulo criarSolicitacao(Usuario usuario, CriarSolicitacaoDTO dto) {

        // 1. Buscar módulo
        Modulo modulo = moduloRepository.findById(dto.moduloId())
                .orElseThrow(() -> new RuntimeException("Módulo não encontrado"));

        if (!modulo.isAtivo()) {
            throw new RuntimeException("Módulo inativo");
        }

        // 2. Validar se o usuário já tem acesso
        boolean jaTemAcesso = usuario.getModulosAtivos().stream()
                .anyMatch(m -> m.getId().equals(modulo.getId()));

        if (jaTemAcesso) {
            throw new RuntimeException("Usuário já possui acesso ativo ao módulo");
        }

        // 3. Verificar se existe solicitação pendente
        boolean existePendente = solicitacaoRepository
                .existsBySolicitanteAndModuloAndStatusIn(
                        usuario,
                        modulo,
                        Arrays.asList(
                                StatusSolicitacao.ABERTA,
                                StatusSolicitacao.AGUARDANDO_GESTOR,
                                StatusSolicitacao.AGUARDANDO_SEGURANCA
                        )
                );

        if (existePendente) {
            throw new RuntimeException("Já existe solicitação pendente para este módulo");
        }

        // 4. Determinar status inicial
        StatusSolicitacao statusInicial;

        if (!modulo.isExigeAprovacaoGestor() && !modulo.isExigeAprovacaoSeguranca()) {
            statusInicial = StatusSolicitacao.APROVADA;
        } else if (modulo.isExigeAprovacaoGestor()) {
            statusInicial = StatusSolicitacao.AGUARDANDO_GESTOR;
        } else {
            statusInicial = StatusSolicitacao.AGUARDANDO_SEGURANCA;
        }

        // 5. Criar solicitação
        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .solicitante(usuario)
                .modulo(modulo)
                .status(statusInicial)
                .dataSolicitacao(LocalDateTime.now())  // o nome do campo correto
                .build();

        // 6. Se foi aprovada automaticamente
        if (statusInicial == StatusSolicitacao.APROVADA) {
            concederAcesso(usuario, modulo);
            registrarExpiracao(solicitacao, modulo);
            usuarioRepository.save(usuario);
        }

        return solicitacaoRepository.save(solicitacao);
    }

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

    private boolean isExpirada(SolicitacaoModulo solicitacao) {
        return solicitacao.getDataExpiracao() != null &&
                solicitacao.getDataExpiracao().isBefore(LocalDateTime.now());
    }

    private void expirarSeNecessario(SolicitacaoModulo solicitacao) {

        if (!isExpirada(solicitacao)) return;

        solicitacao.setStatus(StatusSolicitacao.EXPIRADA);

        Usuario usuario = solicitacao.getSolicitante();
        Modulo modulo = solicitacao.getModulo();

        usuario.getModulosAtivos().remove(modulo);

        usuarioRepository.save(usuario);
        solicitacaoRepository.save(solicitacao);
    }

    public SolicitacaoModulo renovarSolicitacao(Long id, Usuario solicitante) {

        SolicitacaoModulo antiga = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

        expirarSeNecessario(antiga);

        if (antiga.getStatus() != StatusSolicitacao.EXPIRADA) {
            throw new RuntimeException("Somente solicitações expiradas podem ser renovadas.");
        }

        Modulo modulo = antiga.getModulo();

        //criar nova solicitação
        SolicitacaoModulo nova = new SolicitacaoModulo();
        nova.setSolicitante(solicitante);
        nova.setModulo(modulo);
        nova.setStatus(StatusSolicitacao.ABERTA);
        nova.setDataSolicitacao(LocalDateTime.now());

        if (modulo.isExigeAprovacaoGestor()) {
            nova.setStatus(StatusSolicitacao.AGUARDANDO_GESTOR);
        } else if (modulo.isExigeAprovacaoSeguranca()) {
            nova.setStatus(StatusSolicitacao.AGUARDANDO_SEGURANCA);
        } else {
            nova.setStatus(StatusSolicitacao.APROVADA);
            concederAcesso(solicitante, modulo);
            registrarExpiracao(nova, modulo);
            usuarioRepository.save(solicitante);
        }

        return solicitacaoRepository.save(nova);
    }

    public SolicitacaoModulo cancelarSolicitacao(Long id, Usuario solicitante) {

        //buscar a solicitação
        SolicitacaoModulo solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

        //expirar automaticamente qdo necessario
        expirarSeNecessario(solicitacao);

        //verificar status inválidos para cancelamento
        switch (solicitacao.getStatus()) {

            case CANCELADA, REPROVADA, EXPIRADA ->
                    throw new RuntimeException("Solicitação não pode ser cancelada.");

            default -> {
            }
        }

        //remover acesso ativo do usuario se aprovado
        if (solicitacao.getStatus() == StatusSolicitacao.APROVADA) {
            Usuario usuario = solicitacao.getSolicitante();
            Modulo modulo = solicitacao.getModulo();

            usuario.getModulosAtivos().remove(modulo);
            usuarioRepository.save(usuario); //persistir no banco
        }

        //atualizar status
        solicitacao.setStatus(StatusSolicitacao.CANCELADA);

        //registrar data de cancelamento
        solicitacao.setDataCancelamento(LocalDateTime.now());

        //salvar e retornar
        return solicitacaoRepository.save(solicitacao);
    }

    public List<SolicitacaoModulo> listarSolicitacoesDoUsuario(Usuario usuario) {

        // Busca todas as solicitações do usuário
        List<SolicitacaoModulo> lista =
                solicitacaoRepository.findBySolicitante(usuario);

        // Aplica expiração automática em cada uma
        lista.forEach(this::expirarSeNecessario);

        return lista;
    }

    public SolicitacaoModulo detalharSolicitacao(Long id, Usuario usuario) {

        SolicitacaoModulo sol = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

        if (!sol.getSolicitante().getId().equals(usuario.getId())) {
            throw new RuntimeException("Acesso negado");
        }

        expirarSeNecessario(sol);

        return sol;
    }




}