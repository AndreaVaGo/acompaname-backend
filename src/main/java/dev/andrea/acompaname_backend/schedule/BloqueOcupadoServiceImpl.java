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
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionNotFound;
import dev.andrea.acompaname_backend.schedule.mappers.BloqueOcupadoMapper;

@Service
public class BloqueOcupadoServiceImpl implements BloqueOcupadoService {

    private final BloqueOcupadoRepository repository;
    private final PerfilCuidadorRepository perfilRepository;

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
