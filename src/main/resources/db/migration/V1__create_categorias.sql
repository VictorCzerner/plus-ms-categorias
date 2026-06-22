CREATE TABLE categorias (
                            id                BIGSERIAL       PRIMARY KEY,
                            nome              VARCHAR(80)     NOT NULL UNIQUE,
                            descricao         VARCHAR(255),
                            ativo             BOOLEAN         NOT NULL DEFAULT TRUE,
                            categoria_pai_id  BIGINT          REFERENCES categorias(id),
                            criado_em         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                            atualizado_em     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categorias_nome   ON categorias (LOWER(nome));
CREATE INDEX idx_categorias_ativo  ON categorias (ativo);
CREATE INDEX idx_categorias_pai    ON categorias (categoria_pai_id);
