package crewchief;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// Pantalla "Circuito" del dashboard (paso 4). A diferencia de HistorialCircuitos
// (un único mejor tiempo por circuito, guardado aparte), esto recorre TODOS los
// sesiones/*.json de un circuito para sacar la vuelta teórica: el mejor S1, S2 y
// S3 de siempre, aunque cada uno sea de una vuelta o sesión distinta.
@Controller
public class CircuitoController {

    private final SesionesRepositorio repositorio = new SesionesRepositorio();

    @GetMapping("/circuito/{id}")
    public String ver(@PathVariable int id, Model model) {
        model.addAttribute("circuitoId", id);
        model.addAttribute("nombreCircuito", Circuitos.nombre(id));
        model.addAttribute("circuitosConDatos", circuitosConDatos());

        List<SesionesRepositorio.SesionArchivo> sesiones = repositorio.deCircuito(id);
        if (sesiones.isEmpty()) {
            model.addAttribute("sinDatos", true);
            return "circuito";
        }

        RecordVuelta mejorVuelta = null;
        RecordSector mejorS1 = null, mejorS2 = null, mejorS3 = null;

        for (SesionesRepositorio.SesionArchivo s : sesiones) {
            String fecha = s.datos().path("fecha").asText("");
            for (JsonNode v : s.datos().path("vueltas")) {
                if (!v.path("valida").asBoolean(false)) continue;

                int numero = v.path("numero").asInt();
                int tiempo = v.path("tiempoMs").asInt();
                int s1 = v.path("sector1Ms").asInt();
                int s2 = v.path("sector2Ms").asInt();
                int s3 = v.path("sector3Ms").asInt();

                if (tiempo > 0 && (mejorVuelta == null || tiempo < mejorVuelta.tiempoMs())) {
                    mejorVuelta = new RecordVuelta(tiempo, s.archivo(), fecha);
                }
                if (s1 > 0 && (mejorS1 == null || s1 < mejorS1.tiempoMs()))
                    mejorS1 = new RecordSector("Sector 1", s1, tiempoSectorStr(s1), numero, s.archivo(), fecha);
                if (s2 > 0 && (mejorS2 == null || s2 < mejorS2.tiempoMs()))
                    mejorS2 = new RecordSector("Sector 2", s2, tiempoSectorStr(s2), numero, s.archivo(), fecha);
                if (s3 > 0 && (mejorS3 == null || s3 < mejorS3.tiempoMs()))
                    mejorS3 = new RecordSector("Sector 3", s3, tiempoSectorStr(s3), numero, s.archivo(), fecha);
            }
        }

        if (mejorVuelta == null) {
            model.addAttribute("sinDatos", true);
            return "circuito";
        }

        // Puede faltar algún sector (p.ej. una vuelta con datos incompletos) aunque
        // sí haya "mejor vuelta" total — no asumir que siempre vienen los tres juntos.
        List<RecordSector> sectoresDisponibles = Stream.of(mejorS1, mejorS2, mejorS3)
                .filter(Objects::nonNull)
                .toList();
        boolean teoricaCompleta = mejorS1 != null && mejorS2 != null && mejorS3 != null;

        model.addAttribute("mejorVuelta", mejorVuelta);
        model.addAttribute("mejorVueltaStr", ReglaVuelta.formatearTiempo(mejorVuelta.tiempoMs()));
        model.addAttribute("sectores", sectoresDisponibles);
        model.addAttribute("teoricaCompleta", teoricaCompleta);
        if (teoricaCompleta) {
            int sumaTeorica = mejorS1.tiempoMs() + mejorS2.tiempoMs() + mejorS3.tiempoMs();
            model.addAttribute("sumaTeoricaStr", ReglaVuelta.formatearTiempo(sumaTeorica));
            model.addAttribute("deltaStr", formatearDelta(mejorVuelta.tiempoMs() - sumaTeorica));
        }
        model.addAttribute("setup", buscarSetup(mejorVuelta.archivo()));

        return "circuito";
    }

    // Circuitos con al menos una sesión guardada, para el selector de pills.
    private List<CircuitoConDatos> circuitosConDatos() {
        List<CircuitoConDatos> lista = new ArrayList<>();
        List<Integer> ids = repositorio.todas().stream()
                .map(s -> s.datos().path("circuito").asInt(-1))
                .distinct()
                .toList();
        for (int id : ids) {
            lista.add(new CircuitoConDatos(id, Circuitos.nombre(id)));
        }
        lista.sort(Comparator.comparing(CircuitoConDatos::nombre));
        return lista;
    }

    private JsonNode buscarSetup(String archivo) {
        return repositorio.porArchivo(archivo)
                .map(SesionesRepositorio.SesionArchivo::datos)
                .map(n -> n.path("setup"))
                .orElse(null);
    }

    private String formatearDelta(int diffMs) {
        float seg = diffMs / 1000f;
        return String.format("%.3fs", seg);
    }

    private String tiempoSectorStr(int ms) {
        return String.format("%.3f", ms / 1000f);
    }

    public record RecordVuelta(int tiempoMs, String archivo, String fecha) {}

    public record RecordSector(String nombre, int tiempoMs, String tiempoStr, int vuelta, String archivo, String fecha) {}

    public record CircuitoConDatos(int id, String nombre) {}
}
