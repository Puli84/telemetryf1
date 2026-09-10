public class ReglaCombustible {

    private final float margen; // margen de superávit (en vueltas) por debajo del cual se avisa

    public ReglaCombustible(float margen) {
        this.margen = margen;
    }

    public String evaluar(Estado e) {
        if (e.vueltaActual < 2) return null; // en la vuelta 1 el consumo aún no está calibrado

        if (e.combustibleVueltas < margen) {
            return String.format(
                    "Vas justo de gasolina, %.2f vueltas de margen, levanta antes de frenar",
                    e.combustibleVueltas);
        }
        return null;
    }
}
