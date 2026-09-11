public class ReglaSectores {

    private static final String[] MEJORADO = {
            "%s mejorado",
            "%s, tu mejor parcial",
            "%s mejorado, buen ritmo"
    };

    private static final String[] MAS_LENTO = {
            "%s, %.2f más lento",
            "%s, pierdes %.2f",
            "%s a %.2f de tu mejor"
    };

    private int sectorAnterior = -1;
    private int[] mejores = { 0, 0, 0 };   // mejor S1, S2 y S3 de la sesión de hoy

    public String evaluar(Estado e) {
        if (e.sector == sectorAnterior) return null;

        int cerrado = sectorAnterior;   // el que acaba de terminar
        sectorAnterior = e.sector;

        if (cerrado == 0) return comparar(0, e.sector1Ms, "Sector 1", e.currentLapInvalid == 1);
        if (cerrado == 1) return comparar(1, e.sector2Ms, "Sector 2", e.currentLapInvalid == 1);
        if (cerrado == 2) {
            int s3 = e.ultimaVueltaMs - e.sector1Ms - e.sector2Ms; // no viene directo en el paquete
            return comparar(2, s3, "Sector 3", e.ultimaVueltaInvalida);
        }
        return null;
    }

    private String comparar(int idx, int tiempo, String nombre, boolean invalido) {
        System.out.println("DEBUG " + nombre + ": tiempo=" + tiempo + "ms, mejorGuardado=" + mejores[idx] + "ms, invalido=" + invalido);

        if (tiempo <= 0) return null;
        if (invalido) return null; // no cuenta como referencia si viene de una vuelta invalidada

        if (mejores[idx] == 0 || tiempo < mejores[idx]) {
            mejores[idx] = tiempo;
            return String.format(Frases.elegir(MEJORADO), nombre);
        }

        float diff = (tiempo - mejores[idx]) / 1000f;
        return String.format(Frases.elegir(MAS_LENTO), nombre, diff);
    }
}
