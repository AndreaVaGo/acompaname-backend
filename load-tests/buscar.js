// Escenario 1: buscar personas cuidadoras (es público, no hace falta sesión).
// Cada persona virtual pide la lista, abre un perfil y mira qué cuidadores están ocupados mañana.
import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, etapas, fechaEnDias, idsDeCuidadores } from "./lib/comun.js";

export const options = {
  stages: etapas(),
  thresholds: {
    // Menos del 1% de fallos y el 95% de las respuestas en menos de 800 ms
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"],
  },
};

// Antes de empezar se piden los ids de los cuidadores, para poder abrir perfiles
export function setup() {
  return { ids: idsDeCuidadores() };
}

export default function (datos) {
  const lista = http.get(BASE_URL + "/cuidadores", { tags: { paso: "lista" } });
  check(lista, { "la lista responde 200": (r) => r.status === 200 });

  if (datos.ids.length > 0) {
    const id = datos.ids[Math.floor(Math.random() * datos.ids.length)];
    const perfil = http.get(BASE_URL + "/cuidadores/" + id, { tags: { paso: "perfil" } });
    check(perfil, { "el perfil responde 200": (r) => r.status === 200 });
  }

  const ocupados = http.get(BASE_URL + "/solicitudes/cuidadores-ocupados?inicio=" + fechaEnDias(1), {
    tags: { paso: "ocupados" },
  });
  check(ocupados, { "los ocupados responden 200": (r) => r.status === 200 });

  // Una persona de verdad tarda un poco en mirar la pantalla
  sleep(1);
}
