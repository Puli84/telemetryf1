package crewchief;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Punto de partida del dashboard (paso 4 del roadmap): lista lo que ya hay
// guardado en sesiones/ (RegistradorSesion.java). Sin las pantallas del
// mockup todavía, solo confirma que la web sirve datos reales.
@Controller
public class SesionesController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HistorialCircuitos historial = new HistorialCircuitos();

    @GetMapping("/")
    public String listar(Model model) {
        model.addAttribute("sesiones", cargarResumenes());
        model.addAttribute("historial", historial.todos());
        return "sesiones";
    }

    private List<SesionResumen> cargarResumenes() {
        List<SesionResumen> resultado = new ArrayList<>();
        Path dir = Path.of("sesiones");
        if (!Files.isDirectory(dir)) return resultado;

        try (DirectoryStream<Path> archivos = Files.newDirectoryStream(dir, "*.json")) {
            for (Path archivo : archivos) {
                try {
                    JsonNode raiz = mapper.readTree(archivo.toFile());
                    resultado.add(new SesionResumen(
                            archivo.getFileName().toString(),
                            raiz.path("nombreCircuito").asText("?"),
                            raiz.path("fecha").asText("?"),
                            raiz.path("tipoSesion").asInt(-1),
                            raiz.path("vueltas").size()
                    ));
                } catch (IOException ex) {
                    System.err.println("No se pudo leer " + archivo + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("No se pudo listar sesiones/: " + ex.getMessage());
        }

        resultado.sort(Comparator.comparing(SesionResumen::archivo).reversed());
        return resultado;
    }

    public record SesionResumen(String archivo, String circuito, String fecha, int tipoSesion, int numVueltas) {}
}
