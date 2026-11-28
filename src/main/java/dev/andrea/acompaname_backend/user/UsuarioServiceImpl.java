package dev.andrea.acompaname_backend.user;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.user.dtos.ActualizarContactoDTO;
import dev.andrea.acompaname_backend.user.dtos.CambiarPasswordDTO;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTOResponse;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionEmailDuplicado;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionPasswordInvalida;
import dev.andrea.acompaname_backend.user.mappers.UsuarioMapper;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;
    private final RoleRepository roleRepository;
    private final PerfilCuidadorRepository perfilCuidadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AnonimizadorDeCuenta anonimizador;

    public UsuarioServiceImpl(UsuarioRepository repository, RoleRepository roleRepository,
            PerfilCuidadorRepository perfilCuidadorRepository, PasswordEncoder passwordEncoder,
            AnonimizadorDeCuenta anonimizador) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.anonimizador = anonimizador;
    }

    private UsuarioEntity findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UsuarioExceptionNotFound("Usuario no encontrado. Id " + id + " no existe."));
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void verificarPropietario(UsuarioEntity usuario) {
        if (!usuario.getEmail().equals(emailLogueado())) {
            throw new UsuarioExceptionAccesoDenegado("No tiene permiso para acceder a este usuario");
        }
    }

    @Override
    public List<UsuarioDTOResponse> getEntities() {
        // Cada usuario solo ve sus propios datos, no los de los demás
        return repository.findByEmail(emailLogueado()).stream()
                .map(UsuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDTOResponse getById(Long id) {
        UsuarioEntity usuario = findEntityById(id);
        verificarPropietario(usuario);
        return UsuarioMapper.toDTO(usuario);
    }

    @Transactional
    @Override
    public UsuarioDTOResponse storeEntity(UsuarioDTORequest dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new UsuarioExceptionEmailDuplicado(
                    "No ha sido posible completar el registro con los datos proporcionados.");
        }
        Set<RoleEntity> roles = dto.rolesIds().stream()
                .map(id -> roleRepository.findById(id).orElseThrow(
                        () -> new UsuarioExceptionNotFound("Rol no encontrado. Id " + id + " no existe.")))
                .collect(Collectors.toSet());
        // El rol ADMIN no se puede pedir al registrarse: el administrador se crea al arrancar la aplicación
        for (RoleEntity rol : roles) {
            if (rol.getName().equals("ADMIN")) {
                throw new UsuarioExceptionAccesoDenegado("No está permitido registrarse con el rol ADMIN");
            }
        }
        UsuarioEntity usuarioToSave = UsuarioMapper.toEntity(dto, roles);
        usuarioToSave.setPassword(passwordEncoder.encode(dto.password()));
        UsuarioEntity usuarioSaved;
        try {
            usuarioSaved = repository.save(usuarioToSave);
        } catch (DataIntegrityViolationException e) {
            // Dos registros a la vez con el mismo email: el segundo lo frena la base de datos
            throw new UsuarioExceptionEmailDuplicado(
                    "No ha sido posible completar el registro con los datos proporcionados.");
        }

        boolean esCuidador = roles.stream()
                .anyMatch(role -> role.getName().equals("CUIDADOR"));
        if (esCuidador) {
            PerfilCuidadorEntity perfilNuevo = PerfilCuidadorEntity.builder()
                    .especialidad("")
                    .anosExperiencia(0)
                    .tarifaHora(BigDecimal.ZERO)
                    .bio("")
                    .tieneVehiculo(false)
                    .disponibleAhora(false)
                    .usuario(usuarioSaved)
                    .build();
            perfilCuidadorRepository.save(perfilNuevo);
        }

        return UsuarioMapper.toDTO(usuarioSaved);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        UsuarioEntity usuario = findEntityById(id);
        verificarPropietario(usuario);
        // No se borra la fila: la otra parte de sus solicitudes y pagos conserva su historial
        anonimizador.anonimizar(usuario);
    }

    @Transactional
    @Override
    public UsuarioDTOResponse update(Long id, UsuarioDTORequest dto) {
        UsuarioEntity usuarioExistente = findEntityById(id);
        verificarPropietario(usuarioExistente);
        boolean cambiaDeEmail = !usuarioExistente.getEmail().equals(dto.email());
        if (cambiaDeEmail && repository.findByEmail(dto.email()).isPresent()) {
            throw new UsuarioExceptionEmailDuplicado(
                    "No ha sido posible completar la actualización con los datos proporcionados.");
        }
        // Solo se actualizan los datos de contacto. Los roles no se tocan nunca (se eligen
        // al registrarse) y la contraseña solo se cambia con cambiarPassword.
        usuarioExistente.setNombre(dto.nombre());
        usuarioExistente.setEmail(dto.email());
        usuarioExistente.setTelefono(dto.telefono());
        UsuarioEntity usuarioActualizado = guardarYComprobarEmail(usuarioExistente);
        return UsuarioMapper.toDTO(usuarioActualizado);
    }

    @Transactional
    @Override
    public void cambiarPassword(Long id, CambiarPasswordDTO dto) {
        UsuarioEntity usuario = findEntityById(id);
        verificarPropietario(usuario);
        if (!passwordEncoder.matches(dto.passwordActual(), usuario.getPassword())) {
            throw new UsuarioExceptionPasswordInvalida("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(dto.passwordNueva(), usuario.getPassword())) {
            throw new UsuarioExceptionPasswordInvalida("La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(dto.passwordNueva()));
        // Las sesiones abiertas (por ejemplo, la de alguien que robó el token) dejan de valer
        usuario.cerrarSesiones();
        repository.save(usuario);
    }

    @Transactional
    @Override
    public UsuarioDTOResponse actualizarContacto(Long id, ActualizarContactoDTO dto) {
        UsuarioEntity usuario = findEntityById(id);
        verificarPropietario(usuario);
        boolean cambiaDeEmail = !usuario.getEmail().equals(dto.email());
        if (cambiaDeEmail && repository.findByEmail(dto.email()).isPresent()) {
            throw new UsuarioExceptionEmailDuplicado(
                    "No ha sido posible completar la actualización con los datos proporcionados.");
        }
        usuario.setNombre(dto.nombre());
        usuario.setEmail(dto.email());
        usuario.setTelefono(dto.telefono());
        return UsuarioMapper.toDTO(guardarYComprobarEmail(usuario));
    }

    // flush() fuerza el UPDATE ahora (y no al final de la transacción) para poder traducir el error
    private UsuarioEntity guardarYComprobarEmail(UsuarioEntity usuario) {
        try {
            UsuarioEntity guardado = repository.save(usuario);
            repository.flush();
            return guardado;
        } catch (DataIntegrityViolationException e) {
            throw new UsuarioExceptionEmailDuplicado(
                    "No ha sido posible completar la actualización con los datos proporcionados.");
        }
    }
}