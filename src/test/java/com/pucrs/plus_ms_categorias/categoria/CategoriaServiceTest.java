package com.pucrs.plus_ms_categorias.categoria;

import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaPageResponse;
import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaRequest;
import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaResponse;
import com.pucrs.plus_ms_categorias.exception.ConflitoException;
import com.pucrs.plus_ms_categorias.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoriaExistente;

    @BeforeEach
    void setUp() {
        categoriaExistente = Categoria.builder()
                .id(1L)
                .nome("Camisetas")
                .descricao("Camisetas de manga curta e longa")
                .ativo(true)
                .categoriaPai(null)
                .criadoEm(OffsetDateTime.now())
                .atualizadoEm(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("deve retornar página com categorias")
        void deveRetornarPaginaComCategorias() {
            var page = new PageImpl<>(List.of(categoriaExistente));
            when(categoriaRepository.findAllWithFilters(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            CategoriaPageResponse response = categoriaService.listar(null, null, null, 0, 20);

            assertThat(response.content()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.content().get(0).getNome()).isEqualTo("Camisetas");
        }

        @Test
        @DisplayName("deve retornar página vazia quando não há categorias")
        void deveRetornarPaginaVazia() {
            var page = new PageImpl<Categoria>(List.of());
            when(categoriaRepository.findAllWithFilters(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            CategoriaPageResponse response = categoriaService.listar(null, null, null, 0, 20);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
        }

        @Test
        @DisplayName("deve rejeitar número de página negativo antes de consultar o repositório")
        void deveRejeitarPaginaNegativa() {
            assertThatThrownBy(() -> categoriaService.listar(null, null, null, -1, 20))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(categoriaRepository);
        }

        @Test
        @DisplayName("deve rejeitar tamanho de página igual a zero antes de consultar o repositório")
        void deveRejeitarTamanhoZero() {
            assertThatThrownBy(() -> categoriaService.listar(null, null, null, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(categoriaRepository);
        }
    }

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar categoria quando id existe")
        void deveRetornarCategoria() {
            when(categoriaRepository.findByIdWithPai(1L))
                    .thenReturn(Optional.of(categoriaExistente));

            CategoriaResponse response = categoriaService.buscarPorId(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getNome()).isEqualTo("Camisetas");
        }

        @Test
        @DisplayName("deve lançar RecursoNaoEncontradoException quando id não existe")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(categoriaRepository.findByIdWithPai(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.buscarPorId(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("deve criar categoria raiz com sucesso")
        void deveCriarCategoriaRaiz() {
            var request = new CategoriaRequest();
            request.setNome("Calças");
            request.setDescricao("Calças jeans e sociais");
            request.setAtivo(true);
            request.setCategoriaPaiId(null);

            when(categoriaRepository.existsByNome("Calças")).thenReturn(false);
            when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
                Categoria c = inv.getArgument(0);
                c = Categoria.builder()
                        .id(2L)
                        .nome(c.getNome())
                        .descricao(c.getDescricao())
                        .ativo(c.getAtivo())
                        .criadoEm(OffsetDateTime.now())
                        .atualizadoEm(OffsetDateTime.now())
                        .build();
                return c;
            });

            CategoriaResponse response = categoriaService.criar(request);

            assertThat(response.getNome()).isEqualTo("Calças");
            assertThat(response.getCategoriaPaiId()).isNull();
            verify(categoriaRepository).save(any(Categoria.class));
        }

        @Test
        @DisplayName("deve criar subcategoria com categoria pai existente")
        void deveCriarSubcategoria() {
            var request = new CategoriaRequest();
            request.setNome("Camisetas Manga Longa");
            request.setCategoriaPaiId(1L);

            when(categoriaRepository.existsByNome("Camisetas Manga Longa")).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
                Categoria c = inv.getArgument(0);
                return Categoria.builder()
                        .id(3L)
                        .nome(c.getNome())
                        .ativo(true)
                        .categoriaPai(categoriaExistente)
                        .criadoEm(OffsetDateTime.now())
                        .atualizadoEm(OffsetDateTime.now())
                        .build();
            });

            CategoriaResponse response = categoriaService.criar(request);

            assertThat(response.getCategoriaPaiId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("deve lançar ConflitoException quando nome já existe")
        void deveLancarExcecaoQuandoNomeDuplicado() {
            var request = new CategoriaRequest();
            request.setNome("Camisetas");

            when(categoriaRepository.existsByNome("Camisetas")).thenReturn(true);

            assertThatThrownBy(() -> categoriaService.criar(request))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("Camisetas");

            verify(categoriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar RecursoNaoEncontradoException quando categoria pai não existe")
        void deveLancarExcecaoQuandoCategoriaPaiNaoExiste() {
            var request = new CategoriaRequest();
            request.setNome("Subcategoria");
            request.setCategoriaPaiId(99L);

            when(categoriaRepository.existsByNome("Subcategoria")).thenReturn(false);
            when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.criar(request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(categoriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve assumir ativo=true quando não informado")
        void deveAssumirAtivoTrueQuandoNaoInformado() {
            var request = new CategoriaRequest();
            request.setNome("Nova Categoria");
            request.setAtivo(null);

            when(categoriaRepository.existsByNome(any())).thenReturn(false);
            when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
                Categoria c = inv.getArgument(0);
                return Categoria.builder()
                        .id(4L)
                        .nome(c.getNome())
                        .ativo(c.getAtivo())
                        .criadoEm(OffsetDateTime.now())
                        .atualizadoEm(OffsetDateTime.now())
                        .build();
            });

            CategoriaResponse response = categoriaService.criar(request);

            assertThat(response.getAtivo()).isTrue();
        }
    }

    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("deve atualizar categoria com sucesso")
        void deveAtualizarCategoriaComSucesso() {
            var request = new CategoriaRequest();
            request.setNome("Camisetas Atualizadas");
            request.setDescricao("Nova descrição");
            request.setAtivo(false);

            when(categoriaRepository.findByIdWithPai(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByNomeAndIdNot("Camisetas Atualizadas", 1L)).thenReturn(false);
            when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

            CategoriaResponse response = categoriaService.atualizar(1L, request);

            assertThat(response.getNome()).isEqualTo("Camisetas Atualizadas");
            assertThat(response.getDescricao()).isEqualTo("Nova descrição");
            assertThat(response.getAtivo()).isFalse();
            verify(categoriaRepository).save(categoriaExistente);
        }

        @Test
        @DisplayName("deve lançar RecursoNaoEncontradoException quando categoria não existe")
        void deveLancarExcecaoQuandoCategoriaNaoExiste() {
            var request = new CategoriaRequest();
            request.setNome("Novo Nome");

            when(categoriaRepository.findByIdWithPai(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.atualizar(99L, request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("deve lançar ConflitoException quando nome já existe em outra categoria")
        void deveLancarExcecaoQuandoNomeDuplicadoEmOutraCategoria() {
            var request = new CategoriaRequest();
            request.setNome("Camisetas");

            when(categoriaRepository.findByIdWithPai(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByNomeAndIdNot("Camisetas", 1L)).thenReturn(true);

            assertThatThrownBy(() -> categoriaService.atualizar(1L, request))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("Camisetas");

            verify(categoriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar ConflitoException quando categoria tenta ser pai dela mesma")
        void deveLancarExcecaoQuandoCategoriaForPaiDelaMesma() {
            var request = new CategoriaRequest();
            request.setNome("Camisetas");
            request.setCategoriaPaiId(1L);

            when(categoriaRepository.findByIdWithPai(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByNomeAndIdNot("Camisetas", 1L)).thenReturn(false);

            assertThatThrownBy(() -> categoriaService.atualizar(1L, request))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("pai dela mesma");

            verify(categoriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar RecursoNaoEncontradoException quando nova categoria pai não existe")
        void deveLancarExcecaoQuandoNovaCategoriaPaiNaoExiste() {
            var request = new CategoriaRequest();
            request.setNome("Camisetas");
            request.setCategoriaPaiId(99L);

            when(categoriaRepository.findByIdWithPai(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByNomeAndIdNot("Camisetas", 1L)).thenReturn(false);
            when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.atualizar(1L, request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(categoriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve assumir ativo true na atualização quando valor é nulo")
        void deveAssumirAtivoTrueNaAtualizacaoQuandoNulo() {
            var request = new CategoriaRequest();
            request.setNome("Camisetas");
            request.setAtivo(null);

            categoriaExistente.setAtivo(false);
            when(categoriaRepository.findByIdWithPai(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByNomeAndIdNot("Camisetas", 1L)).thenReturn(false);
            when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

            CategoriaResponse response = categoriaService.atualizar(1L, request);

            assertThat(response.getAtivo()).isTrue();
        }

        @Test
        @DisplayName("documenta comportamento atual que permite ciclo indireto na hierarquia")
        void devePermitirCicloIndiretoNoComportamentoAtual() {
            Categoria categoriaFilha = Categoria.builder()
                    .id(2L)
                    .nome("Manga Longa")
                    .ativo(true)
                    .categoriaPai(categoriaExistente)
                    .build();
            var request = new CategoriaRequest();
            request.setNome("Camisetas");
            request.setCategoriaPaiId(2L);

            when(categoriaRepository.findByIdWithPai(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByNomeAndIdNot("Camisetas", 1L)).thenReturn(false);
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoriaFilha));
            when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

            CategoriaResponse response = categoriaService.atualizar(1L, request);

            assertThat(response.getCategoriaPaiId()).isEqualTo(2L);
            assertThat(categoriaFilha.getCategoriaPai()).isSameAs(categoriaExistente);
        }
    }

    @Nested
    @DisplayName("remover()")
    class Remover {

        @Test
        @DisplayName("deve remover categoria sem subcategorias")
        void deveRemoverCategoriaSemSubcategorias() {
            when(categoriaRepository.findByIdWithPai(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByCategoriaPaiId(1L)).thenReturn(false);

            categoriaService.remover(1L);

            verify(categoriaRepository).delete(categoriaExistente);
        }

        @Test
        @DisplayName("deve lançar ConflitoException quando categoria possui subcategorias")
        void deveLancarExcecaoQuandoCategoriaPossuiSubcategorias() {
            when(categoriaRepository.findByIdWithPai(1L)).thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByCategoriaPaiId(1L)).thenReturn(true);

            assertThatThrownBy(() -> categoriaService.remover(1L))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("subcategorias");

            verify(categoriaRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lançar RecursoNaoEncontradoException ao remover categoria inexistente")
        void deveLancarExcecaoAoRemoverCategoriaInexistente() {
            when(categoriaRepository.findByIdWithPai(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoriaService.remover(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(categoriaRepository, never()).delete(any());
        }
    }
}
