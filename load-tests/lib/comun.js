// Cosas que comparten los scripts de carga (k6): dirección del back, etapas y funciones de ayuda.
import http from "k6/http";
import { fail } from "k6";

// A dónde se mandan las peticiones. Con k6 en Docker hay que usar host.docker.internal en vez de localhost
export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/api/v1";

// Una persona de la familia que ya existe en la base de datos (para login y para crear solicitudes)
export const FAMILIA_EMAIL = __ENV.FAMILIA_EMAIL || "familia@test.com";
export const FAMILIA_PASSWORD = __ENV.FAMILIA_PASSWORD || "Password123!";

// Las respuestas buenas son 200 y 201. Cualquier otra cuenta como fallo en http_req_failed
http.setResponseCallback(http.expectedStatuses(200, 201));

// PERFIL=rapido: una prueba corta para comprobar que el script funciona (unos 20 segundos).
// Sin PERFIL (o PERFIL=escalera): se sube poco a poco de 10 a 100 personas virtuales a la vez (unos 5 minutos).
export function etapas() {
  if (__ENV.PERFIL === "rapido") {
    return [
      { duration: "10s", target: 5 },
      { duration: "10s", target: 0 },
    ];
  }
  return [
    { duration: "20s", target: 10 },
    { duration: "40s", target: 10 },
    { duration: "20s", target: 25 },
    { duration: "40s", target: 25 },
    { duration: "20s", target: 50 },
    { duration: "40s", target: 50 },
    { duration: "20s", target: 100 },
    { duration: "40s", target: 100 },
    { duration: "20s", target: 0 },
  ];
}

const CABECERAS_JSON = { "Content-Type": "application/json" };

export function cabecerasJson(token) {
  if (!token) {
    return { headers: CABECERAS_JSON };
  }
  return { headers: { "Content-Type": "application/json", Authorization: "Bearer " + token } };
}

// Inicia sesión y devuelve { token, id }. Si falla, la prueba se para con un mensaje claro
export function iniciarSesion(email, password) {
  const respuesta = http.post(
    BASE_URL + "/login",
    JSON.stringify({ email: email, password: password }),
    cabecerasJson(),
  );
  if (respuesta.status !== 200) {
    fail(
      "No se pudo iniciar sesión con " + email + " (estado " + respuesta.status + "). " +
        "¿Existe ese usuario y el back está arrancado con RATE_LIMIT_MAX_REQUESTS alto?",
    );
  }
  const datos = respuesta.json();
  return { token: datos.token, id: datos.id };
}

// Pide la lista de cuidadores y devuelve sus ids. Si no hay ninguno, la prueba se para
export function idsDeCuidadores() {
  const respuesta = http.get(BASE_URL + "/cuidadores");
  if (respuesta.status !== 200) {
    fail(
      "GET " + BASE_URL + "/cuidadores respondió con el estado " + respuesta.status +
        ". ¿Está el back arrancado y es correcta la dirección (BASE_URL)? Respuesta: " + respuesta.body,
    );
  }
  const ids = [];
  for (const cuidador of respuesta.json()) {
    ids.push(cuidador.id);
  }
  if (ids.length === 0) {
    fail("No hay ninguna persona cuidadora en la base de datos: crea al menos una para probar.");
  }
  return ids;
}

// La fecha de dentro de "dias" días, como 2026-12-01
export function fechaEnDias(dias) {
  const fecha = new Date(Date.now() + dias * 24 * 60 * 60 * 1000);
  return fecha.toISOString().slice(0, 10);
}

// Dos dígitos: 7 -> "07"
export function dosDigitos(numero) {
  return String(numero).padStart(2, "0");
}
