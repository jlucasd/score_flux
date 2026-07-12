package com.scoreflux.api;

import com.scoreflux.domain.Empresa;
import com.scoreflux.domain.Usuario;
import com.scoreflux.repository.EmpresaRepository;
import com.scoreflux.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final PasswordEncoder encoder;

    public UsuarioController(UsuarioRepository usuarios, EmpresaRepository empresas, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.encoder = encoder;
    }

    public record UsuarioRequest(@NotBlank String nome,
                                 @NotBlank @Email String email,
                                 @NotBlank @Size(min = 6, message = "senha deve ter ao menos 6 caracteres") String senha) {
    }

    public record UsuarioResponse(Long id, String nome, String email, boolean ativo, LocalDateTime criadoEm) {
        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.isAtivo(), u.getCriadoEm());
        }
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarios.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest request) {
        if (usuarios.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Já existe usuário com o e-mail " + request.email());
        }
        Empresa empresa = empresas.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma empresa cadastrada"));
        Usuario usuario = new Usuario();
        usuario.setEmpresa(empresa);
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenhaHash(encoder.encode(request.senha()));
        return UsuarioResponse.de(usuarios.save(usuario));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, Authentication auth) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Usuário não encontrado: " + id));
        if (usuario.getEmail().equalsIgnoreCase((String) auth.getPrincipal())) {
            throw new IllegalArgumentException("Você não pode excluir o próprio usuário logado");
        }
        usuarios.delete(usuario);
    }
}
