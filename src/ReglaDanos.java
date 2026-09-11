public class ReglaDanos {

    private static final int UMBRAL_DANO = 40; // % de daño a partir del cual conviene entrar a boxes

    private static final String[] ALERON = {
            "Alerón trasero dañado, %d por ciento, entra a boxes",
            "Daño en el alerón trasero, %d por ciento, valora boxes",
            "Alerón trasero al %d por ciento de daño, mejor entrar a boxes"
    };

    private static final String[] LATERALES = {
            "Daño en los laterales, %d por ciento, entra a boxes",
            "Sidepods dañados, %d por ciento, valora boxes",
            "Daño lateral al %d por ciento, mejor entrar a boxes"
    };

    private boolean avisadoAleron = false;
    private boolean avisadoLaterales = false;

    public String evaluar(Estado e) {
        String mensaje = null;

        if (!avisadoAleron && e.aleronTraseroDano >= UMBRAL_DANO) {
            avisadoAleron = true;
            mensaje = String.format(Frases.elegir(ALERON), e.aleronTraseroDano);
        } else if (!avisadoLaterales && e.lateralesDano >= UMBRAL_DANO) {
            avisadoLaterales = true;
            mensaje = String.format(Frases.elegir(LATERALES), e.lateralesDano);
        }

        if (e.aleronTraseroDano < UMBRAL_DANO) avisadoAleron = false;
        if (e.lateralesDano < UMBRAL_DANO) avisadoLaterales = false;

        return mensaje;
    }
}
