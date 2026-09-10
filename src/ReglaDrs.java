public class ReglaDrs {

    private boolean avisadoCuenta = false;
    private boolean avisadoAbierto = false;

    public String evaluar(Estado e) {

        if (e.drsPermitido == 1) {
            avisadoCuenta = false;
            if (!avisadoAbierto) {
                avisadoAbierto = true;
                return "DRS disponible";
            }
            return null;
        }

        avisadoAbierto = false;

        if (e.drsMetros > 0 && !avisadoCuenta) {
            avisadoCuenta = true;
            return "DRS en " + e.drsMetros + " metros";
        }

        if (e.drsMetros == 0) {
            avisadoCuenta = false;
        }
        return null;
    }
}