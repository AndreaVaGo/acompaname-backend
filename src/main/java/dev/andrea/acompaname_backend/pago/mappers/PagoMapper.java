
package dev.andrea.acompaname_backend.pago.mappers;

import dev.andrea.acompaname_backend.pago.EstadoPago;
import dev.andrea.acompaname_backend.pago.PagoEntity;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;

public class PagoMapper {

    public static PagoEntity toEntity(PagoDTORequest dto, SolicitudEntity solicitud) {
        PagoEntity pago = new PagoEntity();
        pago.setImporte(dto.importe());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFecha(java.time.LocalDate.now());
        pago.setSolicitud(solicitud);
        return pago;
    }

    public static PagoDTOResponse toDTO(PagoEntity entity) {
        return new PagoDTOResponse(entity.getId(), entity.getImporte(), entity.getEstado(), entity.getFecha(),
                entity.getSolicitud().getId());
    }
}