package br.com.fiap.library.application.model;

public record CopyCount(long total, long available) {
    public static final CopyCount NONE = new CopyCount(0, 0);
}
