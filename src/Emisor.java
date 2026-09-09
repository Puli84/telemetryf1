import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Emisor {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress destino = InetAddress.getByName("127.0.0.1");

        byte[] datos = new byte[100];

        int rpm=500;

        while (true) {
            ByteBuffer bb = ByteBuffer.wrap(datos);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putShort(0, (short) 2025);
            bb.put(2, (byte) 1);
            bb.put(3, (byte) 20);
            bb.put(4, (byte) 1);
            bb.put(5, (byte) 6);
            bb.putShort(50, (short) rpm);

            DatagramPacket paquete =
                    new DatagramPacket(datos, datos.length, destino, 20777);
            socket.send(paquete);

            rpm += 500;
            if (rpm > 13000) rpm = 5000;

            Thread.sleep(500);
        }
    }
}