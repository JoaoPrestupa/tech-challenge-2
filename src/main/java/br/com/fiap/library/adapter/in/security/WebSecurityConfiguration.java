package br.com.fiap.library.adapter.in.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfiguration {
    private static final String LIBRARIAN = "ROLE_LIBRARIAN";

    /**
     * Tudo é protegido por padrão e as rotas públicas são liberadas uma a uma. A autorização por
     * objeto (membro só acessa os próprios dados) fica nos casos de uso, por meio de Actor.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh", "/auth/logout")
                        .permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/books/*/copies").hasAuthority(LIBRARIAN)
                        .requestMatchers(HttpMethod.GET, "/books", "/books/*").authenticated()
                        .requestMatchers("/books/**").hasAuthority(LIBRARIAN)
                        .requestMatchers("/reports/**").hasAuthority(LIBRARIAN)
                        .requestMatchers(HttpMethod.GET, "/users").hasAuthority(LIBRARIAN)
                        .requestMatchers(HttpMethod.POST, "/users").hasAuthority(LIBRARIAN)
                        .requestMatchers(HttpMethod.DELETE, "/users/*").hasAuthority(LIBRARIAN)
                        .requestMatchers(HttpMethod.POST, "/loans", "/loans/*/return").hasAuthority(LIBRARIAN)
                        .requestMatchers(HttpMethod.GET, "/loans").hasAuthority(LIBRARIAN)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, e) -> writeProblem(response,
                                HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado",
                                "Informe um token de acesso válido"))
                        .accessDeniedHandler((request, response, e) -> writeProblem(response,
                                HttpServletResponse.SC_FORBIDDEN, "Acesso negado",
                                "Você não tem permissão para esta operação")))
                .build();
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static void writeProblem(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"title":"%s","status":%d,"detail":"%s"}""".formatted(title, status, detail));
    }
}
