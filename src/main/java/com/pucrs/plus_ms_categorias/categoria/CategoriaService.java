package com.pucrs.plus_ms_categorias.categoria;


import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaPageResponse;
import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaRequest;
import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaResponse;
import com.pucrs.plus_ms_categorias.exception.ConflitoException;
import com.pucrs.plus_ms_categorias.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public CategoriaPageResponse listar(String nome, Boolean ativo, Long categoriaPaiId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        var resultado = categoriaRepository
                .findAllWithFilters(nome, ativo, categoriaPaiId, pageable)
                .map(CategoriaResponse::from);
        return CategoriaPageResponse.from(resultado);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return categoriaRepository.findByIdWithPai(id)
                .map(CategoriaResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada com id: " + id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        if (categoriaRepository.existsByNome(request.getNome())) {
            throw new ConflitoException("Já existe uma categoria com o nome '" + request.getNome() + "'.");
        }

        Categoria categoria = Categoria.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .ativo(request.getAtivo() != null ? request.getAtivo() : true)
                .categoriaPai(resolverCategoriaPai(request.getCategoriaPaiId()))
                .build();

        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidadePorId(id);

        if (categoriaRepository.existsByNomeAndIdNot(request.getNome(), id)) {
            throw new ConflitoException("Já existe uma categoria com o nome '" + request.getNome() + "'.");
        }

        categoria.setNome(request.getNome());
        categoria.setDescricao(request.getDescricao());
        categoria.setAtivo(request.getAtivo() != null ? request.getAtivo() : true);
        categoria.setCategoriaPai(resolverCategoriaPai(request.getCategoriaPaiId(), id));

        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public void remover(Long id) {
        Categoria categoria = buscarEntidadePorId(id);

        if (categoriaRepository.existsByCategoriaPaiId(id)) {
            throw new ConflitoException("Não é possível remover categoria com subcategorias.");
        }

        categoriaRepository.delete(categoria);
    }

    private Categoria buscarEntidadePorId(Long id) {
        return categoriaRepository.findByIdWithPai(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada com id: " + id));
    }

    private Categoria resolverCategoriaPai(Long categoriaPaiId) {
        return resolverCategoriaPai(categoriaPaiId, null);
    }

    private Categoria resolverCategoriaPai(Long categoriaPaiId, Long categoriaId) {
        if (categoriaPaiId == null) return null;
        if (categoriaPaiId.equals(categoriaId)) {
            throw new ConflitoException("Uma categoria não pode ser pai dela mesma.");
        }
        return categoriaRepository.findById(categoriaPaiId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria pai não encontrada com id: " + categoriaPaiId
                ));
    }
}
