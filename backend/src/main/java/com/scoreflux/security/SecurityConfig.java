package com.scoreflux.security;

import com.scoreflux.domain.Empresa;
import com.scoreflux.domain.Usuario;
import com.scoreflux.repository.EmpresaRepository;
import com.scoreflux.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // Origens liberadas para CORS (ex.: https://scoreflux.vercel.app). Vírgula-separado;
    // "*" (padrão) libera qualquer origem — seguro aqui pois a auth é por Bearer token, não cookie.
    @Value("${scoreflux.cors.origins:*}")
    private String corsOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value(), "Não autenticado")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origens = List.of(corsOrigins.split("\\s*,\\s*"));
        if (origens.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(origens);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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
