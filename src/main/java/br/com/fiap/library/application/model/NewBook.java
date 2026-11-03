package br.com.fiap.library.application.model;

import java.util.List;

public record NewBook(String title, String author, String isbn, String description, List<String> assetCodes) {
}
