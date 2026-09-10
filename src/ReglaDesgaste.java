public class ReglaDesgaste {

    private static final int UMBRAL_DESGASTE = 85; // % a partir del cual la goma se pinta marrón en el juego

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
            return "Gomas muy desgastadas, cuidado con el grip";
        }
        return null;
    }
}
