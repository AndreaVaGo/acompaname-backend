
package dev.andrea.acompaname_backend.payment.mappers;

import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;
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