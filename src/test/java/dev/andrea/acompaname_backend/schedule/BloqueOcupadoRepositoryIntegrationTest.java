package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba contra un MySQL real que cada persona cuidadora solo ve sus bloques y en orden.
@DataJpaTest
public class BloqueOcupadoRepositoryIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private BloqueOcupadoRepository repository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private PerfilCuidadorEntity crearCuidador(String email) {
        UsuarioEntity usuario = usuarioRepository.saveAndFlush(UsuarioEntity.builder()
                .nombre("Cuidador")
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .build());
        return perfilRepository.saveAndFlush(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(usuario)
                .build());
    }

    private void crearBloque(PerfilCuidadorEntity cuidador, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        repository.saveAndFlush(BloqueOcupadoEntity.builder()
                .cuidador(cuidador)
                .fecha(fecha)
                .horaInicio(inicio)
                .horaFin(fin)
                .motivo("Turno")
                .build());
    }

    @Test
    void testDevuelveSoloLosBloquesDeEsaPersonaCuidadora() {
        PerfilCuidadorEntity pepe = crearCuidador("pepe.bloques@test.com");
        PerfilCuidadorEntity lucia = crearCuidador("lucia.bloques@test.com");
        crearBloque(pepe, LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), LocalTime.of(15, 0));
        crearBloque(lucia, LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), LocalTime.of(15, 0));

        List<BloqueOcupadoEntity> deLucia = repository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(lucia.getId());

        assertThat(deLucia.size(), is(equalTo(1)));
        assertThat(deLucia.get(0).getCuidador().getId(), is(equalTo(lucia.getId())));
    }

    @Test
    void testLosDevuelvePorFechaYDespuesPorHora() {
        PerfilCuidadorEntity pepe = crearCuidador("pepe.orden@test.com");
        crearBloque(pepe, LocalDate.of(2026, 12, 3), LocalTime.of(8, 0), LocalTime.of(9, 0));
        crearBloque(pepe, LocalDate.of(2026, 12, 1), LocalTime.of(16, 0), LocalTime.of(18, 0));
        crearBloque(pepe, LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), LocalTime.of(10, 0));

        List<BloqueOcupadoEntity> bloques = repository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(pepe.getId());

        assertThat(bloques.get(0).getFecha(), is(equalTo(LocalDate.of(2026, 12, 1))));
        assertThat(bloques.get(0).getHoraInicio(), is(equalTo(LocalTime.of(8, 0))));
        assertThat(bloques.get(1).getHoraInicio(), is(equalTo(LocalTime.of(16, 0))));
        assertThat(bloques.get(2).getFecha(), is(equalTo(LocalDate.of(2026, 12, 3))));
    }

    @Test
    void testSinBloquesDevuelveListaVacia() {
        PerfilCuidadorEntity pepe = crearCuidador("pepe.vacio@test.com");

        assertThat(repository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(pepe.getId()).isEmpty(), is(true));
    }
}
