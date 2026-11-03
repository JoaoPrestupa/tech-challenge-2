package br.com.fiap.library.application.port.in;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.model.LoanFilter;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;

public interface LoanManagement {
    LoanDetails borrow(Long userId, Long bookCopyId);

    LoanDetails giveBack(Long loanId);

    LoanDetails get(Actor actor, Long loanId);

    PageResult<LoanDetails> search(LoanFilter filter, PageQuery page);

    PageResult<LoanDetails> historyOf(Actor actor, Long userId, PageQuery page);
}
