package br.com.fiap.library.application.port.out;

import java.util.function.Supplier;

public interface UnitOfWork {
    <T> T inTransaction(Supplier<T> work);

    default void run(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
