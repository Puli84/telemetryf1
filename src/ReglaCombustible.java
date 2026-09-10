public class ReglaCombustible {

    private final float margen;

    public ReglaCombustible(float margen) {
        this.margen = margen;
    }

    public String evaluar(Estado e) {
        int faltan = e.vueltasTotales - e.vueltaActual;
        if (e.combustibleVueltas < faltan - margen) {
            return "Vas justo de gasolina, levanta antes de frenar";
        }
        return null;
    }
}