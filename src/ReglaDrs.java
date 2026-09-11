public class ReglaDrs {

    private static final String[] PREPARA = {
            "Prepara DRS",
            "Zona DRS a la vista",
            "DRS a la vista, prepárate"
    };

    private boolean avisado = false;

    public String evaluar(Estado e) {
        // Avisa una sola vez, lo antes posible (en cuanto aparece la cuenta atrás),
        // porque para cuando se dice "DRS en X metros" o "disponible" ya se pasó la zona.
        if (e.drsMetros > 0 && !avisado) {
            avisado = true;
            return Frases.elegir(PREPARA);
        }

        if (e.drsMetros == 0) {
            avisado = false;
        }

        return null;
    }
}
