import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Emisor {

    // --- El guion: valores que van cambiando vuelta a vuelta ---
    static float combustible = 23.0f;   // vueltas que quedan de gasolina
    static int   vuelta      = 1;
    static int   bandera     = 0;       // 0 ninguna, 3 amarilla

    static DatagramSocket socket;
    static InetAddress destino;

    public static void main(String[] args) throws Exception {
        socket  = new DatagramSocket();
        destino = InetAddress.getByName("127.0.0.1");

        for (vuelta = 1; vuelta <= 20; vuelta++) {
            combustible -= 1.15f;
            if (vuelta == 12) bandera = 3;   // sale amarilla
            if (vuelta == 14) bandera = 0;   // se retira

            System.out.println("Vuelta " + vuelta + " | gasolina " + combustible);
            enviar(paquete7());
            enviar(paquete2());

            Thread.sleep(500);   // media vuelta comprimida
        }
        socket.close();
    }

    // --- Cabecera común a todos los paquetes ---
    static ByteBuffer nuevoPaquete(int tam, int packetId) {
        ByteBuffer bb = ByteBuffer.allocate(tam);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(0, (short) 2025);
        bb.put(2, (byte) 25);
        bb.put(6, (byte) packetId);
        bb.put(27, (byte) 0);          // tu coche es el índice 0
        return bb;
    }

    // --- Paquete 7: Car Status ---
    static byte[] paquete7() {
        ByteBuffer bb = nuevoPaquete(1239, 7);
        int base = 29;                          // 29 + 0 * 55
        bb.putFloat(base + 13, combustible);    // m_fuelRemainingLaps
        bb.put(base + 28, (byte) bandera);      // m_vehicleFiaFlags
        return bb.array();
    }
    static byte[] paquete2() {
        ByteBuffer bb = nuevoPaquete(1285, 2);
        int base = 29;
        bb.put(base + 33, (byte) vuelta);   // m_currentLapNum
        return bb.array();
    }

    // --- Mandarlo por el socket ---
    static void enviar(byte[] datos) throws Exception {
        socket.send(new DatagramPacket(datos, datos.length, destino, 20777));
    }
}