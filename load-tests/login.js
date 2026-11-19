// Escenario 2: iniciar sesión.
// OJO: el login está limitado por IP (5 por minuto por defecto). Para esta prueba hay que arrancar el back
// con RATE_LIMIT_MAX_REQUESTS=1000000, porque si no el límite corta la carga a propósito.
import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, FAMILIA_EMAIL, FAMILIA_PASSWORD, etapas, cabecerasJson } from "./lib/comun.js";

export const options = {
  stages: etapas(),
  thresholds: {
    http_req_failed: ["rate<0.01"],
    // El login es más lento porque comprueba la contraseña con BCrypt (a propósito): se permite más tiempo
    http_req_duration: ["p(95)<1500"],
  },
};

export default function () {
  const respuesta = http.post(
    BASE_URL + "/login",
    JSON.stringify({ email: FAMILIA_EMAIL, password: FAMILIA_PASSWORD }),
    cabecerasJson(),
  );
  check(respuesta, {
    "el login responde 200": (r) => r.status === 200,
    "el login devuelve un token": (r) => r.status === 200 && r.json().token !== undefined,
  });
  sleep(1);
}
