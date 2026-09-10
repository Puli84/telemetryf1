public class ReglaVuelta {

    private int vueltaAnterior = -1;
    private int mejor = 0;

    public String evaluar(Estado e) {
        if (e.vueltaActual == vueltaAnterior) return null;
        vueltaAnterior = e.vueltaActual;

        int t = e.ultimaVueltaMs;
        if (t <= 0) return null;          // primera vuelta, aún no hay tiempo

        String tiempo = formatearTiempo(t);

        if (mejor == 0 || t < mejor) {
            mejor = t;
            return "Vuelta en " + tiempo + ". Mejor vuelta de la sesión";
        }

        float diff = (t - mejor) / 1000f;
        return "Vuelta en " + tiempo + ". " + String.format("%.2f", diff) + " de tu mejor vuelta" ;
    }

    public int getMejor() {
        return mejor;
    }

    public static String formatearTiempo(int ms) {
        int min = ms / 60000;
        float seg = (ms % 60000) / 1000f;
        return String.format("%d:%06.3f", min, seg);
    }
}