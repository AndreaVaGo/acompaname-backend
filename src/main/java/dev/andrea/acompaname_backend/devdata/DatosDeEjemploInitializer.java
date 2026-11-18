package dev.andrea.acompaname_backend.devdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Datos de ejemplo SOLO para desarrollo: una familia, dos personas cuidadoras con perfil,
// cuatro solicitudes (una de cada estado) y dos pagos. Así no hay que registrar usuarios a mano.
// Solo se ejecuta si se arranca con el perfil "dev" (SPRING_PROFILES_ACTIVE=dev).
// Nunca se ejecuta en producción ni en los tests, porque ahí no está activo ese perfil.
// Se ejecuta el tercero (Order 3), cuando RoleInitializer ya ha creado los roles.
@Component
@Profile("dev")
@Order(3)
public class DatosDeEjemploInitializer implements CommandLineRunner {

    // Contraseña de prueba de los tres usuarios de ejemplo (cumple la regla de contraseñas)
    public static final String PASSWORD_DE_EJEMPLO = "Password123!";
    public static final String EMAIL_FAMILIA = "familia@test.com";
    public static final String EMAIL_CUIDADOR = "cuidador@test.com";
    public static final String EMAIL_CUIDADORA = "cuidadora@test.com";

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PerfilCuidadorRepository perfilRepository;
    private final SolicitudRepository solicitudRepository;
    private final PagoRepository pagoRepository;
    private final PasswordEncoder passwordEncoder;

