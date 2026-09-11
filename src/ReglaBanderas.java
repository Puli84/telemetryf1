public class ReglaBanderas {

    private static final String[] VERDE = {
            "Bandera verde, pista despejada",
            "Verde, ya puedes atacar",
            "Verde, pista limpia"
    };

    private static final String[] AZUL = {
            "Bandera azul, te doblan",
            "Azul, déjale pasar",
            "Tienes Bandera azul, vienen por detrás"
    };

    private static final String[] AMARILLA = {
            "Amarilla, levanta el pie",
            "Bandera amarilla, cuidado",
            "Tenemos amarilla ahí delante"
    };

    private int anterior = 0;

    public String evaluar(Estado e) {
        if (e.bandera == anterior) {
            return null;
        }
        anterior = e.bandera;

        switch (e.bandera) {
            case 1:  return Frases.elegir(VERDE);
            case 2:  return Frases.elegir(AZUL);
            case 3:  return Frases.elegir(AMARILLA);
            default: return null;
        }
    }
}
