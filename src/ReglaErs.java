public class ReglaErs {

    private static final float MAX_ENERGIA = 4_000_000f; // Julios, límite FIA del ERS-K

    // Cada umbral solo se "rearma" cuando el nivel baja lo suficiente por debajo de él,
    // así el ruido de la telemetría alrededor del límite no dispara avisos falsos.
    private boolean armado100 = false;
    private boolean armado80 = false;
    private boolean armado50 = true;
    private boolean armado20 = true;

    public String evaluar(Estado e) {
        int porcentaje = Math.round((e.ersEnergia / MAX_ENERGIA) * 100);

        String mensaje = null;

        if (armado100 && porcentaje >= 100) {
            mensaje = "ERS al cien por cien, listo para adelantamiento";
            armado100 = false;
        } else if (armado80 && porcentaje >= 80) {
            mensaje = "ERS al ochenta por ciento";
            armado80 = false;
        } else if (armado50 && porcentaje <= 50) {
            mensaje = "ERS al cincuenta por ciento";
            armado50 = false;
        } else if (armado20 && porcentaje <= 20) {
            mensaje = "ERS al veinte por ciento, cuidado con la energía";
            armado20 = false;
        }

        if (porcentaje <= 90) armado100 = true;
        if (porcentaje <= 70) armado80 = true;
        if (porcentaje >= 55) armado50 = true;
        if (porcentaje >= 25) armado20 = true;

        return mensaje;
    }
}
