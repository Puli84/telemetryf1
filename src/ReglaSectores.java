public class ReglaSectores {

    private int sectorAnterior = -1;
    private int[] mejores = { 0, 0 };   // mejor S1 y S2 de la sesión

    public String evaluar(Estado e) {
        if (e.sector == sectorAnterior) return null;

        int cerrado = sectorAnterior;   // el que acaba de terminar
        sectorAnterior = e.sector;

        if (cerrado == 0) return comparar(0, e.sector1Ms, "Sector 1");
        if (cerrado == 1) return comparar(1, e.sector2Ms, "Sector 2");
        return null;
    }

    private String comparar(int idx, int tiempo, String nombre) {
        if (tiempo <= 0) return null;

        if (mejores[idx] == 0 || tiempo < mejores[idx]) {
            mejores[idx] = tiempo;
            return nombre + " mejorado";
        }

        int diff = tiempo - mejores[idx];
        return nombre + ", " + String.format("%.2f", diff / 1000f) + " más lento";
    }
}