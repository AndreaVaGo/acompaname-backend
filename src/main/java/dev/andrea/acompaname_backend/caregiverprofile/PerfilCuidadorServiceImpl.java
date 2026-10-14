package dev.andrea.acompaname_backend.caregiverprofile;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.caregiverprofile.mappers.PerfilCuidadorMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;
import dev.andrea.acompaname_backend.valoracion.ValoracionRepository;

@Service
public class PerfilCuidadorServiceImpl implements PerfilCuidadorService {

    private final PerfilCuidadorRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ValoracionRepository valoracionRepository;

    public PerfilCuidadorServiceImpl(PerfilCuidadorRepository repository, UsuarioRepository usuarioRepository,
            ValoracionRepository valoracionRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.valoracionRepository = valoracionRepository;
    }

    private PerfilCuidadorEntity findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new PerfilCuidadorExceptionNotFound(
                "Perfil de cuidador no encontrado. Id " + id + " no existe."));
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void verificarPropietario(PerfilCuidadorEntity perfil) {
        if (!perfil.getUsuario().getEmail().equals(emailLogueado())) {
            throw new PerfilCuidadorExceptionAccesoDenegado("No tiene permiso para modificar este perfil");
        }
    }

    // Convierte el perfil a DTO añadiendo la valoración media (1 decimal) y cuántas valoraciones tiene
    private PerfilCuidadorDTOResponse aDTO(PerfilCuidadorEntity perfil) {
        Double media = valoracionRepository.mediaPorCuidador(perfil.getId());
        if (media != null) {
            media = Math.round(media * 10) / 10.0;
        }
        Long numValoraciones = valoracionRepository.countBySolicitudCuidadorId(perfil.getId());
        return PerfilCuidadorMapper.toDTO(perfil, media, numValoraciones);
    }

    @Override
    public List<PerfilCuidadorDTOResponse> getEntities() {
        return repository.findAll().stream()
                .map(this::aDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PerfilCuidadorDTOResponse getById(Long id) {
        PerfilCuidadorEntity perfil = findEntityById(id);
        return aDTO(perfil);
    }

    @Transactional
    @Override
    public PerfilCuidadorDTOResponse storeEntity(PerfilCuidadorDTORequest dto) {
        boolean yaExiste = repository.findAll().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(dto.usuarioId()));
        if (yaExiste) {
            throw new PerfilCuidadorExceptionAccesoDenegado("Este usuario ya tiene un perfil cuidador creado");
        }

        UsuarioEntity usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new UsuarioExceptionNotFound(
                        "Usuario no encontrado. Id " + dto.usuarioId() + " no existe."));
        if (!usuario.getEmail().equals(emailLogueado())) {
            throw new PerfilCuidadorExceptionAccesoDenegado("No puede crear un perfil para otro usuario");
        }
        PerfilCuidadorEntity perfilToSave = PerfilCuidadorMapper.toEntity(dto, usuario);
        PerfilCuidadorEntity perfilSaved = repository.save(perfilToSave);
        return PerfilCuidadorMapper.toDTO(perfilSaved);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        PerfilCuidadorEntity perfil = findEntityById(id);
        verificarPropietario(perfil);
        repository.deleteById(id);
    }

    @Transactional
    @Override
    public PerfilCuidadorDTOResponse update(Long id, PerfilCuidadorDTORequest dto) {
        PerfilCuidadorEntity perfilCuidadorExistente = findEntityById(id);
        verificarPropietario(perfilCuidadorExistente);
        perfilCuidadorExistente.setEspecialidad(dto.especialidad());
        perfilCuidadorExistente.setAnosExperiencia(dto.anosExperiencia());
        perfilCuidadorExistente.setTarifaHora(dto.tarifaHora());
        perfilCuidadorExistente.setBio(dto.bio());
        perfilCuidadorExistente.setTieneVehiculo(dto.tieneVehiculo());
        perfilCuidadorExistente.setDisponibleAhora(dto.disponibleAhora());
        perfilCuidadorExistente.setFoto(dto.foto());
        // Si no llega el tipo de cuidado, se deja el que ya tenía
        if (dto.tipoCuidado() != null) {
            perfilCuidadorExistente.setTipoCuidado(dto.tipoCuidado());
        }
        // Si no llegan servicios, se dejan los que ya tenía (una lista vacía los borra todos)
        if (dto.servicios() != null) {
            perfilCuidadorExistente.setServicios(String.join(",", dto.servicios()));
        }
        // Si no llega el sexo, se deja el que ya tenía (un texto vacío lo borra)
        if (dto.sexo() != null) {
            if (dto.sexo().isBlank()) {
                perfilCuidadorExistente.setSexo(null);
            } else {
                perfilCuidadorExistente.setSexo(dto.sexo());
            }
        }
        // La ciudad se actualiza siempre: si no llega, se borra
        perfilCuidadorExistente.setCiudad(dto.ciudad());
        PerfilCuidadorEntity perfilCuidadorActualizado = repository.save(perfilCuidadorExistente);
        return aDTO(perfilCuidadorActualizado);
    }

    @Override
    public PerfilCuidadorDTOResponse getMiPerfil() {
        String emailLogueado = emailLogueado();

        PerfilCuidadorEntity perfil = repository.findAll().stream()
                .filter(p -> p.getUsuario().getEmail().equals(emailLogueado))
                .findFirst()
                .orElseThrow(() -> new PerfilCuidadorExceptionNotFound("No tienes un perfil de cuidador creado."));

        return aDTO(perfil);
    }
}