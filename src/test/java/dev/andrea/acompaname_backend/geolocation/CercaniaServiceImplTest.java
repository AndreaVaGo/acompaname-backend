package dev.andrea.acompaname_backend.geolocation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.geolocation.dtos.CercaniaDTOResponse;

@ExtendWith(MockitoExtension.class)
public class CercaniaServiceImplTest {

    private static final Coordenadas OVIEDO = new Coordenadas(43.3614, -5.8593);
    private static final Coordenadas GIJON = new Coordenadas(43.5322, -5.6611);
    private static final Coordenadas MADRID = new Coordenadas(40.4168, -3.7038);

    @Mock
    private PerfilCuidadorRepository perfilRepository;
    @Mock
    private Geocodificador geocodificador;

    private CercaniaServiceImpl service;

    @BeforeEach
    void setup() {
        service = new CercaniaServiceImpl(perfilRepository, geocodificador);
    }

    private PerfilCuidadorEntity perfil(Long id, String ciudad) {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();
        perfil.setId(id);
        perfil.setCiudad(ciudad);
        return perfil;
    }

    @Test
    void testOrdenaLasPersonasCuidadorasDeLaMasCercanaALaMasLejana() {
        when(geocodificador.buscar("Oviedo")).thenReturn(Optional.of(OVIEDO));
        when(geocodificador.buscar("Madrid")).thenReturn(Optional.of(MADRID));
        when(geocodificador.buscar("Gijón")).thenReturn(Optional.of(GIJON));
        when(perfilRepository.findAll()).thenReturn(List.of(perfil(1L, "Madrid"), perfil(2L, "Gijón")));

        CercaniaDTOResponse resultado = service.buscarCerca("Oviedo");

        assertThat(resultado.lugarEncontrado(), is(true));
        assertThat(resultado.distancias(), hasSize(2));
        assertThat(resultado.distancias().get(0).cuidadorId(), is(equalTo(2L)));
        assertThat(resultado.distancias().get(0).km(), closeTo(25.0, 1.5));
        assertThat(resultado.distancias().get(1).cuidadorId(), is(equalTo(1L)));
    }

    @Test
    void testUnaPersonaDeLaMismaCiudadEstaACeroKilometros() {
        when(geocodificador.buscar("Oviedo")).thenReturn(Optional.of(OVIEDO));
        when(perfilRepository.findAll()).thenReturn(List.of(perfil(1L, "Oviedo")));

        CercaniaDTOResponse resultado = service.buscarCerca("Oviedo");

        assertThat(resultado.distancias().get(0).km(), is(0.0));
    }

    @Test
    void testLaDistanciaSeRedondeaAUnDecimal() {
        when(geocodificador.buscar("Oviedo")).thenReturn(Optional.of(OVIEDO));
        when(geocodificador.buscar("Gijón")).thenReturn(Optional.of(GIJON));
        when(perfilRepository.findAll()).thenReturn(List.of(perfil(1L, "Gijón")));

        double km = service.buscarCerca("Oviedo").distancias().get(0).km();

        assertThat(Math.round(km * 10.0) / 10.0, is(equalTo(km)));
    }

    @Test
    void testSiNoSeEncuentraElLugarNoSeConsultanLosPerfiles() {
        when(geocodificador.buscar("Lugarinventado")).thenReturn(Optional.empty());

        CercaniaDTOResponse resultado = service.buscarCerca("Lugarinventado");

        assertThat(resultado.lugarEncontrado(), is(false));
        assertThat(resultado.distancias().isEmpty(), is(true));
        verify(perfilRepository, never()).findAll();
    }

    @Test
    void testQuienNoTieneCiudadOSuCiudadNoSeEncuentraNoApareceEnLaLista() {
        when(geocodificador.buscar("Oviedo")).thenReturn(Optional.of(OVIEDO));
        when(geocodificador.buscar("Pueblo raro")).thenReturn(Optional.empty());
        when(perfilRepository.findAll()).thenReturn(List.of(
                perfil(1L, null),
                perfil(2L, "   "),
                perfil(3L, "Pueblo raro")));

        CercaniaDTOResponse resultado = service.buscarCerca("Oviedo");

        assertThat(resultado.lugarEncontrado(), is(true));
        assertThat(resultado.distancias().isEmpty(), is(true));
    }

    @Test
    void testUnLugarVacioONuloNoConsultaNada() {
        assertThat(service.buscarCerca("").lugarEncontrado(), is(false));
        assertThat(service.buscarCerca("   ").lugarEncontrado(), is(false));
        assertThat(service.buscarCerca(null).lugarEncontrado(), is(false));

        verify(geocodificador, never()).buscar(anyString());
    }

    @Test
    void testUnLugarDemasiadoLargoSeDescartaSinConsultar() {
        CercaniaDTOResponse resultado = service.buscarCerca("a".repeat(81));

        assertThat(resultado.lugarEncontrado(), is(false));
        verify(geocodificador, never()).buscar(anyString());
    }
}
