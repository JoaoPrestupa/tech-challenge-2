package br.com.fiap.library.application.model;

import java.time.Instant;

public record LoanDetails(Long id, Long userId, String userName, Long bookCopyId, String assetCode, Long bookId,
                          String bookTitle, Instant borrowedAt, Instant dueAt, Instant returnedAt) {
}
