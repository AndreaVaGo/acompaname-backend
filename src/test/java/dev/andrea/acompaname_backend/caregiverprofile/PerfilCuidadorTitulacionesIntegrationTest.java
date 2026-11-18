package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import jakarta.persistence.EntityManager;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;

@DataJpaTest
public class PerfilCuidadorTitulacionesIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private PerfilCuidadorRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity crearUsuario(String email) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre("Pepe")
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .build();
        return usuarioRepository.saveAndFlush(usuario);
    }

    @Test
    void testGuardaYLeeLasTitulacionesYLasOtras() {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .titulaciones("ENFERMERIA,PRIMEROS_AUXILIOS")
                .otrasTitulaciones("Curso de geriatría")
                .usuario(crearUsuario("titulaciones@test.com"))
                .build();

        PerfilCuidadorEntity guardado = repository.saveAndFlush(perfil);
        entityManager.clear();

        PerfilCuidadorEntity leido = repository.findById(guardado.getId()).get();
        assertThat(leido.getTitulaciones(), is(equalTo("ENFERMERIA,PRIMEROS_AUXILIOS")));
        assertThat(leido.getOtrasTitulaciones(), is(equalTo("Curso de geriatría")));
    }

    @Test
    void testUnPerfilSinTitulacionesLasGuardaComoNull() {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(crearUsuario("sintitulaciones@test.com"))
                .build();

        PerfilCuidadorEntity guardado = repository.saveAndFlush(perfil);
        entityManager.clear();

        PerfilCuidadorEntity leido = repository.findById(guardado.getId()).get();
        assertThat(leido.getTitulaciones(), is(nullValue()));
        assertThat(leido.getOtrasTitulaciones(), is(nullValue()));
    }
}
