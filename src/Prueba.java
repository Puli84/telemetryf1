public class Prueba {
    public static void main(String[] args) {
        Estado e = new Estado();
        e.vueltasTotales = 20;
        e.vueltaActual = 15;
        e.combustibleVueltas = 8.0f;

        ReglaCombustible r = new ReglaCombustible(0.5f);
        System.out.println(r.evaluar(e));
    }
}