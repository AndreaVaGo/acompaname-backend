package dev.andrea.acompaname_backend.payment;

import java.util.List;

import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;

// Los pagos solo se consultan y se pagan: se crean solos al completar la solicitud
public interface PagoService {

    List<PagoDTOResponse> getEntities();

    PagoDTOResponse getById(Long id);

    PagoDTOResponse marcarComoPagado(Long id);
}