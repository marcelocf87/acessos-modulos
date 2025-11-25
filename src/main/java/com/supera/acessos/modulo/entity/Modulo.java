package com.supera.acessos.modulo.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "modulos")
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false)
    private boolean exigeAprovacaoGestor;

    @Column(nullable = false)
    private boolean exigeAprovacaoSeguranca;

    // prazo de expiração em dias (0 = não expira)
    @Column(nullable = false)
    private Integer prazoExpiracaoDias;

    @Column(nullable = false)
    private boolean ativo;
}