package dev.andrea.acompaname_backend.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.admin.dtos.AdminPagoCuidadorDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagoMesDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagoTipoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagosResumenDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;

@Service
public class AdminPagosServiceImpl implements AdminPagosService {

    // Un pago pendiente se avisa cuando lleva más de una semana sin cobrar
    private static final int DIAS_PARA_AVISAR = 7;
    // Cuántos meses salen en el análisis por mes (el actual y los cinco anteriores)
    private static final int MESES_EN_EL_ANALISIS = 6;
    // Cuántos cuidadores salen en el top
    private static final int CUIDADORES_EN_EL_TOP = 5;

    private final PagoRepository pagoRepository;

    public AdminPagosServiceImpl(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    // Redondea a un decimal (por ejemplo 66.67 pasa a 66.7)
    private double redondear(double numero) {
        return Math.round(numero * 10.0) / 10.0;
    }

    @Override
    public AdminPagosResumenDTOResponse resumen() {
        List<PagoEntity> pagos = pagoRepository.findAll();
        LocalDate limite = LocalDate.now().minusDays(DIAS_PARA_AVISAR);

        BigDecimal totalCobrado = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        long numeroCobrados = 0;
        long numeroPendientes = 0;
        long numeroCancelados = 0;
        long pendientesMasDeSieteDias = 0;
        BigDecimal importePendienteMasDeSieteDias = BigDecimal.ZERO;

        for (PagoEntity pago : pagos) {
            if (pago.getEstado() == EstadoPago.COMPLETADO) {
                totalCobrado = totalCobrado.add(pago.getImporte());
                numeroCobrados = numeroCobrados + 1;
            } else if (pago.getEstado() == EstadoPago.PENDIENTE) {
                totalPendiente = totalPendiente.add(pago.getImporte());
                numeroPendientes = numeroPendientes + 1;
                // Mismo criterio que el aviso del panel: creado antes de hace una semana
                if (pago.getFecha().isBefore(limite)) {
                    pendientesMasDeSieteDias = pendientesMasDeSieteDias + 1;
                    importePendienteMasDeSieteDias = importePendienteMasDeSieteDias.add(pago.getImporte());
                }
            } else {
                numeroCancelados = numeroCancelados + 1;
            }
        }

        // Los cancelados no cuentan en la media ni en el porcentaje
        long numeroFacturados = numeroCobrados + numeroPendientes;
        BigDecimal importeMedio = BigDecimal.ZERO;
        Double porcentajeCobrado = null;
        if (numeroFacturados > 0) {
            BigDecimal totalFacturado = totalCobrado.add(totalPendiente);
            importeMedio = totalFacturado.divide(BigDecimal.valueOf(numeroFacturados), 2, RoundingMode.HALF_UP);
            porcentajeCobrado = redondear(numeroCobrados * 100.0 / numeroFacturados);
        }

        List<AdminPagoTipoDTOResponse> porTipoCuidado = new ArrayList<>();
        porTipoCuidado.add(pagosDeTipo(pagos, "hospitalario"));
        porTipoCuidado.add(pagosDeTipo(pagos, "domicilio"));

        return new AdminPagosResumenDTOResponse(totalCobrado, totalPendiente, numeroCobrados, numeroPendientes,
                numeroCancelados, importeMedio, porcentajeCobrado, pendientesMasDeSieteDias,
                importePendienteMasDeSieteDias, porTipoCuidado, pagosPorMes(pagos), topCuidadores(pagos));
    }

    // Cuántos pagos y qué importe se han facturado (cobrados y pendientes) en un tipo de cuidado
    private AdminPagoTipoDTOResponse pagosDeTipo(List<PagoEntity> pagos, String tipo) {
        long cuantos = 0;
        BigDecimal importe = BigDecimal.ZERO;
        for (PagoEntity pago : pagos) {
            boolean facturado = pago.getEstado() != EstadoPago.CANCELADO;
            boolean esDelTipo = tipo.equals(pago.getSolicitud().getTipoCuidado());
            if (facturado && esDelTipo) {
                cuantos = cuantos + 1;
                importe = importe.add(pago.getImporte());
            }
        }
        return new AdminPagoTipoDTOResponse(tipo, cuantos, importe);
    }

    // Lo cobrado y lo pendiente de cada uno de los últimos seis meses, del más antiguo al actual
    private List<AdminPagoMesDTOResponse> pagosPorMes(List<PagoEntity> pagos) {
        List<AdminPagoMesDTOResponse> respuesta = new ArrayList<>();
        YearMonth actual = YearMonth.now();

        for (int atras = MESES_EN_EL_ANALISIS - 1; atras >= 0; atras--) {
            YearMonth mes = actual.minusMonths(atras);
            BigDecimal cobrado = BigDecimal.ZERO;
            BigDecimal pendiente = BigDecimal.ZERO;

            for (PagoEntity pago : pagos) {
                if (YearMonth.from(pago.getFecha()).equals(mes)) {
                    if (pago.getEstado() == EstadoPago.COMPLETADO) {
                        cobrado = cobrado.add(pago.getImporte());
                    } else if (pago.getEstado() == EstadoPago.PENDIENTE) {
                        pendiente = pendiente.add(pago.getImporte());
                    }
                }
            }
            respuesta.add(new AdminPagoMesDTOResponse(mes.toString(), cobrado, pendiente));
        }
        return respuesta;
    }

    // Los cinco cuidadores que más han cobrado (solo cuentan los pagos completados)
    private List<AdminPagoCuidadorDTOResponse> topCuidadores(List<PagoEntity> pagos) {
        // Primero, un registro por cada cuidador que haya cobrado algo
        List<AdminPagoCuidadorDTOResponse> todos = new ArrayList<>();
        for (PagoEntity pago : pagos) {
            if (pago.getEstado() == EstadoPago.COMPLETADO) {
                PerfilCuidadorEntity cuidador = pago.getSolicitud().getCuidador();
                String nombre = cuidador.getUsuario().getNombre();

                int posicion = -1;
                for (int i = 0; i < todos.size(); i++) {
                    if (todos.get(i).cuidadorId().equals(cuidador.getId())) {
                        posicion = i;
                    }
                }

                if (posicion == -1) {
                    todos.add(new AdminPagoCuidadorDTOResponse(cuidador.getId(), nombre, 1, pago.getImporte()));
                } else {
                    AdminPagoCuidadorDTOResponse anterior = todos.get(posicion);
                    todos.set(posicion, new AdminPagoCuidadorDTOResponse(cuidador.getId(), nombre,
                            anterior.pagos() + 1, anterior.importeCobrado().add(pago.getImporte())));
                }
            }
        }

        // Después, se quedan los cinco que más han cobrado: cada vuelta busca el mejor de los que quedan
        List<AdminPagoCuidadorDTOResponse> top = new ArrayList<>();
        while (top.size() < CUIDADORES_EN_EL_TOP && !todos.isEmpty()) {
            AdminPagoCuidadorDTOResponse mejor = todos.get(0);
            for (AdminPagoCuidadorDTOResponse cuidador : todos) {
                if (cuidador.importeCobrado().compareTo(mejor.importeCobrado()) > 0) {
                    mejor = cuidador;
                }
            }
            top.add(mejor);
            todos.remove(mejor);
        }
        return top;
    }

    @Override
    public List<AdminPagoDTOResponse> pagos(EstadoPago estado) {
        List<PagoEntity> pagos;
        if (estado == null) {
            pagos = pagoRepository.findAllByOrderByFechaDescIdDesc();
        } else {
            pagos = pagoRepository.findByEstadoOrderByFechaDescIdDesc(estado);
        }

        LocalDate hoy = LocalDate.now();
        List<AdminPagoDTOResponse> respuesta = new ArrayList<>();
        for (PagoEntity pago : pagos) {
            SolicitudEntity solicitud = pago.getSolicitud();
            long dias = ChronoUnit.DAYS.between(pago.getFecha(), hoy);
            respuesta.add(new AdminPagoDTOResponse(pago.getId(), solicitud.getId(),
                    solicitud.getFamilia().getNombre(), solicitud.getCuidador().getUsuario().getNombre(),
                    solicitud.getTipoCuidado(), pago.getImporte(), pago.getEstado(), pago.getFecha(), dias));
        }
        return respuesta;
    }
}