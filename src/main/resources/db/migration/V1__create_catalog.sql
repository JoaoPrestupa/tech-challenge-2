CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Sequências com incremento 50 permitem inserções em lote via JDBC batch no Hibernate.
CREATE SEQUENCE books_seq INCREMENT BY 50;
CREATE SEQUENCE book_copies_seq INCREMENT BY 50;

CREATE TABLE books (
    id          BIGINT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    author      VARCHAR(255) NOT NULL,
    isbn        VARCHAR(13)  NOT NULL,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_books_isbn UNIQUE (isbn),
    CONSTRAINT ck_books_isbn_length CHECK (length(isbn) IN (10, 13))
);

CREATE TABLE book_copies (
    id         BIGINT PRIMARY KEY,
    book_id    BIGINT      NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    asset_code VARCHAR(60) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT uk_book_copies_asset_code UNIQUE (asset_code),
    CONSTRAINT ck_book_copies_status CHECK (status IN ('AVAILABLE', 'LOANED', 'RESERVED', 'INACTIVE'))
);

-- Busca parcial por título e autor sem varredura completa da tabela.
CREATE INDEX ix_books_title_trgm ON books USING gin (lower(title) gin_trgm_ops);
CREATE INDEX ix_books_author_trgm ON books USING gin (lower(author) gin_trgm_ops);
CREATE INDEX ix_book_copies_book_status ON book_copies (book_id, status);
