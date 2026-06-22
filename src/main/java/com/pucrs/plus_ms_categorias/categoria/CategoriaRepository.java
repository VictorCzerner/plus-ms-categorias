package com.pucrs.plus_ms_categorias.categoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, Long id);

    boolean existsByCategoriaPaiId(Long categoriaPaiId);

    @Query("""
        SELECT c FROM Categoria c
        WHERE (:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
          AND (:ativo IS NULL OR c.ativo = :ativo)
          AND (:categoriaPaiId IS NULL OR c.categoriaPai.id = :categoriaPaiId)
        """)
    Page<Categoria> findAllWithFilters(
            @Param("nome") String nome,
            @Param("ativo") Boolean ativo,
            @Param("categoriaPaiId") Long categoriaPaiId,
            Pageable pageable
    );

    @Query("SELECT c FROM Categoria c LEFT JOIN FETCH c.categoriaPai WHERE c.id = :id")
    Optional<Categoria> findByIdWithPai(@Param("id") Long id);
}