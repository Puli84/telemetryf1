public class ReglaErs {

    private static final float MAX_ENERGIA = 4_000_000f; // Julios, límite FIA del ERS-K

    private static final String[] AL_100 = {
            "ERS al cien por cien, listo para adelantamiento",
            "Batería llena, ERS al cien por cien",
            "ERS a tope, listo para el modo adelantamiento"
    };

    private static final String[] AL_80 = {
            "ERS al ochenta por ciento",
            "Batería al ochenta por ciento",
            "ERS recuperado al ochenta por ciento"
    };

    private static final String[] AL_50 = {
            "ERS al cincuenta por ciento",
            "Batería a la mitad",
            "ERS al cincuenta por ciento de carga"
    };

    private static final String[] AL_20 = {
            "ERS al veinte por ciento, cuidado con la energía",
            "Batería baja, ERS al veinte por ciento",
            "ERS al veinte por ciento, gestiona la energía"
    };

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
            mensaje = Frases.elegir(AL_100);
            armado100 = false;
        } else if (armado80 && porcentaje >= 80) {
            mensaje = Frases.elegir(AL_80);
            armado80 = false;
        } else if (armado50 && porcentaje <= 50) {
            mensaje = Frases.elegir(AL_50);
            armado50 = false;
        } else if (armado20 && porcentaje <= 20) {
            mensaje = Frases.elegir(AL_20);
            armado20 = false;
        }

        if (porcentaje <= 90) armado100 = true;
        if (porcentaje <= 70) armado80 = true;
        if (porcentaje >= 55) armado50 = true;
        if (porcentaje >= 25) armado20 = true;

        return mensaje;
    }
}
