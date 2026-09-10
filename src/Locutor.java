import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Locutor {

    private final Map<String, Long> ultimaVez = new HashMap<>();
    private final BlockingQueue<String> cola = new LinkedBlockingQueue<>();

    public Locutor() {
        Thread hilo = new Thread(this::procesarCola, "locutor-voz");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void decir(String clave, String texto, long cooldownMs) {
        long ahora = System.currentTimeMillis();
        Long anterior = ultimaVez.get(clave);

        if (anterior != null && ahora - anterior < cooldownMs) {
            return;
        }

        ultimaVez.put(clave, ahora);
        System.out.println(">> " + texto);
        cola.offer(texto);
    }

    private void procesarCola() {
        while (true) {
            try {
                String texto = cola.take();
                hablar(texto);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void hablar(String texto) {
        String limpio = texto.replace("'", "");
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
