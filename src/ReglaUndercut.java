public class ReglaUndercut {

    private static final int HUECO_MAX_MS = 3500;    // hueco lo bastante corto como para plantear el undercut
    private static final int GOMAS_MIN_VUELTAS = 8;  // a partir de aquí las gomas ya rinden menos

    private boolean avisado = false;

    public String evaluar(Estado e) {
        if (e.tyresAgeLaps == 0) {
            avisado = false; // gomas nuevas: nuevo stint, se puede volver a avisar
        }

        if (avisado) return null;
        if (e.pitStatus != 0) return null; // ya está entrando o en boxes

        if (e.tyresAgeLaps >= GOMAS_MIN_VUELTAS
                && e.deltaCarDelanteMs > 0
                && e.deltaCarDelanteMs <= HUECO_MAX_MS) {
            avisado = true;
            float segundos = e.deltaCarDelanteMs / 1000f;
            return String.format(
                    "Hueco de %.1f segundos con el de delante, gomas con %d vueltas, valora el undercut",
                    segundos, e.tyresAgeLaps);
        }
        return null;
    }
}
