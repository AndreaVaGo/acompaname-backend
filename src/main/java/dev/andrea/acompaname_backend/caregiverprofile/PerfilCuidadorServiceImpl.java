package dev.andrea.acompaname_backend.caregiverprofile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import dev.andrea.acompaname_backend.review.ValoracionRepository;

@Service
public class PerfilCuidadorServiceImpl implements PerfilCuidadorService {

    private final PerfilCuidadorRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ValoracionRepository valoracionRepository;
    private final FotoValidador fotoValidador;

    public PerfilCuidadorServiceImpl(PerfilCuidadorRepository repository, UsuarioRepository usuarioRepository,
            ValoracionRepository valoracionRepository, FotoValidador fotoValidador) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.valoracionRepository = valoracionRepository;
        this.fotoValidador = fotoValidador;
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

    // Redondea la media a 1 decimal (si no hay media, se queda en null)
    private Double redondearMedia(Double media) {
        if (media == null) {
            return null;
        }
        return Math.round(media * 10) / 10.0;
    }

    // Convierte el perfil a DTO añadiendo la valoración media (1 decimal) y cuántas valoraciones tiene
    private PerfilCuidadorDTOResponse aDTO(PerfilCuidadorEntity perfil) {
        Double media = redondearMedia(valoracionRepository.mediaPorCuidador(perfil.getId()));
        Long numValoraciones = valoracionRepository.countBySolicitudCuidadorId(perfil.getId());
        return PerfilCuidadorMapper.toDTO(perfil, media, numValoraciones);
    }

    // La lista hace siempre 2 consultas, sean 3 cuidadores o 3.000:
    // una para los perfiles (con su usuario) y otra para las valoraciones de todos
    @Override
    public List<PerfilCuidadorDTOResponse> getEntities() {
        List<PerfilCuidadorEntity> perfiles = repository.findAllConUsuario();

        // Guardamos cada fila del resumen según el id del cuidador para encontrarla rápido
        Map<Long, Object[]> resumenes = new HashMap<>();
        for (Object[] fila : valoracionRepository.resumenPorCuidador()) {
            Long cuidadorId = (Long) fila[0];
            resumenes.put(cuidadorId, fila);
        }

        List<PerfilCuidadorDTOResponse> resultado = new ArrayList<>();
        for (PerfilCuidadorEntity perfil : perfiles) {
            Object[] fila = resumenes.get(perfil.getId());
            Double media = null;
            Long numValoraciones = 0L;
            if (fila != null) {
                media = redondearMedia((Double) fila[1]);
                numValoraciones = (Long) fila[2];
            }
            resultado.add(PerfilCuidadorMapper.toDTO(perfil, media, numValoraciones));
        }
        return resultado;
    }

    @Override
    public PerfilCuidadorDTOResponse getById(Long id) {
        PerfilCuidadorEntity perfil = findEntityById(id);
        return aDTO(perfil);
    }

    @Transactional
    @Override
    public PerfilCuidadorDTOResponse storeEntity(PerfilCuidadorDTORequest dto) {
        if (repository.existsByUsuarioId(dto.usuarioId())) {
            throw new PerfilCuidadorExceptionAccesoDenegado("Este usuario ya tiene un perfil cuidador creado");
        }

        UsuarioEntity usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new UsuarioExceptionNotFound(
                        "Usuario no encontrado. Id " + dto.usuarioId() + " no existe."));
        if (!usuario.getEmail().equals(emailLogueado())) {
            throw new PerfilCuidadorExceptionAccesoDenegado("No puede crear un perfil para otro usuario");
        }
        // Si la foto no vale (tipo, tamaño o contenido), se lanza el error y no se guarda nada
        fotoValidador.comprobar(dto.foto());
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
        // Si la foto no vale (tipo, tamaño o contenido), se lanza el error y no se cambia nada
        fotoValidador.comprobar(dto.foto());
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
        // Si no llegan grupos de edad, se dejan los que ya tenía (una lista vacía los borra todos)
        if (dto.gruposEdad() != null) {
            perfilCuidadorExistente.setGruposEdad(PerfilCuidadorMapper.gruposEdadATexto(dto.gruposEdad()));
        }
        // Si no llegan titulaciones, se dejan las que ya tenía (una lista vacía las borra todas)
        if (dto.titulaciones() != null) {
            perfilCuidadorExistente.setTitulaciones(PerfilCuidadorMapper.titulacionesATexto(dto.titulaciones()));
        }
        // Las otras titulaciones se actualizan siempre: si no llegan o vienen vacías, se borran
        perfilCuidadorExistente.setOtrasTitulaciones(
                PerfilCuidadorMapper.otrasTitulacionesONull(dto.otrasTitulaciones()));
        // El recargo propio se actualiza siempre: si no llega, vuelve al recargo por defecto
        perfilCuidadorExistente.setRecargoPersonaExtra(dto.recargoPersonaExtra());
        PerfilCuidadorEntity perfilCuidadorActualizado = repository.save(perfilCuidadorExistente);
        return aDTO(perfilCuidadorActualizado);
    }

    @Override
    public PerfilCuidadorDTOResponse getMiPerfil() {
        String emailLogueado = emailLogueado();

        PerfilCuidadorEntity perfil = repository.findByUsuarioEmail(emailLogueado)
                .orElseThrow(() -> new PerfilCuidadorExceptionNotFound("No tienes un perfil de cuidador creado."));

        return aDTO(perfil);
    }
}