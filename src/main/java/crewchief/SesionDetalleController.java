package crewchief;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

// Pantalla "Sesión" del dashboard (paso 4): el detalle vuelta a vuelta de un
// único fichero de sesiones/*.json.
@Controller
public class SesionDetalleController {

    private final SesionesRepositorio repositorio = new SesionesRepositorio();

    @GetMapping("/sesion/{archivo}")
    public String ver(@PathVariable String archivo, Model model) {
        var sesionOpt = repositorio.porArchivo(archivo);
        if (sesionOpt.isEmpty()) {
            model.addAttribute("noEncontrada", true);
            return "sesion";
        }

        JsonNode datos = sesionOpt.get().datos();
        model.addAttribute("archivo", archivo);
        model.addAttribute("nombreCircuito", datos.path("nombreCircuito").asText("?"));
        model.addAttribute("fecha", datos.path("fecha").asText("?"));
        model.addAttribute("clima", nombreClima(datos.path("clima").asInt(-1)));
        model.addAttribute("tempPista", datos.path("tempPista").asInt());
        model.addAttribute("tempAire", datos.path("tempAire").asInt());
        model.addAttribute("setup", datos.path("setup"));
        model.addAttribute("vueltas", construirFilas(datos.path("vueltas")));

        return "sesion";
    }

    private List<FilaVuelta> construirFilas(JsonNode vueltasNode) {
        List<FilaVuelta> crudas = new ArrayList<>();
        for (JsonNode v : vueltasNode) {
            crudas.add(new FilaVuelta(
                    v.path("numero").asInt(),
                    v.path("tiempoMs").asInt(),
                    v.path("sector1Ms").asInt(),
                    v.path("sector2Ms").asInt(),
                    v.path("sector3Ms").asInt(),
                    v.path("valida").asBoolean(false),
                    nombreCompuesto(v.path("compuesto").asInt(-1)),
                    v.path("vueltasGoma").asInt()
            ));
        }

        int min = crudas.stream().filter(f -> f.valida).mapToInt(f -> f.tiempoMs).min().orElse(0);
        int max = crudas.stream().filter(f -> f.valida).mapToInt(f -> f.tiempoMs).max().orElse(0);
        int rango = Math.max(max - min, 1);

        for (FilaVuelta f : crudas) {
            f.tiempoStr = ReglaVuelta.formatearTiempo(f.tiempoMs);
            f.s1Str = String.format("%.3f", f.sector1Ms / 1000f);
            f.s2Str = String.format("%.3f", f.sector2Ms / 1000f);
            f.s3Str = String.format("%.3f", f.sector3Ms / 1000f);
            if (!f.valida) {
                f.barPct = 16;
                f.esMejor = false;
            } else {
                f.barPct = Math.max(6, (int) (((max - f.tiempoMs) / (double) rango) * 100));
                f.esMejor = f.tiempoMs == min;
            }
        }
        return crudas;
    }

    private String nombreClima(int codigo) {
        return switch (codigo) {
            case 0 -> "Despejado";
            case 1 -> "Nubes ligeras";
            case 2 -> "Nublado";
            case 3 -> "Lluvia ligera";
            case 4 -> "Lluvia intensa";
            case 5 -> "Tormenta";
            default -> "Desconocido";
        };
    }

    private String nombreCompuesto(int visual) {
        return switch (visual) {
            case 16 -> "Blandos";
            case 17 -> "Medios";
            case 18 -> "Duros";
            case 7 -> "Intermedios";
            case 8 -> "Lluvia";
            default -> "—";
        };
    }

    // Clase (no record: los campos derivados se calculan después de crearla) para
    // que Thymeleaf pueda leer sus getters como propiedades (f.barPct, f.esMejor...).
    public static class FilaVuelta {
        public final int numero, tiempoMs, sector1Ms, sector2Ms, sector3Ms, vueltasGoma;
        public final boolean valida;
        public final String neumatico;
        public String tiempoStr, s1Str, s2Str, s3Str;
        public int barPct;
        public boolean esMejor;

        FilaVuelta(int numero, int tiempoMs, int sector1Ms, int sector2Ms, int sector3Ms,
                   boolean valida, String neumatico, int vueltasGoma) {
            this.numero = numero;
            this.tiempoMs = tiempoMs;
            this.sector1Ms = sector1Ms;
            this.sector2Ms = sector2Ms;
            this.sector3Ms = sector3Ms;
            this.valida = valida;
            this.neumatico = neumatico;
            this.vueltasGoma = vueltasGoma;
        }

        public int getNumero() { return numero; }
        public boolean isValida() { return valida; }
        public String getNeumatico() { return neumatico; }
        public int getVueltasGoma() { return vueltasGoma; }
        public String getTiempoStr() { return tiempoStr; }
        public String getS1Str() { return s1Str; }
        public String getS2Str() { return s2Str; }
        public String getS3Str() { return s3Str; }
        public int getBarPct() { return barPct; }
        public boolean isEsMejor() { return esMejor; }
    }
}
