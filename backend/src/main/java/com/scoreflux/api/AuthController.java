package com.scoreflux.api;

import com.scoreflux.domain.Usuario;
import com.scoreflux.repository.UsuarioRepository;
import com.scoreflux.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;

    @Value("${scoreflux.mail.remetente:noreply@scoreflux.com}")
    private String remetente;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder,
                          JwtService jwtService, JavaMailSender mailSender) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.mailSender = mailSender;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha, boolean lembrar) {}
    public record LoginResponse(String token, String nome, String email, String perfil) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<Usuario> usuario = usuarios.findByEmailIgnoreCaseAndAtivoTrue(request.email());
        if (usuario.isEmpty() || !encoder.matches(request.senha(), usuario.get().getSenhaHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "E-mail ou senha inválidos"));
        }
        Usuario u = usuario.get();
        return ResponseEntity.ok(new LoginResponse(
                jwtService.gerarToken(u.getEmail(), request.lembrar()), u.getNome(), u.getEmail(), u.getPerfil()));
    }

    public record ResetRequest(@NotBlank @Email String email) {}

    @PostMapping("/reset-senha")
    public ResponseEntity<?> resetSenha(@Valid @RequestBody ResetRequest request) {
        Optional<Usuario> opt = usuarios.findByEmailIgnoreCaseAndAtivoTrue(request.email());
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("mensagem", "Se o e-mail existir, uma nova senha será enviada."));
        }
        Usuario u = opt.get();
        String novaSenha = gerarSenhaAleatoria();
        u.setSenhaHash(encoder.encode(novaSenha));
        usuarios.save(u);

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remetente);
            msg.setTo(u.getEmail());
            msg.setSubject("ScoreFlux — Nova senha de acesso");
            msg.setText("Olá " + u.getNome() + ",\n\n"
                    + "Sua senha foi redefinida. Use a nova senha abaixo para acessar o sistema:\n\n"
                    + "Senha: " + novaSenha + "\n\n"
                    + "Recomendamos que altere sua senha após o login.\n\n"
                    + "— ScoreFlux");
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de reset para {}: {}", u.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Nova senha gerada, mas falha ao enviar e-mail. Contate o administrador."));
        }

        return ResponseEntity.ok(Map.of("mensagem", "Se o e-mail existir, uma nova senha será enviada."));
    }

    public record AlterarSenhaRequest(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 6, message = "A nova senha deve ter ao menos 6 caracteres") String novaSenha) {}

    @PostMapping("/alterar-senha")
    public ResponseEntity<?> alterarSenha(@Valid @RequestBody AlterarSenhaRequest request,
                                          Authentication auth) {
        String email = (String) auth.getPrincipal();
        Usuario u = usuarios.findByEmailIgnoreCaseAndAtivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));

        if (!encoder.matches(request.senhaAtual(), u.getSenhaHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Senha atual incorreta"));
        }

        u.setSenhaHash(encoder.encode(request.novaSenha()));
        usuarios.save(u);
        return ResponseEntity.ok(Map.of("mensagem", "Senha alterada com sucesso"));
    }

    private String gerarSenhaAleatoria() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }
}
