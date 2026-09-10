import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Listener {

    private static final int LEDS_ABAJO = 60;
    private static Tira tira;
     static int tipoSesion;
    static Estado estado = new Estado();
    static ReglaCombustible reglaCombustible = new ReglaCombustible(0.5f);
    static ReglaBanderas reglaBanderas = new ReglaBanderas();
    static Locutor locutor = new Locutor();

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket (
                20777, InetAddress.getByName ( "192.168.1.39" ) );
        byte[] buffer = new byte[2048];

        JFrame ventana = new JFrame ( "Rev lights" );
        tira = new Tira ();

        ventana.add ( tira );
        ventana.setSize ( 900, 500 );
        ventana.setDefaultCloseOperation ( JFrame.EXIT_ON_CLOSE );
        ventana.setVisible ( true );

        System.out.println ( "Escuchando en 20777..." );


        while (true) {
            DatagramPacket paquete = new DatagramPacket ( buffer, buffer.length );
            socket.receive ( paquete );

            ByteBuffer bb = ByteBuffer.wrap ( buffer );
            bb.order ( ByteOrder.LITTLE_ENDIAN );

            int packetId = bb.get ( 6 );
            int miCoche = bb.get ( 27 );

            switch (packetId) {
                case 2 -> leerVuelta ( bb, miCoche );
                case 6 -> leerTelemetria ( bb, miCoche );
                case 7 -> leerEstado ( bb, miCoche );
                case 1 -> leerSesion ( bb );
            }
        }
    }

    private static void leerTelemetria(ByteBuffer bb, int coche) {
        int base = 29 + coche * 60;

        int velocidad = bb.getShort ( base );
        int rpm = bb.getShort ( base + 16 );
        int drs = bb.get ( base + 18 );
        int revLights = bb.get ( base + 19 );

        tira.setEncendidos ( (revLights * LEDS_ABAJO) / 100 );
    }

    private static void leerEstado(ByteBuffer bb, int coche) {
        int base = 29 + coche * 55;

        estado.combustibleVueltas = bb.getFloat(base + 13);
        estado.bandera  = bb.get ( base + 28 );
        tira.setBandera ( estado.bandera );
        System.out.println("Gasolina: " + estado.combustibleVueltas + " | Bandera: " + estado.bandera);
        String msg = reglaCombustible.evaluar(estado);
        if (msg != null) locutor.decir("combustible", msg, 5000);
        String flag = reglaBanderas.evaluar(estado);
        if (flag != null) locutor.decir("bandera", flag, 0);
    }

    private static void leerVuelta(ByteBuffer bb, int coche) {
        int base = 29 + coche * 57;
        int posicion = bb.get ( base + 32 );
        estado.vueltaActual = bb.get(base + 33);

        System.out.println ( "pos=" + posicion + " vuelta=" + estado.vueltaActual );
    }

    private static void leerSesion(ByteBuffer bb) {

         tipoSesion = bb.get ( 35 );
    }
}


