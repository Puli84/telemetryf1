public class ReglaGomas {

    private final int minimo;
    private final int maximo;

    public ReglaGomas(int minimo, int maximo) {
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public String evaluar(Estado e) {
        for (int t : e.tempGomas) {
            if (t > maximo) return "Gomas pasadas de temperatura, cuida las frenadas";
        }
        return null;
    }
}