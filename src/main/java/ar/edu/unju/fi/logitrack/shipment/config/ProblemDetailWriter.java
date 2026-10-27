package ar.edu.unju.fi.logitrack.shipment.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Escribe un {@link ProblemDetail} (RFC 7807) en las respuestas de seguridad, para que un 401/403
 * tenga el mismo formato que el resto de los errores del microservicio.
 */
final class ProblemDetailWriter {

    private ProblemDetailWriter() {
    }

    static void escribir(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
