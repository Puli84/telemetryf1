package crewchief;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Arranca el Listener (socket UDP + ventana de la tira LED) en un hilo de fondo
// al levantar la app Spring, sin bloquear el arranque del servidor web.
// El Listener sigue siendo el mismo main() de siempre, sin tocar; esto es solo
// el enganche para que viva dentro del mismo proceso que sirve la web.
@Component
public class ListenerRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        Thread hilo = new Thread(() -> {
            try {
                Listener.main(args);
            } catch (Exception ex) {
                System.err.println("El Listener se cayó: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, "listener-udp");
        hilo.setDaemon(true);
        hilo.start();
    }
}
