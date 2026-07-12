package com.scoreflux.api;

import com.scoreflux.domain.Usuario;
import com.scoreflux.repository.UsuarioRepository;
import com.scoreflux.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record LoginResponse(String token, String nome, String email) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<Usuario> usuario = usuarios.findByEmailIgnoreCaseAndAtivoTrue(request.email());
        if (usuario.isEmpty() || !encoder.matches(request.senha(), usuario.get().getSenhaHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "E-mail ou senha inválidos"));
        }
        Usuario u = usuario.get();
        return ResponseEntity.ok(new LoginResponse(jwtService.gerarToken(u.getEmail()), u.getNome(), u.getEmail()));
    }
}
