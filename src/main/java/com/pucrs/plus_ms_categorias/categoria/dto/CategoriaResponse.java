package com.pucrs.plus_ms_categorias.categoria.dto;

import com.pucrs.plus_ms_categorias.categoria.Categoria;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CategoriaResponse {

    private Long id;
    private String nome;
    private String descricao;
    private Boolean ativo;
    private Long categoriaPaiId;
    private OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;

    public static CategoriaResponse from(Categoria categoria) {
        CategoriaResponse response = new CategoriaResponse();
        response.setId(categoria.getId());
        response.setNome(categoria.getNome());
        response.setDescricao(categoria.getDescricao());
        response.setAtivo(categoria.getAtivo());
        response.setCategoriaPaiId(
                categoria.getCategoriaPai() != null ? categoria.getCategoriaPai().getId() : null
        );
        response.setCriadoEm(categoria.getCriadoEm());
        response.setAtualizadoEm(categoria.getAtualizadoEm());
        return response;
    }
}