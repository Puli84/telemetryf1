public class ReglaDesgaste {

    private static final int UMBRAL_DESGASTE = 15; // % a partir del cual ya se nota pérdida de rendimiento real

    private static final String[] DESGASTE = {
            "Gomas muy desgastadas, cuidado con el grip",
            "Neumáticos al límite, ten cuidado",
            "Gomas muy gastadas, pierdes agarre"
    };

    private boolean avisado = false;

    public String evaluar(Estado e) {
        if (e.tyresAgeLaps == 0) {
            avisado = false; // gomas nuevas: nuevo stint, se puede volver a avisar
        }

        if (avisado) return null;

        float peor = 0;
        for (float d : e.desgasteGomas) {
            if (d > peor) peor = d;
        }

        if (peor >= UMBRAL_DESGASTE) {
            avisado = true;
            return Frases.elegir(DESGASTE);
        }
        return null;
    }
}
