package dev.andrea.acompaname_backend.auth;

import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Segundo paso del inicio de sesión: un código de 6 cifras que se manda por correo
public interface DosPasosService {

    // Crea un código para esa persona, se lo manda por correo y devuelve el "desafío",
    // que el front tendrá que enviar de vuelta junto con el código
    String iniciar(UsuarioEntity usuario);

    // Comprueba el desafío y el código. Si valen, devuelve a la persona (y el código no se puede volver a usar).
    // Si no, lanza CodigoInvalidoException
    UsuarioEntity verificar(String desafio, String codigo);

    // Activa o desactiva el segundo paso. Hay que escribir la contraseña actual para poder cambiarlo
    void cambiarEstado(Long usuarioId, boolean activar, String password);
}
