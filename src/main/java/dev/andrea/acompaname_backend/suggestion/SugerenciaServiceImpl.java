package dev.andrea.acompaname_backend.suggestion;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.suggestion.dtos.AdminSugerenciaDTOResponse;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTORequest;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTOResponse;
import dev.andrea.acompaname_backend.suggestion.mappers.SugerenciaMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;

@Service
public class SugerenciaServiceImpl implements SugerenciaService {

    private final SugerenciaRepository repository;
    private final UsuarioRepository usuarioRepository;

    public SugerenciaServiceImpl(SugerenciaRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @Override
    public SugerenciaDTOResponse crear(SugerenciaDTORequest dto) {
        String email = emailLogueado();
        UsuarioEntity usuario = usuarioRepository.findByEmail(email).orElseThrow(
                () -> new UsuarioExceptionNotFound("Usuario no encontrado. Email " + email + " no existe."));
        SugerenciaEntity guardada = repository.save(SugerenciaMapper.toEntity(dto, usuario));
        return SugerenciaMapper.toDTO(guardada);
    }

    @Override
    public List<AdminSugerenciaDTOResponse> listar() {
        return repository.findAllByOrderByFechaDescIdDesc().stream()
                .map(SugerenciaMapper::toAdminDTO)
                .toList();
    }
}
