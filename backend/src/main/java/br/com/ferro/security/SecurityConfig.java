package br.com.ferro.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Ativa o CORS e desativa o CSRF para APIs REST stateless
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                // Sessão sem estado (Stateless) já que usamos Token/JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Libera requisições OPTIONS (Preflight do CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Libera todas as rotas de autenticação e API
                        .requestMatchers("/api/**").permitAll()
                        // Qualquer outra requisição precisa estar autenticada
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Libera QUALQUER origem, porta ou IP que tente acessar seu backend
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // Libera todos os métodos (GET, POST, etc) e todos os cabeçalhos (Headers)
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // IMPORTANTE: Como liberamos tudo via padrão (*), mude para false
        // para o Spring não exigir validação estrita de credenciais em desenvolvimento
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}