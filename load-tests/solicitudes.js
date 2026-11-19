// Escenario 3: una familia crea solicitudes y mira su lista.
// OJO: esta prueba GUARDA solicitudes de verdad en la base de datos. Hazla solo en local (Docker) y,
// al acabar, borra los datos de prueba (por ejemplo, con "docker compose down -v" y volviendo a arrancar).
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";
import {
  BASE_URL,
  FAMILIA_EMAIL,
  FAMILIA_PASSWORD,
  etapas,
  cabecerasJson,
  iniciarSesion,
  idsDeCuidadores,
  fechaEnDias,
  dosDigitos,
} from "./lib/comun.js";

const solicitudesCreadas = new Counter("solicitudes_creadas");
const solicitudesRechazadas = new Counter("solicitudes_rechazadas");

export const options = {
  stages: etapas(),
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1000"],
  },
};

// Una sola vez al empezar: se inicia sesión y se elige una persona cuidadora
export function setup() {
  const sesion = iniciarSesion(FAMILIA_EMAIL, FAMILIA_PASSWORD);
  const ids = idsDeCuidadores();
  return { token: sesion.token, familiaId: sesion.id, cuidadorId: ids[0] };
}

// Cada petición usa un día y una hora distintos, para no chocar con las que ya se han creado
function cuerpoDeSolicitud(datos) {
  const numero = __VU * 100000 + __ITER;
  const dia = 30 + (numero % 730);
  const hora = 6 + (Math.floor(numero / 730) % 14);
  return JSON.stringify({
    tipoCuidado: "domicilio",
    nombrePaciente: "Prueba de carga",
    edadPaciente: 80,
    notas: "Solicitud creada por la prueba de carga",
    fechaCuidado: fechaEnDias(dia),
    horaInicio: dosDigitos(hora) + ":00",
    horaFin: dosDigitos(hora + 1) + ":00",
    familiaId: datos.familiaId,
    cuidadorId: datos.cuidadorId,
    necesidades: [],
  });
}

export default function (datos) {
  const parametros = cabecerasJson(datos.token);

  const parametrosCrear = Object.assign({}, parametros, {
    tags: { paso: "crear" },
    // Una solicitud rechazada por una regla del negocio (400/409) no es un fallo del servidor
    responseCallback: http.expectedStatuses(201, 400, 409),
  });
  const creada = http.post(BASE_URL + "/solicitudes", cuerpoDeSolicitud(datos), parametrosCrear);
  if (creada.status === 201) {
    solicitudesCreadas.add(1);
  } else {
    solicitudesRechazadas.add(1);
    if (__ITER < 3) {
      console.warn("Solicitud rechazada (" + creada.status + "): " + creada.body);
    }
  }
  check(creada, { "crear no da error del servidor": (r) => r.status < 500 });

  const parametrosLista = Object.assign({}, parametros, { tags: { paso: "lista" } });
  const lista = http.get(BASE_URL + "/solicitudes/mis-solicitudes", parametrosLista);
  check(lista, { "mis solicitudes responde 200": (r) => r.status === 200 });

  sleep(1);
}
