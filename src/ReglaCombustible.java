public class ReglaCombustible {

    private static final String[] JUSTO_GASOLINA = {
            "Vas justo de gasolina, %.2f vueltas de margen, levanta antes de frenar",
            "Cuidado con el combustible, %.2f vueltas de margen, gestiona el ritmo",
            "Gasolina ajustada, %.2f vueltas de margen, ahorra donde puedas"
    };

    private final float margen; // margen de superávit (en vueltas) por debajo del cual se avisa

    public ReglaCombustible(float margen) {
        this.margen = margen;
    }

    public String evaluar(Estado e) {
        if (e.vueltaActual < 2) return null; // en la vuelta 1 el consumo aún no está calibrado

        if (e.combustibleVueltas < margen) {
            return String.format(Frases.elegir(JUSTO_GASOLINA), e.combustibleVueltas);
        }
        return null;
    }
}
