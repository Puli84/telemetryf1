package crewchief;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Lee los JSON de sesiones/ (los escribe RegistradorSesion.java) para que los
// controladores web no dupliquen la lógica de listar y parsear ficheros.
public class SesionesRepositorio {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<SesionArchivo> todas() {
        List<SesionArchivo> resultado = new ArrayList<>();
        Path dir = Path.of("sesiones");
        if (!Files.isDirectory(dir)) return resultado;

        try (DirectoryStream<Path> archivos = Files.newDirectoryStream(dir, "*.json")) {
            for (Path archivo : archivos) {
                try {
                    JsonNode raiz = mapper.readTree(archivo.toFile());
                    resultado.add(new SesionArchivo(archivo.getFileName().toString(), raiz));
                } catch (IOException ex) {
                    System.err.println("No se pudo leer " + archivo + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("No se pudo listar sesiones/: " + ex.getMessage());
        }
        return resultado;
    }

    public List<SesionArchivo> deCircuito(int trackId) {
        List<SesionArchivo> resultado = new ArrayList<>();
        for (SesionArchivo s : todas()) {
            if (s.datos().path("circuito").asInt(-1) == trackId) resultado.add(s);
        }
        return resultado;
    }

    public Optional<SesionArchivo> porArchivo(String nombreArchivo) {
        Path destino = Path.of("sesiones", nombreArchivo);
        if (!Files.isRegularFile(destino)) return Optional.empty();
        try {
            return Optional.of(new SesionArchivo(nombreArchivo, mapper.readTree(destino.toFile())));
        } catch (IOException ex) {
            System.err.println("No se pudo leer " + destino + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public record SesionArchivo(String archivo, JsonNode datos) {}
}
