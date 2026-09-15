package crewchief;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

// Página de inicio del dashboard (paso 4 del roadmap): lista lo que ya hay
// guardado, tanto en sesiones/ (RegistradorSesion.java) como en el historial
// de mejores vueltas por circuito (HistorialCircuitos.java).
@Controller
public class SesionesController {

    private final SesionesRepositorio repositorio = new SesionesRepositorio();
    private final HistorialCircuitos historial = new HistorialCircuitos();

    @GetMapping("/")
    public String listar(Model model) {
        List<SesionResumen> resumenes = repositorio.todas().stream()
                .map(s -> new SesionResumen(
                        s.archivo(),
                        s.datos().path("circuito").asInt(-1),
                        s.datos().path("nombreCircuito").asText("?"),
                        s.datos().path("fecha").asText("?"),
                        s.datos().path("tipoSesion").asInt(-1),
                        s.datos().path("vueltas").size()))
                .sorted(Comparator.comparing(SesionResumen::archivo).reversed())
                .toList();

        model.addAttribute("sesiones", resumenes);
        model.addAttribute("historial", historial.todos());
        return "sesiones";
    }

    public record SesionResumen(String archivo, int circuitoId, String circuito, String fecha,
                                 int tipoSesion, int numVueltas) {}
}
