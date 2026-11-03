package br.com.fiap.library.infrastructure.documentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI libraryOpenApi() {
        return new OpenAPI()
                .addServersItem(new Server().url("/api"))
                .info(new Info()
                        .title("API de Biblioteca Online")
                        .version("1.0.0")
                        .description("""
                                Cadastro de livros e usuários, empréstimos, devoluções, reservas e relatórios."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
