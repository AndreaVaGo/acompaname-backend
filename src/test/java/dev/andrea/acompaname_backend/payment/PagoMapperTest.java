package dev.andrea.acompaname_backend.payment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.payment.mappers.PagoMapper;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.PersonaCuidada;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class PagoMapperTest {

    private SolicitudEntity crearSolicitud() {
        RoleEntity rolFamilia = new RoleEntity();
        rolFamilia.setId(1L);
        rolFamilia.setName("FAMILIA");
        RoleEntity rolCuidador = new RoleEntity();
        rolCuidador.setId(2L);
        rolCuidador.setName("CUIDADOR");
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rolFamilia));
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                        Set.of(rolCuidador)));
        return new SolicitudEntity(7L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.COMPLETADA, familia, cuidador);
    }

    @Test
    void testToDTOCopiaTodosLosCampos() {
        PagoEntity pago = PagoEntity.builder()
                .id(3L)
                .importe(new BigDecimal("90.00"))
                .estado(EstadoPago.COMPLETADO)
                .fecha(LocalDate.of(2026, 9, 11))
                .solicitud(crearSolicitud())
                .build();

        PagoDTOResponse dto = PagoMapper.toDTO(pago);

        assertThat(dto.id(), is(equalTo(3L)));
        assertThat(dto.importe(), is(equalTo(new BigDecimal("90.00"))));
        assertThat(dto.estado(), is(equalTo(EstadoPago.COMPLETADO)));
        assertThat(dto.fecha(), is(equalTo(LocalDate.of(2026, 9, 11))));
        assertThat(dto.solicitudId(), is(equalTo(7L)));
    }

    @Test
    void testToDTOIncluyeElNombreDelCuidadorYElTipoDeCuidado() {
        PagoEntity pago = PagoEntity.builder()
                .id(3L)
                .importe(new BigDecimal("90.00"))
                .estado(EstadoPago.PENDIENTE)
                .fecha(LocalDate.of(2026, 9, 11))
                .solicitud(crearSolicitud())
                .build();

        PagoDTOResponse dto = PagoMapper.toDTO(pago);

        assertThat(dto.cuidadorNombre(), is(equalTo("Pepe")));
        assertThat(dto.tipoCuidado(), is(equalTo("Acompañamiento")));
    }

    @Test
    void testToEntityCreaUnPagoPendienteConElImporteYLaSolicitud() {
        SolicitudEntity solicitud = crearSolicitud();

        PagoEntity pago = PagoMapper.toEntity(new BigDecimal("36.00"), solicitud);

        assertThat(pago.getEstado(), is(equalTo(EstadoPago.PENDIENTE)));
        assertThat(pago.getImporte(), is(equalTo(new BigDecimal("36.00"))));
        assertThat(pago.getSolicitud(), is(equalTo(solicitud)));
        assertThat(pago.getFecha(), is(equalTo(LocalDate.now())));
    }

    // ---- Horas y personas del desglose ----

    private PagoEntity pagoDe(SolicitudEntity solicitud) {
        return PagoEntity.builder()
                .id(3L)
                .importe(new BigDecimal("90.00"))
                .estado(EstadoPago.PENDIENTE)
                .fecha(LocalDate.of(2026, 9, 11))
                .solicitud(solicitud)
                .build();
    }

    @Test
    void testToDTOIncluyeLasHorasDeLaSolicitud() {
        SolicitudEntity solicitud = crearSolicitud();
        solicitud.setHoraInicio(LocalTime.of(10, 0));
        solicitud.setHoraFin(LocalTime.of(14, 0));

        PagoDTOResponse dto = PagoMapper.toDTO(pagoDe(solicitud));

        assertThat(dto.horas(), is(equalTo(4L)));
    }

    @Test
    void testToDTOSinHorasCuentaElDiaCompleto() {
        PagoDTOResponse dto = PagoMapper.toDTO(pagoDe(crearSolicitud()));

        assertThat(dto.horas(), is(equalTo(24L)));
    }

    @Test
    void testToDTOIncluyeElNumeroDePersonas() {
        SolicitudEntity solicitud = crearSolicitud();
        solicitud.getPersonas().add(new PersonaCuidada("Carmen", 82));
        solicitud.getPersonas().add(new PersonaCuidada("Antonio", 85));

        PagoDTOResponse dto = PagoMapper.toDTO(pagoDe(solicitud));

        assertThat(dto.personas(), is(equalTo(2)));
    }

    @Test
    void testToDTOUnaSolicitudAntiguaSinListaCuentaComoUnaPersona() {
        PagoDTOResponse dto = PagoMapper.toDTO(pagoDe(crearSolicitud()));

        assertThat(dto.personas(), is(equalTo(1)));
    }
}
