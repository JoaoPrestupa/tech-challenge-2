package br.com.fiap.library.infrastructure.configuration;

import br.com.fiap.library.application.port.in.UserManagement;
import br.com.fiap.library.domain.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class LibrarianBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LibrarianBootstrap.class);

    private final UserManagement users;
    private final LibraryProperties.Bootstrap config;

    LibrarianBootstrap(UserManagement users, LibraryProperties properties) {
        this.users = users;
        this.config = properties.bootstrap();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (config == null || !config.isConfigured()) {
            return;
        }
        try {
            String name = config.name() == null || config.name().isBlank() ? "Bibliotecário" : config.name();
            if (users.bootstrapLibrarian(name, config.email(), config.password())) {
                log.info("Primeiro bibliotecário criado");
            }
        } catch (ConflictException | DataIntegrityViolationException alreadyCreatedByAnotherReplica) {
            log.info("Bibliotecário inicial já foi criado por outra réplica");
        }
    }
}
