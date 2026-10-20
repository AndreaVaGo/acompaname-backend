package dev.andrea.acompaname_backend.admin.dtos;

// Un usuario tal como lo ve el administrador en el panel (nunca incluye la contraseña)
public record AdminUsuarioDTOResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String rol,
        boolean activo) {

}