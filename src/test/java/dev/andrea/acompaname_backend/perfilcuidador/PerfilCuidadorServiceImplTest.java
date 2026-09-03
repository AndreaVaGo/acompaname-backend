package dev.andrea.acompaname_backend.perfilcuidador;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForThaiBuddhistDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.usuario.Rol;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorServiceImplTest {
    @InjectMocks
    private PerfilCuidadorServiceImpl service;
    @Mock
    private PerfilCuidadorRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setup() {
        service = new PerfilCuidadorServiceImpl(repository, usuarioRepository);
    }

    @Test
    void testGetEntities() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234", Rol.CUIDADOR);
        List<PerfilCuidadorEntity> perfilesMock = List.of(
                new PerfilCuidadorEntity(1L, "Fisioterapia", 5, new BigDecimal("15.00"), "Bio de prueba", true, true,
                        usuario));
        when(repository.findAll()).thenReturn(perfilesMock);
        
        List<PerfilCuidadorEntity> perfiles = service.getEntities();
        
    
    }

}
