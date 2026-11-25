package dev.andrea.acompaname_backend.geolocation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.geolocation.dtos.CercaniaDTOResponse;
import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import jakarta.servlet.http.HttpServletRequest;

// Para ordenar la búsqueda de cuidadores por cercanía. Es público, como el listado de cuidadores.
// Cada lugar nuevo se consulta a un servicio de internet con un límite de uso, así que se limitan las
// búsquedas por dirección IP (rate-limit.cerca-max-requests por ventana de rate-limit.window-seconds)
@RestController
@RequestMapping(path = "${api-endpoint}/cuidadores/cerca")
public class CercaniaController {

    private final CercaniaService service;
    private final LimitadorDePeticiones limitador;
    private final int maximoDePeticiones;

    public CercaniaController(CercaniaService service, LimitadorDePeticiones limitador,
            @Value("${rate-limit.cerca-max-requests:20}") int maximoDePeticiones) {
        this.service = service;
        this.limitador = limitador;
        this.maximoDePeticiones = maximoDePeticiones;
    }

    @GetMapping("")
    public CercaniaDTOResponse index(@RequestParam(defaultValue = "") String lugar, HttpServletRequest peticion) {
        limitador.comprobar("cerca:" + peticion.getRemoteAddr(), maximoDePeticiones);
        return service.buscarCerca(lugar);
    }
}
