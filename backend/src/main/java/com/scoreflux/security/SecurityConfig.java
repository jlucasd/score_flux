package com.scoreflux.security;

import com.scoreflux.domain.Empresa;
import com.scoreflux.domain.Usuario;
import com.scoreflux.repository.EmpresaRepository;
import com.scoreflux.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value(), "Não autenticado")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Cria o usuário administrador inicial quando não existe nenhum usuário. */
    @Bean
    public ApplicationRunner seedAdmin(UsuarioRepository usuarios, EmpresaRepository empresas,
                                       PasswordEncoder encoder) {
        return args -> {
            if (usuarios.count() > 0) return;
            Empresa empresa = empresas.findAll().stream().findFirst().orElse(null);
            if (empresa == null) return;
            Usuario admin = new Usuario();
            admin.setEmpresa(empresa);
            admin.setNome("Administrador");
            admin.setEmail("admin@scoreflux.com");
            admin.setSenhaHash(encoder.encode("admin123"));
            usuarios.save(admin);
            log.info("Usuário inicial criado: admin@scoreflux.com / admin123 — troque a senha");
        };
    }
}
