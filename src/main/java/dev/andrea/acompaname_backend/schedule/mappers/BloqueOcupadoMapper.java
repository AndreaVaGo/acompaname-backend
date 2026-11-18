package dev.andrea.acompaname_backend.schedule.mappers;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoEntity;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTORequest;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTOResponse;

public class BloqueOcupadoMapper {

    private BloqueOcupadoMapper() {
    }

    public static BloqueOcupadoEntity toEntity(BloqueOcupadoDTORequest dto, PerfilCuidadorEntity cuidador) {
        BloqueOcupadoEntity bloque = new BloqueOcupadoEntity();
        bloque.setCuidador(cuidador);
        bloque.setFecha(dto.fecha());
        bloque.setHoraInicio(dto.horaInicio());
        bloque.setHoraFin(dto.horaFin());
        // Se quitan los espacios de los lados; si no queda nada, no se guarda motivo
        String motivo = dto.motivo();
        if (motivo != null) {
            motivo = motivo.trim();
            if (motivo.isEmpty()) {
                motivo = null;
            }
        }
        bloque.setMotivo(motivo);
        return bloque;
    }

    public static BloqueOcupadoDTOResponse toDTO(BloqueOcupadoEntity entity) {
        return new BloqueOcupadoDTOResponse(entity.getId(), entity.getFecha(), entity.getHoraInicio(),
                entity.getHoraFin(), entity.getMotivo());
    }
}
