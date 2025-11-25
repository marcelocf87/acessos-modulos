package com.supera.acessos.auth.service;

import com.supera.acessos.auth.dto.LoginRequestDTO;
import com.supera.acessos.auth.dto.LoginResponseDTO;
import com.supera.acessos.auth.security.JwtTokenService;
import com.supera.acessos.usuario.entity.Usuario;
import com.supera.acessos.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioService usuarioService;
    private final JwtTokenService jwtTokenService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponseDTO login(LoginRequestDTO dto) {

        //buscar usuario pelo e-mail
        Usuario usuario = usuarioService.buscarPorEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Usuário ou senha inválidos"));

        //validara senha
        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new RuntimeException("Usuário ou senha inválidos");
        }

        //gerar token
        String token = jwtTokenService.gerarToken(usuario);

        //retornar resposta com token tempo
        return new LoginResponseDTO(
                token,
                jwtTokenService.getExpiracao()
        );
    }
}
