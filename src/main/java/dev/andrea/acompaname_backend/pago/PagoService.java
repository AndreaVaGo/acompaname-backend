package dev.andrea.acompaname_backend.pago;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTOResponse;

public interface PagoService
        extends InterfaceGenericService<PagoEntity, PagoDTORequest, PagoDTOResponse> {

    PagoDTOResponse marcarComoPagado(Long id);
}