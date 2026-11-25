package dev.andrea.acompaname_backend.limpieza;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.auth.CodigoLoginRepository;
import dev.andrea.acompaname_backend.passwordreset.PasswordResetTokenRepository;

@ExtendWith(MockitoExtension.class)
public class LimpiezaDeCaducadosTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 26, 3, 30);

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private CodigoLoginRepository codigoLoginRepository;

    private LimpiezaDeCaducados limpieza;

    @BeforeEach
    void setUp() {
        limpieza = new LimpiezaDeCaducados(tokenRepository, codigoLoginRepository);
    }

    @Test
    void limpiarBorraLosTokensYLosCodigosCaducadosAEsaHora() {
        when(tokenRepository.borrarCaducados(AHORA)).thenReturn(3);
        when(codigoLoginRepository.borrarCaducados(AHORA)).thenReturn(2);

        limpieza.limpiar(AHORA);

        verify(tokenRepository).borrarCaducados(AHORA);
        verify(codigoLoginRepository).borrarCaducados(AHORA);
    }

    @Test
    void limpiarNoTocaNadaMasQueLoCaducado() {
        limpieza.limpiar(AHORA);

        verify(tokenRepository).borrarCaducados(AHORA);
        verify(codigoLoginRepository).borrarCaducados(AHORA);
        verifyNoMoreInteractions(tokenRepository, codigoLoginRepository);
    }

    @Test
    void laTareaProgramadaLimpiaConLaHoraDeAhora() {
        LocalDateTime antes = LocalDateTime.now();

        limpieza.limpiarCadaNoche();

        org.mockito.ArgumentCaptor<LocalDateTime> hora = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tokenRepository).borrarCaducados(hora.capture());
        org.junit.jupiter.api.Assertions.assertFalse(hora.getValue().isBefore(antes));
        verify(codigoLoginRepository).borrarCaducados(hora.getValue());
    }
}
