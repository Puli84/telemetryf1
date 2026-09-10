import java.util.HashMap;
import java.util.Map;

public class Locutor {

    private final Map<String, Long> ultimaVez = new HashMap<>();

    public void decir(String clave, String texto, long cooldownMs) {
        long ahora = System.currentTimeMillis();
        Long anterior = ultimaVez.get(clave);

        if (anterior != null && ahora - anterior < cooldownMs) {
            return;
        }

        ultimaVez.put(clave, ahora);
        System.out.println(">> " + texto);
        hablar(texto);
    }

    private void hablar(String texto) {
        String limpio = texto.replace("'", "");
        String cmd = "Add-Type -AssemblyName System.Speech; "
                + "$v = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$v.SelectVoice('Microsoft Helena Desktop'); "
                + "$v.Speak('" + limpio + "')";
        try {
            new ProcessBuilder("powershell", "-Command", cmd).start();
        } catch (Exception ex) {
            System.err.println("Error de voz: " + ex.getMessage());
        }
    }
}