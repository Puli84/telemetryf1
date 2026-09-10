import java.util.HashMap;
import java.util.Map;

public class Locutor {

    private final Map<String, Long> ultimaVez = new HashMap<>();

    public void decir(String clave, String texto, long cooldownMs) {
        long ahora = System.currentTimeMillis();
        Long anterior = ultimaVez.get(clave);

        if (anterior != null && ahora - anterior < cooldownMs) {
            return;   // dicho hace poco, callar
        }

        ultimaVez.put(clave, ahora);
        System.out.println(">> " + texto);
    }
}