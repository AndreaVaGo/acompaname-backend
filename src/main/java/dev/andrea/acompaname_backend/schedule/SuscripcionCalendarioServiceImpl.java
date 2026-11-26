package dev.andrea.acompaname_backend.schedule;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.schedule.dtos.SuscripcionCalendarioDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;

// Sincronizar el calendario de la persona cuidadora con uno externo (Google, Outlook, iCloud).
// Este servicio NO es @Transactional a propósito: la descarga puede tardar y no debe tener la base de datos
// ocupada. Lo que escribe en la base de datos lo hace GuardadoDeSincronizacion, en su propia transacción.
@Service
public class SuscripcionCalendarioServiceImpl implements SuscripcionCalendarioService {

    private static final Logger log = LoggerFactory.getLogger(SuscripcionCalendarioServiceImpl.class);

    private final SuscripcionCalendarioRepository repository;
    private final PerfilCuidadorRepository perfilRepository;
    private final DescargadorDeCalendario descargador;
    private final GuardadoDeSincronizacion guardado;
    private final LectorDeCalendario lector = new LectorDeCalendario();

    public SuscripcionCalendarioServiceImpl(SuscripcionCalendarioRepository repository,
            PerfilCuidadorRepository perfilRepository, DescargadorDeCalendario descargador,
            GuardadoDeSincronizacion guardado) {
        this.repository = repository;
        this.perfilRepository = perfilRepository;
        this.descargador = descargador;
        this.guardado = guardado;
    }

    private PerfilCuidadorEntity perfilLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return perfilRepository.findByUsuarioEmail(auth.getName()).orElseThrow(
                () -> new PerfilCuidadorExceptionNotFound("Primero tienes que crear tu perfil de cuidador"));
    }

    private static SuscripcionCalendarioDTOResponse aDTO(SuscripcionCalendarioEntity suscripcion) {
        return new SuscripcionCalendarioDTOResponse(true, suscripcion.getUltimaSincronizacion(),
                suscripcion.getUltimoError());
    }

    @Override
    public SuscripcionCalendarioDTOResponse estado() {
        PerfilCuidadorEntity perfil = perfilLogueado();
        return repository.findByCuidadorId(perfil.getId()).map(SuscripcionCalendarioServiceImpl::aDTO)
                .orElse(SuscripcionCalendarioDTOResponse.noVinculado());
    }

    @Override
    public SuscripcionCalendarioDTOResponse vincular(String url) {
        PerfilCuidadorEntity perfil = perfilLogueado();
        String direccion = descargador.normalizar(url);
        // Se lee antes de guardar: si la dirección no vale, la persona lo sabe al momento y no queda nada a medias
        LecturaDeCalendario lectura = lector.leer(descargador.descargar(direccion));

        SuscripcionCalendarioEntity suscripcion = repository.findByCuidadorId(perfil.getId())
                .orElseGet(() -> SuscripcionCalendarioEntity.builder().cuidador(perfil).build());
        suscripcion.setUrl(direccion);
        guardado.aplicar(suscripcion, lectura);
        return aDTO(suscripcion);
    }

    @Override
    public SuscripcionCalendarioDTOResponse sincronizarAhora() {
        PerfilCuidadorEntity perfil = perfilLogueado();
        SuscripcionCalendarioEntity suscripcion = repository.findByCuidadorId(perfil.getId()).orElseThrow(
                () -> new BloqueOcupadoExceptionFechaInvalida("Todavía no has vinculado ningún calendario"));
        sincronizar(suscripcion);
        return aDTO(suscripcion);
    }

    @Override
    public void desvincular() {
        PerfilCuidadorEntity perfil = perfilLogueado();
        // Si no había nada vinculado no pasa nada: el resultado es el mismo
        repository.findByCuidadorId(perfil.getId()).ifPresent(guardado::desvincular);
    }

    @Override
    public void sincronizarTodas() {
        List<SuscripcionCalendarioEntity> todas = repository.findAll();
        for (SuscripcionCalendarioEntity suscripcion : todas) {
            // Si una falla, las demás se siguen leyendo
            sincronizar(suscripcion);
        }
        log.info("Sincronización de calendarios: {} revisados", todas.size());
    }

    // Lee una suscripción. Si falla no lanza el error: lo apunta para enseñarlo en la pantalla
    private void sincronizar(SuscripcionCalendarioEntity suscripcion) {
        try {
            LecturaDeCalendario lectura = lector.leer(descargador.descargar(suscripcion.getUrl()));
            guardado.aplicar(suscripcion, lectura);
        } catch (RuntimeException e) {
            guardado.registrarError(suscripcion, e.getMessage());
        }
    }
}
