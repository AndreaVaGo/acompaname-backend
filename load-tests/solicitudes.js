// Escenario 3: una familia crea una solicitud, mira su lista y la borra.
// Cada vuelta borra la solicitud que ha creado, así la lista de la familia no crece sin parar
// (como pasaría en la vida real: una familia tiene pocas solicitudes) y la base de datos queda limpia.
// Aun así, hazla solo en local (Docker): si se corta a mitad, pueden quedar algunas solicitudes sin borrar.
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
    http_req_duration: ["p(95)<1500"],
    // Umbral de 1,5 s (igual que login): con 100 usuarios a la vez, el back, MySQL y k6
    // comparten la CPU del portátil. En esta prueba salió p95 = 1,15 s y 0 errores.
    // Por separado, para saber cuál de las peticiones es la lenta
    "http_req_duration{paso:crear}": ["p(95)<1500"],
    "http_req_duration{paso:lista}": ["p(95)<1500"],
    "http_req_duration{paso:borrar}": ["p(95)<1500"],
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

  // Se borra la solicitud creada (solo se pueden borrar las pendientes, y esta lo es). El borrado responde 204
  if (creada.status === 201) {
    const parametrosBorrar = Object.assign({}, parametros, {
      tags: { paso: "borrar" },
      responseCallback: http.expectedStatuses(204),
    });
    const borrada = http.del(BASE_URL + "/solicitudes/" + creada.json("id"), null, parametrosBorrar);
    check(borrada, { "borrar responde 204": (r) => r.status === 204 });
  }

  sleep(1);
}
