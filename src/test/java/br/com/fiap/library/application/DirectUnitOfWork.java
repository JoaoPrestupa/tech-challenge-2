package br.com.fiap.library.application;

import br.com.fiap.library.application.port.out.UnitOfWork;
import java.util.function.Supplier;

public class DirectUnitOfWork implements UnitOfWork {
    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return work.get();
    }
}
