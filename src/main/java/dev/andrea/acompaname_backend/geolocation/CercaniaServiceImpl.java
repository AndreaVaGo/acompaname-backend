package dev.andrea.acompaname_backend.geolocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.geolocation.dtos.CercaniaDTOResponse;
import dev.andrea.acompaname_backend.geolocation.dtos.DistanciaCuidadorDTOResponse;

// Privacidad: solo se compara el lugar que escribe la familia con la CIUDAD de cada persona cuidadora.
// No se guarda el lugar buscado ni se usan ni se enseñan direcciones exactas.
@Service
public class CercaniaServiceImpl implements CercaniaService {

    // Un lugar más largo no es una ciudad ni un código postal: se descarta sin consultar nada
    private static final int LONGITUD_MAXIMA_DEL_LUGAR = 80;

    private final PerfilCuidadorRepository perfilRepository;
    private final Geocodificador geocodificador;

    public CercaniaServiceImpl(PerfilCuidadorRepository perfilRepository, Geocodificador geocodificador) {
        this.perfilRepository = perfilRepository;
        this.geocodificador = geocodificador;
    }

    @Override
    public CercaniaDTOResponse buscarCerca(String lugar) {
        if (lugar == null || lugar.isBlank() || lugar.length() > LONGITUD_MAXIMA_DEL_LUGAR) {
            return new CercaniaDTOResponse(false, List.of());
        }

        Optional<Coordenadas> origen = geocodificador.buscar(lugar);
        if (origen.isEmpty()) {
            return new CercaniaDTOResponse(false, List.of());
        }

        List<DistanciaCuidadorDTOResponse> distancias = new ArrayList<>();
        for (PerfilCuidadorEntity perfil : perfilRepository.findAll()) {
            // Quien no ha escrito su ciudad, o una ciudad que no se encuentra, no aparece en la lista
            if (perfil.getCiudad() == null || perfil.getCiudad().isBlank()) {
                continue;
            }
            Optional<Coordenadas> destino = geocodificador.buscar(perfil.getCiudad());
            if (destino.isEmpty()) {
                continue;
            }
            double km = Distancia.enKilometros(origen.get(), destino.get());
            distancias.add(new DistanciaCuidadorDTOResponse(perfil.getId(), redondear(km)));
        }

        distancias.sort(Comparator.comparingDouble(DistanciaCuidadorDTOResponse::km));
        return new CercaniaDTOResponse(true, distancias);
    }

    // A un decimal: más precisión sería falsa, porque se mide de ciudad a ciudad
    private double redondear(double km) {
        return Math.round(km * 10.0) / 10.0;
    }
}
