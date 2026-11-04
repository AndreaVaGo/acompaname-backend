package dev.andrea.acompaname_backend.payment.mappers;

import java.math.BigDecimal;

import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;

public class PagoMapper {

    // El importe lo calcula el servidor (tarifa del cuidador); nunca viene del cliente
    public static PagoEntity toEntity(BigDecimal importe, SolicitudEntity solicitud) {
        PagoEntity pago = new PagoEntity();
        pago.setImporte(importe);
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