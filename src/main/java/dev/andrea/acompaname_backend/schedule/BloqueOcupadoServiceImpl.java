package dev.andrea.acompaname_backend.schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTORequest;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTOResponse;
import dev.andrea.acompaname_backend.schedule.dtos.ImportacionCalendarioDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionNotFound;
import dev.andrea.acompaname_backend.schedule.mappers.BloqueOcupadoMapper;

@Service
public class BloqueOcupadoServiceImpl implements BloqueOcupadoService {

    private final BloqueOcupadoRepository repository;
    private final PerfilCuidadorRepository perfilRepository;
    // No guarda nada de nada: solo lee texto, así que no hace falta inyectarlo
    private final LectorDeCalendario lector = new LectorDeCalendario();

    public BloqueOcupadoServiceImpl(BloqueOcupadoRepository repository, PerfilCuidadorRepository perfilRepository) {
        this.repository = repository;
        this.perfilRepository = perfilRepository;
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @Override
    public BloqueOcupadoDTOResponse crear(BloqueOcupadoDTORequest dto) {
        PerfilCuidadorEntity perfil = perfilRepository.findByUsuarioEmail(emailLogueado()).orElseThrow(
                () -> new PerfilCuidadorExceptionNotFound("Primero tienes que crear tu perfil de cuidador"));

        if (dto.fecha().isBefore(LocalDate.now())) {
            throw new BloqueOcupadoExceptionFechaInvalida("La fecha no puede estar en el pasado");
        }
        if (!dto.horaFin().isAfter(dto.horaInicio())) {
            throw new BloqueOcupadoExceptionFechaInvalida("La hora de fin tiene que ser posterior a la hora de inicio");
        }

        BloqueOcupadoEntity guardado = repository.save(BloqueOcupadoMapper.toEntity(dto, perfil));
        return BloqueOcupadoMapper.toDTO(guardado);
    }

    @Override
    public List<BloqueOcupadoDTOResponse> listarMios() {
        Optional<PerfilCuidadorEntity> perfil = perfilRepository.findByUsuarioEmail(emailLogueado());
        List<BloqueOcupadoDTOResponse> respuesta = new ArrayList<>();
        // Sin perfil todavía no puede tener bloques: la lista sale vacía
        if (perfil.isEmpty()) {
            return respuesta;
        }
        for (BloqueOcupadoEntity bloque : repository
                .findByCuidadorIdOrderByFechaAscHoraInicioAsc(perfil.get().getId())) {
            respuesta.add(BloqueOcupadoMapper.toDTO(bloque));
        }
        return respuesta;
    }

    // Lo que se escribe como motivo en los bloques importados: nunca el título de la cita
    static final String MOTIVO_IMPORTADO = "Calendario importado";

    @Transactional
    @Override
    public ImportacionCalendarioDTOResponse importar(String contenido) {
        PerfilCuidadorEntity perfil = perfilRepository.findByUsuarioEmail(emailLogueado()).orElseThrow(
                () -> new PerfilCuidadorExceptionNotFound("Primero tienes que crear tu perfil de cuidador"));

        // Si el archivo no vale, esto lanza el error antes de guardar nada
        LecturaDeCalendario lectura = lector.leer(contenido);

        // Los bloques que ya tiene, para no crearlos dos veces si importa el mismo archivo otra vez
        List<BloqueOcupadoEntity> existentes = new ArrayList<>(
                repository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(perfil.getId()));

        LocalDate hoy = LocalDate.now();
        int importados = 0;
        int duplicados = 0;
        int omitidos = lectura.omitidos();

        for (TramoOcupado tramo : lectura.tramos()) {
            if (tramo.fecha().isBefore(hoy)) {
                omitidos++;
            } else if (yaExiste(existentes, tramo)) {
                duplicados++;
            } else {
                BloqueOcupadoEntity nuevo = BloqueOcupadoEntity.builder()
                        .cuidador(perfil)
                        .fecha(tramo.fecha())
                        .horaInicio(tramo.horaInicio())
                        .horaFin(tramo.horaFin())
                        .motivo(MOTIVO_IMPORTADO)
                        .build();
                existentes.add(repository.save(nuevo));
                importados++;
            }
        }
        return new ImportacionCalendarioDTOResponse(importados, duplicados, omitidos);
    }

    // Un bloque es el mismo si tiene la misma fecha y las mismas horas
    private boolean yaExiste(List<BloqueOcupadoEntity> existentes, TramoOcupado tramo) {
        for (BloqueOcupadoEntity bloque : existentes) {
            boolean mismaFecha = bloque.getFecha().equals(tramo.fecha());
            boolean mismoInicio = bloque.getHoraInicio().equals(tramo.horaInicio());
            boolean mismoFin = bloque.getHoraFin().equals(tramo.horaFin());
            if (mismaFecha && mismoInicio && mismoFin) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    @Override
    public void borrar(Long id) {
        BloqueOcupadoEntity bloque = repository.findById(id).orElseThrow(
                () -> new BloqueOcupadoExceptionNotFound("Bloque no encontrado. Id " + id + " no existe."));
        String emailDelDueno = bloque.getCuidador().getUsuario().getEmail();
        if (!emailDelDueno.equals(emailLogueado())) {
            throw new BloqueOcupadoExceptionAccesoDenegado("No tienes permiso para borrar este bloque");
        }
        repository.delete(bloque);
    }
}
