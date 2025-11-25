package com.supera.acessos.usuario.entity;

import com.supera.acessos.modulo.entity.Modulo;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor //jpa exige esse construtor por padrão
@Builder
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String senha; //vou armazenar com hash + salt

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Departamento departamento;

    //para fazer relacionamento com módulos ativos
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuarios_modulos",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "modulo_id")
    )

    private Set<Modulo> modulosAtivos = new HashSet<>();
}
