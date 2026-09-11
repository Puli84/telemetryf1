import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Locutor {

    public static final int PRIORIDAD_NORMAL = 0;
    public static final int PRIORIDAD_ALTA = 10; // se cuela delante de los avisos normales en espera

    private final Map<String, Long> ultimaVez = new HashMap<>();
    private final PriorityBlockingQueue<Mensaje> cola = new PriorityBlockingQueue<>();
    private final AtomicLong contador = new AtomicLong();

    private static final class Mensaje implements Comparable<Mensaje> {
        final String texto;
        final int prioridad;
        final long orden;

        Mensaje(String texto, int prioridad, long orden) {
            this.texto = texto;
            this.prioridad = prioridad;
            this.orden = orden;
        }

        @Override
        public int compareTo(Mensaje otro) {
            int cmp = Integer.compare(otro.prioridad, this.prioridad); // prioridad alta primero
            if (cmp != 0) return cmp;
            return Long.compare(this.orden, otro.orden); // a igual prioridad, FIFO
        }
    }

    public Locutor() {
        Thread hilo = new Thread(this::procesarCola, "locutor-voz");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void decir(String clave, String texto, long cooldownMs) {
        decir(clave, texto, cooldownMs, PRIORIDAD_NORMAL);
    }

    public void decir(String clave, String texto, long cooldownMs, int prioridad) {
        long ahora = System.currentTimeMillis();
        Long anterior = ultimaVez.get(clave);

        if (anterior != null && ahora - anterior < cooldownMs) {
            return;
        }

        ultimaVez.put(clave, ahora);
        System.out.println(">> " + texto);
        cola.offer(new Mensaje(texto, prioridad, contador.getAndIncrement()));
    }

    private void procesarCola() {
        while (true) {
            try {
                Mensaje mensaje = cola.take();
                hablar(mensaje.texto);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void hablar(String texto) {
        String limpio = texto.replace("'", "");
        // "boxes" suena mal con la voz en español (sale algo como "baxis");
        // se reescribe fonéticamente solo para la lectura en voz alta.
        limpio = limpio.replaceAll("(?i)\\bboxes\\b", "bokses");
        String cmd = "Add-Type -AssemblyName System.Speech; "
                + "$v = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$v.SelectVoice('Microsoft Helena Desktop'); "
                + "$v.Speak('" + limpio + "')";
        try {
            Process p = new ProcessBuilder("powershell", "-Command", cmd).start();
            p.waitFor(); // espera a que termine de hablar antes de sacar el siguiente de la cola
        } catch (Exception ex) {
            System.err.println("Error de voz: " + ex.getMessage());
        }
    }
}
