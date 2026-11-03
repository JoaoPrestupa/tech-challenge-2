package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.domain.exception.InvalidDataException;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class Pages {
    private Pages() {
    }

    /** A ordenação só aceita campos da lista: o nome vindo do cliente nunca chega direto à consulta. */
    static Pageable of(PageQuery query, Map<String, String> sortable, String defaultSort) {
        String requested = query.sort() == null || query.sort().isBlank() ? defaultSort : query.sort();
        String property = sortable.get(requested);
        if (property == null) {
            throw new InvalidDataException("Ordenação inválida. Use: " + String.join(", ", sortable.keySet()));
        }
        Sort.Direction direction = query.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(query.page(), query.size(), Sort.by(direction, property).and(Sort.by("id")));
    }

    static <T> PageResult<T> result(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
