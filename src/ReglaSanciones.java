public class ReglaSanciones {

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
            return "Sanción de " + nuevos + " segundos";
        }

        if (e.avisos > avisosAnterior) {
            avisosAnterior = e.avisos;
            return "Aviso de dirección de carrera, cuidado con los límites";
        }

        penaltiesAnterior = e.penalties;
        avisosAnterior = e.avisos;
        return null;
    }
}
