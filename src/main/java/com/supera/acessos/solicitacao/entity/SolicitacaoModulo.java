package com.supera.acessos.solicitacao.entity;

import com.supera.acessos.modulo.entity.Modulo;
import com.supera.acessos.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "solicitacoes")
public class SolicitacaoModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // quem pediu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario solicitante;

    // qual módulo está sendo solicitado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao status;

    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    private LocalDateTime dataAprovacao;
    private LocalDateTime dataReprovacao;
    private LocalDateTime dataExpiracao;

    private String motivoRecusa;
}