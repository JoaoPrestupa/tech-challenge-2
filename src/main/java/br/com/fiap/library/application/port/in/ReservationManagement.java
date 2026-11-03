package br.com.fiap.library.application.port.in;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.model.ReservationView;

public interface ReservationManagement {
    ReservationView create(Actor actor, Long userId, Long bookId);

    ReservationView get(Actor actor, Long id);

    void cancel(Actor actor, Long id);

    PageResult<ReservationView> listByUser(Actor actor, Long userId, PageQuery page);

    int expireOverdue();
}
