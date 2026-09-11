public class ReglaVuelta {

    private static final String[] MEJOR_VUELTA = {
            "Vuelta en %s. Mejor vuelta de la sesión",
            "Vuelta en %s, tu mejor de la sesión",
            "Vuelta en %s. Nueva mejor vuelta"
    };

    private static final String[] COMPARADA = {
            "Vuelta en %s. %.2f de tu mejor vuelta",
            "Vuelta en %s, a %.2f de tu mejor",
            "Vuelta en %s. Pierdes %.2f con tu mejor vuelta"
    };

    private int vueltaAnterior = -1;
    private int mejor = 0;
    private boolean nuevoMejorSesion = false;

    public String evaluar(Estado e) {
        nuevoMejorSesion = false;
        if (e.vueltaActual == vueltaAnterior) return null;
        vueltaAnterior = e.vueltaActual;

        int t = e.ultimaVueltaMs;
        if (t <= 0) return null;          // primera vuelta, aún no hay tiempo

        String tiempo = formatearTiempo(t);

        if (e.ultimaVueltaInvalida) {
            return "Vuelta en " + tiempo + ", invalidada";
        }

        if (mejor == 0 || t < mejor) {
            mejor = t;
            nuevoMejorSesion = true;
            return String.format(Frases.elegir(MEJOR_VUELTA), tiempo);
        }

        float diff = (t - mejor) / 1000f;
        return String.format(Frases.elegir(COMPARADA), tiempo, diff);
    }

    public int getMejor() {
        return mejor;
    }

    // true justo después de una llamada a evaluar() que ha marcado nueva mejor vuelta de sesión.
    public boolean esNuevoMejorSesion() {
        return nuevoMejorSesion;
    }

    public static String formatearTiempo(int ms) {
        int min = ms / 60000;
        float seg = (ms % 60000) / 1000f;
        return String.format("%d:%06.3f", min, seg);
    }
}
