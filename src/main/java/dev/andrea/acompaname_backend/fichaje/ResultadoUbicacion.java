package dev.andrea.acompaname_backend.fichaje;

// Qué se sabe de dónde estaba la persona cuidadora al fichar. Nunca se guardan coordenadas, solo este resultado:
// - EN_EL_LUGAR: estaba a menos del radio permitido de la dirección del servicio
// - FUERA_DEL_LUGAR: estaba más lejos del radio permitido
// - SIN_UBICACION: no mandó ubicación (denegó el permiso o su móvil no la da). El fichaje se guarda igual
// - NO_VERIFICABLE: mandó ubicación, pero la dirección del servicio no se pudo situar en el mapa
public enum ResultadoUbicacion {
    EN_EL_LUGAR, FUERA_DEL_LUGAR, SIN_UBICACION, NO_VERIFICABLE
}
