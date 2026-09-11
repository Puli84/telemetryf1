import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

// Guarda, por circuito, la mejor vuelta en carrera de siempre (tiempo + fecha),
// para poder consultarla por voz aunque el programa se reinicie.
public class HistorialCircuitos {

    private static final String ARCHIVO = "historial_vueltas.properties";
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"));

    private final Properties datos = new Properties();

    public HistorialCircuitos() {
        try (FileInputStream in = new FileInputStream(ARCHIVO)) {
            datos.load(in);
        } catch (IOException ignored) {
            // primera vez que se usa, todavía no hay archivo
        }
    }

    // Si tiempoMs mejora el récord guardado (o no hay ninguno), lo guarda y devuelve
    // un mensaje para anunciar por voz. Si no mejora, devuelve null.
    public synchronized String registrar(int trackId, int tiempoMs) {
        if (tiempoMs <= 0) return null;

        int anterior = tiempoMsGuardado(trackId);
        if (anterior != -1 && tiempoMs >= anterior) return null;

        String fecha = LocalDate.now().format(FORMATO_FECHA);
        datos.setProperty(clave(trackId), tiempoMs + "," + LocalDate.now());
        guardar();

        String circuito = Circuitos.nombre(trackId);
        return anterior == -1
                ? String.format("Nuevo récord histórico en %s, %s", circuito, ReglaVuelta.formatearTiempo(tiempoMs))
                : String.format("Nuevo récord histórico en %s, %s, mejoras tu marca anterior", circuito, ReglaVuelta.formatearTiempo(tiempoMs));
    }

    // Respuesta hablada para "récord": tiempo y fecha del mejor crono histórico en este circuito.
    public String consultar(int trackId) {
        String valor = datos.getProperty(clave(trackId));
        String circuito = Circuitos.nombre(trackId);
        if (valor == null) {
            return "Todavía no tienes ninguna vuelta registrada en " + circuito;
        }

        String[] partes = valor.split(",", 2);
        int tiempoMs = Integer.parseInt(partes[0]);
        String fecha = partes.length > 1
                ? LocalDate.parse(partes[1]).format(FORMATO_FECHA)
                : "fecha desconocida";

        return String.format("Tu mejor vuelta histórica en %s es %s, del %s",
                circuito, ReglaVuelta.formatearTiempo(tiempoMs), fecha);
    }

    private int tiempoMsGuardado(int trackId) {
        String valor = datos.getProperty(clave(trackId));
        if (valor == null) return -1;
        return Integer.parseInt(valor.split(",", 2)[0]);
    }

    private String clave(int trackId) {
        return "track." + trackId;
    }

    private void guardar() {
        try (FileOutputStream out = new FileOutputStream(ARCHIVO)) {
            datos.store(out, "Historial de mejores vueltas por circuito");
        } catch (IOException ex) {
            System.err.println("No se pudo guardar el historial de vueltas: " + ex.getMessage());
        }
    }
}
