package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.port.out.UnitOfWork;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class SpringUnitOfWork implements UnitOfWork {
    private final TransactionTemplate template;

    SpringUnitOfWork(PlatformTransactionManager manager) {
        this.template = new TransactionTemplate(manager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
