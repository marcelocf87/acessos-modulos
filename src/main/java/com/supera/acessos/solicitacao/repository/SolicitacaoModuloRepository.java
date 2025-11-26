package com.supera.acessos.solicitacao.repository;

import com.supera.acessos.solicitacao.entity.SolicitacaoModulo;
import com.supera.acessos.solicitacao.entity.StatusSolicitacao;
import com.supera.acessos.usuario.entity.Usuario;
import com.supera.acessos.modulo.entity.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoModuloRepository
        extends JpaRepository<SolicitacaoModulo, Long> {

    boolean existsBySolicitanteAndModuloAndStatusIn(
            Usuario solicitante,
            Modulo modulo,
            List<StatusSolicitacao> status
    );

    List<SolicitacaoModulo> findBySolicitante(Usuario usuario);

}