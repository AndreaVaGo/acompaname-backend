package dev.andrea.acompaname_backend.servicerequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.generics.PaginaDTOResponse;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.globals.ListaDeTexto;
import dev.andrea.acompaname_backend.payment.CalculoDeImporte;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoEntity;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.payment.mappers.PagoMapper;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionEstadoInvalido;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionGrupoNoAtendido;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionSolapamiento;
import dev.andrea.acompaname_backend.servicerequest.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    // Lo máximo que se devuelve en una página, pida lo que pida el cliente
    static final int TAMANO_MAXIMO_PAGINA = 50;

    private final SolicitudRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilCuidadorRepository perfilCuidadorRepository;
    private final PagoRepository pagoRepository;
    private final BloqueOcupadoRepository bloqueRepository;

    // Los avisos por correo son opcionales: si no hay (por ejemplo, en los tests que no los usan),
    // la solicitud funciona igual, solo que sin mandar correos
    private AvisosDeSolicitud avisos;
    private CalculoDeImporte calculoDeImporte = new CalculoDeImporte(CalculoDeImporte.RECARGO_POR_DEFECTO);

    public SolicitudServiceImpl(SolicitudRepository repository, UsuarioRepository usuarioRepository,
            PerfilCuidadorRepository perfilCuidadorRepository, PagoRepository pagoRepository,
            BloqueOcupadoRepository bloqueRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
        this.pagoRepository = pagoRepository;
        this.bloqueRepository = bloqueRepository;
    }

    @Autowired(required = false)
    public void setAvisos(AvisosDeSolicitud avisos) {
        this.avisos = avisos;
    }

    // El cálculo del importe usa el recargo configurado; si no hay configuración (tests sin Spring),
    // usa el recargo por defecto
    @Autowired(required = false)
    public void setCalculoDeImporte(CalculoDeImporte calculoDeImporte) {
        this.calculoDeImporte = calculoDeImporte;
    }

    private SolicitudEntity findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new SolicitudExceptionNotFound("Solicitud no encontrada. Id " + id + " no existe."));
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void verificarPropietario(SolicitudEntity solicitud) {
        String emailLogueado = emailLogueado();
        if (!solicitud.getFamilia().getEmail().equals(emailLogueado)
                && !solicitud.getCuidador().getUsuario().getEmail().equals(emailLogueado)) {
            throw new SolicitudExceptionAccesoDenegado("No tiene permiso para acceder a esta solicitud");
        }
    }

    // Solo el cuidador de la solicitud puede aceptarla, rechazarla o completarla
    private void verificarCuidador(SolicitudEntity solicitud) {
        if (!solicitud.getCuidador().getUsuario().getEmail().equals(emailLogueado())) {
            throw new SolicitudExceptionAccesoDenegado("Solo el cuidador de la solicitud puede cambiar su estado");
        }
    }

    // Una solicitud solo puede pasar a los estados que permite EstadoSolicitud (las reglas están allí)
    private void verificarCambioDeEstado(SolicitudEntity solicitud, EstadoSolicitud nuevoEstado) {
        if (!solicitud.getEstado().puedePasarA(nuevoEstado)) {
            throw new SolicitudExceptionEstadoInvalido(
                    "No se puede pasar una solicitud de " + solicitud.getEstado() + " a " + nuevoEstado);
        }
    }

    // Los datos de una solicitud (fechas, tipo, notas...) solo se pueden cambiar mientras está pendiente
    private void verificarEditable(SolicitudEntity solicitud) {
        if (!solicitud.getEstado().admiteCambios()) {
            throw new SolicitudExceptionEstadoInvalido("Solo se puede modificar una solicitud pendiente");
        }
    }

    // Una solicitud solo se puede borrar mientras está pendiente: una aceptada o completada
    // es un servicio real (y puede tener su pago y su valoración), así que se queda guardada
    private void verificarBorrable(SolicitudEntity solicitud) {
        if (!solicitud.getEstado().admiteCambios()) {
            throw new SolicitudExceptionEstadoInvalido("Solo se puede borrar una solicitud pendiente");
        }
    }

    // El cuidador solo ve la dirección cuando ha aceptado la solicitud; la familia la ve siempre
    private SolicitudDTOResponse aDTO(SolicitudEntity solicitud) {
        SolicitudDTOResponse dto = SolicitudMapper.toDTO(solicitud);
        boolean esCuidador = solicitud.getCuidador().getUsuario().getEmail().equals(emailLogueado());
        boolean aceptada = solicitud.getEstado() == EstadoSolicitud.ACEPTADA
                || solicitud.getEstado() == EstadoSolicitud.COMPLETADA;
        if (!esCuidador || aceptada) {
            return dto;
        }
        return new SolicitudDTOResponse(dto.id(), dto.tipoCuidado(), dto.nombrePaciente(), dto.notas(),
                dto.edadPaciente(), dto.fechaCuidado(), dto.fechaFin(), dto.estado(), dto.familiaId(),
                dto.cuidadorId(), dto.familiaNombre(), dto.cuidadorNombre(), dto.necesidades(), null,
                dto.grupoEdad(), dto.horaInicio(), dto.horaFin(), dto.personas());
    }

    // La persona cuidadora tiene que atender TODOS los grupos de edad de la solicitud.
    // Si no ha marcado ningún grupo en su perfil, no hay restricción
    private void verificarGruposAtendidos(PerfilCuidadorEntity cuidador, List<PersonaCuidada> personas) {
        List<String> atendidos = ListaDeTexto.aLista(cuidador.getGruposEdad());
        if (atendidos.isEmpty()) {
            return;
        }
        for (GrupoEdad grupo : PersonasDeSolicitud.gruposDe(personas)) {
            if (!atendidos.contains(grupo.name())) {
                throw new SolicitudExceptionGrupoNoAtendido(
                        "Esta persona cuidadora no atiende el grupo de edad: " + grupo.etiqueta());
            }
        }
    }

    // La fecha de fin es opcional, pero si viene no puede ser anterior a la de inicio.
    // Si el cuidado dura un solo día, la hora de fin tiene que ser posterior a la de inicio
    private void verificarFechas(SolicitudDTORequest dto) {
        if (dto.fechaFin() != null && dto.fechaFin().isBefore(dto.fechaCuidado())) {
            throw new SolicitudExceptionFechaInvalida("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        boolean unSoloDia = dto.fechaFin() == null || dto.fechaFin().equals(dto.fechaCuidado());
        if (unSoloDia && !dto.horaFin().isAfter(dto.horaInicio())) {
            throw new SolicitudExceptionFechaInvalida("La hora de fin tiene que ser posterior a la hora de inicio");
        }
    }

    // Si no hay fecha de fin, la solicitud cuenta como un solo día
    private LocalDate fechaFinReal(SolicitudEntity solicitud) {
        if (solicitud.getFechaFin() == null) {
            return solicitud.getFechaCuidado();
        }
        return solicitud.getFechaFin();
    }

    // Un cuidador no puede aceptar dos solicitudes cuyas fechas y horas se cruzan
    private void verificarSolapamiento(SolicitudEntity solicitud) {
        Long cuidadorId = solicitud.getCuidador().getId();

        // La base de datos busca los otros servicios aceptados del cuidador que coinciden en algún día
        List<SolicitudEntity> mismosDias = repository.findDelCuidadorQueSeCruzan(cuidadorId, solicitud.getId(),
                solicitud.getFechaCuidado(), fechaFinReal(solicitud), EstadoSolicitud.ACEPTADA);
        // De esos, solo chocan los que también coinciden en las horas
        for (SolicitudEntity otra : mismosDias) {
            if (otra.seCruzaCon(solicitud.momentoInicio(), solicitud.momentoFin())) {
                throw new SolicitudExceptionSolapamiento("Ya tienes otro servicio aceptado en esas fechas");
            }
        }

        // También chocan los bloques ocupados de su calendario (turno en el hospital, citas...)
        for (BloqueOcupadoEntity bloque : bloqueRepository.findByCuidadorIdAndFechaBetween(cuidadorId,
                solicitud.getFechaCuidado(), fechaFinReal(solicitud))) {
            if (bloque.seCruzaCon(solicitud.momentoInicio(), solicitud.momentoFin())) {
                throw new SolicitudExceptionSolapamiento("Tienes un bloque ocupado en tu calendario en esas fechas");
            }
        }
    }

    @Override
    public List<SolicitudDTOResponse> getEntities() {
        // Cada usuario solo ve sus solicitudes, no las de los demás
        return getMisSolicitudes();
    }

    @Override
    public SolicitudDTOResponse getById(Long id) {
        SolicitudEntity solicitud = findEntityById(id);
        verificarPropietario(solicitud);
        return aDTO(solicitud);
    }

    @Transactional
    @Override
    public SolicitudDTOResponse storeEntity(SolicitudDTORequest dto) {
        verificarFechas(dto);
        UsuarioEntity familia = usuarioRepository.findById(dto.familiaId())
                .orElseThrow(() -> new UsuarioExceptionNotFound(
                        "Usuario no encontrado. Id " + dto.familiaId() + " no existe."));
        PerfilCuidadorEntity cuidador = perfilCuidadorRepository.findById(dto.cuidadorId())
                .orElseThrow(() -> new PerfilCuidadorExceptionNotFound(
                        "Perfil de cuidador no encontrado. Id " + dto.cuidadorId() + " no existe."));

        if (!familia.getEmail().equals(emailLogueado())) {
            throw new SolicitudExceptionAccesoDenegado("No puede crear solicitudes en nombre de otra familia");
        }
        // Una persona con los dos roles no puede contratarse a sí misma
        if (cuidador.getUsuario().getEmail().equals(familia.getEmail())) {
            throw new SolicitudExceptionAccesoDenegado("No puede solicitarse un servicio a sí misma");
        }

        verificarGruposAtendidos(cuidador, PersonasDeSolicitud.desdePeticion(dto));

        SolicitudEntity solicitudToSave = SolicitudMapper.toEntity(dto, familia, cuidador);
        SolicitudEntity solicitudSave = repository.save(solicitudToSave);
        // Ya está guardada: se avisa a la persona cuidadora (si el correo falla, la solicitud sigue creada)
        if (avisos != null) {
            avisos.avisarSolicitudNueva(solicitudSave);
        }
        return SolicitudMapper.toDTO(solicitudSave);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        SolicitudEntity solicitud = findEntityById(id);
        verificarPropietario(solicitud);
        verificarBorrable(solicitud);
        repository.deleteById(id);
    }

    @Transactional
    @Override
    public SolicitudDTOResponse update(Long id, SolicitudDTORequest dto) {
        verificarFechas(dto);
        SolicitudEntity solicitudExistente = findEntityById(id);
        verificarPropietario(solicitudExistente);
        verificarEditable(solicitudExistente);
        solicitudExistente.setTipoCuidado(dto.tipoCuidado());
        // Se cambia la lista de personas; la primera también va en nombrePaciente y edadPaciente
        List<PersonaCuidada> personas = PersonasDeSolicitud.desdePeticion(dto);
        verificarGruposAtendidos(solicitudExistente.getCuidador(), personas);
        solicitudExistente.setPersonas(personas);
        solicitudExistente.setNombrePaciente(personas.get(0).getNombre());
        solicitudExistente.setEdadPaciente(personas.get(0).getEdad());
        solicitudExistente.setNotas(dto.notas());
        solicitudExistente.setFechaCuidado(dto.fechaCuidado());
        solicitudExistente.setFechaFin(dto.fechaFin());
        solicitudExistente.setHoraInicio(dto.horaInicio());
        solicitudExistente.setHoraFin(dto.horaFin());
        // Si no vienen necesidades se mantienen las que había; una lista vacía las borra
        if (dto.necesidades() != null) {
            solicitudExistente.setNecesidades(String.join(",", dto.necesidades()));
        }
        // La dirección se actualiza siempre: si no llega, se borra
        solicitudExistente.setDireccion(dto.direccion());
        // El grupo de la solicitud es el de la primera persona. Si no llega ninguno, se deja el que ya tenía
        if (personas.get(0).getGrupoEdad() != null) {
            solicitudExistente.setGrupoEdad(personas.get(0).getGrupoEdad());
        }
        SolicitudEntity solicitudActualizada = repository.save(solicitudExistente);
        return aDTO(solicitudActualizada);
    }

    @Transactional
    @Override
    public SolicitudDTOResponse cambiarEstado(Long id, EstadoSolicitud nuevoEstado) {
        SolicitudEntity solicitud = findEntityById(id);
        verificarCuidador(solicitud);
        verificarCambioDeEstado(solicitud, nuevoEstado);
        boolean seAcepta = nuevoEstado == EstadoSolicitud.ACEPTADA;
        boolean seCompleta = nuevoEstado == EstadoSolicitud.COMPLETADA;
        if (seAcepta) {
            verificarSolapamiento(solicitud);
            // Se guarda cuándo se aceptó, para las estadísticas del administrador
            solicitud.setFechaAceptacion(LocalDateTime.now());
        }
        solicitud.setEstado(nuevoEstado);
        SolicitudEntity solicitudActualizada = repository.save(solicitud);
        if (seCompleta && !pagoRepository.existsBySolicitudId(solicitud.getId())) {
            // Al completar la solicitud se genera su pago pendiente: horas x tarifa del cuidador,
            // con recargo por cada persona extra. La familia solo paga cuando el servicio ya se ha realizado
            BigDecimal importe = calculoDeImporte.calcular(solicitud, solicitud.getCuidador().getTarifaHora(),
                    solicitud.getCuidador().getRecargoPersonaExtra());
            pagoRepository.save(PagoMapper.toEntity(importe, solicitud));
            // Se avisa a la familia de que tiene un pago pendiente (solo cuando se crea el pago)
            if (avisos != null) {
                avisos.avisarServicioCompletado(solicitudActualizada, importe);
            }
        }
        // Ya está guardado el cambio: se avisa a la familia de si la han aceptado o rechazado
        if (avisos != null && nuevoEstado == EstadoSolicitud.ACEPTADA) {
            avisos.avisarSolicitudAceptada(solicitudActualizada);
        }
        if (avisos != null && nuevoEstado == EstadoSolicitud.RECHAZADA) {
            avisos.avisarSolicitudRechazada(solicitudActualizada);
        }
        return aDTO(solicitudActualizada);
    }

    @Override
    public List<SolicitudDTOResponse> getMisSolicitudes() {
        // La base de datos devuelve solo las solicitudes del usuario logueado (como familia o como cuidador)
        return repository.findByParticipante(emailLogueado()).stream()
                .map(this::aDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PaginaDTOResponse<SolicitudDTOResponse> getMisSolicitudesPaginadas(int pagina, int tamano) {
        // Nunca se devuelve todo de golpe: el tamaño tiene un máximo y una página negativa cuenta como la primera
        int paginaSegura = Math.max(pagina, 0);
        int tamanoSeguro = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO_PAGINA);
        // Las más recientes primero; con la misma fecha, las últimas creadas
        Sort orden = Sort.by(Sort.Direction.DESC, "fechaCuidado").and(Sort.by(Sort.Direction.DESC, "id"));
        Page<SolicitudEntity> resultado = repository.findByParticipante(emailLogueado(),
                PageRequest.of(paginaSegura, tamanoSeguro, orden));
        return PaginaDTOResponse.de(resultado, this::aDTO);
    }

    // Para la búsqueda de la familia: devuelve solo ids, sin datos de las solicitudes
    @Override
    public List<Long> getCuidadoresOcupados(LocalDate inicio, LocalDate fin, LocalTime horaInicio,
            LocalTime horaFin) {
        // Sin fecha de fin se busca un solo día
        LocalDate finReal = fin;
        if (finReal == null) {
            finReal = inicio;
        }
        if (finReal.isBefore(inicio)) {
            throw new SolicitudExceptionFechaInvalida("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        // Sin horas se busca el día completo
        LocalTime desde = horaInicio;
        if (desde == null) {
            desde = LocalTime.MIN;
        }
        LocalTime hasta = horaFin;
        if (hasta == null) {
            hasta = LocalTime.MAX;
        }
        LocalDateTime momentoInicio = inicio.atTime(desde);
        LocalDateTime momentoFin = finReal.atTime(hasta);
        if (!momentoFin.isAfter(momentoInicio)) {
            throw new SolicitudExceptionFechaInvalida("La hora de fin tiene que ser posterior a la hora de inicio");
        }

        // La base de datos busca los servicios aceptados que coinciden en algún día; de esos,
        // los que también coinciden en las horas dejan ocupado a su cuidador (sin repetirlo)
        List<Long> ocupados = new ArrayList<>();
        for (SolicitudEntity solicitud : repository.findPorFechasQueSeCruzan(inicio, finReal,
                EstadoSolicitud.ACEPTADA)) {
            Long cuidadorId = solicitud.getCuidador().getId();
            if (solicitud.seCruzaCon(momentoInicio, momentoFin) && !ocupados.contains(cuidadorId)) {
                ocupados.add(cuidadorId);
            }
        }
        // También está ocupada la persona cuidadora que tiene un bloque en su calendario a esas horas
        for (BloqueOcupadoEntity bloque : bloqueRepository.findByFechaBetween(inicio, finReal)) {
            Long cuidadorId = bloque.getCuidador().getId();
            if (bloque.seCruzaCon(momentoInicio, momentoFin) && !ocupados.contains(cuidadorId)) {
                ocupados.add(cuidadorId);
            }
        }
        return ocupados;
    }
}
