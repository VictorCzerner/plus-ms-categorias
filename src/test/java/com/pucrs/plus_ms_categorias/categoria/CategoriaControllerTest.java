package com.pucrs.plus_ms_categorias.categoria;

import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaPageResponse;
import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaRequest;
import com.pucrs.plus_ms_categorias.categoria.dto.CategoriaResponse;
import com.pucrs.plus_ms_categorias.exception.ConflitoException;
import com.pucrs.plus_ms_categorias.exception.GlobalExceptionHandler;
import com.pucrs.plus_ms_categorias.exception.RecursoNaoEncontradoException;
import com.pucrs.plus_ms_categorias.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CategoriaControllerTest.MvcTestConfig.class,
        SecurityConfig.class
})
@WebAppConfiguration
@TestPropertySource(properties = "security.jwt.secret=categorias-controller-test-secret")
class CategoriaControllerTest {

    private static final String TEST_SECRET = "categorias-controller-test-secret";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CategoriaService categoriaService;

    private MockMvc mockMvc;
    private CategoriaResponse categoria;

    @BeforeEach
    void setUp() {
        reset(categoriaService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        categoria = new CategoriaResponse();
        categoria.setId(1L);
        categoria.setNome("Camisetas");
        categoria.setDescricao("Camisetas de manga curta e longa");
        categoria.setAtivo(true);
        categoria.setCriadoEm(OffsetDateTime.parse("2026-06-24T10:00:00-03:00"));
        categoria.setAtualizadoEm(OffsetDateTime.parse("2026-06-24T10:00:00-03:00"));
    }

    @Test
    void shouldReturnCategoriesWhenGetHasValidToken() throws Exception {
        when(categoriaService.listar(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new CategoriaPageResponse(List.of(categoria), 0, 20, 1, 1));

        mockMvc.perform(get("/categorias")
                        .header("Authorization", bearerToken("vendedor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Camisetas"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnUnauthorizedWhenGetHasNoToken() throws Exception {
        mockMvc.perform(get("/categorias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/categorias")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenHasInvalidSignature() throws Exception {
        mockMvc.perform(get("/categorias")
                        .header("Authorization", bearerToken("vendedor", "wrong-secret")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCategoryWhenIdExists() throws Exception {
        when(categoriaService.buscarPorId(1L)).thenReturn(categoria);

        mockMvc.perform(get("/categorias/1")
                        .header("Authorization", bearerToken("vendedor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Camisetas"));
    }

    @Test
    void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
        when(categoriaService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Categoria não encontrada com id: 99"));

        mockMvc.perform(get("/categorias/99")
                        .header("Authorization", bearerToken("vendedor")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Categoria não encontrada com id: 99"));
    }

    @Test
    void shouldCreateCategoryWhenAdminRequestIsValid() throws Exception {
        when(categoriaService.criar(any(CategoriaRequest.class))).thenReturn(categoria);

        mockMvc.perform(post("/categorias")
                        .header("Authorization", bearerToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/categorias/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturnForbiddenWhenSellerTriesToCreate() throws Exception {
        mockMvc.perform(post("/categorias")
                        .header("Authorization", bearerToken("vendedor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenAuthenticatedTokenHasNoRole() throws Exception {
        mockMvc.perform(post("/categorias")
                        .with(jwt().jwt(builder -> builder
                                .subject("usuario@example.com")
                                .claim("user_id", 3)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowGetWhenAuthenticatedTokenHasNoRole() throws Exception {
        when(categoriaService.listar(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new CategoriaPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/categorias")
                        .with(jwt().jwt(builder -> builder.subject("usuario@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnUnauthorizedWhenPostHasNoToken() throws Exception {
        mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnBadRequestWhenPostBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/categorias")
                        .header("Authorization", bearerToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnNotFoundWhenParentCategoryDoesNotExist() throws Exception {
        when(categoriaService.criar(any(CategoriaRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Categoria pai não encontrada com id: 99"));

        mockMvc.perform(post("/categorias")
                        .header("Authorization", bearerToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Manga Longa","categoriaPaiId":99}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnConflictWhenCategoryNameAlreadyExists() throws Exception {
        when(categoriaService.criar(any(CategoriaRequest.class)))
                .thenThrow(new ConflitoException("Já existe uma categoria com o nome 'Camisetas'."));

        mockMvc.perform(post("/categorias")
                        .header("Authorization", bearerToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldUpdateCategoryWhenAdminRequestIsValid() throws Exception {
        when(categoriaService.atualizar(anyLong(), any(CategoriaRequest.class))).thenReturn(categoria);

        mockMvc.perform(put("/categorias/1")
                        .header("Authorization", bearerToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturnForbiddenWhenSellerTriesToUpdate() throws Exception {
        mockMvc.perform(put("/categorias/1")
                        .header("Authorization", bearerToken("vendedor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestWhenPutBodyIsInvalid() throws Exception {
        mockMvc.perform(put("/categorias/1")
                        .header("Authorization", bearerToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteCategoryWhenAdminIsAuthorized() throws Exception {
        mockMvc.perform(delete("/categorias/1")
                        .header("Authorization", bearerToken("admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnForbiddenWhenSellerTriesToDelete() throws Exception {
        mockMvc.perform(delete("/categorias/1")
                        .header("Authorization", bearerToken("vendedor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingCategory() throws Exception {
        org.mockito.Mockito.doThrow(
                        new RecursoNaoEncontradoException("Categoria não encontrada com id: 99"))
                .when(categoriaService).remover(99L);

        mockMvc.perform(delete("/categorias/99")
                        .header("Authorization", bearerToken("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnMethodNotAllowedForDocumentedButMissingPatchEndpoint() throws Exception {
        mockMvc.perform(patch("/categorias/1")
                        .header("Authorization", bearerToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldCurrentlyAcceptRefreshLikeTokenAsAccessToken() throws Exception {
        when(categoriaService.listar(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new CategoriaPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/categorias")
                        .header("Authorization", bearerToken(
                                "vendedor",
                                TEST_SECRET,
                                Instant.now().plusSeconds(7 * 24 * 60 * 60).getEpochSecond())))
                .andExpect(status().isOk());
    }

    private String validRequest() {
        return """
                {"nome":"Camisetas","descricao":"Camisetas de manga curta e longa","ativo":true}
                """;
    }

    private String bearerToken(String role) throws Exception {
        return bearerToken(role, TEST_SECRET);
    }

    private String bearerToken(String role, String secret) throws Exception {
        return bearerToken(role, secret, Instant.now().plusSeconds(3600).getEpochSecond());
    }

    private String bearerToken(String role, String secret, long expiration) throws Exception {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("""
                {"sub":"usuario@example.com","user_id":1,"role":"%s","exp":%d}
                """.formatted(role, expiration));
        String content = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));

        return "Bearer " + content + "." + signature;
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Configuration
    @EnableWebMvc
    @Import({CategoriaController.class, GlobalExceptionHandler.class})
    static class MvcTestConfig {

        @Bean
        CategoriaService categoriaService() {
            return mock(CategoriaService.class);
        }
    }
}
