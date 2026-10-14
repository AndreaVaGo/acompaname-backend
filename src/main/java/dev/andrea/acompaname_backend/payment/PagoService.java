package dev.andrea.acompaname_backend.payment;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;

public interface PagoService
        extends InterfaceGenericService<PagoEntity, PagoDTORequest, PagoDTOResponse> {

    PagoDTOResponse marcarComoPagado(Long id);
}