public class PruebaOyente {
    public static void main(String[] args) {
        System.out.println("Habla ahora (tienes 5 segundos)...");
        Oyente oyente = new Oyente();
        String resultado = oyente.escuchar(new String[]{"verstappen", "hamilton", "perez"});
        System.out.println("Reconocido: " + resultado);
    }
}
