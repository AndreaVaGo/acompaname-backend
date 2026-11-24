package dev.andrea.acompaname_backend.fichaje;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.fichaje.dtos.FichajeConfiguracionDTOResponse;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTORequest;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTOResponse;
import dev.andrea.acompaname_backend.geolocation.Coordenadas;
import dev.andrea.acompaname_backend.geolocation.Distancia;
import dev.andrea.acompaname_backend.geolocation.Geocodificador;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionEstadoInvalido;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;

// Privacidad (RGPD, minimización de datos):
// - La ubicación se pide UNA vez en cada fichaje, nunca de forma continua.
// - Las coordenadas se usan solo para calcular la distancia y se descartan: no se guardan ni se muestran.
// - Solo se consulta la dirección del servicio al geocodificador (un servicio de internet de terceros)
//   si hay ubicación que comparar Y la comprobación del lugar está activada (fichaje.verificar-lugar).
//   Por defecto está APAGADA: la dirección, que puede ser un domicilio, no sale de la aplicación.
@Service
public class FichajeServiceImpl implements FichajeService {

    private static final double METROS_POR_KILOMETRO = 1000.0;

    private final FichajeRepository repository;
    private final SolicitudRepository solicitudRepository;
    private final Geocodificador geocodificador;
    // A cuántos metros de la dirección se considera que la persona cuidadora está "en el lugar"
    private final double radioMetros;
    // Si está apagado, el fichaje solo guarda la hora: no se usa la ubicación ni se consulta la dirección
    private final boolean verificarLugar;

    public FichajeServiceImpl(FichajeRepository repository, SolicitudRepository solicitudRepository,
            Geocodificador geocodificador, @Value("${fichaje.radio-metros:300}") double radioMetros,
            @Value("${fichaje.verificar-lugar:false}") boolean verificarLugar) {
        this.repository = repository;
        this.solicitudRepository = solicitudRepository;
        this.geocodificador = geocodificador;
        this.radioMetros = radioMetros;
        this.verificarLugar = verificarLugar;
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private SolicitudEntity buscarSolicitud(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new SolicitudExceptionNotFound("Solicitud no encontrada"));
    }

    // Solo la persona cuidadora de esa solicitud puede fichar, y solo si ya la aceptó
    private SolicitudEntity solicitudParaFichar(Long solicitudId) {
        SolicitudEntity solicitud = buscarSolicitud(solicitudId);
        if (!solicitud.getCuidador().getUsuario().getEmail().equals(emailLogueado())) {
            throw new SolicitudExceptionAccesoDenegado("Solo la persona cuidadora de la solicitud puede fichar");
        }
        if (solicitud.getEstado() != EstadoSolicitud.ACEPTADA) {
            throw new SolicitudExceptionEstadoInvalido("Solo se puede fichar en una solicitud aceptada");
        }
        return solicitud;
    }

    @Transactional
    @Override
    public FichajeDTOResponse empezar(Long solicitudId, FichajeDTORequest ubicacion) {
        SolicitudEntity solicitud = solicitudParaFichar(solicitudId);
        if (LocalDate.now().isBefore(solicitud.getFechaCuidado())) {
            throw new SolicitudExceptionFechaInvalida("Todavía no es el día del servicio");
        }
        if (repository.existsBySolicitudIdAndTipo(solicitudId, TipoFichaje.INICIO)) {
            throw new SolicitudExceptionEstadoInvalido("El servicio ya se había empezado");
        }
        return guardar(solicitud, TipoFichaje.INICIO, ubicacion);
    }

    @Transactional
    @Override
    public FichajeDTOResponse terminar(Long solicitudId, FichajeDTORequest ubicacion) {
        SolicitudEntity solicitud = solicitudParaFichar(solicitudId);
        if (!repository.existsBySolicitudIdAndTipo(solicitudId, TipoFichaje.INICIO)) {
            throw new SolicitudExceptionEstadoInvalido("Primero hay que empezar el servicio");
        }
        if (repository.existsBySolicitudIdAndTipo(solicitudId, TipoFichaje.FIN)) {
            throw new SolicitudExceptionEstadoInvalido("El servicio ya se había terminado");
        }
        return guardar(solicitud, TipoFichaje.FIN, ubicacion);
    }

    @Override
    public List<FichajeDTOResponse> listar(Long solicitudId) {
        SolicitudEntity solicitud = buscarSolicitud(solicitudId);
        String email = emailLogueado();
        boolean esFamilia = solicitud.getFamilia().getEmail().equals(email);
        boolean esCuidador = solicitud.getCuidador().getUsuario().getEmail().equals(email);
        if (!esFamilia && !esCuidador) {
            throw new SolicitudExceptionAccesoDenegado("No tiene permiso para acceder a esta solicitud");
        }

        List<FichajeDTOResponse> respuesta = new ArrayList<>();
        for (FichajeEntity fichaje : repository.findBySolicitudIdOrderByMomentoAsc(solicitudId)) {
            respuesta.add(FichajeDTOResponse.de(fichaje));
        }
        return respuesta;
    }

    @Override
    public FichajeConfiguracionDTOResponse configuracion() {
        return new FichajeConfiguracionDTOResponse(verificarLugar);
    }

    private FichajeDTOResponse guardar(SolicitudEntity solicitud, TipoFichaje tipo, FichajeDTORequest ubicacion) {
        FichajeEntity fichaje = FichajeEntity.builder()
                .solicitud(solicitud)
                .tipo(tipo)
                .momento(LocalDateTime.now())
                .ubicacion(comprobarUbicacion(solicitud, ubicacion))
                .build();
        return FichajeDTOResponse.de(repository.save(fichaje));
    }

    // Compara el punto que manda el navegador con la dirección del servicio. Las coordenadas no se guardan
    private ResultadoUbicacion comprobarUbicacion(SolicitudEntity solicitud, FichajeDTORequest ubicacion) {
        // Con la comprobación apagada se ignora la ubicación aunque llegue: ni se usa ni se consulta nada
        if (!verificarLugar || ubicacion == null || !ubicacion.tieneUbicacion()) {
            return ResultadoUbicacion.SIN_UBICACION;
        }
        String direccion = solicitud.getDireccion();
        if (direccion == null || direccion.isBlank()) {
            return ResultadoUbicacion.NO_VERIFICABLE;
        }
        Optional<Coordenadas> lugar = geocodificador.buscar(direccion);
        if (lugar.isEmpty()) {
            return ResultadoUbicacion.NO_VERIFICABLE;
        }
        Coordenadas persona = new Coordenadas(ubicacion.latitud(), ubicacion.longitud());
        double metros = Distancia.enKilometros(persona, lugar.get()) * METROS_POR_KILOMETRO;
        if (metros <= radioMetros) {
            return ResultadoUbicacion.EN_EL_LUGAR;
        }
        return ResultadoUbicacion.FUERA_DEL_LUGAR;
    }
}
