import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Emisor {

    // --- El guion: valores que van cambiando durante la carrera ---
    static float combustible    = 23.0f;
    static int   vuelta         = 1;
    static int   bandera        = 0;
    static int   sector         = 0;
    static int   drsMetros      = 0;
    static int   drsPermitido   = 0;
    static int   tempGomas      = 95;
    static int   ultimaVueltaMs = 0;
    static int   sector1Ms      = 0;
    static int   sector2Ms      = 0;

    static DatagramSocket socket;
    static InetAddress destino;

    public static void main(String[] args) throws Exception {
        socket  = new DatagramSocket();
        destino = InetAddress.getByName("127.0.0.1");

        for (vuelta = 1; vuelta <= 20; vuelta++) {

            combustible -= 1.15f;
            if (vuelta == 12) bandera = 3;      // sale amarilla
            if (vuelta == 14) bandera = 0;      // se retira
            if (vuelta >= 15) tempGomas += 4;   // las gomas se calientan

            System.out.println("--- Vuelta " + vuelta
                    + " | gasolina " + String.format("%.1f", combustible)
                    + " | gomas " + tempGomas);

            recorrerVuelta();
        }
        socket.close();
    }

    // Simula el paso por los tres sectores de una vuelta
    static void recorrerVuelta() throws Exception {

        // --- Sector 1 ---
        sector = 0;
        drsMetros = 0;
        drsPermitido = 0;
        emitirTodo();
        Thread.sleep(2000);

        // Zona de DRS: primero la cuenta atras, luego se abre
        drsMetros = 400;
        emitirTodo();
        Thread.sleep(3000);

        drsMetros = 0;
        drsPermitido = 1;
        emitirTodo();
        Thread.sleep(2000);

        drsPermitido = 0;

        // --- Sector 2: al cambiar, se cierra el sector 1 ---
        sector1Ms = 28000 + (int) (Math.random() * 800);
        sector = 1;
        emitirTodo();
        Thread.sleep(10000);

        // --- Sector 3: al cambiar, se cierra el sector 2 ---
        sector2Ms = 31000 + (int) (Math.random() * 800);
        sector = 2;
        emitirTodo();
        Thread.sleep(10000);

        // --- Cruce de meta: queda registrado el tiempo de vuelta ---
        ultimaVueltaMs = sector1Ms + sector2Ms + 25000 + (int) (Math.random() * 900);
    }

    static void emitirTodo() throws Exception {
        enviar(paquete2());
        enviar(paquete6());
        enviar(paquete7());
    }

    // --- Cabecera comun a todos los paquetes ---
    static ByteBuffer nuevoPaquete(int tam, int packetId) {
        ByteBuffer bb = ByteBuffer.allocate(tam);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(0, (short) 2025);      // m_packetFormat
        bb.put(2, (byte) 25);              // m_gameYear
        bb.put(6, (byte) packetId);        // m_packetId
        bb.put(27, (byte) 0);              // m_playerCarIndex
        return bb;
    }

    // --- Paquete 2: Lap Data (bloque de 57) ---
    static byte[] paquete2() {
        ByteBuffer bb = nuevoPaquete(1285, 2);
        int base = 29;
        bb.putInt(base, ultimaVueltaMs);                        // m_lastLapTimeInMS
        bb.putShort(base + 8,  (short) (sector1Ms % 60000));    // m_sector1TimeMSPart
        bb.put(base + 10, (byte) (sector1Ms / 60000));          // m_sector1TimeMinutesPart
        bb.putShort(base + 11, (short) (sector2Ms % 60000));    // m_sector2TimeMSPart
        bb.put(base + 13, (byte) (sector2Ms / 60000));          // m_sector2TimeMinutesPart
        bb.put(base + 32, (byte) 4);                            // m_carPosition
        bb.put(base + 33, (byte) vuelta);                       // m_currentLapNum
        bb.put(base + 36, (byte) sector);                       // m_sector
        return bb.array();
    }

    // --- Paquete 6: Car Telemetry (bloque de 60) ---
    static byte[] paquete6() {
        ByteBuffer bb = nuevoPaquete(1352, 6);
        int base = 29;
        bb.putShort(base, (short) 280);            // m_speed
        bb.putShort(base + 16, (short) 11500);     // m_engineRPM
        bb.put(base + 19, (byte) 70);              // m_revLightsPercent
        for (int i = 0; i < 4; i++) {
            bb.put(base + 30 + i, (byte) tempGomas);   // m_tyresSurfaceTemperature
        }
        return bb.array();
    }

    // --- Paquete 7: Car Status (bloque de 55) ---
    static byte[] paquete7() {
        ByteBuffer bb = nuevoPaquete(1239, 7);
        int base = 29;
        bb.putFloat(base + 13, combustible);        // m_fuelRemainingLaps
        bb.put(base + 22, (byte) drsPermitido);     // m_drsAllowed
        bb.putShort(base + 23, (short) drsMetros);  // m_drsActivationDistance
        bb.put(base + 28, (byte) bandera);          // m_vehicleFiaFlags
        bb.putFloat(base + 37, 2_000_000f);         // m_ersStoreEnergy
        return bb.array();
    }

    static void enviar(byte[] datos) throws Exception {
        socket.send(new DatagramPacket(datos, datos.length, destino, 20777));
    }
}