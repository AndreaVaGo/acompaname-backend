package dev.andrea.acompaname_backend.schedule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;

// Descarga el calendario (.ics) de la dirección que ha pegado la persona cuidadora.
// Como el servidor va a pedir una dirección que escribe un usuario, hay que protegerse (SSRF):
//  - solo https (webcal:// se convierte en https://)
//  - solo servidores de calendarios conocidos (Google, Outlook, iCloud), configurables
//  - no se siguen redirecciones, hay tiempo máximo y tamaño máximo
// Es una clase aparte para poder sustituirla por una falsa en los tests.
@Component
public class DescargadorDeCalendario {

    static final int MAXIMO_URL = 500;
    static final int MAXIMO_BYTES = 500_000;

    private final List<String> hostsPermitidos;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public DescargadorDeCalendario(
            @Value("${calendario.hosts-permitidos:calendar.google.com,outlook.office365.com,outlook.live.com,icloud.com}") String hosts) {
        this.hostsPermitidos = new ArrayList<>();
        for (String host : hosts.split(",")) {
            if (!host.isBlank()) {
                this.hostsPermitidos.add(host.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    // Comprueba la dirección y la devuelve lista para guardar (https). Si no vale, lanza un error 400
    public String normalizar(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            throw new BloqueOcupadoExceptionFechaInvalida("Pega la dirección de tu calendario");
        }
        String url = direccion.trim();
        if (url.regionMatches(true, 0, "webcal://", 0, 9)) {
            url = "https://" + url.substring(9);
        }
        if (url.length() > MAXIMO_URL) {
            throw new BloqueOcupadoExceptionFechaInvalida("La dirección del calendario es demasiado larga");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new BloqueOcupadoExceptionFechaInvalida("La dirección del calendario no es válida");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BloqueOcupadoExceptionFechaInvalida("La dirección del calendario tiene que empezar por https://");
        }
        // Un "usuario@" en la dirección es el truco típico para engañar con el servidor
        if (uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new BloqueOcupadoExceptionFechaInvalida("La dirección del calendario no es válida");
        }
        if (!hostPermitido(uri.getHost())) {
            throw new BloqueOcupadoExceptionFechaInvalida(
                    "Solo se pueden vincular calendarios de Google, Outlook o iCloud");
        }
        return url;
    }

    // El servidor tiene que ser uno de la lista o un subdominio suyo (p12-caldav.icloud.com vale para icloud.com)
    boolean hostPermitido(String host) {
        if (host == null) {
            return false;
        }
        String minusculas = host.toLowerCase(Locale.ROOT);
        for (String permitido : hostsPermitidos) {
            if (minusculas.equals(permitido) || minusculas.endsWith("." + permitido)) {
                return true;
            }
        }
        return false;
    }

    // Descarga el texto del calendario. Cualquier fallo sale como un error 400 con un mensaje claro
    public String descargar(String direccion) {
        String url = normalizar(direccion);
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "text/calendar, text/plain, */*")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream cuerpo = respuesta.body()) {
                if (respuesta.statusCode() != 200) {
                    throw new BloqueOcupadoExceptionFechaInvalida(
                            "El calendario no se pudo leer (respuesta " + respuesta.statusCode()
                                    + "). Comprueba que la dirección sigue siendo válida");
                }
                return leerLimitado(cuerpo);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BloqueOcupadoExceptionFechaInvalida("No se pudo descargar el calendario. Inténtalo más tarde");
        } catch (IOException e) {
            throw new BloqueOcupadoExceptionFechaInvalida("No se pudo descargar el calendario. Inténtalo más tarde");
        }
    }

    // Lee como mucho MAXIMO_BYTES: si hay más, se corta y se avisa (así no se llena la memoria)
    static String leerLimitado(InputStream entrada) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int leidos;
        while ((leidos = entrada.read(buffer)) != -1) {
            salida.write(buffer, 0, leidos);
            if (salida.size() > MAXIMO_BYTES) {
                throw new BloqueOcupadoExceptionFechaInvalida("El calendario es demasiado grande");
            }
        }
        return salida.toString(StandardCharsets.UTF_8);
    }
}