    public DatosDeEjemploInitializer(UsuarioRepository usuarioRepository, RoleRepository roleRepository,
            PerfilCuidadorRepository perfilRepository, SolicitudRepository solicitudRepository,
            PagoRepository pagoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.perfilRepository = perfilRepository;
        this.solicitudRepository = solicitudRepository;
        this.pagoRepository = pagoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public void run(String... args) {
        // Si los datos ya están creados, no se vuelven a crear (así no se duplican al reiniciar)
        if (usuarioRepository.findByEmail(EMAIL_FAMILIA).isPresent()) {
            return;
        }

        Optional<RoleEntity> rolFamilia = roleRepository.findByName("FAMILIA");
        Optional<RoleEntity> rolCuidador = roleRepository.findByName("CUIDADOR");
        if (rolFamilia.isEmpty() || rolCuidador.isEmpty()) {
            return;
        }

        UsuarioEntity familia = crearUsuario("Ana Prueba", EMAIL_FAMILIA, "600123456", rolFamilia.get());
        UsuarioEntity usuarioPepe = crearUsuario("Pepe Prueba", EMAIL_CUIDADOR, "600123457", rolCuidador.get());
        UsuarioEntity usuarioLucia = crearUsuario("Lucía Prueba", EMAIL_CUIDADORA, "600123458", rolCuidador.get());

        PerfilCuidadorEntity pepe = crearPerfilDePepe(usuarioPepe);
        PerfilCuidadorEntity lucia = crearPerfilDeLucia(usuarioLucia);

        LocalDate hoy = LocalDate.now();

        // 1. Pendiente: Pepe todavía no la ha aceptado
        crearSolicitud(familia, pepe, EstadoSolicitud.PENDIENTE, "domicilio", "Carmen", 82, GrupoEdad.MAYORES,
                hoy.plusDays(3), LocalTime.of(9, 0), LocalTime.of(12, 0), "Calle Uría 10, Oviedo");

        // 2. Aceptada: Pepe la aceptó y la familia todavía tiene el pago pendiente (4 horas x 14,50)
        SolicitudEntity aceptada = crearSolicitud(familia, pepe, EstadoSolicitud.ACEPTADA, "domicilio", "Carmen", 82,
                GrupoEdad.MAYORES, hoy.plusDays(7), LocalTime.of(10, 0), LocalTime.of(14, 0),
                "Calle Uría 10, Oviedo");
        crearPago(aceptada, new BigDecimal("58.00"), EstadoPago.PENDIENTE, hoy);

        // 3. Completada: ya se hizo y ya se pagó (4 horas x 18,00)
        SolicitudEntity completada = crearSolicitud(familia, lucia, EstadoSolicitud.COMPLETADA, "hospitalario",
                "Manuel", 75, GrupoEdad.MAYORES, hoy.minusDays(10), LocalTime.of(16, 0), LocalTime.of(20, 0),
                "Hospital Universitario Central, planta 3");
        crearPago(completada, new BigDecimal("72.00"), EstadoPago.COMPLETADO, hoy.minusDays(10));

        // 4. Rechazada: Lucía no pudo hacerla
        crearSolicitud(familia, lucia, EstadoSolicitud.RECHAZADA, "hospitalario", "Manuel", 75, GrupoEdad.MAYORES,
                hoy.plusDays(5), LocalTime.of(8, 0), LocalTime.of(12, 0), "Hospital Universitario Central, planta 3");
    }

    private UsuarioEntity crearUsuario(String nombre, String email, String telefono, RoleEntity rol) {
        UsuarioEntity usuario = new UsuarioEntity(null, nombre, email, telefono,
                passwordEncoder.encode(PASSWORD_DE_EJEMPLO), Set.of(rol));
        return usuarioRepository.save(usuario);
    }

    private PerfilCuidadorEntity crearPerfilDePepe(UsuarioEntity usuario) {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .usuario(usuario)
                .especialidad("Cuidado de personas mayores")
                .anosExperiencia(5)
                .tarifaHora(new BigDecimal("14.50"))
                .bio("Paciente y puntual. Acompaño a personas mayores en casa y en sus gestiones.")
                .tieneVehiculo(true)
                .disponibleAhora(true)
                .tipoCuidado("domicilio")
                .servicios("movilidad,medicacion")
                .sexo("HOMBRE")
                .ciudad("Oviedo")
                .gruposEdad("MAYORES,DISCAPACIDAD_DEPENDENCIA")
                .titulaciones("AUXILIAR_ENFERMERIA,PRIMEROS_AUXILIOS")
                .build();
        return perfilRepository.save(perfil);
    }

    private PerfilCuidadorEntity crearPerfilDeLucia(UsuarioEntity usuario) {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .usuario(usuario)
                .especialidad("Acompañamiento en hospital")
                .anosExperiencia(8)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Enfermera con experiencia en acompañamiento hospitalario y noches.")
                .tieneVehiculo(false)
                .disponibleAhora(true)
                .tipoCuidado("hospitalario")
                .servicios("medicacion,higiene")
                .sexo("MUJER")
                .ciudad("Gijón")
                .gruposEdad("ADULTOS,MAYORES")
                .titulaciones("ENFERMERIA")
                .build();
        return perfilRepository.save(perfil);
    }

    private SolicitudEntity crearSolicitud(UsuarioEntity familia, PerfilCuidadorEntity cuidador,
            EstadoSolicitud estado, String tipoCuidado, String paciente, int edad, GrupoEdad grupoEdad,
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String direccion) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .familia(familia)
                .cuidador(cuidador)
                .estado(estado)
                .tipoCuidado(tipoCuidado)
                .nombrePaciente(paciente)
                .edadPaciente(edad)
                .grupoEdad(grupoEdad)
                .necesidades("movilidad,higiene")
                .notas("Solicitud de ejemplo creada para desarrollo")
                .direccion(direccion)
                .fechaCuidado(fecha)
                .fechaFin(fecha)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .fechaCreacion(LocalDateTime.now())
                .build();
        // Solo las aceptadas y las completadas tienen momento de aceptación
        if (estado == EstadoSolicitud.ACEPTADA || estado == EstadoSolicitud.COMPLETADA) {
            solicitud.setFechaAceptacion(LocalDateTime.now());
        }
        return solicitudRepository.save(solicitud);
    }

    private void crearPago(SolicitudEntity solicitud, BigDecimal importe, EstadoPago estado, LocalDate fecha) {
        PagoEntity pago = PagoEntity.builder()
                .solicitud(solicitud)
                .importe(importe)
                .estado(estado)
                .fecha(fecha)
                .build();
        pagoRepository.save(pago);
    }
}
