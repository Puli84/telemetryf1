package crewchief;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Guarda un JSON por sesión con el historial completo de vueltas (no solo la mejor),
// para alimentar en el futuro la web (paso 4 del roadmap). Sin Maven/Jackson todavía,
// así que el JSON se serializa a mano; el formato sigue el esquema documentado en el README.
public class RegistradorSesion {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HHmmss");

    private final List<VueltaRegistro> vueltas = new ArrayList<>();
    // Fijados en la primera escritura, para que todas las reescrituras vayan al mismo fichero.
    private String archivo;
    private String fechaSesion;

    private static final class VueltaRegistro {
        final int numero, tiempoMs, sector1Ms, sector2Ms, sector3Ms, compuesto, vueltasGoma;
        final boolean valida;

        VueltaRegistro(int numero, int tiempoMs, int sector1Ms, int sector2Ms, int sector3Ms,
                       boolean valida, int compuesto, int vueltasGoma) {
            this.numero = numero;
            this.tiempoMs = tiempoMs;
            this.sector1Ms = sector1Ms;
            this.sector2Ms = sector2Ms;
            this.sector3Ms = sector3Ms;
            this.valida = valida;
            this.compuesto = compuesto;
            this.vueltasGoma = vueltasGoma;
        }
    }

    // Llamar justo cuando se detecta el cambio de vuelta, con el número de la vuelta
    // que ACABA de cerrarse (no la nueva) y el Estado ya actualizado con sus datos.
    // Guarda a disco en el momento, para no perder nada si la sesión termina de golpe
    // (cierre del juego, pérdida de conexión) sin que llegue nunca un evento de fin.
    public void registrarVuelta(Estado e, int numero, int compuesto) {
        if (e.ultimaVueltaMs <= 0) return;
        int s3 = e.ultimaVueltaMs - e.sector1Ms - e.sector2Ms;
        vueltas.add(new VueltaRegistro(numero, e.ultimaVueltaMs, e.sector1Ms, e.sector2Ms, s3,
                !e.ultimaVueltaInvalida, compuesto, e.tyresAgeLaps));
        guardar(e);
    }

    // Escribe (o reescribe) el JSON con lo acumulado hasta ahora. Se puede llamar
    // varias veces sin problema — siempre va al mismo fichero de esta sesión.
    public void guardar(Estado e) {
        if (vueltas.isEmpty()) return;

        try {
            Path dir = Path.of("sesiones");
            Files.createDirectories(dir);

            if (archivo == null) {
                LocalDateTime ahora = LocalDateTime.now();
                String nombreCircuito = Circuitos.nombre(e.trackId);
                String slug = nombreCircuito.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-+|-+$)", "");
                archivo = ahora.format(FECHA) + "_" + ahora.format(HORA) + "_" + slug + ".json";
                fechaSesion = ahora.format(FECHA);
            }

            String json = construirJson(e, Circuitos.nombre(e.trackId), fechaSesion);
            Files.writeString(dir.resolve(archivo), json, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("No se pudo guardar la sesión: " + ex.getMessage());
        }
    }

    private String construirJson(Estado e, String nombreCircuito, String fecha) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"circuito\": ").append(e.trackId).append(",\n");
        sb.append("  \"nombreCircuito\": \"").append(esc(nombreCircuito)).append("\",\n");
        sb.append("  \"fecha\": \"").append(fecha).append("\",\n");
        sb.append("  \"tipoSesion\": ").append(e.tipoSesion).append(",\n");
        sb.append("  \"tempPista\": ").append(e.trackTemp).append(",\n");
        sb.append("  \"tempAire\": ").append(e.airTemp).append(",\n");
        sb.append("  \"clima\": ").append(e.weather).append(",\n");
        sb.append("  \"setup\": {\n");
        sb.append("    \"aleronDelantero\": ").append(e.aleronDelantero).append(",\n");
        sb.append("    \"aleronTrasero\": ").append(e.aleronTrasero).append(",\n");
        sb.append("    \"difEnAcelerador\": ").append(e.difEnAcelerador).append(",\n");
        sb.append("    \"repartoFreno\": ").append(e.repartoFreno).append(",\n");
        sb.append("    \"presionFreno\": ").append(e.presionFreno).append("\n");
        sb.append("  },\n");
        sb.append("  \"vueltas\": [\n");
        for (int i = 0; i < vueltas.size(); i++) {
            VueltaRegistro v = vueltas.get(i);
            sb.append("    { \"numero\": ").append(v.numero)
              .append(", \"tiempoMs\": ").append(v.tiempoMs)
              .append(", \"sector1Ms\": ").append(v.sector1Ms)
              .append(", \"sector2Ms\": ").append(v.sector2Ms)
              .append(", \"sector3Ms\": ").append(v.sector3Ms)
              .append(", \"valida\": ").append(v.valida)
              .append(", \"compuesto\": ").append(v.compuesto)
              .append(", \"vueltasGoma\": ").append(v.vueltasGoma)
              .append(" }");
            sb.append(i < vueltas.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
