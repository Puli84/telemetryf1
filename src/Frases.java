import java.util.Random;

public class Frases {

    private static final Random azar = new Random();

    public static String elegir(String... opciones) {
        return opciones[azar.nextInt(opciones.length)];
    }
}
