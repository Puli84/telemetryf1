public class ReglaSanciones {

    private static final String[] SANCION = {
            "Sanción de %d segundos",
            "Te han metido %d segundos de sanción",
            "Sanción, %d segundos añadidos"
    };

    private static final String[] AVISO_LIMITES = {
            "Aviso de dirección de carrera, cuidado con los límites",
            "Ojo con los límites de pista",
            "Aviso de límites, ten cuidado"
    };

    private int penaltiesAnterior = -1;
    private int avisosAnterior = -1;

    public String evaluar(Estado e) {
        if (penaltiesAnterior == -1) {          // primera lectura
            penaltiesAnterior = e.penalties;
            avisosAnterior = e.avisos;
            return null;
        }

        if (e.penalties > penaltiesAnterior) {
            int nuevos = e.penalties - penaltiesAnterior;
            penaltiesAnterior = e.penalties;
            avisosAnterior = e.avisos;
            return String.format(Frases.elegir(SANCION), nuevos);
        }

        if (e.avisos > avisosAnterior) {
            avisosAnterior = e.avisos;
            return Frases.elegir(AVISO_LIMITES);
        }

        penaltiesAnterior = e.penalties;
        avisosAnterior = e.avisos;
        return null;
    }
}
