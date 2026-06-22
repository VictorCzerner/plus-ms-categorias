package com.pucrs.plus_ms_categorias.categoria.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record CategoriaPageResponse(
        List<CategoriaResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static CategoriaPageResponse from(Page<CategoriaResponse> pageResult) {
        return new CategoriaPageResponse(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}