package crewchief;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrewChiefApplication {
    public static void main(String[] args) {
        // Tiene que fijarse aquí, antes de SpringApplication.run(): Spring Boot decide
        // el modo headless nada más entrar en run(), antes incluso de leer
        // application.properties, así que spring.main.headless=false no llega a tiempo.
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(CrewChiefApplication.class, args);
    }
}
