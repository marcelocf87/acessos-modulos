package com.supera.acessos.modulo.controller;

import com.supera.acessos.modulo.entity.Modulo;
import com.supera.acessos.modulo.repository.ModuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/modulos")
@RequiredArgsConstructor
public class ModuloController {

    private final ModuloRepository moduloRepository;

    @GetMapping
    public ResponseEntity<List<Modulo>> listar() {
        return ResponseEntity.ok(moduloRepository.findAll());
    }
}