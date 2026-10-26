package dev.andrea.acompaname_backend.admin;

import java.util.List;

import dev.andrea.acompaname_backend.admin.dtos.AdminPagoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagosResumenDTOResponse;
import dev.andrea.acompaname_backend.payment.EstadoPago;

public interface AdminPagosService {

    // Resumen y análisis de los pagos: totales, porcentaje cobrado, por tipo, por mes y top de cuidadores
    AdminPagosResumenDTOResponse resumen();

    // Todos los pagos del más reciente al más antiguo, o solo los de un estado si se indica (null = todos)
    List<AdminPagoDTOResponse> pagos(EstadoPago estado);
}