package com.pucrs.plus_ms_categorias.categoria.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CategoriaRequest {

    @NotBlank(message = "O campo 'nome' é obrigatório.")
    @Size(min = 2, max = 80, message = "O campo 'nome' deve ter entre 2 e 80 caracteres.")
    private String nome;

    @Size(max = 255, message = "O campo 'descricao' deve ter no máximo 255 caracteres.")
    private String descricao;

    private Boolean ativo = true;

    private Long categoriaPaiId;
}
