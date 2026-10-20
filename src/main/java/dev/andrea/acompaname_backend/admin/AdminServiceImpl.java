package dev.andrea.acompaname_backend.admin;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.admin.dtos.AvisosDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.CuidadorValoradoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.EstadisticasDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

@Service
public class AdminServiceImpl implements AdminService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilCuidadorRepository perfilCuidadorRepository;
    private final SolicitudRepository solicitudRepository;
    private final PagoRepository pagoRepository;
    private final ValoracionRepository valoracionRepository;

    public AdminServiceImpl(UsuarioRepository usuarioRepository, PerfilCuidadorRepository perfilCuidadorRepository,
            SolicitudRepository solicitudRepository, PagoRepository pagoRepository,
            ValoracionRepository valoracionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
        this.solicitudRepository = solicitudRepository;
        this.pagoRepository = pagoRepository;
        this.valoracionRepository = valoracionRepository;
    }

    // Redondea a un decimal (por ejemplo 4.26 pasa a 4.3)
    private double redondear(double numero) {
        return Math.round(numero * 10.0) / 10.0;
    }

    // Si no hay pagos en ese estado la suma es null, y se cuenta como 0
    private BigDecimal sumaDePagos(EstadoPago estado) {
        BigDecimal suma = pagoRepository.sumaPorEstado(estado);
        if (suma == null) {
            return BigDecimal.ZERO;
        }
        return suma;
    }

    // Media de las puntuaciones de todas las valoraciones (null si todavía no hay ninguna)
    private Double valoracionMedia() {
        Double media = valoracionRepository.mediaGeneral();
        if (media == null) {
            return null;
        }
        return redondear(media);
    }

    // Horas que pasan de media desde que se crea una solicitud hasta que se acepta (null si ninguna se ha aceptado)
    private Double horasMediasHastaAceptar() {
        List<SolicitudEntity> aceptadas = solicitudRepository.findByFechaAceptacionIsNotNull();
        long minutosTotales = 0;
        int cuantas = 0;
        for (SolicitudEntity solicitud : aceptadas) {
            if (solicitud.getFechaCreacion() != null) {
                minutosTotales = minutosTotales
                        + Duration.between(solicitud.getFechaCreacion(), solicitud.getFechaAceptacion()).toMinutes();
                cuantas = cuantas + 1;
            }
        }
        if (cuantas == 0) {
            return null;
        }
        double horas = minutosTotales / 60.0 / cuantas;
        return redondear(horas);
    }

    @Override
    public EstadisticasDTOResponse estadisticas() {
        return new EstadisticasDTOResponse(
                usuarioRepository.countByRolesName("FAMILIA"),
                usuarioRepository.countByRolesName("CUIDADOR"),
                perfilCuidadorRepository.countByDisponibleAhoraTrue(),
                solicitudRepository.countByEstado(EstadoSolicitud.PENDIENTE),
                solicitudRepository.countByEstado(EstadoSolicitud.ACEPTADA),
                solicitudRepository.countByEstado(EstadoSolicitud.RECHAZADA),
                solicitudRepository.countByEstado(EstadoSolicitud.COMPLETADA),
                solicitudRepository.countByTipoCuidado("hospitalario"),
                solicitudRepository.countByTipoCuidado("domicilio"),
                sumaDePagos(EstadoPago.COMPLETADO),
                sumaDePagos(EstadoPago.PENDIENTE),
                valoracionMedia(),
                horasMediasHastaAceptar());
    }

    @Override
    public AvisosDTOResponse avisos() {
        LocalDate hoy = LocalDate.now();

        // Solicitudes sin responder cuya fecha es hoy, mañana o pasado mañana
        long proximas = solicitudRepository.countByEstadoAndFechaCuidadoBetween(EstadoSolicitud.PENDIENTE, hoy,
                hoy.plusDays(2));
        // Solicitudes sin responder cuya fecha ya ha pasado
        long vencidas = solicitudRepository.countByEstadoAndFechaCuidadoBefore(EstadoSolicitud.PENDIENTE, hoy);
        // Pagos pendientes creados hace más de una semana
        long pagosSinCobrar = pagoRepository.countByEstadoAndFechaBefore(EstadoPago.PENDIENTE, hoy.minusDays(7));

        return new AvisosDTOResponse(proximas, vencidas, pagosSinCobrar);
    }

    @Override
    public List<CuidadorValoradoDTOResponse> mejoresCuidadores() {
        // Primero, todos los cuidadores que tienen alguna valoración
        List<CuidadorValoradoDTOResponse> conNota = new ArrayList<>();
        for (PerfilCuidadorEntity perfil : perfilCuidadorRepository.findAll()) {
            Double media = valoracionRepository.mediaPorCuidador(perfil.getId());
            if (media != null) {
                long completados = solicitudRepository.countByCuidadorIdAndEstado(perfil.getId(),
                        EstadoSolicitud.COMPLETADA);
                conNota.add(new CuidadorValoradoDTOResponse(perfil.getId(), perfil.getUsuario().getNombre(),
                        redondear(media), completados));
            }
        }

        // Después, se quedan los tres de mayor nota: cada vuelta busca el mejor de los que quedan
        List<CuidadorValoradoDTOResponse> mejores = new ArrayList<>();
        while (mejores.size() < 3 && !conNota.isEmpty()) {
            CuidadorValoradoDTOResponse mejor = conNota.get(0);
            for (CuidadorValoradoDTOResponse cuidador : conNota) {
                if (cuidador.notaMedia() > mejor.notaMedia()) {
                    mejor = cuidador;
                }
            }
            mejores.add(mejor);
            conNota.remove(mejor);
        }
        return mejores;
    }
}