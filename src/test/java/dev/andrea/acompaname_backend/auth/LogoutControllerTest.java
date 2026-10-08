package dev.andrea.acompaname_backend.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LogoutController.class)
@AutoConfigureMockMvc(addFilters = false)
public class LogoutControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLogoutInvalidaLaSesion() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/logout").session(session))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(204)));
        assertThat(session.isInvalid(), is(true));
    }

    @Test
    void testLogoutSinSesionPreviaDevuelve204() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/logout"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(204)));
    }
}