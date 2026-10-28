package dev.andrea.acompaname_backend.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.user.dtos.ActualizarContactoDTO;
import dev.andrea.acompaname_backend.user.dtos.CambiarPasswordDTO;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UsuarioControllerTest {

    // Contraseña que cumple la regla @PasswordSegura. Los tests que esperan un 400 por
    // otro motivo (email, nombre, teléfono...) la usan para que el único fallo posible
    // sea el que están comprobando.
    private static final String PASSWORD_VALIDA = "Segura123!";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UsuarioService service;
    @Autowired
    ObjectMapper mapper;

    @Test
    void testIndex() throws Exception {
        UsuarioDTOResponse usuario = new UsuarioDTOResponse(1L, "Juan", "juan@test.com", "600111222",
                Set.of("FAMILIA"));
        List<UsuarioDTOResponse> usuarios = new ArrayList<>();
        usuarios.add(usuario);
        String json = mapper.writeValueAsString(usuarios);
        when(service.getEntities()).thenReturn(usuarios);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Juan"));
    }

    @Test
    void testGetById() throws Exception {
        UsuarioDTOResponse usuario = new UsuarioDTOResponse(1L, "Juan", "juan@test.com", "600111222",
                Set.of("FAMILIA"));
        String json = mapper.writeValueAsString(usuario);
        when(service.getById(1L)).thenReturn(usuario);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/usuarios/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Juan"));
    }

    @Test
    void testStore() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana@test.com", "600555666", PASSWORD_VALIDA,
                Set.of(1L));
        UsuarioDTOResponse dtoResponse = new UsuarioDTOResponse(1L, "Ana", "ana@test.com", "600555666",
                Set.of("FAMILIA"));
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.storeEntity(Mockito.any(UsuarioDTORequest.class))).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Ana"));
    }

    @Test
    void testDelete() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/usuarios/1"))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).deleteById(1L);
    }

    @Test
    void testStoreConEmailInvalidoDevuelve400() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "esto-no-es-un-email", "600555666", PASSWORD_VALIDA,
                Set.of(1L));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreConPasswordCortaDevuelve400() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana@test.com", "600555666", "1234567", Set.of(1L));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    // Una contraseña de 8 caracteres o más pero débil (solo minúsculas y números,
    // sin mayúscula ni carácter especial) también se rechaza.
    @Test
    void testStoreConPasswordSinMayusculaNiSimboloDevuelve400() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana@test.com", "600555666", "contraseña123",
                Set.of(1L));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreConNombreVacioDevuelve400() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("", "ana@test.com", "600555666", PASSWORD_VALIDA, Set.of(1L));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreSinTelefonoDevuelve400() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana@test.com", "", PASSWORD_VALIDA, Set.of(1L));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testPutUsuariosYaNoExisteDevuelve405() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana@test.com", "600555666", PASSWORD_VALIDA,
                Set.of(1L));

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/usuarios/1")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(405)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testCambiarPasswordDevuelve204() throws Exception {
        CambiarPasswordDTO dto = new CambiarPasswordDTO("actual1234", PASSWORD_VALIDA);

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/usuarios/1/password")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).cambiarPassword(Mockito.eq(1L), Mockito.any(CambiarPasswordDTO.class));
    }

    // Decisión de diseño: la contraseña ACTUAL puede ser débil (cuentas antiguas), así que
    // no se le exige la regla. Este test deja esa decisión documentada.
    @Test
    void testCambiarPasswordConPasswordActualDebilDevuelve204() throws Exception {
        CambiarPasswordDTO dto = new CambiarPasswordDTO("1234", PASSWORD_VALIDA);

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/usuarios/1/password")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).cambiarPassword(Mockito.eq(1L), Mockito.any(CambiarPasswordDTO.class));
    }

    @Test
    void testCambiarPasswordConPasswordNuevaCortaDevuelve400() throws Exception {
        CambiarPasswordDTO dto = new CambiarPasswordDTO("actual1234", "corta");

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/usuarios/1/password")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    // Contraseña nueva larga pero débil (sin mayúscula ni carácter especial): se rechaza.
    @Test
    void testCambiarPasswordConPasswordNuevaDebilDevuelve400() throws Exception {
        CambiarPasswordDTO dto = new CambiarPasswordDTO("actual1234", "nueva12345");

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/usuarios/1/password")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testCambiarPasswordSinPasswordActualDevuelve400() throws Exception {
        CambiarPasswordDTO dto = new CambiarPasswordDTO("", PASSWORD_VALIDA);

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/usuarios/1/password")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testActualizarContactoDevuelve200() throws Exception {
        ActualizarContactoDTO dto = new ActualizarContactoDTO("Juan Nuevo", "nuevo@test.com", "699000111");
        UsuarioDTOResponse dtoResponse = new UsuarioDTOResponse(1L, "Juan Nuevo", "nuevo@test.com", "699000111",
                Set.of("FAMILIA"));
        when(service.actualizarContacto(Mockito.eq(1L), Mockito.any(ActualizarContactoDTO.class)))
                .thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/usuarios/1/contacto")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(dtoResponse))));
        assertThat(response.getContentAsString(), containsString("Juan Nuevo"));
    }

    @Test
    void testActualizarContactoConEmailInvalidoDevuelve400() throws Exception {
        ActualizarContactoDTO dto = new ActualizarContactoDTO("Juan", "esto-no-es-un-email", "600111222");

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/usuarios/1/contacto")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testActualizarContactoConNombreVacioDevuelve400() throws Exception {
        ActualizarContactoDTO dto = new ActualizarContactoDTO("", "juan@test.com", "600111222");

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/usuarios/1/contacto")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }
}